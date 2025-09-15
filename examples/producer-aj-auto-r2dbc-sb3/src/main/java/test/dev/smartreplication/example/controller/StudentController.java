package test.dev.smartreplication.example.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import test.dev.smartreplication.example.entity.Student;
import test.dev.smartreplication.example.service.StudentService;

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
