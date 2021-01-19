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

package net.kodehawa.mantarobot.options;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.kodehawa.mantarobot.data.I18n;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.options.annotations.Option;
import net.kodehawa.mantarobot.options.core.OptionHandler;
import net.kodehawa.mantarobot.options.core.OptionType;
import net.kodehawa.mantarobot.options.event.OptionRegistryEvent;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.FinderUtils;

import java.util.function.Consumer;

@Option
public class GuildOptions extends OptionHandler {
    public GuildOptions() {
        setType(OptionType.GUILD);
    }

    @Subscribe
    public void onRegistry(OptionRegistryEvent e) {
        registerOption("prefix:set", "Prefix set", """
                Sets the server prefix. 
                **Example:** `~>opts prefix set .` 
                """, "Sets the server prefix.", (ctx, args) -> {
            if (args.length < 1) {
                ctx.sendLocalized("options.prefix_set.no_prefix", EmoteReference.ERROR);
                return;
            }

            String prefix = args[0];

            if (prefix.length() > 50) {
                ctx.sendLocalized("options.prefix_set.too_long", EmoteReference.ERROR);
                return;
            }

            if (prefix.isEmpty()) {
                ctx.sendLocalized("options.prefix_set.empty_prefix", EmoteReference.ERROR);
                return;
            }

            if (prefix.equals("/tts")) {
                var tts = ctx.getSelfMember().hasPermission(ctx.getChannel(), Permission.MESSAGE_TTS);
                ctx.getChannel().sendMessage("wwwwwwwwwwwwwwwwwwwwwwwwwwwwwww")
                        .tts(tts)
                        .queue();
                return;
            }

            if (prefix.equals("/shrug") || prefix.equals("¯\\_(ツ)_/¯")) {
                ctx.send("¯\\_(ツ)_/¯");
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            guildData.setGuildCustomPrefix(prefix);
            dbGuild.save();

            ctx.sendLocalized("options.prefix_set.success", EmoteReference.MEGA, prefix);
        });

        registerOption("prefix:clear", "Prefix clear",
                "Clear the server prefix.\n**Example:** `~>opts prefix clear`", (ctx) -> {
                    DBGuild dbGuild = ctx.getDBGuild();
                    GuildData guildData = dbGuild.getData();
                    guildData.setGuildCustomPrefix(null);
                    dbGuild.save();
                    ctx.sendLocalized("options.prefix_clear.success", EmoteReference.MEGA);
        });
        addOptionAlias("prefix:clear", "prefix:reset");

        registerOption("language:set", "Sets the language of this guild", """
                Sets the language of this guild. Languages use a language code (example en_US or de_DE).
                **Example:** `~>opts language set de_DE` for German. See `~>lang` for a full list.
                """, "Sets the language of this guild", ((ctx, args) -> {
            if (args.length < 1) {
                ctx.sendFormat("%1$sYou need to specify the display language that you want the bot to use on this server. (To see avaliable lang codes, use `~>lang`)",
                        EmoteReference.ERROR
                );

                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            String language = args[0];

            if (!I18n.isValidLanguage(language)) {
                ctx.sendFormat("%s`%s` is not a valid language or it's not yet supported by Mantaro. Check `~>lang` for a list of available languages",
                        EmoteReference.ERROR2, language
                );
                return;
            }

            guildData.setLang(language);
            dbGuild.saveUpdating();
            ctx.sendFormat("%sSuccessfully set the language of this server to `%s`", EmoteReference.CORRECT, language);
        }));

        registerOption("admincustom", "Admin custom commands", """
                Locks or unlocks custom commands to/from admin-only. Default is admin-locked.
                Example: `~>opts admincustom true`
                """, "Locks custom commands to admin-only or unlocks them. Default is locked.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.admincustom.no_args", EmoteReference.ERROR);
                return;
            }

            String action = args[0];
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            var lang = ctx.getLanguageContext();

            try {
                guildData.setCustomAdminLockNew(Boolean.parseBoolean(action));
                dbGuild.save();
                String toSend = String.format("%s%s", EmoteReference.CORRECT,
                        Boolean.parseBoolean(action) ? lang.get("options.admincustom.admin_only") : lang.get("options.admincustom.everyone")
                );

                ctx.send(toSend);
            } catch (Exception ex) {
                ctx.sendLocalized("options.admincustom.not_bool", EmoteReference.ERROR);
            }
        });

