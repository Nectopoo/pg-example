package test.dev.smartreplication.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import test.dev.smartreplication.core.kafka.consumer.ChangeApplicability;
import test.dev.smartreplication.core.kafka.consumer.ChangeApplicabilityStatus;
import test.dev.smartreplication.model.Change;
import test.dev.smartreplication.model.ChangeSource;
import test.dev.smartreplication.version.ChangeApplicabilityResolver;

import javax.sql.DataSource;

@Configuration
public class Config {
    final Logger logger = LoggerFactory.getLogger(Config.class);

    /*
     * Реализация обработчика применимости вектора изменения. Позволяет исключить повторную обработку данных.
     */

    @Bean
    public ChangeApplicabilityResolver<Change> changeApplicabilityResolver(@Qualifier("mainDatasource") DataSource mainDataSource,
                                                                           @Qualifier("standinDatasource") DataSource secondaryDataSource) {
        return change -> {
            logger.warn("My idempotence check {}", change);
            DataSource toApply = change.getChangeSource() == ChangeSource.MAIN ? secondaryDataSource : mainDataSource;
            // написать реализацию


            logger.warn("My idempotence success for Change {}", change);
            // change допущен к сохранению в БД
            return new ChangeApplicability(ChangeApplicabilityStatus.APPLY);
        };
    }
}
