package test.dev.smartreplication.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import test.dev.smartreplication.example.entity.User;
import test.dev.smartreplication.example.service.UserService;

@Slf4j
@SpringBootApplication
public class ProducerAjAutoReturningMain {
    public static void main(String[] args) {
        log.info("Main start");
        final ConfigurableApplicationContext context = SpringApplication.run(ProducerAjAutoReturningMain.class);
        final UserService userService  = context.getBean(UserService.class);
        User user = userService.createUser();
        userService.updateUser(user.getId());
        final JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
        jdbcTemplate.update("DELETE FROM users");
        log.info("Main finish");
    }

}
