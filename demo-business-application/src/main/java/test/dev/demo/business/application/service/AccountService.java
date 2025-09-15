package test.dev.demo.business.application.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import test.dev.demo.business.application.dto.AccountDto;
import test.dev.demo.business.application.dto.AccountType;
import test.dev.demo.business.application.dto.ExchangeRateDto;
import test.dev.demo.business.application.entity.Transaction;
import test.dev.demo.business.application.exception.AccountNotFoundException;
import test.dev.demo.business.application.exception.AccountTypeException;
import test.dev.demo.business.application.mapper.EntityDtoMapper;
import test.dev.demo.business.application.repository.AccountRepository;
import test.dev.streaming.core.ChangeReplicationMode;
import test.dev.streaming.mdc.MDCKeySupplier;

import java.time.Instant;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final ExchangeRateService exchangeRateService;
    private final TransactionService transactionService;

    public AccountDto findAccount(Long accountNumber) {
        var account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Счет с номером:" + accountNumber + " не найден"));
        return EntityDtoMapper.map(account);
    }

    public AccountDto createAccount(AccountDto account) {
        MDC.put(MDCKeySupplier.CHANGE_REPLICATION_MODE, ChangeReplicationMode.STATEMENT.name());
        accountRepository.createNewAccount(account.getAccountNumber(),
                account.getClient().getId(),
                account.getAccountType().getCode());
        account.setAmount(0);
        return account;
    }

    public void deleteAccount(Long accountNumber) {
        accountRepository.deleteByAccountNumber(accountNumber);
    }

    @Transactional
    public AccountDto changeRubleAccountType(AccountDto accountDto) {
        var newAccountType = accountDto.getAccountType();
        var account = findAccount(accountDto.getAccountNumber());
        if (!account.getAccountType().equals(AccountType.RUBLE)) {
            throw new AccountTypeException("Тип изменяемого счета должен быть в рублях");
        }
        var exchangeRate = exchangeRateService.findAll().stream()
                .max(Comparator.comparing(ExchangeRateDto::getDate))
                .orElseThrow();

        if (newAccountType == AccountType.DOLLAR) {
            account.setAmount(account.getAmount() / exchangeRate.getDollar());
            account.setAccountType(AccountType.DOLLAR);
        }
        if (newAccountType == AccountType.EURO) {
            account.setAmount(account.getAmount() / exchangeRate.getEuro());
            account.setAccountType(AccountType.EURO);
        }
        accountRepository.updateAccountType(account.getAccountType(), account.getAmount(), account.getAccountNumber());
        return account;
    }

    @Transactional
    public AccountDto updateAmount(AccountDto accountDto) {
        var accountNumber = accountDto.getAccountNumber();
        var updatableAccount = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Счет с номером:" + accountNumber + " не найден"));
        var newAmount = updatableAccount.getAmount() + accountDto.getAmount();
        var transaction = Transaction.builder()
                .amount(accountDto.getAmount())
                .fromAccount(updatableAccount)
                .date(Instant.now())
                .build();
        transactionService.saveTransaction(transaction);
        accountRepository.updateAmount(newAmount, updatableAccount.getAccountNumber());
        updatableAccount.setAmount(newAmount);
        return EntityDtoMapper.map(updatableAccount);
    }
}
