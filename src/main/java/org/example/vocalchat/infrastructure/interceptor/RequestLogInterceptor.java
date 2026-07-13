package org.example.vocalchat.infrastructure.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Slf4j
@Component
public class RequestLogInterceptor implements HandlerInterceptor {

    private static final String START_TIME = "startTime";
    private static final String TRACE_ID = "traceId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        request.setAttribute(TRACE_ID, traceId);
        request.setAttribute(START_TIME, System.currentTimeMillis());
        log.info("[{}] {} {}", traceId, request.getMethod(), request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        long cost = System.currentTimeMillis() - (long) request.getAttribute(START_TIME);
        String traceId = (String) request.getAttribute(TRACE_ID);
        log.info("[{}] {}ms", traceId, cost);
    }
}
