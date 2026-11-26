package com.cqrs.config;

import com.cqrs.event.*;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "crm.events";
    public static final String QUEUE    = "crm.events.queue";
    public static final String DLQ      = "crm.events.dlq";
    public static final String DLX      = "crm.events.dlx";

    @Bean
    TopicExchange crmExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    TopicExchange dlx() {
        return ExchangeBuilder.topicExchange(DLX).durable(true).build();
    }

    @Bean
    Queue crmQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", "dead")
                .build();
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    Binding crmBinding(Queue crmQueue, TopicExchange crmExchange) {
        return BindingBuilder.bind(crmQueue).to(crmExchange).with("#");
    }

    @Bean
    Binding dlqBinding(Queue deadLetterQueue, TopicExchange dlx) {
        return BindingBuilder.bind(deadLetterQueue).to(dlx).with("#");
    }

    @Bean
    JacksonJsonMessageConverter messageConverter() {
        DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();
        typeMapper.setTrustedPackages("com.cqrs.*");

        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("com.cqrs.domain.event.CustomerCreatedEvent",        CustomerCreatedMessage.class);
        idClassMapping.put("com.cqrs.domain.event.CustomerDataUpdatedEvent",    CustomerDataUpdatedMessage.class);
        idClassMapping.put("com.cqrs.domain.event.AccountManagerAssignedEvent", AccountManagerAssignedMessage.class);
        idClassMapping.put("com.cqrs.domain.event.WalletCreatedEvent",          WalletCreatedMessage.class);
        idClassMapping.put("com.cqrs.domain.event.TransactionRegisteredEvent",  TransactionRegisteredMessage.class);
        idClassMapping.put("com.cqrs.domain.event.AccountManagerCreatedEvent",  AccountManagerCreatedMessage.class);
        idClassMapping.put("com.cqrs.domain.event.AdminProfileGrantedEvent",    AdminProfileGrantedMessage.class);
        idClassMapping.put("com.cqrs.domain.event.BranchAssignedEvent",         BranchAssignedMessage.class);
        idClassMapping.put("com.cqrs.domain.event.BranchCreatedEvent",          BranchCreatedMessage.class);
        typeMapper.setIdClassMapping(idClassMapping);

        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, JacksonJsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
