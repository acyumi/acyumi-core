package com.acyumi.annotation;

import com.acyumi.util.JsonUtils;

import java.lang.annotation.*;

/**
 * 配合{@link JsonUtils#toJsonIgnoreSpeciallyStr(Object)}方法来特殊序列化输出Json字符串的注解.
 * <br>
 * 应用场景：<br>
 * 假设有个dto对象(Knowledge.class)它有个属性(private String content)表示知识内容， <br>
 * 这个属性有可能超长，达到10000个字符，那么我在打印日志的时候就不想输出它了， <br>
 * 这时候你可以在其上声明@JsonIgnoreSpecially <br>
 * 然后打印日志的时候使用{@link JsonUtils#toJsonIgnoreSpeciallyStr(Object)}方法 <br>
 *
 * @author Mr.XiHui
 * @date 2019/3/6
 * @see JsonUtils#toJsonIgnoreSpeciallyStr(Object)
 * @see com.fasterxml.jackson.databind.ObjectMapper#addMixIn(Class, Class)
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonIgnoreSpecially {

}
