package com.example.papersystem.controller;

import com.example.papersystem.common.Result;
import com.example.papersystem.entity.User;
import com.example.papersystem.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class StudentAccountController {
    private final UserRepository users;
    public StudentAccountController(UserRepository users) { this.users = users; }

    @PutMapping("/password")
    @Transactional
    public Result<String> changePassword(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String currentPassword = body.getOrDefault("currentPassword", "");
        String newPassword = body.getOrDefault("newPassword", "");
        if (newPassword.length() < 6) return Result.error(400, "新密码至少需要6位");
        Claims claims = (Claims) request.getAttribute("claims");
        User user = users.findById(((Number) claims.get("userId")).longValue()).orElseThrow();
        if (!BCrypt.checkpw(currentPassword, user.getPassword())) return Result.error(400, "当前密码错误");
        user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        users.save(user);
        return Result.success("密码修改成功", null);
    }
}
