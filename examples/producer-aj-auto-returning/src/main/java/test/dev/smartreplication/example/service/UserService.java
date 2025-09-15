package test.dev.smartreplication.example.service;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import test.dev.smartreplication.example.entity.User;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.Optional;

@Slf4j
@Service
public class UserService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UserService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    /**
     * Пример INSERT с RETURNING
     * ПЖ не будет менять порядок полей в returning.
     * Первым будет email
     */
    @Transactional
    public User createUser() {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("username", "username_" + RandomStringUtils.random(10, true, false));
        params.addValue("email", "email@" + RandomStringUtils.random(30, true, false));
        params.addValue("amount", new BigDecimal(RandomUtils.nextLong()));
        var user = jdbcTemplate.query("INSERT INTO users (username, email, amount)" +
            " VALUES (:username, :email, :amount) RETURNING email, create_date", params, UserService::map);
        return Optional.ofNullable(user).orElseThrow();

    }

    /**
     * Пример UPDATE с RETURNING
     * ПЖ не будет менять порядок полей в returning.
     * Первым будет email
     */
    @Transactional
    public User updateUser(Long id) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        params.addValue("update_email", "_updated_" + LocalTime.now());
        return jdbcTemplate.query("UPDATE users SET email=CONCAT(email,:update_email) WHERE id=:id RETURNING email, username, amount, create_date", params, UserService::map);
    }

    @Transactional
    public String deleteUser(Long id) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", id);
        return jdbcTemplate.query("DELETE FROM users WHERE id=:id RETURNING username", params, (ResultSetExtractor<String>) rs -> {
            if (!rs.next()) {
                throw new IllegalArgumentException(String.format("User[%s] not found.", id));
            }
            return rs.getString(1);
        });

    }

    private static User map(@NonNull ResultSet rs) {
        try {
            if (!rs.next()) {
                return null;
            }
            assert rs.getString(1).startsWith("email@");
            var user = new User();
            user.setId(rs.getLong("id"));
            user.setUsername(rs.getString("username"));
            user.setEmail(rs.getString("email"));
            user.setCreateDate(Optional.ofNullable(rs.getTimestamp("create_date")).map(Timestamp::toInstant).orElse(null));
            user.setAmount(rs.getBigDecimal("amount"));
            return user;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


}
