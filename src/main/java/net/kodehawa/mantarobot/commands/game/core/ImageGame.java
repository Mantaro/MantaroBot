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

package net.kodehawa.mantarobot.commands.game.core;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.requests.RestAction;
import net.kodehawa.mantarobot.utils.cache.URLCache;

import java.awt.Color;
import java.util.function.Consumer;

public abstract class ImageGame extends Game<String> {
    private final URLCache cache;

    public ImageGame(int cacheSize) {
        cache = new URLCache(cacheSize);
    }

    protected RestAction<Message> sendEmbedImage(MessageChannel channel, String url, Consumer<EmbedBuilder> embedConfigurator) {
        var eb = new EmbedBuilder();
        embedConfigurator.accept(eb);

        eb.setImage("attachment://image.png")
                .setColor(Color.PINK);

        var message = new MessageBuilder().setEmbed(eb.build());
        return channel.sendMessage(message.build())
                .addFile(cache.getInput(url), "image.png");
    }
}
