package test.dev.smartreplication.example.config;

import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import test.dev.smartreplication.api.dto.ChangeSource;
import test.dev.smartreplication.api.dto.CompressType;
import test.dev.smartreplication.client.SmartReplicationConfiguration;
import test.dev.smartreplication.client.SmartReplicationDataSource;
import test.dev.smartreplication.client.kafka.KafkaChangeHandler;
import test.dev.smartreplication.configuration.IConfigurationManagerPrivate;
import test.dev.smartreplication.configuration.zookeeper.ZookeeperConfiguration;
import test.dev.smartreplication.configuration.zookeeper.ZookeeperConfigurationManager;
import test.dev.smartreplication.core.kafka.config.KafkaProducerConfig;
import test.dev.smartreplication.core.kafka.producer.OwnerKafkaProducerTopicResolver;
import test.dev.smartreplication.core.provider.ThreadLocalChangeProvider;

import javax.sql.DataSource;

@Configuration
public class Config {

    private static final Logger logger = LoggerFactory.getLogger(Config.class);

    public static final String OWNER_ID = "towner";
    public static final String ZOOKEEPER_CONNECTION = "localhost:2181";

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
    public ThreadLocalChangeProvider changeProvider() {
        return new ThreadLocalChangeProvider();
    }

    @Bean
    public SmartReplicationConfiguration smartReplicationConfiguration(
        IConfigurationManagerPrivate configurationManager
    ) {
        return new SmartReplicationConfiguration(
                OWNER_ID,
                configurationManager,
                new KafkaChangeHandler(producerConfig(), new OwnerKafkaProducerTopicResolver(),
                        //настройки компрессии (сообщения больше млн байт будут компрессироваться)
                        CompressType.SNAPPY, 1_000_000),
                changeProvider()
        );
    }


    @Bean
    public DataSource smartReplicationDataSource(
        SmartReplicationConfiguration configuration
    ) {
        return new SmartReplicationDataSource(
                mainDataSource(),
                standInDataSource(),
                configuration
        );
    }

    @Bean
    public IConfigurationManagerPrivate configurationManager() {
        return new ZookeeperConfigurationManager(new ZookeeperConfiguration(ZOOKEEPER_CONNECTION));
    }

    @Bean
    public static KafkaProducerConfig producerConfig() {
        KafkaProducerConfig producerConfig = new KafkaProducerConfig("localhost:9092");
        /*
          Заведение настроек для подключения по SSL
        */
        return producerConfig;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(
        SmartReplicationConfiguration configuration
    ) {
        return new JdbcTemplate(smartReplicationDataSource(configuration));
    }

    @Bean
    public PlatformTransactionManager transactionManager(
        SmartReplicationConfiguration configuration
    ) {
        return new DataSourceTransactionManager(smartReplicationDataSource(configuration));
    }

    /**
     * Инициализация первоначального состояния в зукипере.
     * При штатной работе, это действие выполняется через админ консоль при создании оунера.
     */
    public static void initializeZookeeperNode() {
        try {
            final ZookeeperConfigurationManager zookeeperManager =
                new ZookeeperConfigurationManager(new ZookeeperConfiguration(ZOOKEEPER_CONNECTION));
            zookeeperManager.start();
            try {
                zookeeperManager.createOwner(OWNER_ID, ChangeSource.MAIN);
            } finally {
                zookeeperManager.stop();
            }
        } catch (Exception e) {
            logger.warn("zookeeper set state exception {}", e.getMessage());
        }
    }
}
