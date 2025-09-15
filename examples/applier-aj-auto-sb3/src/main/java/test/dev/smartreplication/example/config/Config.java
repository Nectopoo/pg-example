package test.dev.smartreplication.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import test.dev.smartreplication.core.kafka.consumer.ChangeHandleResult;
import test.dev.smartreplication.model.Change;
import test.dev.smartreplication.model.ChangeSource;
import test.dev.smartreplication.version.IdempotenceStrategy;

import javax.sql.DataSource;

@Configuration
public class Config {
    final Logger logger = LoggerFactory.getLogger(Config.class);

    /*
     * Реализация идемпотентности.
     */
    @Bean
    public IdempotenceStrategy<Change, ChangeHandleResult> idempotenceStrategy(
        @Qualifier("mainDatasource") DataSource mainDataSource,
        @Qualifier("standinDatasource") DataSource secondaryDataSource
    ) {
        return new IdempotenceStrategy<>() {
            /**
             * Пример реализации стратегии идемпотентности
             * @param change входящее сообщение из кафка
             * @return сохранять ли изменения в БД
             */
            @Override
            public boolean check(final Change change) {
                logger.warn("My idempotence check {}", change);
                DataSource toApply = change.getChangeSource() == ChangeSource.MAIN ? secondaryDataSource : mainDataSource;
                //todo написать реализацию
                return true; // change допущен к сохранинию в БД
            }

            /**
             * Метод вызывается, если check вернул false
             */
            @Override
            public ChangeHandleResult apply(final Change change) {
                logger.warn("My idempotence success for Change {}", change);
                return ChangeHandleResult.success();
            }
        };
    }
}
