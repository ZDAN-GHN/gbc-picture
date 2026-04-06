package com.zdan.gbcpicturebackend;

import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = {
        ShardingSphereAutoConfiguration.class,
})
@EnableAsync
//@MapperScan("com.zdan.gbcpicturebackend.mapper") // 已经改为在 MybatisPlusConfig 中配置
@EnableAspectJAutoProxy(exposeProxy = true)
public class GbcPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GbcPictureBackendApplication.class, args);
    }

}
