package com.example.rabbitMQPractices.consumer;

import com.example.rabbitMQPractices.entity.BlogPost;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class BlogPostConsumer {

    @RabbitListener(queues = "blogQueue")
    public void receiveBlogPost(BlogPost blogPost) {
        System.out.println("Blog post received: " + blogPost);
        // Process the blog post (e.g., save to database)
    }
}