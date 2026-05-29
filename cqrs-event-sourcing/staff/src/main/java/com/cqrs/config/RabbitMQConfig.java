package com.cqrs.config;

import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "crm.events";

    @Bean
    TopicExchange crmExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    Jackson2JsonMessageConverter messageConverter(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
