package com.acyumi.spring.cache;

import org.springframework.cache.Cache;

/**
 * 可指定key过期时间的缓存对象.
 *
 * @author Mr.XiHui
 * @date 2019/9/8
 */
public interface ExpireKeyCache extends Cache {

    /**
     * key如果经过了包装，则请实现此方法来解包得到原来的key.
     *
     * @param key 可能经过包装的key
     * @return 原来的key
     * @see ExpireKeyCacheKeyWrapper
     */
    Object unwrapKey(Object key);

    /**
     * key如果经过了{@link ExpireKeyCacheKeyWrapper}包装，
     * 则可实现此方法来解包得到{@link ExpireKeyCacheOperation}.
     *
     * @param ekckWrapper key包装类
     * @return 原来的key
     * @see ExpireKeyCacheKeyWrapper
     */
    ExpireKeyCacheOperation getExpireKeyCacheOperation(ExpireKeyCacheKeyWrapper ekckWrapper);
}
