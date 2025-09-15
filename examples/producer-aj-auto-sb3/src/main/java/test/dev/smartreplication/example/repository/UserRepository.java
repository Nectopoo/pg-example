package test.dev.smartreplication.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import test.dev.smartreplication.example.entity.User;

import java.time.Instant;

public interface UserRepository extends JpaRepository<User, Long> {
    Integer deleteByCreateDateBefore(Instant date);
}
