package test.dev.demo.business.application.dto;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;
import test.dev.demo.business.application.entity.Account;

import java.time.Instant;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDto {
    @Parameter(name = "Дата транзакции")
    private Instant date;

    @Parameter(name = "Номер счета с которого соверешена транзакция")
    private Account fromAccount;

    @Parameter(name = "Сумма транзакции")
    private double amount;
}
