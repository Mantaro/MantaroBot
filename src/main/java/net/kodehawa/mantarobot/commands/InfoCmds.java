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

package net.kodehawa.mantarobot.commands;

import com.github.natanbc.usagetracker.DefaultBucket;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.utils.cache.SnowflakeCacheView;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.info.stats.manager.*;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.listeners.command.CommandListener;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.core.processor.DefaultCommandProcessor;
import net.kodehawa.mantarobot.data.I18n;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;

import java.awt.*;
import java.lang.management.ManagementFactory;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.commands.info.AsyncInfoMonitor.*;
import static net.kodehawa.mantarobot.commands.info.HelpUtils.forType;
import static net.kodehawa.mantarobot.utils.commands.EmoteReference.BLUE_SMALL_MARKER;

@Module
@SuppressWarnings("unused")
public class InfoCmds {
    private final CategoryStatsManager categoryStatsManager = new CategoryStatsManager();
    private final CommandStatsManager commandStatsManager = new CommandStatsManager();
    private final CustomCommandStatsManager customCommandStatsManager = new CustomCommandStatsManager();
    private final GameStatsManager gameStatsManager = new GameStatsManager();
    private final GuildStatsManager guildStatsManager = new GuildStatsManager();
    private final List<String> tips = new SimpleFileDataManager("assets/mantaro/texts/tips.txt").get();

