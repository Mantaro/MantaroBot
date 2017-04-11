package net.kodehawa.mantarobot.commands.game.core;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.requests.RestAction;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.cache.URLCache;

import java.util.function.Consumer;

public abstract class ImageGame extends Game {
    private final URLCache cache;

    public ImageGame(int cacheSize) {
        cache = new URLCache(cacheSize);
    }

    protected RestAction<Message> sendEmbedImage(MessageChannel channel, String url, Consumer<EmbedBuilder> embedConfigurator) {
        EmbedBuilder eb = new EmbedBuilder();
        embedConfigurator.accept(eb);
        if(MantaroData.config().get().cacheGames) {
            eb.setImage("attachment://image.png");
            return channel.sendFile(cache.getInput(url), "image.png", new MessageBuilder().setEmbed(eb.build()).build());
        }
        eb.setImage(url);
        return channel.sendMessage(eb.build());
    }
}
