package com.example.securing_web.config;

import com.example.securing_web.entity.Role;
import com.example.securing_web.entity.User;
import com.example.securing_web.repository.RoleRepository;
import com.example.securing_web.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        logger.info("=== НАЧАЛО ИНИЦИАЛИЗАЦИИ ДАННЫХ ===");

        try {
            // 1. Создаем роли
            initRoles();

            // 2. Создаем администратора
            initAdmin();

            // 3. Создаем тестового пользователя (опционально)
            initTestUsers();

            logger.info("=== ИНИЦИАЛИЗАЦИЯ ДАННЫХ УСПЕШНО ЗАВЕРШЕНА ===");
        } catch (Exception e) {
            logger.error("ОШИБКА ПРИ ИНИЦИАЛИЗАЦИИ ДАННЫХ: {}", e.getMessage(), e);
        }
    }

    private void initRoles() {
        logger.info("Инициализация ролей...");

        String[][] roles = {
                {"ROLE_STUDENT", "Студент - может создавать посты и комментарии"},
                {"ROLE_TEACHER", "Преподаватель - может модерировать контент, удалять посты"},
                {"ROLE_ADMIN", "Администратор - полный доступ ко всем функциям системы"}
        };

        for (String[] roleData : roles) {
            String name = roleData[0];
            String description = roleData[1];

            if (!roleRepository.existsByName(name)) {
                Role role = new Role();
                role.setName(name);
                role.setDescription(description);
                roleRepository.save(role);
                logger.info("Создана роль: {}", name);
            } else {
                logger.info("Роль уже существует: {}", name);
            }
        }
    }

    private void initAdmin() {
        logger.info("Проверка администратора...");

        // Проверяем, есть ли уже пользователь с именем admin
        if (userRepository.findByUsername("admin").isEmpty()) {
            logger.info("Создание администратора по умолчанию...");

            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("Администратор системы");

            // Находим или создаем роль ADMIN
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> {
                        logger.warn("Роль ADMIN не найдена, создаем...");
                        Role role = new Role();
                        role.setName("ROLE_ADMIN");
                        role.setDescription("Администратор системы");
                        return roleRepository.save(role);
                    });

            admin.getRoles().add(adminRole);
            userRepository.save(admin);

            logger.info("========================================");
            logger.info("СОЗДАН АДМИНИСТРАТОР ПО УМОЛЧАНИЮ");
            logger.info("Логин: admin");
            logger.info("Пароль: admin123");
            logger.info("ВАЖНО: Смените пароль после первого входа!");
            logger.info("========================================");
        } else {
            logger.info("Администратор уже существует в системе");
        }
    }

    private void initTestUsers() {
        // Создаем тестовых пользователей для демонстрации
        createUserIfNotExists("student1", "student123", "Иван Студентов", "ROLE_STUDENT");
        createUserIfNotExists("teacher1", "teacher123", "Петр Преподавателев", "ROLE_TEACHER");
    }

    private void createUserIfNotExists(String username, String password, String fullName, String roleName) {
        if (userRepository.findByUsername(username).isEmpty()) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setFullName(fullName);

            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new RuntimeException("Роль не найдена: " + roleName));

            user.getRoles().add(role);
            userRepository.save(user);

            logger.info("Создан тестовый пользователь: {} / {}", username, password);
        }
    }
}