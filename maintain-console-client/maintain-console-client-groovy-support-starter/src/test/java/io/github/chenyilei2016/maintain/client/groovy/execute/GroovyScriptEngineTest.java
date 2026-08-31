package io.github.chenyilei2016.maintain.client.groovy.execute;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class GroovyScriptEngineTest {
    private final GroovyScriptEngine engine = new GroovyScriptEngine();

    @Test
    public void returnsBoundedLogsWithCurrentResult() {
        String result = String.valueOf(engine.execute("_log.info('value {}', 42); println('printed'); return result(resultText('answer', 'ok'))", new Object(), 4096, false));
        org.junit.Assert.assertTrue(result.contains("本次过程日志"));
        org.junit.Assert.assertTrue(result.contains("value 42"));
        org.junit.Assert.assertTrue(result.contains("printed"));
        String bounded = String.valueOf(engine.execute("_log.info('x' * 100000); return 1", new Object(), 4096, false));
        org.junit.Assert.assertTrue(bounded.length() < 20_000);
        org.junit.Assert.assertTrue(bounded.contains("日志已截断"));
    }

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
