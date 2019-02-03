package com.acyumi.cast;

import com.acyumi.reflect.Reflector;
import com.acyumi.utils.JsonUtils;
import com.acyumi.utils.ParameterUtils;
import com.acyumi.utils.TransformUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 类型转换相关的方法
 * <p>
 * 此类的作用是尽可能将对象转换成目标类型
 *
 * @author Mr.XiHui
 * @date 2018/04/22 18:37
 * @see Reflector
 * @see TransformUtils
 * @see JsonUtils
 */
public abstract class Castor {

    /**
     * 尝试将源对象转换成目标类型
     *
     * @param src     源对象
     * @param srcType 源类型
     * @param tgtType 目标类型
     * @return Object
     */
    public static Object castType2Type(Object src, Type srcType, Type tgtType) {

        if (src == null) {
            return null;
        }
        Assert.notNull(tgtType, "目标Type不能为null");
        Assert.notNull(srcType, "源Type不能为null");

        // all types are assignable to themselves and to class Object
        if (tgtType.equals(srcType) || Object.class == tgtType) {
            return src;
        }

        //如果tgtType是Class
        if (tgtType instanceof Class) {
            Class<?> tgtClass = (Class<?>) tgtType;

            // 跳转到比较两个Class
            if (srcType instanceof Class) {
                Class<?> srcClass = (Class<?>) srcType;

                return castClass2Class(src, srcClass, tgtClass);

            }

            //如果srcType是参数化类型，如Set<String>
            else if (srcType instanceof ParameterizedType) {
                Type srcRaw = ((ParameterizedType) srcType).getRawType();

                // a parameterized type is always assignable to its raw class type
                if (srcRaw instanceof Class) {

                    return castClass2Class(src, (Class<?>) srcRaw, tgtClass);
                }
            }

            //如果srcType是参数化类型数组，如List<String>[]
            else if (srcType instanceof GenericArrayType) {
                Type srcComponent = ((GenericArrayType) srcType).getGenericComponentType();

                if (srcComponent instanceof ParameterizedType) {
                    Type srcComponentRaw = ((ParameterizedType) srcComponent).getRawType();

                    if (srcComponentRaw instanceof Class) {

                        //Array.newInstance((Class<?>) srcComponentRaw,0).getClass()
                        //进行了这么多判断到这里，src就是数组，直接src.getClass()得到数组的Class
                        return castClass2Class(src, src.getClass(), tgtClass);
                    }
                }
            }
        }

        // parameterized types are only assignable to other parameterized types and class types
        else if (tgtType instanceof ParameterizedType) {

            if (srcType instanceof Class) {
                return castClass2ParamType(src, (Class<?>) srcType, (ParameterizedType) tgtType);
            }

            //如果srcType是参数化类型数组，如List<String>[]
            else if (srcType instanceof ParameterizedType) {
                return castParamType2ParamType(src, (ParameterizedType) srcType, (ParameterizedType) tgtType);
            }
        }

        //如果tgtType是泛型数组类型
        else if (tgtType instanceof GenericArrayType) {
            Type tgtComponent = ((GenericArrayType) tgtType).getGenericComponentType();

            if (srcType instanceof GenericArrayType) {
                Type srcComponent = ((GenericArrayType) srcType).getGenericComponentType();

                if (Reflector.isAssignable(tgtComponent, srcComponent)) {
                    return src;
                }

                if (tgtComponent instanceof ParameterizedType) {
                    Type tgtComponentRaw = ((ParameterizedType) tgtComponent).getRawType();

                    if (srcComponent instanceof ParameterizedType) {
                        Type srcComponentRaw = ((ParameterizedType) srcComponent).getRawType();

                        if (tgtComponentRaw instanceof Class) {
                            Class<?> tgtCompRawClass = (Class<?>) tgtComponentRaw;

                            //接下来尝试转换数组元素类型

                            Object[] temp = (Object[]) Array.newInstance(
                                    tgtCompRawClass, ((Object[]) src).length);

                            for (int i = 0; i < ((Object[]) src).length; i++) {
                                temp[i] = castType2Type(((Object[]) src)[i],
                                        srcComponentRaw, tgtCompRawClass);
                            }
                            return temp;
                        }
                    }
                }
            }
        }

        //如果tgtType是限定性的泛型表达式，即 ? extends java.util.AbstractList 等

        //WildcardType中的upperBounds和lowerBounds二者必有一个非空
        //假设是 ? super ArrayList 表达式，
        //则upperBounds == null或upperBounds == new Type[]{Object.class}，lowerBounds == new Type[]{ArrayList.class}
        //假设是 ? extends List 表达式，则upperBounds == new Type[]{List.class}，lowerBounds == new Type[0]
        //假设是 ? 表达式(只有一个问号)，则upperBounds == new Type[]{Object.class}，lowerBounds == new Type[0]
        //getUpperBounds()和getLowerBounds()返回值都是数组（为了保留扩展），但写代码编译的时候就知道，
        //当前java版本(1.8)及以前版本中只可能出现一个元素，不知道后续版本会如何调整
        else if (tgtType instanceof WildcardType) {

            Type[] tgtUnAssignableBounds = getTgtUnAssignableBounds(((WildcardType) tgtType), srcType);
            if (tgtUnAssignableBounds == null) {
                return src;
            }

            //如果不匹配，则尝试递归进行转换
            return castType2Type(src, srcType, tgtUnAssignableBounds[0]);
        }

        throw new IllegalArgumentException(String.format("(类型:%s | toString值:%s)无法转化成(%s)",
                srcType.getTypeName(), src, tgtType.getTypeName()));
    }

