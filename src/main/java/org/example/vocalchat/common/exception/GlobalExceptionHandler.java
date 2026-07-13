package org.example.vocalchat.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.example.vocalchat.common.enums.ErrorEnum;
import org.example.vocalchat.common.result.BaseResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BaseException.class)
    public BaseResult<Void> handleBaseException(BaseException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return BaseResult.error(e.getCode(), e.getMessage());
    }
    @ExceptionHandler(Exception.class)
    public BaseResult<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return BaseResult.error(ErrorEnum.SYSTEM_ERROR);
    }
}