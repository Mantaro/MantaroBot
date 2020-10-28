package net.kodehawa.mantarobot.core.command;

import net.kodehawa.mantarobot.core.command.meta.*;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;

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
    private final CommandCategory category;
    private final CommandPermission permission;
    private final boolean guildOnly;
    private final HelpContent help;
    private NewCommand parent;

    public NewCommand() {
        var clazz = getClass();
        if (clazz.getAnnotation(Name.class) != null) {
            this.name = clazz.getAnnotation(Name.class).value();
        } else {
            this.name = clazz.getSimpleName().toLowerCase();
        }
        this.aliases = Arrays.stream(clazz.getAnnotationsByType(Alias.class))
                .map(Alias::value)
                .collect(Collectors.toUnmodifiableList());
        var c = clazz.getAnnotation(net.kodehawa.mantarobot.core.command.meta.Category.class);
        if (c == null) {
            this.category = null;
        } else {
            this.category = c.value();
        }
        var p = clazz.getAnnotation(Permission.class);
        if (p == null) {
            this.permission = CommandPermission.INHERIT;
        } else {
            this.permission = p.value();
        }
        this.guildOnly = clazz.getAnnotation(GuildOnly.class) != null;
        var h = clazz.getAnnotation(Help.class);
        if (h == null) {
            this.help = new HelpContent.Builder().build();
        } else {
            var builder = new HelpContent.Builder()
                    .setDescription(h.description().isBlank() ? null : h.description())
                    .setUsage(h.usage().isBlank() ? null : h.usage())
                    .setRelated(Arrays.asList(h.related()))
                    .setSeasonal(h.seasonal());
            for (var param : h.parameters()) {
                if (param.optional()) {
                    builder.addParameterOptional(param.name(), param.description());
                } else {
                    builder.addParameter(param.name(), param.description());
                }
            }
            this.help = builder.build();
        }
    }

    public String name() {
        return name;
    }

    public List<String> aliases() {
        return aliases;
    }

    public CommandCategory category() {
        return category;
    }

    public CommandPermission permission() {
        if (permission != CommandPermission.INHERIT) {
            return permission;
        }
        if (parent == null) {
            return category == null ? CommandPermission.OWNER : category.permission;
        }
        return parent.permission();
    }

    public boolean guildOnly() {
        return guildOnly || (parent != null && parent.guildOnly());
    }

    public HelpContent help() {
        return help;
    }

    public final void execute(NewContext ctx) {
        var args = ctx.arguments();
        if (args.hasNext()) {
            var name = args.next().getValue().toLowerCase();
            var child = children.get(name);
            if (child == null) {
                child = children.get(childrenAliases.getOrDefault(name, ""));
            }
            if (child != null) {
                child.execute(ctx);
                return;
            }
            args.back();
        }
        process(ctx);
    }

    void registerParent(NewCommand parent) {
        this.parent = parent;
        parent.children.put(name, this);
        aliases.forEach(a -> parent.childrenAliases.put(a, name));
    }

    protected abstract void process(NewContext ctx);
}
