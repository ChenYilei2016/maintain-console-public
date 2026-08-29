package io.github.chenyilei2016.maintain.client.groovy.execute;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

final class ScriptRiskPolicy {
    private static final List<Pattern> FORBIDDEN_PATTERNS = Arrays.asList(
            Pattern.compile("\\bSystem\\s*\\.\\s*exit\\s*\\("),
            Pattern.compile("\\bRuntime\\s*\\.\\s*getRuntime\\s*\\("),
            Pattern.compile("\\bProcessBuilder\\b"),
            Pattern.compile("\\bClass\\s*\\.\\s*forName\\s*\\("),
            Pattern.compile("\\bGroovyShell\\b"),
            Pattern.compile("\\bGroovyClassLoader\\b"),
            Pattern.compile("@Grab\\b"),
            Pattern.compile("\\.\\s*classLoader\\b"),
            Pattern.compile("\\b(?:new\\s+)?File\\s*\\("),
            Pattern.compile("\\bFiles\\s*\\."),
            Pattern.compile("\\b(?:URL|Socket|ServerSocket|HttpClient)\\b"),
            Pattern.compile("\\bThread\\s*[.(]"),
            Pattern.compile("\\bExecutors\\s*\\."),
            Pattern.compile("\\bSystem\\s*\\.\\s*(?:getenv|getProperties|setProperty)\\s*\\("),
            Pattern.compile("\\bmetaClass\\b")
    );

    private ScriptRiskPolicy() {
    }

    static void validate(String script, int maxScriptLength, boolean allowDangerousScripts) {
        if (script == null || script.trim().isEmpty()) {
            throw new IllegalArgumentException("Script text to compile cannot be blank");
        }
        if (maxScriptLength <= 0 || script.length() > maxScriptLength) {
            throw new IllegalArgumentException("Script exceeds the configured length limit");
        }
        if (allowDangerousScripts) {
            return;
        }
        for (Pattern pattern : FORBIDDEN_PATTERNS) {
            if (pattern.matcher(script).find()) {
                throw new SecurityException("Script contains a forbidden runtime operation: " + pattern.pattern());
            }
        }
    }
}
