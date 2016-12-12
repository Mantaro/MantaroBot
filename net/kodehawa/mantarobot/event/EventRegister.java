package net.kodehawa.mantarobot.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventRegister {
	Class<? extends Event> event() default Event.class;
	String name();
	EventPriority priority();
}
