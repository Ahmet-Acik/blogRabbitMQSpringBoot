package com.example.rabbitMQPractices.consumer;

import com.example.rabbitMQPractices.config.RabbitMqConfig;
import com.example.rabbitMQPractices.entity.BlogPost;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;

@Service
public class BlogPostConsumer {

    @RabbitListener(queues = RabbitMqConfig.QUEUE_NAME)
    public void receiveBlogPost(BlogPost blogPost, Message message, Channel channel) throws Exception {
        try {
            System.out.println("Blog post received: " + blogPost);
            // Process the blog post (e.g., save to database)

            // Acknowledge the message
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            // Reject the message and requeue it
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}