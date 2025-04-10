package com.example.rabbitMQPractices.integtation;

    import com.example.rabbitMQPractices.entity.BlogPost;
    import com.example.rabbitMQPractices.producer.BlogPostProducer;
    import org.junit.jupiter.api.AfterAll;
    import org.junit.jupiter.api.BeforeAll;
    import org.junit.jupiter.api.BeforeEach;
    import org.junit.jupiter.api.Test;
    import org.springframework.amqp.rabbit.annotation.RabbitListener;
    import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
    import org.springframework.amqp.rabbit.core.RabbitAdmin;
    import org.springframework.amqp.rabbit.core.RabbitTemplate;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.boot.test.context.SpringBootTest;
    import org.testcontainers.containers.RabbitMQContainer;

    import java.util.concurrent.CountDownLatch;
    import java.util.concurrent.TimeUnit;

    import static org.junit.jupiter.api.Assertions.assertEquals;

    @SpringBootTest
    class RabbitMQIntegrationTest {

        private static RabbitMQContainer rabbitMQContainer;

        @Autowired
        private BlogPostProducer blogPostProducer;

        @Autowired
        private RabbitTemplate rabbitTemplate;

        private static final String QUEUE_NAME = "testQueue";

        private static CountDownLatch latch = new CountDownLatch(1);
        private static BlogPost receivedBlogPost;

        @BeforeAll
        static void startRabbitMQ() {
            // Start a RabbitMQ container for integration testing
            rabbitMQContainer = new RabbitMQContainer("rabbitmq:3-management");
            rabbitMQContainer.start();

            // Set RabbitMQ host and port for the application
            System.setProperty("spring.rabbitmq.host", rabbitMQContainer.getHost());
            System.setProperty("spring.rabbitmq.port", rabbitMQContainer.getAmqpPort().toString());

            // Configure RabbitTemplate and declare exchange, queue, and binding
            RabbitTemplate rabbitTemplate = new RabbitTemplate();
            rabbitTemplate.setConnectionFactory(new CachingConnectionFactory(rabbitMQContainer.getHost(), rabbitMQContainer.getAmqpPort()));
            rabbitTemplate.execute(channel -> {
                channel.exchangeDeclare("blogExchange", "direct", true); // Declare a direct exchange
                channel.queueDeclare(QUEUE_NAME, false, false, false, null); // Declare a queue
                channel.queueBind(QUEUE_NAME, "blogExchange", "blogRoutingKey"); // Bind the queue to the exchange
                return null;
            });
        }

        @AfterAll
        static void stopRabbitMQ() {
            // Stop the RabbitMQ container after all tests
            rabbitMQContainer.stop();
        }

        @BeforeEach
        void setUp() {
            // Reset the latch and receivedBlogPost before each test
            latch = new CountDownLatch(1);
            receivedBlogPost = null;

            // Purge the queue to ensure no leftover messages
            RabbitAdmin rabbitAdmin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
            rabbitAdmin.purgeQueue(QUEUE_NAME, true);
        }

        @Test
        void testProducerAndConsumer() throws InterruptedException {
            // Test sending and receiving a message
            BlogPost blogPost = new BlogPost("1", "Test Title", "Test Content");

            // Send a blog post message
            blogPostProducer.sendBlogPost(blogPost);

            // Wait for the message to be received
            boolean messageReceived = latch.await(5, TimeUnit.SECONDS);

            if (messageReceived) {
                // Assert that the received message matches the sent message
                assertEquals(blogPost.getId(), receivedBlogPost.getId());
                assertEquals(blogPost.getTitle(), receivedBlogPost.getTitle());
                assertEquals(blogPost.getContent(), receivedBlogPost.getContent());
            } else {
                throw new AssertionError("Message was not received within the timeout period.");
            }
        }

        @RabbitListener(queues = QUEUE_NAME)
        public void listen(BlogPost blogPost) {
            // Listener to process messages from the queue
            receivedBlogPost = blogPost;
            latch.countDown();
        }

        @Test
        void testErrorHandling() throws InterruptedException {
            // Test handling of invalid messages
            BlogPost invalidBlogPost = new BlogPost("1", "Invalid Title", null);

            // Send an invalid blog post message
            blogPostProducer.sendBlogPost(invalidBlogPost);

            // Wait for the message to be received
            boolean messageReceived = latch.await(5, TimeUnit.SECONDS);

            if (messageReceived) {
                // Assert that the received message matches the invalid message
                assertEquals(invalidBlogPost.getId(), receivedBlogPost.getId());
                assertEquals(invalidBlogPost.getTitle(), receivedBlogPost.getTitle());
                assertEquals(invalidBlogPost.getContent(), receivedBlogPost.getContent());
            } else {
                throw new AssertionError("Message was not requeued as expected.");
            }
        }

        @Test
        void testProducerWhenRabbitMQIsDown() {
            // Test producer behavior when RabbitMQ is down
            rabbitMQContainer.stop();

            BlogPost blogPost = new BlogPost("1", "Test Title", "Test Content");
            try {
                // Attempt to send a message when RabbitMQ is down
                blogPostProducer.sendBlogPost(blogPost);
                throw new AssertionError("Message should not be sent when RabbitMQ is down.");
            } catch (Exception e) {
                System.out.println("Producer failed as expected: " + e.getMessage());
            }
        }

        @Test
        void testConsumerWhenQueueIsEmpty() {
            // Test consumer behavior when the queue is empty
            RabbitAdmin rabbitAdmin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
            rabbitAdmin.purgeQueue(QUEUE_NAME, true); // Purge the queue

            // Attempt to receive a message from the empty queue
            BlogPost blogPost = (BlogPost) rabbitTemplate.receiveAndConvert(QUEUE_NAME);

            // Assert that no message is received
            assertEquals(null, blogPost, "Queue should be empty, but a message was received.");
        }

        @Test
        void testDifferentRoutingKey() throws InterruptedException {
            // Test that messages with a different routing key are not consumed
            String differentQueueName = "differentQueue";
            RabbitAdmin rabbitAdmin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
            rabbitAdmin.declareQueue(new org.springframework.amqp.core.Queue(differentQueueName)); // Declare a new queue
            rabbitAdmin.declareBinding(new org.springframework.amqp.core.Binding(
                differentQueueName,
                org.springframework.amqp.core.Binding.DestinationType.QUEUE,
                "blogExchange",
                "differentRoutingKey",
                null
            )); // Bind the new queue to a different routing key

            // Send a message with a different routing key
            rabbitTemplate.convertAndSend("blogExchange", "differentRoutingKey", new BlogPost("2", "Other Title", "Other Content"));

            // Attempt to receive the message from the original queue
            BlogPost blogPost = (BlogPost) rabbitTemplate.receiveAndConvert(QUEUE_NAME);

            // Assert that no message is received in the original queue
            assertEquals(null, blogPost, "Message with a different routing key should not be consumed by the original queue.");
        }
    }