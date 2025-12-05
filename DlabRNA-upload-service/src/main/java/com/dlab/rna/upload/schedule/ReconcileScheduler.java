package com.dlab.rna.upload.schedule;

import com.dlab.rna.upload.config.RabbitConfig;
import com.dlab.rna.upload.config.StorageProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;


@Configuration
@EnableScheduling
public class ReconcileScheduler {
    private final StringRedisTemplate redis;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final StorageProperties props;

    public ReconcileScheduler(StringRedisTemplate redis, KafkaTemplate<String, String> kafkaTemplate, StorageProperties props) {
        this.redis = redis;
        this.kafkaTemplate = kafkaTemplate;
        this.props = props;
    }

    @Scheduled(cron = "${storage.reconcile.cron}")
    public void scan() {
        for (String key : redis.keys("upload:*:state")) {
            String state = redis.opsForValue().get(key);
            if ("MERGING".equals(state)) {
                String fileId = key.substring("upload:".length(), key.length() - ":state".length());
                Boolean ok = redis.opsForValue().setIfAbsent("upload:" + fileId + ":reconcile:enqueued", "1", Duration.ofMinutes(10));
                if (Boolean.FALSE.equals(ok)) {
                    continue;
                }
                kafkaTemplate.send(RabbitConfig.RECONCILE_QUEUE, fileId);
            }
        }
    }
}
