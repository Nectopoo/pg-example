package test.dev.demo.business.application.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import test.dev.smartreplication.cache.CacheAdapter;
import test.dev.smartreplication.client.ChangeOrder;
import test.dev.smartreplication.client.SmartReplicationApplierDataSource;
import test.dev.smartreplication.client.SmartReplicationConfiguration;
import test.dev.smartreplication.client.kafka.ChangeApplierEndpointConfiguration;
import test.dev.smartreplication.client.kafka.ChangeApplierKafkaConfiguration;
import test.dev.smartreplication.client.kafka.consumer.SmartReplicationChangeApplierManager;
import test.dev.smartreplication.configuration.IConfigurationManagerPrivate;
import test.dev.smartreplication.core.kafka.RFC3339DateFormat;
import test.dev.smartreplication.core.kafka.config.KafkaConfiguration;
import test.dev.smartreplication.core.kafka.consumer.ChangeHandleResult;
import test.dev.smartreplication.handler.ReplicationChangeHandler;
import test.dev.smartreplication.jdbc.DatabaseSchemaCrawler;
import test.dev.smartreplication.jdbc.JdbcModifier;
import test.dev.smartreplication.jdbc.PostgresTypeConversion;
import test.dev.smartreplication.model.Change;
import test.dev.smartreplication.version.IdempotenceStrategy;

import javax.sql.DataSource;
import java.sql.Connection;

@Configuration
@ConditionalOnProperty(value = "demo-app.auto-applier", matchIfMissing = true, havingValue = "true")
public class AutoApplierConfiguration {
    private final DemoBusinessConfigurationProperties conf;
    private static final Logger logger = LoggerFactory.getLogger(AutoApplierConfiguration.class);

    public AutoApplierConfiguration(final DemoBusinessConfigurationProperties conf) {
        this.conf = conf;
    }

    @Bean
    public ReplicationChangeHandler replicationChangeHandler(
        final DataSource mainDataSource,
        final DataSource secondaryDataSource,
        final SmartReplicationConfiguration smartReplicationConfiguration) {
        final var applierDataSource = new SmartReplicationApplierDataSource(
            mainDataSource,
            secondaryDataSource,
            smartReplicationConfiguration);

        return new ReplicationChangeHandler(
            new CacheAdapter(new DatabaseSchemaCrawler(applierDataSource)),
            new IdempotenceStrategy<>() {
                @Override
                public boolean check(final Change change) {
                    logger.warn("Checking change {} for idempotence", change);
                    return true;
                }

                @Override
                public ChangeHandleResult apply(final Change change) {
                    logger.warn("Returning success for Change {}", change);
                    return ChangeHandleResult.success();
                }
            },
            () -> new JdbcModifier(
                applierDataSource,
                Connection.TRANSACTION_READ_COMMITTED,
                new PostgresTypeConversion(RFC3339DateFormat::new))
        );
    }

    @Bean(initMethod = "init")
    public SmartReplicationChangeApplierManager changeApplierManager(
        final ReplicationChangeHandler replicationChangeHandler,
        final IConfigurationManagerPrivate configurationManager,
        KafkaConfiguration kafkaConfiguration) {

        return new SmartReplicationChangeApplierManager(
            new ChangeApplierKafkaConfiguration(kafkaConfiguration),
            replicationChangeHandler,
            configurationManager,
            new ChangeApplierEndpointConfiguration(conf.getOwner(), conf.getEndpoint(),
                ChangeOrder.REORDERING)
        );
    }
}
