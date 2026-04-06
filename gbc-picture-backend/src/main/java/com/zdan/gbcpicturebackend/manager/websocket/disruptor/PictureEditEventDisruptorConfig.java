package com.zdan.gbcpicturebackend.manager.websocket.disruptor;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.lmax.disruptor.dsl.Disruptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * 配置 disruptor 任务队列
 */
@Configuration
public class PictureEditEventDisruptorConfig {

    @Resource
    private PictureEditEventWorkHandler pictureEditEventHandler;

    @Bean("pictureEditEventDisruptor")
    public Disruptor<PictureEditEvent> getDisruptor() {
        // 定义 ringBuffer 的大小
        final int bufferSize = 1024 * 256;
        // 创建 disruptor
        Disruptor<PictureEditEvent> disruptor = new Disruptor<>(
                PictureEditEvent::new,
                bufferSize,
                ThreadFactoryBuilder.create()
                        .setNamePrefix("pictureEditEventDisruptorThread-")
                        .build()
        );
        // 设置消费者
        disruptor.handleEventsWithWorkerPool(pictureEditEventHandler);
        // 启动disruptor
        disruptor.start();
        return disruptor;
    }
}
