package io.github.chenyilei2016.maintain.client.groovy.execute;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class GroovyScriptEngineTest {
    private final GroovyScriptEngine engine = new GroovyScriptEngine();

    @Test
    public void rendersStructuredResultAndRejectsProcessAccess() {
        assertEquals(
                "{\"protocolVersion\":1,\"blocks\":[{\"type\":\"metric\",\"title\":\"count\",\"data\":{\"value\":3}}]}",
                engine.execute("return result(resultMetric('count', [value: 3]))", new Object(), 4096, false)
        );
        try {
            engine.execute("return new ProcessBuilder('whoami').start()", new Object(), 4096, false);
            fail("dangerous process access must be rejected");
        } catch (SecurityException expected) {
            // expected
        }
    }
}
