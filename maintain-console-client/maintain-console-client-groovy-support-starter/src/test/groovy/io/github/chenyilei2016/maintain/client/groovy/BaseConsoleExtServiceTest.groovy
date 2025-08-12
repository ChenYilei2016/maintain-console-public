package io.github.chenyilei2016.maintain.client.groovy

import org.junit.Test

/**
 * @author chenyilei
 * @since 2025/08/12 11:30
 */
class BaseConsoleExtServiceTest {


    @Test
    public void testStr() {
        // 测试字符串类型
        assert BaseConsoleExtService.str("hello") == "hello"
        assert BaseConsoleExtService.str("") == ""

        // 测试非字符串类型
        assert BaseConsoleExtService.str(123) == "'123'"
        assert BaseConsoleExtService.str(true) == "'true'"
        assert BaseConsoleExtService.str(null) == "'null'"

        // 测试对象
        List<String> list = ["a", "b"]
        assert BaseConsoleExtService.str(list) == "'[a, b]'"
    }
}
