package com.sparkleshop.gateway.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security.gateway")
public class GatewaySecurityProperties {

    private List<String> whitelist = new ArrayList<>(List.of(
            "/api/v1/user/register",
            "/api/v1/user/login",
            "/api/v1/product/list",
            "/api/v1/product/hot",
            "/api/v1/product/*",
            "/api/v1/product/category/**",
            "/api/v1/product/brand/**"
    ));

    public List<String> getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(List<String> whitelist) {
        this.whitelist = whitelist;
    }
}
