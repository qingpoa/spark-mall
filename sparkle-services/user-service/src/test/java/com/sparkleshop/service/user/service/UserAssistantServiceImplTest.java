package com.sparkleshop.service.user.service;

import com.sparkleshop.common.core.exception.BusinessException;
import com.sparkleshop.common.security.jwt.LoginUserContext;
import com.sparkleshop.common.security.jwt.TokenUser;
import com.sparkleshop.service.user.dto.assistant.UserAssistantChatRequest;
import com.sparkleshop.service.user.service.impl.UserAssistantServiceImpl;
import com.sparkleshop.service.user.support.dify.DifyChatClient;
import com.sparkleshop.service.user.vo.assistant.UserAssistantChatResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAssistantServiceImplTest {

    @Mock
    private DifyChatClient difyChatClient;

    @InjectMocks
    private UserAssistantServiceImpl userAssistantService;

    @AfterEach
    void tearDown() {
        LoginUserContext.clear();
    }

    @Test
    void shouldUseMemberUserWhenLoggedIn() {
        TokenUser tokenUser = new TokenUser();
        tokenUser.setUserId(12L);
        LoginUserContext.set(tokenUser);

        UserAssistantChatRequest request = new UserAssistantChatRequest();
        request.setMessage("你好");
        request.setVisitorId("guest-1");

        UserAssistantChatResponse expected = new UserAssistantChatResponse("c1", "m1", "t1", "您好");
        when(difyChatClient.chat(any(UserAssistantChatRequest.class), eq("member:12"))).thenReturn(expected);

        UserAssistantChatResponse response = userAssistantService.chat(request);

        assertEquals("您好", response.getAnswer());
        verify(difyChatClient).chat(request, "member:12");
    }

    @Test
    void shouldUseVisitorIdWhenAnonymous() {
        UserAssistantChatRequest request = new UserAssistantChatRequest();
        request.setMessage("你好");
        request.setVisitorId("guest-1");

        UserAssistantChatResponse expected = new UserAssistantChatResponse("c1", "m1", "t1", "您好");
        when(difyChatClient.chat(any(UserAssistantChatRequest.class), eq("visitor:guest-1"))).thenReturn(expected);

        UserAssistantChatResponse response = userAssistantService.chat(request);

        assertEquals("c1", response.getConversationId());
        verify(difyChatClient).chat(request, "visitor:guest-1");
    }

    @Test
    void shouldRejectAnonymousRequestWithoutVisitorId() {
        UserAssistantChatRequest request = new UserAssistantChatRequest();
        request.setMessage("你好");

        BusinessException exception = assertThrows(BusinessException.class, () -> userAssistantService.chat(request));

        assertEquals("未登录时 visitorId 不能为空", exception.getMessage());
    }
}
