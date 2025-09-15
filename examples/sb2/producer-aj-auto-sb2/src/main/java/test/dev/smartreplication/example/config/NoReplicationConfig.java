package test.dev.smartreplication.example.config;

import lombok.RequiredArgsConstructor;
import org.hibernate.cfg.AvailableSettings;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import test.dev.smartreplication.example.properties.ApplicationProperties;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@ConditionalOnProperty(value = "smartReplication.enabled", havingValue = "false")
@EnableConfigurationProperties(ApplicationProperties.class)
@RequiredArgsConstructor
public class NoReplicationConfig {

    private final ApplicationProperties conf;

    @Bean(name = "defaultDataSource")
    public DataSource defaultDataSource() {
        final var dataSource = new PGSimpleDataSource();
        dataSource.setUrl(conf.getDefaultDataSource().getUrl());
        dataSource.setUser(conf.getDefaultDataSource().getUsername());
        dataSource.setPassword(conf.getDefaultDataSource().getPassword());
        dataSource.setCurrentSchema(conf.getDefaultDataSource().getSchema());
        return dataSource;
    }

    @Bean
    public PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        final var transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
        return transactionManager;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
        @Qualifier("defaultDataSource")
            DataSource defaultDataSource) {
        final var managerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        managerFactoryBean.setDataSource(defaultDataSource);
        managerFactoryBean.setPackagesToScan("test.dev.smartreplication.example");

        final var vendorAdapter = new HibernateJpaVendorAdapter();
        managerFactoryBean.setJpaVendorAdapter(vendorAdapter);
        managerFactoryBean.setJpaProperties(additionalProperties());

        return managerFactoryBean;
    }

    private Properties additionalProperties() {
        final var properties = new Properties();
        properties.setProperty(AvailableSettings.HBM2DDL_AUTO, "none");
        properties.setProperty(AvailableSettings.DIALECT, "org.hibernate.dialect.PostgreSQL10Dialect");

        // иначе Hibernate бросит Exception
        // The database returned no natively generated identity value
        properties.setProperty("hibernate.jdbc.use_get_generated_keys", "false");
        return properties;
    }
}
