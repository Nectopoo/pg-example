### 1 Предварительная подготовка  
Для работы etcd модулей необходимо запустить два docker-compose  
1 etcd  
```
./etcd-tls-docker/docker-compose.yml
```
2 postgres, kafka  
```
../docker-compose.yml  
```
### 2 Настройка подключения к etcd
Подключение к etcd настраивается в файле ```application.yml```  
В примерах можно увидеть минимальную настройку.  
Со всеми настройками можно ознакомиться в классе ```test.dev.smartreplication.configuration.etcd.EtcdConfigurationProperties```  

#### 2.1 Также может потребоваться проинициализировать первоначальное состояние в etcd
для этого необходимо выполнить следующие команды в терминале: 
```
docker exec -it etcd-tls-docker_etcd_1 bash

docker exec -it kafka-example bash

./kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic nil_RF2_P2 --from-beginning --consumer-property group.id=test1

bin/etcdctl --user=root:root --endpoints=https://localhost:2379 --cacert=ssl/ca.crt --cert=ssl/server.crt --key=ssl/server.key auth disabled

bin/etcdctl --user=root:root --endpoints=https://localhost:2379 --cacert=ssl/ca.crt --cert=ssl/server.crt --key=ssl/server.key auth disable

bin/etcdctl --user=root:root --endpoints=https://localhost:2379 --cacert=ssl/ca.crt --cert=ssl/server.crt --key=ssl/server.key put /smart_replication/standin_status/{owner} MAIN

bin/etcdctl --user=root:root --endpoints=https://localhost:2379 --cacert=ssl/ca.crt --cert=ssl/server.crt --key=ssl/server.key put /smart_replication/standin_enabled/{owner} true
```
