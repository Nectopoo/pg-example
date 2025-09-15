package test.dev.demo.business.application.kafka;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@JsonDeserialize(builder = ConsumerContext.ConsumerContextBuilder.class)
public class ConsumerContext {
    private final MetaContext meta;

    private final int maxAttempts;
    private final int concurrency;
    private final int readLimit;

    @JsonPOJOBuilder(withPrefix = "")
    public static class ConsumerContextBuilder {

    }
}
