package com.acyumi.spring.cache;

import com.acyumi.util.ParameterUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.annotation.CacheAnnotationParser;
import org.springframework.cache.annotation.ProxyCachingConfiguration;
import org.springframework.cache.interceptor.*;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.function.Supplier;

/**
 * 代理缓存配置类，此类主要是用于覆盖{@link CacheAnnotationParser}.
 *
 * @author Mr.XiHui
 * @date 2019/9/4
 * @see ProxyCachingConfiguration
 */
@Aspect
//@ConditionalOnClass(CacheManager.class)
public class ExpireKeyProxyCachingConfiguration {

    /**
     * 提供{@link CacheOperation}的源，它使用{@link CacheAnnotationParser}解析得到{@link CacheOperation}
     */
    private CacheOperationSource cacheOperationSource;
    /**
     * SpringCache相关注解的解析器，由它解析出{@link CacheOperation}. <br>
     * <br>
     * (SpringCache相关注解)的解析过程在启动时和方法被调用时都有可能被执行，
     * 但对于SpringCache有效的方法来说，解析只执行一次，
     * 解析完之后{@link CacheOperation}被缓存在
     * {@link AbstractFallbackCacheOperationSource#attributeCache}
     *
     * @see ExpireKeyCacheAnnotationParser
     */
    private CacheAnnotationParser cacheAnnotationParser;

    /**
     * 如果IOC容器中无{@link CacheAnnotationParser}，则此配置类什么都没做，相当于无效
     * 如果IOC容器中有{@link CacheAnnotationParser}，但不是{@link ExpireKeyCacheAnnotationParser}
     * 则此配置类只替换了{@link CacheOperationSource}，却未替换{@link CacheInterceptor}
     *
     * @param cacheAnnotationParser CacheAnnotationParser
     */
    @Autowired(required = false)
    public void setCacheAnnotationParser(CacheAnnotationParser cacheAnnotationParser) {
        this.cacheAnnotationParser = cacheAnnotationParser;
    }

    /**
     * 通过AOP环绕通知替换掉SpringCache初始化的CacheOperationSource. <br>
     * <br>
     * 调试查看{@link ProxyCachingConfiguration}和 <br>
     * {@link BeanFactoryCacheOperationSourceAdvisor}之后发现 <br><br>
     * 项目启动时初始化默认的{@link AnnotationCacheOperationSource}后 <br>
     * 它就被set给{@link BeanFactoryCacheOperationSourceAdvisor}， <br><br>
     * 然后{@link BeanFactoryCacheOperationSourceAdvisor}的内部类 <br>
     * <div>@link CacheOperationSourcePointcut#matches(java.lang.reflect.Method, Class)</div> <br>
     * 就被调用以进行代理增强匹配操作，这个操作会往<div>@link AnnotationCacheOperationSource#attributeCache</div>缓存数据 <br><br>
     * 如果默认初始化的{@link AnnotationCacheOperationSource}并不是我们想要的， <br><br>
     * 当我们通过简单的自动配置来修改{@link BeanFactoryCacheOperationSourceAdvisor}中的{@link AnnotationCacheOperationSource} <br>
     * 那它做的这些操作会浪费掉，所以我们通过AOP环绕通知在直接把默认的{@link AnnotationCacheOperationSource}替换掉 <br><br>
     * <p>
     * 如果以后SpringCache跟随SpringBoot的升级有版本变动，就再维护
     *
     * @param pjp 执行过程切入点
     * @return CacheOperationSource
     * @throws Throwable pjp.proceed()可能会异常
     * @see ProxyCachingConfiguration#cacheOperationSource()
     */
    @Around("execution(public org.springframework.cache.interceptor.CacheOperationSource " +
            "org.springframework.cache.annotation.ProxyCachingConfiguration.cacheOperationSource())")
    public CacheOperationSource replaceCacheOperationSource(ProceedingJoinPoint pjp) throws Throwable {
        if (cacheAnnotationParser == null) {
            return (CacheOperationSource) pjp.proceed();
        }
        this.cacheOperationSource = new AnnotationCacheOperationSource(cacheAnnotationParser);
        return cacheOperationSource;
    }

