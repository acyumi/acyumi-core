package com.acyumi.spring.cache;

import com.acyumi.util.ParameterUtils;
import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheAnnotationParser;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 拓展过期时间的SpringCache配置类
 *
 * @author Mr.XiHui
 * @date 2019/9/4
 */
//@Configuration
//@ConditionalOnClass({RedissonClient.class, CacheManager.class})
@Import({ExpireKeyProxyCachingConfiguration.class})
public class ExpireKeySpringCacheConfig extends CachingConfigurerSupport implements ResourceLoaderAware {

    /*** slf4j的logger对象. */
    private final Logger logger = LoggerFactory.getLogger(getClass());
    //private final RedissonClient redissonClient;
    /*** 缓存解析器. */
    private final CacheResolver cacheResolver;

    /**
     * Redisson的SpringCache缓存默认使用Hash类型作为{@link org.springframework.cache.Cache}，<br>
     * 而SpringCache要在初始化CacheManager时指定各{@link org.springframework.cache.Cache}的过期时间 <br>
     * 再加上{@link RedissonSpringCacheManager}遵循SpringCache的设定， <br>
     * 提供了配置文件(json/yml)的方式来对需要的{@link org.springframework.cache.Cache}进行过期时间配置 <br>
     * configLocation就是用来指定这个配置文件的 <br>
     *
     * <br>
     * 对应关系就是：
     * {@link org.redisson.spring.cache.RedissonCache} == Redis Hash，
     * Redisson的Spring缓存的key == RedisHash中的field <br>
     * Redis的Hash类型不能给每个field指定过期时间，即Redisson的Spring缓存的key的过期时间只能受限于Cache <br>
     * 值得注意的是，{@link org.redisson.spring.cache.RedissonCache}中持有{@link org.redisson.api.RMapCache} <br>
     * 它通过{@link org.redisson.eviction.EvictionScheduler}进对Hash中的field做定期清理，实现了类似的过期功能 <br>
     * 但是这个过期时间都统一使用初始化时配置{@link CacheConfig} <br>
     * 最后Redisson还是未做到过期时间动态自定义操作，所以才有了{@link ExpireKeyRedissonCacheResolver}等相关代码）
     */
    @Value("${acyumi.spring.cache.redisson.manager.config.location:}")
    private String configLocation;
    private ResourceLoader resourceLoader;

    public ExpireKeySpringCacheConfig(RedissonClient redissonClient) {
        //this.redissonClient = redissonClient;
        this.cacheResolver = new ExpireKeyRedissonCacheResolver(redissonClient, readCacheConfigMap());
        if (logger.isInfoEnabled()) {
            logger.info("==================================================================");
            logger.info("启用拓展过期时间的SpringCache配置类");
            logger.info("启用注解 @ExpireKeyCacheable 和 @ExpireKeyCachePut");
            logger.info("注入BeanType: {}", RedissonClient.class.getName());
            logger.info("初始化BeanType: {}, BeanName: {}", ExpireKeyRedissonCacheResolver.class.getName(),
                    ExpireKeyRedissonCacheResolver.EXPIRE_KEY_REDISSON_CACHE_RESOLVER_BEAN_NAME);
            logger.info("导入配置: {}", ExpireKeyProxyCachingConfiguration.class.getName());
            logger.info("==================================================================");
        }
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 因为这是重写的方法且同时受@Bean影响，这里会被调用两次，所以不在这里进行new操作
     *
     * @return CacheResolver
     */
    @Override
    @Bean(ExpireKeyRedissonCacheResolver.EXPIRE_KEY_REDISSON_CACHE_RESOLVER_BEAN_NAME)
    public CacheResolver cacheResolver() {
        return cacheResolver;
    }

    @Bean
    public CacheAnnotationParser cacheAnnotationParser() {
        return new ExpireKeyCacheAnnotationParser();
    }

    @SuppressWarnings("unchecked")
    private Map<String, CacheConfig> readCacheConfigMap() {
        if (ParameterUtils.isEmpty(configLocation) || resourceLoader == null) {
            //这里不能用Collections.emptyMap()，因为后面要用computeIfAbsent方法
            return new LinkedHashMap<>(0);
        }

        Resource resource = resourceLoader.getResource(configLocation);
        try {
            return (Map<String, CacheConfig>) CacheConfig.fromJSON(resource.getInputStream());
        } catch (IOException e) {
            // try to read yaml
            try {
                return (Map<String, CacheConfig>) CacheConfig.fromYAML(resource.getInputStream());
            } catch (IOException e1) {
                throw new BeanDefinitionStoreException(
                        "Could not parse cache configuration at [" + configLocation + "]", e1);
            }
        }
    }
}
