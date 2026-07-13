package org.example.vocalchat.infrastructure.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.vocalchat.common.annotation.SkipToken;
import org.example.vocalchat.common.context.UserContext;
import org.example.vocalchat.common.enums.ErrorEnum;
import org.example.vocalchat.common.exception.BaseException;
import org.example.vocalchat.common.util.JwtUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class UserInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 类或方法上有 @SkipToken 则跳过鉴权
        if (handlerMethod.getMethodAnnotation(SkipToken.class) != null
                || handlerMethod.getBeanType().isAnnotationPresent(SkipToken.class)) {
            return true;
        }

        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new BaseException(ErrorEnum.TOKEN_MISSING);
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
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
