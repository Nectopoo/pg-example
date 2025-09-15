package test.dev.demo.business.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BusinessApp {
    public static void main(String[] args) {
        SpringApplication.run(BusinessApp.class);
    }
}
