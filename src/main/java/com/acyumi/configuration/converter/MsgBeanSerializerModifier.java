//package com.acyumi.configuration.converter;
//
//import com.fasterxml.jackson.annotation.JsonInclude;
//import com.fasterxml.jackson.databind.*;
//import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
//import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;
//import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
//import org.springframework.util.ClassUtils;
//
//import java.time.temporal.Temporal;
//import java.util.Date;
//import java.util.List;
//
///**
// * 序列化Bean的BeanPropertyWriter修改器
// * 目的是在序列化Bean时指定具体类型的null序列化器
// *
// * @author Mr.XiHui
// * @date 2019/1/7
// * @see MsgNullSerializers
// * @see BeanSerializerFactory#constructBeanSerializer(SerializerProvider, BeanDescription)
// */
////@Component
//public class MsgBeanSerializerModifier extends BeanSerializerModifier {
//
//    private final JsonSerializer<Object> nullJsonStringSerializer;
//    private final JsonSerializer<Object> nullJsonNumberSerializer;
//    private final JsonSerializer<Object> nullJsonBooleanSerializer;
//    private final JsonSerializer<Object> nullJsonArraySerializer;
//
//    /**
//     * 因为这里的入参都是且IOC窗口中有多个相同类型的Bean，
//     * 所以一定要严格匹配Bean名或存在声明有@Primary注解的Bean才能正确注入，
//     * 否则会报需求Bean数与发现Bean数不匹配的错误
//     */
//    public MsgBeanSerializerModifier(JsonSerializer<Object> nullJsonStringSerializer,
//                                     JsonSerializer<Object> nullJsonNumberSerializer,
//                                     JsonSerializer<Object> nullJsonBooleanSerializer,
//                                     JsonSerializer<Object> nullJsonArraySerializer) {
//        this.nullJsonStringSerializer = nullJsonStringSerializer;
//        this.nullJsonNumberSerializer = nullJsonNumberSerializer;
//        this.nullJsonBooleanSerializer = nullJsonBooleanSerializer;
//        this.nullJsonArraySerializer = nullJsonArraySerializer;
//    }
//
//    /**
//     * 修改BeanPropertyWriter集合的属性，指定具体类型的null序列化器
//     * <p>
//     * {@link BeanSerializerFactory#constructBeanSerializer(SerializerProvider, BeanDescription)}方法中会有这么一段 <br>
//     * if (_factoryConfig.hasSerializerModifiers()) { <br>
//     * &nbsp;&nbsp;for (BeanSerializerModifier mod : _factoryConfig.serializerModifiers()) { <br>
//     * &nbsp;&nbsp;&nbsp;&nbsp;props = mod.changeProperties(config, beanDesc, props); <br>
//     * &nbsp;&nbsp;} <br>
//     * } <br>
//     * 代表这里从factoryConfig中拿出来Modifiers集合，并且通过这些Modifiers对List&lt;BeanPropertyWriter&gt;进行修改 <br>
//     * 所以定义当前类重写此方法以指定具体类型的null序列化器
//     *
//     * @return List&lt;BeanPropertyWriter&gt;
//     */
//    @Override
//    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc,
//                                                     List<BeanPropertyWriter> beanProperties) {
//        if (beanProperties == null) {
//            return super.changeProperties(config, beanDesc, null);
//        }
//
//        // 循环所有的BeanPropertyWriter
//        for (int i = 0; i < beanProperties.size(); i++) {
//            BeanPropertyWriter beanPropertyWriter = beanProperties.get(i);
//
//            JsonInclude jsonInclude = beanPropertyWriter.getAnnotation(JsonInclude.class);
//            boolean isIgnoreEmpty = false;
//            if (jsonInclude != null) {
//                JsonInclude.Include value = jsonInclude.value();
//                if (value == JsonInclude.Include.NON_EMPTY
//                        || value == JsonInclude.Include.NON_ABSENT
//                        || value == JsonInclude.Include.NON_NULL) {
//                    isIgnoreEmpty = true;
//                }
//            }
//            //如果忽略解析空值的属性，则不用设置null序列化器
//            if (isIgnoreEmpty) {
//                continue;
//            }
//
//            JavaType javaType = beanPropertyWriter.getType();
//            //判断是否待序列化成字符串类型（时间类型如果为null也使用nullJsonStringSerializer）
//            if (javaType.isTypeOrSubTypeOf(CharSequence.class)
//                    || javaType.isTypeOrSubTypeOf(Date.class)
//                    || javaType.isTypeOrSubTypeOf(Temporal.class)) {
//                beanPropertyWriter.assignNullSerializer(nullJsonStringSerializer);
//            }
//
//            //判断是否待序列化成数组类型
//            else if (javaType.isArrayType() || javaType.isCollectionLikeType()) {
//                beanPropertyWriter.assignNullSerializer(nullJsonArraySerializer);
//            }
//
//            //判断是否基本数据类型及其包装类
//            else {
//
//                Class<?> rawClass = javaType.getRawClass();
//                boolean isPrimitive = javaType.isPrimitive();
//                if (isPrimitive) {
//                    rawClass = ClassUtils.resolvePrimitiveIfNecessary(rawClass);
//                }
//
//                //判断是否待序列化成数字类型
//                if (ClassUtils.isAssignable(Number.class, rawClass)) {
//                    beanPropertyWriter.assignNullSerializer(nullJsonNumberSerializer);
//                }
//
//                //判断是否待序列化成布尔类型
//                else if (ClassUtils.isAssignable(Boolean.class, rawClass)) {
//                    beanPropertyWriter.assignNullSerializer(nullJsonBooleanSerializer);
//                }
//            }
//        }
//
//        return beanProperties;
//    }
//}
