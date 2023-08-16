package org.entcore.common.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(JSONInherits.class)
public @interface JSONInherit
{
    String field();
    String rename() default "";
    String defaultValue() default "null";
    String transform() default "";
}
