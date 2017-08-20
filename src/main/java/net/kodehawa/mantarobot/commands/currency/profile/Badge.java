package net.kodehawa.mantarobot.commands.currency.profile;

import lombok.Getter;
import net.dv8tion.jda.core.utils.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public enum Badge {
    //Self-explanatory.
    DEVELOPER("Developer", "\uD83D\uDEE0",
            "Currently a developer of Mantaro.", 91, 92),
    //Contributed in any way to mantaro's development.
    CONTRIBUTOR("Contributor", "\u2b50",
            "Contributed to Mantaro's Development.", 92, 91),
    //Because lars asked for it.
    COMMUNITY_ADMIN("Community Admin", "\uD83D\uDEE1",
            "Helps to maintain the Mantaro Hub community.", 92, 93),
    //Is a helper, owo.
    HELPER("Helper", "\uD83D\uDC9A",
            "Helps to maintain the support influx on Mantaro Hub.", 92, 94),
    //Self-explanatory.
    DONATOR("Donator", "\u2764",
            "Actively helps on keeping Mantaro alive <3", 92, 94),
    BUG_HUNTER("Bug Hunter", "\uD83D\uDC1B",
            "Has reported one or more important bugs with details.", 92, 94),
    //Have more than 8 billion credits.
    ALTERNATIVE_WORLD("Isekai", "\uD83C\uDF0E",
            "Have more than 8 billion credits at any given time.", 92, 92),
    //Have more than 5000 items stacked.
    SHOPPER("Shopper", "\uD83D\uDED2",
            "Have more than 5000 items of any kind.", 91, 92),
    //Win more than 100 games
    GAMER("Gamer", "\uD83D\uDD79",
            "Win 100 games.", 91, 92),
    //Use a mod action with mantaro
    POWER_USER("Power User", "\uD83D\uDD27",
            "Do mod stuff with Mantaro.", 91, 92),
    //Use opts properly
    DID_THIS_WORK("This worked", "\u26cf",
            "Used `~>opts` properly.", 91, 92),
    //Gamble more than Integer.MAX_VALUE.
    GAMBLER("Gambler", "\uD83D\uDCB0",
            "Gambled their life away.", 91, 92),
    //Queue more than 1000 songs.
    DJ("DJ", "\uD83C\uDFB6",
            "Too many songs.", 91, 92);

    //What does the fox say?
    @Getter
    public final String description;
    //The name to display.
    @Getter
    public final String display;
    //What to put on the user's avatar
    @Getter
    public final byte[] icon;
    private final int iconStartX;
    private final int iconStartY;
    //The unicode to display.
    @Getter
    public final String unicode;


    /**
     * Represents an user badge.
     * A badge is a "recognition" of an user achievements or contributions to Mantaro's code or just achievements inside
     * Mantaro itself.
     * The enum ordinal represents the order of which the badges will be displayed. The first badge will display on the
     * profile title itself, the rest (including the one on the title) will display on the "badges" version.
     * @param display The display name of this badge.
     * @param unicode The unicode of the badge. Used to display on the profile.
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

    public byte[] apply(byte[] userAvatar, boolean white) {
        if(icon.length == 0) return userAvatar;
        return BadgeUtils.applyBadge(userAvatar, icon, iconStartX, iconStartY, white);
    }

    public byte[] apply(byte[] userAvatar) {
        return apply(userAvatar, false);
    }

    /**
     * To show in badge list and probably in some other places.
     * @return A representation of this object in the form of name + unicode.
     */
    @Override
    public String toString() {
        return display + (unicode == null ? "" :  " " + unicode);
    }

    //need this to get access to a logger in the constructor
    private static class LoggerHolder {
        static final Logger LOGGER = LoggerFactory.getLogger("Badge");
    }
}
