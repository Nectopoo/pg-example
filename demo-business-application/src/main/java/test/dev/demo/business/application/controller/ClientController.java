package test.dev.demo.business.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import test.dev.demo.business.application.dto.ClientDto;
import test.dev.demo.business.application.dto.SearchCriteriaDto;
import test.dev.demo.business.application.service.ClientService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Контроллер клиента")
public class ClientController {
    private final ClientService clientService;

    @GetMapping("/client/{id}")
    @Operation(summary = "Возвращает клиента по его id")
    public ClientDto getClient(@PathVariable Long id) {
        return clientService.findClient(id);
    }

    @PostMapping("client")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создает нового клиента")
    public ClientDto save(@RequestBody ClientDto client) {
        return clientService.saveClient(client);
    }

    @PatchMapping("client")
    @Operation(summary = "Изменяет данные клиента")
    public ClientDto update(@RequestBody ClientDto client) {
        return clientService.updateClient(client);
    }

    @DeleteMapping("client/{id}")
    @Operation(summary = "Удаляет клиента по его id")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        clientService.deleteClient(id);
    }

    @DeleteMapping("client/statement/auto")
    @Operation(summary = "Удаляет клиента по его id")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletViaStatement(@RequestBody SearchCriteriaDto searchCriteriaDto) {
        clientService.deleteByCreateDateBeforeViaStatement(searchCriteriaDto);
    }

    @DeleteMapping("client/statement/manual")
    @Operation(summary = "Удаляет клиента по его id")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBeforeViaManual(@RequestBody SearchCriteriaDto searchCriteriaDto) {
        clientService.deleteByCreateDateBeforeViaManual(searchCriteriaDto);
    }
}
