package io.github.chenyilei2016.maintain.manager.pojo.entity;

import org.junit.jupiter.api.Test;

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
                com.alibaba.fastjson2.JSON.toJSONString(java.util.Map.of("value", value))).executableContent();
        assertEquals(value, new groovy.lang.GroovyShell().evaluate(resolved));
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
        assertEquals("token=******", result.sanitizeResult("token=secret"));
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
}
