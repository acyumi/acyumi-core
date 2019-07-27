package com.acyumi.util;

import com.acyumi.annotation.JsonIgnoreSpecially;
import com.acyumi.configuration.converter.MsgDateDeserializer;
import com.acyumi.helper.TransMap;
import com.acyumi.reflect.Reflector;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.*;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapLikeType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Json工具类. <br>
 * 使用的是jackson的ObjectMapper进行对象与json字符串之间的序列化与反序列化 <br>
 *
 * @author Mr.XiHui
 * @date 2018/4/14
 * @see org.springframework.context.annotation.Import
 */
public class JsonUtils {

    /**
     * 为使{@link #toJsonIgnoreSpeciallyStr(Object)}方法通过注解{@link JsonIgnoreSpecially} <br>
     * 过滤属性而定义的Bean序列化修改器
     *
     * @see JsonIgnoreSpeciallyBeanSerializerModifier
     */
    private static final BeanSerializerModifier IGNORE_SPECIALLY_BEAN_SERIALIZER_MODIFIER;

    /**
     * jackson序列化与反序列化json的重量级对象.
     */
    //private static ObjectMapper OBJECT_MAPPER = new MsgObjectMapper();
    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {

        IGNORE_SPECIALLY_BEAN_SERIALIZER_MODIFIER = new JsonIgnoreSpeciallyBeanSerializerModifier();

        //全部字段序列化
        //对象的所有字段全部列入
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.ALWAYS);

        //反序列化时属性名称是否要求带双引号，默认为true
        //OBJECT_MAPPER.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, true);

        //反序列化时是否允许属性名称不带双引号
        OBJECT_MAPPER.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

        //忽略空Bean转json的错误
        OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        //忽略 在json字符串中存在，但是在java对象中不存在对应属性的情况。防止错误
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        //是否接受将没有使用“[]”包裹的单个值反序列化到数组或集合上
        //OBJECT_MAPPER.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

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
        module.addDeserializer(java.util.Date.class, new MsgDateDeserializer());

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
     * 私有构造. <br>
     * 利用{@link org.springframework.stereotype.Component} <br>
     * 或者{@link org.springframework.context.annotation.Import}注解， <br>
     * 可以在启动初始化项目时使Spring强制调用此私有构造给OBJECT_MAPPER重新赋值 <br><br>
     * 注意{@link org.springframework.context.annotation.Configuration}不能达到此效果，<br>
     * 使用@Configuration启动项目时如果没有public构造方法会报错 <br><br>
     * 如此做的目的是： <br>
     * 如果将此工具类放到Spring容器中使用，那么将使用Spring初始化的objectMapper代替静态生成的OBJECT_MAPPER <br>
     * <br>
     * 若需要Spring替换OBJECT_MAPPER，则在SpringBoot启动类或其他配置类上使用@Import({JsonUtils.class})即可
     *
     * @param objectMapper Spring容器自动注入
     * @see org.springframework.context.annotation.Import
     */
    private JsonUtils(ObjectMapper objectMapper) {
        OBJECT_MAPPER = objectMapper;
    }

    /**
     * 可使用此方法获取已经初始化好的ObjectMapper对象来调用ObjectMapper的其他方法.
     *
     * @return ObjectMapper
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    /**
     * 序列化对象成Json字符串.
     *
     * @param obj 对象
     * @return 未格式化的Json字符串
     */
    public static String toJsonStr(Object obj) {

        return toJsonStr(obj, false, null, false);
    }

    /**
     * 去除json字符串的头尾双引号. <br>
     * {@link #toJsonStr(Object)}和{@link #toPrettyJsonStr(Object)}序列化对象时， <br>
     * 得到的jsonStr极有可能会包上一层(\"\")，所以增加此方法来去除头尾双引号 <br>
     * 如"\"2018-05-01\"".equals(JsonUtils.toJsonStr(LocalDate.of(2018, 5, 1)))是true
     *
     * @param jsonStr json字符串
     * @return 去除头尾双引号的字符串
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
     * 序列化对象成格式化好的Json字符串.
     *
     * @param obj 对象
     * @return 格式化好的Json字符串
     */
    public static String toPrettyJsonStr(Object obj) {

        return toJsonStr(obj, false, null, true);
    }

