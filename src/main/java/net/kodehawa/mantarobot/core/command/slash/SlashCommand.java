package net.kodehawa.mantarobot.core.command.slash;

import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.kodehawa.mantarobot.core.command.meta.*;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;

import java.util.*;

public abstract class SlashCommand {
    private final String name;
    private final String description;
    private final List<OptionData> types = new ArrayList<>();
    // Slash sub-commands work a little differently from non-slash, though.
    private final Map<String, SlashSubCommand> subCommands = new HashMap<>();
    private final CommandCategory category;
    private final CommandPermission permission;
    private final boolean guildOnly;
    private final HelpContent help;

    // This is basically the same as NewCommand, but the handling ought to be different everywhere else.
    // There's no aliases either, too little slots.
    public SlashCommand() {
        var clazz = getClass();
        if (clazz.getAnnotation(Name.class) != null) {
            this.name = clazz.getAnnotation(Name.class).value();
        } else {
            this.name = clazz.getSimpleName().toLowerCase();
        }

        // Basically the same as the one above.
        if (clazz.getAnnotation(Description.class) != null) {
            this.description = clazz.getAnnotation(Description.class).value();
        } else {
            this.description = clazz.getSimpleName().toLowerCase();
        }

        var c = clazz.getAnnotation(Category.class);
        if (c == null) {
            this.category = null;
        } else {
            this.category = c.value();
        }

        var p = clazz.getAnnotation(Permission.class);
        if (p == null) {
            this.permission = CommandPermission.USER;
        } else {
            this.permission = p.value();
        }

        var o = clazz.getAnnotation(Options.class);
        if (o != null) {
            for (var option : o.value()) {
                types.add(
                        new OptionData(option.type(), option.name(), option.description())
                                .setMinValue(option.minValue())
                                .setMaxValue(option.maxValue())
                                .setRequired(option.required())
                );
            }
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

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, SlashSubCommand> getSubCommands() {
        return subCommands;
    }

    public List<SlashSubCommand> getSubCommandList() {
        return new ArrayList<>(getSubCommands().values());
    }

    // This is slow, but it's only called once per command.
    public List<SubcommandData> getSubCommandsRaw() {
        List<SubcommandData> temp = new ArrayList<>();
        for (var sub : getSubCommandList()) {
            temp.add(sub.getData());
        }

        return temp;
    }

    public CommandCategory getCategory() {
        return category;
    }

    public CommandPermission getPermission() {
        return permission;
    }

    public boolean isGuildOnly() {
        return guildOnly;
    }

    public HelpContent getHelp() {
        return help;
    }

    public void addOption(OptionData data) {
        types.add(data);
    }

    public List<OptionData> getOptions() {
        return types;
    }

    protected abstract void process(SlashContext ctx);

    public final void execute(SlashContext ctx) {
        var sub = getSubCommands().get(ctx.getSubCommand());
        if (sub != null) {
            sub.process(ctx);
        } else {
            process(ctx);
        }
    }
}
