package io.github.chenyilei2016.maintain.client.groovy.execute;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import io.github.chenyilei2016.maintain.client.groovy.BaseConsoleExtService;
import io.github.chenyilei2016.maintain.client.groovy.ConsoleStorage;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 与 Spring 解耦的 Groovy 执行内核，供客户端执行器和独立 Worker 复用。
 */
public final class GroovyScriptEngine {
    private static final String CONTEXT_VARIABLE = "ctx";

    public Object execute(String script, Object context, int maxScriptLength, boolean allowDangerousScripts) {
        if (script == null || script.trim().isEmpty()) {
            throw new IllegalArgumentException("Script text to compile cannot be blank");
        }
        ScriptRiskPolicy.validate(script, maxScriptLength, allowDangerousScripts);
        try (GroovyClassLoader classLoader = new GroovyClassLoader(getClass().getClassLoader())) {
            CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
            compilerConfiguration.setScriptBaseClass(BaseConsoleExtService.class.getName());
            compilerConfiguration.setSourceEncoding(StandardCharsets.UTF_8.name());
            Binding binding = new Binding();
            binding.setVariable("vars", new ConsoleStorage());
            binding.setVariable(CONTEXT_VARIABLE, context);
            binding.setVariable(CONTEXT_VARIABLE.toUpperCase(), context);
            ScriptExecutionLog executionLog = new ScriptExecutionLog();
            binding.setVariable("_log", executionLog);
            binding.setVariable("out", new java.io.PrintWriter(executionLog));
            return executionLog.withResult(new GroovyShell(classLoader, binding, compilerConfiguration).evaluate(script));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to close Groovy class loader", e);
        }
    }
}
