package org.example.vocalchat.infrastructure.interceptor;

import org.example.vocalchat.common.annotation.SkipToken;
import org.example.vocalchat.common.context.UserContext;
import org.example.vocalchat.common.enums.ErrorEnum;
import org.example.vocalchat.common.exception.BaseException;
import org.example.vocalchat.common.util.JwtUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserInterceptor implements HandlerInterceptor {

    private static final String TOKEN_HEADER = "Token";

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        if (handlerMethod.getMethodAnnotation(SkipToken.class) != null
                || handlerMethod.getBeanType().isAnnotationPresent(SkipToken.class)) {
            return true;
        }

        String token = request.getHeader(TOKEN_HEADER);
        if (token == null || token.isBlank()) {
            throw new BaseException(ErrorEnum.TOKEN_MISSING);
        }

        var claims = jwtUtil.parseJWT(token);
        String userId = claims.get("userId", String.class);
        UserContext.set(userId, token);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
