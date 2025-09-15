### 1 Предварительная подготовка  
Для работы etcd достаточно запустить docker-compose  р
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
docker exec -it etcd-example bash
bin/etcdctl --endpoints=https://localhost:2379 --cacert=ssl/ca.crt --cert=ssl/server.crt --key=ssl/server.key put /smart_replication/standin_status/towner MAIN
bin/etcdctl --endpoints=https://localhost:2379 --cacert=ssl/ca.crt --cert=ssl/server.crt --key=ssl/server.key put /smart_replication/standin_enabled/towner TRUE

// Если запускаем producer-aj-auto-shard-function
bin/etcdctl --endpoints=https://localhost:2379 --cacert=ssl/ca.crt --cert=ssl/server.crt --key=ssl/server.key put /smart_replication/standin_status/towner2 MAIN
bin/etcdctl --endpoints=https://localhost:2379 --cacert=ssl/ca.crt --cert=ssl/server.crt --key=ssl/server.key put /smart_replication/standin_enabled/towner2 TRUE
```

### 3 Прикладной журнал, продюсер ручное формирование ВИ , с использованием etcd
Модуль ```producer-aj-manual-etcd```  
Для запуска продюсера ручного формирование ВИ, необходимо запустить java класс из примера в этом проекте  
```
test.dev.smartreplication.example.ProducerAjManualEtcdMain
```
После запуска, можно наблюдать ВИ в топике ```smart_replication_change_request_towner_default```

### 4 Прикладной журнал, консюмер(Applier) ВИ, с использованием etcd
Модуль ```applier-aj-manual-etcd```  
Следующие шаги при штатной работе делается администратором через Админ консоль при создании оунера,  
но для локального запуска необходимо выполнить вручную.  
  
#### 4.1 Перед первым запуском Applier ВИ необходимо создать топики(если их еще нет) и инициализировать консьюмер группу,  
для этого необходимо выполнить следующие команды в терминале:  
```
docker exec -it kafka-example bash  
./bin/kafka-topics.sh --create --partitions 2 --replication-factor 1 --topic smart_replication_change_request_towner_default --zookeeper zookeeper:2181
./bin/kafka-topics.sh --create --partitions 2 --replication-factor 1 --topic smart_replication_change_request_towner_default.RETRY.standin --zookeeper zookeeper:2181
./bin/kafka-topics.sh --create --partitions 2 --replication-factor 1 --topic smart_replication_change_request_towner_default.DLT.standin --zookeeper zookeeper:2181
./bin/kafka-consumer-groups.sh --bootstrap-server kafka:29092 --group smart_replication_endpoint_towner_standin --reset-offsets --topic smart_replication_change_request_towner_default --to-earliest --execute
./bin/kafka-consumer-groups.sh --bootstrap-server kafka:29092 --group smart_replication_endpoint_towner_standin --reset-offsets --topic smart_replication_change_request_towner_default.RETRY.standin --to-earliest --execute
```
Далее необходимо запустить java класс из примера в этом проекте  
```  
test.dev.smartreplication.example.ApplierAjManualEtcd
```  
### 5 Прикладной журнал, продюсер автоматическое формирование векторов изменения 
Модуль ```producer-aj-auto```  
По-умолчанию работает c etcd.  
  
Для запуска требуется :  
Запустить докер контейнеры etcd см пункт 1  
Проинициализировать состояние в etcd см пункт 2.1  
Запустить класс ```test.dev.smartreplication.example.ProducerAjAutoSB2Main(Spring Boot 2)``` или ```test.dev.smartreplication.example.ProducerAjAutoSB3Main(Spring Boot 3)```  
   
После запуска, можно наблюдать сообщения в топике ```smart_replication_change_request_towner_default```  
, а также записи в main БД в таблицах ```country, users```

### 6 Прикладной журнал, продюсер ручное формирование ВИ , с использованием etcd и trx outbox 
Модуль ```producer-aj-trx-outbox-manual-etcd```  
Для запуска продюсера ручного формирование ВИ, необходимо запустить java класс из примера в этом проекте  
```
test.dev.smartreplication.example.ProducerAjTrxOutboxManualEtcdMain
```
После запуска, можно наблюдать ВИ в топике ```smart_replication_change_request_towner_default``` 

### Troubleshooting

Если flyway ругается на контрольную сумму, надо удалить записи из таблицы public.flyway_schema_history  
