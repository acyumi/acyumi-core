package com.acyumi.reflect;

import com.acyumi.reflect.reflectasm.MethodAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.ClassUtils;
import org.springframework.util.TypeUtils;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 抽出来的反射相关的方法
 *
 * @author Mr.XiHui
 * @date 2017/12/5 19:03
 * @see com.esotericsoftware.reflectasm.MethodAccess
 * @see org.springframework.util.ReflectionUtils
 * @see org.springframework.beans.BeanUtils
 * @see TransformUtils
 */
public abstract class Reflector {

    public static final String SEPARATOR = ".";
    public static final String GETTER_PREFIX = "get";
    public static final String SETTER_PREFIX = "set";
    public static final String GET_CLASS_METHOD_NAME = "getClass";
    public static final byte[] EMPTY_BYTES = {};

    //储存pojo源对象和目标对象的getter和setter方法
    //private static final Map<Class<?>, MethodAccessor> METHOD_ACCESSOR_CACHE = new ConcurrentHashMap<>();
    private static final Cache<Class<?>, MethodAccessor> METHOD_ACCESSOR_CACHE = CacheBuilder.newBuilder()
            //设置cache的初始大小为64，要合理设置该值
            .initialCapacity(64)
            //设置cache的最大缓存个数为256
            .maximumSize(256)
            //设置并发数为5，即同一时间最多只能有10个线程往cache执行写入操作
            .concurrencyLevel(10)
            //缓存项在创建后，在给定时间内没有被读/写访问，则清除
            .expireAfterAccess(30, TimeUnit.MINUTES)
            //构建guava的纯java内存cache实例
            .build();

    //储存方法的参数名，LocalVariableTableParameterNameDiscoverer中有相应的参数名缓存
    private static final ParameterNameDiscoverer PARAMETER_NAME_CACHE = new LocalVariableTableParameterNameDiscoverer();
    //jdk8可以在编译时加-parameters参数达到在字节码中保留参数名的效果
    //Parameter.isNamePresent() 来验证参数名是不是可用

    /**
     * 通过targetClass的无参构造实例化对象
     *
     * @param targetClass 目标Class
     * @param <T>         目标类型
     * @return T 目标对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T newTarget(Class<?> targetClass) {
        if (targetClass == null) {
            throw new IllegalArgumentException("目标类型Class不能为null");
        }
        try {
            return (T) targetClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException(
                    String.format("反射实例化目标Class失败，请检查(%s)的无参构造方法是否可用。" +
                            "另外：interface和abstract class无法通过反射自动实例化", targetClass.getName()),
                    e);
        }
    }

    /**
     * 通过targetClass的有参构造实例化对象
     *
     * @param targetClass 目标Class
     * @param initArgs    实例化对象需要的入参
     * @param <T>         目标类型
     * @return T 目标对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T newTarget(Class<?> targetClass, Object... initArgs) {
        Constructor<?> constructor = getConstructor(targetClass, initArgs);
        if (constructor == null) {
            throw new IllegalArgumentException(
                    String.format("反射实例化目标Class失败，请检查(%s)是否有匹配入参(%s)且可用的的构造方法。" +
                                    "另外：interface和abstract class无法通过反射自动实例化",
                            targetClass.getName(), getMethodArgsStr(initArgs)));
        }
        try {
            return (T) constructor.newInstance(initArgs);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    String.format("反射实例化目标Class失败，请检查(%s)是否有匹配入参(%s)且可用的的构造方法。" +
                                    "另外：interface和abstract class无法通过反射自动实例化",
                            targetClass.getName(), getMethodArgsStr(initArgs)), e);
        }
    }

    /**
     * 通过实例化对象需要的入参查找对象的有参构造方法
     *
     * @param clazz    对象Class
     * @param initArgs 实例化对象需要的入参
     * @return Constructor<?>
     */
    public static Constructor<?> getConstructor(Class<?> clazz, Object... initArgs) {
        if (clazz == null) {
            throw new IllegalArgumentException("目标类型Class不能为null");
        }
        if (initArgs == null) {
            throw new IllegalArgumentException("有参构造入参不能为null(提示：无参构造入参为空数组{})");
        }
        Constructor<?>[] constructors = clazz.getConstructors();
        c:
        for (int i = 0; i < constructors.length; i++) {
            Constructor<?> constructor = constructors[i];
            Parameter[] parameters = constructor.getParameters();
            if (parameters.length != initArgs.length) {
                continue;
            }
            for (int j = 0; j < parameters.length; j++) {
                Parameter parameter = parameters[j];
                if (!isInstance(parameter.getType(), initArgs[j])) {
                    continue c;//不匹配，则跳到下一个外层循环
                }
            }
            return constructor;
        }
        return null;
    }

