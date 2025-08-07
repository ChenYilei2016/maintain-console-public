package cn.chenyilei.maintain.client.common.api;

import cn.chenyilei.maintain.client.common.dto.*;

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
