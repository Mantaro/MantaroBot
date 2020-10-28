package net.kodehawa.mantarobot.core.command.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Help {
    String description();
    String usage()
            default "";
    Parameter[] parameters()
            default {};
    String[] related()
            default {};
    boolean seasonal()
            default false;

    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface Parameter {
        String name();
        String description();
        boolean optional()
                default false;
    }
}