    /**
     * 序列化对象成通过@JsonIgnoreSpecially注解过滤过属性的Json字符串.
     * <br>
     * 通过在对象的成员变量上声明注解{@link JsonIgnoreSpecially} <br>
     * 再调用此方法以在不影响 “框架” 和 {@link #toJsonStr(Object)} 的基础上 <br>
     * 达到输出过滤了指定属性的Json字符串的效果
     *
     * @param obj 对象
     * @return 未格式化的Json字符串
     * @see JsonIgnoreSpecially
     */
    public static String toJsonIgnoreSpeciallyStr(Object obj) {

        return toJsonStr(obj, true, null, false);
    }

    /**
     * 指定要过滤的属性序列化对象成Json字符串.
     *
     * @param obj              对象
     * @param ignoreProperties 对象中需要过滤的属性列表
     * @return 未格式化的Json字符串
     */
    public static String toJsonIgnoreSpeciallyStr(Object obj, String... ignoreProperties) {
        Set<String> ignorePropertySet = new HashSet<>();
        Collections.addAll(ignorePropertySet, ignoreProperties);
        return toJsonStr(obj, false, ignorePropertySet, false);
    }

    public static String toJsonIgnoreSpeciallyStr(Object obj, boolean takeEffectByJsonIgnoreSpecially,
                                                  String... ignoreProperties) {
        Set<String> ignorePropertySet = new HashSet<>();
        Collections.addAll(ignorePropertySet, ignoreProperties);
        return toJsonStr(obj, takeEffectByJsonIgnoreSpecially, ignorePropertySet, false);
    }

