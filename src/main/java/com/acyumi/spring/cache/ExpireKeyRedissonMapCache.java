package com.acyumi.spring.cache;

import org.redisson.api.RMapCache;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonCache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 可动态指定过期时间的Redisson缓存.
 *
 * @author Mr.XiHui
 * @date 2019/9/7
 * @see org.springframework.cache.Cache
 * @see RedissonCache
 */
public class ExpireKeyRedissonMapCache extends RedissonCache implements ExpireKeyCache {

    protected final RMapCache<Object, Object> mapCache;
    protected final CacheConfig config;
    protected final boolean allowNullValues;
    protected final AtomicLong hits;
    protected final AtomicLong puts;
    protected final AtomicLong misses;

    public ExpireKeyRedissonMapCache(RMapCache<Object, Object> mapCache, CacheConfig config, boolean allowNullValues) {
        super(mapCache, config, allowNullValues);
        this.mapCache = mapCache;
        this.config = config;
        this.allowNullValues = allowNullValues;

        Class<?> redissonCacheClass = this.getClass().getSuperclass();
        Field hitsField = ReflectionUtils.findField(redissonCacheClass, "hits");
        Field putsField = ReflectionUtils.findField(redissonCacheClass, "puts");
        Field missesField = ReflectionUtils.findField(redissonCacheClass, "misses");
        //这三个Field取不到的话有可能是版本问题，要更新维护这里的代码才行
        if (hitsField != null) {
            ReflectionUtils.makeAccessible(hitsField);
            this.hits = (AtomicLong) ReflectionUtils.getField(hitsField, this);
        } else {
            this.hits = new AtomicLong();
        }
        if (putsField != null) {
            ReflectionUtils.makeAccessible(putsField);
            this.puts = (AtomicLong) ReflectionUtils.getField(putsField, this);
        } else {
            this.puts = new AtomicLong();
        }
        if (missesField != null) {
            ReflectionUtils.makeAccessible(missesField);
            this.misses = (AtomicLong) ReflectionUtils.getField(missesField, this);
        } else {
            this.misses = new AtomicLong();
        }
    }

    @Override
    public ValueWrapper get(Object key) {
        key = unwrapKey(key);
        return super.get(key);
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        key = unwrapKey(key);
        return super.get(key, type);
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        key = unwrapKey(key);
        return super.get(key, valueLoader);
    }

    @Override
    public void put(Object key, Object value) {
        Object unWrappedKey = unwrapKey(key);
        if (key instanceof ExpireKeyCacheKeyWrapper) {
            ExpireKeyCacheKeyWrapper expireKeyCacheKeyWrapper = (ExpireKeyCacheKeyWrapper) key;
            ExpireKeyCacheOperation expireKeyCacheOperation = getExpireKeyCacheOperation(expireKeyCacheKeyWrapper);

            if (!allowNullValues && value == null) {
                mapCache.fastRemove(key);
                return;
            }

            value = toStoreValue(value);
            mapCache.fastPut(unWrappedKey, value, expireKeyCacheOperation.getExpire(),
                    expireKeyCacheOperation.getExpireTimeUnit(),
                    config.getMaxIdleTime(), TimeUnit.MILLISECONDS);

            puts.incrementAndGet();
            return;
        }
        super.put(unWrappedKey, value);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        Object unWrappedKey = unwrapKey(key);
        if (key instanceof ExpireKeyCacheKeyWrapper) {
            ExpireKeyCacheKeyWrapper expireKeyCacheKeyWrapper = (ExpireKeyCacheKeyWrapper) key;
            ExpireKeyCacheOperation expireKeyCacheOperation = getExpireKeyCacheOperation(expireKeyCacheKeyWrapper);

            Object prevValue;
            if (!allowNullValues && value == null) {
                prevValue = mapCache.get(unWrappedKey);
            } else {
                value = toStoreValue(value);
                prevValue = mapCache.putIfAbsent(unWrappedKey, value, expireKeyCacheOperation.getExpire(),
                        expireKeyCacheOperation.getExpireTimeUnit(),
                        config.getMaxIdleTime(), TimeUnit.MILLISECONDS);
                if (prevValue == null) {
                    puts.incrementAndGet();
                }
            }

            return toValueWrapper(prevValue);
        }
        return super.putIfAbsent(unWrappedKey, value);
    }

    @Override
    public void evict(Object key) {
        key = unwrapKey(key);
        super.evict(key);
    }

    @Override
    public Object unwrapKey(Object key) {
        if (key instanceof ExpireKeyCacheKeyWrapper) {
            return ((ExpireKeyCacheKeyWrapper) key).getKey();
        }
        return key;
    }

    @Override
    public ExpireKeyCacheOperation getExpireKeyCacheOperation(ExpireKeyCacheKeyWrapper ekckWrapper) {
        return ekckWrapper.getExpireKeyCacheOperation();
    }

    /**
     * The number of get requests that were satisfied by the cache.
     *
     * @return the number of hits
     */
    public long getCacheHits() {
        return hits.get();
    }

    /**
     * A miss is a get request that is not satisfied.
     *
     * @return the number of misses
     */
    public long getCacheMisses() {
        return misses.get();
    }

    public long getCachePuts() {
        return puts.get();
    }

    protected ValueWrapper toValueWrapper(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof org.redisson.spring.cache.NullValue) {
            return org.redisson.spring.cache.NullValue.INSTANCE;
        }
        return new SimpleValueWrapper(value);
    }

}
