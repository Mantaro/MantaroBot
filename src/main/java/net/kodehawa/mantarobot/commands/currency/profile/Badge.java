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

package net.kodehawa.mantarobot.commands.currency.profile;

import net.dv8tion.jda.internal.utils.IOUtil;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.BiPredicate;

public enum Badge {
    //IF THE PREDICATE RETURNS FALSE IT MEANS THAT ITS EITHER HANDLED MANUALLY OR ELSEWHERE, NOT IN THE AUTOMATIC PROFILE CHECK.
    //Self-explanatory.
    DEVELOPER("Developer", "\uD83D\uDEE0", "Currently a developer of Mantaro.", 91, 92,
            ((player, dbUser) -> false), false
    ),

    //Contributed in any way to Mantaro's development.
    CONTRIBUTOR("Contributor", "\u2b50", "Contributed to Mantaro's Development.", 92, 91,
            ((player, dbUser) -> false), false
    ),

    //Because lars asked for it.
    COMMUNITY_ADMIN("Community Admin", "\uD83D\uDEE1", "Helps to maintain the Mantaro Hub community.", 92, 93,
            ((player, dbUser) -> false), false
    ),

    //Contributed in any way to Mantaro's developmendiscort.
    TRANSLATOR("Translator", "\uD83C\uDF10", "Helped translate part of Mantaro to another language.", 92, 91,
            ((player, dbUser) -> false), false
    ),

    //Is a helper, owo.
    HELPER_2("Helper", "\uD83D\uDC9A", "Helps to maintain the support influx on Mantaro Hub.", 92, 94,
            ((player, dbUser) -> false), false
    ),

    //Self-explanatory.
    DONATOR_2("Donator", "\u2764", "Actively helps on keeping Mantaro alive <3", 92, 94,
            ((player, dbUser) -> false), false
    ),

    //Helps find important bugs.
    BUG_HUNTER("Bug Hunter", "\uD83D\uDC7E", "Has reported one or more important bugs with details.", 92, 94,
            ((player, dbUser) -> false), false
    ),

    // --- START OF FIRST SEASON BADGES  (Top 1) ---
    SEASON1_WINNER1_MONEY("Season 1 - Top #1 Money", "\uD83D\uDC7E", "The player with the most money at the end of the first season.", 92, 94,
            ((player, dbUser) -> false), false
    ),

    SEASON1_WINNER1_REP("Season 1 - Top #1 Rep", "\uD83D\uDC7E", "The player with the most money at the end of the first season.", 92, 94,
            ((player, dbUser) -> false), false
    ),
    // --- END OF FIRST SEASON BADGES  (Top 1) --

    //Have more than 8 billion credits.
    ALTERNATIVE_WORLD("Isekai", "\uD83C\uDF0E", "Have more than 8 billion credits at any given time.", 92, 92,
            ((player, dbUser) -> player.getMoney() > 7526527671L), false
    ),

    //Self-explanatory. (Description)
    MARATHON_WINNER("Marathon Winner", "\uD83C\uDFC5", "Get to level 200 in Mantaro.", 91, 92,
            (player, dbUser) -> player.getLevel() >= 200, false
    ),

    // --- START OF FIRST SEASON BADGES (Top 2 - 5) ---
    SEASON1_WINNER2_MONEY("Season 1 - Top #2 Money", "\uD83D\uDC7E", "The 2nd player with the most money at the end of the first season.", 92, 94,
            ((player, dbUser) -> false), false
    ),

    SEASON1_WINNER2_REP("Season 1 - Top #2 Rep", "\uD83D\uDC7E", "The 2nd player with the most rep at the end of the first season.", 92, 94,
            ((player, dbUser) -> false), false
    ),

    SEASON1_WINNER3_MONEY("Season 1 - Top #3 Money", "\uD83D\uDC7E", "The 3rd player with the most money at the end of the first season.", 92, 94,
            ((player, dbUser) -> false), false
    ),

    SEASON1_WINNER3_REP("Season 1 - Top #3 Rep", "\uD83D\uDC7E", "The 3rd player with the most rep at the end of the first season.", 92, 94,
            ((player, dbUser) -> false), false
    ),

