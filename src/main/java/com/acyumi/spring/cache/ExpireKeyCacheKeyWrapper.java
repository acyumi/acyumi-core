package com.acyumi.spring.cache;

import com.acyumi.util.JsonUtils;

import java.io.Serializable;

/**
 * 拓展过期时间的缓存key包装类.
 *
 * @author Mr.XiHui
 * @date 2019/9/8
 */
public class ExpireKeyCacheKeyWrapper implements Serializable {

    private static final long serialVersionUID = -6778269985090201545L;
    private final Object key;
    private final ExpireKeyCacheOperation expireKeyCacheOperation;

    public ExpireKeyCacheKeyWrapper(Object key, ExpireKeyCacheOperation expireKeyCacheOperation) {
        this.key = key;
        this.expireKeyCacheOperation = expireKeyCacheOperation;
    }

    public Object getKey() {
        return key;
    }

    public ExpireKeyCacheOperation getExpireKeyCacheOperation() {
        return expireKeyCacheOperation;
    }

    @Override
    public String toString() {
        return JsonUtils.toJsonStr(this);
    }
}
