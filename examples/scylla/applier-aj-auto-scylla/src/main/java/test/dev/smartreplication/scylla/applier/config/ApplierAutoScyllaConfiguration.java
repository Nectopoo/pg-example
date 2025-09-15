package test.dev.smartreplication.scylla.applier.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import test.dev.smartreplication.core.kafka.ISO8601ObjectMapperFactory;
import test.dev.smartreplication.core.kafka.config.KafkaConsumerConfig;
import test.dev.smartreplication.scylla.ApplierCqlSessionConfiguration;
import test.dev.smartreplication.scylla.ChangeHandlerConfiguration;
import test.dev.smartreplication.scylla.applier.properties.ApplierProperties;
import test.dev.smartreplication.client.*;
import test.dev.smartreplication.client.kafka.ChangeApplierEndpointConfiguration;
import test.dev.smartreplication.client.kafka.ChangeApplierKafkaConfiguration;
import test.dev.smartreplication.client.kafka.consumer.SmartReplicationChangeApplierManager;
import test.dev.smartreplication.configuration.IConfigurationManagerPrivate;
import test.dev.smartreplication.core.kafka.consumer.ConsumerChangeHandler;
import test.dev.smartreplication.scylla.ChangeHandler;

import java.net.InetSocketAddress;

@Configuration
@EnableConfigurationProperties(ApplierProperties.class)
@RequiredArgsConstructor
public class ApplierAutoScyllaConfiguration {

    private final ApplierProperties conf;

    @Bean(destroyMethod = "shutdown")
    public ConsumerChangeHandler scyllaChangeHandler() {
        return new ChangeHandler(
            ChangeHandlerConfiguration.builder()
                .mainSessionConfig(
                        ApplierCqlSessionConfiguration.builder()
                                .contactPoint(
                                    InetSocketAddress.createUnresolved(
                                        conf.getMainScylla().getHost(),
                                        conf.getMainScylla().getPort())
                                )
                                .localDatacenter(conf.getMainScylla().getDatacenter())
                                .build()
                )
                .standInSessionConfig(
                        ApplierCqlSessionConfiguration.builder()
                                .contactPoint(
                                    InetSocketAddress.createUnresolved(
                                        conf.getStandInScylla().getHost(),
                                        conf.getStandInScylla().getPort())
                                )
                                .localDatacenter(conf.getStandInScylla().getDatacenter())
                                .build()
                )
                .serviceCode(conf.getServiceCode())
                .keyspaces(conf.getScyllaKeyspaces())
                .build()

        );
    }

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(conf.getConsumerConcurrency() * 2 + 1);
        threadPoolTaskScheduler.setThreadNamePrefix("applier-consumer-monitoring");
        threadPoolTaskScheduler.initialize();
        return threadPoolTaskScheduler;
    }

    @Bean(initMethod = "init")
    public SmartReplicationChangeApplierManager changeApplierManager(
        ConsumerChangeHandler consumerChangeHandler,
        IConfigurationManagerPrivate configurationManager,
        @Qualifier("kafkaConfig") KafkaConsumerConfig consumerConfig,
        ThreadPoolTaskScheduler threadPoolTaskScheduler
    ) {
        ObjectMapper objectMapper = new ISO8601ObjectMapperFactory().create();
        return new SmartReplicationChangeApplierManager(
            new ChangeApplierKafkaConfiguration(
                consumerConfig, conf.getConsumerConcurrency()),
            consumerChangeHandler,
            configurationManager,
            new ChangeApplierEndpointConfiguration(
                conf.getServiceCode(),
                conf.getEndpoint(),
                ChangeOrder.REORDERING),
            threadPoolTaskScheduler,
            objectMapper);
    }
}
