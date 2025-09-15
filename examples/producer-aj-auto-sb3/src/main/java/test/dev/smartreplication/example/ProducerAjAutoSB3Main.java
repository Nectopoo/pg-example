package test.dev.smartreplication.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import test.dev.smartreplication.example.entity.User;
import test.dev.smartreplication.example.properties.SmartReplicationProperties;
import test.dev.smartreplication.example.repository.UserRepository;
import test.dev.smartreplication.example.service.CountryService;
import test.dev.smartreplication.example.service.UserService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@SpringBootApplication
public class ProducerAjAutoSB3Main {
    public static void main(String[] args) {
        log.info("Main start");
        final ConfigurableApplicationContext context = SpringApplication.run(ProducerAjAutoSB3Main.class);
        final CountryService countryService = context.getBean(CountryService.class);
        countryService.delete();

        /* Пример вставки нескольких записей одним multi-value insert.
        List<Country> countries = countryService.insertCountriesByMultiValueInsert(10);
        log.info("Countries inserted: {}", countries);
        countryService.delete();
        */

        // Пример для аварийного отключения
        boolean isSuccess = false;
        while (!isSuccess) {
            try {
                countryService.save(5, "vijetnam", 97_340_000);
                countryService.update(5, 97_340_001);
                isSuccess = true;
            } catch (Exception e) {
                log.error("Exception", e);
            }
        }

        // Пример update для CustomType
        countryService.updateCustomTypeColumn(5, "PARTIALLY_RECOGNIZED");

        countryService.save("Japan", 125_950_000);
        // Пример операции upsert
        countryService.upsert(5, "vijetnam", 97_340_002);

        // Пример массового обновления строк в таблице в рамках одного запроса
        countryService.save(6, "india", 1_380_000_000);
        countryService.save(7, "china", 1_402_000_000);
        countryService.multiUpdate(1_500_000_000, List.of(6, 7));
        countryService.multiUpdateWithResultSetProcessing(1_500_000_001, List.of(6, 7));

        // Пример для hibernate
        final UserRepository userRepository = context.getBean(UserRepository.class);
        final User user = new User();
        user.setEmail("testuser@test");
        user.setUsername("testuser");
        user.setCreateDate(Instant.now());
        user.setWithTimeZone(Instant.now());
        user.setAmount(new BigDecimal("87654321.00"));
        User savedUser = userRepository.save(user);
        // Пример update для hibernate
        savedUser.setUsername("newtestuser@test");
        userRepository.save(savedUser);

            // пример CHANGE_REPLICATION_MODE=NONE
            {
            final UserService userService = context.getBean(UserService.class, 10);
            createUsers(userService, 3);
            final int count = userService.deleteByCreateDateBeforeViaManual(Instant.now().plus(1, ChronoUnit.DAYS));
            assert count >= 3;
            }
            // пример CHANGE_REPLICATION_MODE=STATEMENT
            {
            final UserService userService = context.getBean(UserService.class, 10);
            createUsers(userService, 3);
            final int count = userService.deleteByCreateDateBeforeViaStatement(Instant.now().plus(1, ChronoUnit.DAYS));
            assert count >= 3;
            }

        // Пример отключения репликации
        try {
            final SmartReplicationProperties exampleConfigurationProperties = context.getBean(SmartReplicationProperties.class);
            exampleConfigurationProperties.setShouldStopReplication(true);
            countryService.save(8, "Liechtenstein", 38_378);
            exampleConfigurationProperties.setShouldStopReplication(false);
        } catch (NoSuchBeanDefinitionException e) {
            log.info("Smart replication is disabled");
        }
        checkCreateAndUpdateWithReturning(context);
        log.info("Main finish");
    }

    private static void createUsers(UserService userService, int count) {
        for (int i = 0; i < count; i++) {
            userService.createRandomUserByJpa();
        }
    }

    private static void checkCreateAndUpdateWithReturning(ConfigurableApplicationContext context) {
        var countryService = context.getBean(CountryService.class);
        var userService = context.getBean(UserService.class);
        var countryId = countryService.createCountry();
        var updatedCountry = countryService.updateCountry(countryId);
        log.info("updatedCountry={}", updatedCountry);
        var countryByJpa = countryService.createRandomCountryByJpa();
        countryByJpa.setDescription(countryByJpa.getDescription() + "updated_byJpda" + LocalDateTime.now());
        var updatedCountryByJpa = countryService.saveByJpa(countryByJpa);
        log.info("updatedCountryByJpa={}", updatedCountryByJpa);
        var userId = userService.createUser();
        var updatedUser = userService.updateUser(userId);
        log.info("updatedUser={}", updatedUser);
        var userByJpa = userService.createRandomUserByJpa();
        userByJpa.setEmail(userByJpa.getEmail()+ "updated_byJpda"+ LocalDateTime.now());
        var updatedUserByJpa = userService.saveByJpa(userByJpa);
        log.info("updatedUserByJpa={}", updatedUserByJpa);
    }
}
