package cn.chenyilei.maintain.client.common.dto;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author chenyilei
 * @since 2024/05/17 11:43
 */
@Data
@ToString(callSuper = true)
public class InvokeScriptResultDTO implements Serializable {

    private static final long serialVersionUID = -4020232886232229403L;

    protected long timestamp = System.currentTimeMillis();

    private String scriptResult;

}
