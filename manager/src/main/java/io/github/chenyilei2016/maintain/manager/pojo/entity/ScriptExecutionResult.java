package io.github.chenyilei2016.maintain.manager.pojo.entity;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.util.*;

@Data
public class ScriptExecutionResult {

    private static final int MAX_PROTOCOL_LENGTH = 2 * 1024 * 1024;
    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "text", "log", "json", "table", "metric", "chart", "file", "error");
    private static final Set<String> CHART_TYPES = Set.of("line", "bar", "pie", "area", "scatter");
    private static final int MAX_TABLE_ROWS = 1_000;
    private static final int MAX_CHART_POINTS = 1_000;
    private static final int MAX_INLINE_FILE_BYTES = 1024 * 1024;
    private static final Set<String> INLINE_FILE_MIME_TYPES = Set.of(
            "text/plain", "text/csv", "application/json", "application/pdf",
            "application/zip", "application/octet-stream");

    private int protocolVersion = 1;
    private List<ResultBlock> blocks = new ArrayList<>();

    public static ScriptExecutionResult fromRaw(String raw) {
        String value = raw == null ? "" : raw;
        if (value.length() > MAX_PROTOCOL_LENGTH) {
            return text(value.substring(0, MAX_PROTOCOL_LENGTH) + "\n...结果已截断");
        }
        try {
            Object parsed = JSON.parse(value);
            if (parsed instanceof JSONObject object && object.containsKey("protocolVersion") && object.containsKey("blocks")) {
                ScriptExecutionResult result = object.to(ScriptExecutionResult.class);
                if (result.protocolVersion == 1 && result.blocks != null) {
                    result.normalizeBlocks();
                    return result;
                }
            }
            return block("json", parsed);
        } catch (RuntimeException ignored) {
            return text(value);
        }
    }

    public static ScriptExecutionResult error(String message) {
        return block("error", message == null ? "脚本执行失败" : message);
    }

    public String toJson() {
        return JSON.toJSONString(this);
    }

    public String primaryText() {
        if (blocks == null || blocks.isEmpty()) {
            return "";
        }
        Object data = blocks.get(0).getData();
        return data instanceof String text ? text : JSON.toJSONString(data);
    }

    private static ScriptExecutionResult text(String value) {
        return block("text", value);
    }

    private static ScriptExecutionResult block(String type, Object data) {
        ScriptExecutionResult result = new ScriptExecutionResult();
        ResultBlock block = new ResultBlock();
        block.setType(type);
        block.setData(data);
        result.blocks.add(block);
        return result;
    }

    private void normalizeBlocks() {
        List<ResultBlock> normalized = new ArrayList<>();
        for (ResultBlock block : blocks) {
            String type = block.type == null ? "text" : block.type.toLowerCase(Locale.ROOT);
            if (!SUPPORTED_TYPES.contains(type)) {
                normalized.add(block("text", JSON.toJSONString(block)).blocks.get(0));
                continue;
            }
            block.type = type;
            if (block.title != null && block.title.length() > 200) {
                block.title = block.title.substring(0, 200);
            }
            if ("table".equals(type) && !isTable(block.data)) {
                block.type = "json";
            } else if ("chart".equals(type) && !isChart(block.data)) {
                block.type = "json";
            } else if ("file".equals(type) && !isFile(block.data)) {
                block.type = "json";
            }
            normalized.add(block);
        }
        blocks = normalized;
    }

    private static boolean isTable(Object data) {
        if (!(data instanceof JSONObject table)) return false;
        if (!(table.get("columns") instanceof JSONArray columns)
                || !(table.get("rows") instanceof JSONArray rows) || columns.size() > 100) {
            return false;
        }
        boolean truncated = rows.size() > MAX_TABLE_ROWS;
        truncate(rows, MAX_TABLE_ROWS);
        if (truncated) {
            table.put("truncated", true);
        }
        return true;
    }

    private static boolean isChart(Object data) {
        if (!(data instanceof JSONObject chart)) return false;
        String chartType = chart.getString("chartType");
        if (chartType == null || !CHART_TYPES.contains(chartType.toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (chart.get("labels") instanceof JSONArray labels) {
            truncate(labels, MAX_CHART_POINTS);
        }
        if (chart.get("items") instanceof JSONArray items) {
            truncate(items, MAX_CHART_POINTS);
        }
        if (chart.get("series") instanceof JSONArray series) {
            if (series.size() > 20) return false;
            for (Object item : series) {
                if (!(item instanceof JSONObject seriesItem) || !(seriesItem.get("data") instanceof JSONArray points)) {
                    return false;
                }
                truncate(points, MAX_CHART_POINTS);
            }
        }
        return true;
    }

    private static boolean isFile(Object data) {
        if (!(data instanceof JSONObject file)) return false;
        String name = file.getString("name");
        String url = file.getString("url");
        if (name == null || name.isBlank() || name.length() > 255
                || name.contains("/") || name.contains("\\") || name.chars().anyMatch(Character::isISOControl)) {
            return false;
        }
        if (url != null) {
            return url.startsWith("/manager/files/");
        }
        String mimeType = file.getString("mimeType");
        String contentBase64 = file.getString("contentBase64");
        if (!INLINE_FILE_MIME_TYPES.contains(mimeType) || contentBase64 == null) {
            return false;
        }
        try {
            return Base64.getDecoder().decode(contentBase64).length <= MAX_INLINE_FILE_BYTES;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static void truncate(JSONArray values, int maxSize) {
        if (values.size() <= maxSize) return;
        while (values.size() > maxSize) {
            values.remove(values.size() - 1);
        }
    }

    @Data
    public static class ResultBlock {
        private String type;
        private String title;
        private Object data;
    }
}
