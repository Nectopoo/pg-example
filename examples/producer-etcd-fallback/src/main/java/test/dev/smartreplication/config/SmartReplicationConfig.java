package test.dev.smartreplication.config;

import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.listener.lifecycle.JdbcLifecycleEventListener;
import net.ttddyy.dsproxy.listener.logging.CommonsLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import test.dev.smartreplication.client.*;
import test.dev.smartreplication.client.kafka.KafkaChangeHandler;
import test.dev.smartreplication.client.kafka.KafkaProducerHolder;
import test.dev.smartreplication.client.producer.*;
import test.dev.smartreplication.client.producer.etcd.EtcdChangeSourceResolver;
import test.dev.smartreplication.client.producer.etcd.EtcdChangeSourceResolverListener;
import test.dev.smartreplication.client.producer.handler.CustomReplicationPropertiesProvider;
import test.dev.smartreplication.client.producer.handler.EmergencyFailureHandler;
import test.dev.smartreplication.client.producer.handler.EmptyEmergencyFailureHandler;
import test.dev.smartreplication.client.producer.sending.filter.ChangeSendingFilter;
import test.dev.smartreplication.configuration.etcd.EtcdConfigurationManager;
import test.dev.smartreplication.core.compress.ChangeCompressor;
import test.dev.smartreplication.core.compress.DefaultCompressFactory;
import test.dev.smartreplication.core.compress.JsonConverter;
import test.dev.smartreplication.core.kafka.NanoISO8601ObjectMapperFactory;
import test.dev.smartreplication.core.kafka.config.KafkaProducerConfig;
import test.dev.smartreplication.core.kafka.producer.OwnerKafkaProducerTopicResolver;
import test.dev.smartreplication.core.provider.ChangeFormatType;
import test.dev.smartreplication.core.provider.ChangeProvider;
import test.dev.smartreplication.core.provider.ThreadLocalChangeProvider;
import test.dev.smartreplication.model.Change;
import test.dev.smartreplication.model.ChangeSource;
import test.dev.smartreplication.model.CompressType;
import test.dev.smartreplication.model.ProviderType;
import test.dev.smartreplication.properties.SmartReplicationProperties;
import test.dev.streaming.adapter.flow.AutoChangeFlow;
import test.dev.streaming.adapter.producer.ChangeProducer;
import test.dev.streaming.config.AdapterConfig;
import test.dev.streaming.config.AllowingQueriesPredicate;
import test.dev.streaming.jdbc.RewindCrawlerFactory;
import test.dev.streaming.jdbc.SyncJdbcInterceptor;
import test.dev.streaming.mdc.MDCPropertiesSupplier;
import test.dev.streaming.parser.EnumCastMapper;
import test.dev.streaming.parser.ProxyQueryTransformer;
import test.dev.streaming.proxy.ExtendedResultSetProxyLogicFactory;
import test.dev.streaming.proxy.ReturnProxyFactory;
import test.dev.streaming.visitor.IVisitorFactory;
import test.dev.streaming.visitor.VisitorFactory;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties(SmartReplicationProperties.class)
@Slf4j
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
     * однако при передаче Builder-ов в другие потоки не гарантируют конкурентного доступа.
     */
    @Bean
    public ChangeProvider changeProvider() {
        return new ThreadLocalChangeProvider();
    }

    /**
     * В данной конфигурации используется кэширующая реализация EtcdChangeSourceResolver
     * позволяющая продолжать работу с ПЖ в случае временной недоступности ETCD.
     */
    @Bean
    public AbstractCachedResolver changeSourceResolver(EtcdConfigurationManager etcdConfigurationManager,
                                                       EtcdChangeSourceResolverListener listener) {

        EtcdChangeSourceResolver etcdChangeSourceResolver = new EtcdChangeSourceResolver(conf.getOwner(),
            etcdConfigurationManager);

        etcdChangeSourceResolver.addListener(listener);
        etcdChangeSourceResolver.init();

        return etcdChangeSourceResolver;
    }

    /**
     * Пример создания листенера для получения уведомлений о параметрах прикладного журнала
     * и состоянии подключения к ETCD.
     */
    @Bean
    public EtcdChangeSourceResolverListener etcdChangeSourceResolverListener() {
        EtcdChangeSourceResolverListener listener = new EtcdChangeSourceResolverListener() {


            /* Получаем начальные значения параметров ПЖ при старте нашего сервиса */
            @Override
            public void onInit(ChangeSource changeSource, boolean enableReplicationStatus) {
                log.info("Initial smart replication parameters for service [{}]. ChangeSource [{}], " +
                    "enableReplicationStatus [{}]", conf.getOwner(), changeSource, enableReplicationStatus);
            }


            /* Отслеживаем переключения баз MAIN <-> STAND_IN */
            @Override
            public void onChangeSourceChanged(ChangeSource previousChangeSource,
                                              ChangeSource newChangeSource,
                                              boolean currentEnableReplicationStatus) {

                log.info("Change source is changed from [{}] to [{}]. Current enable replication status [{}].",
                    previousChangeSource,
                    newChangeSource,
                    currentEnableReplicationStatus);
            }


            /* Отслеживаем параметр разрешения работы репликации. */
            @Override
            public void onEnableReplicationStatusChanged(boolean newEnableReplicationStatus,
                                                         ChangeSource currentChangeSource) {

                log.info("Enable replication status is changed from [{}] to [{}]. Current change source [{}].",
                    !newEnableReplicationStatus,
                    newEnableReplicationStatus,
                    currentChangeSource);
            }

            /* Отслеживаем факт недоступности ETCD. Данное уведомление обозначает следующие риски
            * - потеряна возможность выбора БД MAIN или STAND_IN штатными средствами ПЖ;
            * - нет возможности отключить использование ПЖ штатными средствами ПЖ;
            * - рассинхронизация настроек ПЖ на разных экземплярах сервиса, если ETCD продолжает быть доступным для
            * некоторых подов.
            */
            @Override
            public void onDisconnected(ChangeSource cachedChangeSource,
                                       boolean cachedEnableReplicationStatus,
                                       Throwable throwable) {

                log.warn("Disconnected mode! Cached values changeSource [{}], enableReplicationStatus [{}]",
                    cachedChangeSource, cachedEnableReplicationStatus);
                log.warn("Disconnection cause is", throwable);
            }

            /*
             * Отслеживаем факт восстановления доступности ETCD.
             * Несовпадение кэшированных и актуализированных значений параметров ПЖ говорит о высоком риске
             * рассинхронизации баз MAIN и STAND_IN.
             * В любом случае требуется анализ работы сервиса и инфрастуктуры для выяснения корректности и актуальности
             * данных в БД MAIN и STAND_IN.
             */
            @Override
            public void onReconnected(ChangeSource cachedChangeSource,
                                    boolean cachedEnableReplicationStatus,
                                    ChangeSource actualChangeSource,
                                    boolean actualEnableReplicationStatus) {

                log.warn("Connection is restored. Cached values were "
                    + "ChangeSource [{}], enableReplicationStatus [{}]. Actualized values are "
                    + "ChangeSource [{}], enableReplicationStatus [{}]",
                    cachedChangeSource,
                    cachedEnableReplicationStatus,
                    actualChangeSource,
                    actualEnableReplicationStatus);
            }

        };

        return listener;
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
                new JsonConverter(new NanoISO8601ObjectMapperFactory()),
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
                CommonsLogLevel.INFO)
            .autoRetrieveGeneratedKeys(false, new ExtendedResultSetProxyLogicFactory()) // задать, если нужно перехватывать  работу с ResultSet для сгенерированных ключей
            .jdbcProxyFactory(new ReturnProxyFactory())
            .queryTransformer(
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
                jdbcLifeListener
            )
            .build();
    }

    @Bean
    public AdapterConfig adapterConfig() {
        return new AdapterConfig(true)
            .customProperties(new MDCPropertiesSupplier())
            .provider(ProviderType.SMART_REPLICATION)
            .changeFormatType(ChangeFormatType.CHANGES)
            .skipQueryPredicate(new AllowingQueriesPredicateWithNextval())
            .useContextConnection();
    }

    @Bean
    public JdbcTemplate jdbcTemplate(
        @Qualifier("proxyDataSource") final DataSource proxyDataSource) {
        return new JdbcTemplate(proxyDataSource);
    }

    @Bean
    public PlatformTransactionManager transactionManager(
        DataSource proxyDataSource) {

        return new DataSourceTransactionManager(proxyDataSource);
    }


    @Bean
    public CustomReplicationPropertiesProvider customReplicationPropertiesProvider() {
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
     */
    @Bean
    public EmergencyFailureHandler<Change> emergencyFailureHandler(EnableReplicationResolver enableReplicationResolver,
                                                                   CustomReplicationPropertiesProvider customReplicationPropertiesProvider) {
        return new EmptyEmergencyFailureHandler<>();
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
