package com.example.rabbitMQPractices.integtation;

import com.example.rabbitMQPractices.entity.BlogPost;
import com.example.rabbitMQPractices.producer.BlogPostProducer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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

    private static RabbitMQContainer rabbitMQContainer;  // RabbitMQ container for integration testing

    @Autowired
    private BlogPostProducer blogPostProducer;  // Producer to send messages to RabbitMQ

    @Autowired
    private RabbitTemplate rabbitTemplate;   // RabbitTemplate to interact with RabbitMQ

    private static final String QUEUE_NAME = "testQueue";   // Name of the queue for testing

    // Latch to synchronize the test with the message listener
    private static final CountDownLatch latch = new CountDownLatch(1);
    private static BlogPost receivedBlogPost; // Variable to store the received BlogPost

    @BeforeAll
    static void startRabbitMQ() {
        // Start a RabbitMQ container using Testcontainers
        rabbitMQContainer = new RabbitMQContainer("rabbitmq:3-management");
        rabbitMQContainer.start();

        // Set RabbitMQ host and port for the application
        System.setProperty("spring.rabbitmq.host", rabbitMQContainer.getHost());
        System.setProperty("spring.rabbitmq.port", rabbitMQContainer.getAmqpPort().toString());

        // Declare exchange, queue, and binding programmatically
        RabbitTemplate rabbitTemplate = new RabbitTemplate();
        rabbitTemplate.setConnectionFactory(new CachingConnectionFactory(rabbitMQContainer.getHost(), rabbitMQContainer.getAmqpPort()));
        rabbitTemplate.execute(channel -> {
            // Declare a direct exchange
            channel.exchangeDeclare("blogExchange", "direct", true);
            // Declare a queue
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            // Bind the queue to the exchange with a routing key
            channel.queueBind(QUEUE_NAME, "blogExchange", "blogRoutingKey");
            return null;
        });
    }

    @AfterAll
    static void stopRabbitMQ() {
        // Stop the RabbitMQ container after all tests
        rabbitMQContainer.stop();
    }

    @Test
    void testProducerAndConsumer() throws InterruptedException {
        // Create a sample BlogPost to send
        BlogPost blogPost = new BlogPost("1", "Test Title", "Test Content");

        // Send the BlogPost message to RabbitMQ
        blogPostProducer.sendBlogPost(blogPost);

        // Wait for the message to be received by the listener
        boolean messageReceived = latch.await(5, TimeUnit.SECONDS);

        // Assert that the received message matches the sent message
        if (messageReceived) {
            assertEquals(blogPost.getId(), receivedBlogPost.getId()); // Verify ID
            assertEquals(blogPost.getTitle(), receivedBlogPost.getTitle()); // Verify title
            assertEquals(blogPost.getContent(), receivedBlogPost.getContent()); // Verify content
        } else {
            // Fail the test if the message was not received within the timeout
            throw new AssertionError("Message was not received within the timeout period.");
        }
    }

    @RabbitListener(queues = QUEUE_NAME)
    public void listen(BlogPost blogPost) {
        // Listener method to process messages from the queue
        receivedBlogPost = blogPost; // Store the received BlogPost
        latch.countDown(); // Decrement the latch to signal the test
    }

    @Test
    void testErrorHandling() throws InterruptedException {
        // Simulate an invalid BlogPost (e.g., null content)
        BlogPost invalidBlogPost = new BlogPost("1", "Invalid Title", null);

        // Send the invalid BlogPost message
        blogPostProducer.sendBlogPost(invalidBlogPost);

        // Wait for the message to be processed
        boolean messageReceived = latch.await(5, TimeUnit.SECONDS);

        // Assert that the message was requeued (listener should handle it)
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
        // Stop RabbitMQ
        rabbitMQContainer.stop();

        // Attempt to send a message
        BlogPost blogPost = new BlogPost("1", "Test Title", "Test Content");
        try {
            blogPostProducer.sendBlogPost(blogPost);
            throw new AssertionError("Message should not be sent when RabbitMQ is down.");
        } catch (Exception e) {
            // Expected exception due to RabbitMQ being down
            System.out.println("Producer failed as expected: " + e.getMessage());
        }
    }

    @Test
    void testConsumerWhenQueueIsEmpty() {
        // Ensure the queue is empty
        RabbitAdmin rabbitAdmin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
        rabbitAdmin.purgeQueue(QUEUE_NAME, true);

        // Attempt to receive a message
        BlogPost blogPost = (BlogPost) rabbitTemplate.receiveAndConvert(QUEUE_NAME);

        // Assert that no message is received
        assertEquals(null, blogPost, "Queue should be empty, but a message was received.");
    }

//    @Test
//    void testDifferentRoutingKey() throws InterruptedException {
//        // Send a message with a different routing key
//        rabbitTemplate.convertAndSend("blogExchange", "differentRoutingKey", new BlogPost("2", "Other Title", "Other Content"));
//
//        // Wait for the latch (should not be decremented)
//        boolean messageReceived = latch.await(5, TimeUnit.SECONDS);
//
//        // Assert that the message was not received
//        assertEquals(false, messageReceived, "Message with a different routing key should not be consumed.");
//    }
//    this version is failed refer to the below version
//
    @Test
    void testDifferentRoutingKey() throws InterruptedException {
        // Declare a separate queue for the different routing key
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

        // Send a message with a different routing key
        rabbitTemplate.convertAndSend("blogExchange", "differentRoutingKey", new BlogPost("2", "Other Title", "Other Content"));

        // Attempt to receive the message from the original queue
        BlogPost blogPost = (BlogPost) rabbitTemplate.receiveAndConvert(QUEUE_NAME);

        // Assert that no message is received in the original queue
        assertEquals(null, blogPost, "Message with a different routing key should not be consumed by the original queue.");
    }
}