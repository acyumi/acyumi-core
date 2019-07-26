package com.acyumi.utils;

import com.acyumi.cast.Castor;
import com.acyumi.helper.TransMap;
import com.acyumi.reflect.Reflector;
import com.acyumi.reflect.reflectasm.MethodAccessor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 标准POJO对象/Map&lt;String,Object&gt;之间的转化工具类. <br>
 *
 * <span style='color:yellow'>当源对象的值类型与目标对象setter的入参类型不匹配
 * (包括List/Map中元素类型不匹配)时，将尝试进行转换，转换失败则提示异常</span>
 * <p>
 * 如果set到目标pojo成员变量的值是List/Map等含有泛型元素的类型，<br>
 * 且源getter返回值List/Map等的元素与目标setter入参List/Map等的元素不匹配时，反射允许set进去，不出异常，<br>
 * 不过后果就是当你transform完成并从目标pojo对象中取List/Map等成员变量出来再取其元素时，极有可能会出现类型转换异常，<br>
 * 原因就是此时目标pojo的List/Map成员变量中引用的元素类型已经跟声明的不一样了<br>
 * <p>
 * 这里涉及到一些泛型类的操作，所以只是部分使用了reflectasm进行反射处理<br>
 * <p>
 * 类关键词--&gt;“标准POJO”、“Map&lt;String,Object&gt;”、“可转化/传递”
 *
 * @author Mr.XiHui
 * @date 2017/9/9 17:05
 * @see org.springframework.beans.BeanUtils
 * @see org.springframework.core.GenericTypeResolver
 * @see com.google.common.reflect.TypeToken
 * @see org.springframework.asm.MethodVisitor
 * <div style='display: none'>@see org.objectweb.asm.MethodVisitor</div>
 */
public abstract class TransformUtils {

    /**
     * 将源对象的成员变量值转递给目标对象<br>
     * 目标对象必须包含源对象中的所有成员变量/键<br>
     * <span style='color:yellow'>如果源对象的值类型与目标对象setter的入参类型不匹配时，将尝试进行转换，转换失败则提示异常</span>
     *
     * @param source    源对象 (POJO或Map&lt;String,?&gt;)
     * @param pojoClass pojo的Class
     * @param <T>       目标对象的类型 (POJO)
     * @return T 实例化出来且带有从源对象获取到的所有数据的目标对象
     */
    public static <T> T transform(Object source, Class<T> pojoClass) {
        T target = Reflector.newTarget(pojoClass);
        transformSpecify(source, target, false, (String[]) null);
        return target;
    }

    public static void transform(Object source, Object pojo) {
        transformSpecify(source, pojo, false, (String[]) null);
    }

    /**
     * 指定源对象的成员变量值以转递给目标对象<br>
     * 目标对象中必须有与源对象相同的这些被指定的成员变量/键<br>
     * <span style='color:yellow'>如果源对象的值类型与目标对象setter的入参类型不匹配时，将尝试进行转换，转换失败则提示异常</span>
     *
     * @param source           源对象 (POJO或Map&lt;String,Object&gt;)
     * @param pojoClass        目标Class
     * @param onlyNotNull      是否只传递（keysOrFieldNames对应值）非null的值
     * @param keysOrFieldNames 要传递的key/成员变量名列表，如果不指定，则表示所有key/成员变量名
     * @param <T>              目标对象的类型 (POJO)
     * @return T 实例化出来且带有源对象指定部分数据的目标对象
     */
    public static <T> T transformPart(Object source, Class<T> pojoClass, boolean onlyNotNull,
                                      String... keysOrFieldNames) {
        T target = Reflector.newTarget(pojoClass);
        transformSpecify(source, target, onlyNotNull, keysOrFieldNames);
        return target;
    }

    public static void transformPart(Object source, Object pojo, boolean onlyNotNull, String... keysOrFieldNames) {
        transformSpecify(source, pojo, onlyNotNull, keysOrFieldNames);
    }

