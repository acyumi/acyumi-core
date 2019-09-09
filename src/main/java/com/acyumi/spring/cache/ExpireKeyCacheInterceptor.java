package com.acyumi.spring.cache;

import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.cache.interceptor.CacheOperation;

import java.lang.reflect.Method;

/**
 * 拓展过期时间的缓存AOP拦截器.
 *
 * @author Mr.XiHui
 * @date 2019/9/7
 */
public class ExpireKeyCacheInterceptor extends CacheInterceptor {

    private static final long serialVersionUID = -1738036869601419927L;

    private final ExpireKeyCacheAnnotationParser expireKeyCacheAnnotationParser;

    public ExpireKeyCacheInterceptor(ExpireKeyCacheAnnotationParser expireKeyCacheAnnotationParser) {
        this.expireKeyCacheAnnotationParser = expireKeyCacheAnnotationParser;
    }

    @Override
    protected CacheOperationContext getOperationContext(CacheOperation operation, Method method, Object[] args,
                                                        Object target, Class<?> targetClass) {
        ExpireKeyCacheOperation expireKeyCacheOperation = expireKeyCacheAnnotationParser.getExpireKeyCacheOperation(operation);
        if (expireKeyCacheOperation != null) {
            //当前只有两个ExpirableCacheOperation的实现类，且都是CacheOperation
            operation = (CacheOperation) expireKeyCacheOperation;
        }
        CacheOperationMetadata metadata = getCacheOperationMetadata(operation, method, method.getClass());
        return new ExpireKeyCacheOperationContext(metadata, args, target);
    }

    protected class ExpireKeyCacheOperationContext extends CacheOperationContext {

        public ExpireKeyCacheOperationContext(CacheOperationMetadata metadata, Object[] args, Object target) {
            super(metadata, args, target);
        }

        /**
         * 重写此方法的目的：判断当前Context持有的{@link CacheOperation}是否{@link ExpireKeyCacheOperation}，
         * 然后包装一下key以传递到{@link org.springframework.cache.Cache}使用.
         *
         * @param result Object
         * @return Object
         */
        @Override
        protected Object generateKey(Object result) {

            //Method method = getMethod();
            //boolean isParameterNamePresent = true;
            //
            //if (getMethod().getDeclaringClass().isInterface()) {
            //    Parameter[] parameters = method.getParameters();
            //    if (!ParameterUtils.isEmpty(parameters)) {
            //        Parameter firsParam = parameters[0];
            //        isParameterNamePresent = firsParam.isNamePresent();
            //    }
            //}
            //
            //Object key;
            //if (isParameterNamePresent) {
            //    key = super.generateKey(result);
            //} else {
                //CacheAspectSupport通过使用CacheOperationExpressionEvaluator来解析SpEL表达式生成key
                //CacheOperationExpressionEvaluator的父类CachedExpressionEvaluator使用了DefaultParameterNameDiscoverer，
                //1、DefaultParameterNameDiscoverer内部持有StandardReflectionParameterNameDiscoverer
                //2、DefaultParameterNameDiscoverer内部持有LocalVariableTableParameterNameDiscoverer
                //3、Java8开始，编译时带编译参数(-parameters)，通过
                //   StandardReflectionParameterNameDiscoverer或LocalVariableTableParameterNameDiscoverer
                //   都可获取真实参数名
                //4、编译时不带编译参数(-parameters)，通过LocalVariableTableParameterNameDiscoverer可获取class的真实参数名
                //   注意它不能获取interface的真实参数名
                //   原因：LocalVariableTableParameterNameDiscoverer工作原理为检查Java编译器生成在字节码里面的“调试信息”得到真实参数名
                //         然后不幸的是，interface的“调试信息”中并不包含方法名，so...
                //   解决方案：
                //       1、编译时带编译参数(-parameters)
                //       2、避免获取interface上参数名/避免在interface上使用SpringCache的注解（这应该就是不建议在interface上使用SpringCache的原因）
                //       3、SpringCache注解上的key上使用p0、p1...或者a0、a1...等方式使用参数名来指定SpEL表达式
                //       4、使用通用的KeyGenerator
                //
                //框架<groupId>com.thoughtworks.paranamer</groupId><artifactId>paranamer</artifactId>
                //提供了丰富的功用来解析参数名，
                //但是对于编译时不带编译参数(-parameters)的interface的参数名的解析
                //如果不借助于其他东西(如javaDoc的jar包等)还是取不到

                //key = tryGenerateKey(result);
            //}

            Object key = super.generateKey(result);
            CacheOperation operation = getOperation();
            if (operation instanceof ExpireKeyCacheOperation) {
                return new ExpireKeyCacheKeyWrapper(key, (ExpireKeyCacheOperation) operation);
            }
            return key;
        }
    }

}
