package test.dev.demo.business.application.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import test.dev.demo.business.application.entity.Account;
import test.dev.demo.business.application.entity.Transaction;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findAllByFromAccount(@Param("from_account") Account fromAccount);
}

