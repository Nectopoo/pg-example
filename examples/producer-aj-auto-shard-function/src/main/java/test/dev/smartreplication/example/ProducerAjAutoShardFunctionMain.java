package test.dev.smartreplication.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import test.dev.smartreplication.example.service.CountryService;


@Slf4j
@SpringBootApplication
public class ProducerAjAutoShardFunctionMain {

    public static void main(String[] args) throws InterruptedException {
        log.info("Main start");
        final ConfigurableApplicationContext context = SpringApplication.run(ProducerAjAutoShardFunctionMain.class);
        final CountryService bean = context.getBean(CountryService.class);
//        bean.save("Austria", 50);
//        bean.save("Bolivia", 101);
        
        int i = 0;
        while(true) {
            bean.save(i + "-country", 200);
            log.info("Saved Country-" + i);
            i++;
            Thread.sleep(3000);
        }
//        log.info("Main finish");
    }
}
