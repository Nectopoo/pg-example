package test.dev.demo.business.application.scheduler;

import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import test.dev.demo.business.application.service.ExchangeRateService;

/**
 * Метод вызывается quartz по расписанию и сохраняет новый курс валют.
 */
@Component
@RequiredArgsConstructor
public class ExchangeRateJob implements Job {
    private final Logger logger = LoggerFactory.getLogger(ExchangeRateJob.class);
    private final ExchangeRateService jobService;

    @Override
    public void execute(JobExecutionContext context) {
        var exchangeRate = jobService.saveNewExchangeRate();
        logger.info("Курс доллара: {}  Курс евро {}", exchangeRate.getExchangeRateId().getDollar(), exchangeRate.getExchangeRateId().getEuro());
    }
}
