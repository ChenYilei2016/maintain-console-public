package cn.chenyilei.maintain.client.groovy.execute;

import cn.chenyilei.maintain.client.common.console.IMaintainConsoleExecutor;
import cn.chenyilei.maintain.client.groovy.BaseConsoleExtService;
import cn.chenyilei.maintain.client.groovy.ConsoleStorage;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * @author chenyilei
 * @since 2024/05/20 14:32
 */
public class GroovyMaintainConsoleExecutor implements IMaintainConsoleExecutor, ApplicationContextAware {

    private ApplicationContext applicationContext;

    private static final String VARS = "vars";

    private static final String CTX = "ctx";

    private static final String LOG = "_log";

    Logger _log = LoggerFactory.getLogger("maintain-console-exe");

    public Object execute(String script) {
        if (!StringUtils.hasText(script)) {
            throw new IllegalArgumentException("Script text to compile cannot be null!");
        }
        GroovyClassLoader groovyClassLoader = new GroovyClassLoader(this.getClass().getClassLoader());
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.setScriptBaseClass(BaseConsoleExtService.class.getName());
        compilerConfiguration.setSourceEncoding(StandardCharsets.UTF_8.toString());
        Binding binding = new Binding();
        binding.setVariable(VARS, new ConsoleStorage());
        binding.setVariable(CTX, applicationContext);
        binding.setVariable(CTX.toUpperCase(), applicationContext);
        binding.setVariable(LOG, _log);
        return new GroovyShell(groovyClassLoader, binding, compilerConfiguration).evaluate(script);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
