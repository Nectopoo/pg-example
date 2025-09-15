package test.dev.smartreplication.example.config;

import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import test.dev.smartreplication.client.SmartReplicationConfiguration;
import test.dev.smartreplication.client.SmartReplicationDataSource;
import test.dev.smartreplication.client.kafka.KafkaChangeHandler;
import test.dev.smartreplication.client.producer.AbstractCachedResolver;
import test.dev.smartreplication.client.producer.DistributedChangeSourceResolver;
import test.dev.smartreplication.client.producer.EnableReplicationResolver;
import test.dev.smartreplication.client.producer.WatcherChangeSourceResolver;
import test.dev.smartreplication.client.producer.handler.CustomReplicationPropertiesProvider;
import test.dev.smartreplication.client.producer.handler.DefaultEmergencyFailureHandler;
import test.dev.smartreplication.client.producer.handler.EmergencyFailureHandler;
import test.dev.smartreplication.configuration.IConfigurationManagerPrivate;
import test.dev.smartreplication.core.exception.SmartReplicationException;
import test.dev.smartreplication.core.kafka.config.KafkaProducerConfig;
import test.dev.smartreplication.core.kafka.producer.OwnerKafkaProducerTopicResolver;
import test.dev.smartreplication.core.provider.ThreadLocalChangeProvider;
import test.dev.smartreplication.model.Change;
import test.dev.smartreplication.model.ChangeSource;
import test.dev.smartreplication.model.CompressType;

import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@EnableConfigurationProperties(SmartReplicationProperties.class)
public class Config {

    public static final String OWNER_ID = "towner";

    private final SmartReplicationProperties conf;

    public Config(SmartReplicationProperties conf) {
        this.conf = conf;
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
    public ThreadLocalChangeProvider changeProvider() {
        return new ThreadLocalChangeProvider();
    }

    @Bean
    public AbstractCachedResolver changeSourceResolver(IConfigurationManagerPrivate configManager) {
        return new WatcherChangeSourceResolver(new DistributedChangeSourceResolver(OWNER_ID, configManager));
    }

    @Bean
    public SmartReplicationConfiguration smartReplicationConfiguration(final AbstractCachedResolver changeSourceResolver,
                                                                       final EmergencyFailureHandler<Change> emergencyFailureHandler) {
        return new SmartReplicationConfiguration(
                OWNER_ID,
                null,
                new KafkaChangeHandler(producerConfig(), new OwnerKafkaProducerTopicResolver(),
                        //настройки компрессии (сообщения больше млн байт будут компрессироваться)
                        CompressType.SNAPPY, 1_000_000),
                changeProvider(),
            changeSourceResolver,
            () -> false, null, null, null, emergencyFailureHandler
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
     * EmptyEmergencyFailureHandler - обработчик, который не производит никаких действий, просто повторно пробрасывает исключение,
     * останавливая выполнение бизнес-операции при возникновении аварийных ситуаций при репликации данных.
     * Используется по умолчанию.
     * <p/>
     * DefaultEmergencyFailureHandler - обработчик выбрасывает исключение при ошибках KAFKA, выключает репликацию данных
     * при достижении одного из заданных таргетов - maxReplicationErrors или errorPeriodInSeconds.
     * При ошибке Etcd повторно пробрасывает исключение.
     * <p/>
     * MyEmergencyFailureHandler(CustomReplicationPropertiesProvider) - обработчик выбрасывает исключение при ошибках KAFKA,
     * выключает репликацию данных при достижении одного из заданных таргетов - maxReplicationErrors или errorPeriodInSeconds.
     * При ошибках Etcd использует методы CustomReplicationPropertiesProvider.
     *
     */
    @Bean
    public EmergencyFailureHandler<Change> emergencyFailureHandler(EnableReplicationResolver enableReplicationResolver,
                                                                   CustomReplicationPropertiesProvider customReplicationPropertiesProvider) {
//        return new EmptyEmergencyFailureHandler<>();
//        return new DefaultEmergencyFailureHandler<>(enableReplicationResolver, customReplicationPropertiesProvider,
//                conf.getMaxReplicationErrors(), conf.getErrorPeriodInSeconds());
        return new MyEmergencyFailureHandler(enableReplicationResolver, customReplicationPropertiesProvider,
            conf.getMaxReplicationErrors(), conf.getErrorPeriodInSeconds());
    }

    private class MyEmergencyFailureHandler extends DefaultEmergencyFailureHandler<Change> {
        private static final int DEFAULT_MAX_ETCD_ERRORS = 2;

        private final AtomicInteger etcdErrorsCounter = new AtomicInteger(0);

        public MyEmergencyFailureHandler(EnableReplicationResolver enableReplicationResolver,
                                         CustomReplicationPropertiesProvider customReplicationPropertiesProvider,
                                         int maxReplicationErrors, int errorPeriodInSeconds) {
            super(enableReplicationResolver, customReplicationPropertiesProvider, maxReplicationErrors, errorPeriodInSeconds);
        }

        @Override
        protected void handleEtcdGetChangeSourceError(SmartReplicationException smartReplicationException) {
            int etcdErrors = this.etcdErrorsCounter.addAndGet(1);
            if (etcdErrors < DEFAULT_MAX_ETCD_ERRORS) {
                throw smartReplicationException;
            } else {
                // Выключаем репликацию с помощью флага СУБО
                getCustomReplicationPropertiesProvider().setReplicationEnabled(false);
            }
        }

        @Override
        public void handleSuccess(Change change) {
            super.handleSuccess(change);
            this.etcdErrorsCounter.set(0);
        }
    }

    @Bean
    public CustomReplicationPropertiesProvider customReplicationPropertiesProvider() {
//        return new EmptyCustomReplicationPropertiesProvider();
        return new MyCustomReplicationPropertiesProvider();
    }

    /**
     * Пример реализации своего провайдера для методов получения и установления флага репликации данных
     * и получения значения текущего источника данных на стороне СУБО для случая недоступности Etcd.
     */
    private class MyCustomReplicationPropertiesProvider implements CustomReplicationPropertiesProvider {

        @Override
        public boolean isReplicationEnabled() {
            return conf.isReplicationEnabled();
        }

        @Override
        public void setReplicationEnabled(boolean isReplicationEnabled) {
            conf.setReplicationEnabled(isReplicationEnabled);
        }

        @Override
        public ChangeSource getDefaultChangeSource() {
            return ChangeSource.MAIN;
        }
    }
}
