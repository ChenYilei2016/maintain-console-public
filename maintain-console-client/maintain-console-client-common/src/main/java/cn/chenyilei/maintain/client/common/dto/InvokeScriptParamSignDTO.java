package cn.chenyilei.maintain.client.common.dto;

import cn.chenyilei.maintain.client.common.utils.RSAUtil;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

/**
 * @author chenyilei
 * @since 2024/05/17 11:43
 */
@Data
@ToString(callSuper = true)
@NoArgsConstructor
public class InvokeScriptParamSignDTO extends BaseSignDTO implements Serializable {

    private static final long serialVersionUID = 7401184479519916227L;

    private String script;

    public InvokeScriptParamSignDTO(String script) {
        this.script = script;
    }

    @Override
    public String signData() {
        if (script == null) {
            return "";
        }
        return RSAUtil.MD5(script.getBytes(StandardCharsets.UTF_8));
    }
}
