package test.dev.smartreplication.example.config;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.listener.lifecycle.JdbcLifecycleEventListener;
import net.ttddyy.dsproxy.listener.logging.CommonsLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import test.dev.smartreplication.client.EmptyChangeSender;
import test.dev.smartreplication.client.EmptyJdbcCycle;
import test.dev.smartreplication.client.IChangeProducer;
import test.dev.smartreplication.client.SmartReplicationConfiguration;
import test.dev.smartreplication.client.SmartReplicationDataSource;
import test.dev.smartreplication.client.kafka.KafkaChangeHandler;
import test.dev.smartreplication.client.kafka.KafkaProducerHolder;
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
import test.dev.streaming.jdbc.RewindCrawlerFactory;
import test.dev.streaming.jdbc.SyncJdbcInterceptor;
import test.dev.streaming.mdc.MDCPropertiesSupplier;
import test.dev.streaming.parser.EnumCastMapper;
import test.dev.streaming.parser.ProxyParameterTransformer;
import test.dev.streaming.parser.ProxyQueryTransformer;
import test.dev.streaming.proxy.ExtendedResultSetProxyLogicFactory;
import test.dev.streaming.proxy.ReturnProxyFactory;
import test.dev.streaming.visitor.VisitorCachedFactory;
import test.dev.streaming.visitor.IVisitorFactory;

import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@ConditionalOnProperty(value = "smartReplication.enabled", havingValue = "true")
@EnableConfigurationProperties(SmartReplicationProperties.class)
public class SmartReplicationConfig2 {
    private static final int MAX_MESSAGE_SIZE = 1_000_000;

    private final SmartReplicationProperties conf;

    public SmartReplicationConfig2(final SmartReplicationProperties conf) {
        this.conf = conf;
    }

    /**
     * Определение основного DataSource приложения.
     */
    @Bean
    public DataSource mainDataSource2() {
        final var dataSource = new PGSimpleDataSource();
        dataSource.setUrl(conf.getMainDatasource2().getUrl());
        dataSource.setUser(conf.getMainDatasource2().getUsername());
        dataSource.setPassword(conf.getMainDatasource2().getPassword());
        dataSource.setCurrentSchema(conf.getMainDatasource2().getSchema());
        return dataSource;
    }