    SEASON1_WINNER4_MONEY("Season 1 - Top #4 Money", "\uD83D\uDC7E", "The 4th player with the most money at the end of the first season.", 92, 94,
            ((player, dbUser) -> false), false
    ),

    SEASON1_WINNER4_REP("Season 1 - Top #4 Rep", "\uD83D\uDC7E", "The 4th player with the most rep at the end of the first season.", 92, 94,
            ((player, dbUser) -> false), false
    ),

    SEASON1_WINNER5_MONEY("Season 1 - Top #5 Money", "\uD83D\uDC7E", "The 5th player with the most money at the end of the first season.", 92, 94,
            ((player, dbUser) -> false), false
    ),

    SEASON1_WINNER5_REP("Season 1 - Top #5 Rep", "\uD83D\uDC7E", "The 5th player with the most rep at the end of the first season.", 92, 94,
            ((player, dbUser) -> false), false
    ),
    // --- END OF FIRST SEASON BADGES (Top 2 - 5) ---

    BADGE_HUNTER("Badge Hunter", "\uD83C\uDFF5", "Get more than 40 badges", 91, 92,
            (player,  dbUser) -> player.getData().getBadges().size() > 40, false
    ),

    //Win more than 1000 games
    ADDICTED_GAMER("Addicted Gamer", "\uD83C\uDFAE", "Win 1000 games.", 91, 92,
            (player, dbUser) -> player.getData().getGamesWon() >= 1000, false
    ),

    EXPERT_MINER("Expert Miner", "<:sparkle_pick:492882143404359690>", "Get more than 100000 mining experience.", 91, 92,
            ((player, dbUser) -> player.getData().getMiningExperience() > 100000), false),

    EXPERT_FISHER("Expert Fisher", "<:sparkle_rod:492882143505154048>", "Get more than 100000 fishing experience.", 91, 92,
            ((player, dbUser) -> player.getData().getFishingExperience() > 100000), false),

    //Self-explanatory. (Description)
    MARATHON_RUNNER("Marathon Runner", "\uD83C\uDF96", "Get to level 150 in Mantaro.", 91, 92,
            (player, dbUser) -> player.getLevel() >= 150, false
    ),

    //Self-explanatory. (Description)
    FAST_RUNNER("Fast Runner", "\uD83D\uDEA9", "Get to level 100 in Mantaro.", 91, 92,
            (player, dbUser) -> player.getLevel() >= 100, false
    ),

    //Win more than 100 games
    GAMER("Gamer", "\uD83D\uDD79", "Win 100 games.", 91, 92,
            (player, dbUser) -> player.getData().getGamesWon() >= 100, false
    ),

    //Get a loot crate.
    LUCKY("Lucky", "\uD83C\uDF40", "Be lucky enough to loot a loot crate.", 92, 92,
            (player, dbUser) -> false, false
    ),

    //Have more than 5000 items stacked.
    SHOPPER("Shopper", "\uD83D\uDED2", "Have more than 5000 items of any kind.", 91, 92,
            (player, dbUser) -> player.getInventory().asList().stream()
                    .filter(itemStack -> itemStack.getItem() != Items.CLAIM_KEY)
                    .anyMatch(stack -> stack.getAmount() == 5000), false
    ),

    CHAMPION("Champion", "\uD83D\uDC51", "See yourself in a leaderboard.", 91, 92,
            (player, dbUser) -> false, false
    ),

    CRATE_OPENER("Crate Opener", "\uD83D\uDD13", "Open 40 crates.", 91, 92,
            (player, dbUser) -> player.getData().getCratesOpened() >= 40, false
    ),

    //Open a loot crate.
    THE_SECRET("The Secret", "\uD83D\uDCBC", "Open a loot crate.", 92, 92,
            (player, dbUser) -> false, false
    ),

    //Self-explanatory. (Description)
    MOST_KNOWN("Most known", "\uD83E\uDD47", "Earn 1000 reputation.", 91, 92,
            (player, dbUser) -> player.getReputation() >= 1000, false
    ),

    EXPERIENCED_MINER("Experienced Miner", "<:star_pick:492882142993580038>", "Get more than 10000 mining experience.", 91, 92,
            ((player, dbUser) -> player.getData().getMiningExperience() > 10000), false),

