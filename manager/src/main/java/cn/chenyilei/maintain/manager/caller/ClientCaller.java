package cn.chenyilei.maintain.manager.caller;

import cn.chenyilei.maintain.client.common.api.MaintainConsoleClientApi;
import cn.chenyilei.maintain.client.common.dto.*;
import cn.chenyilei.maintain.manager.constant.SPIConstants;
import io.github.chenyilei2016.extension.spi.ExtensionAdaptive;
import io.github.chenyilei2016.extension.spi.ExtensionSPI;

/**
 * @author chenyilei
 * @see MaintainConsoleClientApi
 * @since 2024/05/20 17:22
 */
@ExtensionSPI("http")
public interface ClientCaller {

    @ExtensionAdaptive(value = {SPIConstants.CLIENT_CALL_RPC_TYPE})
    ApiResult<InvokeScriptResultDTO> $invokeScript(ClientCallerContext ctx, InvokeScriptParamSignDTO invokeScriptParamDTO);

    @ExtensionAdaptive(value = {SPIConstants.CLIENT_CALL_RPC_TYPE})
    ApiResult<InvokeCommandResultDTO> $invokeCommend(ClientCallerContext ctx, InvokeCommandParamSignDTO invokeCommandParamDTO);
}
