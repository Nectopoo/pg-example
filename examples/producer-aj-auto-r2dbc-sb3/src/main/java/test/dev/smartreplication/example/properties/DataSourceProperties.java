package test.dev.smartreplication.example.properties;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;


@Setter
@Getter
public class DataSourceProperties {

    public static final String POSTGRESQL_DRIVER_CLASS_NAME = "postgresql";
    public static final int MAX_POOL_SIZE = 10;

    private String driverClassName = POSTGRESQL_DRIVER_CLASS_NAME;

    @NotBlank
    private String host;

    @NotBlank
    private Integer port;

    @NotBlank
    private String database;
    
    @NotEmpty
    private String username;

    @NotEmpty
    private String password;

    private String schema;

    @Positive
    private int maxPoolSize = MAX_POOL_SIZE;

}
