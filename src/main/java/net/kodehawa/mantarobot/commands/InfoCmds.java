/*
 * Copyright (C) 2016-2021 David Rubio Escares / Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.*;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.data.I18n;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.utils.commands.EmoteReference.BLUE_SMALL_MARKER;

@Module
public class InfoCmds {
    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(Information.class);
        cr.registerSlash(Avatar.class);
        cr.registerSlash(UserInfo.class);
        cr.registerSlash(ServerInfo.class);
        cr.registerSlash(RoleInfo.class);
    }

    @Name("information")
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
    }

    @Name("avatar")
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
            var user = ctx.getOptionAsUser("user");
            if (user == null) {
                user = ctx.getAuthor();
            }

            var languageContext = ctx.getLanguageContext();
            var member = ctx.getGuild().getMember(user);

            var embed = new EmbedBuilder()
                    .setColor(ctx.getMemberColor(member))
                    .setAuthor(
                            languageContext.get("commands.avatar.result").formatted(user.getName()),
                            null, member.getEffectiveAvatarUrl()
                    )
                    .setImage(member.getEffectiveAvatarUrl() + "?size=1024")
                    .setFooter(languageContext.get("commands.avatar.footer"), member.getEffectiveAvatarUrl());

            ctx.reply(embed.build());
        }
    }

    @Name("userinfo")
    @Description("See information about specific users.")
    @Category(CommandCategory.INFO)
    @Help(description = "See information about specific users.", usage = "`/userinfo [user]`", parameters = {
            @Help.Parameter(name = "user", description = "The user you want to look.", optional = true)
    })
    @Options({
            @Options.Option(type = OptionType.USER, name = "user", description = "The user to see the information of.")
    })
    public static class UserInfo extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var user = ctx.getOptionAsUser("user");
            if (user == null)
                user = ctx.getAuthor();

            var guildData = ctx.getDBGuild().getData();
            var member = ctx.getGuild().getMember(user);

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

            ctx.reply(new EmbedBuilder()
                    .setColor(ctx.getMemberColor())
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

    @Name("serverinfo")
    @Description("See your server's current stats.")
    @Category(CommandCategory.INFO)
    @Help(description = "See your server's current stats.")
    public static class ServerInfo extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var guild = ctx.getGuild();
            var guildData = ctx.getDBGuild().getData();

            var roles = guild.getRoles().stream()
                    .filter(role -> !guild.getPublicRole().equals(role))
                    .map(Role::getName)
                    .collect(Collectors.joining(", "));

            // Retrieves from cache if we have it.
            var owner = guild.retrieveOwner(false).complete();
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

    @Name("roleinfo")
    @Description("See information about a role.")
    @Category(CommandCategory.INFO)
    @Help(description = "See information about a role.", usage = "`/roleinfo <role>`", parameters = {
            @Help.Parameter(name = "role", description = "The role you want to see the information of.")
    })
    @Options({
            @Options.Option(type = OptionType.ROLE, name = "role", description = "The role to see the information of.")
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
                    Utils.formatDate(role.getTimeCreated(), ctx.getDBGuild().getData().getLang()),
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
                            role.getPermissions().size() == 0 ? lang.get("general.none") :
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
