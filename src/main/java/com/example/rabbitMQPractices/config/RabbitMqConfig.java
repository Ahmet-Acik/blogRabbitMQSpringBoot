package com.example.rabbitMQPractices.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
    import org.springframework.amqp.rabbit.connection.ConnectionFactory;
    import org.springframework.amqp.rabbit.core.RabbitTemplate;
    import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;

    @Configuration
    public class RabbitMqConfig {

        public static final String QUEUE_NAME = "blogQueue";
        public static final String EXCHANGE_NAME = "blogExchange";
        public static final String ROUTING_KEY = "blogRoutingKey";

        @Bean
        public Queue queue() {
            return new Queue(QUEUE_NAME, true);
        }

        @Bean
        public DirectExchange exchange() {
            return new DirectExchange(EXCHANGE_NAME);
        }

        @Bean
        public Binding binding(Queue queue, DirectExchange exchange) {
            return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
        }

        @Bean
        public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
            return new Jackson2JsonMessageConverter();
        }

        @Bean
        public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
            RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
            rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
            return rabbitTemplate;
        }

   @Bean
   public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
       SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
       factory.setConnectionFactory(connectionFactory);
       factory.setAcknowledgeMode(AcknowledgeMode.MANUAL); // Set to MANUAL
       factory.setMessageConverter(jackson2JsonMessageConverter());
       return factory;
   }
    }