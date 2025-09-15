package test.dev.smartreplication.example.controller;

import lombok.RequiredArgsConstructor;
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
import test.dev.smartreplication.example.entity.User;
import test.dev.smartreplication.example.service.UserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class UserController {
    private final UserService userService;

    @PostMapping("user")
    public Mono<User> createUser(@RequestBody User user) {
        return userService.create(user);
    }

    @GetMapping("user/{id}")
    public Mono<User> findUser(@PathVariable Long id) {
        return userService.findById(id);
    }

    @GetMapping("users")
    public Flux<User> findAllUsers() {
        return userService.findAll();
    }

    @PatchMapping("user")
    public Mono<User> updateUsername(@RequestParam Long id,
                                     @RequestParam String username) {
        return userService.updateUsername(id, username);

    }

    @DeleteMapping("user/{id}")
    public Mono<Object> deleteUser(@PathVariable Long id) {
        return userService.delete(id);
    }
}