    public static Object castClass2Class(Object src, Class<?> srcClass, Class<?> tgtClass) {

        //如果tgtClass可由srcClass的对象实例指定，则直接返回src
        if (Reflector.isAssignable(tgtClass, srcClass)) {
            return src;
        }

        //如果tgtClass是Iterable类型
        if (Reflector.isAssignable(Iterable.class, tgtClass)) {

            //Iterable转成Iterable
            if (Reflector.isAssignable(Iterable.class, srcClass)) {
                return castIterable((Iterable<?>) src, tgtClass);
            }

            //数组转成Iterable
            else if (srcClass.isArray()) {
                return castArr2Itr((Object[]) src, tgtClass);
            }

            //Map的值集合转成Iterable
            else if (Reflector.isAssignable(Map.class, srcClass)) {
                return castIterable(((Map<?, ?>) src).values(), tgtClass);
            }
        }

        //如果tgtClass是数组
        else if (tgtClass.isArray()) {
            Class<?> componentType = tgtClass.getComponentType();

            //Iterable转成数组
            if (Reflector.isAssignable(Iterable.class, srcClass)) {
                return castItr2Arr((Iterable<?>) src, componentType);
            }

            //数组转成数组
            else if (srcClass.isArray()) {
                return castElement((Object[]) src, componentType);
            }

            //Map的值集合转成数组
            else if (Reflector.isAssignable(Map.class, srcClass)) {
                return castItr2Arr(((Map<?, ?>) src).values(), componentType);
            }
        }

        //如果tgtClass是Map
        else if (Reflector.isAssignable(Map.class, tgtClass)) {
            return TransformUtils.transformToMap(src, tgtClass, String.class, Object.class);
        }

        //如果tgtClass是jdk类型
        String className = tgtClass.getName();
        if (!className.contains(".") || className.startsWith("java.")) {
            return castSrc2JdkClass(src, tgtClass);
        }

        //如果tgtClass是pojo（且当是pojo）
        else {
            return TransformUtils.transform(src, tgtClass);
        }

        /*throw new IllegalArgumentException(String.format("(类型:%s | toString值:%s)无法转化成(%s)",
                srcClass.getName(), src, tgtClass.getName()));*/
    }

