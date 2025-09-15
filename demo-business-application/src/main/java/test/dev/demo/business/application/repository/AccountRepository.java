package test.dev.demo.business.application.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import test.dev.demo.business.application.dto.AccountType;
import test.dev.demo.business.application.entity.Account;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query(value = "call create_new_account(:_account_number, :_client_id, :_account_type)", nativeQuery = true)
    void createNewAccount(@Param("_account_number") long accountNumber,
                          @Param("_client_id") long clientId,
                          @Param("_account_type") int accountType);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update Account set accountType=:account_type, amount=:amount where accountNumber=:account_number")
    void updateAccountType(@Param("account_type") AccountType accountType,
                           @Param("amount") double amount,
                           @Param("account_number") Long accountNumber);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update Account set amount=:amount where accountNumber=:account_number")
    void updateAmount(@Param("amount") double amount,
                      @Param("account_number") Long accountNumber);

    Optional<Account> findByAccountNumber(@Param("account_number") Long accountNumber);

    void deleteByAccountNumber(@Param("account_number") Long accountNumber);
}
