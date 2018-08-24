/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.core.modules.commands.base;

import net.kodehawa.mantarobot.utils.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Category {
    MUSIC(CommandPermission.USER, "categories.audio"),
    ACTION(CommandPermission.USER, "categories.action"),
    CURRENCY(CommandPermission.USER, "categories.currency"),
    GAMES(CommandPermission.USER, "categories.games"),
    IMAGE(CommandPermission.USER, "categories.image"),
    FUN(CommandPermission.USER, "categories.fun"),
    MODERATION(CommandPermission.ADMIN, "categories.moderation"),
    OWNER(CommandPermission.OWNER, "categories.owner"),
    INFO(CommandPermission.USER, "categories.info"),
    UTILS(CommandPermission.USER, "categories.utility"),
    MISC(CommandPermission.USER, "categories.misc");

    public final CommandPermission permission;
    private final String s;

    Category(CommandPermission p, String s) {
        this.permission = p;
        this.s = s;
    }

    /**
     * Looks up the Category based on a String value, if nothing is found returns null.
     * *
     *
     * @param name The String value to match
     * @return The category, or null if nothing is found.
     */
    public static Category lookupFromString(String name) {
        for(Category cat : Category.values()) {
            if(cat.name().equalsIgnoreCase(name)) {
                return cat;
            }
        }
        return null;
    }

    /**
     * @return The name of the category.
     */
    public static List<String> getAllNames() {
        return Stream.of(Category.values()).map(category -> Utils.capitalize(category.name().toLowerCase())).collect(Collectors.toList());
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