    EXPERIENCED_FISHER("Experienced Fisher", "<:star_rod:492882143354028064>", "Get more than 10000 fishing experience.", 91, 92,
            ((player, dbUser) -> player.getData().getFishingExperience() > 10000), false),

    //Self-explanatory. (Description)
    CELEBRITY("Celebrity", "\uD83E\uDD48", "Earn 100 reputation.", 91, 92,
            (player, dbUser) -> player.getReputation() >= 100, false
    ),

    //Self-explanatory. (Description)
    POPULAR("Popular", "\uD83E\uDD49", "Earn 10 reputation.", 91, 92,
            (player, dbUser) -> player.getReputation() >= 10, false
    ),

    KING_OF_SEA("King of the Sea", "\uD83D\uDD31", "Catch 35 sharks.", 91, 92,
            (player, dbUser) -> player.getData().getSharksCaught() >= 35, false
    ),

    //Get extremely lucky with slots.
    LUCKY_SEVEN("Lucky 7", "\uD83C\uDFB0", "Get more than 175 million in credits from slots.", 92, 92,
            (player, dbUser) -> false, false
    ),

    //Claim daily more than 100 days in a row.
    BIG_CLAIMER("Big Claimer", "\uD83C\uDF8A", "Claim daily more than 100 days in a row.", 91, 92,
            (player, dbUser) -> false, false
    ),

    //Claim daily more than 10 days in a row.
    CLAIMER("Claimer", "\uD83C\uDF89", "Claim daily more than 10 days in a row.", 91, 92,
            (player, dbUser) -> false, false
    ),

    //Be the waifu of your waifu.
    MUTUAL("Mutual", "\uD83C\uDF8E", "The waifu of your waifu.", 91, 92,
            (player, dbUser) -> false, false
    ),

    //Get claimed 1000 times as a waifu.
    BEST_WAIFU("Best Waifu", "\uD83D\uDC9B", "Get waifu claimed 1000 times (how?).", 91, 92,
            (player, dbUser) -> dbUser.getData().getTimesClaimed() >= 1000, false
    ),

    //Get claimed 100 times as a waifu.
    KNOWN_WAIFU("Known Waifu", "\uD83D\uDC9A", "Get waifu claimed 100 times.", 91, 92,
            (player, dbUser) -> dbUser.getData().getTimesClaimed() >= 100, false
    ),

    //Get claimed 10 times as waifu.
    POPULAR_WAIFU("Popular Waifu", "\uD83D\uDC99", "Get waifu claimed 10 times.", 91, 92,
            (player, dbUser) -> dbUser.getData().getTimesClaimed() >= 10, false
    ),

    //Get claimed once as a waifu.
    WAIFU("Waifu", "\ud83d\udda4", "Get waifu claimed once.", 91, 92,
            (player, dbUser) -> dbUser.getData().getTimesClaimed() >= 1, false
    ),

    //Participated on the christmas 2017 event
    CHRISTMAS("Christmas Spirit", "\uD83C\uDF85", "Participated in the christmas 2017 event!", 91, 92,
            (player, dbUser) -> false, false
    ),

    //Use a mod action with mantaro
    POWER_USER("Power User", "\uD83D\uDD27", "Do mod stuff with Mantaro.", 91, 92,
            (player, dbUser) -> false, false
    ),

    GOLDFISH_BRAIN("Goldfish Brain", "\uD83D\uDDD3", "Get reminded way too many times.", 91, 92,
            (player, dbUser) -> dbUser.getData().getRemindedTimes() > 100, false
    ),

    //Find a gem.
    GEM_FINDER("Gem Finder", "\uD83D\uDC8E", "Find a gem while mining.", 91, 92,
            (player, dbUser) -> false, false
    ),

    FIRST_MINER("First Time Miner", "<:comet_pick:492882142788059146>", "Get more than 1000 mining experience.", 91, 92,
            ((player, dbUser) -> player.getData().getMiningExperience() > 1000), false),

    FIRST_FISHER("First Time Fisher", "<:comet_rod:492882142779670528>", "Get more than 1000 fishing experience.", 91, 92,
            ((player, dbUser) -> player.getData().getFishingExperience() > 1000), false),

    //Mine a diamond.
    MINER("Miner", "\u26cf", "Find a diamond while mining.", 91, 92,
            (player, dbUser) -> false, false
    ),

