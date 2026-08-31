package io.github.chenyilei2016.maintain.manager.pojo.entity;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class ScriptParameterSchema {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\$\\{\\s*([^{}]+?)\\s*}");
    private static final String MASKED_VALUE = "******";

    private int version = 1;
    private List<ParameterDefinition> parameters = new ArrayList<>();

    public static ScriptParameterSchema parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            ScriptParameterSchema schema = JSON.parseObject(json, ScriptParameterSchema.class);
            schema.validateDefinitions();
            return schema;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("参数 Schema 不是合法 JSON", e);
        }
    }

    public String validateForScript(String script) {
        validateDefinitions();
        ScriptParameterPlacement.validate(script == null ? "" : script, PLACEHOLDER);
        Set<String> placeholders = new HashSet<>();
        Matcher matcher = PLACEHOLDER.matcher(script == null ? "" : script);
        while (matcher.find()) {
            placeholders.add(matcher.group(1).trim());
        }
        Set<String> declared = new HashSet<>();
        for (ParameterDefinition parameter : parameters) {
            declared.add(parameter.getName());
        }
        if (!declared.equals(placeholders)) {
            Set<String> missing = new HashSet<>(placeholders);
            missing.removeAll(declared);
            Set<String> unused = new HashSet<>(declared);
            unused.removeAll(placeholders);
            throw new IllegalArgumentException("参数 Schema 与脚本占位符不一致，未声明: " + missing + "，未使用: " + unused);
        }
        return JSON.toJSONString(this);
    }

    public ResolvedScript resolve(String script, String paramsJson) {
        validateForScript(script);
        JSONObject input;
        try {
            input = paramsJson == null || paramsJson.isBlank() ? new JSONObject() : JSON.parseObject(paramsJson);
            if (input == null) throw new IllegalArgumentException("脚本参数必须是 JSON 对象");
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("脚本参数不是合法 JSON", e);
        }

        Map<String, ParameterDefinition> definitions = new LinkedHashMap<>();
        for (ParameterDefinition parameter : parameters) {
            definitions.put(parameter.getName(), parameter);
        }
        Set<String> unknown = new HashSet<>(input.keySet());
        unknown.removeAll(definitions.keySet());
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("存在未声明的脚本参数: " + unknown);
        }

        JSONObject persistedParameters = new JSONObject();
        List<String> sensitiveValues = new ArrayList<>();
        StringBuilder executable = new StringBuilder();
        StringBuilder persisted = new StringBuilder();
        Matcher matcher = PLACEHOLDER.matcher(script);
        int previousEnd = 0;
        while (matcher.find()) {
            String name = matcher.group(1).trim();
            ParameterDefinition definition = definitions.get(name);
            Object value = input.containsKey(name) ? input.get(name) : definition.getDefaultValue();
            if (!hasValue(value) && definition.isRequired()) {
                throw new IllegalArgumentException("缺少必填参数: " + name);
            }
            String rendered = hasValue(value) ? definition.getType().render(value, definition) : "null";
            executable.append(script, previousEnd, matcher.start()).append(rendered);
            persisted.append(script, previousEnd, matcher.start())
                    .append(definition.isSensitive() ? groovyStringLiteral(MASKED_VALUE) : rendered);
            persistedParameters.put(name, definition.isSensitive() && hasValue(value) ? MASKED_VALUE : value);
            if (definition.isSensitive() && hasValue(value)) {
                sensitiveValues.add(String.valueOf(value));
            }
            previousEnd = matcher.end();
        }
        executable.append(script, previousEnd, script.length());
        persisted.append(script, previousEnd, script.length());
        return new ResolvedScript(executable.toString(), persisted.toString(), persistedParameters.toJSONString(), sensitiveValues);
    }

    private void validateDefinitions() {
        if (version != 1) {
            throw new IllegalArgumentException("不支持的参数 Schema 版本: " + version);
        }
        if (parameters == null) {
            parameters = new ArrayList<>();
        }
        Set<String> names = new HashSet<>();
        for (ParameterDefinition parameter : parameters) {
            if (parameter.getName() == null || parameter.getName().isBlank()) {
                throw new IllegalArgumentException("参数名称不能为空");
            }
            parameter.setName(parameter.getName().trim());
            if (!names.add(parameter.getName())) {
                throw new IllegalArgumentException("参数名称重复: " + parameter.getName());
            }
            if (parameter.getType() == null) {
                parameter.setType(ParameterType.STRING);
            }
            parameter.getType().validateDefinition(parameter);
        }
    }

    private static boolean hasValue(Object value) {
        return value != null && (!(value instanceof String text) || !text.isBlank());
    }

    public static String groovyStringLiteral(String value) {
        if (value == null) return "null";
        return "'" + value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t") + "'";
    }

    public record ResolvedScript(
            String executableContent,
            String persistedContent,
            String persistedParameters,
            List<String> sensitiveValues
    ) {
        public String sanitizeResult(String result) {
            if (result == null || sensitiveValues == null || sensitiveValues.isEmpty()) return result;
            String sanitized = result;
            List<String> ordered = sensitiveValues.stream().filter(value -> !value.isEmpty())
                    .sorted((left, right) -> Integer.compare(right.length(), left.length())).toList();
            for (String value : ordered) {
                sanitized = sanitized.replace(value, MASKED_VALUE);
                String encoded = JSON.toJSONString(value);
                sanitized = sanitized.replace(encoded.substring(1, encoded.length() - 1), MASKED_VALUE);
            }
            return sanitized;
        }
    }

    @Data
    public static class ParameterDefinition {
        private String name;
        private String label;
        private String group;
        private boolean advanced;
        private ParameterType type = ParameterType.STRING;
        private boolean required;
        private Object defaultValue;
        private String description;
        private String example;
        private List<String> options = new ArrayList<>();
        private String pattern;
        private BigDecimal min;
        private BigDecimal max;
        private boolean sensitive;
    }

    public enum ParameterType {
        STRING {
            @Override
            String render(Object value, ParameterDefinition definition) {
                String text = String.valueOf(value);
                validatePattern(text, definition);
                return groovyStringLiteral(text);
            }
        },
        NUMBER {
            @Override
            String render(Object value, ParameterDefinition definition) {
                BigDecimal number;
                try {
                    number = new BigDecimal(String.valueOf(value));
                } catch (NumberFormatException e) {
                    throw invalid(definition, "必须是数字");
                }
                if (definition.getMin() != null && number.compareTo(definition.getMin()) < 0) {
                    throw invalid(definition, "不能小于 " + definition.getMin());
                }
                if (definition.getMax() != null && number.compareTo(definition.getMax()) > 0) {
                    throw invalid(definition, "不能大于 " + definition.getMax());
                }
                return number.stripTrailingZeros().toPlainString();
            }
        },
        BOOLEAN {
            @Override
            String render(Object value, ParameterDefinition definition) {
                if (value instanceof Boolean bool) {
                    return bool.toString();
                }
                String text = String.valueOf(value);
                if (!"true".equalsIgnoreCase(text) && !"false".equalsIgnoreCase(text)) {
                    throw invalid(definition, "必须是 true 或 false");
                }
                return Boolean.toString(Boolean.parseBoolean(text));
            }
        },
        ENUM {
            @Override
            void validateDefinition(ParameterDefinition definition) {
                if (definition.getOptions() == null || definition.getOptions().isEmpty()) {
                    throw invalid(definition, "枚举选项不能为空");
                }
            }

            @Override
            String render(Object value, ParameterDefinition definition) {
                String text = String.valueOf(value);
                if (!definition.getOptions().contains(text)) {
                    throw invalid(definition, "不在可选值 " + definition.getOptions() + " 中");
                }
                return groovyStringLiteral(text);
            }
        },
        JSON {
            @Override
            String render(Object value, ParameterDefinition definition) {
                try {
                    Object parsed = value instanceof String text ? com.alibaba.fastjson2.JSON.parse(text) : value;
                    return groovyStringLiteral(com.alibaba.fastjson2.JSON.toJSONString(parsed));
                } catch (RuntimeException e) {
                    throw invalid(definition, "必须是合法 JSON");
                }
            }
        },
        MULTILINE {
            @Override
            String render(Object value, ParameterDefinition definition) {
                String text = String.valueOf(value);
                validatePattern(text, definition);
                return groovyStringLiteral(text);
            }
        },
        DATETIME {
            @Override
            String render(Object value, ParameterDefinition definition) {
                String text = String.valueOf(value);
                try {
                    LocalDateTime.parse(text);
                } catch (DateTimeParseException e) {
                    throw invalid(definition, "必须是 ISO-8601 日期时间");
                }
                return groovyStringLiteral(text);
            }
        },
        SERVICE_INSTANCE {
            @Override
            String render(Object value, ParameterDefinition definition) {
                return groovyStringLiteral(String.valueOf(value));
            }
        };

        void validateDefinition(ParameterDefinition definition) {
            if (definition.getMin() != null && definition.getMax() != null
                    && definition.getMin().compareTo(definition.getMax()) > 0) {
                throw invalid(definition, "最小值不能大于最大值");
            }
            if (definition.getPattern() != null && !definition.getPattern().isBlank()) {
                try {
                    Pattern.compile(definition.getPattern());
                } catch (RuntimeException e) {
                    throw invalid(definition, "正则表达式无效");
                }
            }
        }

        abstract String render(Object value, ParameterDefinition definition);

        static void validatePattern(String value, ParameterDefinition definition) {
            if (definition.getPattern() != null && !definition.getPattern().isBlank()
                    && !Pattern.matches(definition.getPattern(), value)) {
                throw invalid(definition, "格式不符合正则 " + definition.getPattern());
            }
        }

        static IllegalArgumentException invalid(ParameterDefinition definition, String reason) {
            return new IllegalArgumentException("参数 " + definition.getName() + " " + reason);
        }
    }
}
