package test.dev.demo.business.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import test.dev.demo.business.application.dto.ExchangeRateDto;
import test.dev.demo.business.application.entity.ExchangeRate;
import test.dev.demo.business.application.entity.ExchangeRateId;
import test.dev.demo.business.application.entity.Session;
import test.dev.demo.business.application.mapper.EntityDtoMapper;
import test.dev.demo.business.application.repository.ExchangeRateRepository;
import test.dev.demo.business.application.repository.SessionRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {
    private final ExchangeRateRepository exchangeRateRepository;
    private final SessionRepository sessionRepository;

    public List<ExchangeRateDto> findAll() {
        return exchangeRateRepository.findAll().stream()
                .map(EntityDtoMapper::map)
                .collect(Collectors.toList());
    }

    public ExchangeRate saveNewExchangeRate() {
        Session session = Session.builder()
                .userId("exchange_job")
                .sessionKey(UUID.randomUUID().toString())
                .build();
        sessionRepository.save(session);
        double dollar = Math.random() * 100;
        double euro = Math.random() * 100;
        ExchangeRateId exchangeRateId = new ExchangeRateId(dollar, euro);
        ExchangeRate exchangeRate = ExchangeRate.builder()
                .exchangeRateId(exchangeRateId)
                .date(Instant.now())
                .build();
        if (exchangeRateRepository.existsById(exchangeRateId)) {
            exchangeRateRepository.deleteById(exchangeRateId);
        }
        return exchangeRateRepository.save(exchangeRate);
    }
}
