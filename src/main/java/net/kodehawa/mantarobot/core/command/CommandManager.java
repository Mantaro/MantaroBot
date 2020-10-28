package net.kodehawa.mantarobot.core.command;

import javax.annotation.Nonnull;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CommandManager {
    private final Map<String, NewCommand> commands = new HashMap<>();
    private final Map<String, String> aliases = new HashMap<>();

    public Map<String, NewCommand> commands() {
        return Collections.unmodifiableMap(commands);
    }

    public <T extends NewCommand> T register(@Nonnull Class<T> clazz) {
        return register(instantiate(clazz));
    }

    public <T extends NewCommand> T register(@Nonnull T command) {
        if (commands.putIfAbsent(command.name(), command) != null) {
            throw new IllegalArgumentException("Duplicate command " + command.name());
        }
        for (var alias : command.aliases()) {
            if (aliases.putIfAbsent(alias, command.name()) != null) {
                throw new IllegalArgumentException("Duplicate alias " + alias);
            }
        }
        registerSubcommands(command);
        return command;
    }

    public boolean execute(@Nonnull NewContext ctx) {
        var args = ctx.arguments();
        if (args.hasNext()) {
            var name = args.next().getValue().toLowerCase();
            var child = commands.get(name);
            if (child == null) {
                child = commands.get(aliases.getOrDefault(name, ""));
            }
            if (child != null) {
                child.execute(ctx);
                return true;
            }
        }
        return false;
    }

    private static <T> T instantiate(Class<T> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to instantiate " + clazz, e);
        }
    }
    
    private static void registerSubcommands(NewCommand command) {
        for (var inner : command.getClass().getDeclaredClasses()) {
            if (!NewCommand.class.isAssignableFrom(inner)) continue;
            if (inner.isLocalClass() || inner.isAnonymousClass()) continue;
            if (!Modifier.isStatic(inner.getModifiers())) continue;
            if (Modifier.isAbstract(inner.getModifiers())) continue;
            var sub = (NewCommand)instantiate(inner);
            sub.registerParent(command);
            registerSubcommands(sub);
        }
    }
}
