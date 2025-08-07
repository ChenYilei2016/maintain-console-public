package io.github.chenyilei2016.maintain.client.common.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author chenyilei
 * @since 2024/05/17 11:31
 */
@Data
public class ApiResult<T> implements Serializable {

    private static final long serialVersionUID = -8624020938251901038L;

    private boolean success;
    private T data;
    private String msg;

    public static <T> ApiResult<T> error(String msg) {
        return new ApiResult<>(false, null, msg);
    }

    public static <T> ApiResult<T> success(T data, String msg) {
        return new ApiResult<>(true, data, msg);
    }

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(true, data, null);
    }

    public ApiResult(boolean success, T data, String msg) {
        this.success = success;
        this.data = data;
        this.msg = msg;
    }
}
