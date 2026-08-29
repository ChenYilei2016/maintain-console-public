package io.github.chenyilei2016.maintain.client.http.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chenyilei2016.maintain.client.common.console.IMaintainConsoleExecutor;
import io.github.chenyilei2016.maintain.client.common.constants.MaintainConsoleClientHttpConst;
import io.github.chenyilei2016.maintain.client.common.dto.*;
import io.github.chenyilei2016.maintain.client.http.security.RequestSignatureVerifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * filter 作为服务提供方
 *
 * @author chenyilei
 * @since 2024/05/20 09:51
 */
public class HttpMaintainConsoleClientApiImplFilter extends OncePerRequestFilter implements Filter {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Charset DEFAULT_CHARSETS = StandardCharsets.UTF_8;
    private final IMaintainConsoleExecutor maintainConsoleExecutor;
    private final RequestSignatureVerifier signatureVerifier;

    public HttpMaintainConsoleClientApiImplFilter(
            IMaintainConsoleExecutor maintainConsoleExecutor,
            RequestSignatureVerifier signatureVerifier
    ) {
        this.maintainConsoleExecutor = maintainConsoleExecutor;
        this.signatureVerifier = signatureVerifier;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (isMaintainConsoleRpcInvoke(request)) {
            doMaintainConsoleRpcInvoke(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isMaintainConsoleRpcInvoke(HttpServletRequest request) {
        String maintainConsoleUrl = request.getHeader(MaintainConsoleClientHttpConst.HEADER_MAINTAIN_CONSOLE_URI);
        if (StringUtils.isEmpty(maintainConsoleUrl)) {
            return false;
        }

        //只接受POST请求
        if (!HttpMethod.POST.name().equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        //指定前缀开头
        String requestURI = request.getRequestURI();
        //指定url结尾
        if (requestURI.endsWith(maintainConsoleUrl)) {
            return true;
        }

        return false;
    }

    private void doMaintainConsoleRpcInvoke(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String consoleURI = request.getHeader(MaintainConsoleClientHttpConst.HEADER_MAINTAIN_CONSOLE_URI);
        try {
            switch (consoleURI) {
                case MaintainConsoleClientHttpConst.URI_INVOKE_SCRIPT:
                    write(ApiResult.success(doInvokeScript(request)), response);
                    break;
                case MaintainConsoleClientHttpConst.URI_INVOKE_COMMEND:
                    write(ApiResult.error("暂不支持的执行指令"), response);
                    break;
                case MaintainConsoleClientHttpConst.URI_RUNTIME_METADATA:
                    write(ApiResult.success(doRuntimeMetadata(request)), response);
                    break;
                default:
                    write(ApiResult.error("不存在的执行指令"), response);
                    break;
            }
        } catch (Throwable throwable) {
            if (response.isCommitted()) {
                return;
            }
            write(ApiResult.error(throwable.getMessage()), response);
        }
    }

    private void write(Object result, HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(DEFAULT_CHARSETS.toString());
        String json = result instanceof String ? (String) result : objectMapper.writeValueAsString(result);
        try (PrintWriter writer = response.getWriter()) {
            writer.write(json);
        }
    }

    private InvokeScriptResultDTO doInvokeScript(HttpServletRequest request) throws IOException {
        String requestBody = StreamUtils.copyToString(request.getInputStream(), DEFAULT_CHARSETS);
        InvokeScriptParamSignDTO invokeScriptParamDTO = objectMapper.readValue(requestBody, InvokeScriptParamSignDTO.class);
        signatureVerifier.verify(invokeScriptParamDTO);
        final String script = invokeScriptParamDTO.getScript();
        Object result = maintainConsoleExecutor.execute(script);
        InvokeScriptResultDTO resultDTO = new InvokeScriptResultDTO();
        resultDTO.setScriptResult(Objects.toString(result));
        return resultDTO;
    }

    private RuntimeMetadataDTO doRuntimeMetadata(HttpServletRequest request) throws IOException {
        String requestBody = StreamUtils.copyToString(request.getInputStream(), DEFAULT_CHARSETS);
        RuntimeMetadataParamSignDTO metadataRequest = objectMapper.readValue(
                requestBody, RuntimeMetadataParamSignDTO.class);
        signatureVerifier.verify(metadataRequest);
        return maintainConsoleExecutor.runtimeMetadata();
    }
}
