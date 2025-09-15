package test.dev.smartreplication.example.config;

import io.r2dbc.proxy.ProxyConnectionFactory;
import io.r2dbc.proxy.callback.ProxyConfig;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.reactive.TransactionalOperator;
import test.dev.smartreplication.client.producer.AbstractCachedResolver;
import test.dev.smartreplication.client.producer.DistributedChangeSourceResolver;
import test.dev.smartreplication.client.producer.EnableReplicationResolver;
import test.dev.smartreplication.client.producer.WatcherChangeSourceResolver;
import test.dev.smartreplication.client.producer.handler.CustomReplicationPropertiesProvider;
import test.dev.smartreplication.client.producer.handler.DefaultEmergencyFailureHandler;
import test.dev.smartreplication.client.producer.handler.EmergencyFailureHandler;
import test.dev.smartreplication.client.producer.handler.EmptyCustomReplicationPropertiesProvider;
import test.dev.smartreplication.client.producer.handler.EmptyEmergencyFailureHandler;
import test.dev.smartreplication.client.producer.handler.ReplicationFailureType;
import test.dev.smartreplication.client.r2dbc.SmartReplicationConnectionFactory;
import test.dev.smartreplication.configuration.IConfigurationManagerPrivate;
import test.dev.smartreplication.core.compress.ChangeCompressor;
import test.dev.smartreplication.core.compress.DefaultCompressFactory;
import test.dev.smartreplication.core.compress.JsonConverter;
import test.dev.smartreplication.core.exception.SmartReplicationException;
import test.dev.smartreplication.core.kafka.ISO8601ObjectMapperFactory;
import test.dev.smartreplication.core.kafka.config.KafkaProducerConfig;
import test.dev.smartreplication.core.kafka.producer.OwnerKafkaProducerTopicResolver;
import test.dev.smartreplication.example.properties.SmartReplicationProperties;
import test.dev.smartreplication.model.Change;
import test.dev.smartreplication.model.ChangeSource;
import test.dev.smartreplication.model.CompressType;
import test.dev.smartreplication.model.ProviderType;
import test.dev.streaming.config.AdapterConfig;
import test.dev.streaming.config.AllowingQueriesPredicate;
import test.dev.streaming.config.InterceptTablesPredicate;
import test.dev.streaming.handler.IEventHandler;
import test.dev.streaming.handler.StreamingHandler;
import test.dev.streaming.producer.KafkaReactiveMessageSender;
import test.dev.streaming.r2dbc.core.R2dbcDatabaseCrawler;
import test.dev.streaming.r2dbc.core.SyncR2dbcInterceptor;
import test.dev.streaming.r2dbc.core.factory.IInterceptFactory;
import test.dev.streaming.r2dbc.core.factory.InterceptFactory;
import test.dev.streaming.r2dbc.core.serializer.R2dbcResultSetMapperFactory;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static io.r2dbc.spi.ConnectionFactoryOptions.DATABASE;
import static io.r2dbc.spi.ConnectionFactoryOptions.DRIVER;
import static io.r2dbc.spi.ConnectionFactoryOptions.HOST;
import static io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD;
import static io.r2dbc.spi.ConnectionFactoryOptions.PORT;
import static io.r2dbc.spi.ConnectionFactoryOptions.USER;

@Configuration
@EnableTransactionManagement
@EnableR2dbcRepositories(basePackages = "test.dev.smartreplication.example.repository")
@EnableConfigurationProperties(SmartReplicationProperties.class)
public class Config {

    protected static final int MAX_MESSAGE_SIZE = 1_000_000;

    private final SmartReplicationProperties conf;

    public Config(SmartReplicationProperties conf) {
        this.conf = conf;
    }

    /**
     * Используется для определения текущей источника данных.
     */
    @Bean
    public AbstractCachedResolver changeSourceResolver(
            IConfigurationManagerPrivate configManager,
            CustomReplicationPropertiesProvider customReplicationPropertiesProvider
    ) {
        return new WatcherChangeSourceResolver(new DistributedChangeSourceResolver(conf.getOwner(), configManager),
                customReplicationPropertiesProvider::getDefaultChangeSource,
                customReplicationPropertiesProvider::isReplicationEnabled);
    }

