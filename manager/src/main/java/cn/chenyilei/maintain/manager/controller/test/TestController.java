package cn.chenyilei.maintain.manager.controller.test;

import cn.chenyilei.maintain.client.common.dto.ApiResult;
import cn.chenyilei.maintain.client.common.dto.InvokeScriptParamSignDTO;
import cn.chenyilei.maintain.client.common.dto.InvokeScriptResultDTO;
import cn.chenyilei.maintain.manager.caller.ClientCaller;
import cn.chenyilei.maintain.manager.caller.ClientCallerContext;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import io.github.chenyilei2016.extension.spi.ExtensionLoader;
import org.junit.jupiter.api.Test;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 用于测试流程
 *
 * @author chenyilei
 * @see cn.chenyilei.maintain.client.common.api.MaintainConsoleClientApi
 * @since 2024/05/17 14:44
 */
@RestController
@RequestMapping("/test")
public class TestController {
    ClientCaller clientCaller;

    public TestController() {
        clientCaller = ExtensionLoader.getExtensionLoader(ClientCaller.class).getAdaptiveExtension();
    }

    /**
     * https://blog.51cto.com/u_15127603/4544205 生成feign client
     * No Feign Client for loadBalancing defined. Did you forget to include spring-cloud-starter-netflix-ribbon or spring-cloud-starter-loadbalancer?
     */
    @GetMapping("/maintain-sample-http-feign/sendScript")
    public Object sendScript(HttpServletRequest request) {
        String serviceName = "maintain-sample-http-feign";
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("getAdaptiveExtension");
        ClientCallerContext ctx = new ClientCallerContext(serviceName);
        InvokeScriptParamSignDTO invokeScriptParamSignDTO = new InvokeScriptParamSignDTO();
        invokeScriptParamSignDTO.setScript("1 + 1");
        stopWatch.stop();
        stopWatch.start("api invoke");
        ApiResult<InvokeScriptResultDTO> invokeScriptResultDTOApiResult = clientCaller.$invokeScript(ctx, invokeScriptParamSignDTO);
        stopWatch.stop();
        System.err.println("/maintain-sample-nacos-feign/sendScript , result : " + invokeScriptResultDTOApiResult);
        System.err.println("spend : " + stopWatch.prettyPrint());
        System.err.println("spend : " + stopWatch.getTotalTimeMillis());
        return invokeScriptResultDTOApiResult;
    }

    @Test
    public void tett() {
        String s = "{\"success\":true,\"data\":null,\"msg\":null}";
        ApiResult<InvokeScriptResultDTO> invokeScriptResultDTOApiResult = JSON.parseObject(s, new TypeReference<ApiResult<InvokeScriptResultDTO>>() {
        }.getType());
        System.err.println(invokeScriptResultDTOApiResult);
    }
}