    /**
     * 将pojo所有可取的数据转到map中. <br>
     * 使用时指定目标Map的value类型
     *
     * @param source     源对象(pojo/map)
     * @param mapClass   目标Map的类型
     * @param keyClass   目标Map的key类型
     * @param valueClass 目标Map的value类型
     * @param <K>        键类型
     * @param <V>        值类型
     * @return Map&lt;K, V&gt; 如果是Map&lt;String, Object&gt;，则其实例为TransMap
     */
    public static <K, V> Map<K, V> transformToMap(Object source, Class<?> mapClass,
                                                  Class<K> keyClass, Class<V> valueClass) {
        return transformToMapSpecify(source, mapClass, keyClass, valueClass, false, (Object[]) null);
    }

    public static TransMap transformToTransMap(Object source) {
        return (TransMap) transformToMapSpecify(source, TransMap.class,
                String.class, Object.class, false, (Object[]) null);
    }

    public static Map<byte[], byte[]> transformToBinaryMap(Object source,
                                                           Converter<String, byte[]> stringSerializer,
                                                           Converter<Object, byte[]> objectSerializer) {
        if (source instanceof Map) {
            return transformMapToBinaryMap((Map<?, ?>) source, stringSerializer, objectSerializer, (Object[]) null);
        }
        //如果source不是map，则将其当成pojo处理，提示异常请自检
        return transformPojoToBinaryMap(source, stringSerializer, objectSerializer, (String[]) null);
    }

    /**
     * 指定变量名列表将pojo的部分数据转到map中
     * 使用时指定目标Map的value类型
     *
     * @param source      源对象(pojo/map)
     * @param mapClass    目标Map的类型
     * @param keyClass    目标Map的key类型
     * @param valueClass  目标Map的value类型
     * @param onlyNotNull 是否只传递（fieldNames对应值）非null的值
     * @param fieldNames  要传递的成员变量名列表，如果不指定，则表示所有成员变量名
     * @param <K>         键类型
     * @param <V>         值类型
     * @return Map&lt;String, V&gt; 如果是Map&lt;String, Object&gt;，则其实例为TransMap
     */
    public static <K, V> Map<K, V> transformPartToMap(Object source, Class<?> mapClass,
                                                      Class<K> keyClass, Class<V> valueClass,
                                                      boolean onlyNotNull, String... fieldNames) {
        return transformToMapSpecify(source, mapClass, keyClass, valueClass, onlyNotNull, (Object[]) fieldNames);
    }

    public static TransMap transformPartToTransMap(Object source, boolean onlyNotNull, String... fieldNames) {
        return (TransMap) transformToMapSpecify(source, TransMap.class, String.class, Object.class,
                onlyNotNull, (Object[]) fieldNames);
    }

    public static Map<byte[], byte[]> transformPartToBinaryMap(Object source,
                                                               Converter<String, byte[]> stringSerializer,
                                                               Converter<Object, byte[]> objectSerializer,
                                                               Object... keysOrFieldNames) {
        if (source instanceof Map) {
            return transformMapToBinaryMap((Map<?, ?>) source, stringSerializer, objectSerializer, keysOrFieldNames);
        }
        //如果source不是map，则将其当成pojo处理，提示异常请自检
        return transformPojoToBinaryMap(source, stringSerializer, objectSerializer, (String[]) keysOrFieldNames);
    }

    /**
     * list集合之间的传递
     *
     * @param sourceList    源List集合(元素类型(POJO或Map))
     * @param listElemClass 目标List集合中的元素Class
     * @param <T>           目标List集合中的元素类型(POJO或Map或TransMap)
     * @param onlyNotNull   是否只传递非空的值
     * @return List&lt;T&gt; 目标List集合
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> transformList(List<?> sourceList, Class<T> listElemClass, boolean onlyNotNull) {
        List<T> targetList = new ArrayList<>();
        if (ParameterUtils.isEmpty(sourceList)) {
            return targetList;
        }
        if (Map.class.isAssignableFrom(listElemClass)) {
            ParameterUtils.iterateObj(sourceList, (source, index) -> {
                targetList.add((T) transformToMapSpecify(source, listElemClass,
                        String.class, Object.class, onlyNotNull, (Object[]) null));
            });
        } else {
            ParameterUtils.iterateObj(sourceList, (source, index) -> {
                T target = Reflector.newTarget(listElemClass);
                transformSpecify(source, target, onlyNotNull, (String[]) null);
                targetList.add(target);
            });
        }
        return targetList;
        //return sourceList.stream().map(source -> {
        //    if (Map.class.isAssignableFrom(listElemClass)) {
        //        return (T) transformToMapSpecify(source, listElemClass,
        //                String.class, Object.class, onlyNotNull, (Object[]) null);
        //    }
        //    T target = Reflector.newTarget(listElemClass);
        //    transformSpecify(source, target, onlyNotNull, (String[]) null);
        //    return target;
        //}).collect(Collectors.toList());
    }

    //----------------------------------------------------------------------------------------------------
    //*******************************************公私分隔线***********************************************/
    //----------------------------------------------------------------------------------------------------

