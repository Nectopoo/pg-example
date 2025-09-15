package test.dev.smartreplication.example.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties("smart-replication.producer")
public class SmartReplicationProperties {

    String owner;

    String kafkaConnectionString;

    boolean shouldStopReplication;
    
    boolean replicationEnabled;

    int maxReplicationErrors;

    int errorPeriodInSeconds;

    @Setter
    @Getter
    public static class Zookeeper {
        String connectionString;
    }
}
