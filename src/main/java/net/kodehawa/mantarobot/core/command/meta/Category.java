package net.kodehawa.mantarobot.core.command.meta;

import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Category {
    CommandCategory value();
}
