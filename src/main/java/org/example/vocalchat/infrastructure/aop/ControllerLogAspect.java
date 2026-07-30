package org.example.vocalchat.infrastructure.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.vocalchat.common.annotation.LogOperation;
import org.example.vocalchat.common.context.UserContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
public class ControllerLogAspect {

    private static final int MAX_RESULT_LENGTH = 200;

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String operation = getOperation(joinPoint);
        String uri = getRequestUri();
        String params = formatArgs(joinPoint.getArgs());
        String user = getCurrentUser();

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;
            String resultStr = formatResult(result);
            log.info("=捕获响应=\n操作描述：{}\n请求地址：{}\n请求参数：{}\n请求返回：{}\n请求用户：{}\n请求耗时：{}ms\n=释放响应=",
                    operation, uri, params, resultStr, user, cost);
            return result;
        } catch (Throwable e) {
            long cost = System.currentTimeMillis() - start;
            log.error("=捕获响应=\n操作描述：{}\n请求地址：{}\n请求参数：{}\n请求异常：{}\n请求用户：{}\n请求耗时：{}ms\n=释放响应=",
                    operation, uri, params, e.getMessage(), user, cost);
            throw e;
        }
    }

    private String getOperation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        LogOperation anno = signature.getMethod().getAnnotation(LogOperation.class);
        return anno != null ? anno.value() : signature.getMethod().getName();
    }

    private String getRequestUri() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return attrs.getRequest().getRequestURI();
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }

    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        return Arrays.stream(args)
                .map(arg -> arg != null ? arg.toString() : "null")
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private String formatResult(Object result) {
        if (result == null) {
            return "void";
        }
        String str = result.toString();
        if (str.length() > MAX_RESULT_LENGTH) {
            return str.substring(0, MAX_RESULT_LENGTH) + "...";
        }
        return str;
    }

    private String getCurrentUser() {
        try {
            String userId = UserContext.getUserId();
            return userId != null ? userId : "anonymous";
        } catch (Exception ignored) {
            return "anonymous";
        }
    }
}