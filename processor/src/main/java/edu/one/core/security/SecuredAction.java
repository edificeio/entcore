package edu.one.core.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.SOURCE)
@Documented
@Target(ElementType.METHOD)
public @interface SecuredAction {

	String value();

}
