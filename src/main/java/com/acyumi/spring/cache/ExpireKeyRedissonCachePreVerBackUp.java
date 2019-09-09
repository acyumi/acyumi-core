//package com.acyumi.spring.cache;
//
//import org.redisson.api.*;
//import org.redisson.spring.cache.CacheConfig;
//import org.springframework.cache.Cache;
//import org.springframework.cache.support.SimpleValueWrapper;
//
//import java.lang.reflect.Constructor;
//import java.util.*;
//import java.util.concurrent.Callable;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ConcurrentMap;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicLong;
//import java.util.function.Supplier;
//
///**
// * 可动态指定过期时间的Redisson缓存.
// *
// * @author Mr.XiHui
// * @date 2019/9/7
// * @see Cache
// * @see org.redisson.spring.cache.RedissonCache
// */
//public class ExpireKeyRedissonCachePreVerBackUp implements Cache {
//
//    //AcyumiRedissonCache不允许缓存null值
//    //private final boolean allowNullValue;
//
//    private final String cacheName;
//
//    private final RedissonClient redissonClient;
//
//    private final ConcurrentMap<Object, CacheConfig> keyConfigMap = new ConcurrentHashMap<>(256);
//
//    private final ConcurrentMap<Object, RObject> cacheMap = new ConcurrentHashMap<>(256);
//
//    private final AtomicLong hits = new AtomicLong();
//
//    private final AtomicLong puts = new AtomicLong();
//
//    private final AtomicLong misses = new AtomicLong();
//
//    public ExpireKeyRedissonCachePreVerBackUp(String cacheName, RedissonClient redissonClient) {
//        this.cacheName = cacheName;
//        this.redissonClient = redissonClient;
//    }
//
//    @Override
//    public String getName() {
//        return cacheName;
//    }
//
//    @Override
//    public ConcurrentMap<Object, RObject> getNativeCache() {
//        return cacheMap;
//    }
//
//    @Override
//    public ValueWrapper get(Object key) {
//        Object value = getValue(key);
//        if (value == null) {
//            addCacheMiss();
//        } else {
//            addCacheHit();
//        }
//        return toValueWrapper(value);
//    }
//
//    @Override
//    @SuppressWarnings("unchecked")
//    public <T> T get(Object key, Class<T> type) {
//        Object value = getValue(key);
//        if (value == null) {
//            addCacheMiss();
//        } else {
//            addCacheHit();
//            if (type != null && !type.isInstance(value)) {
//                throw new IllegalStateException("Cached value is not of required type [" + type.getName() + "]: " + value);
//            }
//        }
//        return (T) value;
//    }
//
//    @Override
//    @SuppressWarnings("unchecked")
//    public <T> T get(Object key, Callable<T> valueLoader) {
//        RObject rObject = cacheMap.get(key);
//        Object value = getValue(rObject);
//        if (value == null) {
//            addCacheMiss();
//            RLock lock = redissonClient.getLock(cacheName + key + "_lock");
//            lock.lock();
//            try {
//                value = getValue(rObject);
//                if (value == null) {
//                    value = putValue(key, valueLoader);
//                }
//            } finally {
//                lock.unlock();
//            }
//        } else {
//            addCacheHit();
//        }
//        return (T) value;
//    }
//
//    @Override
//    @SuppressWarnings("unchecked")
//    public void put(Object key, Object value) {
//
//        String redisKey = cacheName + key;
//        CacheConfig cacheConfig = keyConfigMap.get(key);
//
//        RObject rObject = cacheMap.get(key);
//        if (rObject == null) {
//            if (value == null) {
//                //如果第一次保存缓存时值就是null，则不处理
//                return;
//            }
//            rObject = getrObject(redisKey, value);
//            cacheMap.put(key, rObject);
//        } else {
//            if (value == null) {
//                rObject.delete();
//                return;
//            }
//            if ((rObject instanceof RMap && !(value instanceof Map))
//                    || (rObject instanceof RList && !(value instanceof List))
//                    || (rObject instanceof RSortedSet && !(value instanceof SortedSet))
//                    || (rObject instanceof RSet && !(value instanceof Set))) {
//                rObject.delete();
//                rObject = getrObject(redisKey, value);
//                cacheMap.put(key, rObject);
//            }
//        }
//
//        if (rObject instanceof RBucket) {
//            ((RBucket) rObject).set(value, cacheConfig.getTTL(), TimeUnit.MILLISECONDS);
//        } else if (rObject instanceof RMap) {
//            RMap rMap = (RMap) rObject;
//            rMap.delete();
//            rMap.putAll((Map) value);
//            rMap.expire(cacheConfig.getTTL(), TimeUnit.MILLISECONDS);
//        } else if (rObject instanceof RList) {
//            RList rList = (RList) rObject;
//            rList.delete();
//            rList.addAll((Collection) value);
//            rList.expire(cacheConfig.getTTL(), TimeUnit.MILLISECONDS);
//        } else if (rObject instanceof RSet) {
//            RSet rSet = (RSet) rObject;
//            rSet.delete();
//            rSet.addAll((Collection) value);
//            rSet.expire(cacheConfig.getTTL(), TimeUnit.MILLISECONDS);
//        } else if (rObject instanceof RSortedSet) {
//            RSortedSet rSortedSet = (RSortedSet) rObject;
//            rSortedSet.delete();
//            rSortedSet.addAll((Collection) value);
//            redissonClient.getKeys().expire(redisKey, cacheConfig.getTTL(), TimeUnit.MILLISECONDS);
//        }
//
//        addCachePut();
//    }
//
//    private RObject getrObject(String redisKey, Object value) {
//        RObject rObject;
//        if (value instanceof Map) {
//            rObject = redissonClient.getMap(redisKey);
//        } else if (value instanceof List) {
//            rObject = redissonClient.getList(redisKey);
//        } else if (value instanceof SortedSet) {
//            rObject = redissonClient.getSortedSet(redisKey);
//        } else if (value instanceof Set) {
//            rObject = redissonClient.getSet(redisKey);
//        } else {
//            rObject = redissonClient.getBucket(redisKey);
//        }
//        return rObject;
//    }
//
//    @Override
//    public ValueWrapper putIfAbsent(Object key, Object value) {
//        Object prevValue = getValue(key);
//        if (prevValue == null && value != null) {
//            prevValue = getValue(key);
//            put(key, value);
//            addCachePut();
//        }
//        return toValueWrapper(prevValue);
//    }
//
//    @Override
//    public void evict(Object key) {
//        RObject rObject = cacheMap.get(key);
//        if (rObject == null) {
//            return;
//        }
//        rObject.delete();
//    }
//
//    @Override
//    public void clear() {
//        cacheMap.values().forEach(RObject::delete);
//    }
//
//    public void putCacheConfig(Object key, Supplier<CacheConfig> cacheConfigSupplier) {
//        keyConfigMap.putIfAbsent(key, cacheConfigSupplier.get());
//    }
//
//    /** The number of get requests that were satisfied by the cache.
//     * @return the number of hits
//     */
//    public long getCacheHits(){
//        return hits.get();
//    }
//
//    /** A miss is a get request that is not satisfied.
//     * @return the number of misses
//     */
//    public long getCacheMisses(){
//        return misses.get();
//    }
//
//    public long getCachePuts() {
//        return puts.get();
//    }
//
//    private Object getValue(Object key) {
//        RObject rObject = cacheMap.get(key);
//        return getValue(rObject);
//    }
//
//    private Object getValue(RObject rObject) {
//        Object value = null;
//        if (rObject instanceof RBucket) {
//            value = ((RBucket) rObject).get();
//        } else if (rObject instanceof RMap) {
//            value = ((RMap) rObject).readAllMap();
//        } else if (rObject instanceof RList) {
//            value = ((RList) rObject).readAll();
//        } else if (rObject instanceof RSet) {
//            value = ((RSet) rObject).readAll();
//        } else if (rObject instanceof RSortedSet) {
//            value = ((RSortedSet) rObject).readAll();
//        }
//        return value;
//    }
//
//    private ValueWrapper toValueWrapper(Object value) {
//        if (value == null) {
//            return null;
//        }
//        return new SimpleValueWrapper(value);
//    }
//
//    private <T> Object putValue(Object key, Callable<T> valueLoader) {
//        Object value;
//        try {
//            value = valueLoader.call();
//        } catch (Exception ex) {
//            RuntimeException exception;
//            try {
//                Class<?> c = Class.forName("org.springframework.cache.Cache$ValueRetrievalException");
//                Constructor<?> constructor = c.getConstructor(Object.class, Callable.class, Throwable.class);
//                exception = (RuntimeException) constructor.newInstance(key, valueLoader, ex);
//            } catch (Exception e) {
//                throw new IllegalStateException(e);
//            }
//            throw exception;
//        }
//        put(key, value);
//        return value;
//    }
//
//    private void addCachePut() {
//        puts.incrementAndGet();
//    }
//
//    private void addCacheHit(){
//        hits.incrementAndGet();
//    }
//
//    private void addCacheMiss(){
//        misses.incrementAndGet();
//    }
//}
