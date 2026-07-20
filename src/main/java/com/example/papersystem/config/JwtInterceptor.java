package com.example.papersystem.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    // 开启调试模式：设置为 true 时，直接放行所有权限校验，方便组员联调
    private static final boolean DEBUG_MODE = true;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 放行 OPTIONS 请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 2. 调试模式开关：如果开启，直接通过，不校验 Token
        if (DEBUG_MODE) {
            return true;
        }

        // 3. 原有的校验逻辑
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            Claims claims = JwtUtil.parseToken(token);

            if (claims != null) {
                String role = claims.get("role", String.class);
                String path = request.getRequestURI();

                if (path.startsWith("/api/admin/") && !"ADMIN".equals(role)) {
                    sendForbidden(response, "权限不足：仅管理员可访问");
                    return false;
                }
                request.setAttribute("claims", claims);
                return true;
            }
        }

        sendForbidden(response, "Token 无效或已过期，请重新登录");
        return false;
    }

    private void sendForbidden(HttpServletResponse response, String message) throws Exception {
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403, \"message\":\"" + message + "\"}");
    }
}