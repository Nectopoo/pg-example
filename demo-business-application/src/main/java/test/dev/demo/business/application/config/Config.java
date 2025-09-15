package test.dev.demo.business.application.config;

import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.listener.lifecycle.JdbcLifecycleEventListener;
import net.ttddyy.dsproxy.listener.logging.CommonsLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.hibernate.cfg.AvailableSettings;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import test.dev.smartreplication.client.IChangeProducer;
import test.dev.smartreplication.client.IChangeSender;
import test.dev.smartreplication.client.IJdbcCycle;
import test.dev.smartreplication.client.ReplicationChangeSender;
import test.dev.smartreplication.client.SmartReplicationConfiguration;
import test.dev.smartreplication.client.SmartReplicationDataSource;
import test.dev.smartreplication.client.kafka.producer.KafkaChangeHandler;
import test.dev.smartreplication.client.kafka.producer.KafkaProducerHolder;
import test.dev.smartreplication.client.producer.AbstractCachedResolver;
import test.dev.smartreplication.client.producer.DistributedChangeSourceResolver;
import test.dev.smartreplication.client.producer.EnableReplicationResolver;
import test.dev.smartreplication.client.producer.ResolvingMode;
import test.dev.smartreplication.client.producer.WatcherChangeSourceResolver;
import test.dev.smartreplication.client.producer.handler.CustomReplicationPropertiesProvider;
import test.dev.smartreplication.client.producer.handler.DefaultEmergencyFailureHandler;
import test.dev.smartreplication.client.producer.handler.EmergencyFailureHandler;
import test.dev.smartreplication.client.producer.handler.EmptyCustomReplicationPropertiesProvider;
import test.dev.smartreplication.client.producer.handler.EmptyEmergencyFailureHandler;
import test.dev.smartreplication.configuration.IConfigurationManagerPrivate;
import test.dev.smartreplication.core.compress.ChangeCompressor;
import test.dev.smartreplication.core.compress.DefaultCompressFactory;
import test.dev.smartreplication.core.compress.JsonConverter;
import test.dev.smartreplication.core.exception.SmartReplicationException;
import test.dev.smartreplication.core.kafka.ISO8601ObjectMapperFactory;
import test.dev.smartreplication.core.kafka.config.KafkaConfiguration;
import test.dev.smartreplication.core.kafka.config.KafkaConsumerConfig;
import test.dev.smartreplication.core.kafka.config.KafkaProducerConfig;
import test.dev.smartreplication.core.kafka.producer.OwnerKafkaProducerTopicResolver;
import test.dev.smartreplication.core.provider.ChangeFormatType;
import test.dev.smartreplication.core.provider.ChangeProvider;
import test.dev.smartreplication.core.provider.ThreadLocalChangeProvider;
import test.dev.smartreplication.model.Change;
import test.dev.smartreplication.model.ChangeSource;
import test.dev.smartreplication.model.ProviderType;
import test.dev.streaming.adapter.flow.AutoChangeFlow;
import test.dev.streaming.adapter.jdbc.ChangeJdbcCycle;
import test.dev.streaming.adapter.producer.ChangeProducer;
import test.dev.streaming.config.AdapterConfig;
import test.dev.streaming.config.AllowingQueriesPredicate;
import test.dev.streaming.config.InterceptTablesPredicate;
import test.dev.streaming.core.ChangeReplicationModeProvider;
import test.dev.streaming.core.flow.IFlowProcessor;
import test.dev.streaming.handler.EmptyHandler;
import test.dev.streaming.handler.IEventHandler;
import test.dev.streaming.jdbc.SyncJdbcInterceptor;
import test.dev.streaming.key.KeyProvider;
import test.dev.streaming.mdc.ChangeKeySupplier;
import test.dev.streaming.mdc.MDCChangeReplicationModeProvider;
import test.dev.streaming.mdc.MDCKeySupplier;
import test.dev.streaming.mdc.MDCPropertiesSupplier;
import test.dev.streaming.parser.EnumCastMapper;
import test.dev.streaming.parser.ProxyQueryTransformer;
import test.dev.streaming.proxy.ExtendedResultSetProxyLogicFactory;
import test.dev.streaming.proxy.ReturnProxyFactory;
import test.dev.streaming.visitor.IVisitorFactory;
import test.dev.streaming.visitor.VisitorFactory;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@Slf4j
public class Config {
    private final DemoBusinessConfigurationProperties conf;

