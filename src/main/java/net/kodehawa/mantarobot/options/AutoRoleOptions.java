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
import net.dv8tion.jda.api.entities.Role;
import net.kodehawa.mantarobot.options.annotations.Option;
import net.kodehawa.mantarobot.options.core.OptionHandler;
import net.kodehawa.mantarobot.options.core.OptionType;
import net.kodehawa.mantarobot.options.event.OptionRegistryEvent;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.FinderUtils;

import java.util.ArrayList;
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
                Example:** `~>opts autorole set Member`, `~>opts autorole set "Magic Role"`
                """, "Sets the server autorole.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.autorole_set.no_role", EmoteReference.ERROR);
                return;
            }

            Consumer<Role> consumer = (role) -> {
                var dbGuild = ctx.getDBGuild();
                if (!ctx.getMember().canInteract(role)) {
                    ctx.sendLocalized("options.autorole_set.hierarchy_conflict", EmoteReference.ERROR);
                    return;
                }

                if (!ctx.getSelfMember().canInteract(role)) {
                    ctx.sendLocalized("options.autorole_set.self_hierarchy_conflict", EmoteReference.ERROR);
                    return;
                }

                if(Utils.isRoleAdministrative(role)) {
                    ctx.sendLocalized("options.autorole_set.permissions_conflict", EmoteReference.ERROR);
                    return;
                }

                dbGuild.guildAutoRole(role.getId());
                dbGuild.updateAllChanged();
                ctx.sendLocalized("options.autorole_set.success", EmoteReference.CORRECT, role.getName(), role.getPosition());
            };

            Role role = FinderUtils.findRoleSelect(ctx, ctx.getCustomContent(), consumer);

            if (role != null) {
                consumer.accept(role);
            }
        });

        registerOption("autorole:unbind", "Autorole clear", """
                Clear the server autorole.
                **Example:** `~>opts autorole unbind`
                """, "Resets the servers autorole.", (ctx, args) -> {
            var dbGuild = ctx.getDBGuild();
            dbGuild.guildAutoRole(null);
            dbGuild.updateAllChanged();
            ctx.sendLocalized("options.autorole_unbind.success", EmoteReference.OK);
        });

        registerOption("autoroles:add", "Autoroles add", """
                Adds a role to the `~>iam` list.
                You need the name of the iam and the name of the role. If the role contains spaces wrap it in quotation marks.
                **Example:** `~>opts autoroles add member Member`, `~>opts autoroles add wew "A role with spaces on its name"`
                """, "Adds an auto-assignable role to the iam lists.", (ctx, args) -> {
            if (args.length < 2) {
                ctx.sendLocalized("options.autoroles_add.no_args", EmoteReference.ERROR);
                return;
            }

            String roleName = args[1];
            final var iamName = args[0];
            if (iamName.length() > 40) {
                ctx.sendLocalized("options.autoroles_add.too_long", EmoteReference.ERROR);
                return;
            }

            Consumer<Role> roleConsumer = role -> {
                var dbGuild = ctx.getDBGuild();
                if (!ctx.getMember().canInteract(role)) {
                    ctx.sendLocalized("options.autoroles_add.hierarchy_conflict", EmoteReference.ERROR);
                    return;
                }

                if (!ctx.getSelfMember().canInteract(role)) {
                    ctx.sendLocalized("options.autoroles_add.self_hierarchy_conflict", EmoteReference.ERROR);
                    return;
                }

                if (Utils.isRoleAdministrative(role)) {
                    ctx.sendLocalized("options.autoroles_add.permissions_conflict", EmoteReference.ERROR);
                    return;
                }

                dbGuild.addAutorole(iamName, role.getId());
                dbGuild.updateAllChanged();
                ctx.sendLocalized("options.autoroles_add.success", EmoteReference.OK, iamName, role.getName());
            };

            Role role = FinderUtils.findRoleSelect(ctx, roleName, roleConsumer);
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

            var dbGuild = ctx.getDBGuild();
            Map<String, String> autoroles = dbGuild.getAutoroles();
            if (autoroles.containsKey(args[0])) {
                dbGuild.removeAutorole(args[0]);
                dbGuild.updateAllChanged();
                ctx.sendLocalized("options.autoroles_remove.success", EmoteReference.OK, args[0]);
            } else {
                ctx.sendLocalized("options.autoroles_remove.not_found", EmoteReference.ERROR);
            }
        });

        registerOption("autoroles:clear", "Autoroles clear",
                "Removes all autoroles.",
        "Removes all autoroles.", (ctx, args) -> {
            var dbGuild = ctx.getDBGuild();
            dbGuild.clearAutoroles();
            dbGuild.updateAllChanged();
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

            var dbGuild = ctx.getDBGuild();
            Map<String, List<String>> categories = dbGuild.getAutoroleCategories();
            if (categories.containsKey(category) && autorole == null) {
                ctx.sendLocalized("options.autoroles_category_add.already_exists", EmoteReference.ERROR);
                return;
            }

            categories.computeIfAbsent(category, (a) -> new ArrayList<>());

            if (autorole != null) {
                if (dbGuild.getAutoroles().containsKey(autorole)) {
                    dbGuild.addAutoroleCategory(category, autorole);
                    dbGuild.updateAllChanged();
                    ctx.sendLocalized("options.autoroles_category_add.success", EmoteReference.CORRECT, autorole, category);
                } else {
                    ctx.sendLocalized("options.autoroles_category_add.no_role", EmoteReference.ERROR, autorole);
                }
                return;
            }

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

            var dbGuild = ctx.getDBGuild();
            Map<String, List<String>> categories = dbGuild.getAutoroleCategories();
            if (!categories.containsKey(category)) {
                ctx.sendLocalized("options.autoroles_category_add.no_category", EmoteReference.ERROR, category);
                return;
            }

            if (autorole != null) {
                dbGuild.removeAutoroleCategoryRole(category, autorole);
                dbGuild.updateAllChanged();
                ctx.sendLocalized("options.autoroles_category_remove.success", EmoteReference.CORRECT, category, autorole);
                return;
            }

            dbGuild.removeAutoroleCategory(category);
            dbGuild.updateAllChanged();
            ctx.sendLocalized("options.autoroles_category_remove.success_new", EmoteReference.CORRECT, category);
        });

        registerOption("server:ignorebots:autoroles:toggle",
                "Bot autorole ignore", "Toggles between ignoring bots on autorole assign and not.", (ctx) -> {
            var dbGuild = ctx.getDBGuild();
            boolean ignore = dbGuild.isIgnoreBotsAutoRole();
            dbGuild.ignoreBotsAutoRole(!ignore);
            dbGuild.updateAllChanged();

            ctx.sendLocalized("options.server_ignorebots_autoroles_toggle.success", EmoteReference.CORRECT, dbGuild.isIgnoreBotsAutoRole());
        });
    }

    @SuppressWarnings("unused")
    @Override
    public String description() {
        return "Guild auto role and self-assigned roles configuration";
    }

}
