package com.example.rabbitMQPractices;

import com.example.rabbitMQPractices.entity.BlogPost;
import com.example.rabbitMQPractices.producer.BlogPostProducer;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RabbitMqPracticesApplication {

	@Autowired
	private BlogPostProducer blogPostProducer;

	public static void main(String[] args) {
		SpringApplication.run(RabbitMqPracticesApplication.class, args);
	}
	@PostConstruct
	public void testProducer() {
		BlogPost blogPost = new BlogPost("1", "Test Title", "Test Content");
		blogPostProducer.sendBlogPost(blogPost);
	}
}
