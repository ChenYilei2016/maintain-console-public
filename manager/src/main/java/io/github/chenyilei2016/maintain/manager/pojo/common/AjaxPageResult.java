package io.github.chenyilei2016.maintain.manager.pojo.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Ajax分页返回类
 *
 * @author chenyilei
 * @email 705029004@qq.com
 * 2019/03/29 9:56
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AjaxPageResult<T> extends AjaxResult<T> implements Serializable {
    private int page = 1;
    private int pageSize = 10;
    private long totalElements;

    public AjaxPageResult(boolean success, T data, String msg
            , int page, int pageSize, long totalElements) {
        super(success, data, msg);
        this.page = page;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
    }


}
