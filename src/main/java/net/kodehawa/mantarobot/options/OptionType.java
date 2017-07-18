package net.kodehawa.mantarobot.options;

import net.kodehawa.mantarobot.utils.Utils;

public enum OptionType {
    GENERAL, SPECIFIC, COMMAND, GUILD, CHANNEL, USER, MUSIC, MODERATION;

    @Override
    public String toString() {
        return Utils.capitalize(this.name().toLowerCase());
    }
}
