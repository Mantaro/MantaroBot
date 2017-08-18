package net.kodehawa.mantarobot.core.listeners.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dv8tion.jda.core.entities.User;
import net.kodehawa.mantarobot.MantaroBot;

@AllArgsConstructor
public class CachedMessage {
    private long author;
    @Getter
    private String content;

    public User getAuthor(){
        return MantaroBot.getInstance().getUserById(author);
    }
}
