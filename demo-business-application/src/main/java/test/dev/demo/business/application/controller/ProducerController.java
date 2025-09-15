package test.dev.demo.business.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import test.dev.demo.business.application.config.DemoBusinessConfigurationProperties;
import test.dev.demo.business.application.dto.MessageProducerContextWithCompressDto;
import test.dev.demo.business.application.dto.ProducerContextDto;
import test.dev.demo.business.application.dto.SameMessageProducerContextDto;
import test.dev.demo.business.application.helper.ContextHelper;
import test.dev.demo.business.application.service.ProduceService;
import test.dev.smartreplication.core.compress.ChangeCompressor;
import test.dev.smartreplication.core.compress.IChangeCompressor;
import test.dev.smartreplication.core.compress.JsonConverter;
import test.dev.smartreplication.core.kafka.ISO8601ObjectMapperFactory;

@RequestMapping("/api/v1")
@RestController
@Tag(name = "producer")
@RequiredArgsConstructor
public class ProducerController {

    private final ProduceService produceService;

    private final DemoBusinessConfigurationProperties configurationProperties;

    @Operation(summary = "Запускает producer-а векторов изменений")
    @PostMapping("/producer/change/start")
    public void changes(
        @RequestBody
        @Parameter(name = "Контекст с параметрами запуска") ProducerContextDto contextDto) {
        produceService.produce(ContextHelper.forProducer(contextDto));
    }

    @Operation(summary = "Запускает producer-а ошибок обработки")
    @PostMapping("/producer/error/start")
    public void errors(
        @RequestBody
        @Parameter(name = "Контекст с параметрами запуска") ProducerContextDto contextDto) {
        produceService.produce(ContextHelper.error(contextDto));
    }

    @Operation(summary = "Запускает producer-а, помещающего в топик переданный вектор изменений")
    @PostMapping("/producer/manual/start")
    public void manual(
        @RequestBody
        @Parameter(name = "Контекст с параметрами запуска") SameMessageProducerContextDto contextDto) {
        produceService.produce(ContextHelper.manual(contextDto));
    }

    @Operation(summary = "Запускает producer-а ошибок сериализации")
    @PostMapping("/producer/fake/start")
    public void fakes(
        @RequestBody
        @Parameter(name = "Контекст с параметрами запуска") ProducerContextDto contextDto) {
        produceService.produce(ContextHelper.fake(contextDto));
    }

    @Operation(summary = "Запускает producer-а сжатых сообщений")
    @PostMapping("/producer/compress/start")
    public void compressed(
        @RequestBody
        @Parameter(name = "Контекст с параметрами запуска") MessageProducerContextWithCompressDto contextDto) {
        IChangeCompressor changeCompression =
            new ChangeCompressor(contextDto.getCompressType(), new JsonConverter(new ISO8601ObjectMapperFactory()),
                configurationProperties.getCompress().getMinBytesToCompress());
        produceService.produce(ContextHelper.compress(contextDto, changeCompression));
    }

}
