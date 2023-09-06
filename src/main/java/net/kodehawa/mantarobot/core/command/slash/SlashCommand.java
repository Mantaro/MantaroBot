/*
 * Copyright (C) 2016 Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.core.command.slash;

import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.command.meta.Description;
import net.kodehawa.mantarobot.core.command.meta.NSFW;
import net.kodehawa.mantarobot.core.command.meta.Options;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public abstract class SlashCommand extends DeferrableCommand<SlashContext> {
    private final String description;
    private final List<OptionData> types = new ArrayList<>();
    private final Map<String, SlashCommand> subCommands = new HashMap<>();
    private Predicate<SlashContext> predicate = c -> true;
    private final boolean nsfw;

    public SlashCommand() {
        super();
        var clazz = getClass();

        // Basically the same as the one above.
        if (clazz.getAnnotation(Description.class) != null) {
            this.description = clazz.getAnnotation(Description.class).value();
        } else {
            this.description = clazz.getSimpleName().toLowerCase();
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

                if (option.choices().length > 0 && option.type().canSupportChoices()) {
                    data.addChoices(Arrays.stream(option.choices()).map(choice -> {
                        if (option.type() == OptionType.NUMBER) {
                            return new Command.Choice(choice.description(), Double.parseDouble(choice.value()));
                        } else if (option.type() == OptionType.INTEGER) {
                            return new Command.Choice(choice.description(), Integer.parseInt(choice.value()));
                        } else {
                            return new Command.Choice(choice.description(), choice.value());
                        }
                    }).toList());
                }

                types.add(data);
            }
        }

        this.nsfw = clazz.getAnnotation(NSFW.class) != null;
    }

    public String getDescription() {
        return description;
    }

    public void addSubCommand(String name, SlashCommand command) {
        subCommands.put(name, command);
    }

    public Map<String, SlashCommand> getSubCommands() {
        return subCommands;
    }

    public boolean isEphemeral() {
        return ephemeral;
    }

    public boolean isNsfw() {
        return nsfw;
    }

    @SuppressWarnings("unused")
    public boolean isOwnerCommand() {
        return getPermission() == CommandPermission.OWNER;
    }

    @SuppressWarnings("unused")
    public List<SlashCommand> getSubCommandList() {
        return new ArrayList<>(subCommands.values());
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

    @SuppressWarnings("unused")
    public void addOption(OptionData data) {
        types.add(data);
    }

    public List<OptionData> getOptions() {
        return types;
    }

    public void setPredicate(Predicate<SlashContext> predicate) {
        this.predicate = predicate;
    }

    // This is to be overriden.
    public Predicate<SlashContext> getPredicate() {
        return predicate;
    }

    @Override
    public final void execute(SlashContext ctx) {
        var sub = getSubCommands().get(ctx.getSubCommand());
        // If this is over 2500ms, we should attempt to defer instead, as discord might be lagging.
        var averageLatencyMax = MantaroBot.getInstance().getCore().getRestPing() * 4;

        // Predicate failure
        if (!getPredicate().test(ctx)) {
            return;
        }

        var forceDefer = averageLatencyMax > 2500 && !modal;
        if (sub != null) {
            if ((sub.defer() || forceDefer) && !sub.modal) {
                if (sub.isEphemeral()) ctx.deferEphemeral();
                else ctx.defer();
            }

            sub.process(ctx);
        } else {
            if ((defer() || forceDefer) && !modal) {
                if (isEphemeral()) ctx.deferEphemeral();
                else ctx.defer();
            }

            process(ctx);
        }
    }
}
