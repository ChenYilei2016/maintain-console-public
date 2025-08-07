package cn.chenyilei.maintain.manager.pojo.vo;

import cn.chenyilei.maintain.manager.exceptions.CommonException;
import cn.chenyilei.maintain.manager.pojo.entity.DirectoryNode;
import cn.chenyilei.maintain.manager.pojo.entity.Script;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 脚本内容业务实体类
 *
 * @author chenyilei
 * @since 2025/07/31
 */
@Data
@Accessors(chain = true)
public class ScriptVO {


    private Script script;

    private DirectoryNode directoryNode;


    public String getScriptContent() {
        return this.script.getContent();
    }

    public String getScriptPermissions() {
        return this.script.getPermissions();
    }

    public String getServiceName() {
        return this.directoryNode.getServiceName();
    }

    public static String mergeParamScript(final String script, String params) {
        if (null == params) {
            params = "{}";
        }
        JSONObject jsonObject;
        try {
            jsonObject = JSON.parseObject(params);
        } catch (Throwable e) {
            throw CommonException.createBizException("脚本变量异常", e);
        }

        //替换脚本中的$${}字符
        String scriptContent = script;
        for (String key : jsonObject.keySet()) {
            String value = jsonObject.getString(key);
            if (value == null) {
                value = "";
            }
            scriptContent = scriptContent.replaceAll("\\$\\$\\{\\s*" + key + "\\s*}", value);
        }
        return scriptContent;
    }
}