    /**
     * 通过AOP环绕通知替换掉SpringCache初始化的CacheInterceptor. <br>
     *
     * @param pjp 执行过程切入点
     * @return CacheInterceptor
     * @throws Throwable pjp.proceed()可能会异常
     * @see ProxyCachingConfiguration#cacheInterceptor()
     */
    @Around("execution(public org.springframework.cache.interceptor.CacheInterceptor " +
            "org.springframework.cache.annotation.ProxyCachingConfiguration.cacheInterceptor())")
    @SuppressWarnings("all")
    public CacheInterceptor replaceCacheInterceptor(ProceedingJoinPoint pjp) throws Throwable {
        if (!(cacheAnnotationParser instanceof ExpireKeyCacheAnnotationParser)) {
            return (CacheInterceptor) pjp.proceed();
        }
        ProxyCachingConfiguration pjpTarget = (ProxyCachingConfiguration) pjp.getTarget();
        Class<? extends ProxyCachingConfiguration> configurationClass = pjpTarget.getClass();
        Field errorHandlerField = ReflectionUtils.findField(configurationClass, "errorHandler");
        Field keyGeneratorField = ReflectionUtils.findField(configurationClass, "keyGenerator");
        Field cacheResolverField = ReflectionUtils.findField(configurationClass, "cacheResolver");
        Field cacheManagerField = ReflectionUtils.findField(configurationClass, "cacheManager");
        //这四个Field取不到的话有可能是版本问题，要更新维护这里的代码才行
        if (ParameterUtils.hasAnyEmpty(errorHandlerField, keyGeneratorField, cacheResolverField, cacheManagerField)) {
            return (CacheInterceptor) pjp.proceed();
        }
        ReflectionUtils.makeAccessible(errorHandlerField);
        ReflectionUtils.makeAccessible(keyGeneratorField);
        ReflectionUtils.makeAccessible(cacheResolverField);
        ReflectionUtils.makeAccessible(cacheManagerField);
        ExpireKeyCacheAnnotationParser asaParser = (ExpireKeyCacheAnnotationParser) cacheAnnotationParser;
        ExpireKeyCacheInterceptor expireKeyCacheInterceptor = new ExpireKeyCacheInterceptor(asaParser);
        Supplier<CacheErrorHandler> errorHandler = (Supplier<CacheErrorHandler>) errorHandlerField.get(pjpTarget);
        Supplier<KeyGenerator> keyGenerator = (Supplier<KeyGenerator>) keyGeneratorField.get(pjpTarget);
        Supplier<CacheResolver> cacheResolver = (Supplier<CacheResolver>) cacheResolverField.get(pjpTarget);
        Supplier<CacheManager> cacheManager = (Supplier<CacheManager>) cacheManagerField.get(pjpTarget);
        expireKeyCacheInterceptor.configure(errorHandler, keyGenerator, cacheResolver, cacheManager);
        expireKeyCacheInterceptor.setCacheOperationSource(cacheOperationSource);
        return expireKeyCacheInterceptor;
    }

    ///**
    // * 保留做对比参考
    // * 自定义CacheOperationSource
    // *
    // * @return CacheOperationSource
    // */
    //@Bean
    //@Primary
    //public CacheOperationSource primaryCacheOperationSource(CacheAspectSupport cacheAspectSupport,
    //                                                        BeanFactoryCacheOperationSourceAdvisor cacheAdvisor) {
    //
    //    ExpireKeyCacheAnnotationParser annotationParser = new ExpireKeyCacheAnnotationParser();
    //    AnnotationCacheOperationSource cacheOperationSource = new AnnotationCacheOperationSource(annotationParser);
    //
    //    cacheAspectSupport.setCacheOperationSource(cacheOperationSource);
    //    cacheAdvisor.setCacheOperationSource(cacheOperationSource);
    //
    //    return cacheOperationSource;
    //}

}
