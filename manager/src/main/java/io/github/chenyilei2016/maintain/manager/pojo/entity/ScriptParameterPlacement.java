package io.github.chenyilei2016.maintain.manager.pojo.entity;

import groovyjarjarantlr4.v4.runtime.CharStreams;
import org.apache.groovy.parser.antlr4.GroovyLexer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 使用已有 Groovy 词法器确认参数是独立表达式，拒绝引号、注释和标识符内的原样拼接。
 */
final class ScriptParameterPlacement {
    private ScriptParameterPlacement() {
    }

    static void validate(String script, Pattern placeholder) {
        Matcher matcher = placeholder.matcher(script);
        StringBuilder probe = new StringBuilder();
        List<Integer> starts = new ArrayList<>();
        List<String> names = new ArrayList<>();
        int previousEnd = 0;
        while (matcher.find()) {
            probe.append(script, previousEnd, matcher.start());
            starts.add(probe.codePointCount(0, probe.length()));
            names.add(matcher.group(1).trim());
            probe.append("__maintain_parameter__");
            previousEnd = matcher.end();
        }
        if (starts.isEmpty()) return;
        probe.append(script, previousEnd, script.length());
        GroovyLexer lexer = new GroovyLexer(CharStreams.fromString(probe.toString()));
        lexer.removeErrorListeners();
        Set<Integer> expressions = new HashSet<>();
        lexer.getAllTokens().stream()
                .filter(token -> token.getType() == GroovyLexer.Identifier
                        && "__maintain_parameter__".equals(token.getText()))
                .forEach(token -> expressions.add(token.getStartIndex()));
        for (int index = 0; index < starts.size(); index++) {
            if (!expressions.contains(starts.get(index))) {
                throw new IllegalArgumentException("参数 " + names.get(index)
                        + " 必须单独作为表达式引用，例如 def value = $${" + names.get(index)
                        + "}；请移除外层引号或注释并确认代码差异");
            }
        }
    }
}
