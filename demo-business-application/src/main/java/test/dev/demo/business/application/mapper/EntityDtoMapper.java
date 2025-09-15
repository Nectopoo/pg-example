package test.dev.demo.business.application.mapper;

import lombok.experimental.UtilityClass;
import test.dev.demo.business.application.dto.AccountDto;
import test.dev.demo.business.application.dto.ClientDto;
import test.dev.demo.business.application.dto.ExchangeRateDto;
import test.dev.demo.business.application.dto.TransactionDto;
import test.dev.demo.business.application.entity.Account;
import test.dev.demo.business.application.entity.Client;
import test.dev.demo.business.application.entity.ExchangeRate;
import test.dev.demo.business.application.entity.Transaction;

@UtilityClass
public class EntityDtoMapper {

    public static ClientDto map(Client client) {
        return ClientDto.builder()
                .id(client.getId())
                .firstName(client.getFirstName())
                .lastName(client.getLastName())
                .build();
    }

    public static Client map(ClientDto clientDto) {
        return Client.builder()
                .firstName(clientDto.getFirstName())
                .lastName(clientDto.getLastName())
                .build();
    }

    public static AccountDto map(Account account) {
        return AccountDto.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType())
                .amount(account.getAmount())
                .transactions(account.getTransactions())
                .client(account.getClient())
                .build();
    }

    public static TransactionDto map(Transaction transaction) {
        return TransactionDto.builder()
                .date(transaction.getDate())
                .fromAccount(transaction.getFromAccount())
                .amount(transaction.getAmount())
                .build();
    }

    public static ExchangeRateDto map(ExchangeRate exchangeRate) {
        return ExchangeRateDto.builder()
                .dollar(exchangeRate.getExchangeRateId().getDollar())
                .euro(exchangeRate.getExchangeRateId().getEuro())
                .date(exchangeRate.getDate())
                .build();
    }
}
