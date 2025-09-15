package test.dev.smartreplication.example.config;

import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import test.dev.smartreplication.client.ChangeOrder;
import test.dev.smartreplication.client.SmartReplicationApplierDataSource;
import test.dev.smartreplication.client.SmartReplicationConfiguration;
import test.dev.smartreplication.client.kafka.ChangeApplierEndpointConfiguration;
import test.dev.smartreplication.client.kafka.ChangeApplierKafkaConfiguration;
import test.dev.smartreplication.client.kafka.SmartReplicationChangeApplierManager;
import test.dev.smartreplication.client.producer.CachedChangeSourceResolver;
import test.dev.smartreplication.client.producer.DistributedChangeSourceResolver;
import test.dev.smartreplication.configuration.IConfigurationManagerPrivate;
import test.dev.smartreplication.configuration.zookeeper.ZookeeperConfiguration;
import test.dev.smartreplication.configuration.zookeeper.ZookeeperConfigurationManager;
import test.dev.smartreplication.core.kafka.EndpointManager;
import test.dev.smartreplication.core.kafka.config.KafkaConfiguration;
import test.dev.smartreplication.example.service.ExampleChangeHandler;

import javax.sql.DataSource;

@Configuration
public class Config {

    public static final String OWNER_ID = "towner";

    @Bean(initMethod = "init")
    public SmartReplicationChangeApplierManager kafkaChangeConsumerManager(
        ExampleChangeHandler exampleChangeHandler,
        IConfigurationManagerPrivate configurationManager
    ) {
        final ChangeApplierKafkaConfiguration kafkaConfiguration = new ChangeApplierKafkaConfiguration(
            kafkaConfiguration());
        ChangeApplierEndpointConfiguration endpointConfiguration = new ChangeApplierEndpointConfiguration(
            OWNER_ID, EndpointManager.STANDIN_ENDPOINT, ChangeOrder.STRICT_ORDER
        );
        return new SmartReplicationChangeApplierManager(
            kafkaConfiguration,
            exampleChangeHandler,
            configurationManager,
            endpointConfiguration);
    }

    @Bean
    public IConfigurationManagerPrivate configurationManager() {
        return new ZookeeperConfigurationManager(new ZookeeperConfiguration("localhost:2181"));

    }

    @Bean
    public KafkaConfiguration kafkaConfiguration() {
        /*
            Заведение настроек для подключения по SSL
        */
        return new KafkaConfiguration("localhost:9092");
    }

    /**
     * DataSource основной БД.
     */
    @Bean
    public DataSource mainDataSource() {
        final PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl("jdbc:postgresql://localhost:5432/postgres");
        dataSource.setUser("postgres");
        dataSource.setPassword("mysecretpassword");
        dataSource.setCurrentSchema("main");
        return dataSource;
    }

    /**
     * DataSource БД StandIn.
     */
    @Bean
    public DataSource standInDataSource() {
        final PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl("jdbc:postgresql://localhost:5432/postgres");
        dataSource.setUser("postgres");
        dataSource.setPassword("mysecretpassword");
        dataSource.setCurrentSchema("standin");
        return dataSource;
    }

    @Bean
    public SmartReplicationApplierDataSource applierDataSource(IConfigurationManagerPrivate configurationManager) {
        return new SmartReplicationApplierDataSource(
            mainDataSource(),
            standInDataSource(),
            new SmartReplicationConfiguration(
                OWNER_ID,
                null,
                null,
                new CachedChangeSourceResolver(
                    new DistributedChangeSourceResolver(OWNER_ID, configurationManager)
                )
            )
        );
    }
}
