package net.kodehawa.discord.Mantaro.annotation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;

@Retention(RUNTIME)
public @interface ModuleProperties {
	
	String level();
	String name();
	String type();
	String description();
	String additionalInfo() default "";
	boolean takesArgs() default false;
}
