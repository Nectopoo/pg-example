# Dev environment configuration

Для запуска из проекта необходимо запустить сначала Docker ресурсы, и запустить с помощью Maven.
```
docker-compose
mvn spring-boot:run
```

Для запуска на внешних ресурсах, минимально необходимо определить опции
```
-Ddemo-app.owner=towner

-Ddemo-app.main-data-source.url=jdbc:postgresql://localhost:5432/main
-Ddemo-app.main-data-source.username=postgres
-Ddemo-app.main-data-source.password=mysecretpassword

-Ddemo-app.standin-data-source.url=jdbc:postgresql://localhost:5432/standin
-Ddemo-app.standin-data-source.username=postgres
-Ddemo-app.standin-data-source.password=mysecretpassword

-Ddemo-app.kafka.bootstrapServers=localhost:9092,localhost:9093,localhost:9094

-Dsmart-replication.etcd.enabled=true
-Dsmart-replication.etcd.endpoints[0]=http://localhost:2379
```

Настройка способа вычисления ключа Вектора Изменений:
```
changeKeyMode: CHANGE_KEY
```
CHANGE_KEY - на основании primary key первой записи
MDC_KEY - на основании значения из MDC контекста, UUID, если нет значения  
### Кастомные пробы liveness, readiness  
  
В ПЖ ситуация, когда одна из БД выключена или находится на обслуживании, является штатной.  
При такой ситуации штатный механизм spring-boot-actuator readiness, liveness могут возвращать DOWN,  
что может привести к перезапуску приложения.  
Чтобы этого избежать, рекомендуется сделать следующeе :  

1. Сделать кастомный datasource health indicator,  
как показано в примере test.dev.demo.business.application.config.Config.dataSourceHealthIndicatorA    
ВАЖНО! Бин dataSourceHealthIndicatorA покрывает только DataSource, проксируемые ПЖ.  
Если есть какие-то жизненно важные ресурсы для приложения, их надо перечислить  
в группах management.endpoint.health.group.readiness, liveness.  
```
test.dev.demo.business.application.config.Config.dataSourceHealthIndicatorA
```

2. Выключить штатный spring механизм datasource health indicator
```
management.health.db.enabled: false
```

3. Прописать кастомные пробы readiness/liveness в application.yml
```
management.endpoint.health.probes.enabled: true
management.endpoint.health.group:
        readiness:
          include:
            - dataSourceHealthIndicatorA
        liveness:
          include:
            - dataSourceHealthIndicatorA
```
 
4. Добавить в deployment для k8s запросы к пробам readiness/liveness
```
livenessProbe:
  initialDelaySeconds: 2
  periodSeconds: 5
  httpGet:
    path: /actuator/health/liveness
    port: 8080
readinessProbe:
  initialDelaySeconds: 2
  periodSeconds: 5
  httpGet:
    path: /actuator/health/readiness
    port: 8080
```

При таких настройках для статуса UP будет достаточно, чтобы была живая хотя бы одна БД.  