    /**
     * 通过类型引用对象获取Type
     * 抛砖引玉，这个方法最大的用处是摆出下面的@see，请去查看吧
     *
     * @param typeReference 类型引用对象，一般通过使用内部类的方式构建
     * @return Type
     * @see TypeReference
     * @see org.apache.ibatis.type.TypeReference
     * @see com.google.common.reflect.TypeToken
     * @see org.springframework.core.ParameterizedTypeReference
     * @see #makeParamType(Class, Type...)
     */
    public static Type getType(TypeReference typeReference) {
        return typeReference.getType();
    }

    /**
     * 构造一个参数化类型，用于进行isAssignable判断
     *
     * @param parametricClass 参数化类型的母体（寄主）
     * @param elemTypes       参数化类型的元素类型 （寄生体）
     * @return ParameterizedType
     * @see #getType(TypeReference)
     */
    public static ParameterizedType makeParamType(Class<?> parametricClass,
                                                  Type... elemTypes) {
        return ParameterizedTypeImpl.make(parametricClass, elemTypes, null);
    }

    /*** 判断obj是否clazz的实例 */
    public static boolean isInstance(Class<?> clazz, Object obj) {
        return ClassUtils.isAssignableValue(clazz, obj);
    }

    /**
     * 判断pClass是否subClass的上级(继承的父类或实现的接口)
     *
     * @param pClass   上级Class
     * @param subClass 下级Class
     */
    public static boolean isAssignable(Class<?> pClass, Class<?> subClass) {
        return ClassUtils.isAssignable(pClass, subClass);
    }

    /**
     * 判断pType是否subType的上级(继承的父类或实现的接口)
     * 如果是参数化类型，当最外层Class是之间是isAssignable关系且其中的元素类型相同才是true
     * 其实有时候反射赋值时我们希望List<String>当成Collection<Object>来用，
     * 这时候此方法判断返回的是false，我们需要再进一步对其中元素泛型类型进行判断
     * {@link #makeParamType(Class, Type...)}
     * isAssignable(makeParamType(Collection.class, Object.class),makeParamType(List.class, String.class))
     * 返回false
     * isAssignable(makeParamType(Collection.class, String.class),makeParamType(List.class, String.class))
     * 返回true
     *
     * @param pType   上级Type
     * @param subType 下级Type
     */
    public static boolean isAssignable(Type pType, Type subType) {
        return TypeUtils.isAssignable(pType, subType);
    }

    /*** 判断是否8种基本数据包装类型之一 */
    public static boolean isPrimitiveWrapper(Class<?> clazz) {
        return ClassUtils.isPrimitiveWrapper(clazz);
    }

