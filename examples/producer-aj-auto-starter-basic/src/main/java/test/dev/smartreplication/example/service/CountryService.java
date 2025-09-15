package test.dev.smartreplication.example.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import test.dev.streaming.mdc.MDCKeySupplier;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class CountryService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * Конструктор.
     * @param jdbcTemplate - поставляется из smart-replication-producer-auto-starter.
     */
    public CountryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Transactional
    public int save(String name, int population) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        final int nextval = jdbcTemplate.queryForObject("select nextval('users_id_sec')", Collections.emptyMap(), Integer.class);
        // задаем ключ партиционирования в kafka
        MDC.put(MDCKeySupplier.UNIQUE_KEY, Integer.toString(nextval));
        params.addValue("id", nextval);
        params.addValue("name", name);
        params.addValue("description", name);
        params.addValue("population", population);
        params.addValue("country_type", "FULL_RECOGNIZED");
        params.addValue("amount", new BigDecimal("98765432.00"));
        final int updated = jdbcTemplate.update("insert into country (id, name, description, population, country_type, amount) " +
                        "values (:id, :name, :description, :population, :country_type::my_country_type, :amount)",
                params);
        return updated;
    }

    @Transactional
    public int save(int id, String name, int population) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        // id автоматически будет использоваться в качестве ключа партиционирования в kafka, см реализацию ChangeKeySupplier
        params.addValue("id", id);
        params.addValue("name", name);
        params.addValue("description", name);
        params.addValue("population", population);
        params.addValue("country_type", "FULL_RECOGNIZED");
        params.addValue("amount", new BigDecimal("12345678.00"));
        final int updated = jdbcTemplate.update("insert into country (id, name, description, population, country_type, amount) " +
                        "values (:id, :name, :description, :population, :country_type::my_country_type, :amount)",
                params);
        return updated;
    }

    @Transactional
    public int update(int id, int population) {
        MDC.put(MDCKeySupplier.UNIQUE_KEY, Integer.toString(id));
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        params.addValue("population", population);
        final int updated = jdbcTemplate.update("update country set population=:population where id=:id", params);
        return updated;
    }

    @Transactional
    public int updateCustomTypeColumn(int id, String countryType) {
        MDC.put(MDCKeySupplier.UNIQUE_KEY, Integer.toString(id));
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        params.addValue("country_type", countryType);
        final int updated = jdbcTemplate.update("update country set country_type=:country_type::my_country_type where id=:id", params);
        return updated;
    }

    @Transactional
    public int upsert(int id, String name, int population) {
        MDC.put(MDCKeySupplier.UNIQUE_KEY, Integer.toString(id));
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        params.addValue("name", name);
        params.addValue("description", name);
        params.addValue("population", population);
        params.addValue("country_type", "FULL_RECOGNIZED");
        final int updated = jdbcTemplate.update("insert into country (id, name, population, country_type)" +
            " values (:id, :name, :population, :country_type::my_country_type)" +
                " on conflict (id) do update set name =:name, population=:population", params);
        return updated;
    }

    @Transactional
    public int multiUpdate(int population, List<Integer> ids) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("population", population);
        params.addValue("ids", ids);
        final int updated = jdbcTemplate.update("update country set population=:population where id IN (:ids)", params);
        return updated;
    }

    @Transactional
    public void multiUpdateWithResultSetProcessing(int population, List<Integer> ids) {
        log.info("multiUpdateWithResultSetProcessing start");
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("population", population);
        params.addValue("ids", ids);
        jdbcTemplate.query("update country set population=:population where id IN (:ids) returning id,name,population",
            params,
            (rs) -> {
                int pop = rs.getInt("population");
                log.info("Result set row {}", pop);
            });
        log.info("multiUpdateWithResultSetProcessing finish");
    }

    @Transactional
    public int delete() {
        final int updated = jdbcTemplate.update("delete from country", new MapSqlParameterSource());
        return updated;
    }
}
