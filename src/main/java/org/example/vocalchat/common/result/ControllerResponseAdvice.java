package org.example.vocalchat.common.result;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.vocalchat.common.annotation.AutoResult;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 统一响应体包装处理器。
 *
 * <p>对于标注了 {@link AutoResult} 的 Controller 类或方法，
 * 自动将返回值包装为 {@link BaseResult#success(Object)}。</p>
 *
 * <p>已在 {@code BaseResult} 中的返回值（如 {@link org.example.vocalchat.common.exception.GlobalExceptionHandler}
 * 返回的异常响应）不会被重复包装。</p>
 */
@Slf4j
@RestControllerAdvice
public class ControllerResponseAdvice implements ResponseBodyAdvice<Object> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 判断是否需要包装：方法或所在类标注了 {@link AutoResult} 则包装。
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 方法级别优先
        if (returnType.getMethod() != null
                && returnType.getMethod().isAnnotationPresent(AutoResult.class)) {
            return true;
        }
        // 类级别
        return returnType.getDeclaringClass().isAnnotationPresent(AutoResult.class);
    }

    /**
     * 将成功的返回值包装为 {@link BaseResult#success(Object)}。
     */
    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        // 已经是统一响应格式，不重复包装（如 GlobalExceptionHandler 返回的错误响应）
        if (body instanceof BaseResult) {
            return body;
        }

        // String 需要特殊处理：StringHttpMessageConverter 只能写 String，
        // 所以在 supports 中过滤掉 String 返回值，或在此转为 JSON 字符串
        if (body instanceof String) {
            try {
                return objectMapper.writeValueAsString(BaseResult.success(body));
            } catch (Exception e) {
                log.error("序列化统一响应体失败", e);
                return BaseResult.error(5000, "系统内部错误");
            }
        }

        return BaseResult.success(body);
    }
}