package test.dev.smartreplication.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties("application")
public class ApplicationProperties {

    DataSourceProperties defaultDataSource;
}
