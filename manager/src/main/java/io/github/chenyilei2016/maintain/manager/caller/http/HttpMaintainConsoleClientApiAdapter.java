package io.github.chenyilei2016.maintain.manager.caller.http;

import io.github.chenyilei2016.maintain.client.common.api.MaintainConsoleClientApi;
import io.github.chenyilei2016.maintain.client.common.constants.MaintainConsoleClientHttpConst;
import io.github.chenyilei2016.maintain.client.common.dto.*;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * http-调用console client的适配器
 *
 * @author chenyilei
 * @see MaintainConsoleClientApi
 * @since 2024/05/21 10:37
 */
public interface HttpMaintainConsoleClientApiAdapter {
    @POST(MaintainConsoleClientHttpConst.URI_INVOKE_SCRIPT)
    @Headers({
            MaintainConsoleClientHttpConst.HEADER_MAINTAIN_CONSOLE_URI + ":" + MaintainConsoleClientHttpConst.URI_INVOKE_SCRIPT,
    })
    Call<ApiResult<InvokeScriptResultDTO>> $invokeScript(@Header(OkHttpUrlSelectionInterceptor.HEADER_BASE_URL) String url, @Body InvokeScriptParamSignDTO invokeScriptParamDTO);

    @POST(MaintainConsoleClientHttpConst.URI_INVOKE_COMMEND)
    @Headers({
            MaintainConsoleClientHttpConst.HEADER_MAINTAIN_CONSOLE_URI + ":" + MaintainConsoleClientHttpConst.URI_INVOKE_COMMEND,
    })
    Call<ApiResult<InvokeCommandResultDTO>> $invokeCommend(@Header(OkHttpUrlSelectionInterceptor.HEADER_BASE_URL) String url, @Body InvokeCommandParamSignDTO invokeCommandParamDTO);
}
