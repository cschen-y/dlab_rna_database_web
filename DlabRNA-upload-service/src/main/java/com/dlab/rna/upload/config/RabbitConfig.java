package com.dlab.rna.upload.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    public Queue splitQueue() { return new Queue(SPLIT_QUEUE, true); }
    @Bean
    public Queue parsedQueue() { return new Queue(PARSED_QUEUE, true); }
    @Bean
    public Queue chunksQueue() { return new Queue(CHUNKS_QUEUE, true); }
    @Bean
    public Queue vectorsQueue() { return new Queue(VECTORS_QUEUE, true); }

    @Bean
    public Binding bindSplit(DirectExchange ex, Queue splitQueue) {
        return BindingBuilder.bind(splitQueue).to(ex).with(SPLIT_QUEUE);
    }
    @Bean
    public Binding bindParsed(DirectExchange ex, Queue parsedQueue) {
        return BindingBuilder.bind(parsedQueue).to(ex).with(PARSED_QUEUE);
    }
    @Bean
    public Binding bindChunks(DirectExchange ex, Queue chunksQueue) {
        return BindingBuilder.bind(chunksQueue).to(ex).with(CHUNKS_QUEUE);
    }
    @Bean
    public Binding bindVectors(DirectExchange ex, Queue vectorsQueue) {
        return BindingBuilder.bind(vectorsQueue).to(ex).with(VECTORS_QUEUE);
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