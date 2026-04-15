package com.sparkleshop.common.security.web;

import com.sparkleshop.common.security.annotation.RequireLogin;
import com.sparkleshop.common.security.jwt.JwtTokenService;
import com.sparkleshop.common.security.jwt.LoginUserContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private final JwtTokenService jwtTokenService;

    public JwtAuthenticationInterceptor(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if (!requiresLogin(handlerMethod)) {
            return true;
        }
        LoginUserContext.set(jwtTokenService.authenticate(request.getHeader("Authorization")));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginUserContext.clear();
    }

    private boolean requiresLogin(HandlerMethod handlerMethod) {
        return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), RequireLogin.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), RequireLogin.class);
    }
}
