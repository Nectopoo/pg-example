package test.dev.smartreplication.scylla.applier.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import test.dev.smartreplication.model.ChangeOrder;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "smart-replication.auto-applier")
public class ApplierProperties {

    private boolean autoconfiguration;
    @NotBlank
    private String serviceCode;
    @NotBlank
    private String endpoint;
    @NotNull
    private ChangeOrder ordering;
    @Positive
    private int consumerConcurrency;
    @Positive
    private int dbPoolMaxSize;
    @Positive
    private long strictOrderPeriodSec;

    ScyllaProperties mainScylla;

    ScyllaProperties standInScylla;

    private boolean enableUseBigDecimalForFloats;

    List<String> scyllaKeyspaces;
}