    /**
     * Основная БД.
     */
    @Bean
    public ConnectionFactory mainConnectionFactory() {
        return ConnectionFactories.get(ConnectionFactoryOptions.builder()
                .option(DRIVER, conf.getMainDatasource().getDriverClassName())
                .option(HOST, conf.getMainDatasource().getHost())
                .option(PORT, conf.getMainDatasource().getPort())
                .option(DATABASE, conf.getMainDatasource().getDatabase())
                .option(USER, conf.getMainDatasource().getUsername())
                .option(PASSWORD, conf.getMainDatasource().getPassword())
                .option(Option.valueOf("schema"), conf.getMainDatasource().getSchema())
                .build());
    }

    /**
     * Standin БД.
     */
    @Bean
    public ConnectionFactory standinConnectionFactory() {
        return ConnectionFactories.get(ConnectionFactoryOptions.builder()
                .option(DRIVER, conf.getStandInDatasource().getDriverClassName())
                .option(HOST, conf.getStandInDatasource().getHost())
                .option(PORT, conf.getStandInDatasource().getPort())
                .option(DATABASE, conf.getStandInDatasource().getDatabase())
                .option(USER, conf.getStandInDatasource().getUsername())
                .option(PASSWORD, conf.getStandInDatasource().getPassword())
                .option(Option.valueOf("schema"), conf.getStandInDatasource().getSchema())
                .build());
    }

    @Bean
    public CustomReplicationPropertiesProvider customReplicationPropertiesProvider() {
        return new EmptyCustomReplicationPropertiesProvider();
//        return new MyCustomReplicationPropertiesProvider();
    }

    /**
     * Умная фабрика принимающая решение с какой БД работаем.
     */
    @Bean
    public SmartReplicationConnectionFactory smartReplicationConnectionFactory(final AbstractCachedResolver changeSourceResolver,
                                                                               EmergencyFailureHandler<Change> emergencyFailureHandler) {
        return new SmartReplicationConnectionFactory(mainConnectionFactory(), standinConnectionFactory(), changeSourceResolver, conf.getOwner(),
                emergencyFailureHandler);
    }

    /**
     * Настройки подключения к кафке.
     */
    @Bean
    public KafkaProducerConfig kafkaProducerConfig() {
        final KafkaProducerConfig producerConfig = new KafkaProducerConfig(conf.getKafkaConnectionString());
        /*
      Заведение настроек для подключения по SSL
    */
//        Map<String,String> sslMapConfig = producerConfig.getSslConfiguration();
//        sslMapConfig.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, "SSL");
//        sslMapConfig.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, "/opt/kafka_ssl/kafka.client.truststore.pfx");
//        sslMapConfig.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "password");
//        sslMapConfig.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PKCS12");
//        sslMapConfig.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, "/opt/kafka_ssl/kafka.client.keystore.pfx");
//        sslMapConfig.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "password");
//        sslMapConfig.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12");
//        sslMapConfig.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "password");
        return producerConfig;
    }

    /**
     * Реактивный поставщик сообщений.
     */
    @Bean
    public KafkaReactiveMessageSender kafkaMessageSender(KafkaProducerConfig producerConfig,
                                                         EmergencyFailureHandler<Change> emergencyFailureHandler) {
        final var resolver = new OwnerKafkaProducerTopicResolver();

        return new KafkaReactiveMessageSender(producerConfig,
                conf::getOwner,
                new ISO8601ObjectMapperFactory(),
                new ChangeCompressor(
                        CompressType.GZIP,
                        new JsonConverter(new R2dbcResultSetMapperFactory()),
                        MAX_MESSAGE_SIZE,
                        MAX_MESSAGE_SIZE,
                        new DefaultCompressFactory()
                ),
                resolver,
                emergencyFailureHandler
        );
    }

