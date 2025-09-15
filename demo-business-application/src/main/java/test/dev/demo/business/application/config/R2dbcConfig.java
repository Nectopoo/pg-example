package test.dev.demo.business.application.config;

import io.r2dbc.proxy.ProxyConnectionFactory;
import io.r2dbc.proxy.callback.ProxyConfig;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;
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
import test.dev.smartreplication.client.kafka.producer.KafkaProducerHolder;
import test.dev.smartreplication.client.producer.AbstractCachedResolver;
import test.dev.smartreplication.client.producer.handler.EmergencyFailureHandler;
import test.dev.smartreplication.client.producer.handler.EmptyEmergencyFailureHandler;
import test.dev.smartreplication.client.producer.handler.ReplicationFailureType;
import test.dev.smartreplication.client.r2dbc.SmartReplicationConnectionFactory;
import test.dev.smartreplication.core.compress.ChangeCompressor;
import test.dev.smartreplication.core.compress.DefaultCompressFactory;
import test.dev.smartreplication.core.compress.JsonConverter;
import test.dev.smartreplication.core.exception.SmartReplicationException;
import test.dev.smartreplication.core.kafka.ISO8601ObjectMapperFactory;
import test.dev.smartreplication.core.kafka.config.KafkaProducerConfig;
import test.dev.smartreplication.core.kafka.producer.OwnerKafkaProducerTopicResolver;
import test.dev.smartreplication.model.Change;
import test.dev.smartreplication.model.CompressType;
import test.dev.smartreplication.model.ProviderType;
import test.dev.streaming.config.AdapterConfig;
import test.dev.streaming.config.AllowingQueriesPredicate;
import test.dev.streaming.config.InterceptTablesPredicate;
import test.dev.streaming.handler.IEventHandler;
import test.dev.streaming.handler.StreamingHandler;
import test.dev.smartreplication.client.kafka.producer.AbstractMessageSender;
import test.dev.smartreplication.client.kafka.producer.KafkaMessageSender;
import test.dev.streaming.r2dbc.core.R2dbcDatabaseCrawler;
import test.dev.streaming.r2dbc.core.SyncR2dbcInterceptor;
import test.dev.streaming.r2dbc.core.factory.IInterceptFactory;
import test.dev.streaming.r2dbc.core.factory.InterceptFactory;
import test.dev.streaming.r2dbc.core.serializer.R2dbcResultSetMapperFactory;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import static io.r2dbc.spi.ConnectionFactoryOptions.DATABASE;
import static io.r2dbc.spi.ConnectionFactoryOptions.DRIVER;
import static io.r2dbc.spi.ConnectionFactoryOptions.HOST;
import static io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD;
import static io.r2dbc.spi.ConnectionFactoryOptions.PORT;
import static io.r2dbc.spi.ConnectionFactoryOptions.USER;

@Configuration
@EnableTransactionManagement
@EnableR2dbcRepositories(basePackages = "test.dev.demo.business.application.repository")
public class R2dbcConfig {

    protected static final int MAX_MESSAGE_SIZE = 1_000_000;

    private final DemoBusinessConfigurationProperties conf;

    public R2dbcConfig(DemoBusinessConfigurationProperties conf) {
        this.conf = conf;
    }

    /**
     * Основная БД.
     */
    @Bean
    public ConnectionFactory mainConnectionFactory() {
        return ConnectionFactories.get(ConnectionFactoryOptions.builder()
                .option(DRIVER, conf.getR2dbcPropsMain().getDriver())
                .option(HOST, conf.getR2dbcPropsMain().getHost())
                .option(PORT,  conf.getR2dbcPropsMain().getPort())
                .option(DATABASE,  conf.getR2dbcPropsMain().getDatabase())
                .option(USER, conf.getMainDataSource().getUsername())
                .option(PASSWORD, conf.getMainDataSource().getPassword())
                .option(Option.valueOf("schema"), conf.getDbSchema())
                .build());
    }

    /**
     * Standin БД.
     */
    @Bean
    public ConnectionFactory standinConnectionFactory() {
        return ConnectionFactories.get(ConnectionFactoryOptions.builder()
                .option(DRIVER, conf.getR2dbcPropsStandin().getDriver())
                .option(HOST, conf.getR2dbcPropsStandin().getHost())
                .option(PORT,  conf.getR2dbcPropsStandin().getPort())
                .option(DATABASE,  conf.getR2dbcPropsStandin().getDatabase())
                .option(USER, conf.getMainDataSource().getUsername())
                .option(PASSWORD, conf.getMainDataSource().getPassword())
                .option(Option.valueOf("schema"), conf.getDbSchema())
                .build());
    }

    /**
     * Умная фабрика принимающая решение с какой БД работаем.
     */
    @Bean
    public SmartReplicationConnectionFactory smartReplicationConnectionFactory(
            final AbstractCachedResolver changeSourceResolver
    ) {
        return new SmartReplicationConnectionFactory(mainConnectionFactory(), standinConnectionFactory(), changeSourceResolver, conf.getOwner(),
                new EmptyEmergencyFailureHandler<>());
    }

    /**
     * Реактивный поставщик сообщений.
     */
    @Bean
    public AbstractMessageSender<Change> kafkaMessageSender(KafkaProducerConfig producerConfig,
                                                            EmergencyFailureHandler<Change> emergencyFailureHandler) {
        final var resolver = new OwnerKafkaProducerTopicResolver();

        return new KafkaMessageSender(
                new KafkaProducerHolder(producerConfig),
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
                                                    AbstractMessageSender<Change> kafkaMessageSender,
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
        proxyConfig.setProxyFactoryFactory(factory.createProxyFactoryFactory(config));

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
    ReactiveTransactionManager transactionManagerReactive(ConnectionFactory proxyConnectionFactory) {
        return new R2dbcTransactionManager(proxyConnectionFactory);
    }

    @Bean
    TransactionalOperator transactionalOperator(ReactiveTransactionManager transactionManagerReactive) {
        return TransactionalOperator.create(transactionManagerReactive);
    }

    @Bean
    R2dbcEntityTemplate r2dbcEntityTemplate(DatabaseClient databaseClient) {
        return new R2dbcEntityTemplate(databaseClient, PostgresDialect.INSTANCE);
    }
}
