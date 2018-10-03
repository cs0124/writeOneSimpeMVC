package com.itcast.mvcframework.anotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * @author chishan
 *
 */
@Target({ElementType.FIELD})

@Retention(RetentionPolicy.RUNTIME)

@Documented
public @interface GPAutowired {

	String value() default "";
}
