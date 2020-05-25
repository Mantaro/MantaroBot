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

package net.kodehawa.mantarobot.core.modules.commands.base;

import net.kodehawa.mantarobot.utils.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Category {
    MUSIC(CommandPermission.USER, "categories.music", "Audio"),
    ACTION(CommandPermission.USER, "categories.action", "Action"),
    CURRENCY(CommandPermission.USER, "categories.currency", "Currency"),
    GAMES(CommandPermission.USER, "categories.games", "Games"),
    IMAGE(CommandPermission.USER, "categories.image", "Image"),
    FUN(CommandPermission.USER, "categories.fun", "Fun"),
    MODERATION(CommandPermission.ADMIN, "categories.moderation", "Moderation"),
    OWNER(CommandPermission.OWNER, "categories.owner", "Owner"),
    INFO(CommandPermission.USER, "categories.info", "Info"),
    UTILS(CommandPermission.USER, "categories.utils", "Utility"),
    MISC(CommandPermission.USER, "categories.misc", "Misc"),
    PETS(CommandPermission.USER, "categories.pet", "Pets");

    public final CommandPermission permission;
    private final String s;
    private final String qualifiedName;

    Category(CommandPermission p, String s, String name) {
        this.permission = p;
        this.s = s;
        this.qualifiedName = name;
    }

    /**
     * Looks up the Category based on a String value, if nothing is found returns null.
     * *
     *
     * @param name The String value to match
     * @return The category, or null if nothing is found.
     */
    public static Category lookupFromString(String name) {
        for (Category cat : Category.values()) {
            if (cat.qualifiedName.equalsIgnoreCase(name)) {
                return cat;
            }
        }
        return null;
    }

    /**
     * @return The name of the category.
     */
    public static List<String> getAllNames() {
        return Stream.of(Category.values()).map(category -> Utils.capitalize(category.qualifiedName.toLowerCase())).collect(Collectors.toList());
    }

    /**
     * @return All categories as a List. You could do Category#values anyway, this is just for my convenience.
     */
    public static List<Category> getAllCategories() {
        return Arrays.asList(Category.values());
    }

    @Override
    public String toString() {
        return s;
    }
}