    /**
     * Проксируемая фабрика соединений для перехвата запросов к бд.
     * IInterceptFactory - Перехватчик создания соединений к бд.
     * AdapterConfig - содержит конфиг настроек для работы библиотеки.
     * IEventHandler - обработчик векторов изменений.
     */
    @Bean
    public ConnectionFactory proxyConnectionFactory(ConnectionFactory smartReplicationConnectionFactory,
                                                    AbstractCachedResolver changeSourceResolver,
                                                    KafkaReactiveMessageSender kafkaMessageSender,
                                                    EmergencyFailureHandler<Change> emergencyFailureHandler) {
        // TODO Все-таки наверно лучше инициализацию перевести куда нибудь в postConstruct AbstractCachedResolver и убрать ее из SmartReplicationConfiguration.
        try {
            changeSourceResolver.init();
        } catch (Exception e) {
            emergencyFailureHandler.handleException(ReplicationFailureType.ETCD_INITIALIZATION_ERROR,
                    new SmartReplicationException(e.getLocalizedMessage(), e));
        }
        IInterceptFactory factory = new InterceptFactory();

        // Заполнение кэша.
        new R2dbcDatabaseCrawler(smartReplicationConnectionFactory).metadata();

        AdapterConfig config = new AdapterConfig(true)
                .subo(conf::getOwner)
                .key(() -> UUID.randomUUID().toString())
                /*
                    Позволяет перехватывать только запросы INSERT/UPDATE/DELETE
                */
                .skipQueryPredicate(new AllowingQueriesPredicate())
                /*
                    Позволяет формировать вектор изменений только по нужным таблицам
                */
                .skipTablePredicate(new InterceptTablesPredicate<>(Set.of("users", "student")))
                /*
                    Определяет типы поддерживаемых потоков данных
                */
                .customProperties(HashMap::new)
                .changeSource(changeSourceResolver::resolveChangeSource)
                .provider(ProviderType.SMART_REPLICATION);

        IEventHandler<Change> handler = new StreamingHandler<>(kafkaMessageSender);

        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setProxyFactoryFactory(factory.createProxyFactoryFactory(config, emergencyFailureHandler));

        return ProxyConnectionFactory.builder(smartReplicationConnectionFactory, proxyConfig)
                .listener(new SyncR2dbcInterceptor(
                        smartReplicationConnectionFactory,
                        factory,
                        handler,
                        config,
                        changeSourceResolver
                ))
                .build();
    }

    @Bean
    DatabaseClient databaseClient(ConnectionFactory proxyConnectionFactory) {
        return DatabaseClient.builder()
                .connectionFactory(proxyConnectionFactory)
                .namedParameters(true)
                .build();
    }

    @Bean
    ReactiveTransactionManager transactionManager(ConnectionFactory proxyConnectionFactory) {
        return new R2dbcTransactionManager(proxyConnectionFactory);
    }

    @Bean
    TransactionalOperator transactionalOperator(ReactiveTransactionManager transactionManager) {
        return TransactionalOperator.create(transactionManager);
    }

    @Bean
    R2dbcEntityTemplate r2dbcEntityTemplate(DatabaseClient databaseClient) {
        return new R2dbcEntityTemplate(databaseClient, PostgresDialect.INSTANCE);
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
        return new DefaultEmergencyFailureHandler<>(enableReplicationResolver, conf.getMaxReplicationErrors(), conf.getErrorPeriodInSeconds());
//        return new MyEmergencyFailureHandler(enableReplicationResolver, customReplicationPropertiesProvider,
//                conf.getMaxReplicationErrors(), conf.getErrorPeriodInSeconds());
    }

    private class MyEmergencyFailureHandler extends DefaultEmergencyFailureHandler<Change> {
        private static final int DEFAULT_MAX_ETCD_ERRORS = 10;

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
            }
        }

        @Override
        public void handleSuccess(Change change) {
            super.handleSuccess(change);
            this.etcdErrorsCounter.set(0);
        }
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
