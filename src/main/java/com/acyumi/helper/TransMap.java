package com.acyumi.helper;


import com.acyumi.cast.Castor;
import com.acyumi.reflect.Reflector;
import com.acyumi.utils.JsonUtils;
import com.acyumi.utils.ParameterUtils;
import com.acyumi.utils.TransformUtils;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 参考Nutz的NutMap和Castors，
 * 然后结合阿里FastJson的JSONObject写了此类
 * 先按需求加功能咯
 *
 * @author Mr.XiHui
 * @date 2017/8/16.
 */
public class TransMap extends LinkedHashMap<String, Object> {

    private static final long serialVersionUID = 2698745465007166157L;

    public static TransMap newWithExpectedSize(int expectedSize) {
        return new TransMap(ParameterUtils.calcMapCapacity(expectedSize));
    }

    public TransMap() {
    }

    public TransMap(Map<String, Object> map) {
        super(map);
    }

    public TransMap(int initialCapacity) {
        super(initialCapacity);
    }

    @SuppressWarnings("unchecked")
    public <T> T getObj(String key, TypeReference<T> typeReference) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        return (T) Castor.castType2Type(value, value.getClass(), typeReference.getType());
    }

    @SuppressWarnings("unchecked")
    public <T> T getObj(String key, Class<T> clazz) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        return (T) Castor.castType2Type(value, value.getClass(), clazz);
    }

    /**
     * 获取String类型的value，失败(或为null)时返回null
     *
     * @param key 键
     * @return String
     */
    public String getString(String key) {
        try {
            return Castor.castSrc2JdkClass(get(key), String.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取String类型的value，失败(或为null)时返回defaultValue
     *
     * @param key          键
     * @param defaultValue 失败(或为null)时返回此值
     * @return String
     */
    public String getString(String key, String defaultValue) {
        String value = null;
        try {
            value = Castor.castSrc2JdkClass(get(key), String.class);
        } catch (Exception e) {/*ignore*/}
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    /**
     * 获取Integer类型的value，失败(或为null)时返回null
     *
     * @param key 键
     * @return Integer
     */
    public Integer getInteger(String key) {
        try {
            return Castor.castSrc2JdkClass(get(key), Integer.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取Integer类型的value，失败(或为null)时返回defaultValue
     *
     * @param key          键
     * @param defaultValue 失败(或为null)时返回此值
     * @return Integer
     */
    public Integer getInteger(String key, Integer defaultValue) {
        Integer value = null;
        try {
            value = Castor.castSrc2JdkClass(get(key), Integer.class);
        } catch (Exception e) {/*ignore*/}
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    /**
     * 获取Long类型的value，失败(或为null)时返回null
     *
     * @param key 键
     * @return Long
     */
    public Long getLong(String key) {
        try {
            return Castor.castSrc2JdkClass(get(key), Long.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取Long类型的value，失败(或为null)时返回defaultValue
     *
     * @param key          键
     * @param defaultValue 失败(或为null)时返回此值
     * @return Long
     */
    public Long getLong(String key, Long defaultValue) {
        Long value = null;
        try {
            value = Castor.castSrc2JdkClass(get(key), Long.class);
        } catch (Exception e) {/*ignore*/}
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    /**
     * 获取boolean类型的value，失败(或为null)时返回false（若有异常则吃掉也返回false）
     * 先将value转成String再忽略大小写匹配"true"，匹配成功返回true
     *
     * @param key 键
     * @return boolean
     */
    public Boolean getBoolean(String key) {
        try {
            return Castor.castSrc2JdkClass(get(key), String.class);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取boolean类型的value，为null或异常时返回defaultValue
     * 先将value转成String再忽略大小写匹配"true"，匹配成功返回true
     *
     * @param key          键
     * @param defaultValue 异常或为null时才返回此值
     * @return boolean
     */
    public Boolean getBoolean(String key, boolean defaultValue) {
        Boolean value = null;
        try {
            value = Castor.castSrc2JdkClass(get(key), Boolean.class);
        } catch (Exception e) {/*ignore*/}
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    /**
     * 获取List类型的value，转换失败则提示异常
     *
     * @param key 键
     * @param <E> List的元素类型
     * @return List&lt;E>
     */
    @SuppressWarnings("unchecked")
    public <E> List<E> getList(String key) {
        Object value = get(key);
        if (value == null) {
            return new ArrayList<>(0);
        } else if (value instanceof Iterable) {
            return Castor.castIterable((Iterable<?>) value, List.class);
        } else if (value instanceof Object[]) {
            return Castor.castArr2Itr((Object[]) value, List.class);
        }
        throw new IllegalArgumentException("类型转换失败，请检查" + key + "的值是否可以转换为List，" +
                "如果List的元素类型不匹配，可尝试使用getList(String key, Class<E> elemClass)方法");
    }

    /**
     * 获取List类型的value，转换失败则提示异常
     *
     * @param key 键
     * @param <E> List的元素类型
     * @return List&lt;E>
     */
    @SuppressWarnings("unchecked")
    public <E> List<E> getList(String key, Class<E> elemClass) {
        Object value = get(key);
        if (value == null) {
            return new ArrayList<>(0);
        } else if (value instanceof Iterable) {
            Object firstElem = null;
            List<?> list;
            if (value instanceof List) {
                list = (List<?>) value;
            } else {
                list = Castor.castIterable((Iterable<?>) value, List.class);
            }
            for (int i = 0; i < list.size(); i++) {
                firstElem = list.get(i);
                if (firstElem != null) {
                    break;
                }
            }
            if (Reflector.isInstance(elemClass, firstElem)) {
                return (List<E>) list;
            }
            String elemClassName = elemClass.getName();
            if (!elemClassName.contains(".") || elemClassName.startsWith("java.")) {
                return Castor.castElement(list, elemClass);
            }
            return TransformUtils.transformList(list, elemClass, false);
        } else if (value instanceof Object[]) {
            List<?> list = Castor.castArr2Itr((Object[]) value, List.class);
            if (Reflector.isAssignable(elemClass, Reflector.getElementClass(value.getClass()))) {
                return (List<E>) list;
            }
            String elemClassName = elemClass.getName();
            if (!elemClassName.contains(".") || elemClassName.startsWith("java.")) {
                return Castor.castElement(list, elemClass);
            }
            return TransformUtils.transformList(list, elemClass, false);
        }
        throw new IllegalArgumentException("类型转换失败，请检查" + key + "的值是否为List或者List的元素类型是否匹配");
    }

    /**
     * 获取Map类型的value，转换失败则提示异常
     *
     * @param key 键
     * @param <K> 欲获取Map的键类型
     * @param <V> 欲获取Map的值类型
     * @return Map&lt;K,V>
     */
    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> getMap(String key, TypeReference<Map<K, V>> typeReference) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        return (Map<K, V>) Castor.castType2Type(value, value.getClass(), typeReference.getType());
    }

    /**
     * 获取Map类型的value，转换失败则提示异常
     *
     * @param key 键
     * @return TransMap
     */
    public TransMap getTransMap(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        try {
            return (TransMap) value;
        } catch (Throwable t) {
            return TransformUtils.transformToTransMap(value);
        }
    }

    @Override
    public String toString() {
        return toJsonStr();
    }

    public String toJsonStr() {
        return JsonUtils.toJsonStr(this);
    }

}
