package test.dev.demo.business.application.dto;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;
import test.dev.demo.business.application.entity.Client;
import test.dev.demo.business.application.entity.Transaction;

import java.util.Set;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountDto {
    @Parameter(name = "Id счета")
    private Long id;

    @Parameter(name = "Номер счета")
    private Long accountNumber;

    @Parameter(name = "Тип счета (Евро/Долларовый/Рублевый")
    private AccountType accountType;

    @Parameter(name = "Владелец счета")
    private Client client;

    @Parameter(name = "Сумма денег на счету")
    private double amount;

    @Parameter(name = "Транзакции проводимые с этого счета")
    private Set<Transaction> transactions;
}
