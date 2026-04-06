package com.zdan.gbcpicturebackend.shared.auth.strategy.register;

import com.zdan.gbcpicturebackend.shared.auth.strategy.annotaion.AuthLoader;
import com.zdan.gbcpicturebackend.shared.auth.strategy.interfaces.AuthLoaderInterface;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;

@Component
public class AuthLoaderAutoRegister implements ApplicationContextAware {

    // 策略存储 map
    private final Map<String, AuthLoaderInterface> authContextWrapStrategyMap = new HashMap<>();

    // Spring 应用上下文
    private ApplicationContext applicationContext;

    /**
     * 容器被 springboot 加载的时候触发 @AuthLoader 策略的采集
     *
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        // 1. 扫描容器中所有带 @Strategy 注解的 Bean（key=Bean名称，value=Bean实例）
        Map<String, Object> annotatedBeans = applicationContext.getBeansWithAnnotation(AuthLoader.class);

        if (CollectionUtils.isEmpty(annotatedBeans)) {
            return;
        }

        // 2. 遍历 Bean，解析注解的 key，存入 strategyMap
        for (Map.Entry<String, Object> entry : annotatedBeans.entrySet()) {
            Object bean = entry.getValue();
            Class<?> beanClass = bean.getClass();
            AuthLoader strategyAnnotation = beanClass.getAnnotation(AuthLoader.class);

            // 3. 确定 Map 的 key：注解指定的 key 优先，否则用 Bean 名称（默认类名首字母小写）
            String strategyKey = strategyAnnotation.loginType();
            if (strategyKey.isEmpty()) {
                strategyKey = entry.getKey(); // Bean 名称（如 userStrategy、vipStrategy）
            }
            // 4. 校验 Bean 是否实现了 AuthLoaderInterface 接口（可选，强制规范）
            if (bean instanceof AuthLoaderInterface) {
                authContextWrapStrategyMap.put(strategyKey, (AuthLoaderInterface) bean);
                System.out.println("策略注册成功：loginType = " + strategyKey + "，bean = " + beanClass.getName());
            } else {
                throw new IllegalArgumentException("类 " + beanClass.getName() + " 标注了 @AuthContextMutateStrategy，但未实现 AuthContextMutateInterface 接口");
            }
        }
    }

    /**
     * 对外提供获取权限加载器的方法
     *
     * @param loginType sa-token 登录类型
     * @return 如果 loader 不存在则返回 null
     */
    public AuthLoaderInterface getAuthLoader(String loginType) {
        return authContextWrapStrategyMap.getOrDefault(loginType, null);
    }
}
