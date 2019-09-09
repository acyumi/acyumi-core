package com.acyumi.spring.cache;

import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.CacheConfig;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.AbstractCacheResolver;
import org.springframework.cache.interceptor.BasicOperation;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 可动态指定过期时间的Redisson缓存解析器.
 * <br>
 * 既然Redis的Hash类型不能给每个field指定过期时间，那么我们想动态指定的过期时间就不用Hash了 <br>
 * 或者我们通过{@link org.redisson.api.RMapCache}来动态指定field的过期时间 <br>
 * <br>
 * 查阅Redisson官方wiki可知
 * 目前的Redis自身并不支持散列（Hash）当中的元素淘汰，因此所有过期元素都是通过 <br>
 * {@link org.redisson.eviction.EvictionScheduler}实例来实现定期清理的。 <br>
 * 为了保证资源的有效利用，每次运行最多清理300个过期元素。 <br>
 * 任务的启动时间将根据上次实际清理数量自动调整，间隔时间趋于1秒到1小时之间。 <br>
 * 比如该次清理时删除了300条元素，那么下次执行清理的时间将在1秒以后（最小间隔时间）。 <br>
 * 一旦该次清理数量少于上次清理数量，时间间隔将增加1.5倍。 <br>
 * <br>
 * 通过相关测试，由于不是redis原生支持，所以通常会有秒级别的延迟，要求不高可以使用{@link org.redisson.api.RMapCache}。
 *
 * @author Mr.XiHui
 * @date 2019/9/5
 * @see org.redisson.spring.cache.RedissonSpringCacheManager
 */
public class ExpireKeyRedissonCacheResolver extends AbstractCacheResolver {

    public static final String EXPIRE_KEY_REDISSON_CACHE_RESOLVER_BEAN_NAME = "acyumiExpireKeyRedissonCacheResolver";

    protected final ConcurrentMap<String, ExpireKeyCache> expireKeyCacheInstantMap = new ConcurrentHashMap<>();
    protected final RedissonClient redissonClient;
    protected final Map<String, CacheConfig> cacheConfigMap;
    private boolean allowNullValues = true;

    public ExpireKeyRedissonCacheResolver(CacheManager cacheManager, RedissonClient redissonClient,
                                          Map<String, CacheConfig> cacheConfigMap) {
        super(cacheManager);
        this.redissonClient = redissonClient;
        this.cacheConfigMap = cacheConfigMap;
    }

    @Override
    public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {

        BasicOperation operation = context.getOperation();
        if (operation instanceof ExpireKeyCacheOperation) {
            Collection<String> cacheNames = getCacheNames(context);
            if (cacheNames == null) {
                return Collections.emptyList();
            }
            return getExpireKeyCaches(context, cacheNames);
        }

        return super.resolveCaches(context);
    }

    /**
     * 获取拓展过期时间的{@link ExpireKeyCache}缓存对象列表
     *
     * @param context    CacheOperationInvocationContext
     * @param cacheNames 缓存名列表
     * @return {@link ExpireKeyCache}缓存对象列表
     */
    protected Collection<? extends ExpireKeyCache> getExpireKeyCaches(CacheOperationInvocationContext<?> context,
                                                                      Collection<String> cacheNames) {

        Collection<ExpireKeyCache> expireKeyCaches = new ArrayList<>(cacheNames.size());

        for (String cacheName : cacheNames) {
            ExpireKeyCache expireKeyCache = expireKeyCacheInstantMap.computeIfAbsent(cacheName,
                    cn -> new ExpireKeyRedissonCache(
                            redissonClient.getMapCache(cacheName),
                            cacheConfigMap.computeIfAbsent(cacheName, k -> new CacheConfig()),
                            allowNullValues
                    )
            );
            expireKeyCaches.add(expireKeyCache);
        }

        return expireKeyCaches;
    }

    @Override
    protected Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
        return context.getOperation().getCacheNames();
    }

    /**
     * Defines possibility of storing {@code null} values.
     * <p>
     * Default is <code>true</code>
     *
     * @param allowNullValues - stores if <code>true</code>
     */
    public void setAllowNullValues(boolean allowNullValues) {
        this.allowNullValues = allowNullValues;
    }
}
