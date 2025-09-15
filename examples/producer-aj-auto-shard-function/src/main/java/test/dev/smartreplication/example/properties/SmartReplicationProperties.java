package test.dev.smartreplication.example.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties("smart-replication.producer")
public class SmartReplicationProperties {

    String owner;

    String owner2;

    DataSourceProperties mainDatasource;

    DataSourceProperties standInDatasource;

    DataSourceProperties mainDatasource2;

    DataSourceProperties standInDatasource2;

    String kafkaConnectionString;

    boolean shouldStopReplication;
    
    boolean replicationEnabled;

    int maxReplicationErrors;

    int errorPeriodInSeconds;
    
    boolean elasticRoutingEnabled;

    @Setter
    @Getter
    public static class Zookeeper {
        String connectionString;
    }
}