        registerOption("timedisplay:set", "Time display set", """
                Toggles between 12h and 24h time display.
                Example: `~>opts timedisplay 24h`
                """, "Toggles between 12h and 24h time display.", (ctx, args) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            if (args.length == 0) {
                ctx.sendLocalized("options.timedisplay_set.no_mode_specified", EmoteReference.ERROR);
                return;
            }

            String mode = args[0];

            switch (mode) {
                case "12h" -> {
                    ctx.sendLocalized("options.timedisplay_set.12h", EmoteReference.CORRECT);
                    guildData.setTimeDisplay(1);
                    dbGuild.save();
                }
                case "24h" -> {
                    ctx.sendLocalized("options.timedisplay_set.24h", EmoteReference.CORRECT);
                    guildData.setTimeDisplay(0);
                    dbGuild.save();
                }
                default -> ctx.sendLocalized("options.timedisplay_set.invalid", EmoteReference.ERROR);
            }
        });

        registerOption("server:ignorebots:joinleave:toggle",
                "Bot join/leave ignore", "Toggles between ignoring bots on join/leave message.", (ctx) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            boolean ignore = guildData.isIgnoreBotsWelcomeMessage();
            guildData.setIgnoreBotsWelcomeMessage(!ignore);
            dbGuild.saveAsync();

            ctx.sendLocalized("options.server_ignorebots_joinleave_toggle.success", EmoteReference.CORRECT, guildData.isIgnoreBotsWelcomeMessage());
        });

        registerOption("logs:editmessage", "Edit log message", """
                Sets the edit message.
                **Example:** `~>opts logs editmessage [$(hour)] Message (ID: $(event.message.id)) created by **$(event.user.tag)** in channel **$(event.channel.name)** was modified.\n```diff\n-$(old)\n+$(new)````
                """, "Sets the edit message.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.logs_editmessage.no_message", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            if (args[0].equals("reset")) {
                guildData.setEditMessageLog(null);
                dbGuild.save();
                ctx.sendLocalized("options.logs_editmessage.reset_success", EmoteReference.CORRECT);
                return;
            }

            String editMessage = String.join(" ", args);
            guildData.setEditMessageLog(editMessage);
            dbGuild.save();
            ctx.sendLocalized("options.logs_editmessage.success", EmoteReference.CORRECT, editMessage);
        });
        addOptionAlias("logs:editmessage", "editmessage");

        registerOption("logs:deletemessage", "Delete log message", """
                Sets the delete message.
                **Example:** `~>opts logs deletemessage [$(hour)] Message (ID: $(event.message.id)) created by **$(event.user.tag)** (ID: $(event.user.id)) in channel **$(event.channel.name)** was deleted.```diff\n-$(content)``` `
                """, "Sets the delete message.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.logs_deletemessage.no_message", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            if (args[0].equals("reset")) {
                guildData.setDeleteMessageLog(null);
                dbGuild.save();
                ctx.sendLocalized("options.logs_deletemessage.reset_success", EmoteReference.CORRECT);
                return;
            }

            String deleteMessage = String.join(" ", args);
            guildData.setDeleteMessageLog(deleteMessage);
            dbGuild.save();
            ctx.sendLocalized("options.logs_deletemessage.success", EmoteReference.CORRECT, deleteMessage);
        });
        addOptionAlias("logs:deletemessage", "deletemessage");

        registerOption("logs:banmessage", "Ban log message",
                "Sets the ban message.\n" +
                        "**Example:** `~>opts logs banmessage [$(hour)] $(event.user.tag) just got banned.`",
                "Sets the ban message.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.logs_banmessage.no_message", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            if (args[0].equals("reset")) {
                guildData.setBannedMemberLog(null);
                dbGuild.save();
                ctx.sendLocalized("options.logs_banmessage.reset_success", EmoteReference.CORRECT);
                return;
            }

            String banMessage = String.join(" ", args);
            guildData.setBannedMemberLog(banMessage);
            dbGuild.save();
            ctx.sendLocalized("options.logs_banmessage.success", EmoteReference.CORRECT, banMessage);
        });
        addOptionAlias("logs:banmessage", "banmessage");

        registerOption("logs:unbanmessage", "Unban log message",
                "Sets the unban message.\n" +
                        "**Example:** `~>opts logs unbanmessage [$(hour)] $(event.user.tag) just got unbanned.`",
                "Sets the unban message.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.logs_unbanmessage.no_message", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            if (args[0].equals("reset")) {
                guildData.setUnbannedMemberLog(null);
                dbGuild.save();
                ctx.sendLocalized("options.logs_unbanmessage.reset_success", EmoteReference.CORRECT);
                return;
            }

            String unbanMessage = String.join(" ", args);
            guildData.setUnbannedMemberLog(unbanMessage);
            dbGuild.save();
            ctx.sendLocalized("options.logs_unbanmessage.success", EmoteReference.CORRECT, unbanMessage);
        });
        addOptionAlias("logs:unbanmessage", "unbanmessage");

        registerOption("commands:showdisablewarning", "Show disable warning",
                "Toggles on/off the disabled command warning.",
                "Toggles on/off the disabled command warning.", (ctx, args) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            guildData.setCommandWarningDisplay(!guildData.isCommandWarningDisplay()); //lombok names are amusing
            dbGuild.save();
            ctx.sendLocalized("options.showdisablewarning.success", EmoteReference.CORRECT, guildData.isCommandWarningDisplay());
        });

        registerOption("commands:lobby:disable", "Disables game multiple and lobby.",
                "Disables game multiple and lobby.",
                "Disables game multiple and lobby.", (ctx, args) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            guildData.setGameMultipleDisabled(true);
            dbGuild.save();

            ctx.sendLocalized("options.lobby.disable.success", EmoteReference.CORRECT);
        });

        registerOption("commands:lobby:enable", "Enables game multiple and lobby.",
                "Enables game multiple and lobby.",
                "Enables game multiple and lobby.", (ctx, args) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            if (!guildData.isGameMultipleDisabled()) {
                ctx.sendLocalized("options.lobby.enable.already_enabled", EmoteReference.CORRECT);
                return;
            }

            guildData.setGameMultipleDisabled(false);
            dbGuild.save();

            ctx.sendLocalized("options.lobby.enable.success", EmoteReference.CORRECT);
        });

        registerOption("djrole:set", "Set a custom DJ role", """
                Sets a custom DJ role. This role will be used to control music.
                **Example:** `~>opts djrole set DJ`, `~>opts djrole set \"Magic Role\"`
                """, "Sets the DJ role.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.djrole_set.no_role", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            Consumer<Role> consumer = (role) -> {
                guildData.setDjRoleId(role.getId());
                dbGuild.saveAsync();
                ctx.sendLocalized("options.djrole_set.success", EmoteReference.CORRECT, role.getName(), role.getPosition());
            };

            Role role = FinderUtils.findRoleSelect(ctx.getEvent(), String.join(" ", args), consumer);

            if (role != null) {
                consumer.accept(role);
            }
        });

        registerOption("djrole:reset", "Resets the DJ role",
                "Resets the DJ role", "Resets the DJ role.", (ctx, args) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            guildData.setDjRoleId(null);
            dbGuild.saveAsync();
            ctx.sendLocalized("options.djrole_reset.success", EmoteReference.CORRECT);
        });

    }

    @Override
    public String description() {
        return "Guild Configuration";
    }
}
