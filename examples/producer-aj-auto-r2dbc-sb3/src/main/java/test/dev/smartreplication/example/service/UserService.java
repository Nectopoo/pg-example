package test.dev.smartreplication.example.service;

import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import test.dev.smartreplication.example.entity.User;

@Service
@RequiredArgsConstructor
public class UserService {

    private final DatabaseClient databaseClient;

    @Transactional
    public Mono<User> create(User user) {
        return databaseClient.sql("insert into users(username, email) values(:username, :email)")
            .filter((statement, executeFunction) -> statement.returnGeneratedValues("id").execute())
            .bind("username", user.getUsername())
            .bind("email", user.getEmail())
            .fetch()
            .first()
            .map(row -> User.builder()
                .id((Long) row.get("id"))
                .username(user.getUsername())
                .email(user.getEmail())
                .build());
    }

    public Mono<User> findById(long id) {
        return databaseClient.sql("select * from users where id=$1")
            .bind("$1", id)
            .map(row -> User.builder()
                .id((Long) row.get("id"))
                .username((String) row.get("username"))
                .email((String) row.get("email"))
                .build())
            .one();
    }

    public Flux<User> findAll() {
        return this.databaseClient
            .sql("select * from users")
            .map(row -> User.builder()
                .id((Long) row.get("id"))
                .username((String) row.get("username"))
                .email((String) row.get("email"))
                .build())
            .all();
    }

    @Transactional
    public Mono<User> updateUsername(Long id, String username) {
        return this.databaseClient.sql("update users set username=$1 where id=$2")
            .bind("$1", username)
            .bind("$2", id)
            .fetch()
            .first()
            .map(row -> User.builder()
                .id((Long) row.get("id"))
                .username((String) row.get("username"))
                .email((String) row.get("email"))
                .build());

    }

    @Transactional
    public Mono<Object> delete(Long id) {
        return this.databaseClient.sql("delete from users where id=$1")
            .bind("$1", id)
            .fetch()
            .rowsUpdated()
            .flatMap(rows -> rows == 0 ? Mono.error(new RuntimeException("Issue not found")) :  Mono.empty());
    }

}
