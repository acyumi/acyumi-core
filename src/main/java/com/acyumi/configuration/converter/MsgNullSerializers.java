//package com.acyumi.configuration.converter;
//
//import com.fasterxml.jackson.core.JsonGenerator;
//import com.fasterxml.jackson.databind.JsonSerializer;
//import com.fasterxml.jackson.databind.SerializerProvider;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.io.IOException;
//
///**
// * 各种类型的null值序列化器初始化配置类
// *
// * @author Mr.XiHui
// * @date 2019/1/7
// */
//@Configuration
//public class MsgNullSerializers {
//
//    @Bean
//    public JsonSerializer<Object> nullJsonStringSerializer() {
//        return new JsonSerializer<Object>() {
//            @Override
//            public void serialize(Object value, JsonGenerator gen,
//                                  SerializerProvider serializers) throws IOException {
//                gen.writeString("");
//            }
//        };
//    }
//
//    @Bean
//    public JsonSerializer<Object> nullJsonNumberSerializer() {
//        return new JsonSerializer<Object>() {
//            @Override
//            public void serialize(Object value, JsonGenerator gen,
//                                  SerializerProvider serializers) throws IOException {
//                gen.writeNumber(0);
//            }
//        };
//    }
//
//    @Bean
//    public JsonSerializer<Object> nullJsonBooleanSerializer() {
//        return new JsonSerializer<Object>() {
//            @Override
//            public void serialize(Object value, JsonGenerator gen,
//                                  SerializerProvider serializers) throws IOException {
//                gen.writeBoolean(false);
//            }
//        };
//    }
//
//    @Bean
//    public JsonSerializer<Object> nullJsonArraySerializer() {
//        return new JsonSerializer<Object>() {
//            @Override
//            public void serialize(Object value, JsonGenerator gen,
//                                  SerializerProvider serializers) throws IOException {
//                gen.writeStartArray();
//                gen.writeEndArray();
//            }
//        };
//    }
//}
