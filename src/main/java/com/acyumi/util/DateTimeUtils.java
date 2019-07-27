package com.acyumi.util;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalQuery;
import java.util.Arrays;
import java.util.Date;

/**
 * 日期时间工具类
 * 使用java8的时间api封装一些操作时间的简单工具类
 * <p>
 * Instant：时间戳
 * LocalDate：不带时间的日期
 * LocalTime：不带日期的时间
 * LocalDateTime：含时间与日期，不过没有带时区的偏移量
 * ZonedDateTime：带时区的完整时间
 *
 * @author Mr.XiHui
 * @since 2017/11/2
 */
public abstract class DateTimeUtils {

    /*** 本地时区ID ***/
    public static final ZoneId LOCAL_ZONE_ID = ZoneId.systemDefault();

    /*** 缺省的日期表达式 ***/
    public static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";

    /*** 缺省的日期表达式（中文年月日） ***/
    public static final String DEFAULT_DATE_PATTERN_ZH = "yyyy年MM月dd日";

    /*** 缺省的时间表达式 ***/
    public static final String DEFAULT_TIME_PATTERN = "HH:mm:ss";

    /*** 缺省的时间表达式（中文时分秒） ***/
    public static final String DEFAULT_TIME_PATTERN_ZH = "HH时mm分ss秒";

    /*** 缺省的日期时间表达式(yyyy-MM-dd HH:mm:ss) ***/
    public static final String DEFAULT_DATE_TIME_PATTERN = DEFAULT_DATE_PATTERN + ' ' + DEFAULT_TIME_PATTERN;

    /*** 缺省的时间戳表达式 ***/
    public static final String DEFAULT_TIMESTAMP_PATTERN = "yyyyMMddHHmmss";

    /*** 缺省的动态时间表达式 ***/
    public static final String[] DEFAULT_DYNAMIC_PATTERNS = {
            DEFAULT_DATE_TIME_PATTERN,
            DEFAULT_DATE_PATTERN,
            DEFAULT_TIMESTAMP_PATTERN,
            DEFAULT_DATE_PATTERN_ZH,
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd'T'HH:mm:ss.sss",
            "yyyy-MM-dd'T'HH:mm:ss.sssZ",
            DEFAULT_TIME_PATTERN,
            DEFAULT_TIME_PATTERN_ZH
    };

    /* 因为DateTimeFormatter是线程安全的，所以这里可以提供初始化好的DateTimeFormatter */

