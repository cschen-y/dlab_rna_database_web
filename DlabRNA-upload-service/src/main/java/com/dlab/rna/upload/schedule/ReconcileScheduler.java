package com.dlab.rna.upload.schedule;

import com.dlab.rna.upload.config.RabbitConfig;
import com.dlab.rna.upload.config.StorageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.redis.core.StringRedisTemplate;


@Configuration
@EnableScheduling
public class ReconcileScheduler {
    private final StringRedisTemplate redis;
    private final RabbitTemplate rabbitTemplate;
    private final StorageProperties props;

    public ReconcileScheduler(StringRedisTemplate redis, RabbitTemplate rabbitTemplate, StorageProperties props) {
        this.redis = redis;
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    @Scheduled(cron = "${storage.reconcile.cron}")
    public void scan() {
        for (String key : redis.keys("upload:*:state")) {
            String state = redis.opsForValue().get(key);
            if ("MERGING".equals(state)) {
                String fileId = key.substring("upload:".length(), key.length() - ":state".length());
                rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.RECONCILE_QUEUE, fileId);
            }
        }
    }
}
