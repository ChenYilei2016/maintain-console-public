import org.codehaus.groovy.control.CompilerConfiguration
import org.junit.Test

/**
 * @author chenyilei
 *  2022/05/08 14:42
 */
class ConsoleBindingTest {
/**
 *       groovyShell.parse(getClass().getResource("/com//ConsoleBase.groovy").toURI());
 *       groovyShell.evaluate("setProperty('vars', new com.service.ConsoleBase())");
 */

    def consoleBase = """
            class ConsoleBase {
                def storage = [:] //空map
                
                //属性拦截
                def propertyMissing(String name, value) { this.storage[name] = value }
            
                def propertyMissing(String name) { this.storage[name] }
                
            } """

    @Test
    public void hahatest() {
        Binding binding = new Binding();
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.setScriptBaseClass(ExtScript.class.getName());
        GroovyShell groovyShell = new GroovyShell(new CClassLoader(this.getClass().getClassLoader()), binding, compilerConfiguration);
        groovyShell.parse(consoleBase);

        groovyShell.evaluate("setProperty('vars', new ConsoleBase())");

        def result = groovyShell.evaluate("""
            println 666
            vars.a = 'b'
""");

        System.err.println(result)
    }

    /**
     * property variable 好像没区别
     */
    @Test
    void binding() {
        def p1 = new ConsoleBase()
        p1.setProperty("p1K", "p1V")
        def v1 = new ConsoleBase()
        v1.setProperty("v1K", "v1V");

        Binding binding = new Binding();
        binding.setVariable("name", "Tom");
        binding.setVariable("age", 16);
        binding.setVariable("v1", v1)

        binding.setProperty("favourite", "play paino");
        binding.setProperty("p1", p1)
        GroovyShell shell = new GroovyShell(binding);

        shell.evaluate("""
        println name
        println age
        println favourite
        
        println "binding.name: "+ binding.name
        println "binding.favourite: " + binding.favourite
        
        println "p1.p1K: " + p1.p1K
        println "v1.v1K: " + v1.v1K
        
        v1.aaa = 7
        p1.aaa = 8
        
        name = '修改后的name'
        favourite = '修改后的favourite'
        
        def privateVar = 'privateVar Val'  //def: 不会被binding所读取到 , 私有作用域
""");
        println p1.storage //[p1K:p1V, aaa:8]
        println v1.storage //[v1K:v1V, aaa:7]
        println binding.variables
        //[name:修改后的name, age:16, v1:ConsoleBase@3b7d3a38, favourite:修改后的favourite, p1:ConsoleBase@416c58f5] //没有 privateVar

        println "========"

        //这里注意 用 '' 防止引用外部字符串

        //welcome to 8 ;age is 7;favourite is 7
        shell.evaluate 'println  \"welcome to $p1.aaa ;age is $v1.aaa;favourite is $v1.aaa \"; return v1.aaa';

        //welcome to 修改后的name-修改后的favourite ;age is 8;favourite is 7
        shell.evaluate 'println  \"welcome to ${name}-${favourite} ;age is $p1.aaa;favourite is $v1.aaa \"; return v1.aaa';

    }
}
