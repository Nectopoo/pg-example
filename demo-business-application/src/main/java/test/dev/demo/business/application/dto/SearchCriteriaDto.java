package test.dev.demo.business.application.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class SearchCriteriaDto {
    private Instant dateBefore;
}
