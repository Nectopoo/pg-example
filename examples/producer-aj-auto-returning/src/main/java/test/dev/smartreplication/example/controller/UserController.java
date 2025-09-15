package test.dev.smartreplication.example.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import test.dev.smartreplication.example.entity.User;
import test.dev.smartreplication.example.service.UserService;

@RestController
@RequestMapping("/users")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<User> postUser() {
        User randomUserByJpa = userService.createUser();
        return ResponseEntity.ok(randomUserByJpa);
    }

    @PutMapping
    public ResponseEntity<User> putUser(Long id) {
        return ResponseEntity.ok(userService.updateUser(id));
    }

    @DeleteMapping
    public ResponseEntity<String> deleteUser(Long id) {
        return ResponseEntity.ok(userService.deleteUser(id));
    }
}
