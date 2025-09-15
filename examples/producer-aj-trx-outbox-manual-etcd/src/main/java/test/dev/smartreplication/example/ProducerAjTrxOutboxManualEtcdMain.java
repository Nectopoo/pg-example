package test.dev.smartreplication.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import test.dev.smartreplication.example.service.CountryService;

@SpringBootApplication
public class ProducerAjTrxOutboxManualEtcdMain {
    public static void main(String[] args) {
        final ConfigurableApplicationContext context = SpringApplication.run(ProducerAjTrxOutboxManualEtcdMain.class);
        final CountryService countryService = context.getBean(CountryService.class);
        countryService.save(44, "tanzania", 60_000_000);
        countryService.update(44, 60_000_001);
    }
}
