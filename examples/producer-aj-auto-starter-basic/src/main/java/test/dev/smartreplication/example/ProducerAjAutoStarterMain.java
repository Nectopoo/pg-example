package test.dev.smartreplication.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import test.dev.smartreplication.example.entity.User;
import test.dev.smartreplication.example.repository.UserRepository;
import test.dev.smartreplication.example.service.CountryService;

import java.time.Instant;

@Slf4j
@SpringBootApplication
public class ProducerAjAutoStarterMain {

    public static void main(String[] args) {
        log.info("Main start");
        final ConfigurableApplicationContext context = SpringApplication.run(ProducerAjAutoStarterMain.class);

        // Пример jdbc
        final CountryService countryService = context.getBean(CountryService.class);
        countryService.save(7698, "Nepal", 29_690_000);
        countryService.update(7698, 29_690_001);
        countryService.delete();

        // Пример jpa
        final UserRepository userRepository = context.getBean(UserRepository.class);
        final User user = new User();
        user.setEmail("testuser@test");
        user.setUsername("testuser");
        user.setCreateDate(Instant.now());
        User savedUser = userRepository.save(user);
        savedUser.setUsername("newtestuser@test");
        userRepository.save(savedUser);
        userRepository.delete(user);

        log.info("Main finish");
    }
}
