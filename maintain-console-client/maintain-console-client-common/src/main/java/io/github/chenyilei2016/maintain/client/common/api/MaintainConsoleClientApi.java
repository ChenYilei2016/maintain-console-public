package io.github.chenyilei2016.maintain.client.common.api;

import io.github.chenyilei2016.maintain.client.common.dto.*;

/**
 * @author chenyilei
 * @since 2024/05/17 11:28
 */
public interface MaintainConsoleClientApi {

    /**
     * 执行script
     */
    ApiResult<InvokeScriptResultDTO> $invokeScript(InvokeScriptParamSignDTO invokeScriptParamDTO);

    /**
     * common invoke
     */
    ApiResult<InvokeCommandResultDTO> $invokeCommend(InvokeCommandParamSignDTO invokeCommandParamDTO);
}
