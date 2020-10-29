/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.core;

import com.google.common.base.Preconditions;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.CustomCmds;
import net.kodehawa.mantarobot.commands.info.stats.CategoryStatsManager;
import net.kodehawa.mantarobot.commands.info.stats.CommandStatsManager;
import net.kodehawa.mantarobot.core.command.CommandManager;
import net.kodehawa.mantarobot.core.command.NewCommand;
import net.kodehawa.mantarobot.core.command.NewContext;
import net.kodehawa.mantarobot.core.command.argument.ArgumentParseError;
import net.kodehawa.mantarobot.core.modules.commands.AliasCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.PremiumKey;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.options.core.Option;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RateLimiter;
import net.kodehawa.mantarobot.utils.exporters.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CommandRegistry {
    private static final Logger log = LoggerFactory.getLogger(CommandRegistry.class);

    private final Map<String, Command> commands;
    private final Config conf = MantaroData.config().get();
    private boolean logCommands = false;

    private final CommandManager newCommands = new CommandManager();
    private final RateLimiter rl = new RateLimiter(TimeUnit.MINUTES, 1);

    public CommandRegistry(Map<String, Command> commands) {
        this.commands = Preconditions.checkNotNull(commands);
    }

    public CommandRegistry() {
        this(new HashMap<>());
    }

    public Map<String, Command> commands() {
        return commands;
    }

    public Map<String, Command> getCommandsForCategory(CommandCategory category) {
        return commands.entrySet().stream()
                .filter(cmd -> cmd.getValue().category() == category)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    //BEWARE OF INSTANCEOF CALLS
    //I know there are better approaches to this, THIS IS JUST A WORKAROUND, DON'T TRY TO REPLICATE THIS.
    public void process(GuildMessageReceivedEvent event, String cmdName, String content, String prefix) {
        final ManagedDatabase managedDatabase = MantaroData.db();
        long start = System.currentTimeMillis();

        Command command = commands.get(cmdName.toLowerCase());

        DBGuild dbg = managedDatabase.getGuild(event.getGuild());
        DBUser dbUser = managedDatabase.getUser(event.getAuthor());
        UserData userData = dbUser.getData();
        GuildData guildData = dbg.getData();

        if (command == null) {
            CustomCmds.handle(prefix, cmdName, new Context(event, new I18nContext(guildData, userData), content), content);
            return;
        }

        if (managedDatabase.getMantaroData().getBlackListedUsers().contains(event.getAuthor().getId())) {
            if (rl.process(event.getAuthor())) {
                event.getChannel().sendMessage(EmoteReference.ERROR + "You have been blacklisted from using all of Mantaro's functions. " +
                        "If you wish to get more details on why, don't hesitate to join the support server and ask, but be sincere."
                ).queue();
            }
            return;
        }

        //Variable used in lambda expression should be final or effectively final...
        final Command cmd = command;

        if (guildData.getDisabledCommands().contains(name(cmd, cmdName))) {
            sendDisabledNotice(event, guildData, CommandDisableLevel.COMMAND);
            return;
        }

        List<String> channelDisabledCommands = guildData.getChannelSpecificDisabledCommands().get(event.getChannel().getId());
        if (channelDisabledCommands != null && channelDisabledCommands.contains(name(cmd, cmdName))) {
            sendDisabledNotice(event, guildData, CommandDisableLevel.COMMAND_SPECIFIC);
            return;
        }

        if (guildData.getDisabledUsers().contains(event.getAuthor().getId()) && isNotAdmin(event.getMember())) {
            sendDisabledNotice(event, guildData, CommandDisableLevel.USER);
            return;
        }

        if (guildData.getDisabledChannels().contains(event.getChannel().getId()) && (root(cmd).category() != CommandCategory.MODERATION)) {
            sendDisabledNotice(event, guildData, CommandDisableLevel.CHANNEL);
            return;
        }

        if (guildData.getDisabledCategories().contains(
                root(cmd).category()
        ) && !cmdName.equalsIgnoreCase("opts")) {
            sendDisabledNotice(event, guildData, CommandDisableLevel.CATEGORY);
            return;
        }

        if (guildData.getChannelSpecificDisabledCategories().computeIfAbsent(event.getChannel().getId(), c ->
                new ArrayList<>()).contains(root(cmd).category())
                && !cmdName.equalsIgnoreCase("opts")) {
            sendDisabledNotice(event, guildData, CommandDisableLevel.SPECIFIC_CATEGORY);
            return;
        }

        if (guildData.getWhitelistedRole() != null) {
            Role whitelistedRole = event.getGuild().getRoleById(guildData.getWhitelistedRole());
            if ((whitelistedRole != null && event.getMember().getRoles().stream().noneMatch(r -> whitelistedRole.getId().equalsIgnoreCase(r.getId())) && isNotAdmin(event.getMember()))) {
                return;
            }
            //else continue.
        }

        if (!guildData.getDisabledRoles().isEmpty() && event.getMember().getRoles().stream().anyMatch(r -> guildData.getDisabledRoles().contains(r.getId())) && isNotAdmin(event.getMember())) {
            sendDisabledNotice(event, guildData, CommandDisableLevel.ROLE);
            return;
        }

        HashMap<String, List<String>> roleSpecificDisabledCommands = guildData.getRoleSpecificDisabledCommands();
        if (event.getMember().getRoles().stream().anyMatch(r -> roleSpecificDisabledCommands.computeIfAbsent(r.getId(), s -> new ArrayList<>())
                .contains(name(cmd, cmdName))) && isNotAdmin(event.getMember())) {
            sendDisabledNotice(event, guildData, CommandDisableLevel.SPECIFIC_ROLE);
            return;
        }

        HashMap<String, List<CommandCategory>> roleSpecificDisabledCategories = guildData.getRoleSpecificDisabledCategories();
        if (event.getMember().getRoles().stream().anyMatch(r -> roleSpecificDisabledCategories.computeIfAbsent(r.getId(), s -> new ArrayList<>())
                .contains(root(cmd).category())) && isNotAdmin(event.getMember())) {
            sendDisabledNotice(event, guildData, CommandDisableLevel.SPECIFIC_ROLE_CATEGORY);
            return;
        }

        //If we are in the patreon bot, deny all requests from unknown guilds.
        if (conf.isPremiumBot() && !conf.isOwner(event.getAuthor()) && !dbg.isPremium()) {
            event.getChannel().sendMessage(EmoteReference.ERROR + "Seems like you're trying to use the Patreon bot when this guild is **not** marked as premium. " +
                    "**If you think this is an error please contact Kodehawa#3457 or poke me on #donators in the support guild**").queue();
            return;
        }

        if (!cmd.permission().test(event.getMember())) {
            event.getChannel().sendMessage(EmoteReference.STOP + "You have no permissions to trigger this command :(").queue();
            return;
        }

        PremiumKey currentKey = managedDatabase.getPremiumKey(userData.getPremiumKey());
        PremiumKey guildKey = managedDatabase.getPremiumKey(guildData.getPremiumKey());

        if (currentKey != null) {
            //10 days before expiration or best fit.
            if (currentKey.validFor() <= 10 && currentKey.validFor() > 1) {
                //Handling is done inside the PremiumKey#renew method. This only gets fired if the key has less than 10 days left.
                if (!currentKey.renew() && !userData.hasReceivedExpirationWarning()) {
                    //Send message if the person can't be seen as a patron. Maybe they're still pledging, or wanna pledge again.
                    event.getAuthor().openPrivateChannel().queue(privateChannel ->
                            privateChannel.sendMessage(EmoteReference.WARNING + "Your premium key is about to run out in **" + Math.max(1, currentKey.validFor()) + " days**!\n" +
                                    EmoteReference.HEART + "*If you're still pledging to Mantaro* you can ask Kodehawa#3457 for a key renewal in the #donators channel. " +
                                    "In the case that you're not longer a patron, you cannot renew, but I sincerely hope you had a good time with the bot and its features! " +
                                    "**If you ever want to pledge again you can check the patreon link at <https://patreon.com/mantaro>**\n\n" +
                                    "Thanks you so much for your support to keep Mantaro alive! It wouldn't be possible without the help of all of you.\n" +
                                    "With love, Kodehawa#3457 " + EmoteReference.HEART + ". This will only be sent once (hopefully).").queue()
                    );
                }

                //Set expiration warning flag to true and save.
                userData.setReceivedExpirationWarning(true);
                dbUser.save();
            }
        }

        //Handling is done inside the PremiumKey#renew method. This only gets fired if the key has less than 10 days left.
        if (guildKey != null) {
            if (guildKey.validFor() <= 10 && guildKey.validFor() > 1) {
                guildKey.renew();
            }
        }

        //COMMAND LOGGING
        long end = System.currentTimeMillis();
        Metrics.COMMAND_COUNTER.labels(name(cmd, cmdName)).inc();

        if (logCommands) {
            log.info("COMMAND INVOKE: command:{}, user:{}#{}, userid:{}, guild:{}, channel:{}",
                    cmdName, event.getAuthor().getName(), event.getAuthor().getDiscriminator(), event.getAuthor().getId(),
                    event.getGuild().getId(), event.getChannel().getId()
            );
        } else {
            log.debug("COMMAND INVOKE: command:{}, user:{}#{}, guild:{}, channel:{}",
                    cmdName, event.getAuthor().getName(), event.getAuthor().getDiscriminator(),
                    event.getGuild().getId(), event.getChannel().getId()
            );
        }

        boolean executedNew;
        try {
            executedNew = newCommands.execute(new NewContext(event.getMessage(),
                    new I18nContext(guildData, userData),
                    event.getMessage().getContentRaw().substring(prefix.length())));
        } catch (ArgumentParseError e) {
            if (e.getMessage() != null) {
                event.getChannel().sendMessage(
                        EmoteReference.ERROR + e.getMessage()
                ).queue();
            } else {
                e.printStackTrace();
                event.getChannel().sendMessage(
                        EmoteReference.ERROR + "There was an error parsing the arguments for this command. Please report this to the developers"
                ).queue();
            }
            return;
        }
        if (!executedNew) {
            cmd.run(new Context(event, new I18nContext(guildData, userData), content), cmdName, content);
        }

        //Logging
        if (root(cmd).category() != null) {
            if (!root(cmd).category().name().isEmpty()) {
                Metrics.CATEGORY_COUNTER.labels(root(cmd).category().name().toLowerCase()).inc();

                CommandStatsManager.log(name(cmd, cmdName));
                CategoryStatsManager.log(root(cmd).category().name().toLowerCase());
            }
        }

        Metrics.COMMAND_LATENCY.observe(end - start);
    }

    public void register(Class<? extends NewCommand> clazz) {
        var cmd = newCommands.register(clazz);
        var p = new ProxyCommand(cmd);
        commands.put(cmd.name(), p);
        cmd.aliases().forEach(a -> commands.put(a, new AliasProxyCommand(p)));
    }

    public <T extends Command> T register(String name, T command) {
        commands.putIfAbsent(name, command);
        log.debug("Registered command " + name);
        return command;
    }

    public void registerAlias(String command, String alias) {
        if (!commands.containsKey(command)) {
            log.error(command + " isn't in the command map...");
        }

        Command parent = commands.get(command);
        if (parent instanceof ProxyCommand) {
            throw new IllegalArgumentException("Use @Alias instead");
        }
        parent.getAliases().add(alias);

        register(alias, new AliasCommand(alias, command, parent));
    }

    private boolean isNotAdmin(Member member) {
        return !CommandPermission.ADMIN.test(member);
    }

    public void sendDisabledNotice(GuildMessageReceivedEvent event, GuildData data, CommandDisableLevel level) {
        if (data.isCommandWarningDisplay() && level != CommandDisableLevel.NONE) {
            event.getChannel().sendMessageFormat("%sThis command is disabled on this server. Reason: %s",
                    EmoteReference.ERROR, Utils.capitalize(level.getName())
            ).queue();
        } //else don't
    }

    public void setLogCommands(boolean logCommands) {
        this.logCommands = logCommands;
    }

    private static String name(Command c, String userInput) {
        if (c instanceof AliasCommand) {
            //Return the original command name here for all intents and purposes.
            //This is because in the check for command disable (which is what this is used for), the
            //command disabled will be the original command, and the check expects that.
            return ((AliasCommand) c).getOriginalName();
        }

        if (c instanceof ProxyCommand) {
            return ((ProxyCommand) c).c.name();
        }

        return userInput.toLowerCase();
    }

    private Command root(Command c) {
        if (c instanceof AliasCommand) {
            return commands.get(((AliasCommand) c).parentName());
        }

        if (c instanceof AliasProxyCommand) {
            return ((AliasProxyCommand) c).p;
        }
        return c;
    }

    private static class ProxyCommand implements Command {
        private final NewCommand c;

        private ProxyCommand(NewCommand c) {
            this.c = c;
        }

        @Override
        public CommandCategory category() {
            return c.category();
        }

        @Override
        public CommandPermission permission() {
            return c.permission();
        }

        @Override
        public void run(Context context, String commandName, String content) {
            throw new UnsupportedOperationException();
        }

        @Override
        public HelpContent help() {
            return c.help();
        }

        @Override
        public Command addOption(String call, Option option) {
            Option.addOption(call, option);
            return this;
        }

        @Override
        public List<String> getAliases() {
            return c.aliases();
        }
    }

    private static class AliasProxyCommand extends ProxyCommand {
        private final ProxyCommand p;
        private AliasProxyCommand(ProxyCommand p) {
            super(p.c);
            this.p = p;
        }

        @Override
        public CommandCategory category() {
            return null;
        }
    }

    //lol @ this
    enum CommandDisableLevel {
        NONE("None"),
        CATEGORY("Disabled category on server"),
        SPECIFIC_CATEGORY("Disabled category on specific channel"),
        COMMAND("Disabled command"),
        COMMAND_SPECIFIC("Disabled command on specific channel"),
        GUILD("Disabled command on this server"),
        ROLE("Disabled role on this server"),
        ROLE_CATEGORY("Disabled role for this category"),
        SPECIFIC_ROLE("Disabled role on this channel"),
        SPECIFIC_ROLE_CATEGORY("Disabled role on this channel for this category"),
        CHANNEL("Disabled channel"),
        USER("Disabled user");

        final String name;

        CommandDisableLevel(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }

}
