package io.github.chenyilei2016.maintain.manager.pojo.vo;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.github.chenyilei2016.maintain.manager.exceptions.CommonException;
import io.github.chenyilei2016.maintain.manager.pojo.entity.DirectoryNode;
import io.github.chenyilei2016.maintain.manager.pojo.entity.Script;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static final Pattern pattern = Pattern.compile("\\$\\$\\{(.*)}");

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
        StringBuffer append = new StringBuffer();
        Matcher matcher = pattern.matcher(script);

        while (matcher.find()) {
            String variable_trim = matcher.group(1).trim();
            Object param = jsonObject.get(variable_trim);
            if (null == param) {
                param = "null";  //脚本中替换空字符串
            }
            matcher.appendReplacement(append, param.toString());
        }
        matcher.appendTail(append);
        return append.toString();
    }
}