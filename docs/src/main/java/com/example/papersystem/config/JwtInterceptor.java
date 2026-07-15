package com.example.papersystem.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 放行 OPTIONS 请求（浏览器预检）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 2. 从 Header 获取 Authorization
        String token = request.getHeader("Authorization");

        // 3. 校验 Token 格式与有效性
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7); // 截取有效 Token 部分
            Claims claims = JwtUtil.parseToken(token); // 使用你的 JwtUtil 解析

            if (claims != null) {
                // 4. 获取角色并校验权限
                String role = claims.get("role", String.class);
                String path = request.getRequestURI();

                // 核心权限匹配逻辑：
                // 如果路径以 /api/admin/ 开头，只允许 ADMIN 访问
                if (path.startsWith("/api/admin/") && !"ADMIN".equals(role)) {
                    sendForbidden(response, "权限不足：仅管理员可访问");
                    return false;
                }

                // 可以继续添加其他规则，例如：
                // if (path.startsWith("/api/teacher/") && !"TEACHER".equals(role) && !"ADMIN".equals(role)) { ... }

                // 权限通过，将用户信息存入 Request，供 Controller 使用
                request.setAttribute("claims", claims);
                return true;
            }
        }

        // 5. Token 无效或未提供，拦截请求
        sendForbidden(response, "Token 无效或已过期，请重新登录");
        return false;
    }

    // 统一错误返回方法
    private void sendForbidden(HttpServletResponse response, String message) throws Exception {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403, \"message\":\"" + message + "\"}");
    }
}