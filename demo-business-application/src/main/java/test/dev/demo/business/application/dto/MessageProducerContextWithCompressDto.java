package test.dev.demo.business.application.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import test.dev.smartreplication.model.CompressType;

@EqualsAndHashCode(callSuper = true)
@Data
public class MessageProducerContextWithCompressDto extends SameMessageProducerContextDto {
    private CompressType compressType;
}
