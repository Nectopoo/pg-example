package test.dev.smartreplication.example.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Setter
@Getter
@ConfigurationProperties("smart-replication")
public class SmartReplicationProperties {

    String serviceCode;

    ScyllaProperties mainScylla;

    ScyllaProperties standInScylla;

    List<String> scyllaKeyspaces;

    String kafkaConnectionString;

    boolean shouldStopReplication;
    
    boolean replicationEnabled;

    int maxReplicationErrors;

    int errorPeriodInSeconds;

}
