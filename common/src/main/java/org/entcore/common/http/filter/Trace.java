package org.entcore.common.http.filter;

import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE)
@Documented
@Target(ElementType.METHOD)
public @interface Trace {
    String value();
    boolean body() default true;
}