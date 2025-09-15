package test.dev.smartreplication.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import test.dev.smartreplication.example.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