    /**
     * standin
     * Определение standin DataSource приложения.
     */
    @Bean
    public DataSource secondaryDataSource2() {
        final var dataSource = new PGSimpleDataSource();
        dataSource.setUrl(conf.getStandInDatasource2().getUrl());
        dataSource.setUser(conf.getStandInDatasource2().getUsername());
        dataSource.setPassword(conf.getStandInDatasource2().getPassword());
        dataSource.setCurrentSchema(conf.getStandInDatasource2().getSchema());
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
    public ChangeProvider changeProvider2() {
        return new ThreadLocalChangeProvider();
    }

    /**
     * Определяет не была ли выключена репликация и наблюдает за изменением флага enable-replication в ETCD.
     */
    @Bean
    public AbstractCachedResolver changeSourceResolver2(IConfigurationManagerPrivate configManager,
                                                       CustomReplicationPropertiesProvider customReplicationPropertiesProvider,
                                                        KafkaProducerHolder kafkaProducerHolder2) {
        final WatcherChangeSourceResolver watcherChangeSourceResolver = new WatcherChangeSourceResolver(new DistributedChangeSourceResolver(conf.getOwner2(), configManager),
            customReplicationPropertiesProvider::getDefaultChangeSource,
            customReplicationPropertiesProvider::isReplicationEnabled);
        watcherChangeSourceResolver.addListener(kafkaProducerHolder2);
        return watcherChangeSourceResolver;
    }

    /**
     * Доп проверка и возможность отключения репликации.
     */
    @Bean
    ChangeSendingFilter changeSendingFilter2() {
        return conf::isShouldStopReplication;
    }

    @Bean
    public SmartReplicationConfiguration smartReplicationConfiguration2(
        final KafkaChangeHandler kafkaChangeHandler2,
        final ChangeProvider changeProvider2,
        final AbstractCachedResolver changeSourceResolver2,
        final ChangeSendingFilter changeSendingFilter2,
        final EmergencyFailureHandler<Change> emergencyFailureHandler2) {
        return new SmartReplicationConfiguration(
            conf.getOwner2(),
            () -> "0",
//            new MDCKeySupplier(),
            kafkaChangeHandler2,
            changeProvider2,
            changeSourceResolver2,
            changeSendingFilter2,
            null, null, null,
            emergencyFailureHandler2
        );
    }

    /**
     * Проксирование DataSource для его репликации (Прикладной Журнал / Standin / Smart Replication).
     */
    @Bean
    public DataSource smartReplicationDataSource2(
        final DataSource mainDataSource2,
        final DataSource secondaryDataSource2,
        final SmartReplicationConfiguration smartReplicationConfiguration2) {
        return new SmartReplicationDataSource(
            mainDataSource2,
            secondaryDataSource2,
            smartReplicationConfiguration2
        );
    }

    @Bean
    public KafkaProducerConfig kafkaProducerConfig2() {
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
    public KafkaProducerHolder kafkaProducerHolder2(KafkaProducerConfig kafkaProducerConfig2) {
        return new KafkaProducerHolder(kafkaProducerConfig2);
    }

    @Bean
    public KafkaChangeHandler kafkaChangeHandler2(KafkaProducerHolder kafkaProducerHolder2) {
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
                kafkaProducerHolder2
        );
    }

    @Bean
    public IVisitorFactory<Change> visitorFactory2() {
        return new VisitorCachedFactory(
            new RewindCrawlerFactory() // Если insert, update возращает ResultSet, то надо использовать эту реализацию
        );
    }


    @Bean
    public IChangeProducer<String, ExecutionInfo> producer2(
        DataSource smartReplicationDataSource2,
        AdapterConfig adapterConfig2,
        IVisitorFactory<Change> visitorFactory2,
        ChangeProvider changeProvider2) {
        return ChangeProducer.builder()
            .dataSource(smartReplicationDataSource2)
            .config(adapterConfig2)
            .factory(visitorFactory2)
            .provider(changeProvider2)
            .build();
    }


    /**
     * Создание перехватчика запросов
     * AsyncJdbcInterceptor / SyncJdbcInterceptor - реализует асинхронный и синхронный механизмы перехвата JDBC запросов
     * VisitorFactory - фабрика создания объектов для описания поведения различных объектов JDBC
     */
    @Bean
    public JdbcLifecycleEventListener jdbcLifeListener2(
        final DataSource smartReplicationDataSource2,
        final AdapterConfig adapterConfig2,
        final IVisitorFactory<Change> visitorFactory2,
        IChangeProducer<String, ExecutionInfo> producer2) {
        return new SyncJdbcInterceptor<>(
            AutoChangeFlow.builder()
                .config(adapterConfig2)
                .dataSource(smartReplicationDataSource2)
                .sender(new EmptyChangeSender()) // для отправки в ПЖ используется бин kafkaChangeHandler
                .producer(producer2)
                .jdbcCycle(new EmptyJdbcCycle())
                .processor(visitorFactory2.createProcessor())
                .build(),
            visitorFactory2
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
    public DataSource proxyDataSource2(
        final DataSource smartReplicationDataSource2,
        final JdbcLifecycleEventListener jdbcLifeListener2,
        final AdapterConfig adapterConfig2,
        final IVisitorFactory<Change> visitorFactory2) {

        return ProxyDataSourceBuilder.create(smartReplicationDataSource2)
            .logQueryByCommons(CommonsLogLevel.INFO)// задается уровень логирования
            .autoRetrieveGeneratedKeys(false, new ExtendedResultSetProxyLogicFactory())// задать, если нужно перехватывать  работу с ResultSet для сгенерированных ключей
            .jdbcProxyFactory(new ReturnProxyFactory())// фабрика для создания jdbc объектов
            .parameterTransformer(new ProxyParameterTransformer())// если необходимо настроить трансформацию параметров запроса
            .queryTransformer(//Для обработки SQL запросов перед выполнением
                new ProxyQueryTransformer(
                    smartReplicationDataSource2,
                    visitorFactory2,
                    adapterConfig2,
                    new EnumCastMapper<>(
                        type -> EnumCastMapper.ENUM_CAST + "\"" + type.getSchema() + "\".\"" + type.getType() + "\"",
                        type -> EnumCastMapper.ENUM_CAST + type.getType()
                    )
                )
            )
            .listener(
                jdbcLifeListener2// Listener для JDBC событий
            )
            .build();
    }

    @Bean
    public AdapterConfig adapterConfig2() {
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
                .useContextConnection();
    }

    @Bean
    public CustomReplicationPropertiesProvider customReplicationPropertiesProvider2() {
//        return new EmptyCustomReplicationPropertiesProvider();
        return new MyCustomReplicationPropertiesProvider();
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
    public EmergencyFailureHandler<Change> emergencyFailureHandler2(EnableReplicationResolver changeSourceResolver2,
                                                                   CustomReplicationPropertiesProvider customReplicationPropertiesProvider2) {
//        return new EmptyEmergencyFailureHandler<>();
//        return new DefaultEmergencyFailureHandler<>(enableReplicationResolver, customReplicationPropertiesProvider, 
//                conf.getMaxReplicationErrors(), conf.getErrorPeriodInSeconds());
        return new MyEmergencyFailureHandler(changeSourceResolver2, customReplicationPropertiesProvider2,
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
                    || (!this.queryMasks.contains(query.substring(this.getStartPosition(), this.getEndPosition())) && !query.toLowerCase().startsWith("select nextval"));
        }
    }
}
