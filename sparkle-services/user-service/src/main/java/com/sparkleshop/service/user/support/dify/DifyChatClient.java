package com.sparkleshop.service.user.support.dify;

import com.sparkleshop.service.user.dto.assistant.UserAssistantChatRequest;
import com.sparkleshop.service.user.vo.assistant.UserAssistantChatResponse;

public interface DifyChatClient {

    UserAssistantChatResponse chat(UserAssistantChatRequest request, String difyUser);
}