    public Config(final DemoBusinessConfigurationProperties conf) {
        this.conf = conf;
    }

    /**
     * Определяет хранилище, где накапливаются вектора изменений, до отправки в Kafka
     * <p>
     * ThreadLocalChangeProvider - обеспечивает потокобезопасность Builder-ам,
     * которые занимаются сборкой векторов в случае, если они передаются другим потокам
     * <p>
     * EnumChangeProvider - обеспечивает максимальную производительность,
     * однако при передачи Builder-ов в другие потоки не гарантируют конкурентного доступа.
     */
    @Bean
    @Primary
    public ChangeProvider changeProvider(ChangeReplicationModeProvider changeReplicationModeProvider,
                                         KeyProvider keyProvider) {
        return new ThreadLocalChangeProvider() {
            @Override
            protected void cleanContext() {
                changeReplicationModeProvider.clear();
                if (keyProvider != null) {
                    keyProvider.clear();
                }
            }
        };
    }

    /**
     * Определение основного DataSource приложения.
     */
    @Bean
    public DataSource mainDataSource() {
        final var dataSource = new PGSimpleDataSource();
        var props = conf.getMainDataSource();
        dataSource.setUrl(props.getUrl());
        dataSource.setUser(props.getUsername());
        dataSource.setPassword(props.getPassword());
        dataSource.setCurrentSchema(conf.getDbSchema());
        return dataSource;
    }

    /**
     * standin
     * Определение standin DataSource приложения.
     */
    @Bean
    public DataSource secondaryDataSource() {
        final var dataSource = new PGSimpleDataSource();
        var props = conf.getStandinDataSource();
        dataSource.setUrl(props.getUrl());
        dataSource.setUser(props.getUsername());
        dataSource.setPassword(props.getPassword());
        dataSource.setCurrentSchema(conf.getDbSchema());
        return dataSource;
    }

    @Bean
    public DataSourceHealthIndicator dataSourceHealthIndicatorA(DataSource smartReplicationDataSource) {
        return new DataSourceHealthIndicator(smartReplicationDataSource);
    }

    @Bean
    @ConfigurationProperties("demo-app.watcher")
    public WatcherResolverProperties watcherChangeSourceResolverProperties() {
        return new WatcherResolverProperties();
    }

    @Bean
    public AbstractCachedResolver changeSourceResolver(IConfigurationManagerPrivate configManager,
                                                       CustomReplicationPropertiesProvider customReplicationPropertiesProvider,
                                                       WatcherResolverProperties watcherResolverProperties) {

        DistributedChangeSourceResolver distributedChangeSourceResolver =
            new DistributedChangeSourceResolver(conf.getOwner(), configManager);

        return switch (watcherResolverProperties.getFallbackMode()) {
            case GET -> new WatcherChangeSourceResolver(distributedChangeSourceResolver,
                Duration.ofMillis(watcherResolverProperties.getCacheExpiredMs()));

            case CACHE -> new WatcherChangeSourceResolver(distributedChangeSourceResolver, ResolvingMode.CACHE);

            case CUSTOM -> new WatcherChangeSourceResolver(distributedChangeSourceResolver,
                customReplicationPropertiesProvider::getDefaultChangeSource,
                customReplicationPropertiesProvider::isReplicationEnabled);

            default -> throw new IllegalStateException("Fallback mode not defined.");
        };
    }

    @Bean
    public KeyProvider keyProvider() {
        if (conf.getChangeKeyMode().equals(ChangeKeyMode.CHANGE_KEY)) {
            return new ChangeKeySupplier();
        } else {
             return new MDCKeySupplier();
        }
    }
    
    @Bean
    public SmartReplicationConfiguration smartReplicationConfiguration(
        final KafkaChangeHandler kafkaChangeHandler,
        final ChangeProvider changeProvider,
        final AbstractCachedResolver changeSourceResolver,
        final EmergencyFailureHandler<Change> emergencyFailureHandler,
        final KeyProvider keyProvider) {
        return new SmartReplicationConfiguration(
            conf.getOwner(),
            keyProvider,
            kafkaChangeHandler,
            changeProvider,
            changeSourceResolver, 
            () -> false, null, null, null,
            emergencyFailureHandler
        );
    }