    //Find a fish.
    FISHER("Fisher", "\uD83D\uDC1F", "Find a fish while fishing. How calm.", 91, 92,
            (player, dbUser) -> false, false
    ),

    CLEANER("Cleaner", "\uD83E\uDDF9", "Clean your inventory more than 50 times.", 91, 92,
            (player, dbUser) -> player.getData().getTimesMopped() > 50, false
    ),

    //Self-explanatory. (Description)
    RUNNER("Runner", "\uD83D\uDCCD", "Get to level 50 in Mantaro.", 91, 92,
            (player, dbUser) -> player.getLevel() >= 50, false
    ),

    WASTER("Waster", "", "Dump way too many items.", 91, 92,
            (player, dbUser) -> false, false
    ),

    //Use opts properly
    DID_THIS_WORK("Configurator", "\u2699", "Use any `~>opts` configuration successfully.", 91, 92,
            (player, dbUser) -> false, false
    ),

    //Use market more than 1000 times.
    COMPULSIVE_BUYER("Compulsive Buyer", "\uD83D\uDDDE", "Succesfully use market buy or sell more than 1000 times.", 91, 92,
            (player, dbUser) -> player.getData().getMarketUsed() > 1000, false
    ),

    MAD_SCIENTIST("Mad Scientist", "\u2697", "Used a ton of potions at once.", 91, 92,
            (player, dbUser) -> false, false
    ),

    //Gamble more than Integer.MAX_VALUE.
    GAMBLER("Gambler", "\uD83D\uDCB0", "Gambled their life away.", 91, 92,
            (player, dbUser) -> false, false
    ),

    //Get an unexpected exception.
    FIRE("Fire", "\uD83D\uDD25", "Ouch, ouch, someone please extinguish it!", 91, 92,
            (player, dbUser) -> false, false
    ),

    //Used one of the many NSFW image commands at least once.
    LEWDIE("Lewdie", "\uD83D\uDC40", "Used a lewd command.", 91, 92,
            (player, dbUser) -> false, false
    ),

    //Marry to someone.
    MARRIED("Married", "\uD83D\uDC8D", "Find your loved one.", 91, 92,
            (player, dbUser) -> false, false
    ),

    WAIFU_CLAIMER("Waifu Claimer", "\uD83C\uDF80", "Claimed a waifu.", 91, 92,
            (player, dbUser) -> false, false
    ),

    CLAIMED("Claimed", "\uD83D\uDCFF", "Got claimed as a waifu.", 91, 92,
            (player, dbUser) -> false, false
    ),

    //Self-explanatory. (Description)
    WALKER("Walker", "\uD83C\uDFF7", "Get to level 10 in Mantaro.", 91, 92,
            (player, dbUser) -> player.getLevel() >= 10, false
    ),

    //Divorce.
    HEART_BROKEN("Heart Broken", "\uD83D\uDC94", "Ouch, was good while it lasted.", 91, 92,
            (player, dbUser) -> false, false
    ),

    CALENDAR("Calendar", "\uD83D\uDCC5", "Set your profile timezone", 91, 92,
            (player, dbUser) -> false, false
    ),

    //Self-explanatory.
    WRITER("Writer", "\uD83D\uDCF0", "Set a profile description.", 91, 92,
            (player, dbUser) -> false, false
    ),

    //Get your marriage proposal turned down.
    DENIED("Denied", "\u26d4", "Get your marriage proposal turned down :(.", 91, 92,
            (player, dbUser) -> false, false
    ),

    //Buy something from the market.
    BUYER("Buyer", "\uD83D\uDECD", "Buy something from the market.", 91, 92,
            (player, dbUser) -> false, false
    ),

    //Hush-Hush (Mask-y)
    NUMERIC_LUCK("Numeric Luck", "\uD83D\uDD36", "All the numbers had a party, somehow they all ended up with the same dress.", 91, 92,
            (player, dbUser) -> false, true
    ),

    NUMERIC_PATHWAY("Numeric Pathway", "\uD83D\uDD36", "A lucky and sad destiny with six equal people meeting, who slowly fade away as you spend.", 91, 92,
            (player, dbUser) -> player.getMoney().toString().matches("([1-9])\\1{6,}"), true
    ),

