package com.sparkleshop.common.security.web;

import com.sparkleshop.common.security.constant.SecurityConstants;
import com.sparkleshop.common.security.jwt.LoginUserContext;
import com.sparkleshop.common.security.jwt.TokenUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

public class RequestUserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        TokenUser tokenUser = resolveTokenUser(request);
        if (tokenUser != null) {
            LoginUserContext.set(tokenUser);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginUserContext.clear();
    }

    private TokenUser resolveTokenUser(HttpServletRequest request) {
        String userIdHeader = request.getHeader(SecurityConstants.USER_ID_HEADER);
        String userTypeHeader = request.getHeader(SecurityConstants.USER_TYPE_HEADER);
        String tokenIdHeader = request.getHeader(SecurityConstants.TOKEN_ID_HEADER);
        if (!StringUtils.hasText(userIdHeader)) {
            return null;
        }
        try {
            TokenUser tokenUser = new TokenUser();
            tokenUser.setUserId(Long.valueOf(userIdHeader));
            if (StringUtils.hasText(userTypeHeader)) {
                tokenUser.setUserType(Integer.valueOf(userTypeHeader));
            }
            tokenUser.setTokenId(tokenIdHeader);
            return tokenUser;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
