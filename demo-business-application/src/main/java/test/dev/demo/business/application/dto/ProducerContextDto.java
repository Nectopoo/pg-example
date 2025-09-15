package test.dev.demo.business.application.dto;

import lombok.Data;
import test.dev.demo.business.application.kafka.MetaContext;

@Data
public class ProducerContextDto {
    private MetaContext meta;

    private int thread;
    private int size;
    private int keySize;
}
