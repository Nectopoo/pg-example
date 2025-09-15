package test.dev.smartreplication.example.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.ConnectionInfo;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.listener.MethodExecutionContext;
import net.ttddyy.dsproxy.listener.MethodExecutionListener;
import net.ttddyy.dsproxy.listener.lifecycle.JdbcLifecycleEventListener;
import net.ttddyy.dsproxy.proxy.ProxyConfig;
import net.ttddyy.dsproxy.proxy.ResultSetProxyLogic;
import net.ttddyy.dsproxy.proxy.ResultSetProxyLogicFactory;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
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
import test.dev.smartreplication.client.producer.WatcherChangeSourceResolver;
import test.dev.smartreplication.client.producer.handler.DefaultEmergencyFailureHandler;
import test.dev.smartreplication.client.producer.handler.EmergencyFailureHandler;
import test.dev.smartreplication.configuration.IConfigurationManagerPrivate;
import test.dev.smartreplication.core.compress.ChangeCompressor;
import test.dev.smartreplication.core.compress.DefaultCompressFactory;
import test.dev.smartreplication.core.compress.JsonConverter;
import test.dev.smartreplication.core.kafka.NanoISO8601ObjectMapperFactory;
import test.dev.smartreplication.core.kafka.config.KafkaProducerConfig;
import test.dev.smartreplication.core.kafka.producer.OwnerKafkaProducerTopicResolver;
import test.dev.smartreplication.core.provider.ChangeFormatType;
import test.dev.smartreplication.core.provider.ChangeProvider;
import test.dev.smartreplication.core.provider.ThreadLocalChangeProvider;
import test.dev.smartreplication.example.properties.DataSourceProperties;
import test.dev.smartreplication.example.properties.SmartReplicationProperties;
import test.dev.smartreplication.model.Change;
import test.dev.smartreplication.model.CompressType;
import test.dev.smartreplication.model.ProviderType;
import test.dev.streaming.adapter.flow.AutoChangeFlow;
import test.dev.streaming.adapter.producer.ChangeProducer;
import test.dev.streaming.config.*;
import test.dev.streaming.core.ChangeReplicationModeProvider;
import test.dev.streaming.jdbc.RewindCrawlerFactory;
import test.dev.streaming.jdbc.SyncJdbcInterceptor;
import test.dev.streaming.mdc.MDCChangeReplicationModeProvider;
import test.dev.streaming.mdc.MDCPropertiesSupplier;
import test.dev.streaming.parser.EnumCastMapper;
import test.dev.streaming.parser.ProxyQueryTransformer;
import test.dev.streaming.proxy.ExtendedResultSetProxyLogicFactory;
import test.dev.streaming.proxy.ResultSetMethodProxy;
import test.dev.streaming.proxy.ReturnProxyFactory;
import test.dev.streaming.visitor.IVisitorFactory;
import test.dev.streaming.visitor.VisitorFactory;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.Set;

