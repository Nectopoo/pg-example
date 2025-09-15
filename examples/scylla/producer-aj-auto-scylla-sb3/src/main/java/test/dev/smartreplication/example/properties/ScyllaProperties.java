package test.dev.smartreplication.example.properties;

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
