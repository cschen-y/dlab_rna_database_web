package com.dlab.rna.upload.config;

import com.rabbitmq.stream.Address;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import com.rabbitmq.stream.Environment;

import java.util.List;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "upload.ex";
    public static final String MERGE_QUEUE = "upload.merge";
    public static final String MERGE_DLQ = "upload.merge.dlq";
    public static final String RECONCILE_QUEUE = "upload.reconcile";
    public static final String RECONCILE_DLQ = "upload.reconcile.dlq";

    public static final String SPLIT_QUEUE = "kb.split";
    public static final String PARSED_QUEUE = "kb.parsed";
    public static final String CHUNKS_QUEUE = "kb.chunks";
    public static final String VECTORS_QUEUE = "kb.vectors";

    @Bean
    public DirectExchange uploadExchange() { return new DirectExchange(EXCHANGE, true, false); }

    @Bean
    public Queue mergeQueue() {
        return new Queue(MERGE_QUEUE, true, false, false, java.util.Map.of(
                "x-dead-letter-exchange", EXCHANGE,
                "x-dead-letter-routing-key", MERGE_DLQ
        ));
    }

    @Bean
    public Queue mergeDlq() { return new Queue(MERGE_DLQ, true); }

    @Bean
    public Queue reconcileQueue() {
        return new Queue(RECONCILE_QUEUE, true, false, false, java.util.Map.of(
                "x-dead-letter-exchange", EXCHANGE,
                "x-dead-letter-routing-key", RECONCILE_DLQ
        ));
    }

    @Bean
    public Queue reconcileDlq() { return new Queue(RECONCILE_DLQ, true); }

    @Bean
    public Binding bindMerge(DirectExchange ex, Queue mergeQueue) {
        return BindingBuilder.bind(mergeQueue).to(ex).with(MERGE_QUEUE);
    }

    @Bean
    public Binding bindReconcile(DirectExchange ex, Queue reconcileQueue) {
        return BindingBuilder.bind(reconcileQueue).to(ex).with(RECONCILE_QUEUE);
    }

    @Bean
    public Binding bindMergeDlq(DirectExchange ex, Queue mergeDlq) {
        return BindingBuilder.bind(mergeDlq).to(ex).with(MERGE_DLQ);
    }

    @Bean
    public Binding bindReconcileDlq(DirectExchange ex, Queue reconcileDlq) {
        return BindingBuilder.bind(reconcileDlq).to(ex).with(RECONCILE_DLQ);
    }

    @Bean
    public Environment streamEnvironment(
            @Value("${spring.rabbitmq.username}") String username,
            @Value("${spring.rabbitmq.password}") String password) {

        return Environment.builder()
                .username(username)
                .password(password)
                .addressResolver(address ->
                        new Address("115.190.5.32", 5552))
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory manualAckContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        factory.setConcurrentConsumers(4);
        factory.setMaxConcurrentConsumers(16);
        return factory;
    }
}