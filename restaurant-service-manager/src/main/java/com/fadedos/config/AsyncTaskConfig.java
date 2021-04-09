package com.fadedos.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * @Description:TODO
 * @author: pengcheng
 * @date: 2021/2/8
 */
@Configuration
@EnableAsync
public class AsyncTaskConfig implements AsyncConfigurer {
    /**
     * 获取异步线程池
     * 系统启动的时候有线程池 ,取异步线程的时候会自动取线程池
     * @return
     */
    @Bean
    @Override
    public Executor getAsyncExecutor(){
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(10);
        threadPoolTaskExecutor.setMaxPoolSize(100);
        threadPoolTaskExecutor.setQueueCapacity(10);//缓冲队列长度
        threadPoolTaskExecutor.setWaitForTasksToCompleteOnShutdown(true);//线程池关闭 等待所有的线程结束
        threadPoolTaskExecutor.setAwaitTerminationSeconds(60); //线程池关闭等待的结束时间
        threadPoolTaskExecutor.setThreadNamePrefix("Rabbit-Async-");//设置所有线程的名称
        return threadPoolTaskExecutor;
    }




    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return null;
    }
}
