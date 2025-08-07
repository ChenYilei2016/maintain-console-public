package cn.chenyilei.maintain.manager.params;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author chenyilei
 * @since 2025/08/06 16:42
 */
public class TestParamsReplace {

    @Test
    public void test1() {
        String yuan = "$${d1} $${ d2}  $${d3 }  $${ d4  }";

        yuan = yuan.replaceAll("\\$\\$\\{\\s?" + "d1" + "\\s?}", "a1")
                .replaceAll("\\$\\$\\{\\s?" + "d2" + "\\s?}", "a2")
                .replaceAll("\\$\\$\\{\\s?" + "d3" + "\\s?}", "a3")
                .replaceAll("\\$\\$\\{\\s*" + "d4" + "\\s*}", "a4");
        System.err.println(yuan);
        Assert.assertEquals("a1 a2  a3  a4", yuan);
    }
}
