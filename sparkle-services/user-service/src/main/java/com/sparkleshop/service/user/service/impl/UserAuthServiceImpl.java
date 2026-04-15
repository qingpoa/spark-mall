package com.sparkleshop.service.user.service.impl;

import com.sparkleshop.common.core.exception.BusinessException;
import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.common.security.enums.UserTypeEnum;
import com.sparkleshop.common.security.jwt.JwtToken;
import com.sparkleshop.common.security.jwt.JwtTokenService;
import com.sparkleshop.common.security.jwt.LoginUserContext;
import com.sparkleshop.service.user.dto.auth.LoginRequest;
import com.sparkleshop.service.user.dto.auth.LoginResponse;
import com.sparkleshop.service.user.dto.auth.RegisterRequest;
import com.sparkleshop.service.user.entity.ShopUserDO;
import com.sparkleshop.service.user.enums.LoginTypeEnum;
import com.sparkleshop.service.user.enums.UserStatusEnum;
import com.sparkleshop.service.user.mapper.ShopUserMapper;
import com.sparkleshop.service.user.service.UserAuthService;
import com.sparkleshop.service.user.support.UserRequestUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.sparkleshop.service.user.constant.UserErrorCodes.BAD_CREDENTIALS;
import static com.sparkleshop.service.user.constant.UserErrorCodes.INVALID_REQUEST;
import static com.sparkleshop.service.user.constant.UserErrorCodes.LOGIN_TYPE_NOT_ENABLED;
import static com.sparkleshop.service.user.constant.UserErrorCodes.LOGIN_TYPE_NOT_SUPPORTED;
import static com.sparkleshop.service.user.constant.UserErrorCodes.MOBILE_ALREADY_EXISTS;
import static com.sparkleshop.service.user.constant.UserErrorCodes.USERNAME_ALREADY_EXISTS;
import static com.sparkleshop.service.user.constant.UserErrorCodes.USER_DISABLED;

@Service
@RequiredArgsConstructor
public class UserAuthServiceImpl implements UserAuthService {

    private final ShopUserMapper shopUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest request) {
        validateUsernameUnique(request.getUsername());
        validateMobileUnique(request.getMobile());

        ShopUserDO user = new ShopUserDO();
        user.setUsername(request.getUsername().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(defaultNickname(request));
        user.setMobile(normalize(request.getMobile()));
        user.setStatus(UserStatusEnum.ENABLED.getCode());
        user.setLevel(1);
        user.setLoginIp(UserRequestUtils.getClientIp());
        user.setLoginTime(LocalDateTime.now());
        insertUser(user);
        return buildLoginResponse(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest request) {
        LoginTypeEnum loginType = LoginTypeEnum.fromCode(request.getLoginType());
        if (loginType == null) {
            throw new BusinessException(LOGIN_TYPE_NOT_SUPPORTED, "不支持的登录方式");
        }
        if (loginType == LoginTypeEnum.CODE) {
            throw new BusinessException(LOGIN_TYPE_NOT_ENABLED, "短信验证码登录暂未启用");
        }
        if (StringUtils.isBlank(request.getUsername()) || StringUtils.isBlank(request.getPassword())) {
            throw new BusinessException(INVALID_REQUEST, "用户名和密码不能为空");
        }

        ShopUserDO user = shopUserMapper.selectByUsername(request.getUsername().trim());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(BAD_CREDENTIALS, "用户名或密码错误");
        }
        if (!UserStatusEnum.isEnabled(user.getStatus())) {
            throw new BusinessException(USER_DISABLED, "用户已禁用");
        }

        user.setLoginIp(UserRequestUtils.getClientIp());
        user.setLoginTime(LocalDateTime.now());
        shopUserMapper.updateById(updateLoginInfo(user));
        return buildLoginResponse(user);
    }

    @Override
    public void logout() {
        jwtTokenService.blacklist(LoginUserContext.getRequired());
    }

    private LoginResponse buildLoginResponse(ShopUserDO user) {
        JwtToken jwtToken = jwtTokenService.generateToken(user.getId(), UserTypeEnum.MEMBER.getCode());
        LoginResponse response = new LoginResponse();
        response.setUserId(user.getId());
        response.setToken(jwtToken.getToken());
        response.setExpiresIn(jwtToken.getExpiresIn());

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());
        response.setUserInfo(userInfo);
        return response;
    }

    private void validateUsernameUnique(String username) {
        if (shopUserMapper.selectByUsername(username.trim()) != null) {
            throw new BusinessException(USERNAME_ALREADY_EXISTS, "用户名已存在");
        }
    }

    private void validateMobileUnique(String mobile) {
        String normalized = normalize(mobile);
        if (normalized != null && shopUserMapper.selectByMobile(normalized) != null) {
            throw new BusinessException(MOBILE_ALREADY_EXISTS, "手机号已存在");
        }
    }

    private String defaultNickname(RegisterRequest request) {
        if (StringUtils.isNotBlank(request.getNickname())) {
            return request.getNickname().trim();
        }
        return "用户" + request.getUsername().trim();
    }

    private String normalize(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }

    private void insertUser(ShopUserDO user) {
        try {
            shopUserMapper.insert(user);
        } catch (DuplicateKeyException exception) {
            throw translateDuplicateKeyException(exception);
        }
    }

    private BusinessException translateDuplicateKeyException(DuplicateKeyException exception) {
        String message = StringUtils.defaultString(exception.getMostSpecificCause() == null
                ? exception.getMessage()
                : exception.getMostSpecificCause().getMessage()).toLowerCase();
        if (message.contains("uk_username")) {
            return new BusinessException(USERNAME_ALREADY_EXISTS, "用户名已存在");
        }
        if (message.contains("uk_mobile")) {
            return new BusinessException(MOBILE_ALREADY_EXISTS, "手机号已存在");
        }
        return new BusinessException(Result.CONFLICT, "数据重复，操作冲突");
    }

    private ShopUserDO updateLoginInfo(ShopUserDO user) {
        ShopUserDO update = new ShopUserDO();
        update.setId(user.getId());
        update.setLoginIp(user.getLoginIp());
        update.setLoginTime(user.getLoginTime());
        return update;
    }
}
