package com.sparkleshop.common.security.feign;

import com.sparkleshop.common.core.constant.CommonConstants;
import com.sparkleshop.common.core.trace.TraceContext;
import com.sparkleshop.common.security.constant.SecurityConstants;
import com.sparkleshop.common.security.jwt.LoginUserContext;
import com.sparkleshop.common.security.jwt.TokenUser;
import feign.RequestInterceptor;
import feign.RequestTemplate;

public class SecurityFeignRequestInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        template.header(CommonConstants.TRACE_ID_HEADER, TraceContext.getTraceId());
        TokenUser tokenUser = LoginUserContext.get();
        if (tokenUser == null) {
            template.header(SecurityConstants.USER_ID_HEADER);
            template.header(SecurityConstants.USER_TYPE_HEADER);
            template.header(SecurityConstants.TOKEN_ID_HEADER);
            return;
        }
        template.header(SecurityConstants.USER_ID_HEADER, String.valueOf(tokenUser.getUserId()));
        template.header(SecurityConstants.USER_TYPE_HEADER, String.valueOf(tokenUser.getUserType()));
        template.header(SecurityConstants.TOKEN_ID_HEADER, tokenUser.getTokenId());
    }
}