    DESTINY_REACHES("Destiny Reaches", "\uD83D\uDD36", "Good and bad luck meeting together, with a little gem as a mediator.", 91, 92,
            (player, dbUser) -> false, true
    ),

    HACKING_ADDICTION("Hacking Addiction", "\uD83D\uDD36", "Playing and slashing away your time, with a thousand addictions and thirty-something deeds, meeting together.", 91, 92,
            (player, dbUser) -> player.getData().getGamesWon() == 1337, true //lol
    ),

    PATHWAY_SKY("Pathway to Sky", "\uD83D\uDD36", "With a heavenly beat we shall approach destiny, drums slowly fading between a piano melody, peaceful and chaotic in nature.", 91, 92,
            (player, dbUser) -> false, false
    ),

    HAPPINESS_BETWEEN("Happiness Found Between", "\uD83D\uDD36", "The sour line between happiness and sadness approaches between beats, with a single one we shall decide which prevails.", 91, 92,
            (player, dbUser) -> false, true
    ),

    COLDER_SUMMER("A Cold Summer", "\uD83D\uDD36", "With a drum beat we approach a cold summertime, a place where nobody can catch you, feeling a bit safer with you.", 91, 92,
            (player, dbUser) -> false, true
    ),

    SLOW("Slow", "\uD83D\uDD36", "Alexa, how did we get here again? ", 91, 92,
            (player, dbUser) -> false, true
    ),

    SMALL_SCIENTIST("Small Scientist", "\uD83D\uDD36", "A child scientist, a robot and a cat, what could go wrong?", 91, 92,
            (player, dbUser) -> false, true
    ),

    APPROACHING_DESTINY("Slowly Approaching Destiny", "\uD83D\uDD36", "One in 90, or more. Luck shall decide, you have no choice but go with it.", 91, 92,
            (player, dbUser) -> false, true
    ),

    REVELATION("Revelation", "\uD83D\uDD36", "A gun as a weapon, a chessboard as a world, passing through universes and flying through emotions.", 91, 92,
            (player, dbUser) -> false, true
    ),

    FINDING_WAIFU("Finding a Waifu", "\uD83D\uDD36", "Two Xs and some horns, what a deal.", 91, 92,
            (player, dbUser) -> false, true
    ),

    FLYING_MALWARE("Flying Malware", "\uD83D\uDD36", "Finding a piece of flying malware on your e-mail it's quite the deal, isn't it? And it talks!", 91, 92,
            (player, dbUser) -> false, true
    ),

    GOLD_VALUE("Gold Value", "\uD83D\uDD36", "Nine gold tails and a person. Finding it might not be an ordeal, but the deal is not favorable.", 91, 92,
            (player, dbUser) -> false, true
    ),

    LUCK_BEHIND("Luck Behind", "\uD83D\uDD36", "A one-in-six chance of counting the tale, maybe slightly too much, maybe slightly too little.", 91, 92,
            (player, dbUser) -> false, true
    ),

    TOO_BIG("Too Big To Fit", "\uD83D\uDD36", "The small magical box contained between dimensions, soon forever gone.", 91, 92,
            (player, dbUser) -> false, true
    ),

    RISKY_ORDEAL("Risky Ordeal", "\uD83D\uDD36", "It isn't about how fast you climb, it's about the steps it takes.", 91, 92,
            (player, dbUser) -> false, true
    ),

    SENSELESS_HOARDING("Senseless Hoarding", "\uD83D\uDD36", "With the hoarding comes a line, overflowing in mindless casino paper, fading away in a heartbeat.", 91, 92,
            (player, dbUser) -> false, true
    ),

    CPU("The CPU Inside", "\uD83D\uDD36", "I'm also a CPU. No, really!", 91, 92,
            (player, dbUser) -> false, true
    ),

    // ---------------------------------- LEGACY BADGES START HERE ----------------------------------

    //Legacy Badge DJ
    DJ("DJ", "\uD83C\uDFB6", "Legacy Badge (Unused and unobtainable)", 91, 92,
            (player, dbUser) -> false, false
    ),

    //Legacy Broken Helper Badge
    HELPER("Bugged", "\uD83D\uDC1B", "Bugged Helper. (Old Broken Helper Badge)", 92, 94,
            (player, dbUser) -> false, false
    ),

