package test.dev.demo.business.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ConsumerResponseDto {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private final UUID uuid;
}
