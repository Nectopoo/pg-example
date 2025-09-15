package test.dev.smartreplication.example.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.etcd.jetcd.Client;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import test.dev.smartreplication.api.dto.CompressType;
import test.dev.smartreplication.client.SmartReplicationConfiguration;
import test.dev.smartreplication.client.SmartTrxOutboxDataSource;
import test.dev.smartreplication.client.TrxOutboxChangeFlow;
import test.dev.smartreplication.client.kafka.KafkaChangeHandler;
import test.dev.smartreplication.configuration.IConfigurationManagerPrivate;
import test.dev.smartreplication.configuration.etcd.EtcdClientBuilderConfigurerComposite;
import test.dev.smartreplication.configuration.etcd.EtcdClientBuilderConfigurerWrapper;
import test.dev.smartreplication.configuration.etcd.EtcdMarker;
import test.dev.smartreplication.core.kafka.config.KafkaProducerConfig;
import test.dev.smartreplication.core.kafka.producer.OwnerKafkaProducerTopicResolver;
import test.dev.smartreplication.core.provider.ThreadLocalChangeProvider;
import test.dev.smartreplication.dao.TrxOutboxMessageDao;
import test.dev.smartreplication.dao.TrxOutboxMessageDaoImpl;
import test.dev.smartreplication.initializer.TrxOutboxMessageInitializer;
import test.dev.smartreplication.job.TrxOutboxMessageJob;
import test.dev.smartreplication.lock.EtcdDistributedLock;
import test.dev.smartreplication.service.ITrxOutboxChangeSaver;
import test.dev.smartreplication.service.TrxOutboxChangeSaver;

import javax.sql.DataSource;
import java.util.List;

@Configuration
public class Config {

    public static final String OWNER_ID = "towner";

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
    public TrxOutboxMessageInitializer trxOutboxMessageInitializer() {
        return new TrxOutboxMessageInitializer(mainDataSource(), standInDataSource());
    }

    @Bean
    public TrxOutboxMessageDao trxOutboxMessageDao(DataSource smartReplicationDataSource) {
        return new TrxOutboxMessageDaoImpl(smartReplicationDataSource);
    }

    @Bean
    public Client lockEtcdClient(final List<EtcdClientBuilderConfigurerWrapper<EtcdMarker>> etcdConfigurerWrappers) {
        final var composite = new EtcdClientBuilderConfigurerComposite();
        composite.addConfigurers(etcdConfigurerWrappers);

        final var builder = Client.builder();
        composite.configure(builder);
        return builder.build();
    }

    @Bean
    public TrxOutboxMessageJob trxOutboxMessageJob(TrxOutboxMessageDao trxOutboxMessageDao,
                                                   Client lockEtcdClient) {
        return new TrxOutboxMessageJob(trxOutboxMessageDao,
                new KafkaChangeHandler(producerConfig(), new OwnerKafkaProducerTopicResolver(),
                        //настройки компрессии (сообщения больше млн байт будут компрессироваться)
                        CompressType.SNAPPY, 1_000_000),
                // Синхронизация
                new EtcdDistributedLock(lockEtcdClient, "application-app-key", 30, 1)
                , new ObjectMapper()
        );
    }

    @Bean
    public ITrxOutboxChangeSaver trxOutboxChangeSaver() {
        return new TrxOutboxChangeSaver(new ObjectMapper());
    }

    @Bean
    public SmartReplicationConfiguration smartReplicationConfiguration(
            IConfigurationManagerPrivate configurationManager
    ) {
        return new SmartReplicationConfiguration(
                OWNER_ID,
                configurationManager,
                changeProvider(),
                trxOutboxChangeSaver()
        );
    }


    @Bean
    public DataSource smartReplicationDataSource(
            SmartReplicationConfiguration configuration
    ) {
        return new SmartTrxOutboxDataSource(
                mainDataSource(),
                standInDataSource(),
                configuration,
                TrxOutboxChangeFlow::new
        );
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
}
