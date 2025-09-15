package test.dev.demo.business.application.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetaContext {
    private String owner;
    private String type;
    private String endpoint;
    private long delayMs;
    private boolean isRetry;
}
