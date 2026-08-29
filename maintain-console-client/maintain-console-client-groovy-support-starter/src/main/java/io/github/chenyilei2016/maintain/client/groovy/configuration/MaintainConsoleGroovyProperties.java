package io.github.chenyilei2016.maintain.client.groovy.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashSet;
import java.util.Set;

@ConfigurationProperties(prefix = "maintain.console.groovy")
public class MaintainConsoleGroovyProperties {
    public enum ExecutionMode {
        IN_PROCESS,
        ISOLATED_PROCESS
    }

    private boolean exposeApplicationContext;
    private Set<String> allowedBeanNames = new HashSet<>();
    private int maxScriptLength = 1_048_576;
    private boolean allowDangerousScripts;
    private int metadataMaxBeans = 100;
    private int metadataMaxMethodsPerBean = 100;
    private ExecutionMode executionMode = ExecutionMode.IN_PROCESS;
    private String workerJarPath;
    private int workerMaxMemoryMb = 256;
    private int workerTimeoutSeconds = 180;
    private int workerMaxResultBytes = 2 * 1024 * 1024;

    public boolean isExposeApplicationContext() {
        return exposeApplicationContext;
    }

    public void setExposeApplicationContext(boolean exposeApplicationContext) {
        this.exposeApplicationContext = exposeApplicationContext;
    }

    public Set<String> getAllowedBeanNames() {
        return allowedBeanNames;
    }

    public void setAllowedBeanNames(Set<String> allowedBeanNames) {
        this.allowedBeanNames = allowedBeanNames;
    }

    public int getMaxScriptLength() {
        return maxScriptLength;
    }

    public void setMaxScriptLength(int maxScriptLength) {
        this.maxScriptLength = maxScriptLength;
    }

    public boolean isAllowDangerousScripts() {
        return allowDangerousScripts;
    }

    public void setAllowDangerousScripts(boolean allowDangerousScripts) {
        this.allowDangerousScripts = allowDangerousScripts;
    }

    public int getMetadataMaxBeans() {
        return metadataMaxBeans;
    }

    public void setMetadataMaxBeans(int metadataMaxBeans) {
        this.metadataMaxBeans = metadataMaxBeans;
    }

    public int getMetadataMaxMethodsPerBean() {
        return metadataMaxMethodsPerBean;
    }

    public void setMetadataMaxMethodsPerBean(int metadataMaxMethodsPerBean) {
        this.metadataMaxMethodsPerBean = metadataMaxMethodsPerBean;
    }

    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(ExecutionMode executionMode) {
        this.executionMode = executionMode;
    }

    public String getWorkerJarPath() {
        return workerJarPath;
    }

    public void setWorkerJarPath(String workerJarPath) {
        this.workerJarPath = workerJarPath;
    }

    public int getWorkerMaxMemoryMb() {
        return workerMaxMemoryMb;
    }

    public void setWorkerMaxMemoryMb(int workerMaxMemoryMb) {
        this.workerMaxMemoryMb = workerMaxMemoryMb;
    }

    public int getWorkerTimeoutSeconds() {
        return workerTimeoutSeconds;
    }

    public void setWorkerTimeoutSeconds(int workerTimeoutSeconds) {
        this.workerTimeoutSeconds = workerTimeoutSeconds;
    }

    public int getWorkerMaxResultBytes() {
        return workerMaxResultBytes;
    }

    public void setWorkerMaxResultBytes(int workerMaxResultBytes) {
        this.workerMaxResultBytes = workerMaxResultBytes;
    }
}
