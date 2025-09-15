package test.dev.demo.business.application.kafka;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.stereotype.Component;
import test.dev.demo.business.application.config.DemoBusinessConfigurationProperties;
import test.dev.smartreplication.client.kafka.producer.KafkaProducerCreator;
import test.dev.smartreplication.core.kafka.KafkaTopicBuilder;
import test.dev.smartreplication.core.kafka.config.KafkaProducerConfig;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class KafkaProducerManager {
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerManager.class);

    private final DemoBusinessConfigurationProperties config;

    public <T> void execute(ProducerContext<T> context, ExecutorService executorService) {
        final ProducerFactory<String, T> producerFactory = KafkaProducerCreator.createProducerFactory(
            kafkaProducerConfig(), context.getValueSerializer());
        final KafkaTemplate<String, T> kafkaTemplate = new KafkaTemplate<>(producerFactory);

        //run
        List<Future<Void>> futures = IntStream
            .range(0, context.getNumThreads())
            .mapToObj(
                i -> executorService.submit(new ProducerJob<>(kafkaTemplate, context))
            ).collect(
                Collectors.toList()
            );

        //await
        futures.forEach(future -> {
            try {
                future.get();
            } catch (Exception e) {
                future.cancel(true);
                logger.error("Problem during finished the thread", e);
            }
        });
    }

    public KafkaProducerConfig kafkaProducerConfig() {
        return new KafkaProducerConfig(config.getKafka().getBootstrapServers());
    }

    public static class ProducerJob<T> implements Callable<Void> {
        private final KafkaTemplate<String, T> kafkaTemplate;
        private final String topicName;
        private final ProducerContext<T> context;

        public ProducerJob(KafkaTemplate<String, T> kafkaTemplate, ProducerContext<T> context) {
            this.kafkaTemplate = kafkaTemplate;
            this.context = context;
            final var builder = new KafkaTopicBuilder()
                .setChangeOwner(context.getMeta().getOwner())
                .setChangeType(context.getMeta().getType())
                .setEndpoint(context.getMeta().getEndpoint());

            if (context.getMeta().isRetry()) {
                builder.activateRetry();
            }

            this.topicName = builder.buildTopicName();
        }

        @Override
        public Void call() {
            return sendPack(kafkaTemplate, context);
        }

        /*
            Указываем сколько сообщений хотим сгенерировать,
            и с какой задержкой, позволяет управлять интенсивностью, с которой сообщения поступают в Kafka
         */
        public Void sendPack(KafkaTemplate<String, T> kafkaTemplate, ProducerContext<T> context) {
            final var delay = context.getMeta().getDelayMs();

            IntStream.range(0, context.getSize()).forEach(i -> {
                final var key = RandomStringUtils.randomAlphabetic(context.getKeySize());

                try {
                    final var metadata = kafkaTemplate
                        .send(topicName, key, context.getCreator().apply(i, context))
                        .get().getRecordMetadata();

                    logger.debug("Committed offset for the topic <{}-{}>: {}", topicName, metadata.partition(), metadata.offset());
                    if (delay > 0) {
                        TimeUnit.MILLISECONDS.sleep(delay);
                    }
                } catch (Exception ignored) {
                    logger.error("Send message fail", ignored);
                }
            });
            return null;
        }
    }
}
