package test.dev.smartreplication.example;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import test.dev.smartreplication.example.entity.RuPayment;
import test.dev.smartreplication.example.repository.RuPaymentRepository;
import test.dev.smartreplication.example.service.RuPaymentService;
import test.dev.smartreplication.example.service.TestService;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@SpringBootApplication
public class ProducerAjAutoHikariMain {
    public static void main(String[] args) {
        log.info("Main start");
        final ConfigurableApplicationContext context = SpringApplication.run(ProducerAjAutoHikariMain.class);
//        testService(context);
        testCascadeDelete(context, 1000);
        log.info("Main finish");
    }

    private static void testService(ConfigurableApplicationContext context) {
        final TestService testService = context.getBean(TestService.class);

        log.info("Init start " + Instant.now());
        for (int i = 1; i <= 25_000; i++) {
            testService.save(100);
        }
        long id = testService.getStartIdBefore(25_000);
        log.info("Init finish " + Instant.now());
        //        Instant start = Instant.now();
        ////        for (int i = 0; i < 1000; i++) {
        //            testService.select(100, IntStream.rangeClosed(5000, 15000).boxed().collect(Collectors.toList()));
        ////        }
        //        log.info("Select duration: " + Duration.between(start, Instant.now()));
        testDeleteWhere(testService, id);
    }

    /**
     * 1. Удаление 25_000 записей
     * без ПЖ 1 сек
     * 2. Удаление 25_000 по id c returning
     * без ПЖ порядка 1 сек - не намного дольше чем без returning может 100 мс
     */
    private static void testDeleteWhere(TestService testService, long fromId) {
//        testService.deleteWhere(fromId);
        testService.deleteWhereWithReturning(fromId);
    }

    /**
     * По задаче OMNI13-5043.
     */
    private static void testCascadeDelete(ConfigurableApplicationContext context, int deleteCount) {
        final RuPaymentRepository repo = context.getBean(RuPaymentRepository.class);
        final RuPaymentService service = context.getBean(RuPaymentService.class);
        List<RuPayment> ruPayments = createPayments(deleteCount, repo);
        log.info("Before delete {}", deleteCount);
//        repo.deleteAll(ruPayments);
        OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(Instant.now().plus(1, ChronoUnit.DAYS), ZoneId.of("Europe/Moscow"));
        log.info("Date time delete criteria {}", offsetDateTime);
        service.deleteByCreateDateBeforeViaStatement(offsetDateTime);
        log.info("After delete {}", deleteCount);
    }

    private static List<RuPayment> createPayments(int count, RuPaymentRepository repo) {
        final ArrayList<RuPayment> ruPayments = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final RuPayment savedPay = savePay(repo);
            ruPayments.add(savedPay);
        }
        return ruPayments;
    }

    private static RuPayment savePay(RuPaymentRepository repo) {
        final RuPayment ruPayment = new RuPayment();
        ruPayment.setFraudExternalIpAddress(generateStr());
        ruPayment.setFraudInternalIpAddress(generateStr());
        ruPayment.setFraudMacAddress(generateStr());
        ruPayment.setFraudState(generateStr());
        ruPayment.setOperationType(generateStr());
        ruPayment.setPaymentCode(generateStr());
        ruPayment.setPaymentGroundDescription(generateStr());
        ruPayment.setPaymentGroundNdsCalculation(generateStr());
        ruPayment.setPaymentGroundNdsPercent(generateStr());
        ruPayment.setPaymentGroundOperationCode(generateStr());
        ruPayment.setCreateDate(OffsetDateTime.ofInstant(Instant.now(), ZoneId.of("Europe/Moscow")));
        final RuPayment savedPay = repo.save(ruPayment);
        return savedPay;
    }

    public static String generateStr() {
        return RandomStringUtils.random(250, true, true);
    }
}
