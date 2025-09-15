package test.dev.smartreplication.example.config;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.listener.lifecycle.JdbcLifecycleEventListener;
import net.ttddyy.dsproxy.listener.logging.CommonsLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import test.dev.smartreplication.client.kafka.producer.KafkaChangeHandler;
import test.dev.smartreplication.client.kafka.producer.KafkaProducerHolder;
import test.dev.smartreplication.client.producer.AbstractCachedResolver;
import test.dev.smartreplication.client.producer.DistributedChangeSourceResolver;
import test.dev.smartreplication.client.producer.EnableReplicationResolver;
import test.dev.smartreplication.client.producer.WatcherChangeSourceResolver;
import test.dev.smartreplication.client.producer.handler.CustomReplicationPropertiesProvider;
import test.dev.smartreplication.client.producer.handler.DefaultEmergencyFailureHandler;
import test.dev.smartreplication.client.producer.handler.EmergencyFailureHandler;
import test.dev.smartreplication.client.producer.sending.filter.ChangeSendingFilter;
import test.dev.smartreplication.configuration.IConfigurationManagerPrivate;
import test.dev.smartreplication.core.compress.ChangeCompressor;
import test.dev.smartreplication.core.compress.DefaultCompressFactory;
import test.dev.smartreplication.core.compress.JsonConverter;
import test.dev.smartreplication.core.exception.SmartReplicationException;
import test.dev.smartreplication.core.kafka.NanoISO8601ObjectMapperFactory;
import test.dev.smartreplication.core.kafka.config.KafkaProducerConfig;
import test.dev.smartreplication.core.kafka.producer.OwnerKafkaProducerTopicResolver;
import test.dev.smartreplication.core.provider.ChangeFormatType;
import test.dev.smartreplication.core.provider.ChangeProvider;
import test.dev.smartreplication.core.provider.ThreadLocalChangeProvider;
import test.dev.smartreplication.example.properties.SmartReplicationProperties;
import test.dev.smartreplication.model.Change;
import test.dev.smartreplication.model.ChangeSource;
import test.dev.smartreplication.model.CompressType;
import test.dev.smartreplication.model.ProviderType;
import test.dev.streaming.adapter.flow.AutoChangeFlow;
import test.dev.streaming.adapter.producer.ChangeProducer;
import test.dev.streaming.config.AdapterConfig;
import test.dev.streaming.config.AllowingQueriesPredicate;
import test.dev.streaming.core.ChangeReplicationModeProvider;
import test.dev.streaming.jdbc.RewindCrawlerFactory;
import test.dev.streaming.jdbc.SyncJdbcInterceptor;
import test.dev.streaming.mdc.MDCChangeReplicationModeProvider;
import test.dev.streaming.mdc.MDCPropertiesSupplier;
import test.dev.streaming.parser.EnumCastMapper;
import test.dev.streaming.parser.ProxyQueryTransformer;
import test.dev.streaming.proxy.ExtendedResultSetProxyLogicFactory;
import test.dev.streaming.proxy.ReturnProxyFactory;
import test.dev.streaming.visitor.IVisitorFactory;
import test.dev.streaming.visitor.VisitorFactory;

import javax.sql.DataSource;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@ConditionalOnProperty(value = "smartReplication.enabled", havingValue = "true")
@EnableConfigurationProperties(SmartReplicationProperties.class)
public class SmartReplicationConfig {
    private static final int MAX_MESSAGE_SIZE = 1_000_000;

    private final SmartReplicationProperties conf;

    public SmartReplicationConfig(final SmartReplicationProperties conf) {
        this.conf = conf;
    }

    /**
     * Определение основного DataSource приложения.
     */
    @Bean
    public DataSource mainDataSource() {
        final var dataSource = new PGSimpleDataSource();
        dataSource.setUrl(conf.getMainDatasource().getUrl());
        dataSource.setUser(conf.getMainDatasource().getUsername());
        dataSource.setPassword(conf.getMainDatasource().getPassword());
        dataSource.setCurrentSchema(conf.getMainDatasource().getSchema());
        return dataSource;
    }

