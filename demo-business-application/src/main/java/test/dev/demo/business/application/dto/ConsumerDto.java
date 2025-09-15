package test.dev.demo.business.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import test.dev.demo.business.application.kafka.ConsumerStatus;

import java.util.UUID;

@Getter
@Setter
@Builder
public class ConsumerDto {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID uuid;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String consumerGroup;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String topicName;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private ConsumerStatus status;
}
