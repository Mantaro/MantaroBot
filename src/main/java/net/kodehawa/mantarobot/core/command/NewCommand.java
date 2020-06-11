package net.kodehawa.mantarobot.core.command;

import net.kodehawa.mantarobot.core.command.meta.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// common superclass for either commands or options
public abstract class NewCommand {
    private final Map<String, NewCommand> children = new HashMap<>();
    private final Map<String, String> childrenAliases = new HashMap<>();
    private final String name;
    private final List<String> aliases;
    private final boolean guildOnly;
    private NewCommand parent;

    public NewCommand() {
        var clazz = getClass();
        if(clazz.getAnnotation(Name.class) != null) {
            this.name = clazz.getAnnotation(Name.class).value();
        } else {
            this.name = clazz.getSimpleName().toLowerCase();
        }
        this.aliases = Arrays.stream(clazz.getAnnotationsByType(Alias.class))
                .map(Alias::value)
                .collect(Collectors.toUnmodifiableList());
        this.guildOnly = getClass().getAnnotation(GuildOnly.class) != null;
    }

    public String name() {
        return name;
    }

    public List<String> aliases() {
        return aliases;
    }

    public boolean guildOnly() {
        return guildOnly || (parent != null && parent.guildOnly());
    }

    public final void execute(NewContext ctx) {
        var args = ctx.arguments();
        if(args.hasNext()) {
            var name = args.next().getValue();
            var child = children.get(name);
            if(child == null) {
                child = children.get(childrenAliases.getOrDefault(name, ""));
            }
            if(child != null) {
                child.execute(ctx);
                return;
            }
        }
        args.back();
        process(ctx);
    }

    void registerParent(NewCommand parent) {
        this.parent = parent;
        parent.children.put(name, this);
        aliases.forEach(a -> parent.childrenAliases.put(a, name));
    }

    protected abstract void process(NewContext ctx);
}
