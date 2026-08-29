package io.github.chenyilei2016.maintain.client.groovy.execute;

import io.github.chenyilei2016.maintain.client.common.console.IMaintainConsoleExecutor;
import io.github.chenyilei2016.maintain.client.common.dto.RuntimeMetadataDTO;
import io.github.chenyilei2016.maintain.client.groovy.configuration.MaintainConsoleGroovyProperties;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author chenyilei
 * @since 2024/05/20 14:32
 */
public class GroovyMaintainConsoleExecutor implements IMaintainConsoleExecutor, ApplicationContextAware {

    private ApplicationContext applicationContext;

    private final MaintainConsoleGroovyProperties properties;
    private final GroovyScriptEngine scriptEngine = new GroovyScriptEngine();

    public GroovyMaintainConsoleExecutor() {
        this(new MaintainConsoleGroovyProperties());
    }

    public GroovyMaintainConsoleExecutor(MaintainConsoleGroovyProperties properties) {
        this.properties = properties;
    }

    public Object execute(String script) {
        if (properties.getExecutionMode() == MaintainConsoleGroovyProperties.ExecutionMode.ISOLATED_PROCESS) {
            return new IsolatedGroovyProcessExecutor(properties).execute(script);
        }
        Object context = properties.isExposeApplicationContext()
                ? applicationContext
                : new RestrictedBeanContext(applicationContext, properties.getAllowedBeanNames());
        return scriptEngine.execute(script, context, properties.getMaxScriptLength(), properties.isAllowDangerousScripts());
    }

    @Override
    public RuntimeMetadataDTO runtimeMetadata() {
        if (properties.isExposeApplicationContext()
                || properties.getExecutionMode() == MaintainConsoleGroovyProperties.ExecutionMode.ISOLATED_PROCESS) {
            return new RuntimeMetadataDTO();
        }
        return new RestrictedBeanContext(applicationContext, properties.getAllowedBeanNames()).metadata(
                properties.getMetadataMaxBeans(), properties.getMetadataMaxMethodsPerBean());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