    private static void transformSpecify(Object source, Object pojo, boolean onlyNotNull, String... fieldNames) {
        if (source instanceof Map) {
            transformMapToPojo((Map<?, ?>) source, pojo, onlyNotNull, fieldNames);
        } else {
            transformPojoToPojo(source, pojo, onlyNotNull, fieldNames);
        }
    }

    private static <K, V> Map<K, V> transformToMapSpecify(Object source, Class<?> mapClass,
                                                          Class<K> keyClass, Class<V> valueClass,
                                                          boolean onlyNotNull, Object... keysOrFieldNames) {
        if (source == null) {
            return null;
        }
        Map<K, V> targetMap = initTargetMap(mapClass, keyClass, valueClass);
        if (source instanceof Map) {
            transformMapToMap((Map<?, ?>) source, targetMap, keyClass, valueClass, onlyNotNull, keysOrFieldNames);
        } else {
            //如果source不是map，则将其当成pojo处理，提示异常请自检
            if (!Reflector.isAssignable(keyClass, String.class)) {
                throw new IllegalArgumentException(String.format("(类型:%s | toString值:%s)无法转化成(Map<%s,%s>)",
                        source.getClass(), source, keyClass, valueClass));
            }
            transformPojoToMap(source, targetMap, keyClass, valueClass, onlyNotNull, (String[]) keysOrFieldNames);
        }
        return targetMap;
    }

    @SuppressWarnings("unchecked")
    private static <K, V> void transformPojoToMap(Object source, Map<K, V> targetMap,
                                                  Class<K> keyClass, Class<V> valueClass,
                                                  boolean onlyNotNull, String... fieldNames) {
        if (source == null) {
            return;
        }
        Assert.isTrue(!(source instanceof Map), "holy sh...请不要传入Map类型的source");
        Assert.isTrue(!(source instanceof Iterable), "source属于Iterable,类型不匹配,请传入POJO");
        Assert.isTrue(!(source instanceof Object[]), "source属于Object[],类型不匹配,请传入POJO");
        MethodAccessor sourceAccessor = Reflector.getMethodAccessor(source.getClass());
        if (fieldNames == null || fieldNames.length == 0) {
            fieldNames = sourceAccessor.getFieldNames();
        }
        for (int i = 0; i < fieldNames.length; i++) {
            String fieldName = fieldNames[i];
            if (ParameterUtils.isEmpty(fieldName)) {
                continue;
            }
            Object value = sourceAccessor.getFieldValue(source, fieldName);
            if (onlyNotNull && value == null) {
                continue;
            }
            K key;
            if (Reflector.isInstance(keyClass, fieldName)) {
                key = (K) fieldName;
            } else {
                key = (K) Castor.castClass2Class(fieldName, String.class, keyClass);
            }
            if (value == null || Reflector.isInstance(valueClass, value)) {
                targetMap.put(key, (V) value);
            } else {
                targetMap.put(key, (V) Castor.castClass2Class(value, value.getClass(), valueClass));
            }
        }
    }

