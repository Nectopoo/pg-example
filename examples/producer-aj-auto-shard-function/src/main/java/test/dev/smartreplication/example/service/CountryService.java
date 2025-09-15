package test.dev.smartreplication.example.service;

import lombok.AllArgsConstructor;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import test.dev.smartreplication.client.sharding.domain.ShardBusinessKey;
import test.dev.streaming.mdc.MDCKeySupplier;

import java.util.Collections;

@Service
@AllArgsConstructor
public class CountryService {
    private final JdbcTemplate shardedJdbcTemplate;
    private final DataSourceTransactionManager dataSourceTransactionManager;
    private final ShardBusinessKey<String, String> shardBusinessKey;
//    private final ShardBusinessKey<Long, Long> populationShardBusinessKey;

    public void save(String name, long population) {
        // Задаем Бизнес-ключ шардирования используя ThreadLocal.
        shardBusinessKey.set(name);
//        populationShardBusinessKey.set(population);

        new TransactionTemplate(dataSourceTransactionManager).executeWithoutResult(it -> {
            final NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(shardedJdbcTemplate);
//            namedParameterJdbcTemplate.getJdbcTemplate().execute("delete from country");

            final MapSqlParameterSource params = new MapSqlParameterSource();
            final int nextVal = namedParameterJdbcTemplate.queryForObject("select nextval('users_id_sec')", Collections.emptyMap(), Integer.class);
            MDC.put(MDCKeySupplier.UNIQUE_KEY, Integer.toString(nextVal));
            params.addValue("id", nextVal);
            params.addValue("name", name);
            params.addValue("description", name);
            params.addValue("population", population);
            params.addValue("country_type", "FULL_RECOGNIZED");
            namedParameterJdbcTemplate.update("insert into country (id, name, description, population, country_type) " +
                    "values (:id, :name, :description, :population, :country_type::my_country_type)",
                params);
        });
    }
}
