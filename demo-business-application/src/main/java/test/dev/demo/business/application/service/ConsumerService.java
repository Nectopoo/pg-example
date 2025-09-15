package test.dev.demo.business.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import test.dev.demo.business.application.dto.ConsumerInfo;
import test.dev.demo.business.application.exception.ConsumerNotFoundException;
import test.dev.demo.business.application.kafka.ConsumerContext;
import test.dev.demo.business.application.kafka.ConsumerStatus;
import test.dev.demo.business.application.kafka.KafkaConsumerManager;
import test.dev.smartreplication.client.kafka.consumer.SmartReplicationChangeApplierManager;
import test.dev.smartreplication.configuration.IConfigurationManager;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@EnableScheduling
@RequiredArgsConstructor
public class ConsumerService {
    private final Map<UUID, Pair<SmartReplicationChangeApplierManager, ConsumerInfo>> consumerMap = new ConcurrentHashMap<>();
    private final KafkaConsumerManager consumerManager;
    private final ObjectMapper objectMapper;
    private final IConfigurationManager configurationManager;
    @Value("${demo-app.compress.live.timeout.min}")
    private Long consumerLifetime;


    @SneakyThrows
    public UUID consume(ConsumerContext context) {
        UUID uuid = UUID.randomUUID();

        SmartReplicationChangeApplierManager manager = consumerManager.build(context, () -> this.remove(uuid));
        manager.init();
        ConsumerInfo consumerInfo = ConsumerInfo.builder()
            .uuid(uuid)
            .consumerContext(context)
            .status(ConsumerStatus.RUN).build();

        consumerMap.put(uuid, Pair.of(manager, consumerInfo));

        var consumerBytes = objectMapper.writeValueAsBytes(consumerInfo);
        configurationManager.saveSelfCheckerConsumerInfo(uuid, consumerBytes, Duration.ofMinutes(consumerLifetime));

        executeConsumer(() -> this.remove(uuid));

        return uuid;
    }

    public ConsumerInfo start(UUID uuid) {
        Pair<SmartReplicationChangeApplierManager, ConsumerInfo> consumerInfo = getConsumerInfo(uuid);
        consumerInfo.getFirst().start();
        consumerInfo.getSecond().setStatus(ConsumerStatus.RUN);
        return consumerInfo.getSecond();
    }

    public void createPauseNode(UUID uuid) {
        configurationManager.createSelfCheckerPauseNode(uuid);
        pauseConsumer();
    }

    public void createResumeNode(UUID uuid) {
        configurationManager.createSelfCheckerResumeNode(uuid);
        resumeConsumer();
    }

    public void remove(UUID uuid) {
        configurationManager.createSelfCheckerTombstoneNode(uuid);
        configurationManager.deleteSelfCheckerPauseNode(uuid);
    }

    public void clear() {
        consumerMap.forEach((uuid, consumerInfo) -> {
            consumerInfo.getFirst().stop();
            configurationManager.deleteSelfCheckerPauseNode(uuid);
        });
        consumerMap.clear();
    }

    @SneakyThrows
    public ConsumerInfo get(UUID uuid) {
        var consumerBytes = configurationManager.getSelfCheckerConsumerInfo(uuid, Duration.ofMinutes(consumerLifetime));
        return objectMapper.readValue(consumerBytes, ConsumerInfo.class);
    }

    @SneakyThrows
    public List<ConsumerInfo> getAll() {
        var consumersBytes = configurationManager.getSelfCheckConsumersInfo(Duration.ofMinutes(consumerLifetime));
        List<ConsumerInfo> consumersInfo = new ArrayList<>(consumersBytes.size());
        for (byte[] byteConsumer : consumersBytes) {
            consumersInfo.add(objectMapper.readValue(byteConsumer, ConsumerInfo.class));
        }
        return consumersInfo;
    }

    /**
     * Ставит время жизни consumer-ов 10 минут.
     */
    public void executeConsumer(Runnable execute) {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.schedule(execute, consumerLifetime, TimeUnit.MINUTES);
    }

