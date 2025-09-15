package test.dev.demo.business.application.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.function.BiFunction;

@Getter
@AllArgsConstructor
@Builder
@Setter
public class ProducerContext<R> {
    private final MetaContext meta;

    private final int numThreads;
    private final int size;
    private final int keySize;
    private final String key;
    private final BiFunction<Integer, ProducerContext<R>, R> creator;
    @Builder.Default
    private final Class<?> valueSerializer = JsonSerializer.class;

}


