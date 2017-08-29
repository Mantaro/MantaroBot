/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Category {
    MUSIC(CommandPermission.USER, "Audio"),
    ACTION(CommandPermission.USER, "Action"),
    CURRENCY(CommandPermission.USER, "Currency"),
    GAMES(CommandPermission.USER, "Game"),
    IMAGE(CommandPermission.USER, "Image"),
    FUN(CommandPermission.USER, "Fun"),
    MODERATION(CommandPermission.ADMIN, "Moderation"),
    OWNER(CommandPermission.OWNER, "Owner"),
    INFO(CommandPermission.USER, "Info"),
    UTILS(CommandPermission.USER, "Utility"),
    MISC(CommandPermission.USER, "Misc");

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
            if(cat.s.equalsIgnoreCase(name)) {
                return cat;
            }
        }
        return null;
    }

    /**
     * @return The name of the category.
     */
    public static List<String> getAllNames() {
        return Stream.of(Category.values()).map(category -> category.s).collect(Collectors.toList());
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
