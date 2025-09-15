package test.dev.demo.business.application.config;

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import test.dev.demo.business.application.scheduler.ExchangeRateJob;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Конфигурация для Quartz.
 */
@Configuration
@ConditionalOnExpression("'${using.spring.schedulerFactory}'=='true'")
public class QuartzSchedulerConfiguration {
    @Bean
    public JobDetail jobDetail() {
        return newJob()
                .ofType(ExchangeRateJob.class)
                .storeDurably()
                .withIdentity(JobKey.jobKey("Exchange_Rate_Job"))
                .withDescription("Update the exchange rate")
                .build();
    }

    @Bean
    public Trigger trigger(JobDetail job) {
        int frequencyInMinutes = 30;
        return newTrigger()
                .forJob(job)
                .withIdentity(TriggerKey.triggerKey("Quartz_Trigger"))
                .withDescription("Exchange Rate trigger")
                .withSchedule(simpleSchedule().withIntervalInMinutes(frequencyInMinutes).repeatForever())
                .build();
    }
}