    //Upvote Mantaro on discordbots.org. No longer queried.
    UPVOTER("Upvoter", "\u2b06", "Upvote Mantaro on discordbots.org.", 92, 92,
            (player, dbUser) -> false, false
    ),

    //Legacy Broken Donor Badge
    DONATOR("Bugged 2", "\uD83D\uDC1B", "Bugged Donor. (Old Broken Donator Badge)", 92, 94,
            (player, dbUser) -> false, false
    );


    // ---------------------------------- LEGACY BADGES END HERE ----------------------------------

    //What does the fox say?
    public final String description;

    //The name to display.
    public final String display;

    //What to put on the user's avatar
    public final byte[] icon;
    //The unicode to display.
    public final String unicode;
    //Where does the icon go in the X axis relative to the circle placement on the avatar replacement.
    private final int iconStartX;
    //Where does the icon go in the Y axis relative to the circle placement on the avatar replacement.
    private final int iconStartY;
    private final BiPredicate<Player, DBUser> badgePredicate;

    /**
     * Represents an user badge.
     * A badge is a "recognition" of an user achievements or contributions to Mantaro's code or just achievements inside Mantaro itself.
     * The enum ordinal represents the order of which the badges will be displayed. The first badge will display on the
     * profile title itself, the rest (including the one on the title) will display on the "badges" version.
     *
     * @param display     The display name of this badge.
     * @param unicode     The unicode of the badge. Used to display on the profile.
     * @param description What did you do to win this
     * @param placeholder Whether to expect a placeholder image instead of an actual badge image (this being false will trigger a warning everytime the badge is loaded without a proper image)
     */
    Badge(String display, String unicode, String description, int iconStartX, int iconStartY, BiPredicate<Player, DBUser> badgePredicate, boolean placeholder) {
        this.display = display;
        this.unicode = unicode;
        this.description = description;
        this.iconStartX = iconStartX;
        this.iconStartY = iconStartY;
        this.badgePredicate = badgePredicate;

        if (display.equals("User")) {
            this.icon = new byte[0];
        } else {
            try {
                InputStream is = getClass().getClassLoader().getResourceAsStream("badges/" + display.toLowerCase() + ".png");
                if (is == null) {
                    if (!placeholder) {
                        LoggerHolder.LOGGER.error("No badge found for '" + display + "'");
                        this.icon = new byte[0];
                    } else {
                        //apply placeholder image
                        this.icon = IOUtil.readFully(getClass().getClassLoader().getResourceAsStream("badges/missing_image.png"));
                    }
                } else {
                    this.icon = IOUtil.readFully(is);
                }
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        }

    }

    /**
     * Looks up the Badge based on a String value, if nothing is found returns null.
     *
     * @param name The String value to match
     * @return The badge, or null if nothing is found.
     */
    public static Badge lookupFromString(String name) {
        for (Badge b : Badge.values()) {
            //field name search
            if (b.name().equalsIgnoreCase(name)) {
                return b;
            }

            //show name search
            if (b.display.equalsIgnoreCase(name)) {
                return b;
            }
        }
        return null;
    }

    public static void assignBadges(Player player, DBUser user) {
        for (Badge b : Badge.values()) {
            if (b.badgePredicate.test(player, user)) {
                player.getData().addBadgeIfAbsent(b);
            }
        }
    }

    /**
     * Applies the image into the user's avatar.
     *
     * @param userAvatar Avatar image as a byte array.
     * @param white      Whether the badge should display as only-white or full color otherwise
     * @return A byte[] with the modified image.
     */
    public byte[] apply(byte[] userAvatar, boolean white) {
        if (icon.length == 0) return userAvatar;
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
        return (unicode == null ? "" : " " + unicode + " ") + display;
    }

    public String getDescription() {
        return this.description;
    }

    public String getDisplay() {
        return this.display;
    }

    public byte[] getIcon() {
        return this.icon;
    }

    public String getUnicode() {
        return this.unicode;
    }

    public BiPredicate<Player, DBUser> getBadgePredicate() {
        return this.badgePredicate;
    }

    //need this to get access to a logger in the constructor
    private static class LoggerHolder {
        static final Logger LOGGER = LoggerFactory.getLogger("Badge");
    }

}
