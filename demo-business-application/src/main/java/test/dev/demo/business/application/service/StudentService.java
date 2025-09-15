package test.dev.demo.business.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import test.dev.demo.business.application.entity.Student;
import test.dev.demo.business.application.repository.StudentRepository;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;

    public Flux<Student> findStudentsByName(String name) {
        return (name != null) ? studentRepository.findByName(name) : studentRepository.findAll();
    }

    public Mono<Student> findStudentById(long id) {
        return studentRepository.findById(id);
    }

    @Transactional(transactionManager = "transactionManagerReactive")
    public Mono<Student> addNewStudent(Student student) {
        return studentRepository.save(student);
    }

    @Transactional(transactionManager = "transactionManagerReactive")
    public Mono<Student> updateStudent(long id, Student student) {
        return studentRepository.findById(id)
            .flatMap(s -> {
                student.setId(s.getId());
                return studentRepository.save(student);
            });

    }

    @Transactional(transactionManager = "transactionManagerReactive")
    public Mono<Void> deleteStudent(Student student) {
        return studentRepository.delete(student);
    }

    public Flux<Student> findAll() {
        return studentRepository.findAll();
    }
}
