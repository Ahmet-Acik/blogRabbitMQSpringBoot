package com.example.rabbitMQPractices.controller;

import com.example.rabbitMQPractices.entity.BlogPost;
import com.example.rabbitMQPractices.producer.BlogPostProducer;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/blog-posts")
public class BlogPostController {

    private final BlogPostProducer blogPostProducer;

    public BlogPostController(BlogPostProducer blogPostProducer) {
        this.blogPostProducer = blogPostProducer;
    }

    @PostMapping
    public String createBlogPost(@RequestBody BlogPost blogPost) {
        blogPost.setId(UUID.randomUUID().toString());
        blogPostProducer.sendBlogPost(blogPost);
        return "Blog post sent: " + blogPost;
    }
}