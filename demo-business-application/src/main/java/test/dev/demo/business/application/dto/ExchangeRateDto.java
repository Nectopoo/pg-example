package test.dev.demo.business.application.dto;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExchangeRateDto {
    @Parameter(name = "Доллар")
    private double dollar;

    @Parameter(name = "Евро")
    private double euro;

    @Parameter(name = "Дата этого курса валют")
    private Instant date;
}
