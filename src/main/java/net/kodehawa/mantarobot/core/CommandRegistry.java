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

package net.kodehawa.mantarobot.core;

import com.google.common.base.Preconditions;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.kodehawa.mantarobot.commands.CustomCmds;
import net.kodehawa.mantarobot.core.command.CommandManager;
import net.kodehawa.mantarobot.core.command.NewCommand;
import net.kodehawa.mantarobot.core.command.NewContext;
import net.kodehawa.mantarobot.core.command.argument.ArgumentParseError;
import net.kodehawa.mantarobot.core.command.slash.ContextCommand;
import net.kodehawa.mantarobot.core.command.slash.InteractionContext;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
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
import net.kodehawa.mantarobot.db.entities.MongoGuild;
import net.kodehawa.mantarobot.db.entities.MongoUser;
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
    private static final Logger commandLog = LoggerFactory.getLogger("command-log");
    private static final Logger log = LoggerFactory.getLogger(CommandRegistry.class);

    private final Map<String, Command> commands;
    private final Config config = MantaroData.config().get();
    private final CommandManager newCommands = new CommandManager();
    private final RateLimiter rl = new RateLimiter(TimeUnit.HOURS, 1);

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

    public Map<String, SlashCommand> getSlashCommandsForCategory(CommandCategory category) {
        return getCommandManager().slashCommands().entrySet().stream()
                .filter(cmd -> cmd.getValue().getCategory() == category)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    // Process non-slash commands.
    // We filter non-guild events early on.
    public void process(MessageReceivedEvent event, MongoGuild dbGuild, String cmdName, String content, String prefix, boolean isMention) {
        if (cmdName.length() >= 50) {
            return;
        }

        final var managedDatabase = MantaroData.db();
        final var start = System.currentTimeMillis();
        var command = commands.get(cmdName.toLowerCase());

        if (command == null) {
            // We will create a proper I18nContext once the custom command goes through, if it does. We don't need it otherwise.
            CustomCmds.handle(prefix, cmdName, new Context(event, new I18nContext(), content, isMention), dbGuild, content);
            return;
        }

        final var author = event.getAuthor();
        final var channel = event.getChannel();
        // Variable used in lambda expression should be final or effectively final...
        final var cmd = command;
        final var guild = event.getGuild();
        final var mantaroData = managedDatabase.getMantaroData();

        if (mantaroData.getBlackListedGuilds().contains(guild.getId())) {
            log.debug("Got command from blacklisted guild {}, dropping", guild.getId());
            return;
        }

        // !! Permission check start
        if (dbGuild.getDisabledCommands().contains(name(cmd, cmdName))) {
            sendDisabledNotice(event, dbGuild, CommandDisableLevel.COMMAND);
            return;
        }

        final var member = event.getMember();
        if (member == null) { // Shouldn't be possible at this stage?
            return;
        }

        final var roles = member.getRoles();
        final var channelDisabledCommands = dbGuild.getChannelSpecificDisabledCommands().get(channel.getId());
        if (channelDisabledCommands != null && channelDisabledCommands.contains(name(cmd, cmdName))) {
            sendDisabledNotice(event, dbGuild, CommandDisableLevel.COMMAND_SPECIFIC);
            return;
        }

        if (dbGuild.getDisabledUsers().contains(author.getId()) && isNotAdmin(member)) {
            sendDisabledNotice(event, dbGuild, CommandDisableLevel.USER);
            return;
        }

        var isOptions = cmdName.equalsIgnoreCase("opts");
        if (dbGuild.getDisabledChannels().contains(channel.getId()) && !isOptions) {
            sendDisabledNotice(event, dbGuild, CommandDisableLevel.CHANNEL);
            return;
        }

        if (dbGuild.getDisabledCategories().contains(root(cmd).category()) && !isOptions) {
            sendDisabledNotice(event, dbGuild, CommandDisableLevel.CATEGORY);
            return;
        }

        if (dbGuild.getChannelSpecificDisabledCategories().computeIfAbsent(
                channel.getId(), c -> new ArrayList<>()).contains(root(cmd).category()) && !isOptions) {
            sendDisabledNotice(event, dbGuild, CommandDisableLevel.SPECIFIC_CATEGORY);
            return;
        }

        if (!dbGuild.getDisabledRoles().isEmpty() && roles.stream().anyMatch(
                r -> dbGuild.getDisabledRoles().contains(r.getId())) && isNotAdmin(member)) {
            sendDisabledNotice(event, dbGuild, CommandDisableLevel.ROLE);
            return;
        }

        final var roleSpecificDisabledCommands = dbGuild.getRoleSpecificDisabledCommands();
        if (roles.stream().anyMatch(r -> roleSpecificDisabledCommands.computeIfAbsent(
                r.getId(), s -> new ArrayList<>()).contains(name(cmd, cmdName))) && isNotAdmin(member)) {
            sendDisabledNotice(event, dbGuild, CommandDisableLevel.SPECIFIC_ROLE);
            return;
        }

        final var roleSpecificDisabledCategories = dbGuild.getRoleSpecificDisabledCategories();
        if (roles.stream().anyMatch(r -> roleSpecificDisabledCategories.computeIfAbsent(
                r.getId(), s -> new ArrayList<>()).contains(root(cmd).category())) && isNotAdmin(member)) {
            sendDisabledNotice(event, dbGuild, CommandDisableLevel.SPECIFIC_ROLE_CATEGORY);
            return;
        }

        if (mantaroData.getBlackListedUsers().contains(author.getId())) {
            if (!rl.process(author)) {
                return;
            }

            channel.sendMessage("""
                    :x: You have been blocked from using all of Mantaro's functions, likely for botting or hitting the spam filter.
                    If you wish to get more details on why or appeal the ban, send an email to `contact@mantaro.site`. Make sure to be sincere.
                    """
            ).queue();
            return;
        }

        // If we are in the patreon bot, deny all requests from unknown guilds.
        if (config.isPremiumBot() && !config.isOwner(author) && !dbGuild.isPremium()) {
            channel.sendMessage("""
                            :x: Seems like you're trying to use the Patreon bot when this guild is **not** marked as premium.
                            **If you think this is an error please contact Kodehawa#3457 or poke me on #donators in the support guild**
                            If you didn't contact Kodehawa prior to adding this bot to this server, please do so so we can link it to your pledge.
                            """
            ).queue();
            return;
        }

        if (!cmd.permission().test(member)) {
            channel.sendMessage(EmoteReference.STOP + "You have no permissions to trigger this command :(").queue();
            return;
        }
        // !! Permission check end

        final var dbUser = managedDatabase.getUser(author);
        renewPremiumKey(managedDatabase, author, dbUser, dbGuild);

        // Used a command on the new system?
        // sort-of-fix: remove if statement when we port all commands
        boolean executedNew;
        try {
            executedNew = newCommands.execute(new NewContext(event,
                    new I18nContext(dbGuild, dbUser),
                    event.getMessage().getContentRaw().substring(prefix.length()))
            );
        } catch (ArgumentParseError e) {
            if (e.getMessage() != null) {
                channel.sendMessage(EmoteReference.ERROR + e.getMessage()).queue();
            } else {
                e.printStackTrace();
                channel.sendMessage(
                        EmoteReference.ERROR + "There was an error parsing the arguments for this command. Please report this to the developers"
                ).queue();
            }

            return;
        }

        if (!executedNew) {
            cmd.run(new Context(event, new I18nContext(dbGuild, dbUser), cmdName, content, isMention), cmdName, content);
        }

        commandLog.debug("Command: {}, User: {} ({}), Guild: {}, Channel: {}, Message: {}" ,
                cmdName, author.getName(), author.getId(), guild.getId(), channel.getId(), event.getMessage().getId()
        );

        final var end = System.currentTimeMillis();
        final var category = root(cmd).category() == null ? "custom" : root(cmd).category().name().toLowerCase();

        Metrics.CATEGORY_COUNTER.labels(category).inc();
        Metrics.COMMAND_COUNTER.labels(name(cmd, cmdName)).inc();
        Metrics.COMMAND_LATENCY.observe(end - start);
    }

    // Process (user) context interaction.
    public void process(UserContextInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("This bot does not accept commands in Private Messages. You can add it to your server at https://add.mantaro.site")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        var start = System.currentTimeMillis();
        var cmd = getCommandManager().contextUserCommands().get(event.getFullCommandName());
        if (cmd == null) {
            return;
        }

        final var managedDatabase = MantaroData.db();
        final var mantaroData = managedDatabase.getMantaroData();
        final var guild = event.getGuild();

        if (mantaroData.getBlackListedGuilds().contains(guild.getId())) {
            log.debug("Got command from blacklisted guild {}, dropping", guild.getId());
            event.reply("Not accepting commands from this server.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (!cmd.getPermission().test(event.getMember())) {
            event.reply(EmoteReference.STOP + "You have no permissions to trigger this command :(")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        final var author = event.getUser();
        final var dbGuild = managedDatabase.getGuild(event.getGuild());
        // If we are in the patreon bot, deny all requests from unknown guilds.
        if (config.isPremiumBot() && !config.isOwner(author) && !dbGuild.isPremium()) {
            event.reply("""
                            :x: Seems like you're trying to use the Patreon bot when this guild is **not** marked as premium.
                            **If you think this is an error please contact Kodehawa#3457 or poke me on #donators in the support guild**
                            If you didn't contact Kodehawa prior to adding this bot to this server, please do so so we can link it to your pledge.
                            """
            ).setEphemeral(true).queue();
            return;
        }

        final var dbUser = managedDatabase.getUser(author);
        cmd.execute(new InteractionContext<>(event, new I18nContext(dbGuild, dbUser)));
        commandLog.debug("Context (user) command: {}, User: {} ({}), Guild: {}" ,
                cmd.getName(), author.getName(), author.getId(), guild.getId()
        );

        final var end = System.currentTimeMillis();
        Metrics.COMMAND_COUNTER.labels(cmd.getName() + "-context").inc();
        Metrics.COMMAND_LATENCY.observe(end - start);
    }

    // Process slash commands.
    public void process(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("This bot does not accept commands in Private Messages. You can add it to your server at https://add.mantaro.site")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        final var start = System.currentTimeMillis();
        var command = getCommandManager().slashCommands().get(event.getName().toLowerCase());

        // Only process custom commands outside slash.
        if (command == null) {
            return;
        }

        final var managedDatabase = MantaroData.db();
        final var mantaroData = managedDatabase.getMantaroData();
        final var guild = event.getGuild();

        if (mantaroData.getBlackListedGuilds().contains(guild.getId())) {
            log.debug("Got command from blacklisted guild {}, dropping", guild.getId());
            event.reply("Not accepting commands from this server.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        final var author = event.getUser();
        final var channel = event.getChannel();
        // Variable used in lambda expression should be final or effectively final...
        final var cmd = command;
        final var name = cmd.getName();
        final var dbGuild = managedDatabase.getGuild(event.getGuild());

        // !! Permission check start
        if (dbGuild.getDisabledCommands().contains(name)) {
            sendDisabledNotice(event, CommandDisableLevel.COMMAND);
            return;
        }

        final var member = event.getMember();
        if (member == null) {
            return;
        }

        final var roles = member.getRoles();
        final var channelDisabledCommands = dbGuild.getChannelSpecificDisabledCommands().get(channel.getId());
        if (channelDisabledCommands != null && channelDisabledCommands.contains(name)) {
            sendDisabledNotice(event, CommandDisableLevel.COMMAND_SPECIFIC);
            return;
        }

        if (dbGuild.getDisabledUsers().contains(author.getId()) && isNotAdmin(member)) {
            sendDisabledNotice(event, CommandDisableLevel.USER);
            return;
        }
        if (dbGuild.getDisabledChannels().contains(channel.getId())) {
            sendDisabledNotice(event, CommandDisableLevel.CHANNEL);
            return;
        }

        if (dbGuild.getDisabledCategories().contains(cmd.getCategory())) {
            sendDisabledNotice(event, CommandDisableLevel.CATEGORY);
            return;
        }

        if (dbGuild.getChannelSpecificDisabledCategories().computeIfAbsent(
                channel.getId(), c -> new ArrayList<>()).contains(cmd.getCategory())) {
            sendDisabledNotice(event, CommandDisableLevel.SPECIFIC_CATEGORY);
            return;
        }

        if (!dbGuild.getDisabledRoles().isEmpty() && roles.stream().anyMatch(
                r -> dbGuild.getDisabledRoles().contains(r.getId())) && isNotAdmin(member)) {
            sendDisabledNotice(event, CommandDisableLevel.ROLE);
            return;
        }

        final var roleSpecificDisabledCommands = dbGuild.getRoleSpecificDisabledCommands();
        if (roles.stream().anyMatch(r -> roleSpecificDisabledCommands.computeIfAbsent(
                r.getId(), s -> new ArrayList<>()).contains(name)) && isNotAdmin(member)) {
            sendDisabledNotice(event, CommandDisableLevel.SPECIFIC_ROLE);
            return;
        }

        final var roleSpecificDisabledCategories = dbGuild.getRoleSpecificDisabledCategories();
        if (roles.stream().anyMatch(r -> roleSpecificDisabledCategories.computeIfAbsent(
                r.getId(), s -> new ArrayList<>()).contains(cmd.getCategory())) && isNotAdmin(member)) {
            sendDisabledNotice(event, CommandDisableLevel.SPECIFIC_ROLE_CATEGORY);
            return;
        }

        if (mantaroData.getBlackListedUsers().contains(author.getId())) {
            if (!rl.process(author)) {
                return;
            }

            event.reply("""
                    :x: You have been blocked from using all of Mantaro's functions, likely for botting or hitting the spam filter.
                    If you wish to get more details on why or appeal the ban, send an email to `contact@mantaro.site`. Make sure to be sincere.
                    """
            ).setEphemeral(true).queue();
            return;
        }

        // If we are in the patreon bot, deny all requests from unknown guilds.
        if (config.isPremiumBot() && !config.isOwner(author) && !dbGuild.isPremium()) {
            event.reply("""
                            :x: Seems like you're trying to use the Patreon bot when this guild is **not** marked as premium.
                            **If you think this is an error please contact Kodehawa#3457 or poke me on #donators in the support guild**
                            If you didn't contact Kodehawa prior to adding this bot to this server, please do so so we can link it to your pledge.
                            """
            ).setEphemeral(true).queue();
            return;
        }

        if (!cmd.getPermission().test(member)) {
            event.reply(EmoteReference.STOP + "You have no permissions to trigger this command :(").setEphemeral(true).queue();
            return;
        }
        // !! Permission check end

        final var dbUser = managedDatabase.getUser(author);
        renewPremiumKey(managedDatabase, author, dbUser, dbGuild);

        cmd.execute(new SlashContext(event, new I18nContext(dbGuild, dbUser)));
        commandLog.debug("Slash command: {}, User: {} ({}), Guild: {}, Channel: {}, Options: {}" ,
                cmd.getName(), author.getName(), author.getId(), guild.getId(), channel.getId(), event.getOptions()
        );

        final var end = System.currentTimeMillis();
        final var category = cmd.getCategory().name().toLowerCase();

        Metrics.CATEGORY_COUNTER.labels(category).inc();
        Metrics.COMMAND_COUNTER.labels(name + "-slash").inc();
        Metrics.COMMAND_LATENCY.observe(end - start);
    }

    public void renewPremiumKey(ManagedDatabase managedDatabase, User author, MongoUser dbUser, MongoGuild guildData) {
        if (dbUser.getPremiumKey() != null) {
            final var currentKey = managedDatabase.getPremiumKey(dbUser.getPremiumKey());
            if (currentKey != null) {
                // 10 days before expiration or best fit.
                if (currentKey.validFor() <= 10 && currentKey.validFor() > 1) {
                    // Handling is done inside the PremiumKey#renew method. This only gets fired if the key has less than 10 days left.
                    if (!currentKey.renew() && !dbUser.hasReceivedExpirationWarning()) {
                        author.openPrivateChannel().queue(privateChannel ->
                                privateChannel.sendMessage(
                                        """
                                        %1$sYour premium key is about to expire in **%2$,d** days**!
                                        :heart: *If you're still pledging to Mantaro* you can ask Kodehawa#3457 for a key renewal in the #donators channel.*
                                        In the case that you're not longer a patron, you cannot renew, but I sincerely hope you had a good time with the bot and its features!
                                        **If you ever want to pledge again you can check the patreon link at <https://patreon.com/mantaro>**
                                        
                                        Thanks you so much for your support to keep Mantaro alive! It wouldn't be possible without the help of all of you.
                                        With love, Kodehawa and the Mantaro team :heart:
                                        
                                        This will only be sent once (hopefully). Thanks again!
                                        """.formatted(EmoteReference.WARNING, Math.max(1, currentKey.validFor()))
                                ).queue()
                        );
                    }

                    dbUser.receivedExpirationWarning(true);
                    dbUser.updateAllChanged();
                }
            }
        }

        if (guildData.getPremiumKey() != null) {
            final var guildKey = managedDatabase.getPremiumKey(guildData.getPremiumKey());
            // Handling is done inside the PremiumKey#renew method. This only gets fired if the key has less than 10 days left.
            if (guildKey != null && guildKey.validFor() <= 10 && guildKey.validFor() > 1) {
                guildKey.renew();
            }
        }
    }

    public void register(Class<? extends NewCommand> clazz) {
        var cmd = newCommands.register(clazz);
        var p = new ProxyCommand(cmd);
        commands.put(cmd.getName(), p);
        cmd.aliases().forEach(a -> commands.put(a, new AliasProxyCommand(p)));
    }

    public void registerSlash(Class<? extends SlashCommand> clazz) {
        newCommands.registerSlash(clazz);
    }

    public void registerContextUser(Class<? extends ContextCommand<User>> clazz) {
        newCommands.registerContextUser(clazz);
    }

    public <T extends Command> T register(String name, T command) {
        commands.putIfAbsent(name, command);
        log.debug("Registered command {}", name);
        return command;
    }

    public void registerAlias(String command, String alias) {
        if (!commands.containsKey(command)) {
            log.error("{} isn't in the command map...", command);
        }

        Command parent = commands.get(command);
        if (parent instanceof ProxyCommand) {
            throw new IllegalArgumentException("Use @Alias instead");
        }
        parent.getAliases().add(alias);

        register(alias, new AliasCommand(alias, command, parent));
    }

    public void registerAlias(String command, String... alias) {
        if (!commands.containsKey(command)) {
            log.error("{} isn't in the command map...", command);
        }

        Command parent = commands.get(command);
        if (parent instanceof ProxyCommand) {
            throw new IllegalArgumentException("Use @Alias instead");
        }

        for (String s : alias) {
            parent.getAliases().add(s);
            register(s, new AliasCommand(s, command, parent));
        }
    }

    private boolean isNotAdmin(Member member) {
        return !CommandPermission.ADMIN.test(member);
    }

    public CommandManager getCommandManager() {
        return newCommands;
    }


    private void sendDisabledNotice(MessageReceivedEvent event, MongoGuild data, CommandDisableLevel level) {
        if (data.isCommandWarningDisplay() && level != CommandDisableLevel.NONE) {
            event.getChannel().sendMessageFormat("%sThis command is disabled on this server. Reason: %s",
                    EmoteReference.ERROR, Utils.capitalize(level.getName())
            ).queue();
        } // else don't
    }

    private void sendDisabledNotice(SlashCommandInteractionEvent event, CommandDisableLevel level) {
        event.reply("%sThis command is disabled on this server. Reason: %s"
                .formatted(EmoteReference.ERROR, Utils.capitalize(level.getName()))
        ).setEphemeral(true).queue();
    }

    private static String name(Command c, String userInput) {
        if (c instanceof AliasCommand alias) {
            // Return the original command name here for all intents and purposes.
            // This is because in the check for command disable (which is what this is used for), the
            // command disabled will be the original command, and the check expects that.
            return alias.getOriginalName();
        }

        if (c instanceof ProxyCommand proxyCommand) {
            return proxyCommand.c.getName();
        }

        return userInput.toLowerCase();
    }

    private Command root(Command c) {
        if (c instanceof AliasCommand aliasCommand) {
            return commands.get(aliasCommand.parentName());
        }

        if (c instanceof AliasProxyCommand aliasProxyCommand) {
            return aliasProxyCommand.p;
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
            return c.getCategory();
        }

        @Override
        public CommandPermission permission() {
            return c.getPermission();
        }

        @Override
        public void run(Context context, String commandName, String content) {
            throw new UnsupportedOperationException();
        }

        @Override
        public HelpContent help() {
            return c.getHelp();
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

    enum CommandDisableLevel {
        NONE("None"),
        CATEGORY("Disabled category on server"),
        SPECIFIC_CATEGORY("Disabled category on specific channel"),
        COMMAND("Disabled command"),
        COMMAND_SPECIFIC("Disabled command on specific channel"),
        @SuppressWarnings("unused")
        GUILD("Disabled command on this server"),
        ROLE("Disabled role on this server"),
        @SuppressWarnings("unused")
        ROLE_CATEGORY("Disabled role for this category in this server"),
        SPECIFIC_ROLE("Disabled role for this command in this server"),
        SPECIFIC_ROLE_CATEGORY("Disabled role for this category in this server"),
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
