package test.dev.demo.business.application.kafka;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SmokeContext<R> {
    private final ConsumerContext consumerContext;
    private final ProducerContext<R> producerContext;
}
