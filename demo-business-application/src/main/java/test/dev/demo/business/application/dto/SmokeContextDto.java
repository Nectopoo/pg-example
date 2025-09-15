package test.dev.demo.business.application.dto;

import lombok.Data;
import test.dev.demo.business.application.kafka.MetaContext;

@Data
public class SmokeContextDto {
    private MetaContext meta;

    private ConsumerContextDto consumerContext;
    private ProducerContextDto producerContext;
}
