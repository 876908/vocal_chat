package org.example.vocalchat.common.exception;

import lombok.Getter;
import org.example.vocalchat.common.enums.ErrorEnum;

@Getter
public class BaseException extends RuntimeException {

    private final Integer code;

    public BaseException(ErrorEnum errorEnum) {
        super(errorEnum.getMsg());
        this.code = errorEnum.getCode();
    }

    public BaseException(ErrorEnum errorEnum, Throwable cause) {
        super(errorEnum.getMsg(), cause);
        this.code = errorEnum.getCode();
    }

    public BaseException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
