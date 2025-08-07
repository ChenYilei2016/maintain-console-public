import lombok.ToString
import org.junit.Test

import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * @author chenyilei
 *  2022/05/08 11:32
 */


class _5_闭包 {

    @ToString
    //lombok 不兼容, 需要额外办法
    static class A {
        Integer a;

        @Override
        public String toString() {
            return "A{" +
                    "a=" + a +
                    '}';
        }
    }

    @Test
    public void test1() {
        String a = 'a '
        Closure c1 = { String param1, String param2 ->
            System.out.println("c1" + a + param1 + param2);
            return 1
        }
        println c1.call('6', 'aaaa')


        def c2 = {
            println("666 ${it}") //默认的关键词 隐藏参数 it
        }
        assert "666 hello" == c2.call('hello')

    }

    /**
     * 源码: 外部引用的都变成了引用 和 java的lambda不一样
     * final Reference templateStr = new Reference("Hello");
     * final Reference normalStr = new Reference("normalStr1");
     *
     */
    @Test
    public void 闭包和变化的变量() {
        def templateStr = "Hello";
        def normalStr = "normalStr1";

        def clos = { param -> return "${templateStr} ${param} " + normalStr }
        assert 'Hello World normalStr1' == clos.call("World");

        //闭包里用的变量被修改了  看源码得知 => 字符串模板变量用的都是 Reference
        templateStr = "Welcome";
        normalStr = "normalStr2";
        clos.call("World");
        assert 'Welcome World normalStr2' == clos.call("World");

        assert 'Welcome World normalStr2' == testCall(clos)
    }


    def testCall(Closure c) {
        def normalStr = 'what'
        return c.call('World')
    }

    @Test
    void 闭包each() {
        def map = [
                'a': 1,
                'b': 2,
                'c': 3,
        ]
        map.each {
            if (it.value % 2 == 0) {
                println "偶数: ${it.value}"
            } else {
                println "奇数: ${it.value}"
            }
        }

        def a = new A(
                a: 66,
        )
        println '定义了一个A : ' + a.toString()

    }

    @Test
    void test() {
        def a = [[1, 2, 3], [4, 5, 6], [7, 8, 9, [10, 11, 12]], 5]

        def collect = a
                .stream().flatMap { it -> eeee(it) }
                .collect()
        println collect

        println a.stream().map { it -> 6 }.collect(Collectors.toList())
    }

    def eeee(Object list) {
        if (list instanceof List) {
            return ((List) list).stream().flatMap { it -> eeee(it) }
        }
        return Stream.of(list)
    }


}
