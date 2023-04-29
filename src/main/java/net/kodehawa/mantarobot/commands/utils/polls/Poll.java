/*
 * Copyright (C) 2016 Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands.utils.polls;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.MongoGuild;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;

import java.awt.Color;
import java.beans.ConstructorProperties;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Poll {
    private static final Logger log = LoggerFactory.getLogger(Poll.class);
    private static final JedisPool pool = MantaroData.getDefaultJedisPool();
    private static final ManagedDatabase db = MantaroData.db();
    private static final String ztable = "zpoll";
    private static final String table = "poll";

    private static final Pattern numbers = Pattern.compile("\\d\\u20e3");

    // Jackson infuriates me sometimes...
    @JsonProperty("messageId")
    private String messageId;
    @JsonProperty("guildId")
    private final String guildId;
    @JsonProperty("channelId")
    private final String channelId;
    @JsonProperty("name")
    private final String name;
    @JsonProperty("image")
    private final String image;
    @JsonProperty("options")
    private final List<String> options;
    @JsonProperty("time")
    private final long time;

    @JsonCreator
    @ConstructorProperties({"guildId", "channelId", "messageId", "name", "image", "options", "time"})
    public Poll(String guildId, String channelId, String messageId, String name, String image, List<String> options, long time) {
        this.guildId = guildId;
        this.channelId = channelId;
        this.messageId = messageId;
        this.name = name;
        this.image = image;
        this.options = options;
        this.time = time;
    }

    public String guildId() {
        return guildId;
    }

    public String channelId() {
        return channelId;
    }

    public String messageId() {
        return messageId;
    }

    public String name() {
        return name;
    }

    public String image() {
        return image;
    }

    public List<String> options() {
        return this.options;
    }

    public long time() {
        return time;
    }

    public String id() {
        if (messageId() == null) {
            throw new IllegalArgumentException("Haven't set message id!");
        }

        return channelId() + ":" + messageId();
    }

    @JsonIgnore
    private void setMessageId(String messageId) {
        if (messageId() != null) {
            throw new IllegalArgumentException("Already set message id!");
        }

        this.messageId = messageId;
    }

    public void start(SlashContext ctx) {
        // We might need this sanity checks in case we somehow get a delay on getting the call and the guild has already left.
        // This has happened in Audio before, so better safe than sorry.
        if (getGuild() == null)
            return;

        // Channel somehow doesn't exist anymore? Nowhere to send the message.
        if (getChannel() == null) {
            ctx.edit("commands.poll.invalid_channel", EmoteReference.ERROR);
            return;
        }

        var at = 0;
        var user = ctx.getAuthor();
        var languageContext = ctx.getLanguageContext();
        var builder = new EmbedBuilder().setAuthor(String.format(languageContext.get("commands.poll.header"), user.getName()), null, user.getAvatarUrl())
                .setColor(Color.CYAN)
                .setThumbnail("https://apiv2.mantaro.site/image/common/help-icon.png")
                .setFooter(String.format(languageContext.get("commands.poll.time"),
                        Utils.formatDuration(languageContext, time() - System.currentTimeMillis())), user.getAvatarUrl()
                );

        var filteredOptions = options().stream().filter(s -> !s.isBlank()).toList();
        for (var option : filteredOptions) {
            if (option.length() >= 1024) {
                ctx.edit("commands.poll.too_long", EmoteReference.ERROR);
                return;
            }

            builder.addField("Option #%01d".formatted(++at), MarkdownSanitizer.sanitize(option), false);
        }


        var image = image();
        if (image != null && EmbedBuilder.URL_PATTERN.asPredicate().test(image)) {
            builder.setImage(image);
        }

        if (image != null && EmbedBuilder.URL_PATTERN.asPredicate().test(image)) {
            builder.setImage(image);
        }

        var message = ctx.sendResult(builder.build());
        var reactions = reactions(filteredOptions.size());
        for (String reaction : reactions) {
            message.addReaction(Emoji.fromUnicode(reaction)).queue();
        }

        setMessageId(message.getId());
        schedule();
    }

    public void end() {
        // Cancel poll before doing existing checks -- we don't want the object to dangle.
        cancel();

        // Checks before sending the final poll message.
        if (getGuild() == null)
            return;

        if (getChannel() == null)
            return;

        if (messageId() == null || messageId.isEmpty()) {
            log.error("Null messageId on poll? This shouldn't happen, dump: {}", asJson());
            return;
        }

        getChannel().retrieveMessageById(messageId).queue(message -> {
            var languageContext = new I18nContext(db.getGuild(getGuild()), null);
            var user = message.getAuthor();
            var embedBuilder = new EmbedBuilder()
                    .setTitle(languageContext.get("commands.poll.result_header"))
                    .setDescription(String.format(languageContext.get("commands.poll.result_screen"), user.getName(), name))
                    .setFooter(languageContext.get("commands.poll.thank_note"), null);

            var react = new AtomicInteger(0);
            var counter = new AtomicInteger(0);

            var votes = message.getReactions().stream()
                    .filter(r -> react.getAndIncrement() <= options.size())
                    .filter(r -> numbers.matcher(r.getEmoji().asUnicode().getFormatted()).matches())
                    .map(r -> String.format(languageContext.get("commands.poll.vote_results"),
                            r.getCount() - 1, options.get(counter.getAndIncrement()))
                    )
                    .collect(Collectors.joining("\n"));

            embedBuilder.addField(languageContext.get("commands.poll.results"), "```diff\n" + votes + "```", false);
            getChannel().sendMessageEmbeds(embedBuilder.build()).queue(msg ->
                message.editMessage(languageContext.get("commands.poll.completed").formatted(EmoteReference.CORRECT, msg.getJumpUrl()))
                        .setEmbeds()
                        .queue()
            );
        });
    }

    // Cancel from inside
    public void cancel() {
        cancel(id(), db.getGuild(guildId));
    }

    // Cancel from outside.
    public static void cancel(String id, MongoGuild dbGuild) {
        try (var redis = pool.getResource()) {
            var data = redis.hget(table, id);

            redis.zrem(ztable, data);
            redis.hdel(table, id);
        }

        dbGuild.getRunningPolls().remove(id);
        dbGuild.save();
    }

    private void schedule() {
        // Basically save the entire object, as we'll need to recreate it.
        var scheduled = asJson();
        try (var redis = pool.getResource()) {
            redis.zadd(ztable, time, scheduled.toString());
            redis.hset(table, id(), scheduled.toString());
        }

        var dbGuild = db.getGuild(getGuild());
        dbGuild.getRunningPolls().put(id(), new PollDatabaseObject(messageId, channelId, name, time));
        dbGuild.save();
    }

    public static PollBuilder builder() {
        return new PollBuilder();
    }

    public Guild getGuild() {
        return MantaroBot.getInstance().getShardManager().getGuildById(guildId);
    }

    public GuildMessageChannel getChannel() {
        return getGuild().getChannelById(GuildMessageChannel.class, channelId);
    }

    private JSONObject asJson() {
        return new JSONObject()
                .put("guildId", guildId)
                .put("channelId", channelId)
                .put("messageId", messageId)
                .put("name", name)
                .put("image", image)
                .put("options", options)
                .put("time", time);
    }

    private static String[] reactions(int options) {
        var r = new String[options];
        for (int i = 0; i < options; i++) {
            r[i] = (char) ('\u0031' + i) + "\u20e3";
        }

        return r;
    }

    public record PollDatabaseObject(String messageId, String channelId, String name, long time) { }
}
