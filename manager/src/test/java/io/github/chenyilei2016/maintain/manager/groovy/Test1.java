package io.github.chenyilei2016.maintain.manager.groovy;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.junit.Test;

public class Test1 {


    @Test
    public void test1() {
        String groovyStr = "package io.github.chenyilei2016.maintain.manager.groovy; \n" +
                "\n" +
                "\n" +
                "\n" +
                "\n" +
                "def helloWord() {\n" +
                "    return \"hello groovy\"\n" +
                "}\n" +
                "\n" +
                "helloWord()\n" +
                "\n" +
                "def cal(int a, int b) {" +
                "  " +
                "    return a + b\n" +
                "};\n" +
                "\n" +
                "cal(a , b)";

        // 创建GroovyShell实例
        GroovyShell shell = new GroovyShell();
        Script script = shell.parse(groovyStr);
        Object helloWord = script.invokeMethod("helloWord", null);

        System.err.println(helloWord);
    }

    @Test
    public void classloader() {
        GroovyClassLoader groovyClassLoader = new GroovyClassLoader();
//        groovyClassLoader.loadClass()

    }
}
