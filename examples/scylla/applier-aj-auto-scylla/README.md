## Запуск Эплаера  
1. Проинициализировать офсеты консьюмер группы  
```
docker exec -it kafka-example bash  
./bin/kafka-topics.sh --create --partitions 2 --replication-factor 1 --topic smart_replication_change_request_scyllaowner_default --zookeeper zookeeper:2181
./bin/kafka-topics.sh --create --partitions 2 --replication-factor 1 --topic smart_replication_change_request_scyllaowner_default.RETRY.standin --zookeeper zookeeper:2181
./bin/kafka-topics.sh --create --partitions 2 --replication-factor 1 --topic smart_replication_change_request_scyllaowner_default.DLT.standin --zookeeper zookeeper:2181
./bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group smart_replication_endpoint_scyllaowner_standin --reset-offsets --topic smart_replication_change_request_scyllaowner_default --to-earliest --execute
./bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group smart_replication_endpoint_scyllaowner_standin --reset-offsets --topic smart_replication_change_request_scyllaowner_default.RETRY.standin --to-earliest --execute
```
2. Запустить меин класс  
```
test.dev.smartreplication.scylla.applier.ApplierAutoScyllaMain
```
