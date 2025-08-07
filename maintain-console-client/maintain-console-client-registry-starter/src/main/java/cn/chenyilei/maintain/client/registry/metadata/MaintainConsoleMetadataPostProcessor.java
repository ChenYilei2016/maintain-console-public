package cn.chenyilei.maintain.client.registry.metadata;

import cn.chenyilei.maintain.client.registry.MergedDelegateRegistration;
import cn.chenyilei.maintain.client.registry.properties.MaintainConsoleRegistryProperties;
import com.alibaba.cloud.nacos.registry.NacosRegistration;
import lombok.SneakyThrows;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @see NacosRegistration
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class MaintainConsoleMetadataPostProcessor implements BeanPostProcessor {

    private final MaintainConsoleRegistryProperties registryProperties;

    public MaintainConsoleMetadataPostProcessor(MaintainConsoleRegistryProperties properties) {
        this.registryProperties = properties;
    }

    @SneakyThrows
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof Registration) {
            Map<String, String> metadata = ((Registration) bean).getMetadata();
            metadata.putAll(registryProperties.getUploadMetadata());
        }
        return bean;
    }


    private Object metadataRegistrationWrap(Registration bean) {
        Enhancer enhancer = new Enhancer();
        // 2.设置父类--可以是类或者接口
        enhancer.setSuperclass(bean.getClass());
        //3. 设置回调函数
        enhancer.setCallback(new MethodInterceptor() {
            final Registration delegate = new MergedDelegateRegistration(bean, registryProperties::getUploadMetadata);

            @Override
            public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                method = ClassUtils.getMostSpecificMethod(method, Registration.class);
                //如果是扩展子类, 则调用本身
                if (Registration.class.isAssignableFrom(method.getDeclaringClass())) {
                    return method.invoke(bean, objects);
                }
                //是Registration的super
                return method.invoke(delegate, objects);
            }
        });
        //寻找一个可用的构造函数
        Constructor<?>[] declaredConstructors = bean.getClass().getDeclaredConstructors();
        Constructor<?> declaredConstructor = declaredConstructors[0];
        return enhancer.create(declaredConstructor.getParameterTypes(), new Object[declaredConstructor.getParameterTypes().length]);
    }
}
