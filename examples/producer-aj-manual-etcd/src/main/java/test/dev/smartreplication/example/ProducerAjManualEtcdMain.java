package test.dev.smartreplication.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import test.dev.smartreplication.example.service.CountryService;

import java.util.concurrent.ExecutionException;

@SpringBootApplication
public class ProducerAjManualEtcdMain {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        final ConfigurableApplicationContext context = SpringApplication.run(ProducerAjManualEtcdMain.class);
        final CountryService countryService = context.getBean(CountryService.class);
        countryService.delete(4);
        countryService.save(4, "kenya", 53_770_000);
        countryService.update(4, 53_770_001);
    }
}
