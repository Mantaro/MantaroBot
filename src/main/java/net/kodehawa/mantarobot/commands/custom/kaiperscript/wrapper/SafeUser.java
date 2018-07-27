package net.kodehawa.mantarobot.commands.custom.kaiperscript.wrapper;

import net.dv8tion.jda.core.entities.User;

public class SafeUser extends SafeJDAObject<User> {
    SafeUser(User user) {
        super(user);
    }

    public String getName() {
        return object.getName();
    }

    public String getDiscriminator() {
        return object.getDiscriminator();
    }

    public String getAvatar() {
        return object.getEffectiveAvatarUrl();
    }

    public boolean getIsBot() {
        return object.isBot();
    }

    public String getMention() {
        return object.getAsMention();
    }

    @Override
    public String toString() {
        return "User(" + getId() + ")";
    }
}
