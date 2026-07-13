package org.example.vocalchat.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.vocalchat.common.enums.ErrorEnum;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseResult<T> {
    private Boolean success;
    private Integer errCode;
    private String errMsg;
    private T data;

    public static <T> BaseResult<T> success() {
        return new BaseResult<>(true, ErrorEnum.SUCCESS.getCode(), ErrorEnum.SUCCESS.getMsg(), null);
    }

    public static <T> BaseResult<T> success(T data) {
        return new BaseResult<>(true, ErrorEnum.SUCCESS.getCode(), ErrorEnum.SUCCESS.getMsg(), data);
    }

    public static <T> BaseResult<T> error(Integer errCode, String errMsg) {
        return new BaseResult<>(false, errCode, errMsg, null);
    }

    public static <T> BaseResult<T> error(ErrorEnum errorEnum) {
        return new BaseResult<>(false, errorEnum.getCode(), errorEnum.getMsg(), null);
    }
}
