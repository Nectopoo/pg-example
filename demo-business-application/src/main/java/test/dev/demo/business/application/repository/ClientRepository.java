package test.dev.demo.business.application.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import test.dev.demo.business.application.entity.Client;

import java.time.Instant;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update Client set firstName=:first_name, lastName=:last_name where id=:id")
    void update(@Param("first_name") String firstName,
                @Param("last_name") String lastName,
                @Param("id") Long id);

    int deleteByCreateDateBefore(Instant date);
}
