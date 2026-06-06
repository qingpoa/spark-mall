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
            "/api/v1/user/assistant/chat",
            "/admin/auth/register",
            "/admin/auth/login",
            "/api/v1/product/list",
            "/api/v1/product/hot",
            "/api/v1/product/*",
            "/api/v1/product/category/**",
            "/api/v1/product/brand/**"
    ));
    private List<String> memberPaths = new ArrayList<>(List.of("/api/v1/**"));
    private List<String> adminPaths = new ArrayList<>(List.of("/admin/**"));

    public List<String> getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(List<String> whitelist) {
        this.whitelist = whitelist;
    }

    public List<String> getMemberPaths() {
        return memberPaths;
    }

    public void setMemberPaths(List<String> memberPaths) {
        this.memberPaths = memberPaths;
    }

    public List<String> getAdminPaths() {
        return adminPaths;
    }

    public void setAdminPaths(List<String> adminPaths) {
        this.adminPaths = adminPaths;
    }
}
