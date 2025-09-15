package test.dev.smartreplication.example.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import test.dev.smartreplication.api.dto.ChangeItem;
import test.dev.smartreplication.api.dto.ChangeOperation;
import test.dev.smartreplication.api.dto.ProviderType;
import test.dev.smartreplication.api.dto.TableId;
import test.dev.smartreplication.core.provider.ChangeProvider;

@Service
public class CountryService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final ChangeProvider changeProvider;

    public CountryService(JdbcTemplate jdbcTemplate, ChangeProvider changeProvider) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        ;
        this.changeProvider = changeProvider;
    }

    @Transactional
    public int save(int id, String name, int population) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        params.addValue("name", name);
        params.addValue("population", population);

        final int updated = jdbcTemplate.update("insert into country (id, name, population) " +
                        "values (:id, :name, :population)",
                params);

        changeProvider
                .getCurrentBuilder(ProviderType.SMART_REPLICATION)
                .setChangeKey(String.valueOf(id))
                .addChangeItem(ChangeItem.newBuilder(ChangeOperation.INSERT, TableId.of("country"), 1)
                        .addObjectId("id", id) // первичный ключ
                        .addNewValue("id", id)
                        .addNewValue("name", name)
                        .addNewValue("population", population));
        return updated;
    }

    @Transactional
    public int update(int id, int population) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        params.addValue("population", population);
        final int updated = jdbcTemplate.update("update country set population=:population where id=:id",
                params);
        changeProvider.getCurrentBuilder(ProviderType.SMART_REPLICATION)
                .setChangeKey(String.valueOf(id))
                .addChangeItem(ChangeItem.newBuilder(ChangeOperation.UPDATE, TableId.of("country"), 1)
                        .addObjectId("id", id) // первичный ключ
                        .addNewValue("population", population));
        return updated;
    }

    @Transactional
    public int delete(int id) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        final int updated = jdbcTemplate.update("delete from country where id=:id",
                params);
        changeProvider.getCurrentBuilder(ProviderType.SMART_REPLICATION)
                .setChangeKey(String.valueOf(id))
                .addChangeItem(ChangeItem.newBuilder(ChangeOperation.DELETE, TableId.of("country"), 1)
                        .addObjectId("id", id));
        return updated;
    }
}
