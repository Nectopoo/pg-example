package test.dev.smartreplication.example.service;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import test.dev.smartreplication.core.provider.ChangeProvider;
import test.dev.smartreplication.example.entity.User;
import test.dev.smartreplication.example.repository.UserRepository;
import test.dev.smartreplication.example.utils.Utils;
import test.dev.smartreplication.model.Change;
import test.dev.smartreplication.model.ChangeItem;
import test.dev.smartreplication.model.ChangeOperation;
import test.dev.smartreplication.model.ProviderType;
import test.dev.streaming.core.ChangeReplicationMode;
import test.dev.streaming.mdc.MDCKeySupplier;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Optional;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    // на случай отключения ПЖ
    private final ChangeProvider changeProvider;

    public UserService(UserRepository userRepository, JdbcTemplate jdbcTemplate, @Nullable ChangeProvider changeProvider) {
        this.userRepository = userRepository;
        this.jdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
        this.changeProvider = changeProvider;
    }

    @Transactional
    public User createRandomUserByJpa() {
        final User user = new User();
        user.setEmail("testuser@test");
        user.setUsername("testuser");
        user.setCreateDate(Instant.now());
        user.setWithTimeZone(Instant.now());
        user.setAmount(new BigDecimal(RandomUtils.nextLong()));
        return userRepository.save(user);
    }

    @Transactional
    public User saveByJpa(User user) {
        return userRepository.save(user);
    }

    /**
     * Пример работы флага MDCKeySupplier.CHANGE_REPLICATION_MODE, {@link ChangeReplicationMode.NONE}.
     * При таком флаге сообщение для kafka НЕ будет сформировано автоматически.
     * В этом примере сформирована вручную структура Change(сообщение для kafka) типа STATEMENT.
     */
    @Transactional
    public int deleteByCreateDateBeforeViaManual(Instant date) {
        // флаг работает до конца транзакции, сообщение в kafka не будет сформировано автоматически
        MDC.put(MDCKeySupplier.CHANGE_REPLICATION_MODE, ChangeReplicationMode.NONE.name());
        if (changeProvider != null) {
            final LinkedHashMap<Integer, Object> params = new LinkedHashMap<>();
            params.put(1, Timestamp.from(date));
            final ChangeItem.ChangeItemBuilder changeItem = ChangeItem.newBuilder(ChangeOperation.STATEMENT, null, 0)
                    .addAdditionalProperty(ChangeItem.ADDITIONAL_PROPERTY_QUERY, "delete from users where create_date < ?")
                    .addAdditionalProperty(ChangeItem.ADDITIONAL_PROPERTY_PARAMS, params);
            final Change.ChangeBuilder changeBuilder = changeProvider.getCurrentBuilder(ProviderType.SMART_REPLICATION);
            changeBuilder.addChangeItem(
                    changeItem
            );
        }
        return userRepository.deleteByCreateDateBefore(date);
    }

    /**
     * Пример работы флага MDCKeySupplier.CHANGE_REPLICATION_MODE, {@link ChangeReplicationMode.STATEMENT}.
     * При таком флаге сообщение для kafka будет сформировано автоматически.
     * Statement передается как есть: query, params.
     */
    @Transactional
    public int deleteByCreateDateBeforeViaStatement(Instant date) {
        // флаг работает до конца транзакции, сообщение типа STATEMENT в kafka будет сформировано автоматически
        MDC.put(MDCKeySupplier.CHANGE_REPLICATION_MODE, ChangeReplicationMode.STATEMENT.name());
        return userRepository.deleteByCreateDateBefore(date);
    }

    /**
     * Пример INSERT с RETURNING
     * ПЖ перезапишет набор полей returning на все поля.
     * Будет returning id,username,email,create_date,amount
     */
    @Transactional
    public Long createUser() {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("username", "username_" + RandomStringUtils.random(10, true, false));
        params.addValue("email", "email@" + RandomStringUtils.random(30, true, false));
        params.addValue("amount", new BigDecimal(RandomUtils.nextLong()));
        var user = jdbcTemplate.query("INSERT INTO users (username, email, amount)" +
            " VALUES (:username, :email, :amount) RETURNING email, create_date", params, UserService::map);
        return Optional.ofNullable(user).map(User::getId).orElseThrow();

    }

    /**
     * Пример UPDATE с RETURNING
     * ПЖ перезапишет набор полей returning на поле id и изменяемые поля.
     * Будет returning id,email
     */
    @Transactional
    public User updateUser(Long id) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        params.addValue("update_email", "_updated_" + LocalTime.now());
        return jdbcTemplate.query("UPDATE users SET email=CONCAT(email,:update_email) WHERE id=:id RETURNING id, email, create_date", params, UserService::map);
    }

    private static User map(@NonNull ResultSet rs) {
        try {
            if (!rs.next()) {
                return null;
            }
            var user = new User();
            user.setId(rs.getLong("id"));
            if (Utils.hasColumn(rs, "username")) {
                user.setUsername(rs.getString("username"));
            }
            if (Utils.hasColumn(rs, "email")) {
                user.setEmail(rs.getString("email"));
            }
            if (Utils.hasColumn(rs, "create_date"))
                user.setCreateDate(Optional.ofNullable(rs.getTimestamp("create_date")).map(Timestamp::toInstant).orElse(null));
            if (Utils.hasColumn(rs, "amount")) {
                user.setAmount(rs.getBigDecimal("amount"));
            }
            return user;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
