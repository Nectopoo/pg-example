package test.dev.demo.business.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import test.dev.demo.business.application.dto.ExchangeRateDto;
import test.dev.demo.business.application.service.ExchangeRateService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Контроллер курса валют")
public class ExchangeRateController {
    private final ExchangeRateService exchangeRateService;

    @GetMapping("exchange-rate")
    @Operation(summary = "Возвращает список курса валют")
    public List<ExchangeRateDto> findAll() {
        return exchangeRateService.findAll();
    }
}
