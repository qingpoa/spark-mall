package com.sparkleshop.common.security.config;

import com.sparkleshop.common.security.feign.SecurityFeignRequestInterceptor;
import com.sparkleshop.common.security.jwt.JwtProperties;
import feign.RequestInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecuritySharedAutoConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RequestInterceptor securityFeignRequestInterceptor() {
        return new SecurityFeignRequestInterceptor();
    }
}
