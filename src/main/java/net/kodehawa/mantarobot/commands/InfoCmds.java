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

package net.kodehawa.mantarobot.commands;

import com.github.natanbc.usagetracker.DefaultBucket;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.Region;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.info.stats.manager.CategoryStatsManager;
import net.kodehawa.mantarobot.commands.info.stats.manager.CommandStatsManager;
import net.kodehawa.mantarobot.commands.info.stats.manager.CustomCommandStatsManager;
import net.kodehawa.mantarobot.commands.info.stats.manager.GameStatsManager;
import net.kodehawa.mantarobot.commands.info.stats.manager.GuildStatsManager;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.AliasCommand;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SimpleTreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.base.ITreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.InnerCommand;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.core.processor.DefaultCommandProcessor;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.I18n;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.kodehawa.mantarobot.commands.info.AsyncInfoMonitor.getAvailableProcessors;
import static net.kodehawa.mantarobot.commands.info.AsyncInfoMonitor.getFreeMemory;
import static net.kodehawa.mantarobot.commands.info.AsyncInfoMonitor.getInstanceCPUUsage;
import static net.kodehawa.mantarobot.commands.info.AsyncInfoMonitor.getMaxMemory;
import static net.kodehawa.mantarobot.commands.info.AsyncInfoMonitor.getThreadCount;
import static net.kodehawa.mantarobot.commands.info.AsyncInfoMonitor.getTotalMemory;
import static net.kodehawa.mantarobot.commands.info.AsyncInfoMonitor.getVpsFreeMemory;
import static net.kodehawa.mantarobot.commands.info.AsyncInfoMonitor.getVpsMaxMemory;
import static net.kodehawa.mantarobot.commands.info.AsyncInfoMonitor.getVpsUsedMemory;
import static net.kodehawa.mantarobot.commands.info.HelpUtils.forType;
import static net.kodehawa.mantarobot.utils.Utils.prettyDisplay;

@Module
@SuppressWarnings("unused")
public class InfoCmds {
    private final CategoryStatsManager categoryStatsManager = new CategoryStatsManager();
    private final CommandStatsManager commandStatsManager = new CommandStatsManager();
    private final CustomCommandStatsManager customCommandStatsManager = new CustomCommandStatsManager();
    private final GameStatsManager gameStatsManager = new GameStatsManager();
    private final GuildStatsManager guildStatsManager = new GuildStatsManager();
    private final List<String> tips = new SimpleFileDataManager("assets/mantaro/texts/tips.txt").get();
    Random rand = new Random();
    
