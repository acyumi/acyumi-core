package com.acyumi.spring.cache;

import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.CacheConfig;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.cache.support.SimpleValueWrapper;

import java.lang.reflect.Constructor;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 可动态指定过期时间的Redisson缓存.
 *
 * @author Mr.XiHui
 * @date 2019/9/7
 * @see AbstractValueAdaptingCache
 * @see org.redisson.spring.cache.RedissonCache
 */
public class ExpireKeyRedissonBucketCache extends AbstractValueAdaptingCache implements ExpireKeyCache {

    protected final String cacheName;
    protected final RedissonClient redissonClient;
    protected final CacheConfig config;
    protected final AtomicLong hits = new AtomicLong();
    protected final AtomicLong puts = new AtomicLong();
    protected final AtomicLong misses = new AtomicLong();

    public ExpireKeyRedissonBucketCache(String cacheName, RedissonClient redissonClient,
                                        CacheConfig config, boolean allowNullValues) {
        super(allowNullValues);
        this.cacheName = cacheName;
        this.redissonClient = redissonClient;
        this.config = config;
    }

    @Override
    public String getName() {
        return cacheName;
    }

    @Override
    public RedissonClient getNativeCache() {
        return redissonClient;
    }

    @Override
    public ValueWrapper get(Object key) {
        Object value = lookup(key);
        if (value == null) {
            misses.incrementAndGet();
        } else {
            hits.incrementAndGet();
        }
        return toValueWrapper(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Class<T> type) {
        Object value = lookup(key);
        if (value == null) {
            misses.incrementAndGet();
        } else {
            hits.incrementAndGet();
            if (type != null && !type.isInstance(value)) {
                throw new IllegalStateException("Cached value is not of required type [" + type.getName() + "]: " + value);
            }
        }
        return (T) fromStoreValue(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Callable<T> valueLoader) {

        RBucket<Object> rBucket = getRBucket(key);
        Object value = rBucket.get();
        if (value == null) {
            misses.incrementAndGet();
            RLock lock = redissonClient.getLock(cacheName + unwrapKey(key) + "_lock");
            lock.lock();
            try {
                value = rBucket.get();
                if (value == null) {
                    //注意这里传递的是key而不是unWrappedKey
                    value = putValue(key, valueLoader);
                }
            } finally {
                lock.unlock();
            }
        } else {
            hits.incrementAndGet();
        }
        return (T) fromStoreValue(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void put(Object key, Object value) {

        RBucket<Object> rBucket = getRBucket(key);

        if (!isAllowNullValues() && value == null) {
            rBucket.delete();
            return;
        }

        value = toStoreValue(value);

        if (key instanceof ExpireKeyCacheKeyWrapper) {
            ExpireKeyCacheKeyWrapper expireKeyCacheKeyWrapper = (ExpireKeyCacheKeyWrapper) key;
            ExpireKeyCacheOperation expireKeyCacheOperation = getExpireKeyCacheOperation(expireKeyCacheKeyWrapper);
            rBucket.set(value, expireKeyCacheOperation.getExpire(), expireKeyCacheOperation.getExpireTimeUnit());
        } else {
            rBucket.set(value, config.getTTL(), TimeUnit.MILLISECONDS);
        }

        puts.incrementAndGet();
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {

        RBucket<Object> rBucket = getRBucket(key);
        Object prevValue = rBucket.get();

        if (isAllowNullValues() || value != null) {

            value = toStoreValue(value);

            if (key instanceof ExpireKeyCacheKeyWrapper) {
                ExpireKeyCacheKeyWrapper expireKeyCacheKeyWrapper = (ExpireKeyCacheKeyWrapper) key;
                ExpireKeyCacheOperation expireKeyCacheOperation = getExpireKeyCacheOperation(expireKeyCacheKeyWrapper);
                rBucket.trySet(value, expireKeyCacheOperation.getExpire(), expireKeyCacheOperation.getExpireTimeUnit());
            } else {
                rBucket.trySet(value, config.getTTL(), TimeUnit.MILLISECONDS);
            }

            if (prevValue == null) {
                puts.incrementAndGet();
            }
        }

        return toValueWrapper(prevValue);
    }

    @Override
    public void evict(Object key) {
        getRBucket(key).delete();
    }

    @Override
    public void clear() {
        //只删除cacheName开头的但不包含完全等于cacheName的缓存
        redissonClient.getKeys().deleteByPattern(cacheName + "?*");
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

    @Override
    protected Object lookup(Object key) {
        return getRBucket(key).get();
    }

    protected RBucket<Object> getRBucket(Object key) {
        Object unWrappedKey = unwrapKey(key);
        String redisKey = cacheName + unWrappedKey;
        return redissonClient.getBucket(redisKey);
    }

    protected <T> Object putValue(Object key, Callable<T> valueLoader) {
        Object value;
        try {
            value = valueLoader.call();
        } catch (Exception ex) {
            RuntimeException exception;
            try {
                Class<?> c = Class.forName("org.springframework.cache.Cache$ValueRetrievalException");
                Constructor<?> constructor = c.getConstructor(Object.class, Callable.class, Throwable.class);
                exception = (RuntimeException) constructor.newInstance(key, valueLoader, ex);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            throw exception;
        }
        put(key, value);
        return value;
    }

    @Override
    protected ValueWrapper toValueWrapper(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof org.redisson.spring.cache.NullValue) {
            return org.redisson.spring.cache.NullValue.INSTANCE;
        }
        return new SimpleValueWrapper(value);
    }

    @Override
    protected Object fromStoreValue(Object storeValue) {
        if (storeValue instanceof org.redisson.spring.cache.NullValue) {
            return null;
        }
        return storeValue;
    }

    @Override
    protected Object toStoreValue(Object userValue) {
        if (userValue == null) {
            return org.redisson.spring.cache.NullValue.INSTANCE;
        }
        return userValue;
    }
}
