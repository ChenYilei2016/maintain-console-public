package io.github.chenyilei2016.maintain.sample.httpnacos;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;


/**
 * >>>>>> 自行修改nacos的相关配置 ${nacosAddr}
 *
 * @see org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools#reconstructURI(ServiceInstance, URI)
 */
@SpringBootApplication
@EnableDiscoveryClient
public class SampleNacosHttpApp {
    public static void main(String[] args) {
        System.setProperty("spring.cloud.bootstrap.enabled", "false");

        ConfigurableApplicationContext ctx = SpringApplication.run(SampleNacosHttpApp.class);
        System.err.println("start successfully");

        AtomicReference<String> reference = new AtomicReference<>("");
        Executors.newScheduledThreadPool(1)
                .scheduleWithFixedDelay(() -> {

                    DiscoveryClient discoveryClient = ctx.getBean(DiscoveryClient.class);
                    try {
                        String app = ctx.getEnvironment().getProperty("spring.application.name");
                        List<ServiceInstance> instances = discoveryClient.getInstances(app); //自己的目前就一个
                        ServiceInstance serviceInstance = instances.get(0);
                        String data = instances.size() + "-元信息: " + serviceInstance.getMetadata();

                        if (!reference.get().equals(data)) {
                            reference.set(data);
                            System.err.println(data);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, 1200, 4000, TimeUnit.MILLISECONDS);
    }
}