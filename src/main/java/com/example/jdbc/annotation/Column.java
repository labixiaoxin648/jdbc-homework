package com.example.jdbc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 实体属性与数据库字段的映射注解。
 * 不加该注解时，默认把属性名按驼峰转下划线规则当作字段名（如 enrollmentTime -> enrollment_time）。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {
    /** 数据库字段名 */
    String value();
}
