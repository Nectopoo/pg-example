package test.dev.smartreplication.example.service;

import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;

import java.util.Map;
import java.util.Objects;

@Log4j2
@NoArgsConstructor
public class CustomKafkaPartitioner implements Partitioner {
    private volatile Integer partitionsCount;

    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        if (partitionsCount == null) {
            partitionsCount = cluster.partitionCountForTopic(topic);
            Objects.requireNonNull(partitionsCount, "There is no partitions count metadata for topic " + topic);
        }
        try {
            int partition = Integer.parseInt((String) key) % partitionsCount;
            log.debug("Kafka message key [{}] mapped to partition [{}]", key, partition);
            return Math.abs(partition);
        } catch (ClassCastException | NumberFormatException e) {
            int partition = key.hashCode() % partitionsCount;
            log.debug("Kafka message key [{}] mapped to partition [{}]", key, partition);
            return Math.abs(partition);
        } catch (Exception e) {
            log.warn("Kafka message key [{}] mapped to default partition [0]; caused by: {}", key, e.getMessage());
            return 0;
        }
    }

    @Override
    public void close() {
    }

    @Override
    public void configure(Map<String, ?> map) {
    }
}