    /**
     * 将源对象转化为jdk对象
     * 曲线救国：其他jdk类型借助Json字符串来转换
     *
     * @param src      待转换的源对象
     * @param jdkClass 目标类型Class
     * @param <T>      目标类型
     * @return 目标对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T castSrc2JdkClass(Object src, Class<?> jdkClass) {
        if (jdkClass == null) {
            throw new IllegalArgumentException("目标类型Class不能为null");
        }
        //基本数据的默认值
        else if (src == null && jdkClass.isPrimitive()) {
            return nullCast2Primitive(jdkClass);
        }
        //targetClass是对象，直接返回 null  或者  source是targetClass的实例，直接返回source
        else if (src == null || Reflector.isInstance(jdkClass, src)) {
            return (T) src;
        }
        //targetClass是基本数据类型且source不是targetClass的实例
        else if (ClassUtils.isPrimitiveOrWrapper(jdkClass)) {
            return notNullCast2PrimitiveOrWrapper(src, jdkClass);
        }
        //String是targetClass的实现或子类，返回Json字符串
        else if (jdkClass.isAssignableFrom(String.class)) {
            String jsonStr = JsonUtils.toJsonStr(src);
            return (T) JsonUtils.unWrapJsonStr(jsonStr);
        }
        //其他情况
        else {
            try {
                String jsonStr = JsonUtils.toJsonStr(src);
                return (T) JsonUtils.parseObj(jsonStr, jdkClass);
            } catch (Exception e) {
                throw new IllegalArgumentException(String.format("(类型:%s | toString值:%s)无法转化成(%s)",
                        src.getClass().getName(), src, jdkClass.getName()), e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Iterable<?>> T castIterable(Iterable<?> srcItr, Class<?> tgtItrCls) {
        if (tgtItrCls == null) {
            throw new IllegalArgumentException("目标集合类型Class不能为null");
        }
        if (srcItr == null || tgtItrCls.isInstance(srcItr)) {
            return (T) srcItr;
        }
        //---------------------------------------------------------
        //处理常用接口或抽象类 start
        else if (tgtItrCls == Collection.class
                || tgtItrCls == List.class
                || tgtItrCls == AbstractCollection.class
                || tgtItrCls == AbstractList.class) {
            if (srcItr instanceof Collection) {
                return (T) new ArrayList<>((Collection) srcItr);
            } else {
                List<Object> targetLs = new ArrayList<>();
                for (Object e : srcItr) {
                    targetLs.add(e);
                }
                return (T) targetLs;
            }
        } else if (tgtItrCls == Set.class || tgtItrCls == AbstractSet.class) {
            if (srcItr instanceof Collection) {
                return (T) new LinkedHashSet<>((Collection) srcItr);
            } else {
                Set<Object> targetSt = new LinkedHashSet<>();
                for (Object e : srcItr) {
                    targetSt.add(e);
                }
                return (T) targetSt;
            }
        } else if (tgtItrCls == Queue.class || tgtItrCls == AbstractQueue.class) {
            if (srcItr instanceof Collection) {
                return (T) new ArrayBlockingQueue<>(((Collection) srcItr).size(), false, (Collection) srcItr);
            } else {
                Queue<Object> targetQe = new ArrayBlockingQueue<>(ParameterUtils.getElementSize(srcItr));
                for (Object e : srcItr) {
                    targetQe.add(e);
                }
                return (T) targetQe;
            }
        }
        //处理常用接口或抽象类 end
        //---------------------------------------------------------
        //其他接口或抽象类未知默认实现或子类无法进行实例化，则提示异常
        else if (tgtItrCls.isInterface() || Modifier.isAbstract(tgtItrCls.getModifiers())) {
            throw new IllegalArgumentException(String.format("无法实例化目标集合(%s)", tgtItrCls.getName()));
        }

        try {//尝试使用集合对象作为入参来构造目标集合
            return Reflector.newTarget(tgtItrCls, srcItr);
        } catch (Exception ex) {
            if (Queue.class.isAssignableFrom(tgtItrCls)) {
                Queue<Object> tarQe = Reflector.newTarget(tgtItrCls, ParameterUtils.getElementSize(srcItr));
                for (Object e : srcItr) {
                    tarQe.add(e);
                }
                return (T) tarQe;
            } else if (Collection.class.isAssignableFrom(tgtItrCls)) {
                Collection<Object> tarCo = Reflector.newTarget(tgtItrCls);
                for (Object e : srcItr) {
                    tarCo.add(e);
                }
                return (T) tarCo;
            }
            throw new IllegalArgumentException(String.format("无法将(%s)转化成目标集合(%s)",
                    srcItr.getClass().getName(), tgtItrCls.getName()), ex);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] castItr2Arr(Iterable<?> srcItr, Class<?> componentType) {
        if (componentType == null) {
            throw new IllegalArgumentException("目标数组元素类型Class不能为null");
        }
        if (srcItr == null) {
            return null;
        }
        if (srcItr instanceof Collection) {
            return (T[]) ParameterUtils.toArray((Collection<?>) srcItr, componentType);
        }
        T[] array = (T[]) ParameterUtils.newArray(componentType, ParameterUtils.getElementSize(srcItr));
        int i = 0;
        for (Object e : srcItr) {
            array[i++] = (T) e;
        }
        return array;
    }

    public static <T extends Iterable<?>> T castArr2Itr(Object[] srcArr, Class<?> tgtItrCls) {
        return castIterable(Arrays.asList(srcArr), tgtItrCls);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Iterable<?>> T castElement(Iterable<?> srcItr, Type tgtElemType) {
        if (tgtElemType == null) {
            throw new IllegalArgumentException("目标集合元素类型Class不能为null");
        }
        if (srcItr == null) {
            return null;
        }
        Class<? extends Iterable> srcItrCls = srcItr.getClass();
        if (Queue.class.isAssignableFrom(srcItrCls)) {
            Queue<Object> tarQe;
            int elemSize = ParameterUtils.getElementSize(srcItr);
            try {
                tarQe = Reflector.newTarget(srcItrCls, elemSize);
            } catch (Exception e) {
                tarQe = new ArrayBlockingQueue<>(elemSize);
            }
            for (Object obj : srcItr) {
                if (obj == null) {
                    tarQe.add(null);
                } else {
                    tarQe.add(castType2Type(obj, obj.getClass(), tgtElemType));
                }
            }
            return (T) tarQe;
        } else if (Collection.class.isAssignableFrom(srcItrCls)) {
            Collection<Object> tarCo;
            try {
                tarCo = Reflector.newTarget(srcItrCls);
            } catch (Exception e) {
                if (List.class.isAssignableFrom(srcItrCls)) {
                    tarCo = new ArrayList<>();
                } else {
                    tarCo = new LinkedHashSet<>();
                }
            }
            for (Object obj : srcItr) {
                if (obj == null) {
                    tarCo.add(null);
                } else {
                    tarCo.add(castType2Type(obj, obj.getClass(), tgtElemType));
                }
            }
            return (T) tarCo;
        } else if (ParameterUtils.isEmpty(srcItr)) {
            try {
                return Reflector.newTarget(srcItrCls);
            } catch (Exception e) {
                return Reflector.newTarget(srcItrCls, 0);
            }
        }
        String elemClsName = srcItr.iterator().next().getClass().getName();
        throw new IllegalArgumentException(String.format("无法将集合(%s)的元素类型(%s)转化成(%s)",
                srcItrCls.getName(), elemClsName, tgtElemType.getTypeName()));
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] castElement(Object[] srcArr, Class<?> tgtElemCls) {
        if (tgtElemCls == null) {
            throw new IllegalArgumentException("目标数组元素类型Class不能为null");
        }
        if (srcArr == null) {
            return null;
        }
        Class<?> srcElemCls = srcArr.getClass().getComponentType();
        if (tgtElemCls.isAssignableFrom(srcElemCls)) {
            return (T[]) srcArr;
        }
        T[] array = (T[]) ParameterUtils.newArray(tgtElemCls, srcArr.length);
        for (int i = 0; i < srcArr.length; i++) {
            array[i] = (T) castClass2Class(srcArr[i], srcElemCls, tgtElemCls);
        }
        return array;
    }


    private static Object castClass2ParamType(Object src, Class<?> srcClass, ParameterizedType tgtType) {

        Type tgtRaw = tgtType.getRawType();

        if (tgtRaw instanceof Class) {
            Class<?> tgtRawClass = (Class<?>) tgtRaw;

            //如果tgtRawClass是Iterable类型
            if (Reflector.isAssignable(Iterable.class, tgtRawClass)) {

                Iterable<?> iterable = null;

                //Iterable转成Iterable
                if (Reflector.isAssignable(Iterable.class, srcClass)) {
                    iterable = castIterable((Iterable<?>) src, tgtRawClass);
                }

                //数组转成Iterable
                else if (srcClass.isArray()) {
                    iterable = castArr2Itr((Object[]) src, tgtRawClass);
                }

                //Map的值集合转成Iterable
                else if (Reflector.isAssignable(Map.class, srcClass)) {
                    iterable = castIterable(((Map<?, ?>) src).values(), tgtRawClass);
                }

                if (iterable != null) {
                    Type tgtArgType = tgtType.getActualTypeArguments()[0];

                    if (tgtArgType instanceof Class) {
                        return castElement(iterable, tgtArgType);
                    }

                    //如果tgtType的元素类型也是ParameterizedType
                    else if (tgtArgType instanceof ParameterizedType) {
                        Type tgtArgRaw = ((ParameterizedType) tgtArgType).getRawType();

                        if (tgtArgRaw instanceof Class) {
                            return castElement(iterable, tgtArgRaw);
                        }
                    }
                }
            }

            //如果tgtRawClass是Map
            else if (Reflector.isAssignable(Map.class, tgtRawClass)) {

                Type tgtKeyType = tgtType.getActualTypeArguments()[0];
                Type tgtValueType = tgtType.getActualTypeArguments()[1];
                Class<?> tgtKeyClass = Reflector.getClassFromType(tgtKeyType);
                Class<?> tgtValueClass = Reflector.getClassFromType(tgtValueType);

                if (tgtKeyClass != null && tgtValueClass != null) {
                    return TransformUtils.transformToMap(src, tgtRawClass, tgtKeyClass, tgtValueClass);
                }
            }

            //如果tgtRawClass是jdk类型
            String className = tgtRawClass.getName();
            if (!className.contains(".") || className.startsWith("java.")) {
                return castSrc2JdkClass(src, tgtRawClass);
            }

            //如果tgtRawClass是pojo（且当是pojo）
            else {
                return TransformUtils.transform(src, tgtRawClass);
            }
        }

        return castType2Type(src, srcClass, tgtRaw);
        /*throw new IllegalArgumentException(String.format("(类型:%s | toString值:%s)无法转化成(%s)",
                srcClass.getName(), src, tgtType.getTypeName));*/
    }

    private static Object castParamType2ParamType(Object src,
                                                  ParameterizedType srcType,
                                                  ParameterizedType tgtType) {
        if (tgtType.equals(srcType)) {
            return src;
        }

        Type tgtRaw = tgtType.getRawType();
        Type srcRaw = srcType.getRawType();
        Type[] tgtTypeArgs = tgtType.getActualTypeArguments();
        Type[] srcTypeArgs = srcType.getActualTypeArguments();

        if (tgtRaw instanceof Class) {
            Class<?> tgtRawClass = (Class<?>) tgtRaw;

            //如果tgtType是Iterable
            if (Reflector.isAssignable(Iterable.class, tgtRawClass)) {
                if (srcRaw instanceof Class) {

                    Class<?> srcRawClass = (Class<?>) srcRaw;
                    Iterable<?> temp = (Iterable<?>) src;
                    Type tgtArg = tgtTypeArgs[0];
                    Type srcArg = null;

                    //都是Iterable
                    if (Reflector.isAssignable(Iterable.class, srcRawClass)) {
                        srcArg = srcTypeArgs[0];
                        if (!Reflector.isAssignable(tgtRawClass, srcRawClass)) {
                            temp = castIterable((Iterable<?>) src, tgtRawClass);
                        }
                    }

                    //如果srcType是Map
                    else if (Reflector.isAssignable(Map.class, srcRawClass)) {
                        srcArg = srcTypeArgs[1];
                        if (!Reflector.isAssignable(tgtRawClass, Collection.class)) {
                            temp = castIterable(((Map<?, ?>) src).values(), tgtRawClass);
                        }
                    }

                    //srcType非Iterable或非Map时，直接返回给外层处理，不尝试转换
                    //tgtArg是srcArg的接口或父类时(包括tgtArg是Object.class和?和? extends Object)，
                    //说明是匹配的，也直接返回
                    if (srcArg == null || Reflector.isAssignable(tgtArg, srcArg)) {
                        return temp;
                    }

                    //只有srcType是Iterable或是Map时才会走到这里，然后尝试转换
                    else if (tgtArg instanceof Class) {
                        return castElement(temp, tgtArg);
                    } else if (tgtArg instanceof ParameterizedType) {
                        Type tgtArgRaw = ((ParameterizedType) tgtArg).getRawType();
                        if (tgtArgRaw instanceof Class) {
                            return castElement(temp, tgtArgRaw);
                        }
                    } else if (tgtArg instanceof WildcardType) {

                        Type[] tgtUnAssignableBounds = getTgtUnAssignableBounds(((WildcardType) tgtArg), srcType);
                        if (tgtUnAssignableBounds == null) {
                            return temp;
                        }

                        return castElement(temp, tgtUnAssignableBounds[0]);
                    }
                }
            }

            //如果tgtType是Map
            else if (Reflector.isAssignable(Map.class, tgtRawClass)) {
                if (srcRaw instanceof Class) {

                    Class<?> srcRawClass = (Class<?>) srcRaw;

                    //都是Map
                    if (Reflector.isAssignable(Map.class, srcRawClass)) {

                        //tgtType的key类型是srcType的key类型的接口或父类时
                        if (Reflector.isAssignable(tgtTypeArgs[0], srcTypeArgs[0])) {
                            Type tgtMapValType = tgtTypeArgs[1];
                            Type srcMapValType = srcTypeArgs[1];

                            if (Reflector.isAssignable(tgtMapValType, srcMapValType)) {
                                return src;
                            } else {

                                Map<Object, Object> temp = Reflector.newTarget(tgtRawClass);
                                for (Map.Entry<?, ?> entry : ((Map<?, ?>) src).entrySet()) {
                                    Object value = entry.getValue();
                                    value = castType2Type(value, srcMapValType, tgtMapValType);
                                    temp.put(entry.getKey(), value);
                                }
                                return temp;
                            }
                        }
                    }
                }
            }
        }

        throw new IllegalArgumentException(String.format("(类型:%s | toString值:%s)无法转化成(%s)",
                srcType.getTypeName(), src, tgtType.getTypeName()));
    }

    private static Type[] getTgtUnAssignableBounds(WildcardType tgtType, Type srcType) {

        Type[] tgtArgBounds = tgtType.getLowerBounds();

        //如果tgtType下限lowerBounds为空，则转为获取tgtType上限upperBounds
        if (ParameterUtils.isEmpty(tgtArgBounds)) {
            tgtArgBounds = tgtType.getUpperBounds();

            //如果tgtType是? 或 ? extends Object 表达式，那么直接返回src
            if (tgtArgBounds[0] == Object.class) {
                return null;
            } else if (srcType instanceof WildcardType) {
                Type[] srcArgBounds = ((WildcardType) srcType).getLowerBounds();

                //如果srcType下限lowerBounds也为空，也转为获取srcType上限upperBounds
                if (ParameterUtils.isEmpty(srcArgBounds)) {
                    srcArgBounds = ((WildcardType) srcType).getUpperBounds();

                    //判断(tgtType上限upperBounds)是否是(srcType上限upperBounds)的父类或接口
                    if (Reflector.isAssignable(tgtArgBounds[0], srcArgBounds[0])) {
                        return null;
                    }
                }
            }
        }

        //如果tgtType下限lowerBounds不为空，则获取tgtType下限lowerBounds
        else if (srcType instanceof WildcardType) {
            Type[] srcArgBounds = ((WildcardType) srcType).getLowerBounds();

            //如果srcType下限lowerBounds不为空，则获取srcType下限lowerBounds
            if (!ParameterUtils.isEmpty(srcArgBounds)) {

                //判断(srcType下限lowerBounds)是否是(tgtType下限lowerBounds)的父类或接口
                if (Reflector.isAssignable(srcArgBounds[0], tgtArgBounds[0])) {
                    return null;
                }
            }
        }
        return tgtArgBounds;
    }

    @SuppressWarnings("unchecked")
    private static <T> T nullCast2Primitive(Class<?> tgtClass) {
        if (tgtClass == int.class) {
            return (T) Integer.valueOf(0);
        } else if (tgtClass == long.class) {
            return (T) Long.valueOf(0L);
        } else if (tgtClass == boolean.class) {
            return (T) Boolean.FALSE;
        } else if (tgtClass == double.class) {
            return (T) Double.valueOf(.0);
        } else if (tgtClass == float.class) {
            return (T) Float.valueOf(.0f);
        } else if (tgtClass == short.class) {
            return (T) Short.valueOf((short) 0);
        } else if (tgtClass == byte.class) {
            return (T) Byte.valueOf((byte) 0);
        } else if (tgtClass == char.class) {
            return (T) Character.valueOf(' ');
        }
        throw new IllegalArgumentException("(null)无法转换成(" + tgtClass.getName() + ")");
    }

    @SuppressWarnings("unchecked")
    private static <T> T notNullCast2PrimitiveOrWrapper(Object src, Class<?> tgtClass) {
        String srcStr = src.toString();
        try {
            if (tgtClass == Integer.TYPE || tgtClass == Integer.class) {
                try {
                    return (T) Integer.valueOf(srcStr);
                } catch (Exception e) {
                    return (T) Integer.valueOf(replaceUnused4Primitive(srcStr));
                }
            } else if (tgtClass == Long.TYPE || tgtClass == Long.class) {
                try {
                    return (T) Long.valueOf(srcStr);
                } catch (Exception e) {
                    return (T) Long.valueOf(replaceUnused4Primitive(srcStr));
                }
            } else if (tgtClass == Boolean.TYPE || tgtClass == Boolean.class) {
                try {
                    return (T) Boolean.valueOf(srcStr);
                } catch (Exception e) {
                    return (T) Boolean.valueOf(replaceUnused4Primitive(srcStr));
                }
            } else if (tgtClass == Double.TYPE || tgtClass == Double.class) {
                try {
                    return (T) Double.valueOf(srcStr);
                } catch (Exception e) {
                    return (T) Double.valueOf(replaceUnused4Primitive(srcStr));
                }
            } else if (tgtClass == Float.TYPE || tgtClass == Float.class) {
                try {
                    return (T) Float.valueOf(srcStr);
                } catch (Exception e) {
                    return (T) Float.valueOf(replaceUnused4Primitive(srcStr));
                }
            } else if (tgtClass == Short.TYPE || tgtClass == Short.class) {
                try {
                    return (T) Short.valueOf(srcStr);
                } catch (Exception e) {
                    return (T) Short.valueOf(replaceUnused4Primitive(srcStr));
                }
            } else if (tgtClass == Byte.TYPE || tgtClass == Byte.class) {
                try {
                    return (T) Byte.valueOf(srcStr);
                } catch (Exception e) {
                    return (T) Byte.valueOf(replaceUnused4Primitive(srcStr));
                }
            } else if (tgtClass == Character.TYPE || tgtClass == Character.class) {
                srcStr = srcStr.trim().replaceAll("[\\s]+", "");
                if (srcStr.length() == 1) {
                    return (T) Character.valueOf(srcStr.charAt(0));
                }
            }
        } catch (Exception e) {/*ignore*/}
        throw new IllegalArgumentException(String.format("(类型:%s | toString值:%s)无法转换成(%s)",
                src.getClass().getName(), src, tgtClass.getName()));
    }

    private static String replaceUnused4Primitive(String str) {
        return str.trim().replaceAll("[\"'\\s]+", "");
    }
}