    @Subscribe
    public void donate(CommandRegistry cr) {
        cr.register("donate", new SimpleCommand(Category.INFO) {
            
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                event.getChannel().sendMessageFormat(
                        languageContext.get("commands.donate.beg"), EmoteReference.HEART, String.format(languageContext.get("commands.donate.methods"), "https://patreon.com/mantaro", "https://paypal.me/mantarobot")
                ).queue();
            }
            
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("Shows the donation methods in case you want to support Mantaro.")
                               .build();
            }
        });
    }
    
    @SuppressWarnings("UnstableApiUsage")
    @Subscribe
    public void language(CommandRegistry cr) {
        cr.register("lang", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                event.getChannel().sendMessageFormat(languageContext.get("commands.lang.info"), EmoteReference.ZAP, String.join(", ", I18n.LANGUAGES).replace(".json", "")).queue();
            }
            
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("Shows how to change the server and user languages, along with a language list.")
                               .build();
            }
        });
        
        cr.registerAlias("lang", "language");
    }
    
    @Subscribe
    public void avatar(CommandRegistry cr) {
        cr.register("avatar", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                Member member = Utils.findMember(event, event.getMember(), content);
                if(member == null)
                    return;
                
                User u = member.getUser();
                
                event.getChannel().sendMessageFormat(languageContext.get("commands.avatar.result"), EmoteReference.OK, u.getName(), u.getEffectiveAvatarUrl()).queue();
            }
            
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("Get a user's avatar URL.")
                               .setUsage("`~>avatar [@user]` - Returns the requested avatar URL")
                               .addParameter("@user", "The user you want to check the avatar URL of. Can be a mention, or name#discrim")
                               .build();
            }
        });
    }
    
    @Subscribe
    public void guildinfo(CommandRegistry cr) {
        cr.register("serverinfo", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                Guild guild = event.getGuild();
                TextChannel channel = event.getChannel();
                
                String roles = guild.getRoles().stream()
                                       .filter(role -> !guild.getPublicRole().equals(role))
                                       .map(Role::getName)
                                       .collect(Collectors.joining(", "));
                
                if(roles.length() > 1024)
                    roles = roles.substring(0, 1024 - 4) + "...";
                
                channel.sendMessage(new EmbedBuilder()
                                            .setAuthor(languageContext.get("commands.serverinfo.header"), null, guild.getIconUrl())
                                            .setColor(guild.getOwner().getColor() == null ? Color.ORANGE : guild.getOwner().getColor())
                                            .setDescription(String.format(languageContext.get("commands.serverinfo.description"), guild.getName()))
                                            .setThumbnail(guild.getIconUrl())
                                            .addField(languageContext.get("commands.serverinfo.users"),
                                                    (int) guild.getMembers().stream().filter(u -> !u.getOnlineStatus().equals(OnlineStatus.OFFLINE)).count() + "/" + guild.getMembers().size(), true)
                                            .addField(languageContext.get("commands.serverinfo.created"),
                                                    guild.getTimeCreated().format(DateTimeFormatter.ISO_DATE_TIME).replaceAll("[^0-9.:-]", " "), true)
                                            .addField(languageContext.get("commands.serverinfo.channels"),
                                                    guild.getVoiceChannels().size() + "/" + guild.getTextChannels().size(), true)
                                            .addField(languageContext.get("commands.serverinfo.owner"),
                                                    guild.getOwner().getUser().getName() + "#" + guild.getOwner().getUser().getDiscriminator(), true)
                                            .addField(languageContext.get("commands.serverinfo.region"),
                                                    guild.getRegion() == Region.UNKNOWN ? languageContext.get("general.unknown") : guild.getRegion().getName(), true)
                                            .addField(String.format(languageContext.get("commands.serverinfo.roles"),
                                                    guild.getRoles().size()), roles, false)
                                            .setFooter(String.format(languageContext.get("commands.serverinfo.id_show"), guild.getId()), null)
                                            .build()
                ).queue();
            }
            
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("See your server's current stats.")
                               .build();
            }
        });
        
        cr.registerAlias("serverinfo", "guildinfo");
    }
    
    private void buildHelp(GuildMessageReceivedEvent event, String content, I18nContext languageContext, Category category) {
        DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
        String defaultPrefix = MantaroData.config().get().prefix[0], guildPrefix = dbGuild.getData().getGuildCustomPrefix();
        String prefix = guildPrefix == null ? defaultPrefix : guildPrefix;
        GuildData guildData = dbGuild.getData();
        
        EmbedBuilder embed = new EmbedBuilder()
                                     .setAuthor(languageContext.get("commands.help.title"), null, event.getGuild().getIconUrl())
                                     .setColor(Color.PINK)
                                     .setDescription(
                                             (category == null ?
                                                      languageContext.get("commands.help.base") :
                                                      String.format(languageContext.get("commands.help.base_category"), languageContext.get(category.toString()))
                                             ) +
                                                     languageContext.get("commands.help.support") + languageContext.get("commands.help.patreon") +
                                                     //LISP simulator 2018
                                                     (guildData.getDisabledCommands().isEmpty() ? "" : "\n" +
                                                                                                               String.format(languageContext.get("commands.help.disabled_commands"), guildData.getDisabledCommands().size())
                                                     ) +
                                                     (guildData.getChannelSpecificDisabledCommands().get(event.getChannel().getId()) == null ||
                                                              guildData.getChannelSpecificDisabledCommands().get(event.getChannel().getId()).isEmpty() ?
                                                              "" : "\n" + String.format(languageContext.get("commands.help.channel_specific_disabled_commands"),
                                                             guildData.getChannelSpecificDisabledCommands().get(event.getChannel().getId()).size())
                                                     ) + (tips.isEmpty() ?
                                                                  "" : String.format("\n*Tip: %s*", tips.get(rand.nextInt(tips.size())))
                                             )
                
                                     )
                                     .setFooter(String.format(languageContext.get("commands.help.footer"), prefix,
                                             DefaultCommandProcessor.REGISTRY.commands().values().stream().filter(c -> c.category() != null).count()), null);
        
        Arrays.stream(Category.values())
                .filter(c -> {
                    if(category != null)
                        return c == category;
                    else
                        return true;
                })
                .filter(c -> c != Category.OWNER || CommandPermission.OWNER.test(event.getMember()))
                .forEach(c -> embed.addField(languageContext.get(c.toString()) + " " + languageContext.get("commands.help.commands") + ":",
                        forType(event.getChannel(), guildData, c), false)
                );
        
        event.getChannel().sendMessage(embed.build()).queue();
    }
    
    @Subscribe
    public void help(CommandRegistry cr) {
        final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                                                          .limit(1)
                                                          .spamTolerance(2)
                                                          .cooldown(3, TimeUnit.SECONDS)
                                                          .maxCooldown(3, TimeUnit.SECONDS)
                                                          .randomIncrement(true)
                                                          .pool(MantaroData.getDefaultJedisPool())
                                                          .prefix("help")
                                                          .build();
        
        Random r = new Random();
        List<String> jokes = List.of(
                "Yo damn I heard you like help, because you just issued the help command to get the help about the help command.",
                "Congratulations, you managed to use the help command.",
                "Helps you to help yourself.",
                "Help Inception.",
                "A help helping helping helping help.",
                "I wonder if this is what you are looking for...",
                "Helping you help the world."
        );
        
        cr.register("help", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(!Utils.handleDefaultIncreasingRatelimit(rateLimiter, event.getAuthor(), event, languageContext, false))
                    return;
                
                TextChannel channel = event.getChannel();
                
                if(content.isEmpty()) {
                    buildHelp(event, content, languageContext, null);
                } else if(Category.lookupFromString(content) != null) {
                    Category category = Category.lookupFromString(content);
                    buildHelp(event, content, languageContext, category);
                } else {
                    Command command = DefaultCommandProcessor.REGISTRY.commands().get(content);
                    
                    if(command != null) {
                        if(command.category() == Category.OWNER && !CommandPermission.OWNER.test(event.getMember())) {
                            channel.sendMessageFormat(languageContext.get("commands.help.extended.not_found"), EmoteReference.ERROR).queue();
                            return;
                        }
                        
                        if(command.help() != null && command.help().getDescription() != null) {
                            HelpContent newHelp = command.help();
                            EmbedBuilder builder = new EmbedBuilder()
                                                           .setColor(Color.PINK)
                                                           //asume content = command name
                                                           .setAuthor(Utils.capitalize(content) + " Command Help", null, event.getAuthor().getEffectiveAvatarUrl())
                                                           .setThumbnail("https://cdn.pixabay.com/photo/2012/04/14/16/26/question-34499_960_720.png")
                                                           .setDescription((r.nextBoolean() ? languageContext.get("commands.help.patreon") + "\n" : "") + newHelp.getDescription())
                                                           .setFooter("Don't include <> or [] on the command itself.", event.getAuthor().getEffectiveAvatarUrl());
                            
                            if(newHelp.getUsage() != null) {
                                builder.addField("Usage", newHelp.getUsage(), false);
                            }
                            
                            if(newHelp.getParameters().size() > 0) {
                                builder.addField("Parameters", newHelp.getParameters().entrySet().stream()
                                                                       .map(entry -> "`" + entry.getKey() + "` - *" + entry.getValue() + "*")
                                                                       .collect(Collectors.joining("\n")), false);
                                
                            }
                            
                            if(newHelp.isSeasonal()) {
                                builder.addField("Seasonal", "This command allows the usage of the `-season` (or `-s`) argument.", false);
                            }
                            
                            //Ensure sub-commands show in help.
                            //Only god shall help me now with all of this casting lol.
                            if(command instanceof AliasCommand) {
                                command = ((AliasCommand) command).getCommand();
                            }
                            
                            if(command instanceof ITreeCommand) {
                                Map<String, SubCommand> subCommands = ((ITreeCommand) command).getSubCommands();
                                StringBuilder stringBuilder = new StringBuilder();
                                
                                for(Map.Entry<String, SubCommand> inners : subCommands.entrySet()) {
                                    String name = inners.getKey();
                                    InnerCommand inner = inners.getValue();
                                    if(inner.isChild())
                                        continue;
                                    
                                    if(inner.description() != null) {
                                        stringBuilder.append(EmoteReference.BLUE_SMALL_MARKER).append("`").append(name).append("` - ").append(inner.description()).append("\n");
                                    }
                                }
                                
                                if(stringBuilder.length() > 0) {
                                    builder.addField("Sub-commands", "**Append the main command to use any of this.**\n" + stringBuilder.toString(), false);
                                }
                            }
                            
                            //Known command aliases.
                            List<String> commandAliases = command.getAliases();
                            if(!commandAliases.isEmpty()) {
                                String aliases = commandAliases.stream().filter(alias -> !alias.equalsIgnoreCase(content)).map(alias -> "`" + alias + "`").collect(Collectors.joining(" "));
                                if(!aliases.trim().isEmpty()) {
                                    builder.addField("Aliases", aliases, false);
                                }
                            }
                            
                            channel.sendMessage(builder.build()).queue();
                        } else {
                            channel.sendMessageFormat(languageContext.get("commands.help.extended.no_help"), EmoteReference.ERROR).queue();
                        }
                    } else {
                        channel.sendMessageFormat(languageContext.get("commands.help.extended.not_found"), EmoteReference.ERROR).queue();
                    }
                }
            }
            
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("I wonder if this is what you are looking for...")
                               .setUsage("`~>help <command>`")
                               .addParameter("command", "The command name of the command you want to check information about.")
                               .build();
            }
        });
        
        cr.registerAlias("help", "commands");
        cr.registerAlias("help", "halp"); //why not
    }
    
    @Subscribe
    public void invite(CommandRegistry cr) {
        cr.register("invite", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                event.getChannel().sendMessage(new EmbedBuilder().setAuthor("Mantaro's Invite URL.", null, event.getJDA().getSelfUser().getAvatarUrl())
                                                       .addField(languageContext.get("commands.invite.url"), "http://add.mantaro.site", false)
                                                       .addField(languageContext.get("commands.invite.server"), "https://support.mantaro.site", false)
                                                       .addField(languageContext.get("commands.invite.patreon"), "http://patreon.com/mantaro", false)
                                                       .setDescription(languageContext.get("commands.invite.description.1") + " " +
                                                                               languageContext.get("commands.invite.description.2") + "\n" +
                                                                               languageContext.get("commands.invite.description.3") + " " +
                                                                               languageContext.get("commands.invite.description.4"))
                                                       .setFooter(languageContext.get("commands.invite.footer"), event.getJDA().getSelfUser().getAvatarUrl())
                                                       .build()).queue();
            }
            
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("Gives you a bot OAuth invite link and some other important links.")
                               .build();
            }
        });
    }
    
    @Subscribe
    public void prefix(CommandRegistry cr) {
        cr.register("prefix", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                final ManagedDatabase db = MantaroData.db();
                DBGuild dbGuild = db.getGuild(event.getGuild());
                Config config = MantaroData.config().get();
                String defaultPrefix = Stream.of(config.getPrefix()).map(prefix -> "`" + prefix + "`").collect(Collectors.joining(" "));
                String guildPrefix = dbGuild.getData().getGuildCustomPrefix();
                
                event.getChannel().sendMessageFormat(
                        languageContext.get("commands.prefix.header"), EmoteReference.HEART, defaultPrefix, guildPrefix == null ? languageContext.get("commands.prefix.none") : guildPrefix
                ).queue();
            }
            
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("Gives you information on how to change the prefix and what's the current prefix. If you looked at help, to change the prefix " +
                                                       "use `~>opts prefix set <prefix>`")
                               .build();
            }
        });
    }
    
    @Subscribe
    public void stats(CommandRegistry cr) {
        SimpleTreeCommand statsCommand = (SimpleTreeCommand) cr.register("stats", new SimpleTreeCommand(Category.INFO) {
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("See the bot, usage or vps statistics.")
                               .setUsage("~>stats <option>` - Returns statistical information.")
                               .addParameter("option", "What to check for. See subcommands")
                               .build();
            }
        });
        
        statsCommand.addSubCommand("usage", new SubCommand() {
            @Override
            public String description() {
                return "The bot's (and JVM) hardware usage";
            }
            
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                event.getChannel().sendMessage(new EmbedBuilder()
                                                       .setAuthor(languageContext.get("commands.stats.usage.header"), null, event.getJDA().getSelfUser().getAvatarUrl())
                                                       .setDescription(languageContext.get("commands.stats.usage.description"))
                                                       .setThumbnail(event.getJDA().getSelfUser().getAvatarUrl())
                                                       .addField(languageContext.get("commands.stats.usage.threads"), getThreadCount() + " Threads", true)
                                                       .addField(languageContext.get("commands.stats.usage.memory_usage"), Utils.formatMemoryUsage(getTotalMemory() - getFreeMemory(), getMaxMemory()), true)
                                                       .addField(languageContext.get("commands.stats.usage.cores"), getAvailableProcessors() + " Cores", true)
                                                       .addField(languageContext.get("commands.stats.usage.cpu_usage"), String.format("%.2f", getInstanceCPUUsage()) + "%", true)
                                                       .addField(languageContext.get("commands.stats.usage.assigned_mem"), Utils.formatMemoryAmount(getTotalMemory()), true)
                                                       .addField(languageContext.get("commands.stats.usage.assigned_remaining"), Utils.formatMemoryAmount(getFreeMemory()), true)
                                                       .build()
                ).queue();
                TextChannelGround.of(event).dropItemWithChance(4, 5);
            }
        });
        
        statsCommand.addSubCommand("server", new SubCommand() {
            @Override
            public String description() {
                return "The bot's hardware usage";
            }
            
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                TextChannelGround.of(event).dropItemWithChance(4, 5);
                EmbedBuilder embedBuilder = new EmbedBuilder()
                                                    .setAuthor(languageContext.get("commands.stats.server.header"), null, event.getJDA().getSelfUser().getAvatarUrl())
                                                    .setThumbnail(event.getJDA().getSelfUser().getAvatarUrl())
                                                    .addField(languageContext.get("commands.stats.server.cpu_usage"), String.format("%.2f", getInstanceCPUUsage()) + "%", true)
                                                    .addField(languageContext.get("commands.stats.server.rem"),
                                                            String.format(
                                                                    "%s/%s/%s",
                                                                    Utils.formatMemoryAmount(getVpsMaxMemory()),
                                                                    Utils.formatMemoryAmount(getVpsFreeMemory()),
                                                                    Utils.formatMemoryAmount(getVpsUsedMemory())
                                                            ), false
                                                    );
                
                event.getChannel().sendMessage(embedBuilder.build()).queue();
            }
        });
        
        statsCommand.addSubCommand("cmds", new SubCommand() {
            @Override
            public String description() {
                return "The bot's command usage";
            }
            
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                TextChannel channel = event.getChannel();
                
                String[] args = content.split(" ");
                if(args.length > 0) {
                    String what = args[0];
                    if(what.equals("total")) {
                        channel.sendMessage(CommandStatsManager.fillEmbed(DefaultBucket.TOTAL, baseEmbed(event, "Command Stats | Total")).build()).queue();
                        return;
                    }
                    
                    if(what.equals("daily")) {
                        channel.sendMessage(CommandStatsManager.fillEmbed(DefaultBucket.DAY, baseEmbed(event, "Command Stats | Daily")).build()).queue();
                        return;
                    }
                    
                    if(what.equals("hourly")) {
                        channel.sendMessage(CommandStatsManager.fillEmbed(DefaultBucket.HOUR, baseEmbed(event, "Command Stats | Hourly")).build()).queue();
                        return;
                    }
                    
                    if(what.equals("now")) {
                        channel.sendMessage(CommandStatsManager.fillEmbed(DefaultBucket.MINUTE, baseEmbed(event, "Command Stats | Now")).build()).queue();
                        return;
                    }
                }
                
                //Default
                channel.sendMessage(baseEmbed(event, "Command Stats")
                                            .addField(languageContext.get("general.now"), CommandStatsManager.resume(DefaultBucket.MINUTE), false)
                                            .addField(languageContext.get("general.hourly"), CommandStatsManager.resume(DefaultBucket.HOUR), false)
                                            .addField(languageContext.get("general.daily"), CommandStatsManager.resume(DefaultBucket.DAY), false)
                                            .addField(languageContext.get("general.total"), CommandStatsManager.resume(DefaultBucket.TOTAL), false)
                                            .build()
                ).queue();
            }
        });
        
        statsCommand.addSubCommand("category", new SubCommand() {
            @Override
            public String description() {
                return "The bot's category usage";
            }
            
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                TextChannel channel = event.getChannel();
                String[] args = content.split(" ");
                if(args.length > 0) {
                    String what = args[0];
                    if(what.equals("total")) {
                        channel.sendMessage(categoryStatsManager.fillEmbed(CategoryStatsManager.TOTAL_CATS, baseEmbed(event, "Category Stats | Total")).build()).queue();
                        return;
                    }
                    
                    if(what.equals("daily")) {
                        channel.sendMessage(categoryStatsManager.fillEmbed(CategoryStatsManager.DAY_CATS, baseEmbed(event, "Category Stats | Daily")).build()).queue();
                        return;
                    }
                    
                    if(what.equals("hourly")) {
                        channel.sendMessage(categoryStatsManager.fillEmbed(CategoryStatsManager.HOUR_CATS, baseEmbed(event, "Category Stats | Hourly")).build()).queue();
                        return;
                    }
                    
                    if(what.equals("now")) {
                        channel.sendMessage(categoryStatsManager.fillEmbed(CategoryStatsManager.MINUTE_CATS, baseEmbed(event, "Category Stats | Now")).build()).queue();
                        return;
                    }
                }
                
                //Default
                channel.sendMessage(baseEmbed(event, "Category Stats")
                                            .addField(languageContext.get("general.now"), categoryStatsManager.resume(CategoryStatsManager.MINUTE_CATS), false)
                                            .addField(languageContext.get("general.hourly"), categoryStatsManager.resume(CategoryStatsManager.HOUR_CATS), false)
                                            .addField(languageContext.get("general.daily"), categoryStatsManager.resume(CategoryStatsManager.DAY_CATS), false)
                                            .addField(languageContext.get("general.total"), categoryStatsManager.resume(CategoryStatsManager.TOTAL_CATS), false)
                                            .build()
                ).queue();
            }
        });
    }
    
    @Subscribe
    public void social(CommandRegistry cr) {
        cr.register("social", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                event.getChannel().sendMessage(String.format(languageContext.get("commands.social.header"), EmoteReference.HEART) +
                                                       languageContext.get("commands.social.description.1") + "\n" +
                                                       String.format(languageContext.get("commands.social.description.2"), "https://mantaro.site") + "\n" +
                                                       "**- Patreon:** <https://www.patreon.com/mantaro>\n" +
                                                       "**- Twitter:** <https://twitter.com/mantarodiscord>\n\n" +
                                                       languageContext.get("commands.social.note") + "\n").queue();
            }
            
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("Shows Mantaro's social networks.")
                               .build();
            }
        });
    }
    
    
    @Subscribe
    public void userinfo(CommandRegistry cr) {
        cr.register("userinfo", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                Member member = Utils.findMember(event, event.getMember(), content);
                if(member == null) return;
                
                User user = member.getUser();
                
                String roles = member.getRoles().stream()
                                       .map(Role::getName)
                                       .collect(Collectors.joining(", "));
                
                if(roles.length() > MessageEmbed.TEXT_MAX_LENGTH)
                    roles = roles.substring(0, MessageEmbed.TEXT_MAX_LENGTH - 4) + "...";
                
                String s = String.join("\n",
                        prettyDisplay(languageContext.get("commands.userinfo.id"), user.getId()),
                        prettyDisplay(languageContext.get("commands.userinfo.join_date"),
                                member.getTimeJoined().format(DateTimeFormatter.ISO_DATE).replace("Z", "")),
                        prettyDisplay(languageContext.get("commands.userinfo.created"),
                                user.getTimeCreated().format(DateTimeFormatter.ISO_DATE).replace("Z", "")),
                        prettyDisplay(languageContext.get("commands.userinfo.account_age"),
                                TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - user.getTimeCreated().toInstant().toEpochMilli())
                                        + " " + languageContext.get("general.days")),
                        prettyDisplay(languageContext.get("commands.userinfo.mutual_guilds"), String.valueOf(MantaroBot.getInstance()
                                                                                                                     .getShardManager()
                                                                                                                     .getMutualGuilds(event.getAuthor()).size())),
                        prettyDisplay(languageContext.get("commands.userinfo.vc"),
                                member.getVoiceState().getChannel() != null ? member.getVoiceState().getChannel().getName() : languageContext.get("general.none")),
                        prettyDisplay(languageContext.get("commands.userinfo.color"),
                                member.getColor() == null ? languageContext.get("commands.userinfo.default") : "#" + Integer.toHexString(member.getColor().getRGB()).substring(2).toUpperCase()),
                        prettyDisplay(languageContext.get("commands.userinfo.status"), Utils.capitalize(member.getOnlineStatus().getKey().toLowerCase()))
                );
                
                event.getChannel().sendMessage(new EmbedBuilder()
                                                       .setColor(member.getColor())
                                                       .setAuthor(String.format(languageContext.get("commands.userinfo.header"), user.getName(), user.getDiscriminator()), null, event.getAuthor().getEffectiveAvatarUrl())
                                                       .setThumbnail(user.getEffectiveAvatarUrl())
                                                       .setDescription(s)
                                                       .addField(String.format(languageContext.get("commands.userinfo.roles"), member.getRoles().size()), roles + ".", true)
                                                       .build()
                ).queue();
            }
            
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("See information about specific users.")
                               .setUsage("`~>userinfo <@user>` - Get information about an user.")
                               .addParameter("user", "The user you want to look for. Mentions, nickname and user#discriminator work.")
                               .build();
            }
        });
    }
    
    @Subscribe
    //no need to translate this
    public void tips(CommandRegistry cr) {
        final Random r = new Random();
        
        cr.register("tips", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                event.getChannel().sendMessage(EmoteReference.TALKING + "Tip: " + tips.get(r.nextInt(tips.size()))).queue();
            }
            
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("Shows tips about the bot.")
                               .build();
            }
        });
        
        cr.registerAlias("tips", "bottips");
    }
    
    @Subscribe
    public void season(CommandRegistry registry) {
        registry.register("season", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                event.getChannel().sendMessageFormat(languageContext.get("commands.season.info") + languageContext.get("commands.season.info_2"), getConfig().getCurrentSeason().getDisplay(), MantaroData.db().getAmountSeasonalPlayers()).queue();
            }
            
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("Shows information about this season and about what's a season.")
                               .build();
            }
        });
    }

    @Subscribe
    public void support(CommandRegistry registry) {
        registry.register("support", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                event.getChannel().sendMessageFormat(languageContext.get("commands.support.info"), EmoteReference.POPPER).queue();
            }
        });
    }
    
    @Subscribe
    public void roleinfo(CommandRegistry cr) {
        cr.register("roleinfo", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                Role r = Utils.findRole(event, content);
                if(r == null)
                    return;
                
                String s = String.join("\n",
                        prettyDisplay(languageContext.get("commands.roleinfo.id"), r.getId()),
                        prettyDisplay(languageContext.get("commands.roleinfo.created"),
                                r.getTimeCreated().format(DateTimeFormatter.ISO_DATE).replace("Z", "")),
                        prettyDisplay(languageContext.get("commands.roleinfo.age"),
                                TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - r.getTimeCreated().toInstant().toEpochMilli()) +
                                        " " + languageContext.get("general.days")),
                        prettyDisplay(languageContext.get("commands.roleinfo.color"),
                                r.getColor() == null ? languageContext.get("general.none") : ("#" + Integer.toHexString(r.getColor().getRGB()).substring(2))),
                        prettyDisplay(languageContext.get("commands.roleinfo.members"),
                                String.valueOf(event.getGuild().getMembers().stream().filter(member -> member.getRoles().contains(r)).count())),
                        prettyDisplay(languageContext.get("commands.roleinfo.position"), String.valueOf(r.getPosition())),
                        prettyDisplay(languageContext.get("commands.roleinfo.hoisted"), String.valueOf(r.isHoisted()))
                );
                
                event.getChannel().sendMessage(new EmbedBuilder()
                                                       .setColor(event.getMember().getColor())
                                                       .setAuthor(String.format(languageContext.get("commands.roleinfo.header"), r.getName()), null, event.getGuild().getIconUrl())
                                                       .setDescription(s)
                                                       .addField(String.format(languageContext.get("commands.roleinfo.permissions"), r.getPermissions().size()),
                                                               r.getPermissions().size() == 0 ? languageContext.get("general.none") : r.getPermissions().stream().map(Permission::getName).collect(Collectors.joining(", ")) + ".",
                                                               false
                                                       )
                                                       .build()
                ).queue();
            }
            
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("See information about specific role.")
                               .setUsage("`~>roleinfo <role>` - Get information about a role.")
                               .addParameter("role", "The role you want to look for. Mentions, id and name work.")
                               .build();
            }
        });
    }
}
