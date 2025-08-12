import org.codehaus.groovy.control.CompilerConfiguration
import org.junit.Test

/**
 * @author chenyilei
 *  2022/05/08 14:42
 */
class GroovyExecuteTest {
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
    public void executeTest() {
        Binding binding = new Binding();
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.setScriptBaseClass(ExtScript.class.getName());
        GroovyShell groovyShell = new GroovyShell(new CClassLoader(this.getClass().getClassLoader()), binding, compilerConfiguration);
        groovyShell.parse(consoleBase);

        groovyShell.evaluate("setProperty('vars', new ConsoleBase())");

        Object result = groovyShell.evaluate("""
            
            return [
                    "名称" : "sd"
            ]
""");

        System.err.println(result.getClass())
        System.err.println(result)
    }


    @Test
    public void executeTest_complier() {
        Binding binding = new Binding();
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.setScriptBaseClass(ExtScript.class.getName());
        GroovyShell groovyShell = new GroovyShell(new CClassLoader(this.getClass().getClassLoader()), binding, compilerConfiguration);
        groovyShell.parse(consoleBase);

        groovyShell.evaluate("setProperty('vars', new ConsoleBase())");

        Object result = groovyShell.evaluate("""
        
        vars.id =  
        vars.tn = '子不语'
        
        if(vars.id != null &&  vars.id != '') {
            info("租户ID: " + vars.id, null)
       
        } 
        
        if( vars.tn != null) {
            info("name" + vars.tn as String ,null)
            
        }
        
        return "无"
""");

        System.err.println(result.getClass())
        System.err.println(result)
    }

}
