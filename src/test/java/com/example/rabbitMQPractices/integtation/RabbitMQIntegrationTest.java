package com.example.rabbitMQPractices.integtation;

import com.example.rabbitMQPractices.entity.BlogPost;
import com.example.rabbitMQPractices.producer.BlogPostProducer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
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

    private static final CountDownLatch latch = new CountDownLatch(1);
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

    @Test
    void testProducerAndConsumer() throws InterruptedException {
        // Create a sample BlogPost
        BlogPost blogPost = new BlogPost("1", "Test Title", "Test Content");

        // Send the message
        blogPostProducer.sendBlogPost(blogPost);

        // Wait for the message to be processed
        boolean messageReceived = latch.await(5, TimeUnit.SECONDS);

        // Assert the message content
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
}