package com.sparkleshop.common.log.filter;

import com.sparkleshop.common.core.constant.CommonConstants;
import com.sparkleshop.common.core.trace.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = request.getHeader(CommonConstants.TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = TraceContext.getTraceId();
        } else {
            TraceContext.setTraceId(traceId);
        }
        response.setHeader(CommonConstants.TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TraceContext.clear();
        }
    }
}
