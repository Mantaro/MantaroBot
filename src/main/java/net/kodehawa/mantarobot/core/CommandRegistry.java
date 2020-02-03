/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.core;

import com.google.common.base.Preconditions;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
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
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.PremiumKey;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.RateLimiter;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CommandRegistry {
    //Wiki says they should always be static?
    private static final Histogram commandLatency = Histogram.build()
                                                            .name("command_latency").help("Time it takes for a command to process.")
                                                            .register();
    private static final Counter commandCounter = Counter.build()
                                                          .name("commands").help("Amounts of commands ran (name, userId, guildId:channelId")
                                                          .labelNames("name")
                                                          .register();
    private static final Counter categoryCounter = Counter.build()
                                                           .name("categories").help("Amounts of categories ran (name, userId, guildId")
                                                           .labelNames("name")
                                                           .register();
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(CommandRegistry.class);
    
    private final Map<String, Command> commands;
    private final Config conf = MantaroData.config().get();
    private boolean logCommands = false;
    private RateLimiter rl = new RateLimiter(TimeUnit.MINUTES, 1);
    
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
    public boolean process(GuildMessageReceivedEvent event, String cmdName, String content, String prefix) {
        final ManagedDatabase managedDatabase = MantaroData.db();
        long start = System.currentTimeMillis();
        
        Command command = commands.get(cmdName.toLowerCase());
        
        DBGuild dbg = managedDatabase.getGuild(event.getGuild());
        DBUser dbUser = managedDatabase.getUser(event.getAuthor());
        UserData userData = dbUser.getData();
        GuildData guildData = dbg.getData();
        
        if(command == null) {
            CustomCmds.handle(prefix, cmdName, event, new I18nContext(guildData, userData), content);
            return false;
        }
        
        if(!event.getGuild().getSelfMember().getPermissions(event.getChannel()).contains(Permission.MESSAGE_EMBED_LINKS)) {
            event.getChannel().sendMessage(EmoteReference.STOP + "I require the permission ``Embed Links``. " +
                                                   "All Commands will be refused until you give me that permission.\n" +
                                                   "http://i.imgur.com/Ydykxcy.gifv Refer to this on instructions on how to give the bot the permissions. " +
                                                   "Also check all the other roles the bot has have that permissions and remember to check channel-specific permissions. Thanks you.").queue();
            return false;
        }
        
        if(managedDatabase.getMantaroData().getBlackListedUsers().contains(event.getAuthor().getId())) {
            if(rl.process(event.getAuthor())) {
                event.getChannel().sendMessage(EmoteReference.ERROR + "You have been blacklisted from using all of Mantaro's functions. " +
                                                       "If you wish to get more details on why, don't hesitate to join the support server and ask, but be sincere."
                ).queue();
            }
            return false;
        }
        
        //Variable used in lambda expression should be final or effectively final...
        final Command cmd = command;
        
        if(guildData.getDisabledCommands().contains(cmd instanceof AliasCommand ? ((AliasCommand) cmd).getOriginalName() : cmdName.toLowerCase())) {
            sendDisabledNotice(event, guildData, CommandDisableLevel.COMMAND);
            return false;
        }
        
        List<String> channelDisabledCommands = guildData.getChannelSpecificDisabledCommands().get(event.getChannel().getId());
        if(channelDisabledCommands != null && channelDisabledCommands.contains(cmd instanceof AliasCommand ? ((AliasCommand) cmd).getOriginalName() : cmdName.toLowerCase())) {
            sendDisabledNotice(event, guildData, CommandDisableLevel.COMMAND_SPECIFIC);
            return false;
        }
        
        if(guildData.getDisabledUsers().contains(event.getAuthor().getId()) && !isAdmin(event.getMember())) {
            sendDisabledNotice(event, guildData, CommandDisableLevel.USER);
            return false;
        }
        
        if(guildData.getDisabledChannels().contains(event.getChannel().getId()) && (cmd instanceof AliasCommand ? ((AliasCommand) cmd).parentCategory() != Category.MODERATION : cmd.category() != Category.MODERATION)) {
            sendDisabledNotice(event, guildData, CommandDisableLevel.CHANNEL);
            return false;
        }
        
        if(guildData.getDisabledCategories().contains(
                cmd instanceof AliasCommand ? ((AliasCommand) cmd).parentCategory() : cmd.category()
        ) && !cmdName.toLowerCase().equals("opts")) {
            sendDisabledNotice(event, guildData, CommandDisableLevel.CATEGORY);
            return false;
        }
        
        if(guildData.getChannelSpecificDisabledCategories().computeIfAbsent(event.getChannel().getId(), c ->
                                                                                                                new ArrayList<>()).contains(cmd instanceof AliasCommand ? ((AliasCommand) cmd).parentCategory() :
                                                                                                                                                    cmd.category())
                   && !cmdName.toLowerCase().equals("opts")) {
            sendDisabledNotice(event, guildData, CommandDisableLevel.SPECIFIC_CATEGORY);
            return false;
        }
        
        if(guildData.getWhitelistedRole() != null) {
            Role whitelistedRole = event.getGuild().getRoleById(guildData.getWhitelistedRole());
            if((whitelistedRole != null && event.getMember().getRoles().stream().noneMatch(r -> whitelistedRole.getId().equalsIgnoreCase(r.getId())) && !isAdmin(event.getMember()))) {
                return false;
            }
            //else continue.
        }
        
        if(!guildData.getDisabledRoles().isEmpty() && event.getMember().getRoles().stream().anyMatch(r -> guildData.getDisabledRoles().contains(r.getId())) && !isAdmin(event.getMember())) {
            sendDisabledNotice(event, guildData, CommandDisableLevel.ROLE);
            return false;
        }
        
        HashMap<String, List<String>> roleSpecificDisabledCommands = guildData.getRoleSpecificDisabledCommands();
        if(event.getMember().getRoles().stream().anyMatch(r -> roleSpecificDisabledCommands.computeIfAbsent(r.getId(), s -> new ArrayList<>()).contains(cmd instanceof AliasCommand ? ((AliasCommand) cmd).getOriginalName() : cmdName)) && !isAdmin(event.getMember())) {
            sendDisabledNotice(event, guildData, CommandDisableLevel.SPECIFIC_ROLE);
            return false;
        }
        
        HashMap<String, List<Category>> roleSpecificDisabledCategories = guildData.getRoleSpecificDisabledCategories();
        if(event.getMember().getRoles().stream().anyMatch(r -> roleSpecificDisabledCategories.computeIfAbsent(r.getId(), s -> new ArrayList<>()).contains(cmd instanceof AliasCommand ? ((AliasCommand) cmd).parentCategory() : cmd.category())) && !isAdmin(event.getMember())) {
            sendDisabledNotice(event, guildData, CommandDisableLevel.SPECIFIC_ROLE_CATEGORY);
            return false;
        }
        
        //If we are in the patreon bot, deny all requests from unknown guilds.
        if(conf.isPremiumBot() && !conf.isOwner(event.getAuthor()) && !dbg.isPremium()) {
            event.getChannel().sendMessage(EmoteReference.ERROR + "Seems like you're trying to use the Patreon bot when this guild is **not** marked as premium. " +
                                                   "**If you think this is an error please contact Kodehawa#3457 or poke me on #donators in the support guild**").queue();
            return false;
        }
        
        if(!cmd.permission().test(event.getMember())) {
            event.getChannel().sendMessage(EmoteReference.STOP + "You have no permissions to trigger this command :(").queue();
            return false;
        }
        
        PremiumKey currentKey = managedDatabase.getPremiumKey(userData.getPremiumKey());
        PremiumKey guildKey = managedDatabase.getPremiumKey(guildData.getPremiumKey());
        
        if(currentKey != null) {
            //10 days before expiration or best fit.
            if(currentKey.validFor() <= 10 && currentKey.validFor() > 1) {
                //Handling is done inside the PremiumKey#renew method. This only gets fired if the key has less than 10 days left.
                if(!currentKey.renew() && !userData.hasReceivedExpirationWarning()) {
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
        if(guildKey != null) {
            if(guildKey.validFor() <= 10 && guildKey.validFor() > 1) {
                guildKey.renew();
            }
        }
        
        //COMMAND LOGGING
        long end = System.currentTimeMillis();
        commandCounter.labels(cmdName.toLowerCase()).inc();
        
        if(logCommands) {
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
        
        cmd.run(event, new I18nContext(guildData, userData), cmdName, content);
        
        //Logging
        if(cmd.category() != null && cmd.category().name() != null && !cmd.category().name().isEmpty()) {
            categoryCounter.labels(cmd.category().name().toLowerCase()).inc();
            
            CommandStatsManager.log(cmdName);
            CategoryStatsManager.log(cmd.category().name().toLowerCase());
        }
        
        commandLatency.observe(end - start);
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
        
        Command parent = commands.get(command);
        parent.getAliases().add(alias);
        
        register(alias, new AliasCommand(alias, command, parent));
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
    
    public void sendDisabledNotice(GuildMessageReceivedEvent event, GuildData data, CommandDisableLevel level) {
        if(data.isCommandWarningDisplay() && level != CommandDisableLevel.NONE) {
            event.getChannel().sendMessageFormat("%sThis command is disabled on this server. Reason: %s",
                    EmoteReference.ERROR, Utils.capitalize(level.getName())
            ).queue();
        } //else don't
    }
    
    public void setLogCommands(boolean logCommands) {
        this.logCommands = logCommands;
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
        
        String name;
        
        CommandDisableLevel(String name) {
            this.name = name;
        }
        
        public String getName() {
            return this.name;
        }
    }
    
}
