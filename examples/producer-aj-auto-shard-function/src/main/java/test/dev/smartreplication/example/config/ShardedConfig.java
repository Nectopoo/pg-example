package test.dev.smartreplication.example.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import test.dev.elastic.routing.server.clientlib.userservice.ElasticRoutingLibUserService;
import test.dev.smartreplication.client.sharding.ShardedSmartReplicationDataSource;
import test.dev.smartreplication.client.sharding.domain.ShardBusinessKey;
import test.dev.smartreplication.client.sharding.domain.ShardKey;
import test.dev.smartreplication.example.properties.SmartReplicationProperties;

import javax.sql.DataSource;
import java.util.Map;


/**
 * Конфигурационный класс создающий бины для работы с шардированной базой данных.
 */
@Configuration
@ConditionalOnProperty(value = "smartReplication.enabled", havingValue = "true")
@EnableConfigurationProperties(SmartReplicationProperties.class)
public class ShardedConfig {

    public static final String SHARD_KEY_DATASOURCE_ONE = "SHARD_KEY_DATASOURCE_ONE";
    public static final String SHARD_KEY_DATASOURCE_TWO = "SHARD_KEY_DATASOURCE_TWO";

    private final SmartReplicationProperties conf;

    public ShardedConfig(final SmartReplicationProperties conf) {
        this.conf = conf;
    }

    /**
     * Бизнес-ключ.
     */
    @Bean
    public ShardBusinessKey<String, String> countryShardBusinessKey() {
        return new CountryShardBusinessKey();
    }

    /**
     * Бизнес-ключ.
     */
    @Bean
    public ShardBusinessKey<Long, Long> populationBusinessKey() {
        return new PopulationShardBusinessKey();
    }

    /**
     * Декоратор добавляет логику рассчета DataSource.
     * @param proxyDataSource - один из возможных возвращаемых DataSource.
     * @param proxyDataSource2 - один из возможных возвращаемых DataSource.
     */
    @Bean
    public ShardedSmartReplicationDataSource shardedSmartReplicationDataSource(DataSource proxyDataSource,
                                                                               DataSource proxyDataSource2,
                                                                               ShardBusinessKey<String, String> countryShardBusinessKey,
                                                                               ShardBusinessKey<Long, Long> populationBusinessKey,
                                                                               ElasticRoutingLibUserService elasticRoutingLibUserService) {
        return new ShardedSmartReplicationDataSource(
            // Принимает Map<ShardKey, DataSource> и имплементацию ShardKeyProvider
            Map.of(
                new ShardKey(SHARD_KEY_DATASOURCE_ONE), proxyDataSource,
                new ShardKey(SHARD_KEY_DATASOURCE_TWO), proxyDataSource2),
            conf.isElasticRoutingEnabled()
                ? new ElasticRoutingShardKeyProviderImpl(populationBusinessKey, elasticRoutingLibUserService)
                : new ShardKeyProviderImpl(countryShardBusinessKey));
    }

    /**
     * JdbcTemplate работающий на {@link ShardedSmartReplicationDataSource}.
     */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource shardedSmartReplicationDataSource) {
        return new JdbcTemplate(shardedSmartReplicationDataSource);
    }

    /**
     * DataSourceTransactionManager работающий на {@link ShardedSmartReplicationDataSource}.
     */
    @Bean
    public DataSourceTransactionManager dataSourceTransactionManager(DataSource shardedSmartReplicationDataSource) {
        return new DataSourceTransactionManager(shardedSmartReplicationDataSource);
    }
}
