package net.kodehawa.mantarobot.commands.custom.kaiperscript.wrapper;

import net.dv8tion.jda.core.entities.User;

class SafeUser extends SafeISnowflake<User> {
    SafeUser(User user) {
        super(user);
    }

    public String getName() {
        return snowflake.getName();
    }

    public String getDiscriminator() {
        return snowflake.getDiscriminator();
    }

    public String getAvatarUrl() {
        return snowflake.getEffectiveAvatarUrl();
    }

    public boolean isBot() {
        return snowflake.isBot();
    }

    public String getAsMention() {
        return snowflake.getAsMention();
    }
}
