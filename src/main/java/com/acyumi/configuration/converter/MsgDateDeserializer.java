package com.acyumi.configuration.converter;

import com.acyumi.utils.DateTimeUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;

import java.io.IOException;
import java.util.Date;

/**
 * 动态格式日期反序列化对象
 * 从内部类抽取出来复用
 *
 * @author Mr.XiHui
 * @date 2019/1/3
 */
public class MsgDateDeserializer extends DateDeserializers.DateDeserializer {

    private static final long serialVersionUID = -8992816734856388583L;

    /**
     * 用友云平台返回的非法日期格式，在这里做适配处理
     */
    private static final String YYC_INVALID_DATE_STRING = "-";

    @Override
    public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        if (p.hasToken(JsonToken.VALUE_STRING)) {
            String str = p.getText().trim();
            if (str.length() == 0 || YYC_INVALID_DATE_STRING.equals(str)) {
                return null;
            }

            Date date = DateTimeUtils.dynamicParseToDate(str);
            if (date != null) {
                return date;
            }
        }
        return super._parseDate(p, ctxt);
    }
}
