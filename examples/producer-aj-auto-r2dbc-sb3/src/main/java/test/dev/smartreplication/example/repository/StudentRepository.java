package test.dev.smartreplication.example.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import test.dev.smartreplication.example.entity.Student;

public interface StudentRepository extends ReactiveCrudRepository<Student, Long> {

    Flux<Student> findByName(String name);

}
