package test.dev.smartreplication.scylla.applier.properties;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ScyllaProperties {
    String host;
    int port;
    String datacenter;
    String keyspace;
}
