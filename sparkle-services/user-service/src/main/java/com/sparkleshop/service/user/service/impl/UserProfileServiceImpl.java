package com.sparkleshop.service.user.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkleshop.common.core.exception.BusinessException;
import com.sparkleshop.common.redis.key.RedisKeys;
import com.sparkleshop.common.security.jwt.JwtTokenService;
import com.sparkleshop.common.security.jwt.LoginUserContext;
import com.sparkleshop.service.user.dto.profile.ChangePasswordRequest;
import com.sparkleshop.service.user.dto.profile.UserInfoResponse;
import com.sparkleshop.service.user.dto.profile.UserProfileUpdateRequest;
import com.sparkleshop.service.user.entity.ShopUserDO;
import com.sparkleshop.service.user.mapper.ShopUserMapper;
import com.sparkleshop.service.user.service.UserProfileService;
import com.sparkleshop.service.user.support.UserRequestUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import static com.sparkleshop.service.user.constant.UserErrorCodes.INVALID_REQUEST;
import static com.sparkleshop.service.user.constant.UserErrorCodes.OLD_PASSWORD_INCORRECT;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private static final Duration USER_CACHE_TTL = Duration.ofDays(1);

    private final ShopUserMapper shopUserMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    @Override
    public UserInfoResponse getCurrentUser() {
        Long userId = LoginUserContext.getRequiredUserId();
        String cacheKey = getUserCacheKey(userId);
        String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.isNotBlank(cachedValue)) {
            try {
                return objectMapper.readValue(cachedValue, UserInfoResponse.class);
            } catch (JsonProcessingException ignored) {
                stringRedisTemplate.delete(cacheKey);
            }
        }

        ShopUserDO user = getRequiredUser(userId);
        UserInfoResponse response = toUserInfoResponse(user);
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), USER_CACHE_TTL);
        } catch (JsonProcessingException ignored) {
            // Ignore cache serialization failures and return the DB result directly.
        }
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCurrentUser(UserProfileUpdateRequest request) {
        if (StringUtils.isAllBlank(request.getNickname(), request.getEmail(), request.getAvatar())) {
            throw new BusinessException(INVALID_REQUEST, "至少需要更新一个字段");
        }
        Long userId = LoginUserContext.getRequiredUserId();
        getRequiredUser(userId);

        ShopUserDO update = new ShopUserDO();
        update.setId(userId);
        update.setNickname(normalize(request.getNickname()));
        update.setEmail(normalize(request.getEmail()));
        update.setAvatar(normalize(request.getAvatar()));
        shopUserMapper.updateById(update);
        evictUserCache(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequest request) {
        Long userId = LoginUserContext.getRequiredUserId();
        ShopUserDO user = getRequiredUser(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(OLD_PASSWORD_INCORRECT, "原密码不正确");
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BusinessException(INVALID_REQUEST, "新密码不能与原密码相同");
        }

        ShopUserDO update = new ShopUserDO();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(request.getNewPassword()));
        update.setLoginIp(UserRequestUtils.getClientIp());
        shopUserMapper.updateById(update);
        evictUserCache(userId);
        jwtTokenService.blacklist(LoginUserContext.getRequired());
    }

    private ShopUserDO getRequiredUser(Long userId) {
        ShopUserDO user = shopUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(40400, "用户不存在");
        }
        return user;
    }

    private UserInfoResponse toUserInfoResponse(ShopUserDO user) {
        UserInfoResponse response = new UserInfoResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setMobile(user.getMobile());
        response.setEmail(user.getEmail());
        response.setAvatar(user.getAvatar());
        response.setStatus(user.getStatus());
        response.setLevel(user.getLevel());
        return response;
    }

    private void evictUserCache(Long userId) {
        stringRedisTemplate.delete(getUserCacheKey(userId));
    }

    private String getUserCacheKey(Long userId) {
        return RedisKeys.USER_INFO + userId;
    }

    private String normalize(String value) {
        return StringUtils.trimToNull(value);
    }
}
