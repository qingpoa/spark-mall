package com.sparkleshop.common.security.config;

import com.sparkleshop.common.log.filter.TraceIdFilter;
import com.sparkleshop.common.security.web.RequestUserContextInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnWebApplication(type = Type.SERVLET)
public class SecurityAutoConfiguration implements WebMvcConfigurer {

    @Bean
    public TraceIdFilter traceIdFilter() {
        return new TraceIdFilter();
    }

    @Bean
    public RequestUserContextInterceptor requestUserContextInterceptor() {
        return new RequestUserContextInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestUserContextInterceptor()).addPathPatterns("/**");
    }
}