    /*** 判断是否8种基本数据类型或其包装类型之一 */
    public static boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return ClassUtils.isPrimitiveOrWrapper(clazz);
    }

    /**
     * 获取type的元素类型
     *
     * @param type 集合的ParameterizedType或数组的Class
     * @return 元素Class
     */
    public static Class<?> getElementClass(Type type) {
        return getClassFromType(getElementType(type));
    }

    /**
     * 获取type的元素类型
     *
     * @param type 集合的ParameterizedType或数组的Class
     * @return 元素Type
     */
    public static Type getElementType(Type type) {
        Type elementType = null;
        if (type instanceof ParameterizedType) {
            ParameterizedType getterPt = (ParameterizedType) type;
            Class<?> rawType = (Class<?>) getterPt.getRawType();
            if (Iterable.class.isAssignableFrom(rawType)) {
                ParameterizedType paramType = (ParameterizedType) type;
                elementType = paramType.getActualTypeArguments()[0];
            } else if (Map.class.isAssignableFrom(rawType)) {
                ParameterizedType paramType = (ParameterizedType) type;
                elementType = paramType.getActualTypeArguments()[1];
            } else if (rawType.isArray()) {
                elementType = rawType.getComponentType();
            }
        } else if (type instanceof Class<?>) {
            Class<?> clazz = (Class<?>) type;
            if (clazz.isArray()) {
                elementType = clazz.getComponentType();
            }
        }
        return elementType;
    }

    /*** 获取type的实质Class */
    public static Class<?> getClassFromType(Type type) {
        if (type != null) {
            if (type instanceof Class) {
                return (Class<?>) type;
            } else if (type instanceof ParameterizedType) {
                Type rawType = ((ParameterizedType) type).getRawType();
                if (rawType instanceof Class) {
                    return (Class<?>) rawType;
                }
            }
        }
        return null;
    }

    /*** 判断是否首字母小写，第二字母大写的变量名（奇行种） */
    public static boolean isAlienName(String fieldName) {
        return fieldName.length() > 1
                && Character.isLowerCase(fieldName.charAt(0))
                && Character.isUpperCase(fieldName.charAt(1));
    }

    /**
     * 获取clazz类的变量名列表
     *
     * @param clazz 对象Class
     * @return 变量名列表
     * @see MethodAccessor
     */
    public static String[] getFieldNames(Class<?> clazz) {
        return getMethodAccessor(clazz).getFieldNames();
    }

    /**
     * 通过MethodAccess执行对象的方法
     * 此方法请尽量在非循环中使用
     *
     * @param obj       对象实例
     * @param methodStr method.toString().replaceFirst(".+ ", "")对应的字符串
     *                  也就是className + '.' + methodName + "(参数列表，多个以','分隔)"
     * @param args      查找到的Method的入参数组
     * @return 执行方法后得到的返回值，对象实例为null或找不到方法或方法没有返回值时返回null
     * @see MethodAccessor
     */
    public static Object invokeByMethodStr(Object obj, String methodStr, Object... args) {
        if (obj == null) {
            return null;
        }
        MethodAccessor methodAccessor = getMethodAccessor(obj.getClass());
        Integer methodIndex = methodAccessor.getMethodIndex(methodStr);
        if (methodIndex == null) {
            return null;
        }
        return methodAccessor.invoke(obj, methodIndex, args);
    }

    /**
     * 通过MethodAccess执行对象的方法
     * 此方法请尽量在非循环中使用
     * 因为获取methodIndex方法索引的方式不一样，
     * 强烈建议使用{@link #invokeByMethodStr(Object, String, Object...)}而非此方法
     *
     * @param obj        对象实例
     * @param methodName 要执行的方法名
     * @param args       查找到的Method的入参数组
     * @return 执行方法后得到的返回值，对象实例为null或找不到方法或方法没有返回值时返回null
     * @see MethodAccessor
     */
    public static Object invoke(Object obj, String methodName, Object... args) {
        if (obj == null) {
            return null;
        }
        MethodAccessor methodAccessor = getMethodAccessor(obj.getClass());
        Integer methodIndex = methodAccessor.getMethodIndex(methodName, args);
        if (methodIndex == null) {
            return null;
        }
        return methodAccessor.invoke(obj, methodIndex, args);
    }

    /**
     * 通过getter方法获取对象的变量值
     *
     * @param obj       对象实例，非null!!
     * @param fieldName 对象的成员变量名
     * @return 执行方法后得到的返回值，找不到方法或方法没有返回值时返回null
     * @see MethodAccessor
     */
    public static Object getFieldValue(Object obj, String fieldName) {
        if (obj == null) {
            return null;
        }
        MethodAccessor methodAccessor = getMethodAccessor(obj.getClass());
        return methodAccessor.getFieldValue(obj, fieldName);
    }

    /**
     * 通过setter方法给对象的变量赋值
     *
     * @param obj       对象实例，非null!!
     * @param fieldName 对象的成员变量名
     * @param arg       setter方法的入参
     * @see MethodAccessor
     */
    public static void setFieldValue(Object obj, String fieldName, Object arg) {
        if (obj == null) {
            return;
        }
        MethodAccessor methodAccessor = getMethodAccessor(obj.getClass());
        methodAccessor.setFieldValue(obj, fieldName, arg);
    }

    /*** 从内存中获取MethodAccessor */
    public static MethodAccessor getMethodAccessor(Class<?> clazz) {
        //return METHOD_ACCESSOR_MAP.computeIfAbsent(clazz, MethodAccessor::new);
        try {
            return METHOD_ACCESSOR_CACHE.get(clazz, () -> MethodAccessor.get(clazz));
        } catch (ExecutionException e) {
            throw new RuntimeException("创建MethodAccessor失败", e);
        }
    }

    /*** 获取方法参数名列表(使用Spring支持类库) */
    public static String[] getMethodParamNames(Method method) {
        //jdk8可以在编译时加-parameters参数达到在字节码中保留参数名的效果
        //所以不主动指定去编译，method.getParameters()[0].getName()得到的就不是源码的参数名
        //Parameter.isNamePresent() 来验证参数名是不是可用
        return PARAMETER_NAME_CACHE.getParameterNames(method);
    }

    /*** 获取方法入参列表的字符串，用于打印异常信息 */
    private static String getMethodArgsStr(Object... args) {
        StringBuilder cpsBuilder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            Object initArg = args[i];
            if (initArg == null) {
                cpsBuilder.append("null");
            } else {
                cpsBuilder.append(initArg.getClass().getName());
                cpsBuilder.append(':');
                if (initArg instanceof Object[]) {
                    cpsBuilder.append(Arrays.toString(args));
                } else {
                    cpsBuilder.append(initArg);
                }
            }
            cpsBuilder.append(',').append(' ');
        }
        int length = cpsBuilder.length();
        if (length > 0) {
            cpsBuilder.deleteCharAt(length - 1);
            cpsBuilder.deleteCharAt(length - 2);
        } else {
            cpsBuilder.append("empty args");
        }
        return cpsBuilder.toString();
    }
}