package com.sparkleshop.service.user.service;

import com.sparkleshop.common.core.exception.BusinessException;
import com.sparkleshop.common.security.enums.UserTypeEnum;
import com.sparkleshop.common.security.jwt.JwtProperties;
import com.sparkleshop.common.security.jwt.JwtToken;
import com.sparkleshop.common.security.jwt.JwtTokenService;
import com.sparkleshop.common.security.jwt.TokenUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("change-this-jwt-secret-to-a-secure-value-2026");
        jwtProperties.setIssuer("sparkle-shop");
        jwtProperties.setExpirationSeconds(7200);
        jwtTokenService = new JwtTokenService(stringRedisTemplate, jwtProperties);
    }

    @Test
    void shouldAuthenticateWhenTokenIsValidAndNotBlacklisted() {
        JwtToken jwtToken = jwtTokenService.generateToken(1L, UserTypeEnum.MEMBER.getCode());
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);

        TokenUser tokenUser = jwtTokenService.authenticate("Bearer " + jwtToken.getToken());

        assertEquals(1L, tokenUser.getUserId());
        assertEquals(UserTypeEnum.MEMBER.getCode(), tokenUser.getUserType());
        assertNotNull(tokenUser.getTokenId());
    }

    @Test
    void shouldRejectBlacklistedToken() {
        JwtToken jwtToken = jwtTokenService.generateToken(1L, UserTypeEnum.MEMBER.getCode());
        when(stringRedisTemplate.hasKey(anyString())).thenReturn(true);

        assertThrows(BusinessException.class, () -> jwtTokenService.authenticate("Bearer " + jwtToken.getToken()));
    }

    @Test
    void shouldPersistBlacklistWithRemainingTtl() {
        TokenUser tokenUser = new TokenUser();
        tokenUser.setTokenId("abc123");
        tokenUser.setExpiresAtEpochMilli(System.currentTimeMillis() + 60_000);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        jwtTokenService.blacklist(tokenUser);

        verify(valueOperations).set(anyString(), anyString(), any(Duration.class));
    }
}
