package test.dev.smartreplication.example.config;


import test.dev.smartreplication.client.sharding.ShardedSmartReplicationDataSource;
import test.dev.smartreplication.client.sharding.domain.ShardBusinessKey;
import test.dev.smartreplication.client.sharding.domain.ShardKey;

/**
 * Ключ шардирования, используется в двух случаях:
 * 1. Получение его ThreadLocal в потоке выполнения {@link ShardedSmartReplicationDataSource#getDataSource()}
 * для последующего рассчета {@link ShardKey}
 * 2. Есть возможность дописать логику рассчета {@link ShardKey} используя {@link this#getValue()}
 * {@link ShardKeyProviderImpl#getShardKey()}
 */
public class CountryShardBusinessKey extends ShardBusinessKey<String, String> {

    public String getValue() {
        return get();
    }
}
