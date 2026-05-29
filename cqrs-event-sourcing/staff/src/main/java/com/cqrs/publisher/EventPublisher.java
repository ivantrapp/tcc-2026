package com.cqrs.publisher;

import com.cqrs.config.RabbitMQConfig;
import com.cqrs.domain.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(DomainEvent event) {
        String routingKey = toRoutingKey(event.eventType());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, routingKey, event);
    }

    private String toRoutingKey(String eventType) {
        return eventType.replaceAll("([A-Z])", ".$1").toLowerCase().replaceFirst("^\\.", "");
    }
}
