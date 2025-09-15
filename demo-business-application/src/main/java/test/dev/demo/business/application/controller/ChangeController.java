package test.dev.demo.business.application.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import test.dev.demo.business.application.service.ChangeService;
import test.dev.smartreplication.model.Change;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Контроллер для отправки change вручную")
public class ChangeController {

    private final ChangeService changeService;

    @PostMapping("/change")
    public void sendChange(@RequestBody Change change) {
        changeService.sendChange(change);
    }
}