@Slf4j
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
    @Bean(name = "mainDataSource")
    public DataSource mainDataSource() {
        final HikariConfig hikariConfig = new HikariConfig();
        DataSourceProperties mainDatasourceProperties = conf.getMainDatasource();
        hikariConfig.setDriverClassName(mainDatasourceProperties.getDriverClassName());
        hikariConfig.setJdbcUrl(mainDatasourceProperties.getUrl());
        hikariConfig.setUsername(mainDatasourceProperties.getUsername());
        hikariConfig.setPassword(mainDatasourceProperties.getPassword());
        hikariConfig.setSchema(mainDatasourceProperties.getSchema());

        hikariConfig.setMaximumPoolSize(mainDatasourceProperties.getMaxPoolSize());
        hikariConfig.setPoolName("mainPool");

        return new HikariDataSource(hikariConfig);
    }

    /**
     * standin
     * Определение standin DataSource приложения.
     */
    @Bean
    public DataSource standInDataSource() {
        final HikariConfig hikariConfig = new HikariConfig();
        DataSourceProperties standInDatasourceProperties = conf.getStandInDatasource();
        hikariConfig.setDriverClassName(standInDatasourceProperties.getDriverClassName());
        hikariConfig.setJdbcUrl(standInDatasourceProperties.getUrl());
        hikariConfig.setUsername(standInDatasourceProperties.getUsername());
        hikariConfig.setPassword(standInDatasourceProperties.getPassword());
        hikariConfig.setSchema(standInDatasourceProperties.getSchema());

        hikariConfig.setMaximumPoolSize(standInDatasourceProperties.getMaxPoolSize());
        hikariConfig.setPoolName("standInPool");

        return new HikariDataSource(hikariConfig);
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
    public AbstractCachedResolver changeSourceResolver(IConfigurationManagerPrivate configManager) {
        return new WatcherChangeSourceResolver(new DistributedChangeSourceResolver(conf.getOwner(), configManager));
    }

    @Bean
    public SmartReplicationConfiguration smartReplicationConfiguration(
        final KafkaChangeHandler kafkaChangeHandler,
        final ChangeProvider changeProvider,
        final AbstractCachedResolver changeSourceResolver,
        final EmergencyFailureHandler<Change> emergencyFailureHandler) {
        return new SmartReplicationConfiguration(
            conf.getOwner(),
            () -> "0",
//            new MDCKeySupplier(),
            kafkaChangeHandler,
            changeProvider,
            changeSourceResolver,
            () -> false,
            null, null, null,
            emergencyFailureHandler
        );
    }

    @Bean
    public ChangeReplicationModeProvider changeReplicationModeProvider() {
        return new MDCChangeReplicationModeProvider();
    }

    /**
     * Проксирование DataSource для его репликации (Прикладной Журнал / Standin / Smart Replication).
     */
    @Bean
    public DataSource smartReplicationDataSource(
        @Qualifier("mainDataSource") final DataSource mainDataSource,
        @Qualifier("standInDataSource") final DataSource secondaryDataSource,
        final SmartReplicationConfiguration smartReplicationConfiguration) {
        return new SmartReplicationDataSource(
            mainDataSource,
            secondaryDataSource,
            smartReplicationConfiguration
        );
    }

    @Bean
    public KafkaChangeHandler kafkaChangeHandler(KafkaProducerConfig producerConfig) {
        final var resolver = new OwnerKafkaProducerTopicResolver();
        return new KafkaChangeHandler(
//            producerConfig,
                resolver,
                new ChangeCompressor(
                        CompressType.SNAPPY,
                        new JsonConverter(new NanoISO8601ObjectMapperFactory()),
                        MAX_MESSAGE_SIZE,
                        MAX_MESSAGE_SIZE,
                        new DefaultCompressFactory()
                )
            ,new KafkaProducerHolder(producerConfig) // нужен для версии либы 2.9 +
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
//            .logQueryByCommons(CommonsLogLevel.TRACE)
//            .logQueryBySlf4j(SLF4JLogLevel.TRACE)
//            .logQueryToSysOut() // prints Name:, Connection:2, Time:68, Success:True, Type:Prepared, Batch:False, QuerySize:1, BatchSize:0, Query:["select column_varchar1,column_varchar2,column_varchar3,column_varchar4,column_varchar5,column_varchar6,column_varchar7,column_varchar8,column_varchar9,column_varchar10,column_varchar11,column_varchar12,column_varchar13,column_varchar14,column_varchar15,column_varchar16,column_varchar17,column_varchar18,column_varchar19,column_varchar20,column_varchar21,column_varchar22,column_varchar23,column_varchar24,column_varchar25,column_varchar26,column_varchar27,column_varchar28,column_varchar29,column_varchar30,column_varchar31,column_varchar32,column_varchar33,column_varchar34,column_varchar35,column_varchar36,column_varchar37,column_varchar38,column_varchar39,column_varchar40,column_varchar41,column_varchar42,column_varchar43,column_varchar44,column_varchar45,column_varchar46,column_varchar47,column_varchar48,column_varchar49,column_varchar50,column_varchar51,column_varchar52,column_varchar53,column_varchar54,column_varchar55,column_varchar56,column_varchar57,column_varchar58,column_varchar59,column_varchar60 from test where id IN (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"], Params:[(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119,120,121,122,123,124,125,126,127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,161,162,163,164,165,166,167,168,169,170,171,172,173,174,175,176,177,178,179,180,181,182,183,184,185,186,187,188,189,190,191,192,193,194,195,196,197,198,199,200,201,202,203,204,205,206,207,208,209,210,211,212,213,214,215,216,217,218,219,220,221,222,223,224,225,226,227,228,229,230,231,232,233,234,235,236,237,238,239,240,241,242,243,244,245,246,247,248,249,250,251,252,253,254,255,256,257,258,259,260,261,262,263,264,265,266,267,268,269,270,271,272,273,274,275,276,277,278,279,280,281,282,283,284,285,286,287,288,289,290,291,292,293,294,295,296,297,298,299,300,301,302,303,304,305,306,307,308,309,310,311,312,313,314,315,316,317,318,319,320,321,322,323,324,325,326,327,328,329,330,331,332,333,334,335,336,337,338,339,340,341,342,343,344,345,346,347,348,349,350,351,352,353,354,355,356,357,358,359,360,361,362,363,364,365,366,367,368,369,370,371,372,373,374,375,376,377,378,379,380,381,382,383,384,385,386,387,388,389,390,391,392,393,394,395,396,397,398,399,400,401,402,403,404,405,406,407,408,409,410,411,412,413,414,415,416,417,418,419,420,421,422,423,424,425,426,427,428,429,430,431,432,433,434,435,436,437,438,439,440,441,442,443,444,445,446,447,448,449,450,451,452,453,454,455,456,457,458,459,460,461,462,463,464,465,466,467,468,469,470,471,472,473,474,475,476,477,478,479,480,481,482,483,484,485,486,487,488,489,490,491,492,493,494,495,496,497,498,499,500,501,502,503,504,505,506,507,508,509,510,511,512,513,514,515,516,517,518,519,520,521,522,523,524,525,526,527,528,529,530,531,532,533,534,535,536,537,538,539,540,541,542,543,544,545,546,547,548,549,550,551,552,553,554,555,556,557,558,559,560,561,562,563,564,565,566,567,568,569,570,571,572,573,574,575,576,577,578,579,580,581,582,583,584,585,586,587,588,589,590,591,592,593,594,595,596,597,598,599,600,601,602,603,604,605,606,607,608,609,610,611,612,613,614,615,616,617,618,619,620,621,622,623,624,625,626,627,628,629,630,631,632,633,634,635,636,637,638,639,640,641,642,643,644,645,646,647,648,649,650,651,652,653,654,655,656,657,658,659,660,661,662,663,664,665,666,667,668,669,670,671,672,673,674,675,676,677,678,679,680,681,682,683,684,685,686,687,688,689,690,691,692,693,694,695,696,697,698,699,700,701,702,703,704,705,706,707,708,709,710,711,712,713,714,715,716,717,718,719,720,721,722,723,724,725,726,727,728,729,730,731,732,733,734,735,736,737,738,739,740,741,742,743,744,745,746,747,748,749,750,751,752,753,754,755,756,757,758,759,760,761,762,763,764,765,766,767,768,769,770,771,772,773,774,775,776,777,778,779,780,781,782,783,784,785,786,787,788,789,790,791,792,793,794,795,796,797,798,799,800,801,802,803,804,805,806,807,808,809,810,811,812,813,814,815,816,817,818,819,820,821,822,823,824,825,826,827,828,829,830,831,832,833,834,835,836,837,838,839,840,841,842,843,844,845,846,847,848,849,850,851,852,853,854,855,856,857,858,859,860,861,862,863,864,865,866,867,868,869,870,871,872,873,874,875,876,877,878,879,880,881,882,883,884,885,886,887,888,889,890,891,892,893,894,895,896,897,898,899,900,901,902,903,904,905,906,907,908,909,910,911,912,913,914,915,916,917,918,919,920,921,922,923,924,925,926,927,928,929,930,931,932,933,934,935,936,937,938,939,940,941,942,943,944,945,946,947,948,949,950,951,952,953,954,955,956,957,958,959,960,961,962,963,964,965,966,967,968,969,970,971,972,973,974,975,976,977,978,979,980,981,982,983,984,985,986,987,988,989,990,991,992,993,994,995,996,997,998,999,1000)]
            .autoRetrieveGeneratedKeys(false, new ExtendedResultSetProxyLogicFactory())
            .jdbcProxyFactory(new ReturnProxyFactory())    // нужно для update?
//            .methodListener(createMethodExecutionListener())  // added by mio for testing purposes
//            .parameterTransformer(new ProxyParameterTransformer()) // test.dev.streaming.parser.ProxyParameterTransformer#transformParameters - пустой метод, можно убрать
            .queryTransformer(  // для чего такая трансформация??? в каких случаях
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

    private MethodExecutionListener createMethodExecutionListener() {
        return new MethodExecutionListener() {

            @Override
            public void beforeMethod(MethodExecutionContext methodExecutionContext) {
                if (methodExecutionContext.getMethod().getDeclaringClass().getSimpleName().equals("ResultSet")) {
                    System.out.println(methodExecutionContext.getMethod());
                }
            }

            @Override
            public void afterMethod(MethodExecutionContext methodExecutionContext) {

            }
        };
    }

    private ResultSetProxyLogicFactory createResultSetProxyLogicFactory() {
        return new ResultSetProxyLogicFactory() {
            @Override
            public ResultSetProxyLogic create(ResultSet resultSet,
                                              ConnectionInfo connectionInfo,
                                              ProxyConfig proxyConfig) {

                ResultSetProxyLogic resultSetProxyLogic = DEFAULT.create(resultSet, connectionInfo, proxyConfig);
                return new ResultSetMethodProxy(resultSet, resultSetProxyLogic, Set.of("wasNull", "getType", "getObject", "next"));
            }
        };
    }

    /**
     * Executor, который передается библиотеке jsqlparser для парсинга sql выражений.
     * Максимальное кол-во тредов зависит от нагрузки приложения:
     * java-тредов должно быть не меньше чем параллельно выполняющихся sql запросов.
     * Лучше, чтобы задачи парсинга в очередь не становились, т.к. увеличит latency транзакций.
     */
    @Bean(name = "sqlParserExecutor")
    public ThreadPoolTaskExecutor sqlParserExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setThreadNamePrefix("sql-parser-executor");
        threadPoolTaskExecutor.setQueueCapacity(0);
        threadPoolTaskExecutor.setKeepAliveSeconds(60);
        threadPoolTaskExecutor.setCorePoolSize(10);
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }

    @Bean
    public AdapterConfig adapterConfig(
        @Qualifier("sqlParserExecutor") ThreadPoolTaskExecutor sqlParserExecutor,
        ChangeReplicationModeProvider changeReplicationModeProvider
    ) {
        return new AdapterConfig(true)
                .customProperties(new MDCPropertiesSupplier())
                .provider(ProviderType.SMART_REPLICATION)
                // в ResultSet для потребителя передаются обновляемые поля из таблицы
                // ChangeFormatType.ENTITY в ResultSet для потребителя передаются все поля из таблицы
                .changeFormatType(ChangeFormatType.CHANGES)
                /*
                    Позволяет формировать вектор изменений только по нужным таблицам
                */
//                .skipQueryPredicate(new MyAllowingQueriesPredicate()) // не парсит
                .skipQueryPredicate(new AllowingQueriesPredicateWithNextval())
//                .skipTablePredicate(new SkipTablePredicate<>(Set.of("rrko_ru_payment","rrko_edoc_ref"))) // в кафку не отправляет
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
     *
     */
    @Bean
    public EmergencyFailureHandler<Change> emergencyFailureHandler(EnableReplicationResolver enableReplicationResolver) {
        return new DefaultEmergencyFailureHandler<>(enableReplicationResolver, conf.getMaxReplicationErrors(), conf.getErrorPeriodInSeconds());
    }

    private class AllowingQueriesPredicateWithNextval extends MyAllowingQueriesPredicate {
        @Override
        public boolean test(String query) {
            return query == null
                || query.length() <= this.getEndPosition()
                || (!this.queryMasks.contains(query.substring(this.getStartPosition(), this.getEndPosition())) && !query.toLowerCase().startsWith("select nextval"));
        }
    }

    public class MyAllowingQueriesPredicate extends AbstractQueryPredicate {
        public MyAllowingQueriesPredicate() {
        }

        public boolean test(String query) {
            return query == null || query.length() <= this.getEndPosition() || !this.queryMasks.contains(query.substring(this.getStartPosition(), this.getEndPosition()));
        }

        protected void init() {
            this.queryMasks.add("ins");
            this.queryMasks.add("upd");
            this.queryMasks.add("del");
        }
    }
}
