package test.dev.demo.business.application.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import test.dev.demo.business.application.dto.AccountDto;
import test.dev.demo.business.application.dto.AccountType;
import test.dev.demo.business.application.dto.ExchangeRateDto;
import test.dev.demo.business.application.entity.Account;
import test.dev.demo.business.application.entity.Client;
import test.dev.demo.business.application.entity.Transaction;
import test.dev.demo.business.application.exception.AccountNotFoundException;
import test.dev.demo.business.application.exception.AccountTypeException;
import test.dev.demo.business.application.repository.AccountRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockBean(ClientService.class)
public class AccountServiceTest extends AbstractServiceConfigurationTest {

    private final Client client = Client.builder()
        .id(1L)
        .firstName("Gena")
        .lastName("Ivanov")
        .build();

    private final AccountDto accountDto = AccountDto.builder()
        .id(1L)
        .accountNumber(42L)
        .accountType(AccountType.RUBLE)
        .amount(1000)
        .client(client)
        .build();

    private final Account account = Account.builder()
        .id(1L)
        .accountNumber(42L)
        .accountType(AccountType.RUBLE)
        .amount(1000)
        .client(client)
        .build();

    @Autowired
    private AccountService accountService;

    @MockBean
    private ExchangeRateService exchangeRateService;

    @MockBean
    private AccountRepository accountRepository;

    @Test
    public void createAccount() {
        var actualAccount = accountService.createAccount(accountDto);
        assertEquals(0, actualAccount.getAmount(), 5);
        assertEquals(client, actualAccount.getClient());
        assertEquals(42L, actualAccount.getAccountNumber().longValue());
        assertEquals(AccountType.RUBLE, actualAccount.getAccountType());
    }

    @Test
    public void findAccount() {
        when(accountRepository.findByAccountNumber(42L)).thenReturn(Optional.of(account));
        var actualAccount = accountService.findAccount(42L);
        assertEquals(42L, actualAccount.getAccountNumber().longValue());
        assertEquals(client, actualAccount.getClient());

    }

    @Test(expected = AccountNotFoundException.class)
    public void findNonexistentAccount() {
        when(accountRepository.findByAccountNumber(11L)).thenReturn(Optional.empty());
        accountService.findAccount(11L);
    }

    @Test
    public void changeRubleAccountType() {
        var date = Instant.now();
        ExchangeRateDto exchangeRate = ExchangeRateDto.builder()
            .euro(90.11)
            .dollar(80.92)
            .date(date)
            .build();
        when(accountRepository.findByAccountNumber(42L)).thenReturn(Optional.of(account));
        when(exchangeRateService.findAll()).thenReturn(List.of(exchangeRate));

        accountDto.setAccountType(AccountType.EURO);
        var actualEuroAccount = accountService.changeRubleAccountType(accountDto);

        assertEquals(1000 / 90.11, actualEuroAccount.getAmount(), 0.0000001);
        assertEquals(AccountType.EURO, actualEuroAccount.getAccountType());

        accountDto.setAccountType(AccountType.DOLLAR);
        var actualDollarAccount = accountService.changeRubleAccountType(accountDto);

        assertEquals(1000 / 80.92, actualDollarAccount.getAmount(), 0.0000001);
        assertEquals(AccountType.DOLLAR, actualDollarAccount.getAccountType());
    }

    @Test(expected = AccountTypeException.class)
    public void changeEuroAccountType() {
        account.setAccountType(AccountType.EURO);
        when(accountRepository.findByAccountNumber(42L)).thenReturn(Optional.of(account));
        accountService.changeRubleAccountType(accountDto);
    }

    @Test
    public void updateAmount() {
        accountDto.setAmount(-10);
        when(accountRepository.findByAccountNumber(42L)).thenReturn(Optional.of(account));
        var actualAccount = accountService.updateAmount(accountDto);
        verify(transactionService).saveTransaction(any(Transaction.class));
        assertEquals(990.0, actualAccount.getAmount(), 0.0000001);
    }

}
