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

package net.kodehawa.mantarobot.commands;

import com.github.natanbc.usagetracker.DefaultBucket;
import com.google.common.eventbus.Subscribe;
import lavalink.client.io.LavalinkSocket;
import lavalink.client.io.RemoteStats;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.Region;
import net.dv8tion.jda.api.entities.*;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.info.stats.manager.*;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.AliasCommand;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SimpleTreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.*;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.core.processor.DefaultCommandProcessor;
import net.kodehawa.mantarobot.data.I18n;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.kodehawa.mantarobot.commands.info.AsyncInfoMonitor.*;
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
            protected void call(Context ctx, String content, String[] args) {
                ctx.sendLocalized("commands.donate.beg", EmoteReference.HEART,
                        String.format(ctx.getLanguageContext().get("commands.donate.methods"), "https://patreon.com/mantaro", "https://paypal.me/mantarobot")
                );
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Shows the donation methods in case you want to support Mantaro.")
                        .build();
            }
        });
    }

    @Subscribe
    public void language(CommandRegistry cr) {
        cr.register("lang", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                ctx.sendLocalized("commands.lang.info", EmoteReference.ZAP, String.join(", ", I18n.LANGUAGES).replace(".json", ""));
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
            protected void call(Context ctx, String content, String[] args) {
                Member member = Utils.findMember(ctx.getEvent(), ctx.getMember(), content);
                if (member == null)
                    return;

                User u = member.getUser();
                ctx.sendLocalized("commands.avatar.result", EmoteReference.OK, u.getName(), u.getEffectiveAvatarUrl() + "?size=1024");
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
            protected void call(Context ctx, String content, String[] args) {
                Guild guild = ctx.getGuild();

                String roles = guild.getRoles().stream()
                        .filter(role -> !guild.getPublicRole().equals(role))
                        .map(Role::getName)
                        .collect(Collectors.joining(", "));

                Member owner = guild.getOwner();
                //This is wank lol
                if(owner == null)
                    owner = guild.retrieveOwner(false).complete();

                var languageContext = ctx.getLanguageContext();
                ctx.send(new EmbedBuilder()
                        .setAuthor(languageContext.get("commands.serverinfo.header"), null, guild.getIconUrl())
                        .setColor(guild.getOwner().getColor() == null ? Color.ORANGE : guild.getOwner().getColor())
                        .setDescription(String.format(languageContext.get("commands.serverinfo.description"), guild.getName()))
                        .setThumbnail(guild.getIconUrl())
                        .addField(languageContext.get("commands.serverinfo.users"),
                                (int) guild.getMembers().stream().filter(u ->
                                        !u.getOnlineStatus().equals(OnlineStatus.OFFLINE)).count() + "/" + guild.getMembers().size(), true)
                        .addField(languageContext.get("commands.serverinfo.created"),
                                guild.getTimeCreated().format(DateTimeFormatter.ISO_DATE_TIME)
                                        .replaceAll("[^0-9.:-]", " "), true)
                        .addField(languageContext.get("commands.serverinfo.channels"),
                                guild.getVoiceChannels().size() + "/" + guild.getTextChannels().size(), true)
                        .addField(languageContext.get("commands.serverinfo.owner"),
                                owner.getUser().getName() + "#" + owner.getUser().getDiscriminator(), true)
                        .addField(languageContext.get("commands.serverinfo.region"),
                                guild.getRegion() == Region.UNKNOWN ? languageContext.get("general.unknown") :
                                        guild.getRegion().getName(), true)
                        .addField(String.format(languageContext.get("commands.serverinfo.roles"),
                                guild.getRoles().size()), StringUtils.limit(roles, 1016), false)
                        .setFooter(String.format(languageContext.get("commands.serverinfo.id_show"), guild.getId()), null)
                        .build()
                );
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

    private void buildHelp(Context ctx, Category category) {
        DBGuild dbGuild = ctx.getDBGuild();
        String defaultPrefix = ctx.getConfig().prefix[0], guildPrefix = dbGuild.getData().getGuildCustomPrefix();
        String prefix = guildPrefix == null ? defaultPrefix : guildPrefix;
        GuildData guildData = dbGuild.getData();
        var languageContext = ctx.getLanguageContext();

        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor(languageContext.get("commands.help.title"), null, ctx.getGuild().getIconUrl())
                .setColor(Color.PINK)
                .setDescription(
                        (category == null ?
                                languageContext.get("commands.help.base") :
                                String.format(languageContext.get("commands.help.base_category"), languageContext.get(category.toString()))) +
                                languageContext.get("commands.help.support") +
                                (dbGuild.isPremium() || ctx.getDBUser().isPremium() ? "" : languageContext.get("commands.help.patreon")) +
                                //LISP simulator 2018
                                (guildData.getDisabledCommands().isEmpty() ? "" : "\n" +
                                        String.format(languageContext.get("commands.help.disabled_commands"), guildData.getDisabledCommands().size())
                                ) +
                                (guildData.getChannelSpecificDisabledCommands().get(ctx.getChannel().getId()) == null ||
                                        guildData.getChannelSpecificDisabledCommands().get(ctx.getChannel().getId()).isEmpty() ?
                                        "" : "\n" + String.format(languageContext.get("commands.help.channel_specific_disabled_commands"),
                                        guildData.getChannelSpecificDisabledCommands().get(ctx.getChannel().getId()).size())
                                )

                )
                .setFooter(String.format(languageContext.get("commands.help.footer"), prefix,
                        DefaultCommandProcessor.REGISTRY.commands().values().stream().filter(c -> c.category() != null).count()), null);

        Arrays.stream(Category.values())
                .filter(c -> {
                    if (category != null)
                        return c == category;
                    else
                        return true;
                })
                .filter(c -> c != Category.OWNER || CommandPermission.OWNER.test(ctx.getMember()))
                .filter(c -> !DefaultCommandProcessor.REGISTRY.getCommandsForCategory(c).isEmpty())
                .forEach(c -> embed.addField(languageContext.get(c.toString()) + " " + languageContext.get("commands.help.commands") + ":",
                        forType(ctx.getChannel(), guildData, c), false)
                );

        ctx.send(embed.build());
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
                "Helping you help the world.",
                "The help you might need."
        );

        cr.register("help", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (!Utils.handleIncreasingRatelimit(rateLimiter, ctx.getAuthor(), ctx.getEvent(), ctx.getLanguageContext(), false))
                    return;

                if (content.isEmpty()) {
                    buildHelp(ctx, null);
                } else if (Category.lookupFromString(content) != null) {
                    Category category = Category.lookupFromString(content);
                    buildHelp(ctx, category);
                } else {
                    Command command = DefaultCommandProcessor.REGISTRY.commands().get(content);

                    if (command != null) {
                        if (command.category() == Category.OWNER && !CommandPermission.OWNER.test(ctx.getMember())) {
                            ctx.sendLocalized("commands.help.extended.not_found", EmoteReference.ERROR);
                            return;
                        }

                        var languageContext = ctx.getLanguageContext();

                        if (command.help() != null && command.help().getDescription() != null) {
                            HelpContent newHelp = command.help();
                            List<String> descriptionList = newHelp.getDescriptionList();

                            EmbedBuilder builder = new EmbedBuilder()
                                    .setColor(Color.PINK)
                                    //assume content = command name
                                    .setAuthor(Utils.capitalize(content) + " Command Help", null, ctx.getAuthor().getEffectiveAvatarUrl())
                                    .setThumbnail("https://cdn.pixabay.com/photo/2012/04/14/16/26/question-34499_960_720.png")
                                    .setDescription((r.nextBoolean() ? languageContext.get("commands.help.patreon") + "\n" : "")
                                            + (descriptionList.isEmpty() ? newHelp.getDescription() : descriptionList.get(r.nextInt(descriptionList.size())))
                                    ).setFooter("Don't include <> or [] on the command itself.", ctx.getAuthor().getEffectiveAvatarUrl());

                            if (newHelp.getUsage() != null) {
                                builder.addField("Usage", newHelp.getUsage(), false);
                            }

                            if (newHelp.getParameters().size() > 0) {
                                builder.addField("Parameters", newHelp.getParameters().entrySet().stream()
                                        .map(entry -> "`" + entry.getKey() + "` - *" + entry.getValue() + "*")
                                        .collect(Collectors.joining("\n")), false);

                            }

                            if (newHelp.isSeasonal()) {
                                builder.addField("Seasonal", "This command allows the usage of the `-season` (or `-s`) argument.", false);
                            }

                            //Ensure sub-commands show in help.
                            //Only god shall help me now with all of this casting lol.
                            if (command instanceof AliasCommand) {
                                command = ((AliasCommand) command).getCommand();
                            }

                            if (command instanceof ITreeCommand) {
                                Map<String, SubCommand> subCommands = ((ITreeCommand) command).getSubCommands();
                                StringBuilder stringBuilder = new StringBuilder();

                                for (Map.Entry<String, SubCommand> inners : subCommands.entrySet()) {
                                    String name = inners.getKey();
                                    InnerCommand inner = inners.getValue();
                                    if (inner.isChild())
                                        continue;

                                    if (inner.description() != null) {
                                        stringBuilder.append(EmoteReference.BLUE_SMALL_MARKER).append("`").append(name).append("` - ").append(inner.description()).append("\n");
                                    }
                                }

                                if (stringBuilder.length() > 0) {
                                    builder.addField("Sub-commands", "**Append the main command to use any of this.**\n" + stringBuilder.toString(), false);
                                }
                            }

                            //Known command aliases.
                            List<String> commandAliases = command.getAliases();
                            if (!commandAliases.isEmpty()) {
                                String aliases = commandAliases.stream().filter(alias -> !alias.equalsIgnoreCase(content)).map(alias -> "`" + alias + "`").collect(Collectors.joining(" "));
                                if (!aliases.trim().isEmpty()) {
                                    builder.addField("Aliases", aliases, false);
                                }
                            }

                            ctx.send(builder.build());
                        } else {
                            ctx.sendLocalized("commands.help.extended.no_help", EmoteReference.ERROR);
                        }
                    } else {
                        ctx.sendLocalized("commands.help.extended.not_found", EmoteReference.ERROR);
                    }
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("I wonder if this is what you are looking for...")
                        .setDescriptionList(jokes)
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
            protected void call(Context ctx, String content, String[] args) {
                var languageContext = ctx.getLanguageContext();

                ctx.send(new EmbedBuilder()
                        .setAuthor("Mantaro's Invite URL.", null, ctx.getSelfUser().getAvatarUrl())
                        .addField(languageContext.get("commands.invite.url"), "http://add.mantaro.site", false)
                        .addField(languageContext.get("commands.invite.server"), "https://support.mantaro.site", false)
                        .addField(languageContext.get("commands.invite.patreon"), "http://patreon.com/mantaro", false)
                        .setDescription(languageContext.get("commands.invite.description.1") + " " +
                                languageContext.get("commands.invite.description.2") + "\n" +
                                languageContext.get("commands.invite.description.3") + " " +
                                languageContext.get("commands.invite.description.4"))
                        .setFooter(languageContext.get("commands.invite.footer"), ctx.getSelfUser().getAvatarUrl())
                        .build()
                );
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
            protected void call(Context ctx, String content, String[] args) {
                DBGuild dbGuild = ctx.getDBGuild();
                String defaultPrefix = Stream.of(ctx.getConfig().getPrefix()).map(prefix -> "`" + prefix + "`").collect(Collectors.joining(" "));
                String guildPrefix = dbGuild.getData().getGuildCustomPrefix();

                ctx.sendLocalized("commands.prefix.header", EmoteReference.HEART, defaultPrefix, guildPrefix == null ? ctx.getLanguageContext().get("commands.prefix.none") : guildPrefix);
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
            protected void call(Context ctx, String content) {
                I18nContext languageContext = ctx.getLanguageContext();

                ctx.send(new EmbedBuilder()
                                .setAuthor(languageContext.get("commands.stats.usage.header"), null, ctx.getSelfUser().getAvatarUrl())
                                .setDescription(languageContext.get("commands.stats.usage.description"))
                                .setThumbnail(ctx.getSelfUser().getAvatarUrl())
                                .addField(languageContext.get("commands.stats.usage.threads"),
                                        getThreadCount() + " Threads", false)
                                .addField(languageContext.get("commands.stats.usage.memory_usage"),
                                        Utils.formatMemoryUsage(getTotalMemory() - getFreeMemory(), getMaxMemory()), false)
                                .addField(languageContext.get("commands.stats.usage.cores"),
                                        getAvailableProcessors() + " Cores", true)
                                .addField(languageContext.get("commands.stats.usage.cpu_usage"),
                                        String.format("%.2f", getInstanceCPUUsage()) + "%", true)
                                .addField(languageContext.get("commands.stats.usage.assigned_mem"),
                                        Utils.formatMemoryAmount(getTotalMemory()), false)
                                .addField(languageContext.get("commands.stats.usage.assigned_remaining"),
                                        Utils.formatMemoryAmount(getFreeMemory()), true)
                                .build()
                );

                TextChannelGround.of(ctx.getEvent()).dropItemWithChance(4, 5);
            }
        });

        statsCommand.addSubCommand("nodes", new SubCommand() {
            @Override
            public String description() {
                return "Mantaro node statistics.";
            }

            @Override
            protected void call(Context ctx, String content) {
                Map<String, String> nodeMap;
                try (Jedis jedis = ctx.getJedisPool().getResource()) {
                    nodeMap = jedis.hgetAll("node-stats-" + ctx.getConfig().getClientId());
                }

                var embed = new EmbedBuilder().setTitle("Mantaro Node Statistics")
                        .setDescription("This shows the current status of the online nodes. " +
                                "Every node contains a set amount of shards.")
                        .setThumbnail(ctx.getSelfUser().getAvatarUrl())
                        .setColor(Color.PINK)
                        .setFooter("Available Nodes: " + nodeMap.size());

                List<MessageEmbed.Field> fields = new LinkedList<>();
                for(var node : nodeMap.entrySet()) {
                    var nodeData = new JSONObject(node.getValue());
                    fields.add(new MessageEmbed.Field("Node " + node.getKey(),
                            String.format("**Uptime**: %s\n" +
                                    "**CPU Cores**: %s\n" +
                                    "**CPU Usage**: %s\n" +
                                    "**Memory**: %s\n" +
                                    "**Threads**: %,d\n" +
                                    "**Shards**: %s\n" +
                                    "**Guilds**: %,d\n" +
                                    "**Users**: %,d\n" +
                                    "**Machine Memory**: %s\n",
                                    Utils.formatDuration(nodeData.getLong("uptime")),
                                    nodeData.getLong("available_processors"),
                                    nodeData.getLong("machine_cpu_usage") + "%",
                                    Utils.formatMemoryUsage(nodeData.getLong("used_memory"), nodeData.getLong("total_memory")),
                                    nodeData.getLong("thread_count"),
                                    nodeData.getString("shard_slice"),
                                    nodeData.getLong("guild_count"),
                                    nodeData.getLong("user_count"),
                                    Utils.formatMemoryAmount(nodeData.getLong("machine_total_memory"))),
                            false
                    ));
                }

                var splitFields = DiscordUtils.divideFields(3, fields);
                boolean hasReactionPerms = ctx.hasReactionPerms();

                if (hasReactionPerms)
                    DiscordUtils.list(ctx.getEvent(), 200, false, embed, splitFields);
                else
                    DiscordUtils.listText(ctx.getEvent(), 200, false, embed, splitFields);
            }
        });

        statsCommand.addSubCommand("lavalink", new SubCommand() {
            @Override
            public String description() {
                return "Lavalink node statistics.";
            }

            @Override
            protected void call(Context ctx, String content) {
                List<LavalinkSocket> nodes = ctx.getBot().getLavaLink().getNodes();
                var embed = new EmbedBuilder();
                embed.setTitle("Lavalink Node Statistics")
                        .setDescription("This shows the current status of the online Lavalink nodes. " +
                                "Every node contains a dynamic amount of players. This is for balancing music processes " +
                                "outside of the main bot nodes.")
                        .setThumbnail(ctx.getSelfUser().getAvatarUrl())
                        .setColor(Color.PINK)
                        .setFooter("Available Nodes: " + nodes.size());

                List<MessageEmbed.Field> fields = new LinkedList<>();

                for (LavalinkSocket node : nodes) {
                    if(!node.isAvailable())
                        continue;

                    RemoteStats stats = node.getStats();
                    fields.add(new MessageEmbed.Field(node.getName(),
                            String.format("**Uptime**: %s\n" +
                                    "**Used Memory**: %s\n" +
                                    "**Free Memory**: %s\n" +
                                    "**Players**: %,d\n" +
                                    "**Players Playing**: %,d",
                                    Utils.formatDuration(stats.getUptime()),
                                    Utils.formatMemoryAmount(stats.getMemUsed()),
                                    Utils.formatMemoryAmount(stats.getMemFree()),
                                    stats.getPlayers(),
                                    stats.getPlayingPlayers()
                            ), false
                    ));
                }

                var splitFields = DiscordUtils.divideFields(3, fields);
                boolean hasReactionPerms = ctx.hasReactionPerms();

                if (hasReactionPerms)
                    DiscordUtils.list(ctx.getEvent(), 200, false, embed, splitFields);
                else
                    DiscordUtils.listText(ctx.getEvent(), 200, false, embed, splitFields);
            }
        });

        statsCommand.addSubCommand("cmds", new SubCommand() {
            @Override
            public String description() {
                return "The bot's command usage";
            }

            @Override
            protected void call(Context ctx, String content) {
                String[] args = ctx.getArguments();
                if (args.length > 0) {
                    String what = args[0];
                    if (what.equals("total")) {
                        ctx.send(CommandStatsManager.fillEmbed(DefaultBucket.TOTAL, baseEmbed(ctx, "Command Stats | Total")).build());
                        return;
                    }

                    if (what.equals("daily")) {
                        ctx.send(CommandStatsManager.fillEmbed(DefaultBucket.DAY, baseEmbed(ctx, "Command Stats | Daily")).build());
                        return;
                    }

                    if (what.equals("hourly")) {
                        ctx.send(CommandStatsManager.fillEmbed(DefaultBucket.HOUR, baseEmbed(ctx, "Command Stats | Hourly")).build());
                        return;
                    }

                    if (what.equals("now")) {
                        ctx.send(CommandStatsManager.fillEmbed(DefaultBucket.MINUTE, baseEmbed(ctx, "Command Stats | Now")).build());
                        return;
                    }
                }

                //Default
                var languageContext = ctx.getLanguageContext();
                ctx.send(
                        baseEmbed(ctx, "Command Stats")
                                .addField(languageContext.get("general.now"), CommandStatsManager.resume(DefaultBucket.MINUTE), false)
                                .addField(languageContext.get("general.hourly"), CommandStatsManager.resume(DefaultBucket.HOUR), false)
                                .addField(languageContext.get("general.daily"), CommandStatsManager.resume(DefaultBucket.DAY), false)
                                .addField(languageContext.get("general.total"), CommandStatsManager.resume(DefaultBucket.TOTAL), false)
                                .build()
                );
            }
        });

        statsCommand.addSubCommand("category", new SubCommand() {
            @Override
            public String description() {
                return "The bot's category usage";
            }

            @Override
            protected void call(Context ctx, String content) {
                String[] args = ctx.getArguments();
                if (args.length > 0) {
                    String what = args[0];
                    if (what.equals("total")) {
                        ctx.send(categoryStatsManager.fillEmbed(CategoryStatsManager.TOTAL_CATS, baseEmbed(ctx, "Category Stats | Total")).build());
                        return;
                    }

                    if (what.equals("daily")) {
                        ctx.send(categoryStatsManager.fillEmbed(CategoryStatsManager.DAY_CATS, baseEmbed(ctx, "Category Stats | Daily")).build());
                        return;
                    }

                    if (what.equals("hourly")) {
                        ctx.send(categoryStatsManager.fillEmbed(CategoryStatsManager.HOUR_CATS, baseEmbed(ctx, "Category Stats | Hourly")).build());
                        return;
                    }

                    if (what.equals("now")) {
                        ctx.send(categoryStatsManager.fillEmbed(CategoryStatsManager.MINUTE_CATS, baseEmbed(ctx, "Category Stats | Now")).build());
                        return;
                    }
                }

                //Default
                var languageContext = ctx.getLanguageContext();
                ctx.send(
                        baseEmbed(ctx, "Category Stats")
                                .addField(languageContext.get("general.now"), categoryStatsManager.resume(CategoryStatsManager.MINUTE_CATS), false)
                                .addField(languageContext.get("general.hourly"), categoryStatsManager.resume(CategoryStatsManager.HOUR_CATS), false)
                                .addField(languageContext.get("general.daily"), categoryStatsManager.resume(CategoryStatsManager.DAY_CATS), false)
                                .addField(languageContext.get("general.total"), categoryStatsManager.resume(CategoryStatsManager.TOTAL_CATS), false)
                                .build()
                );
            }
        });
    }

    @Subscribe
    public void userinfo(CommandRegistry cr) {
        cr.register("userinfo", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                Member member = Utils.findMember(ctx.getEvent(), ctx.getMember(), content);
                if (member == null)
                    return;

                User user = member.getUser();

                String roles = member.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.joining(", "));

                var languageContext = ctx.getLanguageContext();
                String s = String.join("\n",
                        prettyDisplay(languageContext.get("commands.userinfo.id"), user.getId()),
                        prettyDisplay(languageContext.get("commands.userinfo.join_date"),
                                member.getTimeJoined().format(DateTimeFormatter.ISO_DATE).replace("Z", "")
                        ),
                        prettyDisplay(languageContext.get("commands.userinfo.created"),
                                user.getTimeCreated().format(DateTimeFormatter.ISO_DATE).replace("Z", "")
                        ),
                        prettyDisplay(languageContext.get("commands.userinfo.account_age"),
                                TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - user.getTimeCreated().toInstant().toEpochMilli())
                                        + " " + languageContext.get("general.days")
                        ),
                        prettyDisplay(languageContext.get("commands.userinfo.mutual_guilds"), String.valueOf(MantaroBot.getInstance()
                                .getShardManager()
                                .getMutualGuilds(ctx.getAuthor()).size())
                        ),
                        prettyDisplay(languageContext.get("commands.userinfo.vc"),
                                member.getVoiceState().getChannel() != null ?
                                        member.getVoiceState().getChannel().getName() :
                                        languageContext.get("general.none")
                        ),
                        prettyDisplay(languageContext.get("commands.userinfo.color"),
                                member.getColor() == null ? languageContext.get("commands.userinfo.default") : "#" +
                                        Integer.toHexString(member.getColor().getRGB()).substring(2).toUpperCase()
                        ),
                        prettyDisplay(languageContext.get("commands.userinfo.status"), Utils.capitalize(member.getOnlineStatus().getKey().toLowerCase()))
                );

                ctx.send(new EmbedBuilder()
                        .setColor(member.getColor())
                        .setAuthor(String.format(languageContext.get("commands.userinfo.header"),
                                user.getName(), user.getDiscriminator()), null, ctx.getAuthor().getEffectiveAvatarUrl()
                        ).setThumbnail(user.getEffectiveAvatarUrl())
                        .setDescription(s)
                        .addField(String.format(languageContext.get("commands.userinfo.roles"),
                                member.getRoles().size()), StringUtils.limit(roles, 900), true
                        ).build()
                );
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
    public void season(CommandRegistry registry) {
        registry.register("season", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                I18nContext languageContext = ctx.getLanguageContext();

                ctx.sendFormat(languageContext.get("commands.season.info") + languageContext.get("commands.season.info_2"),
                        ctx.getConfig().getCurrentSeason().getDisplay(), ctx.db().getAmountSeasonalPlayers()
                );
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
            protected void call(Context ctx, String content, String[] args) {
                ctx.sendLocalized("commands.support.info", EmoteReference.POPPER);
            }
        });
    }

    @Subscribe
    public void roleinfo(CommandRegistry cr) {
        cr.register("roleinfo", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                Role r = Utils.findRole(ctx.getEvent(), content);
                if (r == null)
                    return;

                var languageContext = ctx.getLanguageContext();
                String s = String.join("\n",
                        prettyDisplay(languageContext.get("commands.roleinfo.id"), r.getId()),
                        prettyDisplay(languageContext.get("commands.roleinfo.created"),
                                r.getTimeCreated().format(DateTimeFormatter.ISO_DATE).replace("Z", "")
                        ),
                        prettyDisplay(languageContext.get("commands.roleinfo.age"),
                                TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - r.getTimeCreated().toInstant().toEpochMilli()) +
                                        " " + languageContext.get("general.days")
                        ),
                        prettyDisplay(languageContext.get("commands.roleinfo.color"),
                                r.getColor() == null ?
                                        languageContext.get("general.none") :
                                        ("#" + Integer.toHexString(r.getColor().getRGB()).substring(2))
                        ),
                        prettyDisplay(languageContext.get("commands.roleinfo.members"),
                                String.valueOf(ctx.getGuild().getMembers().stream().filter(member -> member.getRoles().contains(r)).count())
                        ),
                        prettyDisplay(languageContext.get("commands.roleinfo.position"), String.valueOf(r.getPosition())),
                        prettyDisplay(languageContext.get("commands.roleinfo.hoisted"), String.valueOf(r.isHoisted()))
                );

                ctx.send(
                        new EmbedBuilder()
                                .setColor(ctx.getMember().getColor())
                                .setAuthor(String.format(languageContext.get("commands.roleinfo.header"),
                                        r.getName()), null, ctx.getGuild().getIconUrl()
                                ).setDescription(s)
                                .addField(String.format(languageContext.get("commands.roleinfo.permissions"), r.getPermissions().size()),
                                        r.getPermissions().size() == 0 ? languageContext.get("general.none") :
                                                r.getPermissions().stream().map(Permission::getName).collect(Collectors.joining(", ")) + ".",
                                        false
                                )
                                .build()
                );
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
