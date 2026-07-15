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
        if (user.getUsername() == null || user.getPassword() == null || user.getRole() == null) {
            return Result.error(400, "用户名、密码和角色不能为空");
        }
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

        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty() || !BCrypt.checkpw(password, opt.get().getPassword())) {
            return Result.error(401, "用户名或密码错误");
        }
        User user = opt.get();
        if (user.getStatus() != 1) {
            return Result.error(403, "账户已被禁用");
        }

        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("role", user.getRole());
        data.put("nickname", user.getNickname());
        return Result.success("登录成功", data);
    }
}
