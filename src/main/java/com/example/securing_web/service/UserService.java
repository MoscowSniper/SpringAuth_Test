package com.example.securing_web.service;

import com.example.securing_web.entity.User;
import com.example.securing_web.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean register(String username, String password) {
        if(userRepository.findByUsername(username).isPresent()){
            return false; // если пользователель уже есть
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        return true;
    }

    // Добавляем метод для получения всех пользователей
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }
}