## Конфигурирование прикладного журнала (ПЖ) для работы в условиях предполагающих временную недоступность ETCD. 

ВАЖНО! При принятии решения о возможности использование данного режима необходимо учитывать следующие риски:

* Потеря централизованного управления через штатные средства ПЖ (web-консоль).
* Некорректное состояние обеих баз MAIN и STAND_IN. Это произойдет в случаях если только часть подов сервиса потеряло связь с ETCD а другая нет. Если при этом будет инициировано переключении БД то, часть подов будет работать с БД MAIN а часть с БД STAND_IN.

#### 1. Предусловия

Версия библиотек ПЖ 2.9.2 и выше. Необходимые зависимости:
```xml
<dependency>
    <groupId>test.dev.smartreplication</groupId>
    <artifactId>smart-replication-producer-lib</artifactId>
    <version>${client.version}</version>
</dependency>
<dependency>
    <groupId>test.dev.smartreplication</groupId>
    <artifactId>smart-replication-core-etcd-support-starter</artifactId>
    <version>${client.version}</version>
</dependency>
```

#### 2. Конфигурирование  

Необходимо использовать резолвер ```test.dev.smartreplication.client.producer.etcd.EtcdChangeSourceResolver```.
```
    @Bean
    public AbstractCachedResolver changeSourceResolver(EtcdConfigurationManager etcdConfigurationManager,
                                                       EtcdChangeSourceResolverListener listener) {

        EtcdChangeSourceResolver etcdChangeSourceResolver = new EtcdChangeSourceResolver(conf.getOwner(),
            etcdConfigurationManager);

        etcdChangeSourceResolver.addListener(listener);
        etcdChangeSourceResolver.init();

        return etcdChangeSourceResolver;
    }
```

Пример см. файл ```SmartReplicationConfig.java```

#### 3. Получение уведомлений о состоянии параметров ПЖ и состоянии подключения к ETCD

Реализована возможность получения уведомлений
* Уведомление при инициализации, при первоначальном подключении к ETCD.
* Уведомление об изменении источника репликации.
* Уведомление об изменении флага разрешения репликации.
* Уведомление о потере связи с ETCD и переходе в режим использования кэшированных значений changeSource и  enableReplicationStatus - последних актуальных значений полученных от ETCD.
* Уведомление о восстановлении соединения с ETCD. Указываются значения параметров прикладного журнала которые использовались в отключенном состоянии и актуализированные значения, полученные после восстановления подключения.

Для получения уведомлений необходимо имплементировать ```test.dev.smartreplication.client.producer.etcd.EtcdChangeSourceResolverListener```.
Пример см. файл ```SmartReplicationConfig.java```. Там же даны комментарии к отдельным методам листенера.

