package com.acyumi.annotation;

import com.acyumi.reflect.Reflector;
import com.acyumi.reflect.reflectasm.MethodAccessor;
import com.acyumi.util.ParameterUtils;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.*;

/**
 * 对对象的多个成员变量进行不全空的校验. <br>
 * 只要有一个成员变量不为空就校验通过 <br><br>
 * <p>
 * 声明在类头上时，被校验的对象为类本身的实例 <br>
 * 声明在方法上时，被校验的对象为方法返回类型的实例
 *
 * @author Mr.XiHui
 * @date 2018/12/19
 */
@Documented
@Target({
        ElementType.TYPE,
        ElementType.METHOD,
        ElementType.ANNOTATION_TYPE
})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(NotAllEmpty.NotAllEmpties.class)
@Constraint(validatedBy = NotAllEmpty.NotAllEmptyValidator.class)
public @interface NotAllEmpty {

    /**
     * 被校验类的成员变量名.
     *
     * @return 成员变量名数组
     */
    String[] fieldNames();

    /**
     * 校验失败时的消息.
     *
     * @return 校验失败时的消息
     */
    String message();

    /**
     * 验证时所属的组别，因为@Valid未吸收分组功能，所以只能配合@Validated使用
     * 这里groups()包含{@link org.springframework.validation.annotation.Validated#value()}的值时当前注解才会进行校验
     *
     * @return 分组Class数组
     * @see org.springframework.validation.annotation.Validated#value()
     */
    Class<?>[] groups() default {};

    /**
     * 约束注解的有效负载
     * 怎么用？喵喵喵~
     *
     * @return Payload的Class数组
     */
    Class<? extends Payload>[] payload() default {};

    /**
     * Defines several {@code @NotAllEmpty} constraints on the same element.
     *
     * @see NotAllEmpty
     */
    @Target({
            ElementType.TYPE,
            ElementType.METHOD,
            ElementType.ANNOTATION_TYPE
    })
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface NotAllEmpties {

        NotAllEmpty[] value();
    }

    /**
     * NotAllEmpty注解的约束校验器.
     *
     * @author Mr.XiHui
     * @date 2018/12/19
     */
    class NotAllEmptyValidator implements ConstraintValidator<NotAllEmpty, Object> {

        private String[] fieldNames;

        @Override
        public void initialize(NotAllEmpty notAllEmpty) {
            this.fieldNames = notAllEmpty.fieldNames();
        }

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {

            //BeanWrapper beanWrapper = new BeanWrapperImpl(value);
            MethodAccessor methodAccessor = Reflector.getMethodAccessor(value.getClass());

            for (int i = 0; i < fieldNames.length; i++) {
                String fieldName = fieldNames[i];
                Object fieldValue;
                try {
                    //fieldValue = beanWrapper.getPropertyValue(fieldName);
                    fieldValue = methodAccessor.getFieldValue(value, fieldName);
                } catch (Exception e) {
                    throw new RuntimeException("找不到@NotAllEmpty注解所校验类("
                            + value.getClass().getSimpleName() + ")对应属性(" + fieldName + ")的get方法");
                }
                if (!ParameterUtils.isEmpty(fieldValue)) {
                    return true;
                }
            }

            return false;
        }
    }
}
