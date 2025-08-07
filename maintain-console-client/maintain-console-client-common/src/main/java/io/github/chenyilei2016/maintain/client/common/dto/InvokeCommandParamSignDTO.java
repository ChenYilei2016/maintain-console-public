package io.github.chenyilei2016.maintain.client.common.dto;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author chenyilei
 * @since 2024/05/17 11:44
 */
@Data
@ToString(callSuper = true)
public class InvokeCommandParamSignDTO extends BaseSignDTO implements Serializable {

    private static final long serialVersionUID = 3147271378102085282L;

    @Override
    public String signData() {
        return "";
    }
}
