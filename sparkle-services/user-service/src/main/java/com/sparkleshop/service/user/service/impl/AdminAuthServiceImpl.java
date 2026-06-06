package com.sparkleshop.service.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.sparkleshop.common.core.exception.BusinessException;
import com.sparkleshop.common.security.enums.UserTypeEnum;
import com.sparkleshop.common.security.jwt.JwtToken;
import com.sparkleshop.common.security.jwt.JwtTokenService;
import com.sparkleshop.common.security.jwt.LoginUserContext;
import com.sparkleshop.common.security.jwt.TokenUser;
import com.sparkleshop.service.user.dto.adminauth.AdminChangePasswordRequest;
import com.sparkleshop.service.user.dto.adminauth.AdminCreateRequest;
import com.sparkleshop.service.user.dto.adminauth.AdminCreateResponse;
import com.sparkleshop.service.user.dto.adminauth.AdminLoginRequest;
import com.sparkleshop.service.user.dto.adminauth.AdminLoginResponse;
import com.sparkleshop.service.user.entity.AdminUserDO;
import com.sparkleshop.service.user.enums.UserStatusEnum;
import com.sparkleshop.service.user.mapper.AdminUserMapper;
import com.sparkleshop.service.user.service.AdminAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.sparkleshop.service.user.constant.UserErrorCodes.BAD_CREDENTIALS;
import static com.sparkleshop.service.user.constant.UserErrorCodes.INVALID_REQUEST;
import static com.sparkleshop.service.user.constant.UserErrorCodes.OLD_PASSWORD_INCORRECT;
import static com.sparkleshop.service.user.constant.UserErrorCodes.USER_DISABLED;
import static com.sparkleshop.service.user.constant.UserErrorCodes.USERNAME_ALREADY_EXISTS;

@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminCreateResponse createAdmin(AdminCreateRequest request) {
        String username = StrUtil.trim(request.getUsername());
        if (adminUserMapper.selectByUsername(username) != null) {
            throw new BusinessException(USERNAME_ALREADY_EXISTS, "管理员用户名已存在");
        }

        AdminUserDO adminUser = new AdminUserDO();
        adminUser.setUsername(username);
        adminUser.setPassword(passwordEncoder.encode(request.getPassword()));
        adminUser.setNickname(StrUtil.trimToNull(request.getNickname()));
        adminUser.setLevel(request.getLevel() == null ? 2 : request.getLevel());
        adminUser.setStatus(UserStatusEnum.ENABLED.getCode());
        try {
            adminUserMapper.insert(adminUser);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(USERNAME_ALREADY_EXISTS, "管理员用户名已存在");
        }
        return new AdminCreateResponse(adminUser.getId());
    }

    @Override
    public AdminLoginResponse login(AdminLoginRequest request) {
        if (StrUtil.isBlank(request.getUsername()) || StrUtil.isBlank(request.getPassword())) {
            throw new BusinessException(INVALID_REQUEST, "用户名和密码不能为空");
        }

        AdminUserDO adminUser = adminUserMapper.selectByUsername(StrUtil.trim(request.getUsername()));
        if (adminUser == null || !passwordEncoder.matches(request.getPassword(), adminUser.getPassword())) {
            throw new BusinessException(BAD_CREDENTIALS, "用户名或密码错误");
        }
        if (!UserStatusEnum.isEnabled(adminUser.getStatus())) {
            throw new BusinessException(USER_DISABLED, "管理员已禁用");
        }
        return buildLoginResponse(adminUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(AdminChangePasswordRequest request) {
        TokenUser tokenUser = LoginUserContext.getRequired();
        AdminUserDO adminUser = adminUserMapper.selectById(tokenUser.getUserId());
        if (adminUser == null) {
            throw new BusinessException(40400, "管理员不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), adminUser.getPassword())) {
            throw new BusinessException(OLD_PASSWORD_INCORRECT, "原密码不正确");
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BusinessException(INVALID_REQUEST, "新密码不能与原密码相同");
        }

        AdminUserDO update = new AdminUserDO();
        update.setId(adminUser.getId());
        update.setPassword(passwordEncoder.encode(request.getNewPassword()));
        adminUserMapper.updateById(update);
        jwtTokenService.invalidateUserTokens(tokenUser.getUserType(), tokenUser.getUserId());
        jwtTokenService.blacklist(tokenUser);
    }

    private AdminLoginResponse buildLoginResponse(AdminUserDO adminUser) {
        JwtToken jwtToken = jwtTokenService.generateToken(adminUser.getId(), UserTypeEnum.ADMIN.getCode());
        AdminLoginResponse response = new AdminLoginResponse();
        response.setAdminId(adminUser.getId());
        response.setToken(jwtToken.getToken());
        response.setExpiresIn(jwtToken.getExpiresIn());

        AdminLoginResponse.AdminInfo adminInfo = new AdminLoginResponse.AdminInfo();
        adminInfo.setId(adminUser.getId());
        adminInfo.setUsername(adminUser.getUsername());
        adminInfo.setNickname(adminUser.getNickname());
        adminInfo.setLevel(adminUser.getLevel());
        response.setAdminInfo(adminInfo);
        return response;
    }
}
