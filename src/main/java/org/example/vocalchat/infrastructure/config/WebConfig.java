package org.example.vocalchat.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.example.vocalchat.infrastructure.interceptor.UserInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final UserInterceptor userInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/public/user/register",
                        "/api/public/user/login",
                        "/api/public/user/getVerificationCode",
                        "/error"
                )
                .order(1);
    }
}