package test.dev.demo.business.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import test.dev.demo.business.application.dto.AccountDto;
import test.dev.demo.business.application.service.AccountService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Контроллер счета клиента")
public class AccountController {
    private final AccountService accountService;

    @PostMapping("account")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Сохраняет новый счет с нулевой суммой денег")
    public AccountDto save(@RequestBody AccountDto account) {
        return accountService.createAccount(account);
    }

    @GetMapping("account/{accountNumber}")
    @Operation(summary = "Возвращает счет по его номеру счета")
    public AccountDto find(@PathVariable Long accountNumber) {
        return accountService.findAccount(accountNumber);
    }

    @DeleteMapping("account/{accountNumber}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удаляет счет по его номеру счета")
    public void delete(@PathVariable Long accountNumber) {
        accountService.deleteAccount(accountNumber);
    }

    @PatchMapping("account/change-type")
    @Operation(summary = "Меняет рублевый счет на евровый или доларовый")
    public AccountDto changeRubleAccountType(@RequestBody AccountDto accountDto) {
        return accountService.changeRubleAccountType(accountDto);
    }

    @PatchMapping("account")
    @Operation(summary = "Изменяет сумму денег на счету")
    public AccountDto changeAmount(@RequestBody AccountDto accountDto) {
        return accountService.updateAmount(accountDto);
    }
}
