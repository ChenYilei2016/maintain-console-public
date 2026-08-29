package io.github.chenyilei2016.maintain.client.groovy.worker;

import io.github.chenyilei2016.maintain.client.groovy.execute.GroovyScriptEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public final class GroovyWorkerMain {
    private static final String SUCCESS_PREFIX = "MC_OK:";
    private static final String ERROR_PREFIX = "MC_ERROR:";

    private GroovyWorkerMain() {
    }

    public static void main(String[] args) {
        PrintStream protocolOutput = System.out;
        try {
            int maxScriptLength = parsePositiveInt(args, 0, 1_048_576);
            boolean allowDangerousScripts = args.length > 1 && Boolean.parseBoolean(args[1]);
            int maxResultBytes = parsePositiveInt(args, 2, 2 * 1024 * 1024);
            PrintStream boundedConsole = new PrintStream(
                    new BoundedOutputStream(64 * 1024), true, StandardCharsets.UTF_8.name());
            System.setOut(boundedConsole);
            System.setErr(boundedConsole);
            String script = new String(Base64.getDecoder().decode(readAll(System.in, maxScriptLength * 2)), StandardCharsets.UTF_8);
            Object result = new GroovyScriptEngine().execute(
                    script, new NoBeanContext(), maxScriptLength, allowDangerousScripts);
            byte[] resultBytes = Objects.toString(result).getBytes(StandardCharsets.UTF_8);
            if (resultBytes.length > maxResultBytes) {
                throw new IllegalStateException("Script result exceeds the configured limit");
            }
            protocolOutput.println(SUCCESS_PREFIX + Base64.getEncoder().encodeToString(resultBytes));
        } catch (Throwable throwable) {
            String message = throwable.getClass().getSimpleName() + ": " + Objects.toString(throwable.getMessage(), "unknown error");
            protocolOutput.println(ERROR_PREFIX + Base64.getEncoder().encodeToString(message.getBytes(StandardCharsets.UTF_8)));
            System.exit(1);
        }
    }

    private static int parsePositiveInt(String[] args, int index, int defaultValue) {
        if (args.length <= index) {
            return defaultValue;
        }
        int value = Integer.parseInt(args[index]);
        if (value <= 0) {
            throw new IllegalArgumentException("Worker numeric arguments must be positive");
        }
        return value;
    }

    private static byte[] readAll(InputStream input, int maxBytes) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            if (output.size() + read > maxBytes) {
                throw new IllegalArgumentException("Encoded script exceeds the configured limit");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    public static final class NoBeanContext {
        public Object getBean(String beanName) {
            throw new SecurityException("Spring Beans are not available in isolated Worker mode: " + beanName);
        }
    }

    private static final class BoundedOutputStream extends java.io.OutputStream {
        private final int maxBytes;
        private int written;

        private BoundedOutputStream(int maxBytes) {
            this.maxBytes = maxBytes;
        }

        @Override
        public void write(int value) {
            if (written < maxBytes) {
                written++;
            }
        }

        @Override
        public void write(byte[] bytes, int offset, int length) {
            written = Math.min(maxBytes, written + length);
        }
    }
}
