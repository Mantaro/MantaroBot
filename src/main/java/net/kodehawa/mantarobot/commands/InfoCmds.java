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

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.Region;
import net.dv8tion.jda.api.entities.Role;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.I18n;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.CustomFinderUtil;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.FinderUtils;

import java.awt.Color;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.kodehawa.mantarobot.utils.commands.EmoteReference.BLUE_SMALL_MARKER;

@Module
public class InfoCmds {
    @Subscribe
    public void donate(CommandRegistry cr) {
        cr.register("donate", new SimpleCommand(CommandCategory.INFO) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                ctx.sendLocalized("commands.donate.beg", EmoteReference.HEART,
                        ctx.getLanguageContext().get("commands.donate.methods")
                                .formatted("https://patreon.com/mantaro", "https://paypal.me/kodemantaro")
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
        cr.register("lang", new SimpleCommand(CommandCategory.INFO) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                ctx.sendLocalized("commands.lang.info", EmoteReference.ZAP,
                        String.join(", ", I18n.LANGUAGES).replace(".json", "")
                );
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription(
                                """
                                Shows how to change the server and user languages, along with a language list.
                                You can change the server language (if applicable) using `~>opts language set <language code>`. 
                                Your personal language preferences can be changed using `~>profile lang <language code>`.
                                Use the command to get a list of language codes.
                                """
                        )
                        .build();
            }
        });

        cr.registerAlias("lang", "language");
    }