    private static void transformPojoToPojo(Object source, Object pojo, boolean onlyNotNull, String... fieldNames) {
        if (source == null) {
            return;
        }
        Assert.isTrue(!(source instanceof Iterable), "source属于Iterable,类型不匹配,请传入POJO");
        Assert.isTrue(!(source instanceof Object[]), "source属于Object[],类型不匹配,请传入POJO");
        Assert.isTrue(!(pojo instanceof Map), "target属于Map,类型不匹配,请传入POJO");
        Assert.isTrue(!(pojo instanceof Iterable), "target属于Iterable,类型不匹配,请传入POJO");
        Assert.isTrue(!(pojo instanceof Object[]), "target属于Object[],类型不匹配,请传入POJO");
        Class<?> sourceClass = source.getClass();
        Class<?> pojoClass = pojo.getClass();
        MethodAccessor sourceAccessor = Reflector.getMethodAccessor(sourceClass);
        MethodAccessor targetAccessor = sourceClass == pojoClass ?
                sourceAccessor : Reflector.getMethodAccessor(pojoClass);
        if (fieldNames == null || fieldNames.length == 0) {
            fieldNames = sourceAccessor.getFieldNames();
        }
        for (int i = 0; i < fieldNames.length; i++) {
            String fieldName = fieldNames[i];
            if (ParameterUtils.isEmpty(fieldName)) {
                continue;
            }
            Object value = sourceAccessor.getFieldValue(source, fieldName);
            if (onlyNotNull && value == null) {
                continue;
            }
            transValToTargetPojo(value, pojo, fieldName, sourceAccessor, targetAccessor);
        }
    }

