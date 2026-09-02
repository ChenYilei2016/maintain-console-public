package io.github.chenyilei2016.maintain.client.groovy.execute;

import groovy.json.JsonOutput;
import groovy.json.JsonSlurper;
import org.slf4j.event.Level;
import org.slf4j.helpers.MessageFormatter;

import java.io.Writer;
import java.util.*;

/**
 * 本次脚本日志只进入有界结果，不输出到未脱敏的应用日志。
 */
public final class ScriptExecutionLog extends Writer {
    private static final int MAX_LOG_CHARACTERS = 16 * 1024;
    private final StringBuilder content = new StringBuilder();
    private boolean truncated;

    public void info(String message, Object... arguments) {
        append(Level.INFO, message, arguments);
    }

    public void warn(String message, Object... arguments) {
        append(Level.WARN, message, arguments);
    }

    public void error(String message, Object... arguments) {
        append(Level.ERROR, message, arguments);
    }

    public void debug(String message, Object... arguments) {
        append(Level.DEBUG, message, arguments);
    }

    public void trace(String message, Object... arguments) {
        append(Level.TRACE, message, arguments);
    }

    private void append(Level level, String message, Object[] arguments) {
        String formatted = MessageFormatter.arrayFormat(message, arguments).getMessage();
        appendBounded("[" + level + "] " + formatted + "\n");
    }

    private synchronized void appendBounded(String value) {
        int remaining = MAX_LOG_CHARACTERS - content.length();
        if (value.length() > remaining) truncated = true;
        if (remaining > 0) content.append(value, 0, Math.min(value.length(), remaining));
    }

    @Override
    public void write(char[] value, int offset, int length) {
        appendBounded(new String(value, offset, Math.min(length, MAX_LOG_CHARACTERS)));
        if (length > MAX_LOG_CHARACTERS) truncated = true;
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

    public synchronized Object withResult(Object result) {
        Object parsed = result;
        if (result instanceof String) {
            try {
                parsed = new JsonSlurper().parseText((String) result);
            } catch (RuntimeException ignored) {
                parsed = result;
            }
        }
        boolean protocolResult = parsed instanceof Map
                && Integer.valueOf(1).equals(((Map<?, ?>) parsed).get("protocolVersion"))
                && ((Map<?, ?>) parsed).get("blocks") instanceof List;
        boolean singleBlock = parsed instanceof Map
                && ((Map<?, ?>) parsed).get("type") instanceof CharSequence
                && ((Map<?, ?>) parsed).containsKey("title")
                && ((Map<?, ?>) parsed).containsKey("data");
        boolean hasLogs = content.length() > 0 || truncated;
        if (!hasLogs) {
            if (protocolResult) {
                return result instanceof String ? result
                        : toProtocolJson((List<?>) ((Map<?, ?>) parsed).get("blocks"));
            }
            return singleBlock ? toProtocolJson(Collections.singletonList(parsed)) : result;
        }

        List<Object> blocks = new ArrayList<>();
        if (protocolResult) {
            blocks.addAll((List<?>) ((Map<?, ?>) parsed).get("blocks"));
        } else if (singleBlock) {
            blocks.add(parsed);
        } else {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("type", result instanceof String ? "text" : "json");
            value.put("data", result);
            blocks.add(value);
        }
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("type", "log");
        log.put("title", "本次过程日志");
        log.put("data", content.toString() + (truncated ? "\n...日志已截断" : ""));
        blocks.add(log);
        return toProtocolJson(blocks);
    }

    private static String toProtocolJson(List<?> blocks) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("protocolVersion", 1);
        payload.put("blocks", blocks);
        return JsonOutput.toJson(payload);
    }
}
