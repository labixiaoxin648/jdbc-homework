package com.example.jdbc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 实体类与数据库表名的映射注解。
 * 不加该注解时，默认把类名按驼峰转下划线规则当作表名。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Table {
    /** 数据库表名 */
    String value();
}
