package test.dev.demo.business.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import test.dev.demo.business.application.dto.TransactionDto;
import test.dev.demo.business.application.service.TransactionService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Контроллер транзакций по счету")
public class TransactionController {
    private final TransactionService transactionService;

    @GetMapping("transaction/{accountId}")
    @Operation(summary = "Возващает все транзакции по id счета")
    public List<TransactionDto> findAll(@PathVariable Long accountId) {
        return transactionService.findAllTransactionsByAccount(accountId);
    }

}
