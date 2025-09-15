package test.dev.demo.business.application.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import test.dev.demo.business.application.entity.ExchangeRate;
import test.dev.demo.business.application.entity.ExchangeRateId;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, ExchangeRateId> {
}