    //@Subscribe
    public void about(CommandRegistry cr) {
        TreeCommand aboutCommand = (TreeCommand) cr.register("about", new TreeCommand(Category.INFO) {
            @Override
            public Command defaultTrigger(GuildMessageReceivedEvent event, String thisCommand, String attemptedSubCommand) {
                return new SubCommand() {
                    @Override
                    protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                        SnowflakeCacheView<Guild> guilds = MantaroBot.getInstance().getGuildCache();
                        SnowflakeCacheView<User> users = MantaroBot.getInstance().getUserCache();
                        SnowflakeCacheView<TextChannel> textChannels = MantaroBot.getInstance().getTextChannelCache();
                        SnowflakeCacheView<VoiceChannel> voiceChannels = MantaroBot.getInstance().getVoiceChannelCache();

                        event.getChannel().sendMessage(new EmbedBuilder()
                                .setColor(Color.PINK)
                                .setAuthor(languageContext.get("commands.about.title"), "https://add.mantaro.site", event.getJDA().getSelfUser().getEffectiveAvatarUrl())
                                .setThumbnail(event.getJDA().getSelfUser().getEffectiveAvatarUrl())
                                .setDescription(languageContext.get("commands.about.description.1") + "\n" +
                                        languageContext.get("commands.about.description.2") + "\n" +
                                        "\u2713 " + languageContext.get("commands.about.description.3") + "\n" +
                                        "\u2713 " + languageContext.get("commands.about.description.4") + "\n" +
                                        "\u2713 " + languageContext.get("commands.about.description.5") + "\n" +
                                        "\u2713 " + languageContext.get("commands.about.description.support") + "\n\n" +
                                        String.format(languageContext.get("commands.about.description.credits"), EmoteReference.POPPER) + (MantaroData.config().get().isPremiumBot() ?
                                        "\nRunning a Patreon Bot instance, thanks you for your support! \u2764" : "")
                                )
                                .addField(languageContext.get("commands.about.version"), MantaroInfo.VERSION, false)
                                .addField(languageContext.get("commands.about.uptime"), Utils.getHumanizedTime(ManagementFactory.getRuntimeMXBean().getUptime()), false)
                                .addField(languageContext.get("commands.about.shards"), String.valueOf(MantaroBot.getInstance().getShardedMantaro().getTotalShards()), true)
                                .addField(languageContext.get("commands.about.threads"), String.format("%,d", Thread.activeCount()), true)
                                .addField(languageContext.get("commands.about.guilds"), String.format("%,d", guilds.size()), true)
                                .addField(languageContext.get("commands.about.users"), String.format("%,d", users.size()), true)
                                .addField(languageContext.get("commands.about.tc"), String.format("%,d", textChannels.size()), true)
                                .addField(languageContext.get("commands.about.vc"), String.format("%,d", voiceChannels.size()), true)
                                .setFooter(String.format(languageContext.get("commands.about.invite"), CommandListener.getCommandTotalInt(), MantaroBot.getInstance().getShardForGuild(event.getGuild().getId()).getId() + 1), event.getJDA().getSelfUser().getEffectiveAvatarUrl())
                                .build()).queue();
                    }
                };
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "About Command")
                        .setDescription("**Read info about Mantaro!**")
                        .addField("Information",
                                "`~>about credits` - **Lists everyone who has helped on the bot's development**, " +
                                        "`~>about patreon` - **Lists our patreon supporters**", false)
                        .setColor(Color.PINK)
                        .build();
            }
        });

        aboutCommand.addSubCommand("patreon", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                Guild mantaroGuild = MantaroBot.getInstance().getGuildById("213468583252983809");
                String donators = mantaroGuild.getMembers().stream().filter(member -> member.getRoles().stream().filter(role ->
                                role.getName().equals("Patron")).collect(Collectors.toList()).size() > 0).map(Member::getUser)
                                .map(user -> String.format("%s#%s", user.getName(), user.getDiscriminator()))
                                .collect(Collectors.joining("\n"));

                boolean hasReactionPerms = event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_ADD_REACTION);
                List<String> donatorList = DiscordUtils.divideString(300, donators);
                List<String> messages = new LinkedList<>();
                for(String s1 : donatorList) {
                    messages.add(languageContext.get("commands.about.patreon.header") + "\n" +
                            (hasReactionPerms ?
                                languageContext.get("general.arrow_react") + " " :
                                languageContext.get("general.text_menu")
                            )
                            + String.format("```%s```", s1));
                }

                //won't translate this
                messages.add("Thanks to **MrLar#8117** for a $1025 donation and many other people who has donated once via paypal.");

                if(hasReactionPerms) {
                    DiscordUtils.list(event, 45, false, messages);
                } else {
                    DiscordUtils.listText(event, 45, false, messages);
                }
            }
        });

        aboutCommand.addSubCommand("credits", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                EmbedBuilder builder = new EmbedBuilder();
                builder.setAuthor("Credits.", null, event.getJDA().getSelfUser().getEffectiveAvatarUrl())
                        .setColor(Color.LIGHT_GRAY)
                        .setDescription(
                                String.join("\n",
                                        "**" + languageContext.get("commands.about.credits.main_dev") + "**: Kodehawa#3457",
                                        "**" + languageContext.get("commands.about.credits.dev") + "**: AdrianTodt#0722",
                                        "**" + languageContext.get("commands.about.credits.dev") + "**:  Natan#1289",
                                        "**" + languageContext.get("commands.about.credits.docs") + "**:  MrLar#8117 & Yuvira#7832",
                                        "**" + languageContext.get("commands.about.credits.community_admin") + "**:  MrLar#8117"
                                ))
                        .addField("Special mentions",
                                languageContext.get("commands.about.credits.special_mentions"), false)
                        .setFooter(languageContext.get("commands.about.credits.thank_note"), event.getJDA().getSelfUser().getEffectiveAvatarUrl());
                event.getChannel().sendMessage(builder.build()).queue();
            }
        });
    }

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
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Donation Methods")
                        .setDescription("**Shows the donation methods in case you want to support Mantaro!**")
                        .build();
            }
        });
    }

    @Subscribe
    public void language(CommandRegistry cr) {
        cr.register("lang", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                event.getChannel().sendMessageFormat(languageContext.get("commands.lang.info"), EmoteReference.ZAP, String.join(", ", I18n.LANGUAGES).replace(".json", "")).queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Localization Help")
                        .setDescription("**Shows how to change the server and user languages, along with a language list.**")
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
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Avatar")
                        .setDescription("**Get a user's avatar URL**")
                        .addField("Usage",
                                "`~>avatar` - **Get your avatar url**" +
                                        "\n `~>avatar <mention, nickname or name#discriminator>` - **Get a user's avatar url.**", false)
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
                                guild.getCreationTime().format(DateTimeFormatter.ISO_DATE_TIME).replaceAll("[^0-9.:-]", " "), true)
                        .addField(languageContext.get("commands.serverinfo.channels"),
                                guild.getVoiceChannels().size() + "/" + guild.getTextChannels().size(), true)
                        .addField(languageContext.get("commands.serverinfo.owner"),
                                guild.getOwner().getUser().getName() + "#" + guild.getOwner().getUser().getDiscriminator(), true)
                        .addField(languageContext.get("commands.serverinfo.region"),
                                guild.getRegion() == null ? languageContext.get("general.unknown") : guild.getRegion().getName(), true)
                        .addField(String.format(languageContext.get("commands.serverinfo.roles"),
                                guild.getRoles().size()), roles, false)
                        .setFooter(String.format(languageContext.get("commands.serverinfo.id_show"), guild.getId()), null)
                        .build()
                ).queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Server Info Command")
                        .setDescription("**See your server's current stats.**")
                        .setColor(event.getGuild().getOwner().getColor() == null ? Color.ORANGE : event.getGuild().getOwner().getColor())
                        .build();
            }
        });

        cr.registerAlias("serverinfo", "guildinfo");
    }

    @Subscribe
    public void help(CommandRegistry cr) {
        Random r = new Random();
        List<String> jokes = Collections.unmodifiableList(Arrays.asList(
                "Yo damn I heard you like help, because you just issued the help command to get the help about the help command.",
                "Congratulations, you managed to use the help command.",
                "Helps you to help yourself.",
                "Help Inception.",
                "A help helping helping helping help.",
                "I wonder if this is what you are looking for..."
        ));

        cr.register("help", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(content.isEmpty()) {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    String defaultPrefix = MantaroData.config().get().prefix[0], guildPrefix = dbGuild.getData().getGuildCustomPrefix();
                    String prefix = guildPrefix == null ? defaultPrefix : guildPrefix;
                    GuildData guildData = dbGuild.getData();

                    EmbedBuilder embed = baseEmbed(event, languageContext.get("commands.help.title"))
                            .setColor(Color.PINK)
                            .setDescription(languageContext.get("commands.help.base") +
                                    languageContext.get("commands.help.support") + languageContext.get("commands.help.patreon") +
                                    //LISP simulator 2018
                                    (guildData.getDisabledCommands().isEmpty() ? "" : "\n" +
                                            String.format(languageContext.get("commands.help.disabled_commands"), guildData.getDisabledCommands().size())
                                    ) +
                                    (guildData.getChannelSpecificDisabledCommands().get(event.getChannel().getId()) == null ||
                                            guildData.getChannelSpecificDisabledCommands().get(event.getChannel().getId()).isEmpty() ?
                                            "" : "\n" + String.format(languageContext.get("commands.help.channel_specific_disabled_commands"),
                                            guildData.getChannelSpecificDisabledCommands().get(event.getChannel().getId()).size())
                                    ) + ( tips.isEmpty() ?
                                        "" : String.format("\n*Tip: %s*", tips.get(r.nextInt(tips.size())))
                                    )

                            )
                            .setFooter(String.format(languageContext.get("commands.help.footer"), prefix,
                                    DefaultCommandProcessor.REGISTRY.commands().values().stream().filter(c -> c.category() != null).count()), null);

                    Arrays.stream(Category.values())
                            //.filter(c -> c != Category.CURRENCY || !MantaroData.config().get().isPremiumBot())
                            .filter(c -> c != Category.OWNER || CommandPermission.OWNER.test(event.getMember()))
                            .forEach(c -> embed.addField(languageContext.get(c.toString()) + " " + languageContext.get("commands.help.commands") +":",
                                    forType(event.getChannel(), guildData, c), false)
                            );

                    event.getChannel().sendMessage(embed.build()).queue();

                } else {
                    Command command = DefaultCommandProcessor.REGISTRY.commands().get(content);

                    if(command != null) {
                        final MessageEmbed help = command.help(event);
                        
                        if(help != null) {
                            event.getChannel().sendMessage(help).queue();
                        } else {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.help.extended.no_help"), EmoteReference.ERROR).queue();
                        }
                    } else {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.help.extended.not_found"), EmoteReference.ERROR).queue();
                    }
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Help Command")
                        .setColor(Color.PINK)
                        .setDescription("**" + jokes.get(r.nextInt(jokes.size())) + "**")
                        .addField(
                                "Usage",
                                "`~>help` - **Returns a list of commands that you can use**.\n" +
                                        "`~>help <command>` - **Return information about the command specified**.",
                                false
                        ).build();
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
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Invite command").setDescription("**Gives you a bot OAuth invite link.**").build();
            }
        });
    }

    @Subscribe
    public void stats(CommandRegistry cr) {
        TreeCommand statsCommand = (TreeCommand) cr.register("stats", new TreeCommand(Category.INFO) {
            @Override
            public Command defaultTrigger(GuildMessageReceivedEvent event, String currentCommand, String attemptedCommand) {
                return new SubCommand() {
                    @Override
                    protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                        onHelp(event);
                    }
                };
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Statistics command")
                        .setDescription("**See the bot, usage or vps statistics**")
                        .addField("Usage", "`~>stats <usage/server/cmds/guilds>` - **Returns statistical information**", true)
                        .build();
            }
        });

        statsCommand.addSubCommand("usage", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                event.getChannel().sendMessage(new EmbedBuilder()
                        .setAuthor(languageContext.get("commands.stats.usage.header"), null, event.getJDA().getSelfUser().getAvatarUrl())
                        .setDescription(languageContext.get("commands.stats.usage.description"))
                        .setThumbnail(event.getJDA().getSelfUser().getAvatarUrl())
                        .addField(languageContext.get("commands.stats.usage.threads"), getThreadCount() + " Threads", true)
                        .addField(languageContext.get("commands.stats.usage.memory_usage"), getTotalMemory() - getFreeMemory() + "MB/" + getMaxMemory() + "MB", true)
                        .addField(languageContext.get("commands.stats.usage.cores"), getAvailableProcessors() + " Cores", true)
                        .addField(languageContext.get("commands.stats.usage.cpu_usage"), String.format("%.2f", getVpsCPUUsage()) + "%", true)
                        .addField(languageContext.get("commands.stats.usage.assigned_mem"), getTotalMemory() + "MB", true)
                        .addField(languageContext.get("commands.stats.usage.assigned_remaining"), getFreeMemory() + "MB", true)
                        .build()
                ).queue();
                TextChannelGround.of(event).dropItemWithChance(4, 5);
            }
        });

        statsCommand.addSubCommand("server", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                TextChannelGround.of(event).dropItemWithChance(4, 5);
                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setAuthor(languageContext.get("commands.stats.server.header"), null, event.getJDA().getSelfUser().getAvatarUrl())
                        .setThumbnail(event.getJDA().getSelfUser().getAvatarUrl())
                        .addField(languageContext.get("commands.stats.server.cpu_usage"), String.format("%.2f", getVpsCPUUsage()) + "%", true)
                        .addField(languageContext.get("commands.stats.server.rem"), String.format("%.2f", getVpsMaxMemory()) + "GB/" + String.format("%.2f", getVpsFreeMemory())
                                + "GB/" + String.format("%.2f", getVpsUsedMemory()) + "GB", false);

                event.getChannel().sendMessage(embedBuilder.build()).queue();
            }
        });

        statsCommand.addSubCommand("cmds", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                String[] args = content.split(" ");
                if(args.length > 0) {
                    String what = args[0];
                    if(what.equals("total")) {
                        event.getChannel().sendMessage(CommandStatsManager.fillEmbed(DefaultBucket.TOTAL, baseEmbed(event, "Command Stats | Total")).build()).queue();
                        return;
                    }

                    if(what.equals("daily")) {
                        event.getChannel().sendMessage(CommandStatsManager.fillEmbed(DefaultBucket.DAY, baseEmbed(event, "Command Stats | Daily")).build()).queue();
                        return;
                    }

                    if(what.equals("hourly")) {
                        event.getChannel().sendMessage(CommandStatsManager.fillEmbed(DefaultBucket.HOUR, baseEmbed(event, "Command Stats | Hourly")).build()).queue();
                        return;
                    }

                    if(what.equals("now")) {
                        event.getChannel().sendMessage(CommandStatsManager.fillEmbed(DefaultBucket.MINUTE, baseEmbed(event, "Command Stats | Now")).build()).queue();
                        return;
                    }
                }

                //Default
                event.getChannel().sendMessage(baseEmbed(event, "Command Stats")
                        .addField(languageContext.get("general.now"), CommandStatsManager.resume(DefaultBucket.MINUTE), false)
                        .addField(languageContext.get("general.hourly"), CommandStatsManager.resume(DefaultBucket.HOUR), false)
                        .addField(languageContext.get("general.daily"), CommandStatsManager.resume(DefaultBucket.DAY), false)
                        .addField(languageContext.get("general.total"), CommandStatsManager.resume(DefaultBucket.TOTAL), false)
                        .build()
                ).queue();
            }
        });

        statsCommand.addSubCommand("guilds", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                String[] args = content.split(" ");
                if(args.length > 0) {
                    String what = args[0];
                    if(what.equals("total")) {
                        event.getChannel().sendMessage(guildStatsManager.fillEmbed(GuildStatsManager.TOTAL_EVENTS, baseEmbed(event, "Guild Stats | Total")).build()).queue();
                        return;
                    }

                    if(what.equals("daily")) {
                        event.getChannel().sendMessage(guildStatsManager.fillEmbed(GuildStatsManager.DAY_EVENTS, baseEmbed(event, "Guild Stats | Daily")).build()).queue();
                        return;
                    }

                    if(what.equals("hourly")) {
                        event.getChannel().sendMessage(guildStatsManager.fillEmbed(GuildStatsManager.HOUR_EVENTS, baseEmbed(event, "Guild Stats | Hourly")).build()).queue();
                        return;
                    }

                    if(what.equals("now")) {
                        event.getChannel().sendMessage(guildStatsManager.fillEmbed(GuildStatsManager.MINUTE_EVENTS, baseEmbed(event, "Guild Stats | Now")).build()).queue();
                        return;
                    }
                }

                //Default
                event.getChannel().sendMessage(baseEmbed(event, "Guild Stats")
                        .addField(languageContext.get("general.now"), guildStatsManager.resume(GuildStatsManager.MINUTE_EVENTS), false)
                        .addField(languageContext.get("general.hourly"), guildStatsManager.resume(GuildStatsManager.HOUR_EVENTS), false)
                        .addField(languageContext.get("general.daily"), guildStatsManager.resume(GuildStatsManager.DAY_EVENTS), false)
                        .addField(languageContext.get("general.total"), guildStatsManager.resume(GuildStatsManager.TOTAL_EVENTS), false)
                        .setFooter("Guilds: " + MantaroBot.getInstance().getGuildCache().size(), null)
                        .build()
                ).queue();
            }
        });

        statsCommand.addSubCommand("category", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                String[] args = content.split(" ");
                if(args.length > 0) {
                    String what = args[0];
                    if(what.equals("total")) {
                        event.getChannel().sendMessage(categoryStatsManager.fillEmbed(CategoryStatsManager.TOTAL_CATS, baseEmbed(event, "Category Stats | Total")).build()).queue();
                        return;
                    }

                    if(what.equals("daily")) {
                        event.getChannel().sendMessage(categoryStatsManager.fillEmbed(CategoryStatsManager.DAY_CATS, baseEmbed(event, "Category Stats | Daily")).build()).queue();
                        return;
                    }

                    if(what.equals("hourly")) {
                        event.getChannel().sendMessage(categoryStatsManager.fillEmbed(CategoryStatsManager.HOUR_CATS, baseEmbed(event, "Category Stats | Hourly")).build()).queue();
                        return;
                    }

                    if(what.equals("now")) {
                        event.getChannel().sendMessage(categoryStatsManager.fillEmbed(CategoryStatsManager.MINUTE_CATS, baseEmbed(event, "Category Stats | Now")).build()).queue();
                        return;
                    }
                }

                //Default
                event.getChannel().sendMessage(baseEmbed(event, "Category Stats")
                        .addField(languageContext.get("general.now"), categoryStatsManager.resume(CategoryStatsManager.MINUTE_CATS), false)
                        .addField(languageContext.get("general.hourly"), categoryStatsManager.resume(CategoryStatsManager.HOUR_CATS), false)
                        .addField(languageContext.get("general.daily"), categoryStatsManager.resume(CategoryStatsManager.DAY_CATS), false)
                        .addField(languageContext.get("general.total"), categoryStatsManager.resume(CategoryStatsManager.TOTAL_CATS), false)
                        .build()
                ).queue();
            }
        });

        statsCommand.addSubCommand("custom", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                event.getChannel().sendMessage(
                        customCommandStatsManager.fillEmbed(CustomCommandStatsManager.TOTAL_CUSTOM_CMDS, baseEmbed(event, "CCS Stats | Total")
                        ).build()).queue();
            }
        });

        statsCommand.addSubCommand("game", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                event.getChannel().sendMessage(baseEmbed(event, "Game Stats").setDescription(gameStatsManager.resume(GameStatsManager.TOTAL_GAMES)).build()).queue();
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
                        languageContext.get("commands.social.note") +"\n").queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Social")
                        .setDescription("**Shows Mantaro's social networks.**")
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
                        BLUE_SMALL_MARKER + "**" + languageContext.get("commands.userinfo.id") + ":** " +
                                user.getId(),
                        BLUE_SMALL_MARKER + "**" + languageContext.get("commands.userinfo.join_date") + ":** "  +
                                member.getJoinDate().format(DateTimeFormatter.ISO_DATE).replace("Z", ""),
                        BLUE_SMALL_MARKER + "**" + languageContext.get("commands.userinfo.created") + ":** " +
                                user.getCreationTime().format(DateTimeFormatter.ISO_DATE).replace("Z", ""),
                        BLUE_SMALL_MARKER + "**" + languageContext.get("commands.userinfo.account_age") + ":** " +
                                TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - user.getCreationTime().toInstant().toEpochMilli()) + " " + languageContext.get("general.days"),
                        BLUE_SMALL_MARKER + "**" + languageContext.get("commands.userinfo.mutual_guilds") + ":** " +
                                MantaroBot.getInstance().getMutualGuilds(event.getAuthor()).size(),
                        BLUE_SMALL_MARKER + "**" + languageContext.get("commands.userinfo.vc") + ":** " +
                                (member.getVoiceState().getChannel() != null ? member.getVoiceState().getChannel().getName() : languageContext.get("general.none")),
                        BLUE_SMALL_MARKER + "**" + languageContext.get("commands.userinfo.color") + ":** " +
                                (member.getColor() == null ? languageContext.get("commands.userinfo.default") : "#" + Integer.toHexString(member.getColor().getRGB()).substring(2).toUpperCase()),
                        BLUE_SMALL_MARKER + "**" + languageContext.get("commands.userinfo.status") + ":** " +
                                Utils.capitalize(member.getOnlineStatus().getKey().toLowerCase())
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
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "User Info Command")
                        .setDescription("**See information about specific users.**")
                        .addField("Usage:",
                                "`~>userinfo @user (or user#disciminator, or nickname)` - **Get information about the specific user.**" +
                                        "\n`~>userinfo` - **Get information about yourself!**", false)
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
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Tips Command")
                        .setDescription("**Shows tips about the bot!**")
                        .build();
            }
        });

        cr.registerAlias("tips", "bottips");
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
                        BLUE_SMALL_MARKER + "**" + languageContext.get("commands.roleinfo.id") + ":** " +
                                r.getId(),
                        BLUE_SMALL_MARKER + "**" + languageContext.get("commands.roleinfo.created") + ":** " +
                                r.getCreationTime().format(DateTimeFormatter.ISO_DATE).replace("Z", ""),
                        BLUE_SMALL_MARKER + "**" + languageContext.get("commands.roleinfo.age") + ":** " +
                                TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - r.getCreationTime().toInstant().toEpochMilli()) + " " + languageContext.get("general.days"),
                        BLUE_SMALL_MARKER + "**" + languageContext.get("commands.roleinfo.color") + ":** " +
                                //Here: remove first two parts of the hex code, which contains transparency data and therefore it's not needed (discord role color transparency is always ff)
                                (r.getColor() == null ? languageContext.get("general.none") : ("#" +  Integer.toHexString(r.getColor().getRGB()).substring(2))),
                        BLUE_SMALL_MARKER + "**" + languageContext.get("commands.roleinfo.members") + ":** " +
                                event.getGuild().getMembers().stream().filter(member -> member.getRoles().contains(r)).count(),
                        BLUE_SMALL_MARKER + "**" + languageContext.get("commands.roleinfo.position") + ":** " +
                                r.getPosition(),
                        BLUE_SMALL_MARKER + "**" + languageContext.get("commands.roleinfo.managed") + ":** " +
                                r.isManaged(),
                        BLUE_SMALL_MARKER +"**" + languageContext.get("commands.roleinfo.hoisted") + ":** " +
                                r.isHoisted()
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
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "User Info Command")
                        .setDescription("**See information about specific users.**")
                        .addField("Usage:",
                                "`~>roleinfo role` - **Get information about the specific role.**" +
                                        "\n`~>roleinfo` - **Get information about top role!**", false)
                        .build();
            }
        });
    }
}
