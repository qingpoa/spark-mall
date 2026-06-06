package com.sparkleshop.service.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.sparkleshop.common.core.exception.BusinessException;
import com.sparkleshop.common.security.jwt.LoginUserContext;
import com.sparkleshop.common.security.jwt.TokenUser;
import com.sparkleshop.service.user.constant.UserErrorCodes;
import com.sparkleshop.service.user.dto.assistant.UserAssistantChatRequest;
import com.sparkleshop.service.user.service.UserAssistantService;
import com.sparkleshop.service.user.support.dify.DifyChatClient;
import com.sparkleshop.service.user.vo.assistant.UserAssistantChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserAssistantServiceImpl implements UserAssistantService {

    private final DifyChatClient difyChatClient;

    @Override
    public UserAssistantChatResponse chat(UserAssistantChatRequest request) {
        return difyChatClient.chat(request, resolveDifyUser(request));
    }

    private String resolveDifyUser(UserAssistantChatRequest request) {
        TokenUser tokenUser = LoginUserContext.get();
        if (tokenUser != null) {
            return "member:" + tokenUser.getUserId();
        }
        if (StrUtil.isBlank(request.getVisitorId())) {
            throw new BusinessException(UserErrorCodes.INVALID_REQUEST, "未登录时 visitorId 不能为空");
        }
        return "visitor:" + request.getVisitorId().trim();
    }
}
