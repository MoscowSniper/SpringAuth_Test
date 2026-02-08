package com.example.securing_web.service;

import com.example.securing_web.entity.Role;
import com.example.securing_web.entity.User;
import com.example.securing_web.repository.RoleRepository;
import com.example.securing_web.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public boolean register(String username, String password, String fullName, String roleName) {
        // Проверяем существование пользователя
        if(userRepository.findByUsername(username).isPresent()){
            return false;
        }

        // Создаем пользователя
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName != null ? fullName : username);

        // Находим роль
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Роль не найдена: " + roleName));

        // Назначаем роль
        user.getRoles().add(role);

        // Сохраняем
        userRepository.save(user);
        return true;
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public void updateUserRoles(Long userId, Set<String> roleNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        // Очищаем текущие роли
        user.getRoles().clear();

        // Добавляем новые роли
        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new IllegalArgumentException("Роль не найдена: " + roleName));
            user.getRoles().add(role);
        }

        userRepository.save(user);
    }

    public List<Role> findAllRoles() {
        return roleRepository.findAll();
    }

    @Transactional
    public void deleteUser(Long userId, String currentUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        // Нельзя удалить самого себя
        if (user.getUsername().equals(currentUsername)) {
            throw new IllegalArgumentException("Нельзя удалить свой собственный аккаунт");
        }

        userRepository.delete(user);
    }

    @Transactional
    public void changePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public long countUsersByRole(String roleName) {
        return userRepository.countByRoleName(roleName);
    }

    public List<User> findUsersByRole(String roleName) {
        return userRepository.findByRoles_Name(roleName);
    }
}