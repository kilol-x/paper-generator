package com.example.papersystem.config;

import com.example.papersystem.entity.User;
import com.example.papersystem.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("demo")
public class DemoDataInitializer {

    @Bean
    CommandLineRunner initializeDemoUsers(UserRepository users) {
        return args -> {
            createUser(users, "student", "student123", "张明", "STUDENT", "student@example.edu.cn");
            createUser(users, "teacher", "teacher123", "陈教授", "TEACHER", "teacher@example.edu.cn");
            createUser(users, "admin", "admin123", "系统管理员", "ADMIN", "admin@example.edu.cn");
        };
    }

    private void createUser(UserRepository users, String username, String password,
                            String nickname, String role, String email) {
        if (users.existsByUsername(username)) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setNickname(nickname);
        user.setRole(role);
        user.setEmail(email);
        user.setStatus(1);
        users.save(user);
    }
}
