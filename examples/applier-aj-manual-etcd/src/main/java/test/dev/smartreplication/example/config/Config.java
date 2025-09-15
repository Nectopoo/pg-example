package test.dev.smartreplication.example.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import test.dev.smartreplication.example.service.ExampleChangeHandler;
import test.dev.smartreplication.client.ChangeOrder;
import test.dev.smartreplication.client.SmartReplicationApplierDataSource;
import test.dev.smartreplication.client.SmartReplicationConfiguration;
import test.dev.smartreplication.client.kafka.ChangeApplierEndpointConfiguration;
import test.dev.smartreplication.client.kafka.ChangeApplierKafkaConfiguration;
import test.dev.smartreplication.client.kafka.SmartReplicationChangeApplierManager;
import test.dev.smartreplication.client.producer.DistributedChangeSourceResolver;
import test.dev.smartreplication.client.producer.WatcherChangeSourceResolver;
import test.dev.smartreplication.configuration.IConfigurationManagerPrivate;
import test.dev.smartreplication.core.compress.ChangeCompressor;
import test.dev.smartreplication.core.compress.JsonConverter;
import test.dev.smartreplication.core.kafka.EndpointManager;
import test.dev.smartreplication.core.kafka.config.KafkaConfiguration;

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
            endpointConfiguration,
            new ChangeCompressor(new JsonConverter()),
            createJsonObjectMapper());
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
                new WatcherChangeSourceResolver(new DistributedChangeSourceResolver(OWNER_ID, configurationManager))
            )
        );
    }

    private ObjectMapper createJsonObjectMapper() {
        return JsonMapper.builder()
                .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
                .configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true)
                .build();
    }
}
