/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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

package net.kodehawa.mantarobot.core;

import com.google.common.base.Preconditions;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.CustomCmds;
import net.kodehawa.mantarobot.commands.info.stats.manager.CategoryStatsManager;
import net.kodehawa.mantarobot.commands.info.stats.manager.CommandStatsManager;
import net.kodehawa.mantarobot.core.modules.commands.AliasCommand;
import net.kodehawa.mantarobot.core.modules.commands.SimpleTreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.PremiumKey;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.*;

@Slf4j
public class CommandRegistry {
    private final Map<String, Command> commands;
    private final Config conf = MantaroData.config().get();

    public CommandRegistry(Map<String, Command> commands) {
        this.commands = Preconditions.checkNotNull(commands);
    }

    public CommandRegistry() {
        this(new HashMap<>());
    }

    public Map<String, Command> commands() {
        return commands;
    }

    //BEWARE OF INSTANCEOF CALLS
    //I know there are better approaches to this, THIS IS JUST A WORKAROUND, DON'T TRY TO REPLICATE THIS.
    public boolean process(GuildMessageReceivedEvent event, String cmdName, String content) {
        long start = System.currentTimeMillis();
        Command command = commands.get(cmdName.toLowerCase());
        if (MantaroData.db().getMantaroData().getBlackListedUsers().contains(event.getAuthor().getId())) {
            return false;
        }

        DBGuild dbg = MantaroData.db().getGuild(event.getGuild());
        DBUser dbUser = MantaroData.db().getUser(event.getAuthor());
        UserData userData = dbUser.getData();
        GuildData data = dbg.getData();

        if (command == null) {
            CustomCmds.handle(cmdName, event, new I18nContext(data, userData), content);
            return false;
        }

        if (!event.getGuild().getSelfMember().getPermissions(event.getChannel()).contains(Permission.MESSAGE_EMBED_LINKS)) {
            event.getChannel().sendMessage(EmoteReference.STOP + "I require the permission ``Embed Links``. " +
                    "All Commands will be refused until you give me that permission.\n" +
                    "http://i.imgur.com/Ydykxcy.gifv Refer to this on instructions on how to give the bot the permissions. " +
                    "Also check all the other roles the bot has have that permissions and remember to check channel-specific permissions. Thanks you.").queue();
            return false;
        }

        //Variable used in lambda expression should be final or effectively final...
        final Command cmd = command;

        if (data.getDisabledCommands().contains(cmd instanceof AliasCommand ? ((AliasCommand) cmd).getOriginalName() : cmdName)) {
            return false;
        }

        List<String> channelDisabledCommands = data.getChannelSpecificDisabledCommands().get(event.getChannel().getId());
        if (channelDisabledCommands != null && channelDisabledCommands.contains(cmd instanceof AliasCommand ? ((AliasCommand) cmd).getOriginalName() : cmdName)) {
            return false;
        }

        if (data.getDisabledUsers().contains(event.getAuthor().getId()) && !isAdmin(event.getMember())) {
            return false;
        }

        if (data.getDisabledChannels().contains(event.getChannel().getId()) && (cmd instanceof AliasCommand ? ((AliasCommand) cmd).parentCategory() != Category.MODERATION : cmd.category() != Category.MODERATION)) {
            return false;
        }

        if (conf.isPremiumBot() && (cmd instanceof AliasCommand ? ((AliasCommand) cmd).parentCategory() == Category.CURRENCY : cmd.category() == Category.CURRENCY)) {
            return false;
        }

        if (data.getDisabledCategories().contains(cmd instanceof AliasCommand ? ((AliasCommand) cmd).parentCategory() : cmd.category())) {
            return false;
        }

        if (data.getChannelSpecificDisabledCategories().computeIfAbsent(event.getChannel().getId(), c ->
                new ArrayList<>()).contains(cmd instanceof AliasCommand ? ((AliasCommand) cmd).parentCategory() : cmd.category())) {
            return false;
        }

        if (data.getWhitelistedRole() != null) {
            Role whitelistedRole = event.getGuild().getRoleById(data.getWhitelistedRole());
            if ((whitelistedRole != null && event.getMember().getRoles().stream().noneMatch(r -> whitelistedRole.getId().equalsIgnoreCase(r.getId())) && !isAdmin(event.getMember()))) {
                return false;
            }

            //else continue.
        }

        if (!data.getDisabledRoles().isEmpty() && event.getMember().getRoles().stream().anyMatch(r -> data.getDisabledRoles().contains(r.getId())) && !isAdmin(event.getMember())) {
            return false;
        }

        HashMap<String, List<String>> roleSpecificDisabledCommands = data.getRoleSpecificDisabledCommands();
        if (event.getMember().getRoles().stream().anyMatch(r -> roleSpecificDisabledCommands.computeIfAbsent(r.getId(), s -> new ArrayList<>()).contains(cmd instanceof AliasCommand ? ((AliasCommand) cmd).getOriginalName() : cmdName)) && !isAdmin(event.getMember())) {
            return false;
        }

        HashMap<String, List<Category>> roleSpecificDisabledCategories = data.getRoleSpecificDisabledCategories();
        if (event.getMember().getRoles().stream().anyMatch(r -> roleSpecificDisabledCategories.computeIfAbsent(r.getId(), s -> new ArrayList<>()).contains(cmd instanceof AliasCommand ? ((AliasCommand) cmd).parentCategory() : cmd.category())) && !isAdmin(event.getMember())) {
            return false;
        }

        //If we are in the patreon bot, deny all requests from unknown guilds.
        if (conf.isPremiumBot() && !conf.isOwner(event.getAuthor()) && !dbg.isPremium()) {
            event.getChannel().sendMessage(EmoteReference.ERROR + "Seems like you're trying to use the Patreon bot when this guild is **not** marked as premium. " +
                    "**If you think this is an error please contact Kodehawa#3457 or poke me on #donators in the support guild**").queue();
            return false;
        }

        if (!cmd.permission().test(event.getMember())) {
            event.getChannel().sendMessage(EmoteReference.STOP + "You have no permissions to trigger this command :(").queue();
            return false;
        }

        PremiumKey currentKey = MantaroData.db().getPremiumKey(userData.getPremiumKey());
        if(currentKey != null) {
            //10 days before expiration or best fit.
            if(currentKey.validFor() < 10 && !userData.isReceivedExpirationWarning()) {
                event.getAuthor().openPrivateChannel().queue(privateChannel ->
                        privateChannel.sendMessage(EmoteReference.WARNING + "Your premium key is about to run out in " + currentKey.validFor() + "days!\n" +
                                EmoteReference.HEART + "*If you're still pledging to Mantaro* you can ask Kodehawa#3457 for a key renewal in the #donators channel. " +
                                "In the case that you're not longer a patron, you cannot renew, but I sincerely hope you had a good time with the bot and its features! " +
                                "**If you ever want to pledge again you can check the patreon link at <https://patreon.com/mantaro>**\n\n" +
                                "Thanks you so much for your support to keep Mantaro alive! It wouldn't be possible without the help of all of you.\n" +
                                "With love, Kodehawa#3457 " + EmoteReference.HEART).queue()
                );

                //Set expiration warning flag to true and save.
                userData.setReceivedExpirationWarning(true);
                dbUser.save();
            }
        }

        long end = System.currentTimeMillis();
        MantaroBot.getInstance().getStatsClient().increment("commands");
        log.debug("Command invoked: {}, by {}#{} with timestamp {}", cmdName, event.getAuthor().getName(), event.getAuthor().getDiscriminator(), new Date(System.currentTimeMillis()));
        cmd.run(event, new I18nContext(data, userData), cmdName, content);

        //Logging
        if(cmd.category() != null && cmd.category().name() != null && !cmd.category().name().isEmpty()) {
            MantaroBot.getInstance().getStatsClient().increment("command", "name:" + cmdName);
            MantaroBot.getInstance().getStatsClient().increment("category", "name:" + cmd.category().name().toLowerCase());

            JDA.ShardInfo shardInfo = event.getJDA().getShardInfo();
            int shardId = shardInfo == null ? 0 : shardInfo.getShardId();

            MantaroBot.getInstance().getStatsClient().increment("shard_command", "id:" + shardId);

            CommandStatsManager.log(cmdName);
            CategoryStatsManager.log(cmd.category().name().toLowerCase());
        }

        MantaroBot.getInstance().getStatsClient().histogram("command_process_time", (end - start));

        return true;
    }

    public Command register(String name, Command command) {
        commands.putIfAbsent(name, command);
        log.debug("Registered command " + name);
        return command;
    }

    public void registerAlias(String command, String alias) {
        if(!commands.containsKey(command)) {
            log.error(command + " isn't in the command map...");
        }

        register(alias, new AliasCommand(alias, command, commands.get(command)));
    }

    public void addSubCommandTo(TreeCommand command, String name, SubCommand subCommand) {
        command.addSubCommand(name, subCommand);
    }

    public void addSubCommandTo(SimpleTreeCommand command, String name, SubCommand subCommand) {
        command.addSubCommand(name, subCommand);
    }

    private boolean isAdmin(Member member) {
        return CommandPermission.ADMIN.test(member);
    }
}
