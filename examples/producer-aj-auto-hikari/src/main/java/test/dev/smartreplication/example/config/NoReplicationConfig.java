package test.dev.smartreplication.example.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import test.dev.smartreplication.example.properties.DataSourceProperties;
import test.dev.smartreplication.example.properties.SmartReplicationProperties;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@ConditionalOnProperty(value = "smartReplication.enabled", havingValue = "false")
@EnableConfigurationProperties(SmartReplicationProperties.class)
@RequiredArgsConstructor
public class NoReplicationConfig {

    private final SmartReplicationProperties conf;

    @Bean(name = "defaultDataSource")
    public DataSource defaultDataSource() {
        final HikariConfig hikariConfig = new HikariConfig();
        DataSourceProperties mainDatasourceProperties = conf.getMainDatasource();
        hikariConfig.setDriverClassName(mainDatasourceProperties.getDriverClassName());
        hikariConfig.setJdbcUrl(mainDatasourceProperties.getUrl());
        hikariConfig.setUsername(mainDatasourceProperties.getUsername());
        hikariConfig.setPassword(mainDatasourceProperties.getPassword());
        hikariConfig.setSchema(mainDatasourceProperties.getSchema());

        hikariConfig.setMaximumPoolSize(mainDatasourceProperties.getMaxPoolSize());
        hikariConfig.setPoolName("mainPool");

        return new HikariDataSource(hikariConfig);
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
