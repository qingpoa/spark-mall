package com.sparkleshop.service.user.support.dify;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkleshop.common.core.exception.BusinessException;
import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.service.user.dto.assistant.UserAssistantChatRequest;
import com.sparkleshop.service.user.vo.assistant.UserAssistantChatResponse;
import lombok.Data;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DefaultDifyChatClient implements DifyChatClient {

    private final DifyProperties difyProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public DefaultDifyChatClient(DifyProperties difyProperties,
                                 ObjectMapper objectMapper,
                                 RestTemplateBuilder restTemplateBuilder) {
        this.difyProperties = difyProperties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplateBuilder
                .connectTimeout(difyProperties.getConnectTimeout())
                .readTimeout(difyProperties.getReadTimeout())
                .build();
    }

    @Override
    public UserAssistantChatResponse chat(UserAssistantChatRequest request, String difyUser) {
        if (StrUtil.isBlank(difyProperties.getBaseUrl()) || StrUtil.isBlank(difyProperties.getApiKey())) {
            throw new BusinessException(Result.SERVER_ERROR, "Dify 配置未完成，请先配置 DIFY_BASE_URL 和 DIFY_API_KEY");
        }

        String url = buildChatMessagesUrl();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(difyProperties.getApiKey().trim());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        Map<String, Object> inputs = new LinkedHashMap<>();
        if (request.getInputs() != null) {
            inputs.putAll(request.getInputs());
        }
        if (StrUtil.isNotBlank(request.getUserToken())) {
            inputs.put("usertoken", request.getUserToken().trim());
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("inputs", inputs);
        payload.put("query", request.getMessage());
        payload.put("response_mode", "blocking");
        payload.put("user", difyUser);
        if (StrUtil.isNotBlank(request.getConversationId())) {
            payload.put("conversation_id", request.getConversationId().trim());
        }

        try {
            ResponseEntity<DifyChatMessageResponse> response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(payload, headers),
                    DifyChatMessageResponse.class
            );
            DifyChatMessageResponse body = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || body == null || StrUtil.isBlank(body.getAnswer())) {
                throw new BusinessException(Result.SERVER_ERROR, "Dify 返回结果异常，请稍后重试");
            }
            return new UserAssistantChatResponse(
                    body.getConversationId(),
                    body.getMessageId(),
                    body.getTaskId(),
                    body.getAnswer()
            );
        } catch (HttpStatusCodeException exception) {
            throw new BusinessException(Result.SERVER_ERROR, extractErrorMessage(exception));
        } catch (ResourceAccessException exception) {
            throw new BusinessException(Result.SERVER_ERROR, "请求 Dify 超时或连接失败，请稍后重试");
        }
    }

    private String extractErrorMessage(HttpStatusCodeException exception) {
        String responseBody = exception.getResponseBodyAsString();
        if (StrUtil.isBlank(responseBody)) {
            return "请求 Dify 失败，请稍后重试";
        }
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            JsonNode messageNode = jsonNode.get("message");
            if (messageNode != null && StrUtil.isNotBlank(messageNode.asText())) {
                return "Dify 调用失败: " + messageNode.asText();
            }
        } catch (Exception ignored) {
            // ignore
        }
        return "请求 Dify 失败，请稍后重试";
    }

    private String buildChatMessagesUrl() {
        String baseUrl = StrUtil.removeSuffix(difyProperties.getBaseUrl().trim(), "/");
        if (baseUrl.endsWith("/v1")) {
            return baseUrl + "/chat-messages";
        }
        return baseUrl + "/v1/chat-messages";
    }

    @Data
    private static class DifyChatMessageResponse {

        @JsonProperty("conversation_id")
        private String conversationId;

        @JsonProperty("message_id")
        private String messageId;

        @JsonProperty("task_id")
        private String taskId;

        private String answer;
    }
}
