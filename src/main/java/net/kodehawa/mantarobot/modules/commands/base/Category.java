package net.kodehawa.mantarobot.modules.commands.base;

import net.kodehawa.mantarobot.modules.commands.CommandPermission;

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
