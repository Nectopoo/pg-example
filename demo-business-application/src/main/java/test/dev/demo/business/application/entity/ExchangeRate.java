package test.dev.demo.business.application.entity;

import lombok.*;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Курс валюты.
 */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@Table(name = "exchange_rate")
@NoArgsConstructor
public class ExchangeRate {

    @EmbeddedId
    private ExchangeRateId exchangeRateId;

    private Instant date;
}
