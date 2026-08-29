package io.github.chenyilei2016.maintain.client.groovy.execute;

import io.github.chenyilei2016.maintain.client.common.dto.RuntimeMetadataDTO;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public class RestrictedBeanContext {
    private final ApplicationContext applicationContext;
    private final Set<String> allowedBeanNames;

    public RestrictedBeanContext(ApplicationContext applicationContext, Set<String> allowedBeanNames) {
        this.applicationContext = applicationContext;
        this.allowedBeanNames = Collections.unmodifiableSet(new HashSet<>(allowedBeanNames));
    }

    public Object getBean(String beanName) {
        if (!allowedBeanNames.contains(beanName)) {
            throw new SecurityException("Bean is not exposed to Maintain Console: " + beanName);
        }
        return applicationContext.getBean(beanName);
    }

    public List<String> beanNames() {
        List<String> names = new ArrayList<>(allowedBeanNames);
        Collections.sort(names);
        return Collections.unmodifiableList(names);
    }

    public RuntimeMetadataDTO metadata(int maxBeans, int maxMethodsPerBean) {
        RuntimeMetadataDTO metadata = new RuntimeMetadataDTO();
        for (String beanName : beanNames().stream().limit(Math.max(0, maxBeans)).collect(Collectors.toList())) {
            Class<?> beanType = applicationContext.getType(beanName);
            if (beanType == null) {
                continue;
            }
            Class<?> userType = ClassUtils.getUserClass(beanType);
            RuntimeMetadataDTO.ExposedBeanDTO bean = new RuntimeMetadataDTO.ExposedBeanDTO();
            bean.setName(beanName);
            bean.setType(userType.getName());
            bean.setMethods(Arrays.stream(userType.getMethods())
                    .filter(method -> Modifier.isPublic(method.getModifiers()))
                    .filter(method -> method.getDeclaringClass() != Object.class)
                    .map(RestrictedBeanContext::methodSignature)
                    .distinct()
                    .sorted()
                    .limit(Math.max(0, maxMethodsPerBean))
                    .collect(Collectors.toList()));
            metadata.getBeans().add(bean);
        }
        return metadata;
    }

    private static String methodSignature(Method method) {
        return method.getName() + '(' + Arrays.stream(method.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", ")) + ") : " + method.getReturnType().getSimpleName();
    }
}
