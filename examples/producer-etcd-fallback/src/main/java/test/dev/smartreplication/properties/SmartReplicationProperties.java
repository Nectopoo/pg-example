package test.dev.smartreplication.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties("smart-replication.producer")
public class SmartReplicationProperties {

    String owner;

    DataSourceProperties mainDatasource;

    DataSourceProperties standInDatasource;

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
