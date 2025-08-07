package cn.chenyilei.maintain.manager.pojo.common;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * ajax页面返回主体
 *
 * @author chenyilei
 * 2019/03/14- 22:13
 */
@Data
@NoArgsConstructor
public class AjaxResult<T> implements Serializable {

    private boolean success;
    private T data;
    private String msg;
    private int code = 200;

    public static AjaxResult error(String msg) {
        return new AjaxResult(false, null, msg);
    }

    public static <T> AjaxResult<T> success(T data, String msg) {
        return new AjaxResult<>(true, data, msg);
    }

    public static <T> AjaxResult<T> success(T data) {
        return new AjaxResult<>(true, data, null);
    }

    public AjaxResult(boolean success, T data, String msg) {
        this.success = success;
        this.data = data;
        this.msg = msg;
    }
}
