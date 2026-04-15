package com.sparkleshop.service.user.service;

import com.sparkleshop.common.core.exception.BusinessException;
import com.sparkleshop.common.security.jwt.JwtToken;
import com.sparkleshop.common.security.jwt.JwtTokenService;
import com.sparkleshop.service.user.dto.auth.LoginRequest;
import com.sparkleshop.service.user.dto.auth.LoginResponse;
import com.sparkleshop.service.user.dto.auth.RegisterRequest;
import com.sparkleshop.service.user.constant.UserErrorCodes;
import com.sparkleshop.service.user.entity.ShopUserDO;
import com.sparkleshop.service.user.mapper.ShopUserMapper;
import com.sparkleshop.service.user.service.impl.UserAuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuthServiceImplTest {

    @Mock
    private ShopUserMapper shopUserMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private UserAuthServiceImpl userAuthService;

    @Test
    void shouldRegisterAndReturnJwt() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("spark");
        request.setPassword("123456");
        request.setNickname("火花");

        when(shopUserMapper.selectByUsername("spark")).thenReturn(null);
        when(passwordEncoder.encode("123456")).thenReturn("encoded-password");
        when(jwtTokenService.generateToken(anyLong(), anyInt())).thenReturn(new JwtToken("jwt-token", "jti-1", 7200));
        doAnswer(invocation -> {
            ShopUserDO user = invocation.getArgument(0);
            user.setId(1L);
            return 1;
        }).when(shopUserMapper).insert(any(ShopUserDO.class));

        LoginResponse response = userAuthService.register(request);

        assertEquals(1L, response.getUserId());
        assertEquals("jwt-token", response.getToken());
        assertEquals("spark", response.getUserInfo().getUsername());
    }

    @Test
    void shouldRejectUnsupportedLoginType() {
        LoginRequest request = new LoginRequest();
        request.setLoginType("social");

        assertThrows(BusinessException.class, () -> userAuthService.login(request));
    }

    @Test
    void shouldRejectBadCredentials() {
        LoginRequest request = new LoginRequest();
        request.setLoginType("password");
        request.setUsername("spark");
        request.setPassword("wrong");

        ShopUserDO user = new ShopUserDO();
        user.setId(1L);
        user.setUsername("spark");
        user.setPassword("encoded-password");
        user.setStatus(1);

        when(shopUserMapper.selectByUsername("spark")).thenReturn(user);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(BusinessException.class, () -> userAuthService.login(request));
    }

    @Test
    void shouldTranslateDuplicateUsernameToBusinessConflict() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("spark");
        request.setPassword("123456");

        when(shopUserMapper.selectByUsername("spark")).thenReturn(null);
        when(passwordEncoder.encode("123456")).thenReturn("encoded-password");
        when(shopUserMapper.insert(any(ShopUserDO.class)))
                .thenThrow(new DuplicateKeyException("Duplicate entry for key 'uk_username'"));

        BusinessException exception = assertThrows(BusinessException.class, () -> userAuthService.register(request));

        assertEquals(UserErrorCodes.USERNAME_ALREADY_EXISTS, exception.getCode());
    }
}
