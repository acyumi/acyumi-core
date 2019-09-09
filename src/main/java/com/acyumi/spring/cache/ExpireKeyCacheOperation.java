package com.acyumi.spring.cache;

import org.springframework.cache.interceptor.BasicOperation;

import java.util.concurrent.TimeUnit;

/**
 * 可在注解上指定key的过期时间的属性描述对象.
 *
 * @author Mr.XiHui
 * @date 2019/9/7
 */
public interface ExpireKeyCacheOperation extends BasicOperation {

    /**
     * 过期时间值，其单位通过{@link #getExpireTimeUnit()}指定
     *
     * @return long
     */
    long getExpire();

    /**
     * 时间单位
     *
     * @return TimeUnit
     */
    TimeUnit getExpireTimeUnit();
}
