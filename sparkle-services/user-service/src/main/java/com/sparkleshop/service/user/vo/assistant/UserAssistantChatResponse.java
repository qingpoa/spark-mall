package com.sparkleshop.service.user.vo.assistant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAssistantChatResponse {

    private String conversationId;
    private String messageId;
    private String taskId;
    private String answer;
}
