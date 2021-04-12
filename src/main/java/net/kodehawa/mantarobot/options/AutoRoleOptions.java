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
import net.dv8tion.jda.api.entities.Role;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.options.annotations.Option;
import net.kodehawa.mantarobot.options.core.OptionHandler;
import net.kodehawa.mantarobot.options.core.OptionType;
import net.kodehawa.mantarobot.options.event.OptionRegistryEvent;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.FinderUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Option
public class AutoRoleOptions extends OptionHandler {
    public AutoRoleOptions() {
        setType(OptionType.GUILD);
    }

    @Subscribe
    public void onRegistry(OptionRegistryEvent event) {
        registerOption("autorole:set", "Autorole set", """
                Sets the server autorole. This means every user who joins will get this role.
                **You need to use the role name, if it contains spaces you need to wrap it in quotation marks**
                Example:** `~>opts autorole set Member`, `~>opts autorole set \"Magic Role\"`
                """, "Sets the server autorole.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.autorole_set.no_role", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            Consumer<Role> consumer = (role) -> {
                if (!ctx.getMember().canInteract(role)) {
                    ctx.sendLocalized("options.autorole_set.hierarchy_conflict", EmoteReference.ERROR);
                    return;
                }

                if (!ctx.getSelfMember().canInteract(role)) {
                    ctx.sendLocalized("options.autorole_set.self_hierarchy_conflict", EmoteReference.ERROR);
                    return;
                }

                guildData.setGuildAutoRole(role.getId());
                dbGuild.saveAsync();
                ctx.sendLocalized("options.autorole_set.success", EmoteReference.CORRECT, role.getName(), role.getPosition());
            };

            Role role = FinderUtils.findRoleSelect(ctx.getEvent(), String.join(" ", args), consumer);

            if (role != null) {
                consumer.accept(role);
            }
        });

        registerOption("autorole:unbind", "Autorole clear", """
                Clear the server autorole.
                **Example:** `~>opts autorole unbind`
                """, "Resets the servers autorole.", (ctx, args) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            guildData.setGuildAutoRole(null);
            dbGuild.saveAsync();
            ctx.sendLocalized("options.autorole_unbind.success", EmoteReference.OK);
        });

        registerOption("autoroles:add", "Autoroles add", """
                Adds a role to the `~>iam` list.
                You need the name of the iam and the name of the role. If the role contains spaces wrap it in quotation marks.
                **Example:** `~>opts autoroles add member Member`, `~>opts autoroles add wew \"A role with spaces on its name\"`
                """, "Adds an auto-assignable role to the iam lists.", (ctx, args) -> {
            if (args.length < 2) {
                ctx.sendLocalized("options.autoroles_add.no_args", EmoteReference.ERROR);
                return;
            }

            String roleName = args[1];
            final var iamName = args[0];
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            if (iamName.length() > 40) {
                ctx.sendLocalized("options.autoroles_add.too_long", EmoteReference.ERROR);
                return;
            }

            Consumer<Role> roleConsumer = role -> {
                if (!ctx.getMember().canInteract(role)) {
                    ctx.sendLocalized("options.autoroles_add.hierarchy_conflict", EmoteReference.ERROR);
                    return;
                }

                if (!ctx.getSelfMember().canInteract(role)) {
                    ctx.sendLocalized("options.autoroles_add.self_hierarchy_conflict", EmoteReference.ERROR);
                    return;
                }

                guildData.getAutoroles().put(iamName, role.getId());
                dbGuild.saveAsync();
                ctx.sendLocalized("options.autoroles_add.success", EmoteReference.OK, iamName, role.getName());
            };

            Role role = FinderUtils.findRoleSelect(ctx.getEvent(), roleName, roleConsumer);
            if (role != null) {
                roleConsumer.accept(role);
            }
        });

        registerOption("autoroles:remove", "Autoroles remove", """
                Removes a role from the `~>iam` list. You need the name of the iam.
                **Example:** `~>opts autoroles remove iamname`
                """, "Removes an auto-assignable role from iam.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.autoroles_add.no_args", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            HashMap<String, String> autoroles = guildData.getAutoroles();
            if (autoroles.containsKey(args[0])) {
                autoroles.remove(args[0]);
                dbGuild.saveAsync();
                ctx.sendLocalized("options.autoroles_remove.success", EmoteReference.OK, args[0]);
            } else {
                ctx.sendLocalized("options.autoroles_remove.not_found", EmoteReference.ERROR);
            }
        });

        registerOption("autoroles:clear", "Autoroles clear",
                "Removes all autoroles.",
        "Removes all autoroles.", (ctx, args) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            dbGuild.getData().getAutoroles().clear();
            dbGuild.saveAsync();
            ctx.sendLocalized("options.autoroles_clear.success", EmoteReference.CORRECT);
        });

        registerOption("autoroles:category:add", "Adds a category to autoroles",
                "Adds a category to autoroles. Useful for organizing",
                "Adds a category to autoroles.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.autoroles_category_add.no_args", EmoteReference.ERROR);
                return;
            }

            String category = args[0];
            String autorole = null;
            if (args.length > 1) {
                autorole = args[1];
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            Map<String, List<String>> categories = guildData.getAutoroleCategories();
            if (categories.containsKey(category) && autorole == null) {
                ctx.sendLocalized("options.autoroles_category_add.already_exists", EmoteReference.ERROR);
                return;
            }

            categories.computeIfAbsent(category, (a) -> new ArrayList<>());

            if (autorole != null) {
                if (guildData.getAutoroles().containsKey(autorole)) {
                    categories.get(category).add(autorole);
                    dbGuild.save();
                    ctx.sendLocalized("options.autoroles_category_add.success", EmoteReference.CORRECT, autorole, category);
                } else {
                    ctx.sendLocalized("options.autoroles_category_add.no_role", EmoteReference.ERROR, autorole);
                }
                return;
            }

            dbGuild.save();
            ctx.sendLocalized("options.autoroles_category_add.success_new", EmoteReference.CORRECT, category);
        });

        registerOption("autoroles:category:remove", "Removes a category from autoroles",
                "Removes a category from autoroles. Useful for organizing",
                "Removes a category from autoroles.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.autoroles_category_remove.no_args", EmoteReference.ERROR);
                return;
            }

            String category = args[0];
            String autorole = null;
            if (args.length > 1) {
                autorole = args[1];
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            Map<String, List<String>> categories = guildData.getAutoroleCategories();
            if (!categories.containsKey(category)) {
                ctx.sendLocalized("options.autoroles_category_add.no_category", EmoteReference.ERROR, category);
                return;
            }

            if (autorole != null) {
                categories.get(category).remove(autorole);
                dbGuild.save();
                ctx.sendLocalized("options.autoroles_category_remove.success", EmoteReference.CORRECT, category, autorole);
                return;
            }

            categories.remove(category);
            dbGuild.save();
            ctx.sendLocalized("options.autoroles_category_remove.success_new", EmoteReference.CORRECT, category);
        });

        registerOption("server:ignorebots:autoroles:toggle",
                "Bot autorole ignore", "Toggles between ignoring bots on autorole assign and not.", (ctx) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            boolean ignore = guildData.isIgnoreBotsAutoRole();
            guildData.setIgnoreBotsAutoRole(!ignore);
            dbGuild.saveAsync();

            ctx.sendLocalized("options.server_ignorebots_autoroles_toggle.success", EmoteReference.CORRECT, guildData.isIgnoreBotsAutoRole());
        });
    }

    @Override
    public String description() {
        return "Guild auto role and self-assigned roles configuration";
    }

}
