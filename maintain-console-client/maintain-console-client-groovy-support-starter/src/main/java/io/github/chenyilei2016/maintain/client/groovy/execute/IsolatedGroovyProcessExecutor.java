package io.github.chenyilei2016.maintain.client.groovy.execute;

import io.github.chenyilei2016.maintain.client.groovy.configuration.MaintainConsoleGroovyProperties;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 每次调用启动独立 Worker JVM，确保超时后能够终止失控脚本。
 * 文件与网络权限由 Worker 所在容器或操作系统账号限制。
 */
final class IsolatedGroovyProcessExecutor {
    private static final String SUCCESS_PREFIX = "MC_OK:";
    private static final String ERROR_PREFIX = "MC_ERROR:";
    private static final int MAX_WORKER_ERROR_BYTES = 16 * 1024;

    private final MaintainConsoleGroovyProperties properties;

    IsolatedGroovyProcessExecutor(MaintainConsoleGroovyProperties properties) {
        this.properties = properties;
    }

    String execute(String script) {
        ScriptRiskPolicy.validate(script, properties.getMaxScriptLength(), properties.isAllowDangerousScripts());
        validateConfiguration();
        Path workDirectory = null;
        Process process = null;
        try {
            workDirectory = Files.createTempDirectory("maintain-console-worker-");
            Path resultFile = workDirectory.resolve("result.txt");
            Path errorFile = workDirectory.resolve("worker.log");
            ProcessBuilder processBuilder = new ProcessBuilder(
                    javaExecutable(),
                    "-Xmx" + properties.getWorkerMaxMemoryMb() + "m",
                    "-XX:MaxMetaspaceSize=128m",
                    "-Djava.io.tmpdir=" + workDirectory,
                    "-jar",
                    properties.getWorkerJarPath(),
                    String.valueOf(properties.getMaxScriptLength()),
                    String.valueOf(properties.isAllowDangerousScripts()),
                    String.valueOf(properties.getWorkerMaxResultBytes())
            );
            processBuilder.directory(workDirectory.toFile());
            processBuilder.redirectOutput(resultFile.toFile());
            processBuilder.redirectError(errorFile.toFile());
            process = processBuilder.start();
            try (OutputStream input = process.getOutputStream()) {
                input.write(Base64.getEncoder().encode(script.getBytes(StandardCharsets.UTF_8)));
            }
            if (!process.waitFor(properties.getWorkerTimeoutSeconds(), TimeUnit.SECONDS)) {
                stop(process);
                throw new IllegalStateException("Groovy Worker execution timed out");
            }
            String response = readBounded(resultFile, properties.getWorkerMaxResultBytes() * 2L);
            if (response.startsWith(SUCCESS_PREFIX)) {
                return decode(response.substring(SUCCESS_PREFIX.length()));
            }
            if (response.startsWith(ERROR_PREFIX)) {
                throw new IllegalStateException(decode(response.substring(ERROR_PREFIX.length())));
            }
            String workerLog = readBounded(errorFile, MAX_WORKER_ERROR_BYTES);
            throw new IllegalStateException("Groovy Worker exited with code " + process.exitValue()
                    + (workerLog.isEmpty() ? "" : ": " + workerLog));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start Groovy Worker", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (process != null) {
                stop(process);
            }
            throw new IllegalStateException("Groovy Worker execution interrupted", e);
        } finally {
            deleteWorkDirectory(workDirectory);
        }
    }

    private void validateConfiguration() {
        if (properties.getWorkerJarPath() == null
                || !Files.isRegularFile(Paths.get(properties.getWorkerJarPath()))) {
            throw new IllegalStateException("maintain.console.groovy.worker-jar-path must reference the Worker jar");
        }
        if (properties.getWorkerMaxMemoryMb() < 64 || properties.getWorkerMaxMemoryMb() > 4096) {
            throw new IllegalArgumentException("worker-max-memory-mb must be between 64 and 4096");
        }
        if (properties.getWorkerTimeoutSeconds() < 1 || properties.getWorkerTimeoutSeconds() > 3600) {
            throw new IllegalArgumentException("worker-timeout-seconds must be between 1 and 3600");
        }
        if (properties.getWorkerMaxResultBytes() < 1024
                || properties.getWorkerMaxResultBytes() > 16 * 1024 * 1024) {
            throw new IllegalArgumentException("worker-max-result-bytes must be between 1024 and 16777216");
        }
    }

    private String javaExecutable() {
        String executable = System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java";
        return Paths.get(System.getProperty("java.home"), "bin", executable).toString();
    }

    private static String readBounded(Path file, long maxBytes) throws IOException {
        if (!Files.exists(file)) {
            return "";
        }
        if (Files.size(file) > maxBytes) {
            throw new IllegalStateException("Groovy Worker output exceeds the configured limit");
        }
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8).trim();
    }

    private static String decode(String value) {
        try {
            return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Groovy Worker returned an invalid response", e);
        }
    }

    private static void stop(Process process) {
        process.destroy();
        try {
            if (!process.waitFor(1, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    private static void deleteWorkDirectory(Path directory) {
        if (directory == null) {
            return;
        }
        List<String> fileNames = java.util.Arrays.asList("result.txt", "worker.log");
        for (String fileName : fileNames) {
            try {
                Files.deleteIfExists(directory.resolve(fileName));
            } catch (IOException ignored) {
                // Temporary files are best-effort cleanup only.
            }
        }
        try {
            Files.deleteIfExists(directory);
        } catch (IOException ignored) {
            // Temporary directory is best-effort cleanup only.
        }
    }
}