    @Subscribe
    public void avatar(CommandRegistry cr) {
        cr.register("avatar", new SimpleCommand(CommandCategory.INFO) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                ctx.findMember(content, ctx.getMessage()).onSuccess(members -> {
                    var member = CustomFinderUtil.findMemberDefault(content, members, ctx, ctx.getMember());
                    if (member == null) {
                        return;
                    }

                    var languageContext = ctx.getLanguageContext();
                    var user = member.getUser();
                    var embed = new EmbedBuilder()
                            .setColor(member.getColor() == null ? Color.PINK : member.getColor())
                            .setAuthor(
                                    languageContext.get("commands.avatar.result").formatted(user.getName()),
                                    null, user.getEffectiveAvatarUrl()
                            )
                            .setImage(user.getEffectiveAvatarUrl() + "?size=1024")
                            .setFooter(languageContext.get("commands.avatar.footer"), user.getEffectiveAvatarUrl());

                    ctx.send(embed.build());
                });
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Get a user's avatar URL.")
                        .setUsage("`~>avatar [@user]` - Returns the requested avatar URL")
                        .addParameter("@user",
                                "The user you want to check the avatar URL of. Can be a mention, or name#discrim")
                        .build();
            }
        });
    }

    @Subscribe
    public void guildinfo(CommandRegistry cr) {
        cr.register("serverinfo", new SimpleCommand(CommandCategory.INFO) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                var guild = ctx.getGuild();
                var guildData = ctx.getDBGuild().getData();

                var roles = guild.getRoles().stream()
                        .filter(role -> !guild.getPublicRole().equals(role))
                        .map(Role::getName)
                        .collect(Collectors.joining(", "));

                var owner = guild.getOwner();
                //This is wank lol
                if (owner == null) {
                    owner = guild.retrieveOwner(false).complete();
                }

                var languageContext = ctx.getLanguageContext();
                var str = """
                        **%1$s**
                        %2$s **%3$s:** %4$s
                        %2$s **%5$s:** %6$s
                        %2$s **%7$s:** %8$s
                        %2$s **%9$s:** %10$s
                        %2$s **%11$s:** %12$s
                        """.formatted(languageContext.get("commands.serverinfo.description").formatted(guild.getName()),
                        BLUE_SMALL_MARKER,
                        languageContext.get("commands.serverinfo.users"),
                        guild.getMemberCount(),
                        languageContext.get("commands.serverinfo.channels"),
                        "%,d / %,d".formatted(guild.getVoiceChannels().size(), guild.getTextChannels().size()),
                        languageContext.get("commands.serverinfo.owner"),
                        owner.getUser().getAsTag(),
                        languageContext.get("commands.serverinfo.region"),
                        guild.getRegion() == Region.UNKNOWN ?
                                languageContext.get("general.unknown") :
                                guild.getRegion().getName(),
                        languageContext.get("commands.serverinfo.created"),
                        Utils.formatDate(guild.getTimeCreated(), guildData.getLang())
                );

                ctx.send(new EmbedBuilder()
                        .setAuthor(languageContext.get("commands.serverinfo.header"), null, guild.getIconUrl())
                        .setColor(guild.getOwner().getColor() == null ? Color.PINK: guild.getOwner().getColor())
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

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("See your server's current stats.")
                        .build();
            }
        });

        cr.registerAlias("serverinfo", "guildinfo");
    }

    @Subscribe
    public void invite(CommandRegistry cr) {
        cr.register("invite", new SimpleCommand(CommandCategory.INFO) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                var languageContext = ctx.getLanguageContext();

                ctx.send(new EmbedBuilder()
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
        cr.register("prefix", new SimpleCommand(CommandCategory.INFO) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                var dbGuild = ctx.getDBGuild();

                var defaultPrefix = Stream.of(ctx.getConfig().getPrefix())
                        .map(prefix -> "`" + prefix + "`")
                        .collect(Collectors.joining(" "));

                var guildPrefix = dbGuild.getData().getGuildCustomPrefix();

                ctx.sendLocalized("commands.prefix.header", EmoteReference.HEART,
                        defaultPrefix, guildPrefix == null ?
                                ctx.getLanguageContext().get("commands.prefix.none") : guildPrefix
                );
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription(
                                """
                                Gives you information on how to change the prefix and what's the current prefix.
                                If you looked at help, to change the prefix use `~>opts prefix set <prefix>`
                                """
                        )
                        .build();
            }
        });
    }

    @Subscribe
    public void userinfo(CommandRegistry cr) {
        cr.register("userinfo", new SimpleCommand(CommandCategory.INFO) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                ctx.findMember(content, ctx.getMessage()).onSuccess(members -> {
                    var member = CustomFinderUtil.findMemberDefault(content, members, ctx, ctx.getMember());
                    if (member == null)
                        return;

                    var guildData = ctx.getDBGuild().getData();
                    var user = member.getUser();

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
                            .setColor(member.getColor() == null ? Color.PINK : member.getColor())
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
                });
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("See information about specific users.")
                        .setUsage("`~>userinfo <@user>` - Get information about an user.")
                        .addParameter("user", "The user you want to look for. " +
                                "Mentions, nickname and user#discriminator work.")
                        .build();
            }
        });
    }

    @Subscribe
    public void season(CommandRegistry registry) {
        registry.register("season", new SimpleCommand(CommandCategory.INFO) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                I18nContext languageContext = ctx.getLanguageContext();

                ctx.sendFormat(languageContext.get("commands.season.info") +
                                languageContext.get("commands.season.info_2"),
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
        registry.register("support", new SimpleCommand(CommandCategory.INFO) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                ctx.sendLocalized("commands.support.info", EmoteReference.POPPER);
            }
        });
    }

    @Subscribe
    public void roleinfo(CommandRegistry cr) {
        cr.register("roleinfo", new SimpleCommand(CommandCategory.INFO) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                var role = FinderUtils.findRole(ctx.getEvent(), content);
                if (role == null) {
                    return;
                }

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
                        Utils.formatDate(role.getTimeCreated(), ctx.getDBGuild().getData().getLang()),
                        lang.get("commands.roleinfo.color"),
                        role.getColor() == null ?
                                lang.get("general.none") :
                                "#%s".formatted(Integer.toHexString(role.getColor().getRGB()).substring(2)),
                        lang.get("commands.roleinfo.position"), role.getPosition(),
                        lang.get("commands.roleinfo.hoisted"), role.isHoisted()
                );

                ctx.send(
                        new EmbedBuilder()
                                .setColor(ctx.getMember().getColor())
                                .setAuthor(lang.get("commands.roleinfo.header").formatted(role.getName()),
                                        null, ctx.getGuild().getIconUrl()
                                )
                                .setDescription(str)
                                .addField(lang.get("commands.roleinfo.permissions").formatted(role.getPermissions().size()),
                                        role.getPermissions().size() == 0 ? lang.get("general.none") :
                                                role.getPermissions()
                                                        .stream()
                                                        .map(Permission::getName)
                                                        .collect(Collectors.joining(", ")) + ".",
                                        false
                                ).build()
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
