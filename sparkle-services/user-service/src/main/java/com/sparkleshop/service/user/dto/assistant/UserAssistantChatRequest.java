package com.sparkleshop.service.user.dto.assistant;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class UserAssistantChatRequest {

    @NotBlank(message = "消息内容不能为空")
    private String message;

    private String conversationId;

    private String visitorId;

    @JsonAlias({"usertoken"})
    private String userToken;

    private Map<String, Object> inputs = new LinkedHashMap<>();
}
