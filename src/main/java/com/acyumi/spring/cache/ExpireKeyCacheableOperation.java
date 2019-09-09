package com.acyumi.spring.cache;

import org.springframework.cache.interceptor.CacheableOperation;

import java.util.concurrent.TimeUnit;

/**
 * AcyumiCacheable注解的属性描述对象
 *
 * @author Mr.XiHui
 * @date 2019/9/7
 */
public class ExpireKeyCacheableOperation extends CacheableOperation implements ExpireKeyCacheOperation {

    private final long expire;

    private final TimeUnit expireTimeUnit;

    /**
     * Create a new {@link CacheableOperation} instance from the given builder.
     *
     * @param b 构建器
     */
    public ExpireKeyCacheableOperation(Builder b) {
        super(b);
        this.expire = b.expire;
        this.expireTimeUnit = b.expireTimeUnit == null ? TimeUnit.SECONDS : b.expireTimeUnit;
    }

    @Override
    public long getExpire() {
        return expire;
    }

    @Override
    public TimeUnit getExpireTimeUnit() {
        return expireTimeUnit;
    }

    public static class Builder extends CacheableOperation.Builder {

        private long expire;

        private TimeUnit expireTimeUnit;

        public void setExpire(long expire) {
            this.expire = expire;
        }

        public void setExpireTimeUnit(TimeUnit expireTimeUnit) {
            this.expireTimeUnit = expireTimeUnit;
        }

        @Override
        protected StringBuilder getOperationDescription() {
            StringBuilder sb = super.getOperationDescription();
            sb.append(" | expire='");
            sb.append(this.expire);
            sb.append("'");
            sb.append(" | expireTimeUnit='");
            sb.append(this.expireTimeUnit);
            sb.append("'");
            return sb;
        }

        @Override
        public ExpireKeyCacheableOperation build() {
            return new ExpireKeyCacheableOperation(this);
        }
    }
}