    private static void transformMapToPojo(Map<?, ?> map, Object pojo, boolean onlyNotNull, String... keys) {
        if (ParameterUtils.isEmpty(map)) {
            return;
        }
        Assert.isTrue(!(pojo instanceof Map), "target属于Map,类型不匹配,请传入POJO");
        Assert.isTrue(!(pojo instanceof Iterable), "target属于Iterable,类型不匹配,请传入POJO");
        Assert.isTrue(!(pojo instanceof Object[]), "target属于Object[],类型不匹配,请传入POJO");
        MethodAccessor targetAccessor = Reflector.getMethodAccessor(pojo.getClass());
        if (keys == null || keys.length == 0) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object value = entry.getValue();
                if (onlyNotNull && value == null) {
                    continue;
                }
                Object key = entry.getKey();
                if (key == null) {
                    continue;
                }
                String strKey = key.toString();
                transValToTargetPojo(value, pojo, strKey, null, targetAccessor);
            }
        } else {
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];
                if (ParameterUtils.isEmpty(key)) {
                    continue;
                }
                Object value = map.get(key);
                if (onlyNotNull && value == null) {
                    continue;
                }
                transValToTargetPojo(value, pojo, key, null, targetAccessor);
            }
        }
        /*//另一种写法
        Object obj = (keys == null || keys.length == 0) ? map : keys;
        ParameterUtils.iterateObj(obj, (elem, index) -> {
            String key;
            Object value;
            if (elem instanceof Map.Entry) {
                value = ((Map.Entry) elem).getValue();
                if (onlyNotNull && value == null) {
                    return;
                }
                Object objKey = ((Map.Entry) elem).getKey();
                if (objKey == null) {
                    return;
                }
                key = objKey.toString();
            } else {
                key = (String) elem;
                value = map.get(key);
                if (onlyNotNull && value == null) {
                    return;
                }
            }
            transValToTargetPojo(value, pojo, key, null, targetAccessor);
        });*/
    }

    @SuppressWarnings("unchecked")
    private static <K, V> void transformMapToMap(Map<?, ?> map, Map<K, V> targetMap,
                                                 Class<K> keyClass, Class<V> valueClass,
                                                 boolean onlyNotNull, Object... keys) {
        if (targetMap == null) {
            return;
        }
        if (!ParameterUtils.isEmpty(map)) {
            if (keys == null || keys.length == 0) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    Object value = entry.getValue();
                    if (onlyNotNull && value == null) {
                        continue;
                    }
                    Object key = entry.getKey();
                    if (key == null) {
                        continue;
                    }
                    if (!Reflector.isInstance(keyClass, key)) {
                        key = Castor.castClass2Class(key, key.getClass(), keyClass);
                    }
                    if (!Reflector.isInstance(valueClass, value)) {
                        value = Castor.castClass2Class(value, value.getClass(), valueClass);
                    }
                    targetMap.put((K) key, (V) value);
                }
            } else {
                for (int i = 0; i < keys.length; i++) {
                    Object key = keys[i];
                    if (key == null) {
                        continue;
                    }
                    Object value = map.get(key);
                    if (onlyNotNull && value == null) {
                        continue;
                    }
                    if (!Reflector.isInstance(keyClass, key)) {
                        key = Castor.castClass2Class(key, key.getClass(), keyClass);
                    }
                    if (!Reflector.isInstance(valueClass, value)) {
                        value = Castor.castClass2Class(value, value.getClass(), valueClass);
                    }
                    targetMap.put((K) key, (V) value);
                }
            }
        }
    }

    private static Map<byte[], byte[]> transformPojoToBinaryMap(Object source,
                                                                Converter<String, byte[]> stringSerializer,
                                                                Converter<Object, byte[]> objectSerializer,
                                                                String... fieldNames) {
        Assert.notNull(stringSerializer, "key序列化转换器不能为null");
        Assert.notNull(objectSerializer, "value序列化转换器不能为null");
        Map<byte[], byte[]> binaryMap = new LinkedHashMap<>();
        if (source == null) {
            return binaryMap;
        }
        Assert.isTrue(!(source instanceof Map), "holy sh...请不要传入Map类型的source");
        Assert.isTrue(!(source instanceof Iterable), "source属于Iterable,类型不匹配,请传入POJO");
        Assert.isTrue(!(source instanceof Object[]), "source属于Object[],类型不匹配,请传入POJO");
        MethodAccessor sourceAccessor = Reflector.getMethodAccessor(source.getClass());
        if (fieldNames == null || fieldNames.length == 0) {
            fieldNames = sourceAccessor.getFieldNames();
        }
        for (int i = 0; i < fieldNames.length; i++) {
            String fieldName = fieldNames[i];
            if (ParameterUtils.isEmpty(fieldName)) {
                continue;
            }
            //成员变量名,也就是Map的key
            Object value = sourceAccessor.getFieldValue(source, fieldName);
            putToBinaryMap(fieldName, value, binaryMap, stringSerializer, objectSerializer);
        }
        return binaryMap;
    }

    private static Map<byte[], byte[]> transformMapToBinaryMap(Map<?, ?> map,
                                                               Converter<String, byte[]> stringSerializer,
                                                               Converter<Object, byte[]> objectSerializer,
                                                               Object... keys) {
        Assert.notNull(stringSerializer, "key序列化转换器不能为null");
        Assert.notNull(objectSerializer, "value序列化转换器不能为null");
        Map<byte[], byte[]> binaryMap = new LinkedHashMap<>();
        if (!ParameterUtils.isEmpty(map)) {
            if (keys == null) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    putToBinaryMap(entry.getKey(), entry.getValue(), binaryMap, stringSerializer, objectSerializer);
                }
            } else {
                for (int i = 0; i < keys.length; i++) {
                    Object key = keys[i];
                    putToBinaryMap(key, map.get(key), binaryMap, stringSerializer, objectSerializer);
                }
            }
        }
        return binaryMap;
    }

    private static void putToBinaryMap(Object keyOrFieldName, Object value,
                                       Map<byte[], byte[]> binaryMap,
                                       Converter<String, byte[]> stringSerializer,
                                       Converter<Object, byte[]> objectSerializer) {
        try {
            byte[] keyBytes = convert(keyOrFieldName, stringSerializer, objectSerializer);
            byte[] valueBytes = convert(value, stringSerializer, objectSerializer);
            binaryMap.put(keyBytes, valueBytes);
        } catch (Exception ex) {
            //throw new SerializationException("Cannot serialize", ex);
            throw new RuntimeException("Cannot serialize", ex);
        }
    }

    private static byte[] convert(Object obj, Converter<String, byte[]> stringSerializer,
                                  Converter<Object, byte[]> objectSerializer) {
        if (obj == null) {
            return Reflector.EMPTY_BYTES;
        } else if (obj instanceof String) {
            return stringSerializer.convert((String) obj);
        } else {
            return objectSerializer.convert(obj);
        }
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> initTargetMap(Class<?> mapClass, Class<K> keyClass, Class<V> valueClass) {
        Map<K, V> targetMap = null;
        if (mapClass != null) {
            Assert.isTrue(Reflector.isAssignable(Map.class, mapClass), "mapClass非Map类型");
            try {
                targetMap = Reflector.newTarget(mapClass);
            } catch (Exception e) {/*ignore*/}
        }
        if (targetMap == null) {
            if (Reflector.isAssignable(keyClass, String.class) && valueClass == Object.class) {
                targetMap = (Map<K, V>) new TransMap();
            } else {
                targetMap = new LinkedHashMap<>();
            }
        }
        return targetMap;
    }

    /**
     * 给pojo目标对象实例对应keyOrFieldName的单个成员变量设值，
     * 如果类型不匹配，将尝试进行转换，转换失败则提示异常
     *
     * @param value          从源对象取得的对应keyOrFieldName的值
     * @param targetPojo     pojo目标对象实例
     * @param keyOrFieldName 键或成员变量
     * @param targetAccessor pojo目标对象的MethodAccessor
     * @param <T>            pojo目标对象类型
     */
    private static <T> void transValToTargetPojo(Object value, T targetPojo, String keyOrFieldName,
                                                 MethodAccessor sourceAccessor,
                                                 MethodAccessor targetAccessor) {
        Integer setterIndex = targetAccessor.setterIndex(keyOrFieldName);
        if (setterIndex == null) {
            return;//找不到setter方法则跳过
        }
        if (value == null) {
            targetAccessor.invoke(targetPojo, setterIndex, new Object[]{null});
            return;
        }

        Type getterGenericReturnType = null;
        if (sourceAccessor != null) {
            Integer getterIndex = sourceAccessor.getterIndex(keyOrFieldName);
            if (getterIndex != null) {
                getterGenericReturnType = sourceAccessor.getGenericReturnTypes()[getterIndex];
            }
        }
        value = trans2SetterParameter(keyOrFieldName, value, getterGenericReturnType,
                targetAccessor.getGenericParameterTypes()[setterIndex][0]);

        try {
            targetAccessor.invoke(targetPojo, setterIndex, value);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("方法(%s.%s(%s))的入参(%s)不匹配",
                            value.getClass().getName(),
                            targetAccessor.getMethodNames()[setterIndex],
                            StringUtils.arrayToDelimitedString(targetAccessor.getParameterTypes()[setterIndex], ","),
                            value.getClass().getName()),
                    e);
        }
    }

    /**
     * 类型擦除: Java 不能在运行时保留对象的泛型类型信息
     * 这就导致List等集合内元素不相干时也能set成功，这是个bug！
     * 所以根据需要，在这里加一层判断
     * 如果值和setter入参不匹配，先尝试递归进行转换......
     * 总之，是否会进行递归在于开发人员如何使用本工具类
     *
     * @param keyOrFieldName             键或成员变量
     * @param value                      源对象getter的返回值
     * @param getterGenericReturnType    源对象getter的返回值泛型Type，源对象非pojo时为null
     * @param setterGenericParameterType 目标setter的入参泛型Type，非null
     * @return Object 转换后的value
     * @see com.google.common.reflect.TypeToken
     * @see com.fasterxml.jackson.core.type.TypeReference
     * @see org.springframework.core.ParameterizedTypeReference
     * <div style='display: none'>@see org.apache.ibatis.type.TypeReference</div>
     */
    private static Object trans2SetterParameter(String keyOrFieldName, Object value,
                                                Type getterGenericReturnType,
                                                Type setterGenericParameterType) {
        if (getterGenericReturnType == null) {
            getterGenericReturnType = value.getClass();
        }
        try {
            return Castor.castType2Type(value, getterGenericReturnType, setterGenericParameterType);
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("(key或变量名:%s | 类型:%s | toString值:%s)无法转化成(%s)",
                    keyOrFieldName, getterGenericReturnType.getTypeName(),
                    value, setterGenericParameterType.getTypeName()));
        }
    }

}

