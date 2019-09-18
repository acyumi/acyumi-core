package com.acyumi.spring.cache;

import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.CacheConfig;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.AbstractCacheResolver;
import org.springframework.cache.interceptor.BasicOperation;
import org.springframework.cache.interceptor.CacheEvictOperation;
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
 * <br>
 * <br>
 * 如果使用原生的SpringCache注解来添加缓存，则依Redisson默认使用Hash结构来缓存数据 <br>
 * 否则将根据以下属性来决定redis缓存结构 <br>
 * {@link com.acyumi.annotation.ExpireKeyCacheable#usingHash()}
 *  或 {@link com.acyumi.annotation.ExpireKeyCachePut#usingHash()}
 *
 *
 * @author Mr.XiHui
 * @date 2019/9/5
 * @see org.redisson.spring.cache.RedissonSpringCacheManager
 */
public class ExpireKeyRedissonCacheResolver extends AbstractCacheResolver {

    public static final String EXPIRE_KEY_REDISSON_CACHE_RESOLVER_BEAN_NAME = "acyumiExpireKeyRedissonCacheResolver";

    /**
     * ExpireKeyCache数组的Map代码缓存
     * key: cacheName
     * value: ExpireKeyCache[]，第一索引位为ExpireKeyRedissonBucketCache，第二索引位为ExpireKeyRedissonMapCache
     */
    protected final ConcurrentMap<String, ExpireKeyCache[]> expireKeyCacheInstantsMap = new ConcurrentHashMap<>(256);
    protected final RedissonClient redissonClient;
    protected final Map<String, CacheConfig> cacheConfigMap;
    private boolean allowNullValues = true;
    private long defaultExpire;
    private long defaultMaxIdleTime;

    public ExpireKeyRedissonCacheResolver(RedissonClient redissonClient,
                                          Map<String, CacheConfig> cacheConfigMap) {
        super(null);
        this.redissonClient = redissonClient;
        this.cacheConfigMap = cacheConfigMap;
    }

    @Override
    public void afterPropertiesSet() {
        //父类中会校验CacheManager，我们现在不需要CacheManager，所以这里啥都不做也不调用父类的方法
    }

    @Override
    public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {

        Collection<String> cacheNames = getCacheNames(context);
        if (cacheNames == null) {
            return Collections.emptyList();
        }

        BasicOperation operation = context.getOperation();
        if (operation instanceof ExpireKeyCacheOperation) {
            return getExpireKeyCaches(context, (ExpireKeyCacheOperation) operation, cacheNames);
        }

        return getCachesForNativeAnnotation(context, cacheNames);
    }

    /**
     * 获取拓展过期时间的{@link ExpireKeyCache}缓存对象列表
     *
     * @param context                 CacheOperationInvocationContext
     * @param expireKeyCacheOperation ExpireKeyCacheOperation
     * @param cacheNames              缓存名列表
     * @return {@link ExpireKeyCache}缓存对象列表
     */
    protected Collection<? extends ExpireKeyCache> getExpireKeyCaches(CacheOperationInvocationContext<?> context,
                                                                      ExpireKeyCacheOperation expireKeyCacheOperation,
                                                                      Collection<String> cacheNames) {

        boolean usingHash = expireKeyCacheOperation.isUsingHash();
        Collection<ExpireKeyCache> expireKeyCaches = new ArrayList<>(cacheNames.size());

        for (String cacheName : cacheNames) {
            ExpireKeyCache[] ekcs = expireKeyCacheInstantsMap.computeIfAbsent(cacheName, cn -> new ExpireKeyCache[2]);
            ExpireKeyCache ekc;
            if (usingHash) {
                ekc = ekcs[1];
                if (ekc == null) {
                    ekc = new ExpireKeyRedissonMapCache(
                            redissonClient.getMapCache(cacheName),
                            cacheConfigMap.computeIfAbsent(cacheName,
                                    k -> new CacheConfig(defaultExpire, defaultMaxIdleTime)),
                            allowNullValues
                    );
                    ekcs[1] = ekc;
                }
                expireKeyCaches.add(ekc);
            } else {
                ekc = ekcs[0];
                if (ekc == null) {
                    ekc = new ExpireKeyRedissonBucketCache(
                            cacheName, redissonClient,
                            cacheConfigMap.computeIfAbsent(cacheName,
                                    k -> new CacheConfig(defaultExpire, defaultMaxIdleTime)),
                            allowNullValues
                    );
                    ekcs[0] = ekc;
                }
                expireKeyCaches.add(ekc);
            }
        }

        return expireKeyCaches;
    }

    /**
     * 获取拓展过期时间的{@link ExpireKeyCache}缓存对象列表
     *
     * @param context    CacheOperationInvocationContext
     * @param cacheNames 缓存名列表
     * @return {@link ExpireKeyCache}缓存对象列表
     */
    protected Collection<Cache> getCachesForNativeAnnotation(CacheOperationInvocationContext<?> context,
                                                             Collection<String> cacheNames) {

        BasicOperation operation = context.getOperation();

        //如果是CacheEvict，那么一个cacheName可能对应两个Cache（即ExpireKeyRedissonBucketCache和ExpireKeyRedissonMapCache）
        //如果是Cacheable或CachePut，那么一个cacheName只对应一个Cache（即ExpireKeyRedissonMapCache）
        //意思是如果使用原生的SpringCache注解来添加缓存，则使用Hash结构来缓存数据
        boolean isCacheEvict = operation instanceof CacheEvictOperation;

        Collection<Cache> result = new ArrayList<>(isCacheEvict ? cacheNames.size() << 1 : cacheNames.size());

        for (String cacheName : cacheNames) {
            ExpireKeyCache[] ekcs = expireKeyCacheInstantsMap.computeIfAbsent(cacheName, cn -> new ExpireKeyCache[2]);
            ExpireKeyCache ekc = ekcs[1];
            if (ekc == null) {
                ekc = new ExpireKeyRedissonMapCache(
                        redissonClient.getMapCache(cacheName),
                        cacheConfigMap.computeIfAbsent(cacheName,
                                k -> new CacheConfig(defaultExpire, defaultMaxIdleTime)),
                        allowNullValues
                );
                ekcs[1] = ekc;
            }
            result.add(ekc);

            if (isCacheEvict && (ekc = ekcs[0]) != null) {
                result.add(ekc);
            }
        }

        return result;
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

    public void setDefaultExpire(long defaultExpire) {
        this.defaultExpire = defaultExpire;
    }

    public void setDefaultMaxIdleTime(long defaultMaxIdleTime) {
        this.defaultMaxIdleTime = defaultMaxIdleTime;
    }
}