    /**
     * 解析Json字符串转成对象.
     *
     * @param jsonStr Json字符串
     * @param clazz   返回值Class
     * @param <T>     返回值的类型
     * @return T
     */
    @SuppressWarnings("unchecked")
    public static <T> T parseObj(String jsonStr, Class<T> clazz) {
        if (ParameterUtils.isEmpty(jsonStr)) {
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
     * 解析Json字符串转成对象（包括转成泛型类或复杂参数型类的对象）. <br>
     * 强烈建议使用此方法来反序列化json字符串到复杂的java对象
     * <p>
     * 如Map&lt;String,String&gt; map = parseObj("{}", new TypeReference&lt;Map&lt;String, String&gt;&gt;(){}); <br>
     * 如List&lt;Set&lt;String&gt;&gt; list = JsonUtils.parseObj("[[\"666\"]]", new TypeReference&lt;List&lt;Set&lt;
     * String&gt;&gt;&gt;(){});
     *
     * @param jsonStr       Json字符串
     * @param typeReference 类型引用对象，可以是Map/List/Set等(不限于集合)的类型引用
     * @param <T>           返回值的类型
     * @return T
     */
    @SuppressWarnings("unchecked")
    public static <T> T parseObj(String jsonStr, TypeReference<T> typeReference) {
        if (ParameterUtils.isEmpty(jsonStr)) {
            return null;
        }
        if (typeReference == null) {
            throw new IllegalArgumentException("TypeReference不能为null");
        } else if (Reflector.isAssignable(typeReference.getType(), String.class)) {
            return (T) toJsonStr(jsonStr);
        }
        try {
            return OBJECT_MAPPER.readValue(jsonStr, typeReference);
        } catch (Exception e) {
            throw new IllegalArgumentException("解析Json字符串转成对象失败", e);
        }
    }

    /**
     * 解析Json字符串转成泛型类或复杂参数型类对象. <br>
     * 可解析成类似以下对象 <br>
     * RequestMsg&lt;AddEnterpriseInVo&gt; msg = JsonUtils.parseParametricObj("{}", RequestMsg.class,
     * AddEnterpriseInVo.class); <br>
     * Map&lt;String, Object&gt; map = JsonUtils.parseParametricObj("{}", HashMap.class, String.class, Object.class);
     * <br>
     * 还有List&lt;String&gt;等等
     *
     * @param jsonStr         Json字符串
     * @param parametricClass 最外层Class，即带泛型等参数的Class，如Map.class/List.class
     * @param paramClasses    泛型参数Class数组
     * @param <T>             返回值的类型
     * @return T
     */
    @SuppressWarnings("unchecked")
    public static <T> T parseParametricObj(String jsonStr, Class<?> parametricClass,
                                           Class<?>... paramClasses) {
        if (ParameterUtils.isEmpty(jsonStr)) {
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
        if (ParameterUtils.isEmpty(jsonStr)) {
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
     * 解析Json字符串转成TransMap.
     *
     * @param jsonStr Json字符串
     * @return TransMap
     */
    public static TransMap parseTransMap(String jsonStr) {
        if (!StringUtils.hasText(jsonStr)) {
            return new TransMap(0);
        }

        MapLikeType mapLikeType = OBJECT_MAPPER.getTypeFactory()
                .constructRawMapLikeType(TransMap.class);

        try {
            return OBJECT_MAPPER.readValue(jsonStr, mapLikeType);
        } catch (Exception e) {
            throw new IllegalArgumentException("解析Json字符串转成TransMap失败", e);
        }
    }

    /**
     * 解析Json字符串转成List.
     *
     * @param jsonStr   Json字符串
     * @param elemClass 元素Class
     * @param <T>       元素的类型
     * @return List&lt;T&gt;
     */
    public static <T> List<T> parseList(String jsonStr, Class<T> elemClass) {
        if (ParameterUtils.isEmpty(jsonStr)) {
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
     * 解析Json字符串转成Set.
     *
     * @param jsonStr   Json字符串
     * @param elemClass 元素Class
     * @param <T>       元素的类型
     * @return Set&lt;T&gt;
     */
    public static <T> Set<T> parseSet(String jsonStr, Class<T> elemClass) {
        if (ParameterUtils.isEmpty(jsonStr)) {
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

    /**
     * 序列化对象成Json字符串.
     *
     * @param obj                             待序列化成json字符串的对象
     * @param takeEffectByJsonIgnoreSpecially 是否通过@JsonIgnoreSpecially注解过滤属性
     * @param ignoreProperties                哪些属性需要额外过滤
     * @param printerPretty                   是否格式化输出
     * @return json字符串
     */
    private static String toJsonStr(Object obj, boolean takeEffectByJsonIgnoreSpecially,
                                    Set<String> ignoreProperties, boolean printerPretty) {
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

            ObjectMapper objectMapper;
            if (ignoreProperties != null && ignoreProperties.size() > 0) {
                objectMapper = createIgnoreSpeciallyMapper(obj.getClass(), ignoreProperties);
            } else {
                objectMapper = OBJECT_MAPPER;
            }

            if (takeEffectByJsonIgnoreSpecially) {
                //判断是否还是静态的OBJECT_MAPPER，如果是，则复制一个对象出来使用
                if (objectMapper == OBJECT_MAPPER) {
                    objectMapper = objectMapper.copy();
                }
                SerializerFactory serializerFactory = objectMapper.getSerializerFactory();
                serializerFactory = serializerFactory.withSerializerModifier(IGNORE_SPECIALLY_BEAN_SERIALIZER_MODIFIER);
                objectMapper.setSerializerFactory(serializerFactory);
            }

            ObjectWriter objectWriter = objectMapper.writer();
            if (printerPretty) {
                objectWriter = objectWriter.withDefaultPrettyPrinter();
            }

            return objectWriter.writeValueAsString(obj);
        } catch (Exception e) {
            throw new IllegalArgumentException("序列化对象成Json字符串失败", e);
        }
    }

    /**
     * 生成clazz和ignoreProperties对应的ObjectMapper.
     *
     * @param clazz            被过滤属性的载体对象
     * @param ignoreProperties 被过滤的属性列表
     * @return ObjectMapper
     */
    private static ObjectMapper createIgnoreSpeciallyMapper(Class<?> clazz, Set<String> ignoreProperties) {
        //为保证静态的OBJECT_MAPPER的初始性，复制一个对象出来使用
        ObjectMapper objectMapper = OBJECT_MAPPER.copy();
        objectMapper.addMixIn(clazz, JsonIgnoreSpeciallyBeanSerializerModifier.class);
        FilterProvider filterProvider = createIgnoreSpeciallyFilterProvider(ignoreProperties);
        return objectMapper.setFilterProvider(filterProvider);
    }

    /**
     * 生成ignoreProperties对应的FilterProvider.
     *
     * @param ignoreProperties 属性过滤列表
     * @return FilterProvider
     */
    private static FilterProvider createIgnoreSpeciallyFilterProvider(Set<String> ignoreProperties) {
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        SimpleBeanPropertyFilter propertyFilter = SimpleBeanPropertyFilter.serializeAllExcept(ignoreProperties);
        filterProvider.addFilter(JsonIgnoreSpeciallyBeanSerializerModifier.JSON_IGNORE_PROPERTY_SPECIALLY_FILTER_ID,
                propertyFilter);
        return filterProvider;
    }

    /**
     * 序列化Bean的BeanPropertyWriter修改器. <br>
     * 目的是进行属性过滤操作
     *
     * @author Mr.XiHui
     * @date 2019/1/7
     * @see BeanSerializerFactory#constructBeanSerializer(SerializerProvider, BeanDescription)
     */
    @JsonFilter(JsonIgnoreSpeciallyBeanSerializerModifier.JSON_IGNORE_PROPERTY_SPECIALLY_FILTER_ID)
    private static class JsonIgnoreSpeciallyBeanSerializerModifier extends BeanSerializerModifier {

        /**
         * 注意当前类头上声明了一个注解. <br>
         * "@JsonFilter(JsonIgnoreSpeciallyBeanSerializerModifier.JSON_IGNORE_PROPERTY_SPECIALLY_FILTER_ID)", <br>
         * 是因为要使用{@link com.fasterxml.jackson.databind.ObjectMapper#addMixIn(Class target, Class mixinSource)} <br>
         * 来混入/掺和mixinSource上声明的JsonFilter注解指定的{@link com.fasterxml.jackson.databind.ser.PropertyFilter} <br>
         * 属性过滤器，再通过{@link JsonUtils#toJsonIgnoreSpeciallyStr(Object, String...)}方法来序列化json日志字符串
         */
        private static final String JSON_IGNORE_PROPERTY_SPECIALLY_FILTER_ID = "JsonIgnoreSpecially";

        /**
         * 修改BeanPropertyWriter集合的属性，以使 <br>
         * {@link JsonUtils#toJsonIgnoreSpeciallyStr(Object)}方法或 <br>
         * {@link JsonUtils#toJsonIgnoreSpeciallyStr(Object obj,
         * boolean takeEffectByJsonIgnoreSpecially,
         * String... ignoreProperties)} <br>
         * 当takeEffectByJsonIgnoreSpecially为true时对属性进行过滤操作
         * <p>
         * {@link BeanSerializerFactory#constructBeanSerializer(SerializerProvider, BeanDescription)}方法中会有这么一段 <br/>
         * if (_factoryConfig.hasSerializerModifiers()) { <br/>
         * &nbsp;&nbsp;for (BeanSerializerModifier mod : _factoryConfig.serializerModifiers()) { <br/>
         * &nbsp;&nbsp;&nbsp;&nbsp;props = mod.changeProperties(config, beanDesc, props); <br/>
         * &nbsp;&nbsp;} <br/>
         * } <br/>
         * 代表这里从factoryConfig中拿出来Modifiers集合，并且通过这些Modifiers对List<BeanPropertyWriter>进行修改 <br/>
         * 所以定义当前类重写此方法以进行属性过滤操作
         * <br>
         * 为使当前修改器生效，需要如下配置ObjectMapper再使用 <br>
         * <p>
         * SerializerFactory serializerFactory = objectMapper.getSerializerFactory(); <br>
         * serializerFactory = serializerFactory.withSerializerModifier(beanSerializerModifier); <br>
         * objectMapper.setSerializerFactory(serializerFactory); <br>
         *
         * @return List&lt;BeanPropertyWriter>
         */
        @Override
        public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc,
                                                         List<BeanPropertyWriter> beanProperties) {
            if (beanProperties == null) {
                return super.changeProperties(config, beanDesc, null);
            }

            // 循环所有的BeanPropertyWriter
            for (int i = 0; i < beanProperties.size(); i++) {
                BeanPropertyWriter beanPropertyWriter = beanProperties.get(i);

                //调用JsonUtils的toJsonIgnoreSpeciallyStr方法的takeEffectByJsonIgnoreSpecially为true时，
                //判断属性上是否声明有@JsonIgnoreSpecially注解，如果有则过滤此属性
                JsonIgnoreSpecially jsonIgnoreSpecially = beanPropertyWriter.getAnnotation(JsonIgnoreSpecially.class);
                if (jsonIgnoreSpecially != null) {
                    beanProperties.remove(i--);
                }
            }
            return beanProperties;
        }
    }
}
