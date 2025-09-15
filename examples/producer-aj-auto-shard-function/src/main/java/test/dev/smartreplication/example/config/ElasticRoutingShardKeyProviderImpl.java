package test.dev.smartreplication.example.config;

import test.dev.elastic.routing.common.dto.DictionaryBlockDto;
import test.dev.elastic.routing.server.clientlib.userservice.ElasticRoutingLibUserService;
import test.dev.smartreplication.client.sharding.ShardKeyProvider;
import test.dev.smartreplication.client.sharding.domain.ShardBusinessKey;
import test.dev.smartreplication.client.sharding.domain.ShardKey;

/**
 * Шардирующая функция.
 */
public class ElasticRoutingShardKeyProviderImpl implements ShardKeyProvider {

    public static final String SHARD_KEY = "SHARD_KEY";

    private final ElasticRoutingLibUserService elasticRoutingLibUserService;
    private final ShardBusinessKey<Long, Long> populationShardBusinessKey;

    public ElasticRoutingShardKeyProviderImpl(ShardBusinessKey<Long, Long> populationShardBusinessKey,
                                              ElasticRoutingLibUserService elasticRoutingLibUserService) {
        this.populationShardBusinessKey = populationShardBusinessKey;
        this.elasticRoutingLibUserService = elasticRoutingLibUserService;
    }

    @Override
    public ShardKey getShardKey() {
        DictionaryBlockDto dictionaryBlock = elasticRoutingLibUserService.getDictionaryBlock
            ("TESTTSAJTSSG", "Dictionary", populationShardBusinessKey.getValue().toString());
        return new ShardKey(dictionaryBlock.getDict().get(SHARD_KEY));
    }
}
