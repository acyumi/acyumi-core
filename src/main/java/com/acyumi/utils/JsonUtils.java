package com.acyumi.utils;

import com.acyumi.configuration.converter.MsgDateDeserializer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.util.StringUtils;
import org.springframework.util.TypeUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Json工具类
 * 使用的是jackson的ObjectMapper进行对象与json字符串之间的序列化与反序列化
 *
 * @author Mr.XiHui
 * @since 2018/4/14
 */
//@Component
public class JsonUtils {

    /**
     * jackson序列化与反序列化json的重量级对象
     */
    //private static ObjectMapper OBJECT_MAPPER = new MsgObjectMapper();
    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        //全部字段序列化
        //对象的所有字段全部列入
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.ALWAYS);

        //忽略空Bean转json的错误
        OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        //忽略 在json字符串中存在，但是在java对象中不存在对应属性的情况。防止错误
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        //取消默认转换timestamps形式
        OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        //Date对象的日期格式统一为以下的样式，即yyyy-MM-dd HH:mm:ss
        OBJECT_MAPPER.setDateFormat(DateTimeUtils.getDateFormat(DateTimeUtils.DEFAULT_DATE_TIME_PATTERN));

        //支持jsr310日期时间API
        JavaTimeModule module = new JavaTimeModule();

        module.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeUtils.DEFAULT_DATE_FORMATTER));
        module.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeUtils.DEFAULT_TIME_FORMATTER));
        module.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeUtils.DEFAULT_DATE_TIME_FORMATTER));

        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeUtils.DEFAULT_DATE_FORMATTER));
        module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeUtils.DEFAULT_TIME_FORMATTER));
        module.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeUtils.DEFAULT_DATE_TIME_FORMATTER));

        //设置Date使用动态格式的字符串反序列化成对象
        module.addDeserializer(Date.class, new MsgDateDeserializer());

        /*MsgDateDeserializer即此内部类，在此备份一下
        module.addDeserializer(java.util.Date.class, new DateDeserializers.DateDeserializer() {

            private static final long serialVersionUID = -8992816734856388583L;

            @Override
            public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

                if (p.hasToken(JsonToken.VALUE_STRING)) {
                    String str = p.getText().trim();
                    if (str.length() == 0) {
                        return null;
                    }

                    Date date = DateTimeUtils.dynamicParseToDate(str);
                    if (date != null) {
                        return date;
                    }
                }
                return super._parseDate(p, ctxt);
            }
        });
        */

        //使用自指定格式的JavaTimeModule
        OBJECT_MAPPER.registerModule(module);
    }

    /**
     * 利用@Component注解，可以在启动初始化项目时使Spring强制调用此私有构造给OBJECT_MAPPER重新赋值
     * 注意@Configuration不能达到此效果，使用@Configuration启动项目时如果没有public构造方法会报错
     * 如此做的目的是：
     * 如果将此工具类放到Spring容器中使用，那么将使用Spring初始化的objectMapper代替静态生成的OBJECT_MAPPER
     * 若不需要Spring替换OBJECT_MAPPER，去掉@Component注解即可
     *
     * @param objectMapper Spring容器自动注入
     */
    private JsonUtils(ObjectMapper objectMapper) {
        OBJECT_MAPPER = objectMapper;
    }

    /**
     * 可使用此方法获取已经初始化好的ObjectMapper对象来调用ObjectMapper的其他方法
     *
     * @return ObjectMapper
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * 序列化对象成Json字符串
     *
     * @param obj 对象
     * @return 未格式化的Json字符串
     */
    public static String toJsonStr(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            if (obj instanceof String) {
                try {
                    //如果obj本身是字符串，先转一次成对象(Map或List或String)
                    //这么做可以去掉字符串中的回车等空白字符串
                    obj = OBJECT_MAPPER.readValue((String) obj, Object.class);
                } catch (IOException e) {/*ignore*/}
            }
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new IllegalArgumentException("序列化对象成Json字符串失败", e);
        }
    }

    /**
     * {@link #toJsonStr(Object)}和{@link #toPrettyJsonStr(Object)}序列化对象时，
     * 得到的jsonStr极有可能会包上一层(\"\")，所以增加此方法来去除头尾双引号
     * 如"\"2018-05-01\"".equals(JsonUtils.toJsonStr(LocalDate.of(2018, 5, 1)))是true
     */
    public static String unWrapJsonStr(String jsonStr) {
        if (jsonStr == null) {
            return null;
        }
        int length = jsonStr.length();
        if (length > 1 && jsonStr.startsWith("\"") && jsonStr.endsWith("\"")) {
            jsonStr = jsonStr.substring(1, length - 1);
        }
        return jsonStr;
    }

    /**
     * 序列化对象成格式化好的Json字符串
     *
     * @param obj 对象
     * @return 格式化好的Json字符串
     */
    public static String toPrettyJsonStr(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            if (obj instanceof String) {
                try {
                    //如果obj本身是字符串，先转一次成对象(Map或List)
                    //这么做可以去掉字符串中的回车等空白字符串
                    obj = OBJECT_MAPPER.readValue((String) obj, Object.class);
                } catch (IOException e) {/*ignore*/}
            }
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            throw new IllegalArgumentException("序列化对象成格式化好的Json字符串失败", e);
        }
    }

    /**
     * 解析Json字符串转成对象
     *
     * @param jsonStr Json字符串
     * @param clazz   返回值Class
     * @return T
     */
    @SuppressWarnings("unchecked")
    public static <T> T parseObj(String jsonStr, Class<T> clazz) {
        if (!StringUtils.hasText(jsonStr)) {
            return null;
        }
        if (clazz == null) {
            throw new IllegalArgumentException("返回值Class不能为null");
        }
        if (clazz.isAssignableFrom(String.class)) {
            return (T) toJsonStr(jsonStr);
        }
        try {
            return OBJECT_MAPPER.readValue(jsonStr, clazz);
        } catch (Exception e) {
            throw new IllegalArgumentException("解析Json字符串转成对象失败", e);
        }
    }

    /**
     * 解析Json字符串转成对象（包括转成泛型类或复杂参数型类的对象）
     * 强烈建议使用此方法来反序列化json字符串到复杂的java对象
     * <p>
     * 如Map<String,String> map = parseObj("{}", new TypeReference<Map<String, String>>(){});
     * 如List<Set<String>> list = JsonUtils.parseObj("[[\"666\"]]", new TypeReference<List<Set<String>>>(){});
     *
     * @param jsonStr       Json字符串
     * @param typeReference 类型引用对象，可以是Map/List/Set等(不限于集合)的类型引用
     * @return T
     */
    @SuppressWarnings("unchecked")
    public static <T> T parseObj(String jsonStr, TypeReference<T> typeReference) {
        if (!StringUtils.hasText(jsonStr)) {
            return null;
        }
        if (typeReference == null) {
            throw new IllegalArgumentException("TypeReference不能为null");
        } else if (TypeUtils.isAssignable(typeReference.getType(), String.class)) {
            return (T) toJsonStr(jsonStr);
        }
        try {
            return OBJECT_MAPPER.readValue(jsonStr, typeReference);
        } catch (Exception e) {
            throw new IllegalArgumentException("解析Json字符串转成对象失败", e);
        }
    }

    /**
     * 解析Json字符串转成泛型类或复杂参数型类对象
     * 可解析成类似以下对象
     * RequestMsg<AddEnterpriseInVo> msg = JsonUtils.parseParametricObj("{}", RequestMsg.class, AddEnterpriseInVo.class);
     * Map<String, Object> map = JsonUtils.parseParametricObj("{}", HashMap.class, String.class, Object.class);
     * 还有List<String>等等
     *
     * @param jsonStr         Json字符串
     * @param parametricClass 最外层Class，即带泛型等参数的Class，如Map.class/List.class
     * @param paramClasses    泛型参数Class数组
     * @return T
     */
    @SuppressWarnings("unchecked")
    public static <T> T parseParametricObj(String jsonStr, Class<?> parametricClass,
                                           Class<?>... paramClasses) {
        if (!StringUtils.hasText(jsonStr)) {
            return null;
        }
        if (parametricClass == null) {
            throw new IllegalArgumentException("最外层Class，即带泛型等参数的Class不能为null");
        }
        if (parametricClass.isAssignableFrom(String.class)) {
            return (T) toJsonStr(jsonStr);
        }
        if (paramClasses == null) {
            throw new IllegalArgumentException("泛型参数Class数组不能为null");
        }
        JavaType javaType = OBJECT_MAPPER.getTypeFactory()
                .constructParametricType(parametricClass, paramClasses);
        try {
            return OBJECT_MAPPER.readValue(jsonStr, javaType);
        } catch (Exception e) {
            throw new IllegalArgumentException("解析Json字符串转成泛型类或复杂参数型类对象失败", e);
        }
    }

    public static <K, V> Map<K, V> parseMap(String jsonStr, Class<K> keyClass,
                                            Class<V> valueClass) {
        if (!StringUtils.hasText(jsonStr)) {
            return new LinkedHashMap<>(0);
        }
        if (keyClass == null) {
            throw new IllegalArgumentException("键Class不能为null");
        }
        if (valueClass == null) {
            throw new IllegalArgumentException("值Class不能为null");
        }


        MapType mapType = OBJECT_MAPPER.getTypeFactory()
                .constructMapType(LinkedHashMap.class, keyClass, valueClass);

        try {
            return OBJECT_MAPPER.readValue(jsonStr, mapType);
        } catch (Exception e) {
            throw new IllegalArgumentException("解析Json字符串转成Map失败", e);
        }
    }

    /**
     * 解析Json字符串转成List
     *
     * @param jsonStr   Json字符串
     * @param elemClass 元素Class
     * @return List<T>
     */
    public static <T> List<T> parseList(String jsonStr, Class<T> elemClass) {
        if (!StringUtils.hasText(jsonStr)) {
            return new ArrayList<>(0);
        }
        if (elemClass == null) {
            throw new IllegalArgumentException("List的元素Class不能为null");
        }

        CollectionType coType = OBJECT_MAPPER.getTypeFactory()
                .constructCollectionType(ArrayList.class, elemClass);

        try {
            return OBJECT_MAPPER.readValue(jsonStr, coType);
        } catch (Exception e) {
            throw new IllegalArgumentException("解析Json字符串转成List失败", e);
        }
    }

    /**
     * 解析Json字符串转成Set
     *
     * @param jsonStr   Json字符串
     * @param elemClass 元素Class
     * @return Set<T>
     */
    public static <T> Set<T> parseSet(String jsonStr, Class<T> elemClass) {
        if (!StringUtils.hasText(jsonStr)) {
            return new LinkedHashSet<>(0);
        }
        if (elemClass == null) {
            throw new IllegalArgumentException("Set的元素Class不能为null");
        }

        CollectionType coType = OBJECT_MAPPER.getTypeFactory()
                .constructCollectionType(LinkedHashSet.class, elemClass);

        try {
            return OBJECT_MAPPER.readValue(jsonStr, coType);
        } catch (Exception e) {
            throw new IllegalArgumentException("解析Json字符串转成Set失败", e);
        }
    }

}
