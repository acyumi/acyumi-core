//package com.acyumi.configuration.converter;
//
//import com.acyumi.util.DateTimeUtils;
//import com.fasterxml.jackson.databind.Module;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;
//import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
//import com.fasterxml.jackson.databind.ser.SerializerFactory;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
//import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
//import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
//import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
//import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
//import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
//import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
//import org.springframework.boot.autoconfigure.jackson.JacksonProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
//import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
//import org.springframework.util.StringUtils;
//
//import java.text.DateFormat;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.LocalTime;
//import java.time.format.DateTimeFormatter;
//import java.util.Locale;
//import java.util.TimeZone;
//
///**
// * http请求信息转换器配置
// *
// * @author Mr.XiHui
// * @date 2018/3/21
// *
// * @see JacksonAutoConfiguration 此类初始化ObjectMapper
// * @see MsgInputDecorator 此类用于包装(待解析成对象的)Json字符串输入流
// * @see MsgObjectMapper 此类使用了MsgInputDecorator，并重写了{@link ObjectMapper#_readMapAndClose}方法以打印解析异常日志
// * @see org.springframework.boot.autoconfigure.http.JacksonHttpMessageConvertersConfiguration
// * JacksonHttpMessageConvertersConfiguration为原初始化MappingJackson2HttpMessageConverter的类
// */
//@Configuration
//public class MsgConverterConfig {
//
//    @Bean
//    public MappingJackson2HttpMessageConverter getMappingJackson2HttpMessageConverter(ObjectMapper objectMapper) {
//
//        return new MappingJackson2HttpMessageConverter(objectMapper);
//    }
//
//    /**
//     * 代替Jackson2ObjectMapperBuilder#build()操作生成ObjectMapper
//     *
//     * @see Jackson2ObjectMapperBuilder#build()
//     */
//    @Bean("jacksonObjectMapper")
//    @Primary
//    public ObjectMapper jacksonObjectMapper(Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder,
//                                            JacksonProperties jacksonProperties,
//                                            BeanSerializerModifier beanSerializerModifier,
//                                            JavaTimeModule javaTimeModule) {
//        //日期时间格式配置
//        String jacksonDtFormat = jacksonProperties.getDateFormat();
//        TimeZone timeZone = jacksonProperties.getTimeZone();
//        Locale locale = jacksonProperties.getLocale();
//        if (!StringUtils.hasText(jacksonDtFormat)) {
//            jacksonDtFormat = DateTimeUtils.DEFAULT_DATE_TIME_PATTERN;
//        }
//        DateFormat dateFormat = DateTimeUtils.getDateFormat(jacksonDtFormat);
//        if (timeZone == null) {
//            //"GMT+8"或"Asia/Shanghai"(正常情况下在国内，ZoneId.systemDefault()得到的是"Asia/Shanghai")
//            timeZone = TimeZone.getTimeZone("GMT+8");
//        }
//        if (locale == null) {
//            locale = Locale.CHINA;
//        }
//
//        //指定好时间格式后替换jackson2ObjectMapperBuilder的相关时间格式配置
//        jackson2ObjectMapperBuilder.dateFormat(dateFormat);
//        jackson2ObjectMapperBuilder.timeZone(timeZone);
//        jackson2ObjectMapperBuilder.locale(locale);
//
//        //------------------------------------------------------------------------------------------
//
//        //ObjectMapper的子类
//        ObjectMapper objectMapper = new MsgObjectMapper();
//
//        SerializerFactory serializerFactory = objectMapper.getSerializerFactory();
//        serializerFactory = serializerFactory.withSerializerModifier(beanSerializerModifier);
//        objectMapper.setSerializerFactory(serializerFactory);
//
//        //取消默认转换timestamps形式
//        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
//
//        //设置支持jsr310日期时间API同时指定日期时间格式
//        objectMapper.registerModule(javaTimeModule);
//
//        //设置BigDecimal输出为两个小数四舍五入的字符串
//        //SimpleModule bigDecimalModule = new SimpleModule();
//        //bigDecimalModule.addSerializer(BigDecimal.class, new JsonSerializer<BigDecimal>() {
//        //    @Override
//        //    public void serialize(BigDecimal value, JsonGenerator gen,
//        //                          SerializerProvider serializers) throws IOException {
//        //        if (value == null) {
//        //            gen.writeString("0.00");
//        //        } else {
//        //            gen.writeString(value.setScale(0, BigDecimal.ROUND_HALF_UP).toString());
//        //        }
//        //    }
//        //});
//        //objectMapper.registerModules(bigDecimalModule);
//
//
//        //objectMapper.registerModule(javaTimeModule);必须在
//        //jackson2ObjectMapperBuilder.configure(objectMapper);前调用
//        //否则javaTimeModule将不生效，原因见下面生成JavaTimeModule的bean的注释
//        //------------------------------------------------------------------------------------------
//
//        //根据项目配置对objectMapper进行其他设置
//        jackson2ObjectMapperBuilder.configure(objectMapper);
//
//        return objectMapper;
//    }
//
//    /**
//     * 实现java8 api全局日期时间格式的重点步骤！！！
//     * 生成JavaTimeModule的bean用于objectMapper
//     * <p>
//     * {@link JacksonAutoConfiguration.Jackson2ObjectMapperBuilderCustomizerConfiguration.StandardJackson2ObjectMapperBuilderCustomizer#configureModules(Jackson2ObjectMapperBuilder)}方法
//     * 会从applicationContext中获取所有Module.class的bean存入Jackson2ObjectMapperBuilder中
//     * 如果 {@link Jackson2ObjectMapperBuilder#findWellKnownModules}的值为true，
//     * 查阅 {@link Jackson2ObjectMapperBuilder#configure(ObjectMapper)} 和
//     * {@link Jackson2ObjectMapperBuilder#registerWellKnownModulesIfAvailable(ObjectMapper)}，
//     * Jackson2ObjectMapperBuilder就会先设置一个并非我们想要的日期时间格式的JavaTimeModule给ObjectMapper
//     * 然后再用Jackson2ObjectMapperBuilder中储存的所有Module.class的bean给ObjectMapper注册module
//     * 所以，如果要当前JavaTimeModule的bean作用于ObjectMapper，有以下两种方式：
//     * 1、把当前JavaTimeModule的bean作为入参，
//     * 在{@link Jackson2ObjectMapperBuilder#configure(ObjectMapper)} 之前
//     * 手动调用 {@link ObjectMapper#registerModule(Module)}
//     * 2、把当前JavaTimeModule的bean作为入参之一，
//     * 调用 {@link Jackson2ObjectMapperBuilder#modules(Module...)}
//     * 将 {@link Jackson2ObjectMapperBuilder#findWellKnownModules} 的值设置成false
//     *
//     * @return jsr310日期时间module
//     */
//    @Bean
//    @Primary
//    public JavaTimeModule jacksonJavaTimeModule(JacksonProperties jacksonProperties) {
//
//        DateTimeFormatter jodaDtFormatter;
//
//        //从application.properties中读取spring.jackson.joda-date-time-format的值
//        String jacksonJodaDtFormat = jacksonProperties.getJodaDateTimeFormat();
//        if (!StringUtils.hasText(jacksonJodaDtFormat) ||
//                DateTimeUtils.DEFAULT_DATE_TIME_PATTERN.equals(jacksonJodaDtFormat)) {
//            jodaDtFormatter = DateTimeUtils.DEFAULT_DATE_TIME_FORMATTER;
//        } else {
//            jodaDtFormatter = DateTimeFormatter.ofPattern(jacksonJodaDtFormat);
//        }
//
//        //支持jsr310日期时间API
//        JavaTimeModule module = new JavaTimeModule();
//
//        module.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeUtils.DEFAULT_DATE_FORMATTER));
//        module.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeUtils.DEFAULT_TIME_FORMATTER));
//        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(jodaDtFormatter));
//
//        module.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeUtils.DEFAULT_DATE_FORMATTER));
//        module.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeUtils.DEFAULT_TIME_FORMATTER));
//        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(jodaDtFormatter));
//
//        //设置Date使用动态格式的字符串反序列化成对象
//        module.addDeserializer(java.util.Date.class, new MsgDateDeserializer());
//
//        return module;
//    }
//}
