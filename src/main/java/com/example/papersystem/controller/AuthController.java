package com.example.papersystem.controller;

import com.example.papersystem.common.Result;
import com.example.papersystem.config.JwtUtil;
import com.example.papersystem.entity.User;
import com.example.papersystem.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    public Result<String> register(@RequestBody User user) {
        if (user.getUsername() == null || user.getUsername().isBlank()
                || user.getPassword() == null || user.getPassword().isBlank()
                || user.getRole() == null || user.getRole().isBlank()) {
            return Result.error(400, "用户名、密码和角色不能为空");
        }
        String role = user.getRole().trim().toUpperCase();
        if (!role.equals("STUDENT") && !role.equals("TEACHER") && !role.equals("ADMIN")) {
            return Result.error(400, "注册角色无效");
        }
        if (role.equals("STUDENT")) {
            return Result.error(403, "学生账号只能由教师在学生管理中创建");
        }
        if (user.getPassword().length() < 6) {
            return Result.error(400, "密码至少需要6位");
        }
        user.setUsername(user.getUsername().trim());
        user.setRole(role);
        if (userRepository.existsByUsername(user.getUsername())) {
            return Result.error(400, "用户名已存在");
        }
        user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
        user.setStatus(1);
        userRepository.save(user);
        return Result.success("注册成功", null);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");
        String selectedRole = params.get("role");

        if (username == null || username.isBlank() || password == null || password.isBlank()
                || selectedRole == null || selectedRole.isBlank()) {
            return Result.error(400, "请选择身份并填写用户名和密码");
        }
        selectedRole = selectedRole.trim().toUpperCase();
        if (!selectedRole.equals("STUDENT") && !selectedRole.equals("TEACHER") && !selectedRole.equals("ADMIN")) {
            return Result.error(400, "登录身份无效");
        }

        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty() || !BCrypt.checkpw(password, opt.get().getPassword())) {
            return Result.error(401, "用户名或密码错误");
        }
        User user = opt.get();
        if (user.getStatus() != 1) {
            return Result.error(403, "账号已被禁用");
        }
        if (!selectedRole.equalsIgnoreCase(user.getRole())) {
            return Result.error(403, "所选身份与该账号的角色不匹配");
        }

        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("role", user.getRole());
        data.put("nickname", user.getNickname());
        data.put("username", user.getUsername());
        return Result.success("登录成功", data);
    }
    @PostMapping("/updatePassword")
    public Result<String> updatePassword(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        if (username == null || username.isBlank() || oldPassword == null || oldPassword.isBlank() || newPassword == null || newPassword.isBlank()) {
            return Result.error(400, "参数不能为空");
        }
        if (newPassword.length() < 6) {
            return Result.error(400, "新密码至少需要6位");
        }

        Optional<User> opt = userRepository.findByUsername(username.trim());
        if (opt.isEmpty()) {
            return Result.error(404, "用户不存在");
        }

        User user = opt.get();
        // 校验旧密码（使用 BCrypt 匹配）
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            return Result.error(400, "旧密码输入错误");
        }

        // 加密新密码并保存
        user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        userRepository.save(user);

        return Result.success("修改密码成功", null);
    }
}
