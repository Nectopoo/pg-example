package test.dev.smartreplication.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import test.dev.smartreplication.example.entity.RuPayment;

import java.time.OffsetDateTime;

public interface RuPaymentRepository extends JpaRepository<RuPayment, Long>, JpaSpecificationExecutor<RuPayment> {
    int deleteByCreateDateBefore(OffsetDateTime date);
}
