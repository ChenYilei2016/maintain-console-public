package io.github.chenyilei2016.maintain.client.groovy.execute;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class GroovyScriptEngineTest {
    private final GroovyScriptEngine engine = new GroovyScriptEngine();

    @Test
    public void returnsBoundedLogsWithCurrentResult() {
        String result = String.valueOf(engine.execute("_log.info('value {}', 42); println('printed'); return resultText('answer', 'ok')", new Object(), 4096, false));
        java.util.Map<?, ?> payload = (java.util.Map<?, ?>) new groovy.json.JsonSlurper().parseText(result);
        java.util.List<?> blocks = (java.util.List<?>) payload.get("blocks");
        assertEquals("text", ((java.util.Map<?, ?>) blocks.get(0)).get("type"));
        assertEquals("log", ((java.util.Map<?, ?>) blocks.get(1)).get("type"));
        org.junit.Assert.assertTrue(result.contains("value 42"));
        org.junit.Assert.assertTrue(result.contains("printed"));
        String bounded = String.valueOf(engine.execute("_log.info('x' * 100000); return 1", new Object(), 4096, false));
        org.junit.Assert.assertTrue(bounded.length() < 20_000);
        org.junit.Assert.assertTrue(new groovy.json.JsonSlurper().parseText(bounded).toString().contains("日志已截断"));
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

    @Test
    public void normalizesSingleBlockWithoutRepeatingExistingEnvelope() {
        String expected = "{\"protocolVersion\":1,\"blocks\":[{\"type\":\"text\",\"title\":\"greeting\",\"data\":\"Hello\"}]}";

        assertEquals(expected, engine.execute(
                "return resultText('greeting', 'Hello')", new Object(), 4096, false));
        assertEquals(expected, engine.execute(
                "return result(resultText('greeting', 'Hello'))", new Object(), 4096, false));
    }
}
