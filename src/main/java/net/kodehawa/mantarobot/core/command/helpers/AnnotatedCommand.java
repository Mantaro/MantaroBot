package net.kodehawa.mantarobot.core.command.helpers;

import net.dv8tion.jda.api.EmbedBuilder;
import net.kodehawa.mantarobot.core.command.helpers.CommandCategory;
import net.kodehawa.mantarobot.core.command.helpers.CommandPermission;
import net.kodehawa.mantarobot.core.command.helpers.HelpContent;
import net.kodehawa.mantarobot.core.command.meta.Category;
import net.kodehawa.mantarobot.core.command.meta.Help;
import net.kodehawa.mantarobot.core.command.meta.Name;
import net.kodehawa.mantarobot.core.command.meta.Permission;
import net.kodehawa.mantarobot.core.command.helpers.IContext;

import java.util.Arrays;

public abstract class AnnotatedCommand<T extends IContext> {
    protected CommandCategory category;

    protected final String name;
    protected final CommandPermission permission;
    protected HelpContent help;

    public AnnotatedCommand() {
        var clazz = getClass();
        if (clazz.getAnnotation(Name.class) != null) {
            this.name = clazz.getAnnotation(Name.class).value();
        } else {
            this.name = clazz.getSimpleName().toLowerCase();
        }
        var c = clazz.getAnnotation(Category.class);
        if (c == null) {
            this.category = null;
        } else {
            this.category = c.value();
        }

        var p = clazz.getAnnotation(Permission.class);
        if (p == null) {
            this.permission = getDefaultPermission();
        } else {
            this.permission = p.value();
        }

        var h = clazz.getAnnotation(Help.class);
        if (h == null) {
            this.help = new HelpContent.Builder().build();
        } else {
            var builder = new HelpContent.Builder()
                    .setDescription(h.description().isBlank() ? null : h.description())
                    .setUsage(h.usage().isBlank() ? null : h.usage())
                    .setRelated(Arrays.asList(h.related()))
                    .setParameters(Arrays.asList(h.parameters()))
                    .setSeasonal(h.seasonal());
            this.help = builder.build();
        }
    }

    protected CommandPermission getDefaultPermission() {
        return CommandPermission.USER;
    }

    public String getName() {
        return name;
    }

    public CommandCategory getCategory() {
        return category;
    }

    public CommandPermission getPermission() {
        return permission;
    }

    public HelpContent getHelp() {
        return help;
    }

    @SuppressWarnings("unused")
    public abstract void execute(T ctx);
    protected abstract void process(T ctx);

    public void setCategory(CommandCategory category) {
        this.category = category;
    }

    public void setHelp(HelpContent help) {
        this.help = help;
    }

    protected EmbedBuilder baseEmbed(T ctx, String name) {
        return baseEmbed(ctx, name, ctx.getMember().getEffectiveAvatarUrl());
    }

    protected EmbedBuilder baseEmbed(T ctx, String name, String image) {
        return new EmbedBuilder()
                .setAuthor(name, null, image)
                .setColor(ctx.getMember().getColor())
                .setFooter("Requested by: %s".formatted(ctx.getMember().getEffectiveName()),
                        ctx.getGuild().getIconUrl()
                );
    }
}
