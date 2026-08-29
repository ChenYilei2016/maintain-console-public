package io.github.chenyilei2016.maintain.manager.pojo.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScriptExecutionResultTest {

    @Test
    void wrapsLegacyTextAndPlainJson() {
        assertEquals("text", ScriptExecutionResult.fromRaw("done").getBlocks().get(0).getType());
        assertEquals("json", ScriptExecutionResult.fromRaw("{\"count\":1}").getBlocks().get(0).getType());
    }

    @Test
    void acceptsKnownBlocksAndDowngradesUnknownTypes() {
        ScriptExecutionResult result = ScriptExecutionResult.fromRaw("""
                {"protocolVersion":1,"blocks":[
                  {"type":"metric","title":"总数","data":{"value":12}},
                  {"type":"html","data":"<script>alert(1)</script>"}
                ]}
                """);

        assertEquals("metric", result.getBlocks().get(0).getType());
        assertEquals("text", result.getBlocks().get(1).getType());
    }

    @Test
    void downgradesMalformedChartToJson() {
        ScriptExecutionResult result = ScriptExecutionResult.fromRaw(
                "{\"protocolVersion\":1,\"blocks\":[{\"type\":\"chart\",\"data\":{\"chartType\":\"radar\"}}]}");

        assertEquals("json", result.getBlocks().get(0).getType());
    }

    @Test
    void acceptsSafeChartAndRejectsExternalFileUrl() {
        ScriptExecutionResult result = ScriptExecutionResult.fromRaw("""
                {"protocolVersion":1,"blocks":[
                  {"type":"chart","data":{"chartType":"line","labels":["Mon","Tue"],
                    "series":[{"name":"count","data":[1,2]}]}},
                  {"type":"file","data":{"name":"report.csv","url":"https://evil.example/report.csv"}}
                ]}
                """);

        assertEquals("chart", result.getBlocks().get(0).getType());
        assertEquals("json", result.getBlocks().get(1).getType());
    }

    @Test
    void acceptsBoundedInlineFile() {
        ScriptExecutionResult result = ScriptExecutionResult.fromRaw("""
                {"protocolVersion":1,"blocks":[{"type":"file","data":{
                  "name":"report.csv","mimeType":"text/csv","size":3,"contentBase64":"YSxi"
                }}]}
                """);

        assertEquals("file", result.getBlocks().getFirst().getType());
    }
}
