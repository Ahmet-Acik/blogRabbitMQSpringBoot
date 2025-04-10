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
            rabbitMQContainer = new RabbitMQContainer("rabbitmq:3-management");
            rabbitMQContainer.start();

            System.setProperty("spring.rabbitmq.host", rabbitMQContainer.getHost());
            System.setProperty("spring.rabbitmq.port", rabbitMQContainer.getAmqpPort().toString());

            RabbitTemplate rabbitTemplate = new RabbitTemplate();
            rabbitTemplate.setConnectionFactory(new CachingConnectionFactory(rabbitMQContainer.getHost(), rabbitMQContainer.getAmqpPort()));
            rabbitTemplate.execute(channel -> {
                channel.exchangeDeclare("blogExchange", "direct", true);
                channel.queueDeclare(QUEUE_NAME, false, false, false, null);
                channel.queueBind(QUEUE_NAME, "blogExchange", "blogRoutingKey");
                return null;
            });
        }

        @AfterAll
        static void stopRabbitMQ() {
            rabbitMQContainer.stop();
        }

        @BeforeEach
        void setUp() {
            latch = new CountDownLatch(1);
            receivedBlogPost = null;

            RabbitAdmin rabbitAdmin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
            rabbitAdmin.purgeQueue(QUEUE_NAME, true);
        }

        @Test
        void testProducerAndConsumer() throws InterruptedException {
            BlogPost blogPost = new BlogPost("1", "Test Title", "Test Content");

            blogPostProducer.sendBlogPost(blogPost);

            boolean messageReceived = latch.await(5, TimeUnit.SECONDS);

            if (messageReceived) {
                assertEquals(blogPost.getId(), receivedBlogPost.getId());
                assertEquals(blogPost.getTitle(), receivedBlogPost.getTitle());
                assertEquals(blogPost.getContent(), receivedBlogPost.getContent());
            } else {
                throw new AssertionError("Message was not received within the timeout period.");
            }
        }

        @RabbitListener(queues = QUEUE_NAME)
        public void listen(BlogPost blogPost) {
            receivedBlogPost = blogPost;
            latch.countDown();
        }

        @Test
        void testErrorHandling() throws InterruptedException {
            BlogPost invalidBlogPost = new BlogPost("1", "Invalid Title", null);

            blogPostProducer.sendBlogPost(invalidBlogPost);

            boolean messageReceived = latch.await(5, TimeUnit.SECONDS);

            if (messageReceived) {
                assertEquals(invalidBlogPost.getId(), receivedBlogPost.getId());
                assertEquals(invalidBlogPost.getTitle(), receivedBlogPost.getTitle());
                assertEquals(invalidBlogPost.getContent(), receivedBlogPost.getContent());
            } else {
                throw new AssertionError("Message was not requeued as expected.");
            }
        }

        @Test
        void testProducerWhenRabbitMQIsDown() {
            rabbitMQContainer.stop();

            BlogPost blogPost = new BlogPost("1", "Test Title", "Test Content");
            try {
                blogPostProducer.sendBlogPost(blogPost);
                throw new AssertionError("Message should not be sent when RabbitMQ is down.");
            } catch (Exception e) {
                System.out.println("Producer failed as expected: " + e.getMessage());
            }
        }

        @Test
        void testConsumerWhenQueueIsEmpty() {
            RabbitAdmin rabbitAdmin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
            rabbitAdmin.purgeQueue(QUEUE_NAME, true);

            BlogPost blogPost = (BlogPost) rabbitTemplate.receiveAndConvert(QUEUE_NAME);

            assertEquals(null, blogPost, "Queue should be empty, but a message was received.");
        }

        @Test
        void testDifferentRoutingKey() throws InterruptedException {
            String differentQueueName = "differentQueue";
            RabbitAdmin rabbitAdmin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
            rabbitAdmin.declareQueue(new org.springframework.amqp.core.Queue(differentQueueName));
            rabbitAdmin.declareBinding(new org.springframework.amqp.core.Binding(
                differentQueueName,
                org.springframework.amqp.core.Binding.DestinationType.QUEUE,
                "blogExchange",
                "differentRoutingKey",
                null
            ));

            rabbitTemplate.convertAndSend("blogExchange", "differentRoutingKey", new BlogPost("2", "Other Title", "Other Content"));

            BlogPost blogPost = (BlogPost) rabbitTemplate.receiveAndConvert(QUEUE_NAME);

            assertEquals(null, blogPost, "Message with a different routing key should not be consumed by the original queue.");
        }
    }