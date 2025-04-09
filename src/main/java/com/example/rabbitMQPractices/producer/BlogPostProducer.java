package com.example.rabbitMQPractices.producer;

import com.example.rabbitMQPractices.entity.BlogPost;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class BlogPostProducer {

    private final RabbitTemplate rabbitTemplate;

    public BlogPostProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                System.out.println("Message successfully sent to the broker.");
            } else {
                System.err.println("Message failed to send: " + cause);
            }
        });
    }

    public void sendBlogPost(BlogPost blogPost) {
        rabbitTemplate.convertAndSend("blogExchange", "blogRoutingKey", blogPost);
        System.out.println("Blog post sent: " + blogPost);
    }
}