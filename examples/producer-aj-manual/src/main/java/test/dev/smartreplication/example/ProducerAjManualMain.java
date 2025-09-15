package test.dev.smartreplication.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import test.dev.smartreplication.example.config.Config;
import test.dev.smartreplication.example.service.CountryService;

@SpringBootApplication
public class ProducerAjManualMain {

    public static void main(String[] args) {
        Config.initializeZookeeperNode();
        final ConfigurableApplicationContext context = SpringApplication.run(ProducerAjManualMain.class);
        final CountryService countryService = context.getBean(CountryService.class);
        countryService.save(1, "russia", 144_000_000);
        countryService.update(1, 144_000_001);
    }
}
