package net.kodehawa.discord.Mantaro.annotation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;

@Retention(RUNTIME)
public @interface Metadata {
	
	String date();
	String build();
	String credits();
}
