package com.sparkleshop.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparkleshop.common.core.constant.CommonConstants;
import com.sparkleshop.common.core.exception.BusinessException;
import com.sparkleshop.common.core.model.Result;
import com.sparkleshop.common.security.constant.SecurityConstants;
import com.sparkleshop.common.security.jwt.JwtTokenService;
import com.sparkleshop.common.security.jwt.TokenUser;
import com.sparkleshop.gateway.config.GatewaySecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GatewayAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtTokenService jwtTokenService;
    private final GatewaySecurityProperties gatewaySecurityProperties;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String traceId = resolveTraceId(exchange);
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().set(CommonConstants.TRACE_ID_HEADER, traceId);

        String authorization = exchange.getRequest().getHeaders().getFirst(SecurityConstants.AUTHORIZATION_HEADER);
        boolean whitelisted = isWhitelisted(path);
        if (whitelisted) {
            return chain.filter(mutateExchange(exchange, traceId, null));
        }
        if (!StringUtils.hasText(authorization)) {
            return writeFailure(response, HttpStatus.UNAUTHORIZED, Result.UNAUTHORIZED, "未登录或登录态无效", traceId);
        }

        try {
            TokenUser tokenUser = jwtTokenService.authenticate(authorization);
            return chain.filter(mutateExchange(exchange, traceId, tokenUser));
        } catch (BusinessException exception) {
            return writeFailure(response, resolveHttpStatus(exception.getCode()), exception.getCode(), exception.getMessage(), traceId);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private ServerWebExchange mutateExchange(ServerWebExchange exchange, String traceId, TokenUser tokenUser) {
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(SecurityConstants.AUTHORIZATION_HEADER);
                    headers.remove(SecurityConstants.USER_ID_HEADER);
                    headers.remove(SecurityConstants.USER_TYPE_HEADER);
                    headers.remove(SecurityConstants.TOKEN_ID_HEADER);
                    headers.set(CommonConstants.TRACE_ID_HEADER, traceId);
                    if (tokenUser != null) {
                        headers.set(SecurityConstants.USER_ID_HEADER, String.valueOf(tokenUser.getUserId()));
                        headers.set(SecurityConstants.USER_TYPE_HEADER, String.valueOf(tokenUser.getUserType()));
                        headers.set(SecurityConstants.TOKEN_ID_HEADER, tokenUser.getTokenId());
                    }
                })
                .build();
        return exchange.mutate().request(mutatedRequest).build();
    }

    private boolean isWhitelisted(String path) {
        return gatewaySecurityProperties.getWhitelist().stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private String resolveTraceId(ServerWebExchange exchange) {
        String traceId = exchange.getRequest().getHeaders().getFirst(CommonConstants.TRACE_ID_HEADER);
        if (StringUtils.hasText(traceId)) {
            return traceId;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    private Mono<Void> writeFailure(ServerHttpResponse response,
                                    HttpStatus status,
                                    Integer code,
                                    String message,
                                    String traceId) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Result body = Result.error(code, message).traceId(traceId);
        byte[] content = toJsonBytes(body);
        DataBuffer buffer = response.bufferFactory().wrap(content);
        return response.writeWith(Mono.just(buffer));
    }

    private byte[] toJsonBytes(Result body) {
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException exception) {
            return ("{\"code\":50000,\"msg\":\"" + exception.getMessage() + "\"}").getBytes(StandardCharsets.UTF_8);
        }
    }

    private HttpStatus resolveHttpStatus(Integer code) {
        if (code == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        int prefix = code / 100;
        return switch (prefix) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 409 -> HttpStatus.CONFLICT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
