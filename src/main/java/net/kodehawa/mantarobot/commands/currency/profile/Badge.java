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

package net.kodehawa.mantarobot.commands.currency.profile;

import lombok.Getter;
import net.dv8tion.jda.core.utils.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public enum Badge {

    //Self-explanatory.
    DEVELOPER("Developer", "\uD83D\uDEE0", "Currently a developer of Mantaro.", 91, 92),

    //Contributed in any way to Mantaro's development.
    CONTRIBUTOR("Contributor", "\u2b50", "Contributed to Mantaro's Development.", 92, 91),

    //Because lars asked for it.
    COMMUNITY_ADMIN("Community Admin", "\uD83D\uDEE1", "Helps to maintain the Mantaro Hub community.", 92, 93),

    //Is a helper, owo.
    HELPER("Helper", "\uD83D\uDC9A", "Helps to maintain the support influx on Mantaro Hub.", 92, 94),

    //Self-explanatory.
    DONATOR("Donator", "\u2764", "Actively helps on keeping Mantaro alive <3", 92, 94),

    //Helps find important bugs.
    BUG_HUNTER("Bug Hunter", "\uD83D\uDC1B", "Has reported one or more important bugs with details.", 92, 94),

    //Have more than 8 billion credits.
    ALTERNATIVE_WORLD("Isekai", "\uD83C\uDF0E", "Have more than 8 billion credits at any given time.", 92, 92),

    //Self-explanatory. (Description)
    MARATHON_WINNER("Marathon Winner", "\uD83C\uDFC5", "Get to level 200 in Mantaro.", 91, 92),

    //Get a loot crate.
    LUCKY("Lucky", "\uD83C\uDF40", "Be lucky enough to loot a loot crate.", 92, 92),

    //Upvote Mantaro on discordbots.org.
    UPVOTER("Upvoter", "\u2b06", "Upvote Mantaro on discordbots.org.", 92, 92),

    //Have more than 5000 items stacked.
    SHOPPER("Shopper", "\uD83D\uDED2", "Have more than 5000 items of any kind.", 91, 92),

    //Open a loot crate.
    THE_SECRET("The Secret", "\uD83D\uDCBC", "Open a loot crate.", 92, 92),

    //Self-explanatory. (Description)
    MARATHON_RUNNER("Marathon Runner", "\uD83C\uDF96", "Get to level 150 in Mantaro.", 91, 92),

    //Self-explanatory. (Description)
    FAST_RUNNER("Fast Runner", "\uD83D\uDEA9", "Get to level 100 in Mantaro.", 91, 92),

    //Win more than 100 games
    GAMER("Gamer", "\uD83D\uDD79", "Win 100 games.", 91, 92),

    //Get extremely lucky with slots.
    LUCKY_SEVEN("Lucky 7", "\uD83C\uDFB0", "Get more than 175 million in credits from slots.", 92, 92),

    //Claim daily more than 100 days in a row.
    BIG_CLAIMER("Big Claimer", "\uD83C\uDF8A", "Claim daily more than 100 days in a row.", 91, 92),

    //Claim daily more than 10 days in a row.
    CLAIMER("Claimer", "\uD83C\uDF89", "Claim daily more than 10 days in a row.", 91, 92),

    //Participated on the christmas 2017 event
    CHRISTMAS("Christmas Spirit", "\uD83C\uDF85", "Participated on the christmas 2017 event!", 91, 92),

    //Use a mod action with mantaro
    POWER_USER("Power User", "\uD83D\uDD27", "Do mod stuff with Mantaro.", 91, 92),

    //Mine a diamond.
    MINER("Miner", "\u26cf", "Find a diamond while mining.", 91, 92),

    //Self-explanatory. (Description)
    RUNNER("Runner", "\uD83D\uDCCD", "Get to level 50 in Mantaro.", 91, 92),

    //Use opts properly
    DID_THIS_WORK("Configurator", "\u2699", "Use any `~>opts` configuration successfully.", 91, 92),

    //Gamble more than Integer.MAX_VALUE.
    GAMBLER("Gambler", "\uD83D\uDCB0", "Gambled their life away.", 91, 92),

    //Used one of the many NSFW image commands at least once.
    LEWDIE("Lewdie", "\uD83D\uDC40", "Used a lewd command.", 91, 92),

    //Marry to someone.
    MARRIED("Married", "\uD83D\uDC8D", "Find your loved one.", 91, 92),

    //Self-explanatory. (Description)
    WALKER("Walker", "\uD83C\uDFF7", "Get to level 10 in Mantaro.", 91, 92),

    //Get your marriage proposal turned down.
    DENIED("Denied", "\u26d4", "Get your marriage proposal turned down :(.", 91, 92),

    //Buy something from the market.
    BUYER("Buyer", "\uD83D\uDECD", "Get your marriage proposal turned down :(.", 91, 92),

    //TODO do this for once
    //Queued more than 3000 songs.
    DJ("DJ", "\uD83C\uDFB6", "Too many songs.", 91, 92);

    //What does the fox say?
    @Getter
    public final String description;

    //The name to display.
    @Getter
    public final String display;

    //What to put on the user's avatar
    @Getter
    public final byte[] icon;
    //The unicode to display.
    @Getter
    public final String unicode;
    //Where does the icon go in the X axis relative to the circle placement on the avatar replacement.
    private final int iconStartX;
    //Where does the icon go in the Y axis relative to the circle placement on the avatar replacement.
    private final int iconStartY;


    /**
     * Represents an user badge.
     * A badge is a "recognition" of an user achievements or contributions to Mantaro's code or just achievements inside Mantaro itself.
     * The enum ordinal represents the order of which the badges will be displayed. The first badge will display on the
     * profile title itself, the rest (including the one on the title) will display on the "badges" version.
     *
     * @param display     The display name of this badge.
     * @param unicode     The unicode of the badge. Used to display on the profile.
     * @param description What did you do to win this
     */
    Badge(String display, String unicode, String description, int iconStartX, int iconStartY) {
        this.display = display;
        this.unicode = unicode;
        this.description = description;
        this.iconStartX = iconStartX;
        this.iconStartY = iconStartY;

        if(display.equals("User")) {
            this.icon = new byte[0];
            return;
        }

        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("badges/" + display.toLowerCase() + ".png");
            if(is == null) {
                LoggerHolder.LOGGER.error("No badge found for '" + display + "'");
                this.icon = new byte[0];
            } else {
                this.icon = IOUtil.readFully(is);
            }
        } catch(IOException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Looks up the Badge based on a String value, if nothing is found returns null.
     *
     * @param name The String value to match
     * @return The badge, or null if nothing is found.
     */
    public static Badge lookupFromString(String name) {
        for(Badge b : Badge.values()) {
            if(b.name().equalsIgnoreCase(name)) {
                return b;
            }
        }
        return null;
    }

    /**
     * Applies the image into the user's avatar.
     *
     * @param userAvatar Avatar image as a byte array.
     * @param white      Whether the badge should display as only-white or full color otherwise
     * @return A byte[] with the modified image.
     */
    public byte[] apply(byte[] userAvatar, boolean white) {
        if(icon.length == 0) return userAvatar;
        return BadgeUtils.applyBadge(userAvatar, icon, iconStartX, iconStartY, white);
    }

    /**
     * Applies the image into the user's avatar.
     *
     * @param userAvatar Avatar image as a byte array.
     * @return A byte[] with the modified image.
     */
    public byte[] apply(byte[] userAvatar) {
        return apply(userAvatar, false);
    }

    /**
     * To show in badge list and probably in some other places.
     *
     * @return A representation of this object in the form of name + unicode.
     */
    @Override
    public String toString() {
        return display + (unicode == null ? "" : " " + unicode);
    }

    //need this to get access to a logger in the constructor
    private static class LoggerHolder {
        static final Logger LOGGER = LoggerFactory.getLogger("Badge");
    }
}
