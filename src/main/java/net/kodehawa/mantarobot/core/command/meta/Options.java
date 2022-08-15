package net.kodehawa.mantarobot.core.command.meta;

import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Options {
    Option[] value() default {};

    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface Option {
        OptionType type();
        String name();
        String description();
        boolean required()
                default false;
        int minValue()
                default 1;
        int maxValue()
                default Integer.MAX_VALUE;
        Choice[] choices() default {};
    }
    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface Choice {
        String description();
        String value();
    }
}
