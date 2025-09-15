package test.dev.smartreplication.example.config;

import com.datastax.oss.driver.api.core.CqlSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import test.dev.smartreplication.cassandra.proxy.ReconnectWhenNullStrategy;
import test.dev.smartreplication.client.ChangeHandler;
import test.dev.smartreplication.client.ReplicationChangeFlow;
import test.dev.smartreplication.client.SmartReplicationConfiguration;
import test.dev.smartreplication.client.SmartReplicationConfigurationBuilder;
import test.dev.smartreplication.client.kafka.producer.KafkaChangeHandler;
import test.dev.smartreplication.client.kafka.producer.KafkaProducerHolder;
import test.dev.smartreplication.client.producer.AbstractCachedResolver;
import test.dev.smartreplication.client.producer.etcd.EtcdChangeSourceResolver;
import test.dev.smartreplication.client.producer.handler.EmergencyFailureHandler;
import test.dev.smartreplication.client.producer.handler.EmptyEmergencyFailureHandler;
import test.dev.smartreplication.configuration.IConfigurationManagerPrivate;
import test.dev.smartreplication.configuration.etcd.EtcdConfigurationManager;
import test.dev.smartreplication.core.compress.ChangeCompressor;
import test.dev.smartreplication.core.compress.DefaultCompressFactory;
import test.dev.smartreplication.core.compress.JsonConverter;
import test.dev.smartreplication.core.kafka.ISO8601ObjectMapperFactory;
import test.dev.smartreplication.core.kafka.config.KafkaProducerConfig;
import test.dev.smartreplication.core.kafka.producer.OwnerKafkaProducerTopicResolver;
import test.dev.smartreplication.core.provider.ChangeProvider;
import test.dev.smartreplication.core.provider.ThreadLocalChangeProvider;
import test.dev.smartreplication.example.properties.SmartReplicationProperties;
import test.dev.smartreplication.model.Change;
import test.dev.smartreplication.model.CompressType;

import java.net.InetSocketAddress;

@Configuration
@EnableConfigurationProperties(SmartReplicationProperties.class)
@Slf4j
public class Config {

    private final SmartReplicationProperties conf;

    public Config(final SmartReplicationProperties conf) {
        this.conf = conf;
    }

    @Bean
    public ChangeProvider changeProvider() {
        return new ThreadLocalChangeProvider();
    }

    /**
     * Получает состояние из etcd.
     */
    @Bean
    public AbstractCachedResolver resolver(EtcdConfigurationManager configurationManager) {
        return new EtcdChangeSourceResolver(conf.getServiceCode(), configurationManager);
    }

    /**
     * Отправляет сообщение в кафка.
     * При необходимости сжимает сообщение, разбивает.
     */
    @Bean
    public KafkaChangeHandler changeHandler(KafkaProducerConfig kafkaProducerConfig,
                                            EmergencyFailureHandler<Change> failureHandler) {
        final OwnerKafkaProducerTopicResolver resolver = new OwnerKafkaProducerTopicResolver();
        return new KafkaChangeHandler(
            resolver,
            new ChangeCompressor(
                CompressType.SNAPPY,
                new JsonConverter(new ISO8601ObjectMapperFactory()),
                1_000_000,
                1_000_000,
                new DefaultCompressFactory()
            ),
            new KafkaProducerHolder(kafkaProducerConfig) {
                @Override
                protected Producer<String, byte[]> producerFactoryMethod() {
                    return AsyncKafkaProducer.<String, byte[]>builder()
                        .producer(super.producerFactoryMethod())
                        .owner(conf.getServiceCode())
                        .failureHandler(failureHandler)
                        .build();
                }
            }
        );
        // для повышения производительности можно использовать return new AsyncKafkaHandler(); (отправляет в кафку асинхронно)
    }

    @Bean
    public EmergencyFailureHandler<Change> emergencyFailureHandler() {
        return new EmptyEmergencyFailureHandler<Change>();
    }

    @Bean
    public SmartReplicationConfiguration smartReplicationConfiguration(
        ChangeProvider changeProvider,
        AbstractCachedResolver resolver,
        ChangeHandler<Change> changeHandler,
        IConfigurationManagerPrivate configurationManager,
        EmergencyFailureHandler<Change> emergencyFailureHandler) {
        return new SmartReplicationConfigurationBuilder()
            .setOwner(conf.getServiceCode())
            .setChangeHandler(changeHandler)
            .setChangeProvider(changeProvider)
            .setConfigurationManager(
                configurationManager
            )
            .setChangeResolver(resolver)
            .setFlowInitializer(ReplicationChangeFlow::new)
            .setReplicationFailureHandler(
                emergencyFailureHandler
            )
            .build();
    }

    /**
     * Во время запросов к scylla автоматически формирует и отправляет сообщение в кафку.
     */
    @Bean(destroyMethod = "close", name = "proxyReplicationSession")
    public CqlSession proxyReplicationSession(SmartReplicationConfiguration smartReplicationConfiguration,
                                                  ChangeProvider changeProvider,
                                                  MetadataCache metadataCache,
                                                  SmartReplicationProperties properties) {
        return Sessions.createProxyReplicationCqlSession(
            smartReplicationConfiguration,
            changeProvider,
            metadataCache,
            new ReconnectWhenNullStrategy(),
            CqlSession.builder()
                .addContactPoint(InetSocketAddress.createUnresolved(properties.getMainScylla().getHost(), properties.getMainScylla().getPort()))
                .withLocalDatacenter(properties.getMainScylla().getDatacenter())
                .withKeyspace(properties.getMainScylla().getKeyspace()),
            CqlSession.builder()
                .addContactPoint(InetSocketAddress.createUnresolved(properties.getStandInScylla().getHost(), properties.getStandInScylla().getPort()))
                .withLocalDatacenter(properties.getStandInScylla().getDatacenter())
                .withKeyspace(properties.getStandInScylla().getKeyspace())
        );
    }

    @Bean
    public MetadataCache metadataCache() {
        return new MetadataCacheImpl();
    }

    @Bean
    public ApplicationRunner metadata(PlainSessionProvider plainSessionProvider,
                                      MetadataCache metadataCache,
                                      SmartReplicationProperties properties) {
        return args -> new MetadataImpl(plainSessionProvider, properties.getScyllaKeyspaces(), metadataCache).init();
    }

}
