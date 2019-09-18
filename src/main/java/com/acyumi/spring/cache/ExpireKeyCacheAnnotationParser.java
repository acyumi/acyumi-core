package com.acyumi.spring.cache;

import com.acyumi.annotation.ExpireKeyCachePut;
import com.acyumi.annotation.ExpireKeyCacheable;
import com.acyumi.cast.Castor;
import com.acyumi.util.ParameterUtils;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.SpringCacheAnnotationParser;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SpringCache相关注解的解析器，由它解析出{@link CacheOperation}， <br>
 * 并且额外地解析{@link ExpireKeyCacheable}和{@link ExpireKeyCachePut} <br>
 * <br>
 * (SpringCache相关注解)的解析过程在启动时和方法被调用时都有可能被执行，
 * 但对于SpringCache有效的方法来说，解析只执行一次，
 * 解析完之后{@link CacheOperation}被缓存在
 * {@link org.springframework.cache.interceptor.AbstractFallbackCacheOperationSource#attributeCache}
 * {@link ExpireKeyCacheOperation}被缓存在{@link #oeCacheOperationMap}
 *
 * @author Mr.XiHui
 * @date 2019/9/6
 * @see ExpireKeyProxyCachingConfiguration
 * @see ExpireKeyCacheInterceptor
 */
public class ExpireKeyCacheAnnotationParser extends SpringCacheAnnotationParser {

    private static final long serialVersionUID = 4324884379539351437L;
    private final ConcurrentMap<CacheOperation, ExpireKeyCacheOperation> oeCacheOperationMap = new ConcurrentHashMap<>();

    public ExpireKeyCacheOperation getExpireKeyCacheOperation(CacheOperation originalCacheOperation) {
        return oeCacheOperationMap.get(originalCacheOperation);
    }

    @Override
    public Collection<CacheOperation> parseCacheAnnotations(Class<?> type) {
        Collection<CacheOperation> cacheOperations = super.parseCacheAnnotations(type);
        if (ParameterUtils.isEmpty(cacheOperations)) {
            return cacheOperations;
        }
        return parseCacheAnnotations(type, cacheOperations);
    }

    @Override
    public Collection<CacheOperation> parseCacheAnnotations(Method method) {
        Collection<CacheOperation> cacheOperations = super.parseCacheAnnotations(method);
        if (ParameterUtils.isEmpty(cacheOperations)) {
            return cacheOperations;
        }
        return parseCacheAnnotations(method, cacheOperations);
    }

    /**
     * 解析{@link ExpireKeyCacheable}和{@link ExpireKeyCachePut}，选优去重
     *
     * @param ae              AnnotatedElement
     * @param cacheOperations CacheOperation集合
     * @return 解析之后的CacheOperation集合
     */
    protected Collection<CacheOperation> parseCacheAnnotations(AnnotatedElement ae,
                                                               Collection<CacheOperation> cacheOperations) {
        //先判断此元素(类/方法)上是否使用了@AcyumiCacheable和@AcyumiCachePut这两个注解
        ExpireKeyCacheable expireKeyCacheable = AnnotatedElementUtils.findMergedAnnotation(ae, ExpireKeyCacheable.class);
        ExpireKeyCachePut expireKeyCachePut = AnnotatedElementUtils.findMergedAnnotation(ae, ExpireKeyCachePut.class);
        if (expireKeyCacheable == null && expireKeyCachePut == null) {
            return cacheOperations;
        }

        //取出类上面的@CacheConfig注解的配置（如果使用了@CacheConfig注解）
        String[] cacheConfigCacheNames = null;
        String cacheConfigKeyGenerator = null;
        CacheConfig cacheConfig = AnnotatedElementUtils.findMergedAnnotation(ae, CacheConfig.class);
        if (cacheConfig != null) {
            cacheConfigCacheNames = cacheConfig.cacheNames();
            cacheConfigKeyGenerator = cacheConfig.keyGenerator();
        }

        //初始化一个存放CacheOperation的Map，用来选优去重
        Map<String, CacheOperation> cacheOperationMap = new LinkedHashMap<>(ParameterUtils.calcMapCapacity(2));
        if (expireKeyCacheable != null) {
            ExpireKeyCacheableOperation expireKeyCacheableOperation = parseExpireKeyCacheableAnnotation(ae,
                    expireKeyCacheable, cacheConfigCacheNames, cacheConfigKeyGenerator);
            cacheOperationMap.put(getCacheOperationMapKey(expireKeyCacheableOperation), expireKeyCacheableOperation);
        }
        if (expireKeyCachePut != null) {
            ExpireKeyCachePutOperation expireKeyCachePutOperation = parseExpireKeyCachePutAnnotation(ae,
                    expireKeyCachePut, cacheConfigCacheNames, cacheConfigKeyGenerator);
            cacheOperationMap.put(getCacheOperationMapKey(expireKeyCachePutOperation), expireKeyCachePutOperation);
        }

        //父方法super.parseCacheAnnotations()解析完之后，
        //AcyumiCacheable被解析得到CacheableOperation，并储存在cacheOperations中
        //AcyumiCachePut被解析得到CachePutOperation，并储存在cacheOperations中

        //下面选优去重，
        //如果出现了cacheResolver为AcyumiRedissonCacheResolver.ACYUMI_REDISSON_CACHE_RESOLVER_BEAN_NAME，
        //且（注解、cacheNames、key、keyGenerator）相同的情况
        //则只保留其中一个且优先保留通过AcyumiCacheable和AcyumiCachePut注解对应的CacheOperation

        List<CacheOperation> coList = Castor.castIterable(cacheOperations, List.class);
        for (int i = 0; i < coList.size(); i++) {
            CacheOperation cacheOperation = coList.get(i);

            String cacheResolver = cacheOperation.getCacheResolver();
            if (!ExpireKeyRedissonCacheResolver.EXPIRE_KEY_REDISSON_CACHE_RESOLVER_BEAN_NAME.equals(cacheResolver)) {
                continue;
            }

            String cacheOperationMapKey = getCacheOperationMapKey(cacheOperation);
            CacheOperation preOperation = cacheOperationMap.get(cacheOperationMapKey);
            //如果存在相同缓存操作
            if (preOperation instanceof ExpireKeyCacheOperation) {
                //而且是通过使用@AcyumiCacheable和@AcyumiCachePut而来，则保存映射关系备缓存AOP用
                oeCacheOperationMap.put(cacheOperation, (ExpireKeyCacheOperation) preOperation);
                cacheOperationMap.put(cacheOperationMapKey, cacheOperation);
            } else if (preOperation != null) {
                //去重
                coList.remove(i--);
            }
        }
        return coList;
    }

    protected ExpireKeyCacheableOperation parseExpireKeyCacheableAnnotation(AnnotatedElement ae,
                                                                            ExpireKeyCacheable cacheable,
                                                                            String[] cacheConfigCacheNames,
                                                                            String cacheConfigKeyGenerator) {

        ExpireKeyCacheableOperation.Builder builder = new ExpireKeyCacheableOperation.Builder();
        builder.setName(ae.toString());
        builder.setCacheNames(cacheable.cacheNames());
        builder.setCondition(cacheable.condition());
        builder.setUnless(cacheable.unless());
        builder.setKey(cacheable.key());
        builder.setKeyGenerator(cacheable.keyGenerator());
        builder.setCacheManager("");
        builder.setCacheResolver(ExpireKeyRedissonCacheResolver.EXPIRE_KEY_REDISSON_CACHE_RESOLVER_BEAN_NAME);
        builder.setSync(cacheable.sync());
        builder.setExpire(cacheable.expire());
        builder.setExpireTimeUnit(cacheable.expireTimeUnit());

        applyDefault(cacheConfigCacheNames, cacheConfigKeyGenerator, builder);

        ExpireKeyCacheableOperation op = builder.build();
        validateCacheOperation(ae, op);

        return op;
    }

    protected ExpireKeyCachePutOperation parseExpireKeyCachePutAnnotation(AnnotatedElement ae,
                                                                          ExpireKeyCachePut cachePut,
                                                                          String[] cacheConfigCacheNames,
                                                                          String cacheConfigKeyGenerator) {

        ExpireKeyCachePutOperation.Builder builder = new ExpireKeyCachePutOperation.Builder();
        builder.setName(ae.toString());
        builder.setCacheNames(cachePut.cacheNames());
        builder.setCondition(cachePut.condition());
        builder.setUnless(cachePut.unless());
        builder.setKey(cachePut.key());
        builder.setKeyGenerator(cachePut.keyGenerator());
        builder.setCacheManager("");
        builder.setCacheResolver(ExpireKeyRedissonCacheResolver.EXPIRE_KEY_REDISSON_CACHE_RESOLVER_BEAN_NAME);
        builder.setExpire(cachePut.expire());
        builder.setExpireTimeUnit(cachePut.expireTimeUnit());

        applyDefault(cacheConfigCacheNames, cacheConfigKeyGenerator, builder);

        ExpireKeyCachePutOperation op = builder.build();
        validateCacheOperation(ae, op);

        return op;
    }

    private void applyDefault(String[] cacheConfigCacheNames, String cacheConfigKeyGenerator,
                              CacheOperation.Builder builder) {
        if (builder.getCacheNames().isEmpty() && cacheConfigCacheNames != null) {
            builder.setCacheNames(cacheConfigCacheNames);
        }
        if (ParameterUtils.isEmpty(builder.getKey()) && ParameterUtils.isEmpty(builder.getKeyGenerator()) &&
                !ParameterUtils.isEmpty(cacheConfigKeyGenerator)) {
            builder.setKeyGenerator(cacheConfigKeyGenerator);
        }
    }

    private void validateCacheOperation(AnnotatedElement ae, CacheOperation op) {
        if (StringUtils.hasText(op.getKey()) && StringUtils.hasText(op.getKeyGenerator())) {
            throw new IllegalStateException("Invalid cache annotation configuration on '" +
                    ae.toString() + "'. Both 'key' and 'keyGenerator' attributes have been set. " +
                    "These attributes are mutually exclusive: either set the SpEL expression used to" +
                    "compute the key at runtime or set the name of the KeyGenerator bean to use.");
        }
    }

    private String getCacheOperationMapKey(CacheOperation cacheOperation) {
        String name = cacheOperation.getName();
        Set<String> cacheNames = cacheOperation.getCacheNames();
        String key = cacheOperation.getKey();
        String keyGenerator = cacheOperation.getKeyGenerator();
        return name + cacheNames + key + keyGenerator;
    }
}
