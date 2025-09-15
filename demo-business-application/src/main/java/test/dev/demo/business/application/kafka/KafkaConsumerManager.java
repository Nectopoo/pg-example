package test.dev.demo.business.application.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import test.dev.demo.business.application.config.DemoBusinessConfigurationProperties;
import test.dev.demo.business.application.helper.ContextHelper;
import test.dev.smartreplication.client.ChangeOrder;
import test.dev.smartreplication.client.kafka.ChangeApplierEndpointConfiguration;
import test.dev.smartreplication.client.kafka.ChangeApplierKafkaConfiguration;
import test.dev.smartreplication.client.kafka.consumer.SmartReplicationChangeApplierManager;
import test.dev.smartreplication.configuration.IConfigurationManagerPrivate;
import test.dev.smartreplication.core.kafka.config.KafkaConfiguration;
import test.dev.smartreplication.core.kafka.consumer.ChangeHandleResult;
import test.dev.smartreplication.core.kafka.consumer.ConsumerChangeHandler;
import test.dev.smartreplication.model.Change;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class KafkaConsumerManager {
    private final DemoBusinessConfigurationProperties config;
    private final IConfigurationManagerPrivate configurationManager;

    public static KafkaConfiguration transform(DemoBusinessConfigurationProperties config) {
        return new KafkaConfiguration(config.getKafka().getBootstrapServers());
    }

    public SmartReplicationChangeApplierManager build(ConsumerContext context, Runnable onFinishCallback) {
        return new SmartReplicationChangeApplierManager(
            new ChangeApplierKafkaConfiguration(transform(config), context.getConcurrency()),
            new TestConsumerChangeHandler(context, onFinishCallback),
            configurationManager,
            new ChangeApplierEndpointConfiguration(
                context.getMeta().getOwner(),
                context.getMeta().getType(),
                context.getMeta().getEndpoint(),
                ChangeOrder.REORDERING)
        );
    }

    public static class TestConsumerChangeHandler implements ConsumerChangeHandler {
        private final ConsumerContext context;
        private final ThreadLocal<AtomicInteger> threadLocalScope = ThreadLocal.withInitial(() -> new AtomicInteger(0));
        private final AtomicInteger processedChangeCounter = new AtomicInteger(0);
        private final Runnable onFinishCallback;

        public TestConsumerChangeHandler(ConsumerContext context, Runnable onFinishCallback) {
            this.context = context;
            this.onFinishCallback = onFinishCallback;
        }

        @Override
        public ChangeHandleResult handleChange(String key, Change value) {
            int limit = context.getReadLimit();
            if (limit > 0 && processedChangeCounter.incrementAndGet() >= limit) {
                onFinishCallback.run();
                return ChangeHandleResult.error("");
            }
            try {
                long delay = context.getMeta().getDelayMs();
                if (delay > 0) {
                    TimeUnit.MILLISECONDS.sleep(delay);
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }

            if (value != null) {
                String changeKey = value.getChangeKey();
                AtomicInteger counter = threadLocalScope.get();

                if (counter.get() < context.getMaxAttempts()
                    && changeKey != null
                    && changeKey.startsWith(ContextHelper.ERROR_PREFIX)) {
                    counter.incrementAndGet();
                    throw new IllegalArgumentException("Message contains error code {" + changeKey + "}");
                } else {
                    counter.getAndSet(0);
                }
            }

            return ChangeHandleResult.success();
        }

        public void unload() {
            threadLocalScope.remove();
        }
    }

}
