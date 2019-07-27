//package com.acyumi.configuration.converter;
//
//import com.acyumi.util.JsonUtils;
//import com.fasterxml.jackson.core.JsonFactory;
//import com.fasterxml.jackson.core.JsonParser;
//import com.fasterxml.jackson.core.JsonToken;
//import com.fasterxml.jackson.databind.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.util.StreamUtils;
//import org.springframework.web.context.request.RequestContextHolder;
//import org.springframework.web.context.request.ServletRequestAttributes;
//
//import javax.servlet.http.HttpServletRequest;
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.util.LinkedHashMap;
//import java.util.Map;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
///**
// * http请求信息对象映射类
// * 重点是重写了{@link ObjectMapper#_readMapAndClose}方法，在抛出映射Json异常时打印请求信息日志以供排查问题
// *
// * @author Mr.XiHui
// * @date 2018/3/21
// *
// * @see MsgInputDecorator
// */
//public class MsgObjectMapper extends ObjectMapper {
//
//    private static final long serialVersionUID = -8423247887084358633L;
//    private static final MsgInputDecorator INPUT_DECORATOR = new MsgInputDecorator();
//    private static final Pattern PATTERN = Pattern.compile("requestId[\"']\\s*:\\s*[\"'](.+)[\"']\\s*,?");
//
//    private final Logger logger = LoggerFactory.getLogger(this.getClass());
//
//    public MsgObjectMapper() {
//        if (_jsonFactory.getInputDecorator() == null) {
//            _jsonFactory.setInputDecorator(INPUT_DECORATOR);
//        }
//    }
//
//    public MsgObjectMapper(JsonFactory jsonFactory) {
//        super(jsonFactory);
//        if (_jsonFactory.getInputDecorator() == null) {
//            _jsonFactory.setInputDecorator(INPUT_DECORATOR);
//        }
//    }
//
//    protected MsgObjectMapper(ObjectMapper src) {
//        super(src);
//        if (_jsonFactory.getInputDecorator() == null) {
//            _jsonFactory.setInputDecorator(INPUT_DECORATOR);
//        }
//    }
//
//    @Override
//    public MsgObjectMapper copy() {
//        _checkInvalidCopy(MsgObjectMapper.class);
//        return new MsgObjectMapper(this);
//    }
//
//    @Override
//    protected Object _readMapAndClose(JsonParser jsonParser, JavaType valueType) throws IOException {
//
//        try {
//            Object result;
//            JsonToken t = _initForReading(jsonParser, valueType);
//            final DeserializationConfig cfg = getDeserializationConfig();
//            final DeserializationContext ctxt = createDeserializationContext(jsonParser, cfg);
//            if (t == JsonToken.VALUE_NULL) {
//                // Ask JsonDeserializer what 'null value' to use:
//                result = _findRootDeserializer(ctxt, valueType).getNullValue(ctxt);
//            } else if (t == JsonToken.END_ARRAY || t == JsonToken.END_OBJECT) {
//                result = null;
//            } else {
//                JsonDeserializer<Object> deser = _findRootDeserializer(ctxt, valueType);
//                if (cfg.useRootWrapping()) {
//                    result = _unwrapAndDeserialize(jsonParser, ctxt, cfg, valueType, deser);
//                } else {
//                    result = deser.deserialize(jsonParser, ctxt);
//                }
//                ctxt.checkUnresolvedObjectId();
//            }
//            if (cfg.isEnabled(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)) {
//                _verifyNoTrailingTokens(jsonParser, ctxt, valueType);
//            }
//
//            //如果没有异常那就是成功转化为JavaType对应的对象了
//            return result;
//        } catch (Exception e) {
//
//            HttpServletRequest request;
//            try {
//                request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
//            } catch (IllegalStateException ise) {
//                //有可能不是在启动servlet容器的情况下使用此类，尝试获取绑定到线程中的request就会报错
//                //这时就不进行下面的逻辑了，直接抛出Json的异常
//                throw e;
//            }
//
//            String url = request.getRequestURL().toString();
//            String remoteAddr = AuthUtils.getIpAddress(request);
//            String httpMethod = request.getMethod();
//
//            Map<String, String> requestMap = new LinkedHashMap<>();
//            requestMap.put("remoteAddr", remoteAddr);
//            requestMap.put("url", url);
//            requestMap.put("httpMethod", httpMethod);
//
//            logger.error("(ｷ｀ﾟДﾟ´)!! : please check request >>> {}", JsonUtils.toJsonStr(requestMap));
//            logger.error("(ｷ｀ﾟДﾟ´)!! : please check requestParamMap >>> {}",
//                    JsonUtils.toJsonStr(request.getParameterMap()));
//
//            //从JsonParser中取出http请求输入流
//            Object inputSource = jsonParser.getInputSource();
//            if (inputSource instanceof ByteArrayInputStream) {
//
//                //如果得到的是经过转化的ByteArrayInputStream，那就记录一下日志
//                ByteArrayInputStream bais = (ByteArrayInputStream) inputSource;
//                try {
//                    //重置ByteArrayInputStream的读取位置到最后一次mark的位置
//                    //由于前面没有调用过mark(readAheadLimit)方法，所以这里直接重置到初始位置
//                    bais.reset();
//
//                    String requestMsgBody = StreamUtils.copyToString(bais, StandardCharsets.UTF_8);
//                    Matcher matcher = PATTERN.matcher(requestMsgBody);
//                    //取requestId
//                    if (matcher.find()) {
//                        request.setAttribute("requestId", matcher.group(1));
//                    }
//
//                    logger.error("ヽ(#`Д´)ﾉ┌┛〃 or check requestMsgBody >>> {}", requestMsgBody);
//                    logger.error("(´థ౪థ)σ      for javaType >>> {}", valueType);
//                } catch (Exception ex) {/*ignore*/}
//
//            }
//
//            //日志记录完后异常还是要照旧抛出去，待框架后面处理
//            throw e;
//
//        } finally {
//            //因为要再次利用jsonParser中的输入流，
//            //所以不使用try(Closeable)代码块来自动调用jsonParser.close()
//            jsonParser.close();//此操作会使jsonParser = null;
//        }
//    }
//}
