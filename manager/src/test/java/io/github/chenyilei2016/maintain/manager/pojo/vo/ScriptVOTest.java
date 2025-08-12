package io.github.chenyilei2016.maintain.manager.pojo.vo;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chenyilei
 * @since 2025/08/12 14:11
 */
public class ScriptVOTest {

    @Test
    public void mergeParamScript_t1() {
        String s = ScriptVO.mergeParamScript("echo $${  a } nihao", "{\"a\":\"1\"}");
        System.err.println(s);
        Assert.assertEquals("echo 1 nihao", s);
    }

    @Test
    public void mergeParamScript_t2() {
        String s = ScriptVO.mergeParamScript("echo $${  a$${b}  } nihao", "{\"a\":\"1\" ,\"b\":\"a\"}");
        System.err.println(s);
        //优先替换整体变量
        Assert.assertEquals("echo null nihao", s);
    }
}