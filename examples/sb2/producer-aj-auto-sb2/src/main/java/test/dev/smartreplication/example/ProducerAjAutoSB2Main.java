package test.dev.smartreplication.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import test.dev.smartreplication.example.entity.Address;
import test.dev.smartreplication.example.entity.User;
import test.dev.smartreplication.example.properties.SmartReplicationProperties;
import test.dev.smartreplication.example.repository.UserRepository;
import test.dev.smartreplication.example.service.CountryService;
import test.dev.smartreplication.example.service.UserService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@SpringBootApplication
public class ProducerAjAutoSB2Main {
    public static void main(String[] args) throws Exception {
        log.info("Main start");
        final ConfigurableApplicationContext context = SpringApplication.run(ProducerAjAutoSB2Main.class);
        final CountryService countryService = context.getBean(CountryService.class);
        countryService.delete();

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

        final UserRepository userRepository = context.getBean(UserRepository.class);
        final User user = new User();
        user.setEmail("testuser@test");
        user.setUsername("testuser");
        user.setCreateDate(Instant.now());
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
        } catch (NoSuchBeanDefinitionException e) {
            log.info("Smart replication is disabled");
        }

        // Пример конфликтующих транзакций
        // concurrentTxTest(context.getBean(UserService.class));

        log.info("Main finish");
    }

    /**
     * Демонстрация, что из двух параллельных транзакций, если одна из них завершилась с ошибкой,
     * то в БД запишется только одна и в kafka отправится тоже только одна (таже, что и в БД).
     */
    private static void concurrentTxTest(UserService userService) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            final Future<?> submitted1 = executorService.submit(() -> {
                final User user = new User();
                user.setEmail("alice@test");
                user.setUsername("testuser");
                user.setCreateDate(Instant.now());
                final HashSet<Address> addresses = new HashSet<>();
                addresses.add(Address.builder()
                    .id(2L)
                    .postalCode("1112")
                    .build());
                user.setAddresses(addresses);
                try {
                    userService.txParallelTest(user, latch);
                } catch (InterruptedException e) {
                    log.error("Save user fail", e);
                }
            });
            final Future<?> submitted2 = executorService.submit(() -> {
                final User user = new User();
                user.setEmail("bob@test");
                user.setUsername("testuser");
                user.setCreateDate(Instant.now());
                final HashSet<Address> addresses = new HashSet<>();
                addresses.add(Address.builder()
                    .id(2L)
                    .postalCode("1112")
                    .build());
                user.setAddresses(addresses);
                try {
                    userService.txParallelTest(user, latch);
                } catch (InterruptedException e) {
                    log.error("Save user fail", e);
                }
            });
            latch.countDown();
            submitted1.get();
            submitted2.get();
        } finally {
            executorService.shutdown();
        }
    }

    private static void createUsers(UserService userService, int count) {
        for (int i = 0; i < count; i++) {
            userService.save();
        }
    }
}
