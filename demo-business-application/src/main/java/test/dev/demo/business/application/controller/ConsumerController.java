package test.dev.demo.business.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import test.dev.demo.business.application.dto.ConsumerContextDto;
import test.dev.demo.business.application.dto.ConsumerDto;
import test.dev.demo.business.application.dto.ConsumerInfo;
import test.dev.demo.business.application.dto.ConsumerResponseDto;
import test.dev.demo.business.application.exception.ConsumerNotFoundException;
import test.dev.demo.business.application.helper.ContextHelper;
import test.dev.demo.business.application.service.ConsumerService;
import test.dev.smartreplication.core.kafka.EndpointManager;
import test.dev.smartreplication.core.kafka.KafkaTopicBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequestMapping("/api/v1")
@RestController
@Tag(name = "consumer")
@RequiredArgsConstructor
public class ConsumerController {

    private final ConsumerService consumerService;

    @Operation(summary = "Регистрирует и запускает consumer-а")
    @PostMapping("/consumer")
    public ConsumerResponseDto registration(
        @RequestBody
        @Parameter(name = "Контекст с параметрами запуска") ConsumerContextDto contextDto) {
        return ConsumerResponseDto.builder()
            .uuid(consumerService.consume(ContextHelper.forConsumer(contextDto)))
            .build();
    }

    @Operation(summary = "Запускает consumer-а")
    @PostMapping("/consumer/{uuid}/start")
    public ConsumerInfo start(final
                              @PathVariable
                              @Parameter(name = "Идентификатор зарегистрированного consumer-а") UUID uuid) {
        return consumerService.start(uuid);
    }


    @Operation(summary = "Приостанавливает работу consumer-а")
    @PostMapping("/consumer/{uuid}/pause")
    public void pause(final
                      @PathVariable
                      @Parameter(name = "Идентификатор зарегистрированного consumer-а") UUID uuid) {
        consumerService.createPauseNode(uuid);
    }

    @Operation(summary = "Снимает с паузы работу consumer-а")
    @PostMapping("/consumer/{uuid}/resume")
    public void resume(final
                       @PathVariable
                       @Parameter(name = "Идентификатор зарегистрированного consumer-а") UUID uuid) {
        consumerService.createResumeNode(uuid);
    }

    @Operation(summary = "Удаляет ранее зарегистрированного consumer-а")
    @DeleteMapping("/consumer/{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(final
                       @PathVariable
                       @Parameter(name = "Идентификатор зарегистрированного consumer-а") UUID uuid) {
        try {
            consumerService.remove(uuid);
        } catch (ConsumerNotFoundException ignored) {
        }
    }

    @Operation(summary = "Останавливает и удаляет всех consumer-ов")
    @PostMapping("/consumers/clear")
    public void clear() {
        consumerService.clear();
    }

    @Operation(summary = "Возвращает список consumer-ов")
    @GetMapping("/consumers")
    public List<ConsumerDto> getConsumers() {
        final var allConsumers = consumerService.getAll();
        List<ConsumerDto> consumers = new ArrayList<>(allConsumers.size());
        for (ConsumerInfo consumer : allConsumers) {
            consumers.add(buildConsumerDto(consumer));
        }
        return consumers;
    }

    @Operation(summary = "Возвращает consumer-а по UUID")
    @GetMapping("/consumer/{uuid}")
    public ConsumerDto getConsumer(final
                                   @PathVariable
                                   @Parameter(name = "Идентификатор зарегистрированного consumer-а") UUID uuid) {
        return buildConsumerDto(consumerService.get(uuid));
    }

    private ConsumerDto buildConsumerDto(ConsumerInfo info) {
        final var meta = info.getConsumerContext().getMeta();
        final var builder = new KafkaTopicBuilder()
            .setChangeOwner(meta.getOwner())
            .setEndpoint(meta.getEndpoint())
            .setChangeType(meta.getType());
        return ConsumerDto.builder()
            .uuid(info.getUuid())
            .consumerGroup(EndpointManager.generateEndpointName(meta.getOwner(), meta.getEndpoint()))
            .topicName(builder.buildTopicName())
            .status(info.getStatus())
            .build();
    }
}
