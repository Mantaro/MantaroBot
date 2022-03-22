package net.kodehawa.mantarobot.core.command.slash;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.kodehawa.mantarobot.core.command.meta.*;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;

import java.util.*;
import java.util.function.Predicate;

public abstract class SlashCommand {
    private final String name;
    private final String description;
    private final List<OptionData> types = new ArrayList<>();
    private final Map<String, SlashCommand> subCommands = new HashMap<>();
    private CommandCategory category;
    private final CommandPermission permission;
    private Predicate<SlashContext> predicate = c -> true;
    private final boolean guildOnly;
    private HelpContent help;

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
                OptionData data = new OptionData(option.type(), option.name(), option.description())
                        .setRequired(option.required());

                if (option.type() == OptionType.INTEGER || option.type() == OptionType.NUMBER) {
                    data.setMinValue(option.minValue())
                            .setMaxValue(option.maxValue());
                }

                types.add(data);
            }
        }

        this.guildOnly = clazz.getAnnotation(GuildOnly.class) != null;
        var h = clazz.getAnnotation(Help.class);
        if (h == null) {
            this.help = new HelpContent.Builder().build();
        } else {
            var builder = new HelpContent.Builder()
                    .setDescription(h.description().isBlank() ? this.description : h.description())
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

    public SlashCommand addSubCommand(String name, SlashCommand command) {
        return subCommands.put(name, command);
    }

    public Map<String, SlashCommand> getSubCommands() {
        return subCommands;
    }

    public boolean isOwnerCommand() {
        return getPermission() == CommandPermission.OWNER;
    }

    public List<SlashCommand> getSubCommandList() {
        return new ArrayList<>(subCommands.values());
    }

    // Needed for subcommands
    public void setCategory(CommandCategory category) {
        this.category = category;
    }

    // This is slow, but it's only called once per command.
    public List<SubcommandData> getSubCommandsRaw() {
        List<SubcommandData> temp = new ArrayList<>();
        for (var sub : subCommands.values()) {
            var subObj = new SubcommandData(sub.getName(), "[%s] %s".formatted(sub.getCategory().readableName(), sub.getDescription()))
                    .addOptions(sub.getOptions());

            temp.add(subObj);
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

    public void setHelp(HelpContent help) {
        this.help = help;
    }

    public void setPredicate(Predicate<SlashContext> predicate) {
        this.predicate = predicate;
    }

    // This is to be overriden.
    public Predicate<SlashContext> getPredicate() {
        return predicate;
    }

    protected abstract void process(SlashContext ctx);

    protected EmbedBuilder baseEmbed(SlashContext ctx, String name) {
        return baseEmbed(ctx, name, ctx.getMember().getEffectiveAvatarUrl());
    }

    protected EmbedBuilder baseEmbed(SlashContext ctx, String name, String image) {
        return new EmbedBuilder()
                .setAuthor(name, null, image)
                .setColor(ctx.getMember().getColor())
                .setFooter("Requested by: %s".formatted(ctx.getMember().getEffectiveName()),
                        ctx.getGuild().getIconUrl()
                );
    }

    public final void execute(SlashContext ctx) {
        var sub = getSubCommands().get(ctx.getSubCommand());
        if (!getPredicate().test(ctx)) return;

        if (sub != null) {
            sub.process(ctx);
        } else {
            process(ctx);
        }
    }
}
