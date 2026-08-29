package io.github.chenyilei2016.maintain.client.groovy.execute;

import io.github.chenyilei2016.maintain.client.groovy.configuration.MaintainConsoleGroovyProperties;
import org.junit.Test;
import org.springframework.context.support.StaticApplicationContext;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class GroovyMaintainConsoleExecutorTest {

    @Test
    public void exposesOnlyConfiguredBeansAndBlocksDangerousRuntimeCalls() {
        MaintainConsoleGroovyProperties properties = new MaintainConsoleGroovyProperties();
        properties.setAllowedBeanNames(Collections.singleton("safeBean"));
        GroovyMaintainConsoleExecutor executor = new GroovyMaintainConsoleExecutor(properties);
        StaticApplicationContext context = new StaticApplicationContext();
        context.getBeanFactory().registerSingleton("safeBean", "safe");
        context.getBeanFactory().registerSingleton("hiddenBean", "hidden");
        executor.setApplicationContext(context);

        assertEquals("safe", executor.execute("return ctx.getBean('safeBean')"));
        assertRejected(executor, "return ctx.getBean('hiddenBean')");
        assertRejected(executor, "Runtime.getRuntime().exec('whoami')");
    }

    private void assertRejected(GroovyMaintainConsoleExecutor executor, String script) {
        try {
            executor.execute(script);
            fail("script must be rejected");
        } catch (SecurityException expected) {
            // expected
        }
    }
}
