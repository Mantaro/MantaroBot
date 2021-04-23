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
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.options.annotations.Option;
import net.kodehawa.mantarobot.options.core.OptionHandler;
import net.kodehawa.mantarobot.options.core.OptionType;
import net.kodehawa.mantarobot.options.event.OptionRegistryEvent;
import net.kodehawa.mantarobot.utils.commands.CustomFinderUtil;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.FinderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Option
public class BirthdayOptions extends OptionHandler {
    private final Logger log = LoggerFactory.getLogger(BirthdayOptions.class);

    public BirthdayOptions() {
        setType(OptionType.GUILD);
    }

    @Subscribe
    public void onRegistry(OptionRegistryEvent event) {
        registerOption("birthday:test", "Tests if the birthday assigner works.",
                "Tests if the birthday assigner works properly. You need to input an user mention/id/tag to test it with.",
                "Tests if the birthday assigner works.", (ctx, args) -> {
            if (args.length < 1) {
                ctx.sendLocalized("options.birthday_test.no_user", EmoteReference.ERROR2);
                return;
            }

            String query = String.join(" ", args);
            ctx.findMember(query, members -> {
                final var m = CustomFinderUtil.findMemberDefault(query, members, ctx, ctx.getMember());
                if (m == null) {
                    return;
                }

                final var dbGuild = ctx.getDBGuild();
                final var guildData = dbGuild.getData();
                final var guild = ctx.getGuild();

                var birthdayChannel = guildData.getBirthdayChannel() == null ? null : guild.getTextChannelById(guildData.getBirthdayChannel());
                var birthdayRole = guildData.getBirthdayRole() == null ? null : guild.getRoleById(guildData.getBirthdayRole());

                if (birthdayChannel == null) {
                    ctx.sendLocalized("options.birthday_test.no_bd_channel", EmoteReference.ERROR);
                    return;
                }

                if (birthdayRole == null) {
                    ctx.sendLocalized("options.birthday_test.no_bd_role", EmoteReference.ERROR);
                    return;
                }

                if (!birthdayChannel.canTalk()) {
                    ctx.sendLocalized("options.birthday_test.no_talk_permission", EmoteReference.ERROR);
                    return;
                }

                if (!ctx.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
                    ctx.sendLocalized("options.birthday_test.no_role_permission", EmoteReference.ERROR);
                    return;
                }

                if (!ctx.getSelfMember().canInteract(birthdayRole)) {
                    ctx.sendLocalized("options.birthday_test.cannot_interact", EmoteReference.ERROR);
                    return;
                }

                try {
                    User user = m.getUser();
                    String message = String.format("%s**%s is a year older now! Wish them a happy birthday.** :tada: (test)", EmoteReference.POPPER, m.getEffectiveName());
                    if (dbGuild.getData().getBirthdayMessage() != null) {
                        message = dbGuild.getData().getBirthdayMessage().replace("$(user)", m.getEffectiveName()).replace("$(usermention)", m.getAsMention());
                    }

                    //Value used in lambda... blabla :c
                    final String finalMessage = message;
                    guild.addRoleToMember(m, birthdayRole).queue(
                            success -> birthdayChannel.sendMessage(finalMessage).queue(
                                    s -> ctx.sendLocalized("options.birthday_test.success",
                                            EmoteReference.CORRECT, birthdayChannel.getName(), user.getName(), birthdayRole.getName()
                                    ), error -> ctx.sendLocalized("options.birthday_test.error",
                                            EmoteReference.CORRECT, birthdayChannel.getName(), user.getName(), birthdayRole.getName()
                                    )
                            ), error -> ctx.sendLocalized("options.birthday_test.error",
                                    EmoteReference.CORRECT, birthdayChannel.getName(), user.getName(), birthdayRole.getName()
                            ));
                } catch (Exception e) {
                    log.error("Error sending birthday test message!", e);
                }
            });
        });

        registerOption("birthday:enable", "Birthday Monitoring enable", """
                Enables birthday monitoring. You need the channel **name** and the role name (it assigns that role on birthday)
                **Example:** `~>opts birthday enable general Birthday`, `~>opts birthday enable general \"Happy Birthday\"`
                """, "Enables birthday monitoring.", (ctx, args) -> {
            if (args.length < 2) {
                ctx.sendLocalized("options.birthday_enable.no_args", EmoteReference.ERROR);
                return;
            }

            var lang = ctx.getLanguageContext();
            var dbGuild = ctx.getDBGuild();
            var guildData = dbGuild.getData();

            try {
                var channel = args[0];
                var role = args[1];

                var channelObj = FinderUtils.findChannel(ctx.getEvent(), channel);
                if (channelObj == null)
                    return;

                var channelId = channelObj.getId();
                var roleObj = FinderUtils.findRole(ctx.getEvent(), role);
                if (roleObj == null)
                    return;

                if (roleObj.isPublicRole()) {
                    ctx.sendLocalized("options.birthday_enable.public_role", EmoteReference.ERROR);
                    return;
                }

                if (guildData.getGuildAutoRole() != null && roleObj.getId().equals(guildData.getGuildAutoRole())) {
                    ctx.sendLocalized("options.birthday_enable.autorole", EmoteReference.ERROR);
                    return;
                }

                ctx.sendFormat(String.join("\n", lang.get("options.birthday_enable.warning"),
                        lang.get("options.birthday_enable.warning_1"),
                        lang.get("options.birthday_enable.warning_2"),
                        lang.get("options.birthday_enable.warning_3"),
                        lang.get("options.birthday_enable.warning_4")),
                        EmoteReference.WARNING, roleObj.getName()
                );

                InteractiveOperations.create(ctx.getChannel(), ctx.getAuthor().getIdLong(), 45, interactiveEvent -> {
                    String content = interactiveEvent.getMessage().getContentRaw();
                    if (content.equalsIgnoreCase("yes")) {
                        String roleId = roleObj.getId();
                        guildData.setBirthdayChannel(channelId);
                        guildData.setBirthdayRole(roleId);
                        dbGuild.saveUpdating();
                        ctx.sendLocalized("options.birthday_enable.success", EmoteReference.MEGA, channelObj.getName(), channelId, role, roleId);
                        return Operation.COMPLETED;
                    } else if (content.equalsIgnoreCase("no")) {
                        ctx.sendLocalized("general.cancelled", EmoteReference.CORRECT);
                        return Operation.COMPLETED;
                    }

                    return Operation.IGNORED;
                });

            } catch (IndexOutOfBoundsException ex1) {
                ctx.sendFormat(lang.get("options.birthday_enable.error_channel_1") + "\n" + lang.get("options.birthday_enable.error_channel_2"),
                        EmoteReference.ERROR
                );
            } catch (Exception ex) {
                ctx.send(lang.get("general.invalid_syntax") + "\nCheck https://github.com/Mantaro/MantaroBot/wiki/Birthday-101 for more information.");
            }
        });

        registerOption("birthday:disable", "Birthday disable", "Disables birthday monitoring.", (ctx) -> {
            var dbGuild = ctx.getDBGuild();
            var guildData = dbGuild.getData();
            guildData.setBirthdayChannel(null);

            guildData.setBirthdayRole(null);
            dbGuild.saveUpdating();
            ctx.sendLocalized("options.birthday_disable.success", EmoteReference.MEGA);
        });

        registerOption("birthday:message:set", "Birthday message", "Sets the message to display on a new birthday",
                "Sets the birthday message", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.birthday_message_set.no_message", EmoteReference.ERROR);
                return;
            }

            var dbGuild = ctx.getDBGuild();
            var guildData = dbGuild.getData();

            // Trim start/end whitespace and/or newlines.
            String birthdayMessage = String.join(" ", args).trim();
            guildData.setBirthdayMessage(birthdayMessage);
            dbGuild.saveUpdating();
            ctx.sendLocalized("options.birthday_message_set.success", EmoteReference.CORRECT, birthdayMessage);
        });

        registerOption("birthday:message:clear", "Birthday message clear", "Clears the message to display on a new birthday",
                "Clears the message to display on birthday", (ctx, args) -> {
            var dbGuild = ctx.getDBGuild();
            var guildData = dbGuild.getData();

            guildData.setBirthdayMessage(null);
            dbGuild.saveUpdating();

            ctx.sendLocalized("options.birthday_message_clear.success", EmoteReference.CORRECT);
        });

        registerOption("commands:birthdayblacklist:add", "Add someone to the birthday blacklist",
                "Adds a person to the birthday blacklist",
                "Add someone to the birthday blacklist", (ctx, args) -> {
            var dbGuild = ctx.getDBGuild();
            var guildData = dbGuild.getData();
            if (args.length == 0) {
                ctx.sendLocalized("options.birthdayblacklist.no_args", EmoteReference.ERROR);
                return;
            }

            String content = String.join(" ", args);
            ctx.findMember(content, members -> {
                Member member = CustomFinderUtil.findMemberDefault(content, members, ctx, ctx.getMember());
                if (member == null)
                    return;

                guildData.getBirthdayBlockedIds().add(member.getId());
                ctx.sendLocalized("options.birthdayblacklist.add.success", EmoteReference.CORRECT, member.getEffectiveName(), member.getId());
            });
        });

        registerOption("commands:birthdayblacklist:remove", "Removes someone to the birthday blacklist",
                "Removes a person from the birthday blacklist",
                "Remove someone to the birthday blacklist", (ctx, args) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            if (args.length == 0) {
                ctx.sendLocalized("options.birthdayblacklist.no_args", EmoteReference.ERROR);
                return;
            }

            String content = String.join(" ", args);
            ctx.findMember(content, members -> {
                Member member = CustomFinderUtil.findMemberDefault(content, members, ctx, ctx.getMember());
                if (member == null)
                    return;

                guildData.getBirthdayBlockedIds().remove(member.getId());
                ctx.sendLocalized("options.birthdayblacklist.remove.success", EmoteReference.CORRECT, member.getEffectiveName(), member.getId());
            });
        });
    }

    @Override
    public String description() {
        return "Guild birthday announcer options";
    }
}
