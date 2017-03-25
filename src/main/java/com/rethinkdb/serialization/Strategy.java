package com.rethinkdb.serialization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Strategy {
	Class<? extends SerializationStrategy> serialization() default SerializationStrategy.class;

	Class<? extends UnserializationStrategy> unserialization() default UnserializationStrategy.class;
}
