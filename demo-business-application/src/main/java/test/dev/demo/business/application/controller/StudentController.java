package test.dev.demo.business.application.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import test.dev.demo.business.application.entity.Student;
import test.dev.demo.business.application.service.StudentService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class StudentController {

    private final StudentService studentService;

    @PostMapping("student")
    public Mono<Student> createStudent(@RequestBody Student student) {
        return studentService.addNewStudent(student);
    }

    @GetMapping("student/{id}")
    public Mono<Student> findStudent(@PathVariable Long id) {
        return studentService.findStudentById(id);
    }

    @GetMapping("student")
    public Flux<Student> findStudents(@RequestParam(name = "name", required = false) String name) {
        return studentService.findStudentsByName(name);
    }
    
    @GetMapping("students")
    public Flux<Student> findAllStudents() {
        return studentService.findAll();
    }

    @PatchMapping("student")
    public Mono<Student> updateStudent(@RequestParam Long id, @RequestBody Student student) {
        return studentService.updateStudent(id, student);

    }

    @DeleteMapping("student/{id}")
    public Mono<ResponseEntity<Void>> deleteUser(@PathVariable Long id) {
        return studentService.findStudentById(id)
            .flatMap(s ->
                studentService.deleteStudent(s)
                    .then(Mono.just(new ResponseEntity<Void>(HttpStatus.OK)))
            )
            .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
