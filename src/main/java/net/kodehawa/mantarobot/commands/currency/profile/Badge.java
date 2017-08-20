package net.kodehawa.mantarobot.commands.currency.profile;

import lombok.Getter;

public enum Badge {
    //Self-explanatory.
    DEVELOPER("Developer", "\uD83D\uDEE0"),
    //Contributed in any way to mantaro's development.
    CONTRIBUTOR("Contributor", "\u2b50"),
    //Is a helper, owo.
    HELPER("Helper", "\uD83D\uDC9A"),
    //Self-explanatory.
    DONATOR("Donator", "\u2764"),
    //Have more than 8 billion credits.
    ALTERNATIVE_WORLD("Isekai", "\uD83C\uDF0E"),
    //Have more than 5000 items stacked.
    SHOPPER("Shopper", "\uD83D\uDED2"),
    //Win more than 100 games
    GAMER("Gamer", "\uD83D\uDD79"),
    //Use a mod action with mantaro
    POWER_USER("Power User", "\uD83D\uDD27"),
    //Use opts properly
    DID_THIS_WORK("This worked??", "\u26cf"),
    //Gamble more than Integer.MAX_VALUE.
    GAMBLER("Gambler", "\uD83D\uDCB0"),
    //Queue more than 1000 songs.
    DJ("DJ", "\uD83C\uDFB6"),
    //Default.
    USER("User", null);


    //The name to display.
    @Getter
    public String display;
    //The unicode to display.
    @Getter
    public String unicode;

    /**
     * Represents an user badge.
     * A badge is a "recognition" of an user achievements or contributions to Mantaro's code or just achievements inside
     * Mantaro itself.
     * The enum ordinal represents the order of which the badges will be displayed. The first badge will display on the
     * profile title itself, the rest (including the one on the title) will display on the "badges" version.
     * @param display The display name of this badge.
     * @param unicode The unicode of the badge. Used to display on the profile.
     */
    Badge(String display, String unicode){
        this.display = display;
        this.unicode = unicode;
    }

    /**
     * To show in badge list and probably in some other places.
     * @return A representation of this object in the form of name + unicode.
     */
    @Override
    public String toString() {
        return display + (unicode == null ? "" :  " " + unicode);
    }
}
