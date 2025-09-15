package test.dev.smartreplication.example.config;

import test.dev.smartreplication.client.sharding.ShardKeyProvider;
import test.dev.smartreplication.client.sharding.domain.ShardBusinessKey;
import test.dev.smartreplication.client.sharding.domain.ShardKey;

import static test.dev.smartreplication.example.config.ShardedConfig.SHARD_KEY_DATASOURCE_ONE;
import static test.dev.smartreplication.example.config.ShardedConfig.SHARD_KEY_DATASOURCE_TWO;

/**
 * Шардирующая функция.
 */
public class ShardKeyProviderImpl implements ShardKeyProvider {

    public static final ShardKey SHARD_KEY_ONE = new ShardKey(SHARD_KEY_DATASOURCE_ONE);
    public static final ShardKey SHARD_KEY_TWO = new ShardKey(SHARD_KEY_DATASOURCE_TWO);

    public ShardBusinessKey<String, String> countryShardBusinessKey;

    public ShardKeyProviderImpl(ShardBusinessKey<String, String> countryShardBusinessKey) {
        this.countryShardBusinessKey = countryShardBusinessKey;
    }

    @Override
    public ShardKey getShardKey() {
        return countryShardBusinessKey.getValue().toCharArray()[0] % 2 == 0
            ? SHARD_KEY_ONE : SHARD_KEY_TWO;
    }
}
