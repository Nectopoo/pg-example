package test.dev.demo.business.application.dto;

import lombok.Data;
import test.dev.demo.business.application.kafka.MetaContext;
import test.dev.smartreplication.model.Change;

@Data
public class SameMessageProducerContextDto {
    private String key;
    private Change change;

    private MetaContext meta;
    private int size;
    private Boolean isRetry;
}
