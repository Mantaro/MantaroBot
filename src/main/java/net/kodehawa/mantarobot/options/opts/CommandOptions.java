/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
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
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.options.opts;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.core.entities.ISnowflake;
import net.dv8tion.jda.core.entities.TextChannel;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.processor.DefaultCommandProcessor;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.options.OptionType;
import net.kodehawa.mantarobot.options.annotations.Option;
import net.kodehawa.mantarobot.options.event.OptionRegistryEvent;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.commands.OptsCmd.getOpts;
import static net.kodehawa.mantarobot.commands.OptsCmd.optsCmd;

@Option
public class CommandOptions extends OptionHandler {

    public CommandOptions() {
        setType(OptionType.COMMAND);
    }

    @Subscribe
    public void onRegister(OptionRegistryEvent e) {
        //region disallow
        registerOption("server:command:disallow", "Command disallow",
                "Disallows a command from being triggered at all. Use the command name\n" +
                        "**Example:** `~>opts server command disallow 8ball`",
                "Disallows a command from being triggered at all.", (event, args) -> {
                    if(args.length == 0) {
                        onHelp(event);
                        return;
                    }
                    String commandName = args[0];
                    if(DefaultCommandProcessor.REGISTRY.commands().get(commandName) == null) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "No command called " + commandName).queue();
                        return;
                    }
                    if(commandName.equals("opts") || commandName.equals("help")) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot disable the options or the help command.")
                                .queue();
                        return;
                    }
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    guildData.getDisabledCommands().add(commandName);
                    event.getChannel().sendMessage(EmoteReference.MEGA + "Disabled " + commandName + " on this server.").queue();
                    dbGuild.saveAsync();
                });
        //endregion
        //region allow
        registerOption("server:command:allow", "Command allow",
                "Allows a command from being triggered. Use the command name\n" +
                        "**Example:** `~>opts server command allow 8ball`",
                "Allows a command from being triggered.", (event, args) -> {
                    if(args.length == 0) {
                        onHelp(event);
                        return;
                    }
                    String commandName = args[0];
                    if(DefaultCommandProcessor.REGISTRY.commands().get(commandName) == null) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "No command called " + commandName).queue();
                        return;
                    }
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    guildData.getDisabledCommands().remove(commandName);
                    event.getChannel().sendMessage(EmoteReference.MEGA + "Enabled " + commandName + " on this server.").queue();
                    dbGuild.saveAsync();
                });
        //endregion
        //region specific
        registerOption("server:command:specific:disallow", "Specific command disallow",
                "Disallows a command from being triggered at all in a specific channel. Use the channel **name** and command name\n" +
                        "**Example:** `~>opts server command specific disallow general 8ball`",
                "Disallows a command from being triggered at all in a specific channel.", (event, args) -> {
                    if(args.length == 0) {
                        onHelp(event);
                        return;
                    }

                    if(args.length < 2) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You need to specify the channel name and the command to disalllow!").queue();
                        return;
                    }

                    String channelName = args[0];
                    String commandName = args[1];

                    if(DefaultCommandProcessor.REGISTRY.commands().get(commandName) == null) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "No command called " + commandName).queue();
                        return;
                    }

                    if(event.getGuild().getTextChannelsByName(channelName, true).isEmpty()) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "No channel called " + channelName + " was found. Try again with the correct name.").queue();
                        return;
                    }

                    if(commandName.equals("opts") || commandName.equals("help")) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot disable the options or the help command.")
                                .queue();
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();

                    String id = event.getGuild().getTextChannelsByName(channelName, true).get(0).getId();
                    guildData.getChannelSpecificDisabledCommands().computeIfAbsent(id, k -> new ArrayList<>());

                    guildData.getChannelSpecificDisabledCommands().get(id).add(commandName);

                    event.getChannel().sendMessage(EmoteReference.MEGA + "Disabled " + commandName + " on channel #" + channelName + ".").queue();
                    dbGuild.saveAsync();

                });

        registerOption("server:command:specific:allow", "Specific command allow",
                "Re-allows a command from being triggered in a specific channel. Use the channel **name** and command name\n" +
                        "**Example:** `~>opts server command specific allow general 8ball`",
                "Re-allows a command from being triggered in a specific channel.", ((event, args) -> {
                    if(args.length == 0) {
                        onHelp(event);
                        return;
                    }

                    if(args.length < 2) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You need to specify the channel name and the command to disalllow!").queue();
                        return;
                    }

                    String channelName = args[0];
                    String commandName = args[1];

                    if(DefaultCommandProcessor.REGISTRY.commands().get(commandName) == null) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "No command called " + commandName).queue();
                        return;
                    }

                    if(event.getGuild().getTextChannelsByName(channelName, true).isEmpty()) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "No channel called " + channelName + " was found. Try again with the correct name.").queue();
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    String id = event.getGuild().getTextChannelsByName(channelName, true).get(0).getId();

                    guildData.getChannelSpecificDisabledCommands().computeIfAbsent(id, k -> new ArrayList<>());

                    guildData.getChannelSpecificDisabledCommands().get(id).remove(commandName);

                    event.getChannel().sendMessage(EmoteReference.MEGA + "Enabled " + commandName + " on channel #" + channelName + ".").queue();
                    dbGuild.saveAsync();
                }));
        //endregion
        //region channel
        //region disallow
        registerOption("server:channel:disallow", "Channel disallow",
                "Disallows a channel from commands. Use the channel **name**\n" +
                        "**Example:** `~>opts server channel disallow general`",
                "Disallows a channel from commands.", (event, args) -> {
                    if(args.length == 0) {
                        onHelp(event);
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();

                    if(args[0].equals("*")) {
                        Set<String> allChannelsMinusCurrent = event.getGuild().getTextChannels().
                                stream().filter(textChannel -> textChannel.getId().equals(event.getChannel().getId())).map(ISnowflake::getId).collect(Collectors.toSet());
                        guildData.getDisabledChannels().addAll(allChannelsMinusCurrent);
                        dbGuild.save();
                        event.getChannel().sendMessage(EmoteReference.CORRECT + "Disallowed all channels except the current one. " +
                                "You can start allowing channels one by one again with `opts server channel allow` from **this** channel. " +
                                "You can disallow this channel later if you so desire.").queue();
                        return;
                    }

                    if((guildData.getDisabledChannels().size() + 1) >= event.getGuild().getTextChannels().size()) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot disable more channels since the bot " +
                                "wouldn't be able to talk otherwise :<").queue();
                        return;
                    }
                    List<TextChannel> textChannels = event.getGuild().getTextChannels().stream()
                            .filter(textChannel -> textChannel.getName().contains(args[0]))
                            .collect(Collectors.toList());
                    DiscordUtils.selectList(event, textChannels,
                            textChannel -> String.format("%s (ID: %s)", textChannel.getName(), textChannel.getId()),
                            s -> getOpts().baseEmbed(event, "Select the Channel:").setDescription(s).build(),
                            textChannel -> {
                                guildData.getDisabledChannels().add(textChannel.getId());
                                dbGuild.save();
                                event.getChannel().sendMessage(EmoteReference.OK + "Channel " + textChannel.getAsMention() + " " +
                                        "will not longer listen to commands").queue();
                            }
                    );
                });//endregion

        //region allow
        registerOption("server:channel:allow", "Channel allow",
                "Allows a channel from commands. Use the channel **name**\n" +
                        "**Example:** `~>opts server channel allow general`",
                "Re-allows a channel from commands.", (event, args) -> {
                    if(args.length == 0) {
                        onHelp(event);
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();

                    if(args[0].equals("*")) {
                        guildData.getDisabledChannels().clear();
                        dbGuild.save();
                        event.getChannel().sendMessage(EmoteReference.CORRECT + "All channels are allowed now.").queue();
                        return;
                    }

                    List<TextChannel> textChannels = event.getGuild().getTextChannels().stream()
                            .filter(textChannel -> textChannel.getName().contains(args[0]))
                            .collect(Collectors.toList());
                    DiscordUtils.selectList(event, textChannels,
                            textChannel -> String.format("%s (ID: %s)", textChannel.getName(), textChannel.getId()),
                            s -> ((SimpleCommand) optsCmd).baseEmbed(event, "Select the Channel:").setDescription(s).build(),
                            textChannel -> {
                                guildData.getDisabledChannels().remove(textChannel.getId());
                                dbGuild.save();
                                event.getChannel().sendMessage(EmoteReference.OK + "Channel " + textChannel.getAsMention() + " " +
                                        "will now listen to commands").queue();
                            }
                    );
                });//endregion
        //endregion
        //region category
        registerOption("category:disable", "Disable categories",
                "Disables a specified category.\n" +
                        "If a non-valid category it's specified, it will display a list of valid categories\n" +
                        "You need the category name, for example ` ~>opts category disable Action`",
                "Disables a specified category", (event, args) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You need to specify a category to disable.").queue();
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    Category toDisable = Category.lookupFromString(args[0]);

                    if(toDisable == null) {
                        AtomicInteger at = new AtomicInteger();
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You entered a invalid category. A list of valid categories to disable (case-insensitive) will be shown below"
                                + "```md\n" + Category.getAllNames().stream().map(name -> "#" + at.incrementAndGet() + ". " + name).collect(Collectors.joining("\n")) + "```").queue();
                        return;
                    }

                    if(guildData.getDisabledCategories().contains(toDisable)) {
                        event.getChannel().sendMessage(EmoteReference.WARNING + "This category is already disabled.").queue();
                        return;
                    }

                    if(toDisable.toString().equals("Moderation")) {
                        event.getChannel().sendMessage(EmoteReference.WARNING + "You cannot disable moderation since it contains this command.").queue();
                        return;
                    }

                    guildData.getDisabledCategories().add(toDisable);
                    dbGuild.save();
                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Disabled category `" + toDisable.toString() + "`").queue();
                });

        registerOption("category:enable", "Enable categories",
                "Enables a specified category.\n" +
                        "If a non-valid category it's specified, it will display a list of valid categories\n" +
                        "You need the category name, for example ` ~>opts category enable Action`",
                "Enables a specified category", (event, args) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You need to specify a category to disable.").queue();
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    Category toEnable = Category.lookupFromString(args[0]);

                    if(toEnable == null) {
                        AtomicInteger at = new AtomicInteger();
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You entered a invalid category. A list of valid categories to disable (case-insensitive) will be shown below"
                                + "```md\n" + Category.getAllNames().stream().map(name -> "#" + at.incrementAndGet() + ". " + name).collect(Collectors.joining("\n")) + "```").queue();
                        return;
                    }

                    guildData.getDisabledCategories().remove(toEnable);
                    dbGuild.save();
                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Enabled category `" + toEnable.toString() + "`").queue();
                });
        //region specific
        registerOption("category:specific:disable", "Disable categories on a specific channel",
                "Disables a specified category on a specific channel.\n" +
                        "If a non-valid category it's specified, it will display a list of valid categories\n" +
                        "You need the category name and the channel name, for example ` ~>opts category specific disable Action general`",
                "Disables a specified category", (event, args) -> {
                    if(args.length < 2) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You need to specify a category to disable and the channel where.").queue();
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    Category toDisable = Category.lookupFromString(args[0]);

                    String channelName = args[1];

                    TextChannel selectedChannel;
                    List<TextChannel> channels = event.getGuild().getTextChannelsByName(channelName, true);
                    if(channels.isEmpty()) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "No channel called " + channelName + " was found. Try again with the correct name.").queue();
                        return;
                    }

                    selectedChannel = channels.get(0);

                    if(toDisable == null) {
                        AtomicInteger at = new AtomicInteger();
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You entered a invalid category. A list of valid categories to disable (case-insensitive) will be shown below"
                                + "```md\n" + Category.getAllNames().stream().map(name -> "#" + at.incrementAndGet() + ". " + name).collect(Collectors.joining("\n")) + "```").queue();
                        return;
                    }

                    guildData.getChannelSpecificDisabledCategories().computeIfAbsent(selectedChannel.getId(), uwu -> new ArrayList<>());

                    if(guildData.getChannelSpecificDisabledCategories().get(selectedChannel.getId()).contains(toDisable)) {
                        event.getChannel().sendMessage(EmoteReference.WARNING + "This category is already disabled.").queue();
                        return;
                    }

                    if(toDisable.toString().equals("Moderation")) {
                        event.getChannel().sendMessage(EmoteReference.WARNING + "You cannot disable moderation since it contains this command.").queue();
                        return;
                    }

                    guildData.getChannelSpecificDisabledCategories().get(selectedChannel.getId()).add(toDisable);
                    dbGuild.save();
                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Disabled category `" + toDisable.toString() + "` on channel " + selectedChannel.getAsMention()).queue();

                });

        registerOption("category:specific:enable", "Enable categories on a specific channel",
                "Enables a specified category on a specific channel.\n" +
                        "If a non-valid category it's specified, it will display a list of valid categories\n" +
                        "You need the category name and the channel name, for example ` ~>opts category specific enable Action general`",
                "Enables a specified category", (event, args) -> {
                    if(args.length < 2) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You need to specify a category to disable and the channel where.").queue();
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    Category toEnable = Category.lookupFromString(args[0]);
                    String where = args[1];
                    List<TextChannel> channels = event.getGuild().getTextChannelsByName(where, true);
                    TextChannel selectedChannel;
                    if(channels.isEmpty()) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "No channel called " + where + " was found. Try again with the correct name.").queue();
                        return;
                    }

                    selectedChannel = channels.get(0);

                    if(toEnable == null) {
                        AtomicInteger at = new AtomicInteger();
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You entered a invalid category. A list of valid categories to disable (case-insensitive) will be shown below"
                                + "```md\n" + Category.getAllNames().stream().map(name -> "#" + at.incrementAndGet() + ". " + name).collect(Collectors.joining("\n")) + "```").queue();
                        return;
                    }

                    if(selectedChannel == null) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "That's not a valid channel!").queue();
                        return;
                    }

                    List l = guildData.getChannelSpecificDisabledCategories().computeIfAbsent(selectedChannel.getId(), uwu -> new ArrayList<>());
                    if(l.isEmpty() || !l.contains(toEnable)) {
                        event.getChannel().sendMessage(EmoteReference.THINKING + "This category wasn't enabled?").queue();
                        return;
                    }
                    guildData.getChannelSpecificDisabledCategories().get(selectedChannel.getId()).remove(toEnable);
                    dbGuild.save();
                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Enabled category `" + toEnable.toString() + "` on channel " + selectedChannel.getAsMention()).queue();
                });//endregion
        //endregion
    }

    @Override
    public String description() {
        return "Command related options. Disabling/enabling commands or categories belong here.";
    }

}
