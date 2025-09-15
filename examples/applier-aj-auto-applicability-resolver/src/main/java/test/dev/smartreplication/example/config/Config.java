package test.dev.smartreplication.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import test.dev.smartreplication.core.kafka.consumer.ChangeApplicability;
import test.dev.smartreplication.core.kafka.consumer.ChangeApplicabilityStatus;
import test.dev.smartreplication.model.Change;
import test.dev.smartreplication.model.ChangeItem;
import test.dev.smartreplication.model.ChangeSource;
import test.dev.smartreplication.version.ChangeApplicabilityResolver;

import java.util.Map;

@Configuration
public class Config {
    final Logger logger = LoggerFactory.getLogger(Config.class);

    /*
     * Реализация идемпотентности.
     */
    @Bean
    public ChangeApplicabilityResolver<Change> applicabilityResolver(
        @Qualifier("mainJdbcTemplate") NamedParameterJdbcTemplate mainJdbcTemplate,
        @Qualifier("standInJdbcTemplate") NamedParameterJdbcTemplate standInJdbcTemplate
    ) {
        return change -> {
            logger.warn("My idempotence check {}", change);

            NamedParameterJdbcTemplate toApply = change.getChangeSource() == ChangeSource.MAIN ? standInJdbcTemplate : mainJdbcTemplate;

            ChangeItem changeItem = change.getPayload().get(0);
            return switch (changeItem.getOperation()) {
                case UPDATE -> processUpdate(changeItem, toApply);
                case INSERT -> processInsert(changeItem, toApply);
                case DELETE, SEQUENCE, NEXTVAL, UPSERT, STATEMENT ->
                    new ChangeApplicability(ChangeApplicabilityStatus.APPLY);
            };

        };
    }

    private ChangeApplicability processUpdate(ChangeItem changeItem, NamedParameterJdbcTemplate jdbcTemplate) {
        if (changeItem.getTableId().getName().equals("country")) {
            Object id = changeItem.getObjectId().get("id");
            // если страны еще нет - отправляем в ретрай
            if (!isExists(jdbcTemplate, "SELECT id FROM country WHERE id = :id", id)) {
                return new ChangeApplicability(ChangeApplicabilityStatus.RETRY, String.format("Страны не существует[%s]", id));
            }
            return new ChangeApplicability(ChangeApplicabilityStatus.APPLY);
        } else if (changeItem.getTableId().getName().equals("users")) {
            Object id = changeItem.getObjectId().get("id");

            // если пользователя еще нет - отправляем в ретрай
            if (!isExists(jdbcTemplate, "SELECT id FROM users WHERE id = :id", id)) {
                return new ChangeApplicability(ChangeApplicabilityStatus.RETRY, String.format("Пользователя не существует[%s]", id));
            }
            Object countryId = changeItem.getNewValue().get("country_id");
            // если страны еще нет - отправляем в ретрай
            if (!isExists(jdbcTemplate, "SELECT id FROM country WHERE id = :id", countryId)) {
                return new ChangeApplicability(ChangeApplicabilityStatus.RETRY, String.format("Страны не существует[%s]", countryId));
            }

            return new ChangeApplicability(ChangeApplicabilityStatus.APPLY);
        } else {
            return new ChangeApplicability(ChangeApplicabilityStatus.SKIP);
        }
    }

    private ChangeApplicability processInsert(ChangeItem changeItem, NamedParameterJdbcTemplate jdbcTemplate) {
        if (changeItem.getTableId().getName().equals("country")) {
            // если страны еще нет - разрешаем применение
            return new ChangeApplicability(
                Boolean.TRUE.equals(
                    isExists(jdbcTemplate, "SELECT id FROM country WHERE id = :id", changeItem.getObjectId().get("id")))
                    ? ChangeApplicabilityStatus.SKIP
                    : ChangeApplicabilityStatus.APPLY);
        } else if (changeItem.getTableId().getName().equals("users")) {
            Object id = changeItem.getObjectId().get("id");
            // если пользователя еще нет - переходим к след проверке
            if (isExists(jdbcTemplate, "SELECT id FROM users WHERE id = :id", id)) {
                return new ChangeApplicability(ChangeApplicabilityStatus.SKIP, String.format("Пользователь уже существует[%s]", id));
            }
            Object countryId = changeItem.getNewValue().get("country_id");
            // если страны еще нет - переводим в RETRY
            if (!isExists(jdbcTemplate, "SELECT id FROM country WHERE id = :id", countryId)) {
                return new ChangeApplicability(ChangeApplicabilityStatus.RETRY, String.format("Страны не существует[%s]", countryId));
            }

            return new ChangeApplicability(ChangeApplicabilityStatus.APPLY);
        }

        return new ChangeApplicability(ChangeApplicabilityStatus.SKIP);
    }

    private Boolean isExists(NamedParameterJdbcTemplate jdbcTemplate, String query, Object id) {
        return jdbcTemplate.query(
            query,
            Map.of("id", id), rs -> {
                if (rs.next()) {
                    return rs.getObject(1) != null;
                }
                return false;
            }
        );
    }

}
