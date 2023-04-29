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

package net.kodehawa.mantarobot.options;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.kodehawa.mantarobot.data.I18n;
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
                        .setTTS(tts)
                        .queue();
                return;
            }

            if (prefix.equals("/shrug") || prefix.equals("¯\\_(ツ)_/¯")) {
                ctx.send("¯\\_(ツ)_/¯");
                return;
            }

            var dbGuild = ctx.getDBGuild();
            dbGuild.guildCustomPrefix(prefix);
            dbGuild.updateAllChanged();

            ctx.sendLocalized("options.prefix_set.success", EmoteReference.MEGA, prefix);
        });

        registerOption("prefix:clear", "Prefix clear",
                "Clear the server prefix.\n**Example:** `~>opts prefix clear`", (ctx) -> {
                    var dbGuild = ctx.getDBGuild();
                    dbGuild.guildCustomPrefix(null);
                    dbGuild.updateAllChanged();
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

            var dbGuild = ctx.getDBGuild();
            String language = args[0];

            if (!I18n.isValidLanguage(language)) {
                ctx.sendFormat("%s`%s` is not a valid language or it's not yet supported by Mantaro. Check `~>lang` for a list of available languages",
                        EmoteReference.ERROR2, language
                );
                return;
            }

            dbGuild.lang(language);
            dbGuild.updateAllChanged();
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
            var dbGuild = ctx.getDBGuild();
            var lang = ctx.getLanguageContext();

            try {
                dbGuild.customAdminLockNew(Boolean.parseBoolean(action));
                dbGuild.updateAllChanged();
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
            var dbGuild = ctx.getDBGuild();
            if (args.length == 0) {
                ctx.sendLocalized("options.timedisplay_set.no_mode_specified", EmoteReference.ERROR);
                return;
            }

            String mode = args[0];

            switch (mode) {
                case "12h" -> {
                    ctx.sendLocalized("options.timedisplay_set.12h", EmoteReference.CORRECT);
                    dbGuild.timeDisplay(1);
                    dbGuild.updateAllChanged();
                }
                case "24h" -> {
                    ctx.sendLocalized("options.timedisplay_set.24h", EmoteReference.CORRECT);
                    dbGuild.timeDisplay(0);
                    dbGuild.updateAllChanged();
                }
                default -> ctx.sendLocalized("options.timedisplay_set.invalid", EmoteReference.ERROR);
            }
        });

        registerOption("server:ignorebots:joinleave:toggle",
                "Bot join/leave ignore", "Toggles between ignoring bots on join/leave message.", (ctx) -> {
            var dbGuild = ctx.getDBGuild();
            boolean ignore = dbGuild.isIgnoreBotsWelcomeMessage();
            dbGuild.ignoreBotsWelcomeMessage(!ignore);
            dbGuild.updateAllChanged();

            ctx.sendLocalized("options.server_ignorebots_joinleave_toggle.success", EmoteReference.CORRECT, dbGuild.isIgnoreBotsWelcomeMessage());
        });

        registerOption("logs:editmessage", "Edit log message", """
                Sets the edit message.
                **Example:** `~>opts logs editmessage [$(hour)] Message (ID: $(event.message.id)) created by **$(event.user.tag)** in channel **$(event.channel.name)** was modified.
                ```diff
                -$(old)
                +$(new)````
                """, "Sets the edit message.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.logs_editmessage.no_message", EmoteReference.ERROR);
                return;
            }

            var dbGuild = ctx.getDBGuild();
            if (args[0].equals("reset")) {
                dbGuild.editMessageLog(null);
                dbGuild.updateAllChanged();
                ctx.sendLocalized("options.logs_editmessage.reset_success", EmoteReference.CORRECT);
                return;
            }

            String editMessage = ctx.getCustomContent();
            dbGuild.editMessageLog(editMessage);
            dbGuild.updateAllChanged();
            ctx.sendLocalized("options.logs_editmessage.success", EmoteReference.CORRECT, editMessage);
        });
        addOptionAlias("logs:editmessage", "editmessage");

        registerOption("logs:deletemessage", "Delete log message", """
                Sets the delete message.
                **Example:** `~>opts logs deletemessage [$(hour)] Message (ID: $(event.message.id)) created by **$(event.user.tag)** (ID: $(event.user.id)) in channel **$(event.channel.name)** was deleted.```diff
                -$(content)``` `
                """, "Sets the delete message.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.logs_deletemessage.no_message", EmoteReference.ERROR);
                return;
            }

            var dbGuild = ctx.getDBGuild();
            if (args[0].equals("reset")) {
                dbGuild.deleteMessageLog(null);
                dbGuild.updateAllChanged();
                ctx.sendLocalized("options.logs_deletemessage.reset_success", EmoteReference.CORRECT);
                return;
            }

            String deleteMessage = ctx.getCustomContent();
            dbGuild.deleteMessageLog(deleteMessage);
            dbGuild.updateAllChanged();
            ctx.sendLocalized("options.logs_deletemessage.success", EmoteReference.CORRECT, deleteMessage);
        });
        addOptionAlias("logs:deletemessage", "deletemessage");

        registerOption("commands:showdisablewarning", "Show disable warning",
                "Toggles on/off the disabled command warning.",
                "Toggles on/off the disabled command warning.", (ctx, args) -> {
            var dbGuild = ctx.getDBGuild();
            dbGuild.commandWarningDisplay(!dbGuild.isCommandWarningDisplay()); //lombok names are amusing
            dbGuild.updateAllChanged();
            ctx.sendLocalized("options.showdisablewarning.success", EmoteReference.CORRECT, dbGuild.isCommandWarningDisplay());
        });

        registerOption("commands:lobby:disable", "Disables game multiple and lobby.",
                "Disables game multiple and lobby.",
                "Disables game multiple and lobby.", (ctx, args) -> {
            var dbGuild = ctx.getDBGuild();
            dbGuild.gameMultipleDisabled(true);
            dbGuild.updateAllChanged();

            ctx.sendLocalized("options.lobby.disable.success", EmoteReference.CORRECT);
        });

        registerOption("commands:lobby:enable", "Enables game multiple and lobby.",
                "Enables game multiple and lobby.",
                "Enables game multiple and lobby.", (ctx, args) -> {
            var dbGuild = ctx.getDBGuild();
            if (!dbGuild.isGameMultipleDisabled()) {
                ctx.sendLocalized("options.lobby.enable.already_enabled", EmoteReference.CORRECT);
                return;
            }

            dbGuild.gameMultipleDisabled(false);
            dbGuild.updateAllChanged();

            ctx.sendLocalized("options.lobby.enable.success", EmoteReference.CORRECT);
        });

        registerOption("djrole:set", "Set a custom DJ role", """
                Sets a custom DJ role. This role will be used to control music.
                **Example:** `~>opts djrole set DJ`, `~>opts djrole set "Magic Role"`
                """, "Sets the DJ role.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.djrole_set.no_role", EmoteReference.ERROR);
                return;
            }

            Consumer<Role> consumer = (role) -> {
                var dbGuild = ctx.getDBGuild();
                dbGuild.djRoleId(role.getId());
                dbGuild.updateAllChanged();
                ctx.sendLocalized("options.djrole_set.success", EmoteReference.CORRECT, role.getName(), role.getPosition());
            };

            Role role = FinderUtils.findRoleSelect(ctx, ctx.getCustomContent(), consumer);

            if (role != null) {
                consumer.accept(role);
            }
        });

        registerOption("djrole:reset", "Resets the DJ role",
                "Resets the DJ role", "Resets the DJ role.", (ctx, args) -> {
            var dbGuild = ctx.getDBGuild();
            dbGuild.djRoleId(null);
            dbGuild.updateAllChanged();
            ctx.sendLocalized("options.djrole_reset.success", EmoteReference.CORRECT);
        });

        registerOption("imageboard:disableexplicit", "Disables explicit searches",
            "Disables explicit/questionable searches, regardless of the channel type.",
            "Disables explicit searches.", (ctx, args) -> {
                var dbGuild = ctx.getDBGuild();
                dbGuild.disableExplicit(true);
                dbGuild.updateAllChanged();
                ctx.sendLocalized("options.imageboard_disableexplicit.success", EmoteReference.CORRECT);
        });

        registerOption("imageboard:enableexplicit", "Re-enables explicit searches",
            "Re-enables explicit/questionable searches",
            "Re-enables explicit searches.", (ctx, args) -> {
                var dbGuild = ctx.getDBGuild();
                if (!dbGuild.isDisableExplicit()) {
                    ctx.sendLocalized("options.imageboard_enableexplicit.already_enabled", EmoteReference.ERROR);
                    return;
                }
                dbGuild.disableExplicit(false);
                dbGuild.updateAllChanged();
                ctx.sendLocalized("options.imageboard_enableexplicit.success", EmoteReference.CORRECT);
        });
    }

    @Override
    public String description() {
        return "Guild Configuration";
    }
}
