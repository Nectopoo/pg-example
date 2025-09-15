package test.dev.demo.business.application.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import test.dev.demo.business.application.entity.Session;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
}
