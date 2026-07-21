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
            createUser(users, "teacher", "teacher123", "陈教授", "TEACHER", null);
            createUser(users, "admin", "admin123", "系统管理员", "ADMIN", null);
        };
    }

    private User createUser(UserRepository users, String username, String password,
                            String nickname, String role, Long teacherId) {
        User existing = users.findByUsername(username).orElse(null);
        if (existing != null) return existing;
        User user = new User();
        user.setUsername(username); user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setNickname(nickname); user.setRole(role); user.setTeacherId(teacherId); user.setStatus(1);
        return users.save(user);
    }
}
