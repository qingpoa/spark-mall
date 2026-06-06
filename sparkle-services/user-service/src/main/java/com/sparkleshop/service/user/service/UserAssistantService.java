package com.sparkleshop.service.user.service;

import com.sparkleshop.service.user.dto.assistant.UserAssistantChatRequest;
import com.sparkleshop.service.user.vo.assistant.UserAssistantChatResponse;

public interface UserAssistantService {

    UserAssistantChatResponse chat(UserAssistantChatRequest request);
}
