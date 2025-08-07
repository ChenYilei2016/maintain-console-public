package cn.chenyilei.maintain.manager.caller;

import cn.chenyilei.maintain.client.common.constants.MaintainConsoleClientHttpConst;
import cn.chenyilei.maintain.client.common.dto.ApiResult;
import cn.chenyilei.maintain.client.common.dto.InvokeScriptParamSignDTO;
import cn.chenyilei.maintain.client.common.dto.InvokeScriptResultDTO;
import cn.chenyilei.maintain.manager.caller.http.RetrofitHttpProxyFactory;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.http.*;

import java.io.IOException;

public class ClientCallerTest {

    static interface SampleClientApi {
        @GET("/")
//        @Headers({"Content-Type: text/plain"})
        @Headers("Accept: text/plain")
        Call<String> testBaidu(@Header(HostSelectionInterceptor.HEADER_BASE_URL) String h);

        @POST(MaintainConsoleClientHttpConst.URI_INVOKE_SCRIPT)
        @Headers({
                MaintainConsoleClientHttpConst.HEADER_MAINTAIN_CONSOLE_URI + ":" + MaintainConsoleClientHttpConst.URI_INVOKE_SCRIPT,
        })
        Call<ApiResult<InvokeScriptResultDTO>> $invokeScript(@Header(HostSelectionInterceptor.HEADER_BASE_URL) String h, @Body InvokeScriptParamSignDTO invokeScriptParamDTO);

    }


    @Test
    public void testProxyTextPlain() throws IOException {
        SampleClientApi sampleClientApi = RetrofitHttpProxyFactory.getProxy(SampleClientApi.class);
        System.err.println(sampleClientApi.testBaidu("http://baidu.com").execute().body());
    }

    @Test
    public void testProxyPostJson() throws IOException {
        SampleClientApi sampleClientApi = RetrofitHttpProxyFactory.getProxy(SampleClientApi.class);
        Call<ApiResult<InvokeScriptResultDTO>> apiResultCall = sampleClientApi.$invokeScript("http://baidu.com/", new InvokeScriptParamSignDTO());
        try {
            System.err.println(apiResultCall.request());
            System.err.println(apiResultCall.execute().errorBody());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}