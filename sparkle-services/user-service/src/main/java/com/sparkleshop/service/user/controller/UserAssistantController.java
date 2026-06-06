package com.sparkleshop.service.user.controller;

import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.common.web.util.Results;
import com.sparkleshop.service.user.dto.assistant.UserAssistantChatRequest;
import com.sparkleshop.service.user.service.UserAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/user/assistant")
public class UserAssistantController {

    private final UserAssistantService userAssistantService;

    @PostMapping("/chat")
    public ResponseEntity<Result> chat(@Valid @RequestBody UserAssistantChatRequest request) {
        return Results.ok(userAssistantService.chat(request));
    }
}
