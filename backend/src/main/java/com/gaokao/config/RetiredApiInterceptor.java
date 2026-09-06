package com.gaokao.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import java.io.IOException;

public class RetiredApiInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        response.setStatus(410);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":410,\"message\":\"旧版业务接口已下线，请使用新版志愿与投档流程\",\"data\":null}");
        return false;
    }
}
