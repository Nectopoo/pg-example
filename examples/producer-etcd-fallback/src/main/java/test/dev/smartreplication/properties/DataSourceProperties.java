package test.dev.smartreplication.properties;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;


@Setter
@Getter
public class DataSourceProperties {

    public static final String POSTGRESQL_DRIVER_CLASS_NAME = "org.postgresql.Driver";
    public static final int MAX_POOL_SIZE = 100;

    private String driverClassName = POSTGRESQL_DRIVER_CLASS_NAME;

    @NotBlank
    private String url;

    @NotEmpty
    private String username;

    @NotEmpty
    private String password;

    private String schema;

    @Positive
    private int maxPoolSize = MAX_POOL_SIZE;

}