    /**
     * Проксирование DataSource для его репликации (Прикладной Журнал / Standin / Smart Replication).
     */
    @Bean
    public DataSource smartReplicationDataSource(
            @Qualifier("mainDataSource") final DataSource mainDataSource,
            @Qualifier("secondaryDataSource") final DataSource secondaryDataSource,
            final SmartReplicationConfiguration smartReplicationConfiguration
    ) {
        return new SmartReplicationDataSource(
                mainDataSource,
                secondaryDataSource,
                smartReplicationConfiguration
        );
    }

    /**
     * Настройки подключения к кафке.
     */
    @Bean
    public KafkaProducerConfig kafkaProducerConfig() {
        final KafkaProducerConfig producerConfig = new KafkaProducerConfig(conf.getKafka().getBootstrapServers());
        DemoBusinessConfigurationProperties.Security kafkaSecurity = conf.getKafka().getSecurity();
        /*
      Заведение настроек для подключения по SSL
        */
        if (kafkaSecurity.isEnable()) {
            Map<String, String> sslMapConfig = producerConfig.getSslConfiguration();
            sslMapConfig.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, kafkaSecurity.getProtocol());
            sslMapConfig.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, kafkaSecurity.getTrustStoreLocation());
            sslMapConfig.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, kafkaSecurity.getTrustStorePassword());
            sslMapConfig.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, kafkaSecurity.getTrustStoreType());
            sslMapConfig.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, kafkaSecurity.getKeyStoreLocation());
            sslMapConfig.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, kafkaSecurity.getKeyStorePassword());
            sslMapConfig.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, kafkaSecurity.getKeyStoreType());
            sslMapConfig.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, kafkaSecurity.getKeyPassword());
        }
        return producerConfig;
    }

    @Bean
    @Primary
    public KafkaChangeHandler kafkaChangeHandler(KafkaProducerConfig kafkaProducerConfig) {
        final var resolver = new OwnerKafkaProducerTopicResolver();
        final var compress = conf.getCompress();
        return new KafkaChangeHandler(
                resolver,
                new ChangeCompressor(
                        compress.getCompressType(),
                        new JsonConverter(new ISO8601ObjectMapperFactory()),
                        compress.getMinBytesToCompress(),
                        compress.getMinBytesForSplit(),
                        new DefaultCompressFactory()
                ),
                new KafkaProducerHolder(kafkaProducerConfig)
            );
    }

    @Bean
    public ChangeReplicationModeProvider changeReplicationModeProvider() {
        return new MDCChangeReplicationModeProvider();
    }

    @Bean
    public IChangeSender sender(
            SmartReplicationConfiguration smartReplicationConfiguration,
            IConfigurationManagerPrivate configurationManager
    ) {
        return new ReplicationChangeSender(
                smartReplicationConfiguration,
                configurationManager.getChangeSource(conf.getOwner())
        );
    }

    @Bean
    public IFlowProcessor<Change> flowProcessor(IVisitorFactory<Change> visitorFactory) {
        return visitorFactory.createProcessor();
    }

    @Bean
    public IJdbcCycle jdbcCycle(DataSource smartReplicationDataSource,
                                AdapterConfig adapterConfig,
                                IVisitorFactory<Change> visitorFactory,
                                IEventHandler<Change> handler,
                                IChangeSender sender,
                                ChangeProvider changeProvider) {
        return ChangeJdbcCycle.builder()
                .dataSource(smartReplicationDataSource)
                .config(adapterConfig)
                .factory(visitorFactory)
                .handler(handler)
                .sender(sender)
                .provider(changeProvider)
                .build();
    }

    @Bean
    public IChangeProducer<String, ExecutionInfo> producer(DataSource smartReplicationDataSource,
                                                           AdapterConfig adapterConfig,
                                                           IVisitorFactory<Change> visitorFactory,
                                                           ChangeProvider changeProvider) {
        return ChangeProducer.builder()
                .dataSource(smartReplicationDataSource)
                .config(adapterConfig)
                .factory(visitorFactory)
                .provider(changeProvider)
                .build();
    }

    @Bean
    public IVisitorFactory<Change> visitorFactory() {
        return new VisitorFactory();
    }

    @Bean
    public JdbcLifecycleEventListener syncLifeListener(DataSource smartReplicationDataSource,
                                                       AdapterConfig adapterConfig,
                                                       IChangeSender sender,
                                                       IChangeProducer<String, ExecutionInfo> producer,
                                                       IFlowProcessor<Change> flowProcessor,
                                                       IJdbcCycle jdbcCycle,
                                                       IVisitorFactory<Change> visitorFactory) {
        return new SyncJdbcInterceptor<>(
                AutoChangeFlow.builder()
                        .config(adapterConfig)
                        .dataSource(smartReplicationDataSource)
                        .sender(sender)
                        .producer(producer)
                        .jdbcCycle(jdbcCycle)
                        .processor(flowProcessor)
                        .build(),
                visitorFactory
        );
    }

    @Bean
    @Primary
    public AdapterConfig adapterConfig(ChangeReplicationModeProvider changeReplicationModeProvider) {
        return new AdapterConfig(true)
                .subo(conf::getOwner)
                .key(new MDCKeySupplier())
                .customProperties(new MDCPropertiesSupplier())
                .provider(ProviderType.SMART_REPLICATION)
                /*
                    Позволяет перехватывать только запросы INSERT/UPDATE/DELETE/SELECT nextval()
                */
                .skipQueryPredicate(new AllowingQueriesPredicate(){
                    @Override
                    public boolean test(String query) {
                        return query == null 
                            || query.length() <= this.getEndPosition()
                            || (!this.queryMasks.contains(query.substring(this.getStartPosition(), this.getEndPosition())) 
                                && !query.toLowerCase().startsWith("select nextval") 
                                && !query.toLowerCase().startsWith("call create_new_account"));
                    }
                })
                /*
                    Позволяет формировать вектор изменений только по нужным таблицам
                */
                .skipTablePredicate(new InterceptTablesPredicate<>(Set.of(
                        "account", "client", "exchange_rate", "transaction", "user_session"
                )))
                /*
                    CHANGES - отправляются только изменения
                    ENTITY - отправляются все поля сущности (даже те которые не фигурировали в запросе на изменения)
                */
                .changeFormatType(ChangeFormatType.CHANGES)
                .useContextConnection()
                .changeReplicationModeProvider(changeReplicationModeProvider);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(final DataSource proxyDataSource) {
        return new JdbcTemplate(proxyDataSource);
    }

    @Bean
    public IEventHandler<Change> proxyHandler() {
        return new EmptyHandler<>();
    }

    @Bean
    public DataSource proxyDataSource(
            final DataSource smartReplicationDataSource,
            final JdbcLifecycleEventListener syncLifeListener,
            final AdapterConfig adapterConfig) {
        return ProxyDataSourceBuilder.create(smartReplicationDataSource)
            .logQueryByCommons(CommonsLogLevel.INFO) // задается уровень логирования
            .autoRetrieveGeneratedKeys(false, new ExtendedResultSetProxyLogicFactory())// задать, если нужно перехватывать  работу с ResultSet для сгенерированных ключей
            .jdbcProxyFactory(new ReturnProxyFactory())// фабрика для создания jdbc объектов
//            .parameterTransformer(new ProxyParameterTransformer())// если необходимо настроить трансформацию параметров запроса
            .queryTransformer(//Для обработки SQL запросов перед выполнением
                new ProxyQueryTransformer(
                    smartReplicationDataSource,
                    adapterConfig,
                    new EnumCastMapper<>(
                            type -> EnumCastMapper.ENUM_CAST + "\"" + type.getSchema() + "\".\"" + type.getType() + "\"",
                            type -> EnumCastMapper.ENUM_CAST + type.getType()
                    )
                )
            )
            .listener(

                //В асинхронном режиме, данные в начале сохраняются в базу данных, затем в Kafka.
                //При синхронном, в начале в Kafka, затем в базу данных.

                syncLifeListener
            )
            .build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource proxyDataSource) {
        final var managerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        managerFactoryBean.setDataSource(proxyDataSource);
        managerFactoryBean.setPackagesToScan("test.dev.demo.business.application");

        final var vendorAdapter = new HibernateJpaVendorAdapter();
        managerFactoryBean.setJpaVendorAdapter(vendorAdapter);
        managerFactoryBean.setJpaProperties(additionalProperties());

        return managerFactoryBean;
    }

    private Properties additionalProperties() {
        final var properties = new Properties();
        properties.setProperty(AvailableSettings.HBM2DDL_AUTO, "none");
        properties.setProperty(AvailableSettings.DIALECT, "org.hibernate.dialect.PostgreSQLDialect");

        // иначе Hibernate бросит Exception
        // The database returned no natively generated identity value
        properties.setProperty("hibernate.jdbc.use_get_generated_keys", "false");
        return properties;
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        final var transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
        return transactionManager;
    }

    @Bean
    public AdminClient kafkaAdminClient(@Qualifier("kafkaConsumerConfig") KafkaConsumerConfig kafkaConsumerConfig) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConsumerConfig.getBootstrapServers());
        props.putAll(kafkaConsumerConfig.getSslConfiguration());

        return AdminClient.create(props);
    }

    @Bean("kafkaConsumerConfig")
    public KafkaConsumerConfig kafkaConsumerConfig() {
        final var consumerConfig = new KafkaConsumerConfig(conf.getKafka().getBootstrapServers());
        DemoBusinessConfigurationProperties.Security kafkaSecurity = conf.getKafka().getSecurity();
        if (kafkaSecurity.isEnable()) {
            Map<String, String> sslMapConfig = consumerConfig.getSslConfiguration();
            sslMapConfig.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, kafkaSecurity.getProtocol());
            sslMapConfig.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, kafkaSecurity.getTrustStoreLocation());
            sslMapConfig.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, kafkaSecurity.getTrustStorePassword());
            sslMapConfig.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, kafkaSecurity.getTrustStoreType());
            sslMapConfig.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, kafkaSecurity.getKeyStoreLocation());
            sslMapConfig.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, kafkaSecurity.getKeyStorePassword());
            sslMapConfig.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, kafkaSecurity.getKeyStoreType());
            sslMapConfig.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, kafkaSecurity.getKeyPassword());
        }
        return new KafkaConsumerConfig(consumerConfig);
    }

    @Bean
    public KafkaConfiguration kafkaConfiguration() {
        KafkaConfiguration kafkaConfiguration = new KafkaConfiguration(conf.getKafka().getBootstrapServers());
        if (conf.getKafka().getSecurity().isEnable()) {
            kafkaConfiguration.getSslConfiguration().putAll(conf.getKafka().getSecurity().toMap());
        }
        return kafkaConfiguration;
    }

    @Bean
    public CustomReplicationPropertiesProvider customReplicationPropertiesProvider() {
        DemoBusinessConfigurationProperties.FailureHandling failureHandling = conf.getFailureHandling();
        if (failureHandling == null || failureHandling.getMode() == null) {
            return new EmptyCustomReplicationPropertiesProvider();
        }

        switch (failureHandling.getMode()) {
            case EMPTY:
            case DEFAULT:
                return new EmptyCustomReplicationPropertiesProvider();
            case CUSTOM:
                return new MyCustomReplicationPropertiesProvider();
            default:
                throw new IllegalArgumentException("Not supported replication failure handling mode " + failureHandling.getMode());
        }
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
        DemoBusinessConfigurationProperties.FailureHandling failureHandling = conf.getFailureHandling();
        if (failureHandling == null || failureHandling.getMode() == null) {
            return new EmptyEmergencyFailureHandler<>();
        }

        switch (failureHandling.getMode()) {
            case EMPTY:
                return new EmptyEmergencyFailureHandler<>();
            case DEFAULT:
                return new DefaultEmergencyFailureHandler<>(enableReplicationResolver,
                        customReplicationPropertiesProvider,
                        conf.getFailureHandling().getMaxReplicationErrors(),
                        conf.getFailureHandling().getErrorPeriodInSeconds());
            case CUSTOM:
                return new MyEmergencyFailureHandler(enableReplicationResolver, customReplicationPropertiesProvider,
                        conf.getFailureHandling().getMaxReplicationErrors(),
                        conf.getFailureHandling().getErrorPeriodInSeconds());
            default:
                throw new IllegalArgumentException("Not supported replication failure handling mode " + failureHandling.getMode());
        }
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
                log.info("etcdErrors [{}] is less than [{}]", etcdErrors, DEFAULT_MAX_ETCD_ERRORS);
                throw smartReplicationException;
            } else {
                log.info("etcdErrors [{}] is equal or more than [{}]. SmartReplicationException is ignored.",
                    etcdErrors,
                    DEFAULT_MAX_ETCD_ERRORS);
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
            boolean replicationEnabled = conf.isReplicationEnabled();
            log.info("Get custom value for replicationEnabled [{}]", replicationEnabled);
            return replicationEnabled;
        }

        @Override
        public void setReplicationEnabled(boolean isReplicationEnabled) {
            conf.setReplicationEnabled(isReplicationEnabled);
        }

        @Override
        public ChangeSource getDefaultChangeSource() {
            ChangeSource changeSource = ChangeSource.MAIN;
            log.info("Get custom value for changeSource [{}]", changeSource);
            return changeSource;
        }
    }


}
