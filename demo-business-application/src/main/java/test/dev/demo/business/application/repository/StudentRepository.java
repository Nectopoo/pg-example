package test.dev.demo.business.application.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import test.dev.demo.business.application.entity.Student;

public interface StudentRepository extends ReactiveCrudRepository<Student, Long> {

    Flux<Student> findByName(String name);

}
