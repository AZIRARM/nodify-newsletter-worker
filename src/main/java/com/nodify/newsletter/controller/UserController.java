package com.nodify.newsletter.controller;

import com.nodify.newsletter.model.User;
import com.nodify.newsletter.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public String usersPage(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "users";
    }

    @GetMapping("/api/list")
    @ResponseBody
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        Optional<User> user = userRepository.findById(id);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/api/create")
    @ResponseBody
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> body) {
        if (userRepository.findByEmail(body.get("email")).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
        }
        User user = new User();
        user.setEmail(body.get("email"));
        user.setFirstName(body.get("firstName"));
        user.setLastName(body.get("lastName"));
        user.setPhone(body.get("phone"));
        user.setAddress(body.get("address"));
        user.setCreatedAt(LocalDateTime.now());
        return ResponseEntity.ok(userRepository.save(user));
    }

    @PutMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Optional<User> existing = userRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User user = existing.get();
        user.setEmail(body.get("email"));
        user.setFirstName(body.get("firstName"));
        user.setLastName(body.get("lastName"));
        user.setPhone(body.get("phone"));
        user.setAddress(body.get("address"));
        return ResponseEntity.ok(userRepository.save(user));
    }

    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok().body(Map.of("success", true));
    }

    @PostMapping("/api/import")
    @ResponseBody
    public ResponseEntity<?> importUsers(@RequestBody List<Map<String, String>> users) {
        for (Map<String, String> u : users) {
            if (userRepository.findByEmail(u.get("email")).isEmpty()) {
                User user = new User();
                user.setEmail(u.get("email"));
                user.setFirstName(u.get("firstName"));
                user.setLastName(u.get("lastName"));
                user.setPhone(u.get("phone"));
                user.setAddress(u.get("address"));
                user.setCreatedAt(LocalDateTime.now());
                userRepository.save(user);
            }
        }
        return ResponseEntity.ok().body(Map.of("imported", users.size()));
    }

    @GetMapping("/api/export")
    @ResponseBody
    public List<User> exportUsers() {
        return userRepository.findAll();
    }
}