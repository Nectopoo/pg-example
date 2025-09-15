package test.dev.smartreplication;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import test.dev.smartreplication.service.CountryService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Slf4j
@SpringBootApplication
public class ProducerEtcdFallbackMain {
    public static void main(String[] args) throws IOException {
        log.info("Main start");
        final ConfigurableApplicationContext context = SpringApplication.run(ProducerEtcdFallbackMain.class);
        final CountryService countryService = context.getBean(CountryService.class);
        countryService.delete();

        int population = 97_340_000;
        countryService.save(5, "vijetnam", population++);

        System.out.println("Press <Enter> for next update operation");

        try (InputStreamReader inputStreamReader = new InputStreamReader(System.in);
             BufferedReader br = new BufferedReader(inputStreamReader)) {

            while (br.readLine() != null) {
                try {
                    log.info("countryService.update begin... ");
                    countryService.update(5, population++);
                    log.info("countryService.update finished");
                } catch (Exception e) {
                    log.info("countryService.update failed", e);
                }
            }

        }
    }
}
