package test.dev.demo.business.application.dto;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import test.dev.demo.business.application.kafka.MetaContext;

@Data
public class ConsumerContextDto {
    private MetaContext meta;

    private int concurrency;
    private int maxAttempts;
    @Parameter(name = "Колличество читаемых сообщений, которые может прочитать потребитель, " +
        "когда нужное количество прочитано, потребитель ставиться на паузу. Warning, работать будет" +
        "только при concurrency = 1")
    private int readLimit;
}
