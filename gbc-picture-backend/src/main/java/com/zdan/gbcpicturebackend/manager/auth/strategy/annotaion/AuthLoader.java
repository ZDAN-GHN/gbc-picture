package com.zdan.gbcpicturebackend.manager.auth.strategy.annotaion;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * sa-token 权限获取策略
 */
@Target({ElementType.TYPE}) // 仅作用于类
@Retention(RetentionPolicy.RUNTIME) // 运行时保留，可通过反射获取
@Documented
@Component // 间接让 Spring 扫描为 Bean（无需额外加 @Service/@Component）
public @interface AuthLoader {

    /**
     * 策略标识（作为 Map 的 key）
     * 默认为空时，使用类名首字母小写作为 key
     */
    String loginType() default "";
}