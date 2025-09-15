package test.dev.demo.business.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import test.dev.demo.business.application.kafka.KafkaProducerManager;
import test.dev.demo.business.application.kafka.ProducerContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@SuppressWarnings(value = {"rawtypes"})
public class ProduceService {
    private static final Logger logger = LoggerFactory.getLogger(ProduceService.class);

    private final KafkaProducerManager producer;

    public ProduceService(KafkaProducerManager producer) {
        this.producer = producer;
    }

    @SuppressWarnings(value = {"unchecked"})
    public void produce(ProducerContext context) {
        final ExecutorService executorService = Executors.newFixedThreadPool(context.getNumThreads());
        try {
            producer.execute(context, executorService);
        } catch (Exception e) {
            logger.error("Problems with running threads", e);
        } finally {
            executorService.shutdown();
        }
    }

}
