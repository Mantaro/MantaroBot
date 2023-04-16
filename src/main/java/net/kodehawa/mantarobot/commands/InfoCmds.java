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

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.Category;
import net.kodehawa.mantarobot.core.command.meta.Defer;
import net.kodehawa.mantarobot.core.command.meta.Description;
import net.kodehawa.mantarobot.core.command.meta.Ephemeral;
import net.kodehawa.mantarobot.core.command.meta.Help;
import net.kodehawa.mantarobot.core.command.meta.Name;
import net.kodehawa.mantarobot.core.command.meta.Options;
import net.kodehawa.mantarobot.core.command.slash.ContextCommand;
import net.kodehawa.mantarobot.core.command.slash.IContext;
import net.kodehawa.mantarobot.core.command.slash.InteractionContext;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.data.I18n;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;

import java.awt.Color;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.utils.commands.EmoteReference.BLUE_SMALL_MARKER;

@Module
public class InfoCmds {
    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(Information.class);
        cr.registerSlash(Avatar.class);
        cr.registerSlash(Info.class);
        cr.registerContextUser(UserInfo.class);
    }

    @Name("mantaro")
    @Description("Shows useful bot information (not statistics).")
    @Category(CommandCategory.INFO)
    @Help(description = "Shows useful bot information.")
    public static class Information extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {}

        @Name("support")
        @Description("Shows a link to the support server")
        @Help(description = "Shows a link to the support server")
        public static class Support extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                ctx.replyEphemeral("commands.support.info", EmoteReference.POPPER);
            }
        }

        @Name("donate")
        @Description("Shows the donation methods in case you want to support Mantaro.")
        @Help(description = "Shows the donation methods in case you want to support Mantaro.")
        public static class Donate extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                ctx.replyEphemeral("commands.donate.beg", EmoteReference.HEART,
                        ctx.getLanguageContext().get("commands.donate.methods")
                                .formatted("https://patreon.com/mantaro", "https://paypal.me/kodemantaro")
                );
            }
        }

        @Name("language")
        @Description("Shows how to change the server and user languages, along with a language list.")
        @Help(description =
                """
                Shows how to change the server and user languages, along with a language list.
                You can change the server language (if applicable) using `~>opts language set <language code>`.
                Your personal language preferences can be changed using `~>profile lang <language code>`.
                Use the command to get a list of language codes.
                """
        )
        public static class Language extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                ctx.replyEphemeral("commands.lang.info", EmoteReference.ZAP,
                        String.join(", ", I18n.LANGUAGES).replace(".json", "")
                );
            }
        }

        @Description("Shows the message the bot sends when it's added to a server.")
        @Help(description = "Shows the message the bot sends when it's added to a server.")
        public static class Welcome extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                final var embedBuilder = new EmbedBuilder()
                        .setThumbnail(ctx.getJDA().getSelfUser().getEffectiveAvatarUrl())
                        .setColor(Color.PINK)
                        .setDescription("""
                                Welcome to **Mantaro**, a fun, quirky and complete Discord bot! Thanks for adding me to your server, I highly appreciate it <3
                                We have music, currency (money/economy), games and way more stuff you can check out!
                                Make sure you use the `~>help` command to make yourself comfy and to get started with the bot!

                                If you're interested in supporting Mantaro, check out our Patreon page below, it'll greatly help to improve the bot.
                                Check out the links below for some help resources and quick start guides.
                                This message will only be shown once.""")
                        .addField(EmoteReference.PENCIL.toHeaderString() + "Important Links",
                                """
                                        [Support Server](https://support.mantaro.site) - The place to check if you're lost or if there's an issue with the bot.
                                        [Official Wiki](https://www.mantaro.site/mantaro-wiki) - Good place to check if you're lost.
                                        [Custom Commands](https://www.mantaro.site/mantaro-wiki/guides/custom-commands) - Great customizability for your server needs!
                                        [Currency Guide](https://www.mantaro.site/mantaro-wiki/currency/101) - A lot of fun to be had!
                                        [Configuration](https://www.mantaro.site/mantaro-wiki/basics/server-configuration) -  Customizability for your server needs!
                                        [Patreon](https://patreon.com/mantaro) - Help Mantaro's development directly by donating a small amount of money each month.
                                        [Official Website](https://mantaro.site) - A cool website.""",
                                true
                        ).setFooter("We hope you enjoy using Mantaro! For any questions, go to our support server.");

                ctx.replyEphemeral(embedBuilder.build());
            }
        }
        @Name("invite")
        @Description("Gives you a bot OAuth invite link and some other important links.")
        @Help(description = "Gives you a bot OAuth invite link and some other important links.")
        public static class Invite extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var languageContext = ctx.getLanguageContext();

                ctx.replyEphemeral(new EmbedBuilder()
                        .setAuthor("Mantaro's Invite URL.", null, ctx.getSelfUser().getAvatarUrl())
                        .addField(languageContext.get("commands.invite.url"),
                                "http://add.mantaro.site",
                                false
                        )
                        .addField(languageContext.get("commands.invite.server"),
                                "https://support.mantaro.site",
                                false)

                        .addField(languageContext.get("commands.invite.patreon"),
                                "http://patreon.com/mantaro",
                                false
                        )
                        .setDescription(
                                languageContext.get("commands.invite.description.1") + " " +
                                        languageContext.get("commands.invite.description.2") + "\n" +
                                        languageContext.get("commands.invite.description.3") + " " +
                                        languageContext.get("commands.invite.description.4")
                        )
                        .setFooter(languageContext.get("commands.invite.footer"), ctx.getSelfUser().getAvatarUrl())
                        .build()
                );
            }
        }

        @Name("shardlist")
        @Defer
        @Ephemeral
        @Description("Returns information about shards.")
        @Help(description = "Returns information about shards.")
        public static class ShardInfo extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                StringBuilder builder = new StringBuilder();
                Map<String, String> stats;

                try(Jedis jedis = ctx.getJedisPool().getResource()) {
                    stats = jedis.hgetAll("shardstats-" + ctx.getConfig().getClientId());
                }

                //id, shard_status, cached_users, guild_count, last_ping_diff, gateway_ping
                stats.entrySet().stream().sorted(
                        Comparator.comparingInt(e -> Integer.parseInt(e.getKey()))
                ).forEach(shard -> {
                    var jsonData = new JSONObject(shard.getValue());
                    var shardId = Integer.parseInt(shard.getKey());

                    builder.append("%-7s | %-9s | U: %-6d | G: %-4d | EV: %-8s | P: %-6s".formatted(
                            shardId + " / " + ctx.getBot().getShardManager().getShardsTotal(),
                            jsonData.getString("shard_status"),
                            jsonData.getLong("cached_users"),
                            jsonData.getLong("guild_count"),
                            jsonData.getLong("last_ping_diff") + " ms",
                            jsonData.getLong("gateway_ping")
                    ));

                    if (shardId == ctx.getJDA().getShardInfo().getShardId()) {
                        builder.append(" <- CURRENT");
                    }

                    builder.append("\n");
                });

                List<String> m = DiscordUtils.divideString(builder);
                List<String> messages = new LinkedList<>();

                for (String shard : m) {
                    messages.add("%s%n%n```prolog%n%s```"
                            .formatted("**Mantaro's Shard Information**", shard)
                    );
                }

                DiscordUtils.listButtons(ctx.getUtilsContext(), 150, messages);
            }
        }

        @Name("shard")
        @Description("Returns in what shard I am.")
        @Help(description = "Returns in what shard I am.")
        public static class Shard extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                long nodeAmount;
                try(Jedis jedis = MantaroData.getDefaultJedisPool().getResource()) {
                    nodeAmount = jedis.hlen("node-stats-" + ctx.getConfig().getClientId());
                }

                final var jda = ctx.getJDA();
                final var guildCache = jda.getGuildCache();

                ctx.replyEphemeral("commands.shard.info",
                        jda.getShardInfo().getShardId(),
                        ctx.getBot().getShardManager().getShardsTotal(),
                        ctx.getBot().getNodeNumber(), nodeAmount,
                        guildCache.size(), jda.getUserCache().size(),
                        guildCache.stream().mapToLong(guild -> guild.getMemberCache().size()).sum()
                );
            }
        }
    }

    @Description("Get a user's avatar URL.")
    @Category(CommandCategory.INFO)
    @Options({
          @Options.Option(type = OptionType.USER, name = "user", description = "The user to get the avatar of.")
    })
    @Help(description = "Get a user's avatar URL.", usage = "`/avatar [user]`" ,parameters = {
            @Help.Parameter(name = "User", description = "The user to get the avatar of.", optional = true)
    })
    public static class Avatar extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var member = ctx.getOptionAsMember("user", ctx.getMember());
            var languageContext = ctx.getLanguageContext();

            var embed = new EmbedBuilder()
                    .setColor(ctx.getMemberColor(member))
                    .setAuthor(
                            languageContext.get("commands.avatar.result").formatted(member.getEffectiveName()),
                            null, member.getEffectiveAvatarUrl()
                    )
                    .setImage(member.getEffectiveAvatarUrl() + "?size=1024")
                    .setFooter(languageContext.get("commands.avatar.footer"), member.getEffectiveAvatarUrl());

            ctx.reply(embed.build());
        }
    }

    @Description("The hub for info related commands.")
    @Category(CommandCategory.INFO)
    @Help(description = "The hub for (user/role/server) info related commands.")
    public static class Info extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {}

        @Name("user")
        @Description("See information about specific users.")
        @Help(description = "See information about specific users.", usage = "`/info user user:[user]`", parameters = {
                @Help.Parameter(name = "user", description = "The user you want to look.", optional = true)
        })
        @Options({
                @Options.Option(type = OptionType.USER, name = "user", description = "The user to see the information of.")
        })
        public static class UserInfo extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var user = ctx.getOptionAsUser("user", ctx.getAuthor());
                userInfo(ctx, user);
            }
        }

        @Name("server")
        @Description("See your server's current stats.")
        @Help(description = "See your server's current stats.")
        public static class ServerInfo extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var guild = ctx.getGuild();
                var guildData = ctx.getDBGuild();
                var roles = guild.getRoles().stream()
                        .filter(role -> !guild.getPublicRole().equals(role))
                        .map(Role::getName)
                        .collect(Collectors.joining(", "));

                // Retrieves from cache if we have it.
                var owner = guild.retrieveOwner().useCache(true).complete();
                var languageContext = ctx.getLanguageContext();
                var str = """
                        **%1$s**
                        %2$s **%3$s:** %4$s
                        %2$s **%5$s:** %6$s
                        %2$s **%7$s:** %8$s
                        %2$s **%9$s:** %10$s
                        """.formatted(languageContext.get("commands.serverinfo.description").formatted(guild.getName()),
                        BLUE_SMALL_MARKER,
                        languageContext.get("commands.serverinfo.users"),
                        guild.getMemberCount(),
                        languageContext.get("commands.serverinfo.channels"),
                        "%,d / %,d".formatted(guild.getVoiceChannels().size(), guild.getTextChannels().size()),
                        languageContext.get("commands.serverinfo.owner"),
                        owner.getUser().getAsTag(),
                        languageContext.get("commands.serverinfo.created"),
                        Utils.formatDate(guild.getTimeCreated(), guildData.getLang())
                );

                ctx.reply(new EmbedBuilder()
                        .setAuthor(languageContext.get("commands.serverinfo.header"), null, guild.getIconUrl())
                        .setColor(ctx.getMemberColor(owner))
                        .setDescription(str)
                        .setThumbnail(guild.getIconUrl())
                        .addField(
                                languageContext.get("commands.serverinfo.roles").formatted(guild.getRoles().size()),
                                StringUtils.limit(roles, 500), false
                        )
                        .setFooter(languageContext.get("commands.serverinfo.id_show").formatted(guild.getId()), null)
                        .build()
                );
            }
        }

        @Name("role")
        @Description("See information about a role.")
        @Options({
                @Options.Option(type = OptionType.ROLE, name = "role", description = "The role to see the information of.", required = true)
        })
        @Help(description = "See information about a role.", usage = "`/info role role:[role name]`", parameters = {
                @Help.Parameter(name = "role", description = "The role you want to see the information of.")
        })
        public static class RoleInfo extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var role = ctx.getOptionAsRole("role");
                var lang = ctx.getLanguageContext();
                var str = """
                        %1$s **%2$s:** %3$s
                        %1$s **%4$s:** %5$s
                        %1$s **%6$s:** %7$s
                        %1$s **%8$s:** %9$s
                        %1$s **%10$s:** %11$s
                        """.formatted(BLUE_SMALL_MARKER,
                        lang.get("commands.roleinfo.id"),
                        role.getId(),
                        lang.get("commands.roleinfo.created"),
                        Utils.formatDate(role.getTimeCreated(), ctx.getDBGuild().getLang()),
                        lang.get("commands.roleinfo.color"),
                        role.getColor() == null ?
                                lang.get("general.none") :
                                "#%s".formatted(Integer.toHexString(role.getColor().getRGB()).substring(2)),
                        lang.get("commands.roleinfo.position"), role.getPosition(),
                        lang.get("commands.roleinfo.hoisted"), role.isHoisted()
                );

                ctx.reply(new EmbedBuilder()
                        .setColor(ctx.getMember().getColor())
                        .setAuthor(lang.get("commands.roleinfo.header").formatted(role.getName()),
                                null, ctx.getGuild().getIconUrl()
                        )
                        .setDescription(str)
                        .addField(lang.get("commands.roleinfo.permissions").formatted(role.getPermissions().size()),
                                role.getPermissions().isEmpty() ? lang.get("general.none") :
                                        role.getPermissions()
                                                .stream()
                                                .map(Permission::getName)
                                                .collect(Collectors.joining(", ")) + ".",
                                false
                        ).build()
                );
            }
        }
    }

    @Name("User information")
    public static class UserInfo extends ContextCommand<User> {
        @Override
        protected void process(InteractionContext<User> ctx) {
            var user = ctx.getTarget();
            userInfo(ctx, user);
        }
    }

    private static void userInfo(IContext ctx, User user) {
        var guildData = ctx.getDBGuild();
        var member = ctx.getGuild().getMember(user);
        if (member == null) {
            ctx.sendLocalized("general.slash_member_lookup_failure", EmoteReference.ERROR);
            return;
        }

        var roles = member.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.joining(", "));

        var languageContext = ctx.getLanguageContext();
        var voiceState = member.getVoiceState();
        var str = """
                            %1$s **%2$s:** %3$s
                            %1$s **%4$s:** %5$s
                            %1$s **%6$s:** %7$s
                            %1$s **%8$s:** %9$s
                            %1$s **%10$s:** %11$s
                            %1$s **%12$s:** %13$s
                            """.formatted(BLUE_SMALL_MARKER,
                languageContext.get("commands.userinfo.id"), user.getId(),
                languageContext.get("commands.userinfo.join_date"),
                Utils.formatDate(member.getTimeJoined(), guildData.getLang()),
                languageContext.get("commands.userinfo.created"),
                Utils.formatDate(user.getTimeCreated(), guildData.getLang()),
                languageContext.get("commands.userinfo.account_age"),
                TimeUnit.MILLISECONDS.toDays(
                        System.currentTimeMillis() - user.getTimeCreated().toInstant().toEpochMilli())
                        + " " + languageContext.get("general.days"),
                languageContext.get("commands.userinfo.vc"),
                voiceState != null && voiceState.getChannel() != null ?
                        voiceState.getChannel().getName() : languageContext.get("general.none"),
                languageContext.get("commands.userinfo.color"),
                member.getColor() == null ? languageContext.get("commands.userinfo.default") :
                        "#%s".formatted(Integer.toHexString(member.getColor().getRGB()).substring(2).toUpperCase())
        );

        ctx.send(new EmbedBuilder()
                .setColor(ctx.getMember().getColor())
                .setAuthor(
                        languageContext.get("commands.userinfo.header")
                                .formatted( user.getName(), user.getDiscriminator()),
                        null, ctx.getAuthor().getEffectiveAvatarUrl()
                )
                .setThumbnail(user.getEffectiveAvatarUrl())
                .setDescription(str)
                .addField(
                        languageContext.get("commands.userinfo.roles").formatted(member.getRoles().size()),
                        StringUtils.limit(roles, 900), true
                ).build()
        );
    }
}
