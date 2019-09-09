package com.acyumi.annotation;

import com.acyumi.spring.cache.ExpireKeyRedissonCacheResolver;
import com.acyumi.spring.cache.ExpireKeySpringCacheConfig;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 拓展过期时间的CachePut注解. <br>
 * 使用此注解必须保证SpringIOC中有名字为 <br>
 * {@link ExpireKeyRedissonCacheResolver#EXPIRE_KEY_REDISSON_CACHE_RESOLVER_BEAN_NAME} <br>
 * 且类型为{@link ExpireKeyRedissonCacheResolver}的Bean， <br>
 * 直接引入配置类{@link ExpireKeySpringCacheConfig}即可
 *
 * @author Mr.XiHui
 * @date 2019/9/4
 * @see ExpireKeyRedissonCacheResolver
 * @see ExpireKeySpringCacheConfig
 */
@Inherited
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@CachePut(cacheResolver = ExpireKeyRedissonCacheResolver.EXPIRE_KEY_REDISSON_CACHE_RESOLVER_BEAN_NAME)
public @interface ExpireKeyCachePut {

    /**
     * Alias for {@link #cacheNames}.
     *
     * @return String[]
     */
    @AliasFor(value = "cacheNames", annotation = CachePut.class)
    String[] value() default {};

    /**
     * Names of the caches to use for the cache put operation.
     * <p>Names may be used to determine the target cache (or caches), matching
     * the qualifier value or bean name of a specific bean definition.
     *
     * @return String[]
     * @see #value
     * @see CacheConfig#cacheNames
     */
    @AliasFor(value = "value", annotation = CachePut.class)
    String[] cacheNames() default {};

    /**
     * Spring Expression Language (SpEL) expression for computing the key dynamically.
     * <p>Default is {@code ""}, meaning all method parameters are considered as a key,
     * unless a custom {@link #keyGenerator} has been set.
     * <p>The SpEL expression evaluates against a dedicated context that provides the
     * following meta-data:
     * <ul>
     * <li>{@code #result} for a reference to the result of the method invocation. For
     * supported wrappers such as {@code Optional}, {@code #result} refers to the actual
     * object, not the wrapper</li>
     * <li>{@code #root.method}, {@code #root.target}, and {@code #root.caches} for
     * references to the {@link java.lang.reflect.Method method}, target object, and
     * affected cache(s) respectively.</li>
     * <li>Shortcuts for the method name ({@code #root.methodName}) and target class
     * ({@code #root.targetClass}) are also available.
     * <li>Method arguments can be accessed by index. For instance the second argument
     * can be accessed via {@code #root.args[1]}, {@code #p1} or {@code #a1}. Arguments
     * can also be accessed by name if that information is available.</li>
     * </ul>
     *
     * @return String
     */
    @AliasFor(annotation = CachePut.class)
    String key() default "";

    /**
     * The bean name of the custom {@link org.springframework.cache.interceptor.KeyGenerator}
     * to use.
     * <p>Mutually exclusive with the {@link #key} attribute.
     *
     * @return String
     * @see CacheConfig#keyGenerator
     */
    @AliasFor(annotation = CachePut.class)
    String keyGenerator() default "";

    /**
     * 过期时间值，其单位通过{@link #expireTimeUnit()}指定，默认单位为秒
     *
     * @return long
     */
    long expire() default 30 * 60;

    /**
     * 时间单位，默认为秒
     *
     * @return TimeUnit
     */
    TimeUnit expireTimeUnit() default TimeUnit.SECONDS;

    /**
     * Spring Expression Language (SpEL) expression used for making the cache
     * put operation conditional.
     * <p>Default is {@code ""}, meaning the method result is always cached.
     * <p>The SpEL expression evaluates against a dedicated context that provides the
     * following meta-data:
     * <ul>
     * <li>{@code #root.method}, {@code #root.target}, and {@code #root.caches} for
     * references to the {@link java.lang.reflect.Method method}, target object, and
     * affected cache(s) respectively.</li>
     * <li>Shortcuts for the method name ({@code #root.methodName}) and target class
     * ({@code #root.targetClass}) are also available.
     * <li>Method arguments can be accessed by index. For instance the second argument
     * can be accessed via {@code #root.args[1]}, {@code #p1} or {@code #a1}. Arguments
     * can also be accessed by name if that information is available.</li>
     * </ul>
     *
     * @return String
     */
    @AliasFor(annotation = CachePut.class)
    String condition() default "";

    /**
     * Spring Expression Language (SpEL) expression used to veto the cache put operation.
     * <p>Unlike {@link #condition}, this expression is evaluated after the method
     * has been called and can therefore refer to the {@code result}.
     * <p>Default is {@code ""}, meaning that caching is never vetoed.
     * <p>The SpEL expression evaluates against a dedicated context that provides the
     * following meta-data:
     * <ul>
     * <li>{@code #result} for a reference to the result of the method invocation. For
     * supported wrappers such as {@code Optional}, {@code #result} refers to the actual
     * object, not the wrapper</li>
     * <li>{@code #root.method}, {@code #root.target}, and {@code #root.caches} for
     * references to the {@link java.lang.reflect.Method method}, target object, and
     * affected cache(s) respectively.</li>
     * <li>Shortcuts for the method name ({@code #root.methodName}) and target class
     * ({@code #root.targetClass}) are also available.
     * <li>Method arguments can be accessed by index. For instance the second argument
     * can be accessed via {@code #root.args[1]}, {@code #p1} or {@code #a1}. Arguments
     * can also be accessed by name if that information is available.</li>
     * </ul>
     *
     * @return boolean
     */
    @AliasFor(annotation = CachePut.class)
    String unless() default "";
}
