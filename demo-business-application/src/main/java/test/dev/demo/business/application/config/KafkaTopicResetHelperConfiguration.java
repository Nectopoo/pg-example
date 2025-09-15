package test.dev.demo.business.application.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.RoundRobinAssignor;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.retry.policy.AlwaysRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import test.dev.smartreplication.core.kafka.EndpointManager;
import test.dev.smartreplication.core.kafka.config.KafkaConfiguration;
import test.dev.smartreplication.model.Change;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

//@Configuration
// TODO: 24.12.2024 IMarkivskiy Выключил эту конфигурацию так как она подписывается на ВСЕ ТОПИКИ и мешает апплайеру!
// Необходимо разобраться и либо документировать для чего он нужен или убрать за ненадобностью.
@Deprecated
public class KafkaTopicResetHelperConfiguration {
    private final DemoBusinessConfigurationProperties conf;
    private static final Logger logger = LoggerFactory.getLogger(KafkaTopicResetHelperConfiguration.class);

    public KafkaTopicResetHelperConfiguration(final DemoBusinessConfigurationProperties conf) {
        this.conf = conf;
    }

    @Bean(initMethod = "start")
    public ConcurrentMessageListenerContainer<String, Change> concurrentMessageListenerContainer() {

        final var retryTemplate = new RetryTemplate();
        final var retryPolicy = new AlwaysRetryPolicy();
        retryTemplate.setRetryPolicy(retryPolicy);

        final var containerProperties = new ContainerProperties(Pattern.compile(".*"));
        containerProperties.setMessageListener(new SmartReplicationMessageListener());
        containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        final var consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProperties(),
            new StringDeserializer(),
            new JsonDeserializer<>(Change.class));

        return new ConcurrentMessageListenerContainer<>(consumerFactory, containerProperties);
    }

    private Map<String, Object> consumerProperties() {
        Map<String, Object> props = new HashMap<>();
        var config = transform(conf);
        props.put("bootstrap.servers", conf.getKafka().getBootstrapServers());
        props.put("key.deserializer", StringDeserializer.class);
        props.put("value.deserializer", ErrorHandlingDeserializer.class);
        props.put("enable.auto.commit", false);
        props.put("auto.offset.reset", "latest");
        props.put("partition.assignment.strategy", RoundRobinAssignor.class.getName());
        props.put("group.id", EndpointManager.generateEndpointName(conf.getOwner(), conf.getEndpoint()));
        props.putAll(config.getSslConfiguration());

        return props;
    }

    private static class SmartReplicationMessageListener implements AcknowledgingMessageListener<String, Change> {
        @Override
        public void onMessage(final ConsumerRecord<String, Change> data, final Acknowledgment acknowledgment) {
            logger.warn("Ack: {}", data);
            acknowledgment.acknowledge();
        }
    }

    private static KafkaConfiguration transform(DemoBusinessConfigurationProperties config) {
        KafkaConfiguration kafkaConfiguration = new KafkaConfiguration(config.getKafka().getBootstrapServers());
        if (config.getKafka().getSecurity().isEnable()) {
            kafkaConfiguration.getSslConfiguration().putAll(config.getKafka().getSecurity().toMap());
        }
        return kafkaConfiguration;
    }
}
