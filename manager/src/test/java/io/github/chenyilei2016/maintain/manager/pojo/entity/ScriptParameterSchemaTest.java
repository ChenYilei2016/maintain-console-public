package io.github.chenyilei2016.maintain.manager.pojo.entity;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import groovy.json.JsonOutput;
import groovy.lang.GroovyShell;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScriptParameterSchemaTest {
    @Test
    void typedValuesCannotEscapeTheirGroovyExpression() {
        ScriptParameterSchema schema = ScriptParameterSchema.parse("""
                {"version":1,"parameters":[{"name":"value","type":"STRING"}]}
                """);
        for (String script : new String[]{"return '$${value}'", "return \"$${value}\"",
                "return '''$${value}'''", "return /$${value}/", "// $${value}\nreturn 1",
                "def prefix$${value} = 1"}) {
            assertThrows(IllegalArgumentException.class, () -> schema.validateForScript(script), script);
        }
        String value = "'; throw new RuntimeException(); //\\\n${1 + 1}";
        String resolved = schema.resolve("return $${value}",
                JSON.toJSONString(Map.of("value", value))).executableContent();
        assertEquals(value, new GroovyShell().evaluate(resolved));
    }

    private static final String SCHEMA = """
            {
              "version": 1,
              "parameters": [
                {"name":"name","type":"STRING","required":true},
                {"name":"count","type":"NUMBER","min":1,"max":10,"defaultValue":2},
                {"name":"token","type":"STRING","required":true,"sensitive":true}
              ]
            }
            """;

    @Test
    void resolvesTypedParametersAndMasksSensitiveValues() {
        ScriptParameterSchema schema = ScriptParameterSchema.parse(SCHEMA);

        ScriptParameterSchema.ResolvedScript result = schema.resolve(
                "return [$${name}, $${count}, $${token}]",
                "{\"name\":\"O'Reilly\",\"token\":\"secret\"}");

        assertEquals("return ['O\\'Reilly', 2, 'secret']", result.executableContent());
        assertEquals("return ['O\\'Reilly', 2, '******']", result.persistedContent());
        assertEquals("{\"name\":\"O'Reilly\",\"count\":2,\"token\":\"******\"}", result.persistedParameters());
        assertEquals("token=******", ScriptExecutionResult.fromRaw(result.sanitizeResult("token=secret")).primaryText());
    }

    @Test
    void sanitizesEscapedJsonWithoutCorruptingProtocolFields() {
        var resolved = new ScriptParameterSchema.ResolvedScript("", "", "{}", List.of("秘密\n\"", "1"));
        String raw = JsonOutput.toJson(Map.of("protocolVersion", 1, "blocks", List.of(
                Map.of("type", "json", "data", Map.of("secret", "秘密\n\"", "number", 1)))));
        var result = ScriptExecutionResult.fromRaw(resolved.sanitizeResult(raw));
        assertEquals(1, result.getProtocolVersion());
        assertEquals("******", ((JSONObject) result.getBlocks().getFirst().getData()).getString("secret"));
        assertEquals("******", ((JSONObject) result.getBlocks().getFirst().getData()).getString("number"));
    }

    @Test
    void rejectsSchemaThatDoesNotMatchScript() {
        ScriptParameterSchema schema = ScriptParameterSchema.parse(SCHEMA);

        assertThrows(IllegalArgumentException.class,
                () -> schema.validateForScript("return $${name}"));
    }

    @Test
    void rejectsNumberOutsideConfiguredRange() {
        ScriptParameterSchema schema = ScriptParameterSchema.parse(SCHEMA);

        assertThrows(IllegalArgumentException.class, () -> schema.resolve(
                "return [$${name}, $${count}, $${token}]",
                "{\"name\":\"test\",\"count\":11,\"token\":\"secret\"}"));
    }

    @Test
    void jsonAndBooleanRemainDataAndUnknownParametersAreRejected() {
        var schema = ScriptParameterSchema.parse("""
                {"version":1,"parameters":[{"name":"payload","type":"JSON"},{"name":"enabled","type":"BOOLEAN"}]}
                """);
        String script = "return [$${payload}, $${enabled}]";
        String payload = JSON.toJSONString(Map.of("text", "\'; throw new RuntimeException(); //\\\n${1+1}"));
        var parameters = new JSONObject();
        parameters.put("payload", payload);
        parameters.put("enabled", false);
        var resolved = schema.resolve(script, parameters.toJSONString());
        var values = (List<?>) new GroovyShell().evaluate(resolved.executableContent());
        assertEquals(JSON.parse(payload), JSON.parse((String) values.getFirst()));
        assertEquals(false, values.get(1));
        parameters.put("unknown", "ignored?");
        assertThrows(IllegalArgumentException.class, () -> schema.resolve(script, parameters.toJSONString()));
        parameters.remove("unknown");
        parameters.put("enabled", "false; return 1");
        assertThrows(IllegalArgumentException.class, () -> schema.resolve(script, parameters.toJSONString()));
    }
}