    public Pair<SmartReplicationChangeApplierManager, ConsumerInfo> getConsumerInfo(UUID uuid) {
        var consumerInfo = consumerMap.get(uuid);
        if (consumerInfo == null) {
            throw new ConsumerNotFoundException("UUID " + uuid + "не найден");
        }
        return consumerInfo;
    }

    /**
     * Каждые 10 секунд проверяет etcd на наличие consumer-ов помеченных для возобновления
     * и возобновляет их если они есть в consumerMap.
     */
    @SneakyThrows
    @Scheduled(cron = "${demo-app.server.partitioning.cron:*/10 * * * * *}")
    private void resumeConsumer() {
        List<String> resumedConsumers = configurationManager.getSelfCheckerResumeConsumers();
        for (Map.Entry<UUID, Pair<SmartReplicationChangeApplierManager, ConsumerInfo>> data : consumerMap.entrySet()) {
            if (resumedConsumers.contains(data.getKey().toString())) {
                Pair<SmartReplicationChangeApplierManager, ConsumerInfo> consumerInfo = getConsumerInfo(data.getKey());
                consumerInfo.getFirst().resume();
                consumerInfo.getSecond().setStatus(ConsumerStatus.RUN);
                var consumerBytes = objectMapper.writeValueAsBytes(consumerInfo.getSecond());
                configurationManager.saveSelfCheckerConsumerInfo(data.getKey(), consumerBytes, Duration.ofMinutes(consumerLifetime));
                configurationManager.deleteSelfCheckerResumeNode(data.getKey());
            }
        }
    }

    /**
     * Каждые 10 секунд проверяет etcd на наличие consumer-ов помеченных для паузы
     * и ставит на паузу если они есть в consumerMap.
     */
    @SneakyThrows
    @Scheduled(cron = "${demo-app.server.partitioning.cron:*/10 * * * * *}")
    private void pauseConsumer() {
        final var pausedConsumers = configurationManager.getSelfCheckerPauseConsumers();
        for (Map.Entry<UUID, Pair<SmartReplicationChangeApplierManager, ConsumerInfo>> data : consumerMap.entrySet()) {
            if (pausedConsumers.contains(data.getKey().toString())) {
                Pair<SmartReplicationChangeApplierManager, ConsumerInfo> consumerInfo = getConsumerInfo(data.getKey());
                consumerInfo.getFirst().pause();
                consumerInfo.getSecond().setStatus(ConsumerStatus.PAUSE);
                var consumerBytes = objectMapper.writeValueAsBytes(consumerInfo.getSecond());
                configurationManager.saveSelfCheckerConsumerInfo(data.getKey(), consumerBytes, Duration.ofMinutes(consumerLifetime));
                configurationManager.deleteSelfCheckerPauseNode(data.getKey());
            }
        }
    }

    @PostConstruct
    private void init() {
        configurationManager.createSelfCheckerRoots();
    }

    /**
     * Каждые 10 секунд проверяет протухших consumer-ов, и если он должен был удалиться, но работает, удаляет его.
     */
    @SneakyThrows
    @Scheduled(cron = "${demo-app.server.partitioning.cron:*/10 * * * * *}")
    private void removeObsoleteConsumers() {
        final var obsoleteConsumers = configurationManager.getSelfCheckerTombstoneConsumers();
        for (Map.Entry<UUID, Pair<SmartReplicationChangeApplierManager, ConsumerInfo>> data : consumerMap.entrySet()) {
            if (obsoleteConsumers.contains(data.getKey().toString())) {
                Pair<SmartReplicationChangeApplierManager, ConsumerInfo> consumerInfo = getConsumerInfo(data.getKey());
                consumerInfo.getFirst().stop();
                consumerMap.remove(data.getKey());
                configurationManager.deleteSelfCheckerTombstoneNode(data.getKey());
            }
        }
    }

}
