package test.dev.demo.business.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import test.dev.demo.business.application.kafka.ConsumerContext;
import test.dev.demo.business.application.kafka.ConsumerStatus;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerInfo {
    private UUID uuid;
    private ConsumerContext consumerContext;
    private volatile ConsumerStatus status;
}
