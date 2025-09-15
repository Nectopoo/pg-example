package test.dev.demo.business.application.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

@Configuration
public class FlywayCustomConfig {

    private static final String LOCATIONS = "classpath:db/migration";

    @Autowired
    @Qualifier("mainDataSource")
    private DataSource mainDataSource;
    @Autowired
    @Qualifier("secondaryDataSource")
    private DataSource secondaryDataSource;

    @PostConstruct
    public void migrate() {
        final var mainDbFlywayMigration = Flyway.configure()
                .baselineOnMigrate(true)
                .dataSource(mainDataSource)
                .locations(LOCATIONS)
                .load();
        mainDbFlywayMigration.migrate();
        final var standinDbFlywayMigration = Flyway.configure()
                .baselineOnMigrate(true)
                .dataSource(secondaryDataSource)
                .locations(LOCATIONS)
                .load();
        standinDbFlywayMigration.migrate();
    }

}
