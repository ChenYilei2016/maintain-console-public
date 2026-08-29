package io.github.chenyilei2016.maintain.manager.caller;

import io.github.chenyilei2016.maintain.client.common.api.MaintainConsoleClientApi;
import io.github.chenyilei2016.maintain.client.common.dto.*;

/**
 * @author chenyilei
 * @see MaintainConsoleClientApi
 * @since 2024/05/20 17:22
 */
public interface ClientCaller {

    ApiResult<InvokeScriptResultDTO> $invokeScript(ClientCallerContext ctx, InvokeScriptParamSignDTO invokeScriptParamDTO);

    ApiResult<InvokeCommandResultDTO> $invokeCommend(ClientCallerContext ctx, InvokeCommandParamSignDTO invokeCommandParamDTO);

    ApiResult<RuntimeMetadataDTO> $runtimeMetadata(ClientCallerContext ctx, RuntimeMetadataParamSignDTO request);
}
