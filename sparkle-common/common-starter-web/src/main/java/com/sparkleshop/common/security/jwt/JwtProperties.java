package com.sparkleshop.common.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

    private String secret = "change-this-jwt-secret-to-a-secure-value-2026";
    private String issuer = "sparkle-shop";
    private long expirationSeconds = 7200;
}
