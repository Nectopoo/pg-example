package test.dev.smartreplication.example.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfiguration {

    @Bean
    @Qualifier("mainJdbcTemplate")
    public NamedParameterJdbcTemplate mainJdbcTemplate(@Qualifier("mainDatasource") DataSource mainDataSource) {
        return new NamedParameterJdbcTemplate(mainDataSource);
    }

    @Bean
    @Qualifier("standInJdbcTemplate")
    public NamedParameterJdbcTemplate standInJdbcTemplate(@Qualifier("standinDatasource") DataSource standInDatasource) {
        return new NamedParameterJdbcTemplate(standInDatasource);
    }

}
