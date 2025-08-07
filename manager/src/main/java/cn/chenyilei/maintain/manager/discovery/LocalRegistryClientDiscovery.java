package cn.chenyilei.maintain.manager.discovery;

import cn.chenyilei.maintain.manager.context.ApplicationContextHolder;
import com.google.common.collect.Lists;
import io.github.chenyilei2016.extension.spi.context.Lifecycle;
import io.github.chenyilei2016.extension.spi.kernel.URL;
import lombok.SneakyThrows;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

import java.util.List;

import static cn.chenyilei.maintain.manager.CONST.APP_NAME;

/**
 * @author chenyilei
 * @since 2024/05/20 16:42
 */
public class LocalRegistryClientDiscovery implements MaintainConsoleRegistryClientDiscovery, Lifecycle {


    @Override
    public ServiceInstance findServiceInstance(URL url, String serviceName, String env) {
        DefaultServiceInstance local = new DefaultServiceInstance();
        local.setInstanceId(APP_NAME);
        local.setServiceId(APP_NAME);
        local.setHost("127.0.0.1");
        local.setPort(Integer.valueOf(ApplicationContextHolder.getEnvironment().getProperty("server.port")));
        return local;
    }

    @Override
    @SneakyThrows
    public List<String> listServiceNames(URL url) {
        return Lists.newArrayList(APP_NAME, "MOCK SERVICE");
    }


    @Override
    public void initialize() throws IllegalStateException {

    }

    @Override
    public void start() throws IllegalStateException {

    }

    @Override
    public void destroy() throws IllegalStateException {

    }
}