    /**
     * standin
     * Определение standin DataSource приложения.
     */
    @Bean
    public DataSource secondaryDataSource() {
        final var dataSource = new PGSimpleDataSource();
        dataSource.setUrl(conf.getStandInDatasource().getUrl());
        dataSource.setUser(conf.getStandInDatasource().getUsername());
        dataSource.setPassword(conf.getStandInDatasource().getPassword());
        dataSource.setCurrentSchema(conf.getStandInDatasource().getSchema());
        return dataSource;
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
    public ChangeProvider changeProvider(ChangeReplicationModeProvider changeReplicationModeProvider) {
        return new ThreadLocalChangeProvider() {
            @Override
            protected void cleanContext() {
                changeReplicationModeProvider.clear();
            }
        };
    }

    /**
     * Определяет не была ли выключена репликация и наблюдает за изменением флага enable-replication в ETCD.
     */
    @Bean
    public AbstractCachedResolver changeSourceResolver(IConfigurationManagerPrivate configManager,
                                                       CustomReplicationPropertiesProvider customReplicationPropertiesProvider) {
        return new WatcherChangeSourceResolver(new DistributedChangeSourceResolver(conf.getOwner(), configManager),
            customReplicationPropertiesProvider::getDefaultChangeSource,
            customReplicationPropertiesProvider::isReplicationEnabled);
    }

    /**
     * Доп проверка и возможность отключения репликации.
     */
    @Bean
    ChangeSendingFilter changeSendingFilter() {
        return conf::isShouldStopReplication;
    }

    @Bean
    public SmartReplicationConfiguration smartReplicationConfiguration(
        final KafkaChangeHandler kafkaChangeHandler,
        final ChangeProvider changeProvider,
        final AbstractCachedResolver changeSourceResolver,
        final ChangeSendingFilter changeSendingFilter,
        final EmergencyFailureHandler<Change> emergencyFailureHandler) {
        return new SmartReplicationConfiguration(
            conf.getOwner(),
            () -> "0",
//            new MDCKeySupplier(),
            kafkaChangeHandler,
            changeProvider,
            changeSourceResolver,
            changeSendingFilter,
            null, null, null,
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
        final SmartReplicationConfiguration smartReplicationConfiguration) {
        return new SmartReplicationDataSource(
            mainDataSource,
            secondaryDataSource,
            smartReplicationConfiguration
        );
    }

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

    @Bean
    public ChangeReplicationModeProvider changeReplicationModeProvider() {
        return new MDCChangeReplicationModeProvider();
    }

    @Bean
    public KafkaChangeHandler kafkaChangeHandler(KafkaProducerConfig producerConfig) {
        final var resolver = new OwnerKafkaProducerTopicResolver();
        return new KafkaChangeHandler(
            resolver,
            new ChangeCompressor(
                CompressType.SNAPPY,
                new JsonConverter(new NanoISO8601ObjectMapperFactory()),
                MAX_MESSAGE_SIZE,
                MAX_MESSAGE_SIZE,
                new DefaultCompressFactory()
            ),
            new KafkaProducerHolder(producerConfig)
        );
    }

    @Bean
    public IVisitorFactory<Change> visitorFactory() {
        return new VisitorFactory(
            new RewindCrawlerFactory() // Если insert, update возращает ResultSet, то надо использовать эту реализацию
        );
    }


    @Bean
    public IChangeProducer<String, ExecutionInfo> producer(
        @Qualifier("smartReplicationDataSource") DataSource smartReplicationDataSource,
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


    /**
     * Создание перехватчика запросов
     * AsyncJdbcInterceptor / SyncJdbcInterceptor - реализует асинхронный и синхронный механизмы перехвата JDBC запросов
     * VisitorFactory - фабрика создания объектов для описания поведения различных объектов JDBC
     */
    @Bean
    public JdbcLifecycleEventListener jdbcLifeListener(
        @Qualifier("smartReplicationDataSource") final DataSource smartReplicationDataSource,
        final AdapterConfig adapterConfig,
        final IVisitorFactory<Change> visitorFactory,
        IChangeProducer<String, ExecutionInfo> producer) {
        return new SyncJdbcInterceptor<>(
            AutoChangeFlow.builder()
                .config(adapterConfig)
                .dataSource(smartReplicationDataSource)
                .sender(new EmptyChangeSender()) // для отправки в ПЖ используется бин kafkaChangeHandler
                .producer(producer)
                .jdbcCycle(new EmptyJdbcCycle())
                .processor(visitorFactory.createProcessor())
                .build(),
            visitorFactory
        );
    }

    /**
     * Создание proxy для текущего dataSource:
     * <p>
     * .logQueryByCommons  -> определяет уровень логирования внутри библиотеки
     * .autoRetrieveGeneratedKeys -> позволят проксировать возвращаемые ключи
     * .parameterTransformer -> определяет стратегию трансформации передаваемых параметров
     * .queryTransformer -> определяет стратегию трансформации перехватываемых запросов
     * .listener -> реализация перехватчика запросов, коим является ProxyJdbcInterceptor
     * <p>
     */
    @Bean
    public DataSource proxyDataSource(
        @Qualifier("smartReplicationDataSource") final DataSource smartReplicationDataSource,
        final JdbcLifecycleEventListener jdbcLifeListener,
        AdapterConfig adapterConfig) {

        return ProxyDataSourceBuilder.create(smartReplicationDataSource)
            .logQueryByCommons(
                CommonsLogLevel.INFO) // задается уровень логирования prints Name:, Connection:2, Time:68, Success:True, Type:Prepared, Batch:False, QuerySize:1, BatchSize:0, Query:["select column_varchar1,column_varchar2,column_varchar3,column_varchar...
//          .logQueryBySlf4j(SLF4JLogLevel.TRACE)
//          .logQueryToSysOut()
            .autoRetrieveGeneratedKeys(false, new ExtendedResultSetProxyLogicFactory()) // задать, если нужно перехватывать  работу с ResultSet для сгенерированных ключей
            .jdbcProxyFactory(new ReturnProxyFactory())// фабрика для создания jdbc объектов
//          .parameterTransformer(new ProxyParameterTransformer())// если необходимо настроить трансформацию параметров запроса
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
                jdbcLifeListener// Listener для JDBC событий
            )
            .build();
    }

    @Bean
    public AdapterConfig adapterConfig(ChangeReplicationModeProvider changeReplicationModeProvider) {
        return new AdapterConfig(true)
            .customProperties(new MDCPropertiesSupplier())
            .provider(ProviderType.SMART_REPLICATION)
            // в ResultSet для потребителя передаются обновляемые поля из таблицы
            // ChangeFormatType.ENTITY в ResultSet для потребителя передаются все поля из таблицы
            .changeFormatType(ChangeFormatType.CHANGES)
            /*
                Позволяет формировать вектор изменений только по нужным таблицам
            */
//                .skipQueryPredicate(new AllowingQueriesPredicate())
            .skipQueryPredicate(new AllowingQueriesPredicateWithNextval())
//                .skipTablePredicate(new InterceptTablesPredicate<>(Set.of("table1", "table2", "table3")))
            .useContextConnection()
            .changeReplicationModeProvider(changeReplicationModeProvider)
            // Настройка parserCacheExpireAfterAccessSec включает кэширование результатов парсинга SQL запросов.
            // Данное кэширование эффективно в случае если сервис формирует большое количество одинаковых
            // SQL запросов. Эффект данной настройки будет проявляться как снижение утилизации CPU
            // и увеличение утилизации RAM.
            // Доступно начиная с версии 2.9.6 библиотеки прикладного журнала.
            .parserCacheExpireAfterAccessSec(300);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(
        @Qualifier("proxyDataSource") final DataSource proxyDataSource) {
        return new JdbcTemplate(proxyDataSource);
    }

    /**
     * Если нужен только JDBC (без hibernate) достаточно сделать этот transactionManager
     * вместо {@link SmartReplicationConfig#transactionManager(LocalContainerEntityManagerFactoryBean)}
     *
     */
//    @Bean
//    public PlatformTransactionManager transactionManager(
//        DataSource proxyDataSource
//    ) {
//        return new DataSourceTransactionManager(proxyDataSource);
//    }

    /**
     * HIBERNATE, необходимо закомментировать бин {@link SmartReplicationConfig#transactionManager(DataSource)}
     * и раскомментировать следующие бины:
     * {@link SmartReplicationConfig#transactionManager(LocalContainerEntityManagerFactoryBean)}
     * {@link SmartReplicationConfig#entityManagerFactory(DataSource)}
     */
    @Bean
    public PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        final var transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
        return transactionManager;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
        @Qualifier("proxyDataSource")
            DataSource proxyDataSource) {
        final var managerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        managerFactoryBean.setDataSource(proxyDataSource);
        managerFactoryBean.setPackagesToScan("test.dev.smartreplication.example");

        final var vendorAdapter = new HibernateJpaVendorAdapter();
        managerFactoryBean.setJpaVendorAdapter(vendorAdapter);
        managerFactoryBean.setJpaProperties(additionalProperties());

        return managerFactoryBean;
    }

    @Bean
    public CustomReplicationPropertiesProvider customReplicationPropertiesProvider() {
//        return new EmptyCustomReplicationPropertiesProvider();
        return new MyCustomReplicationPropertiesProvider();
    }

    private Properties additionalProperties() {
        final var properties = new Properties();
        properties.setProperty(AvailableSettings.HBM2DDL_AUTO, "none");
        properties.setProperty(AvailableSettings.DIALECT, "org.hibernate.dialect.PostgreSQL10Dialect");

        // иначе Hibernate бросит Exception
        // The database returned no natively generated identity value
        properties.setProperty("hibernate.jdbc.use_get_generated_keys", "false");
        return properties;
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

    private class AllowingQueriesPredicateWithNextval extends AllowingQueriesPredicate {
        @Override
        public boolean test(String query) {
            return query == null
                || query.length() <= this.getEndPosition()
                || (!this.queryMasks.contains(query.substring(this.getStartPosition(), this.getEndPosition())) &&
                !query.toLowerCase().startsWith("select nextval"));
        }
    }
}
