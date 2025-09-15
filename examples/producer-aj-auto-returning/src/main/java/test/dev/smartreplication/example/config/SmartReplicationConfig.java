package test.dev.smartreplication.example.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.listener.lifecycle.JdbcLifecycleEventListener;
import net.ttddyy.dsproxy.listener.logging.CommonsLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.postgresql.ds.PGSimpleDataSource;
import org.postgresql.jdbc.PgArray;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import test.dev.smartreplication.client.EmptyChangeSender;
import test.dev.smartreplication.client.EmptyJdbcCycle;
import test.dev.smartreplication.client.IChangeProducer;
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
import test.dev.streaming.key.KeyProvider;
import test.dev.streaming.mdc.ChangeKeySupplier;
import test.dev.streaming.mdc.MDCChangeReplicationModeProvider;
import test.dev.streaming.mdc.MDCPropertiesSupplier;
import test.dev.streaming.parser.EnumCastMapper;
import test.dev.streaming.parser.SaveReturningProxyQueryTransformer;
import test.dev.streaming.proxy.ExtendedResultSetProxyLogicFactory;
import test.dev.streaming.proxy.ReturnProxyFactory;
import test.dev.streaming.visitor.IVisitorFactory;
import test.dev.streaming.visitor.VisitorFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
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
    public ChangeProvider changeProvider(ChangeReplicationModeProvider changeReplicationModeProvider,
                                         KeyProvider keyProvider) {
        return new ThreadLocalChangeProvider() {
            @Override
            protected void cleanContext() {
                changeReplicationModeProvider.clear();
                keyProvider.clear();
            }
        };
    }

    @Bean
    public ChangeReplicationModeProvider changeReplicationModeProvider() {
        return new MDCChangeReplicationModeProvider();
    }

    @Bean
    public KeyProvider keyProvider() {
        // return new MDCKeySupplier();
        return new ChangeKeySupplier();
        // return new MyChangeKeySupplier();  // пример кастомной реализации, формирования ключа партиционирования для kafka
    }


    /**
     * Определяет не была ли выключена репликация и наблюдает за изменением флага enable-replication в ETCD.
     */
    @Bean
    public AbstractCachedResolver changeSourceResolver(IConfigurationManagerPrivate configManager,
                                                       CustomReplicationPropertiesProvider customReplicationPropertiesProvider) {

        // При недоступности ETCD ПЖ будут использованы кешированные значения управляющих параметров.
        return new WatcherChangeSourceResolver(new DistributedChangeSourceResolver(conf.getOwner(), configManager),
            ResolvingMode.CACHE);

        // При недоступности ETCD ПЖ будут использованы кастомные поставщики значений управляющих параметров Прикладного
        // Журнала.
        //return new WatcherChangeSourceResolver(new DistributedChangeSourceResolver(conf.getOwner(), configManager),
        //    customReplicationPropertiesProvider::getDefaultChangeSource,
        //    customReplicationPropertiesProvider::isReplicationEnabled);
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
        final EmergencyFailureHandler<Change> emergencyFailureHandler,
        final KeyProvider keyProvider) {
        return new SmartReplicationConfiguration(
            conf.getOwner(),
            keyProvider,
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
    public KafkaChangeHandler kafkaChangeHandler(KafkaProducerConfig producerConfig) {
        final var resolver = new OwnerKafkaProducerTopicResolver();
        KafkaProducerHolder kafkaProducerHolder = new KafkaProducerHolder(producerConfig);
        return new KafkaChangeHandler(
            resolver,
            new ChangeCompressor(
                CompressType.SNAPPY,
                //                new JsonConverter(new NanoISO8601ObjectMapperFactory()),
                new JsonConverter(createStandInObjectMapperFactory()),
                MAX_MESSAGE_SIZE,
                MAX_MESSAGE_SIZE,
                new DefaultCompressFactory()
            ),
            kafkaProducerHolder
        );
    }

    @Bean
    public IVisitorFactory<Change> visitorFactory() {
        return new VisitorFactory(
            // Если insert, update возращает ResultSet, то надо использовать эту реализацию к примеру:
            // если предполагается использовать INSERT, UPDATE, DELETE с RETURNING.
            new RewindCrawlerFactory()
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
     * Создание перехватчика запросов.
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
            .logQueryByCommons(CommonsLogLevel.INFO)
            .autoRetrieveGeneratedKeys(false, new ExtendedResultSetProxyLogicFactory())
            .jdbcProxyFactory(new ReturnProxyFactory())
            .queryTransformer(
                // Требуемый трансформер нужен если предполагается использовать INSERT, UPDATE, DELETE с RETURNING.
                new SaveReturningProxyQueryTransformer(
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
            .changeFormatType(ChangeFormatType.CHANGES)
            .skipQueryPredicate(new AllowingQueriesPredicateWithNextval())
            .useContextConnection()
            .changeReplicationModeProvider(changeReplicationModeProvider);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(
        @Qualifier("proxyDataSource") final DataSource proxyDataSource) {
        return new JdbcTemplate(proxyDataSource);
    }

    @Bean
    public PlatformTransactionManager transactionManager(
        DataSource proxyDataSource
    ) {
        return new DataSourceTransactionManager(proxyDataSource);
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

    private class AllowingQueriesPredicateWithNextval extends AllowingQueriesPredicate {
        @Override
        public boolean test(String query) {
            return query == null
                || query.length() <= this.getEndPosition()
                || (!this.queryMasks.contains(query.substring(this.getStartPosition(), this.getEndPosition())) &&
                !query.toLowerCase().startsWith("select nextval"));
        }
    }

    protected static NanoISO8601ObjectMapperFactory createStandInObjectMapperFactory() {
        return new NanoISO8601ObjectMapperFactory() {
            @Override
            public ObjectMapper create() {
                ObjectMapper objectMapper = super.create();
                objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
                SimpleModule simpleModule = new SimpleModule();
                simpleModule.addSerializer(PgArray.class, new JsonSerializer<>() {
                    @Override
                    public void serialize(PgArray pgArray,
                                          JsonGenerator jsonGenerator,
                                          SerializerProvider serializerProvider) throws IOException {
                        try {
                            jsonGenerator.writeObject(pgArray.getArray());
                        } catch (Exception e) {
                            jsonGenerator.writeObject(new Object[]{});
                        }
                    }
                });
                objectMapper.registerModule(simpleModule);
                return objectMapper;
            }
        };
    }

}
