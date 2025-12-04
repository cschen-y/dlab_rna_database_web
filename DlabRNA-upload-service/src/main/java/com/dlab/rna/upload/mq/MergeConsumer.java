// package com.dlab.rna.upload.mq;

// import com.dlab.rna.upload.config.RabbitConfig;
// import com.dlab.rna.upload.service.UploadService;
// import com.rabbitmq.client.Channel;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.amqp.support.AmqpHeaders;
// import org.springframework.amqp.rabbit.annotation.RabbitListener;
// import org.springframework.messaging.handler.annotation.Header;
// import org.springframework.messaging.handler.annotation.Payload;
// import org.springframework.stereotype.Component;
// import com.rabbitmq.stream.Environment;
// import com.rabbitmq.stream.Producer;
// import jakarta.annotation.PostConstruct;

// @Component
// @Slf4j
// public class MergeConsumer {
//     private final UploadService service;
//     private final Environment env;
//     private Producer splitProducer;

//     public MergeConsumer(UploadService service, Environment env) {
//         this.service = service;
//         this.env = env;
//     }

//     @PostConstruct
//     public void init() {
//         try { env.streamCreator().stream(RabbitConfig.SPLIT_QUEUE).create(); } catch (Throwable ignored) {}
//         splitProducer = env.producerBuilder().stream(RabbitConfig.SPLIT_QUEUE).build();
//     }

//     @RabbitListener(queues = RabbitConfig.MERGE_QUEUE, containerFactory = "manualAckContainerFactory")
//     public void onMessage(@Payload String fileId,
//                           Channel channel,
//                           @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
//         try {
//             log.info("fileId: " + fileId + " deliveryTag: " + deliveryTag);
//             service.compose(fileId);
//             service.finishMerged(fileId);
//             String object = service.getStoragePath(fileId);
//             String payload = "{\"fileId\":\"" + fileId + "\",\"object\":\"" + object + "\"}";
//             splitProducer.send(splitProducer.messageBuilder().addData(payload.getBytes()).build(), s -> {});
//             log.info("payload: " + payload);
//             channel.basicAck(deliveryTag, false);
//         } catch (Exception e) {
//             try {
//                 service.failMerged(fileId, e.getMessage());
//                 channel.basicAck(deliveryTag, false);
//             } catch (Exception ignored) {}
//         }
//     }
// }