    /*** 缺省的日期时间格式器(yyyy-MM-dd) ***/
    public static final DateTimeFormatter DEFAULT_DATE_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_DATE_PATTERN);

    /*** 缺省的时间格式器(HH:mm:ss) ***/
    public static final DateTimeFormatter DEFAULT_TIME_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_TIME_PATTERN);

    /*** 缺省的日期时间格式器(yyyy-MM-dd HH:mm:ss) ***/
    public static final DateTimeFormatter DEFAULT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_DATE_TIME_PATTERN);

    /**
     * 根据pattern生成新的Date格式化类
     * 因为SimpleDateFormat不是结程安全的，所以这里不提供初始化好的SimpleDateFormat
     *
     * @param pattern 格式字符串
     * @return SimpleDateFormat
     */
    public static SimpleDateFormat getDateFormat(String pattern) {
        return new SimpleDateFormat(pattern);
    }

    /**
     * 将LocalDateTime格式化成指定格式的字符串
     *
     * @param localDateTime 本时区日期时间
     * @param pattern       格式字符串 (一定要包含日期段和时间段，
     *                      如果少了时间段如"yyyy-MM-dd"则报错)
     * @return String 格式化后的时间字符串
     */
    public static String format(LocalDateTime localDateTime, String pattern) {
        if (localDateTime == null) {
            return null;
        }
        if (DEFAULT_DATE_TIME_PATTERN.equals(pattern)) {
            return DEFAULT_DATE_TIME_FORMATTER.format(localDateTime);
        }
        return DateTimeFormatter.ofPattern(pattern).format(localDateTime);
    }

    public static String format(LocalDate localDate, String pattern) {
        if (localDate == null) {
            return null;
        }
        if (DEFAULT_DATE_PATTERN.equals(pattern)) {
            return localDate.toString();
            //return DEFAULT_DATE_FORMATTER.format(localDate);
        }
        return DateTimeFormatter.ofPattern(pattern).format(localDate);
    }

    public static String format(Date date, String pattern) {
        if (date == null) {
            return null;
        }
        return getDateFormat(pattern).format(date);
    }

    /**
     * 将LocalDate格式化成yyyy-MM-dd格式的字符串
     *
     * @param localDate 本时区日期
     * @return String yyyy-MM-dd格式的日期字符串
     */
    public static String defaultDateFormat(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return localDate.toString();
        //return DEFAULT_DATE_FORMATTER.format(localDate);
    }

    /**
     * 将Date格式化成yyyy-MM-dd格式的字符串
     *
     * @param date java.util.Date
     * @return String yyyy-MM-dd格式的日期字符串
     */
    public static String defaultDateFormat(Date date) {
        if (date == null) {
            return null;
        }
        return getDateFormat(DEFAULT_DATE_PATTERN).format(date);
    }

    /**
     * 将LocalDateTime格式化成yyyy-MM-dd HH:mm:ss格式的字符串
     *
     * @param localDateTime 本时区日期
     * @return String yyyy-MM-dd HH:mm:ss格式的日期字符串
     */
    public static String defaultDateTimeFormat(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return DEFAULT_DATE_TIME_FORMATTER.format(localDateTime);
    }

    /**
     * 将Date格式化成yyyy-MM-dd格式的字符串
     *
     * @param date java.util.Date
     * @return String yyyy-MM-dd HH:mm:ss格式的日期字符串
     */
    public static String defaultDateTimeFormat(Date date) {
        if (date == null) {
            return null;
        }
        return getDateFormat(DEFAULT_DATE_TIME_PATTERN).format(date);
    }

    /**
     * 将指定格式的时间字符串解析成LocalDateTime对象
     * 如果timeStr不符合pattern格式或不是日期+时间的完整内容则报错!!!
     *
     * @param dateTimeStr 日期时间字符串
     * @param pattern     格式字符串
     * @return LocalDateTime 本时区日期时间
     * @see DateTimeFormatter#parse(CharSequence, TemporalQuery)
     */
    public static LocalDateTime parseToLocalDateTime(String dateTimeStr, String pattern) {
        return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 将yyyy-MM-dd HH:mm:ss格式的时间字符串解析成LocalDateTime对象
     * 如果timeStr不符合yyyy-MM-dd HH:mm:ss则报错!!!
     *
     * @param timeStr yyyy-MM-dd HH:mm:ss格式的时间字符串
     * @return java.util.Date
     */
    public static LocalDateTime parseToLocalDateTime(String timeStr) {
        return LocalDateTime.parse(timeStr, DEFAULT_DATE_TIME_FORMATTER);
    }

    /**
     * 将指定格式的时间字符串解析成LocalDateTime对象
     *
     * @param dateStr 日期字符串
     * @param pattern 格式字符串
     * @return LocalDate 本时区日期
     * @see DateTimeFormatter#parse(CharSequence, TemporalQuery)
     */
    public static LocalDate parseToLocalDate(String dateStr, String pattern) {
        return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
    }

    public static LocalDate parseToLocalDate(String timeStr) {
        return LocalDate.parse(timeStr, DEFAULT_DATE_FORMATTER);
    }

    /**
     * 将指定格式的时间字符串解析成LocalDateTime对象
     *
     * @param timeStr 时间字符串
     * @param pattern 格式字符串
     * @return LocalTime 本时区时间
     * @see DateTimeFormatter#parse(CharSequence, TemporalQuery)
     */
    public static LocalTime parseToLocalTime(String timeStr, String pattern) {
        return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 将指定格式的时间字符串解析成java.util.Date对象
     * 如果timeStr不符合pattern格式则返回null
     *
     * @param timeStr 时间字符串
     * @param pattern 格式字符串
     * @return java.util.Date
     */
    public static Date parseToDate(String timeStr, String pattern) {
        return new SimpleDateFormat(pattern).parse(timeStr, new ParsePosition(0));
    }

    /**
     * 将yyyy-MM-dd HH:mm:ss格式的时间字符串解析成java.util.Date对象
     * 如果timeStr不符合yyyy-MM-dd HH:mm:ss则返回null
     *
     * @param timeStr 时间字符串
     * @return java.util.Date
     */
    public static Date parseToDate(String timeStr) {
        return getDateFormat(DEFAULT_DATE_TIME_PATTERN).parse(timeStr, new ParsePosition(0));
    }

    /**
     * 接收动态格式的时间字符串，尝试解析成java.util.Date对象
     * 解析失败则返回null
     *
     * @param timeStr 时间字符串
     * @param specialPatterns 除方法体中指定的时间格式外，可自行增加一些特殊的时间格式
     * @return java.util.Date
     */
    public static Date dynamicParseToDate(String timeStr, String... specialPatterns) {

        if (timeStr == null || ParameterUtils.isEmpty(timeStr = timeStr.trim())) {
            return null;
        }

        //合并defaultPatterns和specialPatterns成patternArray
        String[] patternArray;
        if (specialPatterns == null || specialPatterns.length == 0) {
            patternArray = DEFAULT_DYNAMIC_PATTERNS;
        } else {
            patternArray = Arrays.copyOf(DEFAULT_DYNAMIC_PATTERNS, DEFAULT_DYNAMIC_PATTERNS.length + specialPatterns.length);
            System.arraycopy(specialPatterns, 0, patternArray, DEFAULT_DYNAMIC_PATTERNS.length, specialPatterns.length);
        }

        for (int i = 0; i < patternArray.length; i++) {
            try {
                //尝试循环尝试使用多种格式解析
                return getDateFormat(patternArray[i]).parse(timeStr);
            } catch (ParseException e) {
                //ignore
            }
        }
        return null;
    }

    /**
     * 将LocalDateTime转成新纪元时间的毫秒值
     * 从1970-01-01 00:00:00至今的毫秒值
     * 如果传进来的是now()，那么得到的毫秒值
     * 与System.currentTimeMillis()相同
     * 与new Date().getTime()相同
     *
     * @param localDateTime 本时区日期时间
     * @return long 毫秒值
     */
    public static long toEpochMilli(LocalDateTime localDateTime) {
        ZonedDateTime zonedDateTime = localDateTime.atZone(LOCAL_ZONE_ID);
        return toEpochMilli(zonedDateTime);
    }

    /**
     * 将ZonedDateTime转成新纪元时间的毫秒值
     * 从1970-01-01 00:00:00至今的毫秒值
     * 如果传进来的是now()，那么得到的毫秒值
     * 与System.currentTimeMillis()相同
     * 与new Date().getTime()相同
     *
     * @param zonedDateTime 带时区的完整时间
     * @return long 毫秒值
     */
    public static long toEpochMilli(ZonedDateTime zonedDateTime) {
        Instant instant = zonedDateTime.toInstant();
        return instant.toEpochMilli();
    }

    /**
     * 将LocalDateTime对象转换成Date对象
     *
     * @param localDateTime 本地日期时间
     * @return java.util.Date
     */
    public static Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        ZonedDateTime zdt = localDateTime.atZone(LOCAL_ZONE_ID);
        Instant instant = zdt.toInstant();
        return Date.from(instant);
        //return Timestamp.valueOf(localDateTime);
    }

    /**
     * 将LocalDate对象转换成Date对象
     *
     * @param localDate 本地日期
     * @return java.util.Date
     */
    public static Date toDate(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        ZonedDateTime zdt = localDate.atStartOfDay(LOCAL_ZONE_ID);
        Instant instant = zdt.toInstant();
        return Date.from(instant);
        //return java.sql.Date.valueOf(localDate);
    }

    /**
     * 将LocalDate对象转换成Date对象
     *
     * @param date java.util.Date
     * @return LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        if (date instanceof Timestamp) {
            return ((Timestamp) date).toLocalDateTime();
        }
        return LocalDateTime.ofInstant(date.toInstant(), LOCAL_ZONE_ID);
    }

    /**
     * 将新纪元时间的毫秒long值转换成LocalDateTime对象
     *
     * @param epochMilli long
     * @return LocalDateTime
     */
    public static LocalDateTime toLocalDateTime(long epochMilli) {
        Instant instant = Instant.ofEpochMilli(epochMilli);
        return instant.atZone(LOCAL_ZONE_ID).toLocalDateTime();
        //return LocalDateTime.ofInstant(instant, LOCAL_ZONE_ID);
    }

    /**
     * 将LocalDate对象转换成Date对象
     *
     * @param date java.util.Date
     * @return LocalDate
     */
    public static LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        }
        return date.toInstant().atZone(LOCAL_ZONE_ID).toLocalDate();
    }

    /**
     * 将新纪元时间的毫秒long值转换成LocalDate对象
     *
     * @param epochMilli long
     * @return LocalDate
     */
    public static LocalDate toLocalDate(long epochMilli) {
        Instant instant = Instant.ofEpochMilli(epochMilli);
        return instant.atZone(LOCAL_ZONE_ID).toLocalDate();
    }
}
