Запустить контейнеры  
```
../docker-compose.yaml
./docker-compose.yaml
```

Задать ключи в etcd  
```
docker exec -it etcd-example bash
bin/etcdctl --endpoints=https://localhost:2379 --cacert=ssl/ca.crt --cert=ssl/server.crt --key=ssl/server.key put /smart_replication/standin_status/scyllaowner MAIN
bin/etcdctl --endpoints=https://localhost:2379 --cacert=ssl/ca.crt --cert=ssl/server.crt --key=ssl/server.key put /smart_replication/standin_enabled/scyllaowner TRUE
```

Запустить класс  
```
test.dev.smartreplication.example.ProducerAjAutoScyllaMain
```

Наблюдать сообщения в Offset Explorer (Kafka) в топике  
```
smart_replication_change_request_scyllaowner_default
```

Наблюдать записи в сцилле по адресу localhost:9042 в кейспейсе baeldung  


## Если Scylla в Докер-контейнере падает с ошибкой по нехватке CPU: 
`Could not setup Async I/O: Resource temporarily unavailable. The most common cause is not enough request capacity in /proc/sys/fs/aio-max-nr. Try increasing that number or reducing the amount of logical CPUs available for your application`

1. Остановить контейнер Scylla 
2. Выполнить в cmd: 
```
docker run --rm -it --privileged ubuntu bash -c "echo \"fs.aio-max-nr = 1048576\" >> /etc/sysctl.conf && sysctl -p /etc/sysctl.conf"
```
3. Запустить контейнер заново