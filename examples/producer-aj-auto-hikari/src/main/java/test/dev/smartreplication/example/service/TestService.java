package test.dev.smartreplication.example.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import test.dev.streaming.mdc.MDCKeySupplier;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class TestService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TestService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Transactional
    public int save(int number) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder queryColumnsBuilder = new StringBuilder();
        StringBuilder queryValuesBuilder = new StringBuilder();
        for (int i = 1; i <=number; i++) {
            params.addValue("column_varchar" + i, "column_varchar" + i + "_value");
            queryColumnsBuilder.append("column_varchar" + i + ",");
            queryValuesBuilder.append(":column_varchar" + i + ",");
        }
        queryColumnsBuilder.deleteCharAt(queryColumnsBuilder.lastIndexOf(","));
        queryValuesBuilder.deleteCharAt(queryValuesBuilder.lastIndexOf(","));
        
        final int updated = jdbcTemplate.update("insert into test (" + queryColumnsBuilder + ")" +
                        " values (" + queryValuesBuilder + ")", params);
        return updated;
    }

    @Transactional
    public void select(int columnNumber, List<Integer> ids) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ids", ids);

        StringBuilder queryColumnsBuilder = new StringBuilder();
        for (int i = 1; i <=columnNumber; i++) {
            queryColumnsBuilder.append("column_varchar" + i + ",");
        }
        queryColumnsBuilder.deleteCharAt(queryColumnsBuilder.lastIndexOf(","));

        SqlRowSet sqlRowSet = jdbcTemplate.queryForRowSet("select " + queryColumnsBuilder + " from test where id IN (:ids)", params);
//        while(sqlRowSet.next())
//        {
//            String columnVarchar1 = sqlRowSet.getString("column_varchar1");
//            System.out.println("columnVarchar1: " + columnVarchar1);
//            System.out.println("---");
//        }
    }

    @Transactional
    public int delete() {
        final int updated = jdbcTemplate.update("delete from test", new MapSqlParameterSource());
        return updated;
    }

    @Transactional
    public int deleteWhere(long fromId) {
        log.info("Delete where start");
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("idValue", fromId);
        final int updated = jdbcTemplate.update("delete from test where id > :idValue", parameterSource);
        log.info("Delete where finish, deleted = {}", updated);
        return updated;
    }

    @Transactional
    public int deleteWhereWithReturning(long fromId) {
        log.info("Delete where start");
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("idValue", fromId);
        final SqlRowSet sqlRowSet = jdbcTemplate.queryForRowSet("delete from test where id > :idValue returning id", parameterSource);
        int updated = 0;
        while (sqlRowSet.next()){
            updated ++;
            final long id = sqlRowSet.getLong("id");
            log.trace("Deleted id = {}", id);
        }
        log.info("Delete where finish, deleted = {}", updated);
        return updated;
    }

    public long getStartIdBefore(long count) {
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("count", count);
        final SqlRowSet sqlRowSet = jdbcTemplate.queryForRowSet("select id from test order by id desc offset :count limit 1", parameterSource);
        sqlRowSet.next();
        return sqlRowSet.getLong("id");
    }
}
