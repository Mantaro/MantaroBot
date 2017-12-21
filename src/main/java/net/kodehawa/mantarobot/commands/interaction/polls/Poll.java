/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
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
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands.interaction.polls;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.interaction.Lobby;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.ReactionOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.core.listeners.operations.core.ReactionOperation;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Poll extends Lobby {
    @JsonIgnore
    private static final Map<String, Poll> runningPolls = new HashMap<>();
    @JsonIgnore
    private Future<Void> runningPoll;

    private final String id;
    private final long timeout;
    private boolean isCompliant = true;
    private String name = "";
    private String owner = "";
    private final String[] options;

    public Poll(@JsonProperty("id") String id, @JsonProperty("guildId") String guildId, @JsonProperty("channelId") String channelId, @JsonProperty("ownerId") String ownerId,
                @JsonProperty("name") String name, @JsonProperty("timeout") long timeout, @JsonProperty("options") String... options) {
        super(guildId, channelId);
        this.id = id;
        this.options = options;
        this.timeout = timeout;
        this.name = name;
        this.owner = ownerId;

        if(options.length > 9 || options.length < 2 || timeout > 2820000 || timeout < 30000) {
            isCompliant = false;
        }
    }

    public static Map<String, Poll> getRunningPolls() {
        return runningPolls;
    }

    public static PollBuilder builder() {
        return new PollBuilder();
    }

    public void startPoll() {
        try {
            if(!isCompliant) {
                getChannel().sendMessage(EmoteReference.WARNING +
                        "This poll cannot build. " +
                        "**Remember that the options must be a maximum of 9 and a minimum of 2 and the timeout must be a maximum of 45m and a minimum of 30s.**\n" +
                        "Options are separated with a comma, for example `1,2,3`. For spaced stuff use quotation marks at the start and end of the sentence.").queue();
                getRunningPolls().remove(getChannel().getId());
                return;
            }

            if(isPollAlreadyRunning(getChannel())) {
                getChannel().sendMessage(EmoteReference.WARNING + "There seems to be another poll running here...").queue();
                return;
            }

            if(!getGuild().getSelfMember().hasPermission(getChannel(), Permission.MESSAGE_ADD_REACTION)) {
                getChannel().sendMessage(EmoteReference.ERROR + "Seems like I cannot add reactions here...").queue();
                getRunningPolls().remove(getChannel().getId());
                return;
            }

            DBGuild dbGuild = MantaroData.db().getGuild(getGuild());
            GuildData data = dbGuild.getData();
            AtomicInteger at = new AtomicInteger();

            data.setRanPolls(data.getRanPolls() + 1L);
            dbGuild.saveAsync();

            String toShow = Stream.of(options).map(opt -> String.format("#%01d.- %s", at.incrementAndGet(), opt)).collect(Collectors.joining("\n"));

            if(toShow.length() > 1014) {
                toShow = "This was too long to show, so I pasted it: " + Utils.paste(toShow);
            }

            User author = MantaroBot.getInstance().getUserById(owner);

            EmbedBuilder builder = new EmbedBuilder().setAuthor(String.format("Poll #%1d created by %s",
                    data.getRanPolls(), author.getName()), null, author.getAvatarUrl())
                    .setDescription("**Poll started. React to the number to vote.**\n*" + name + "*\n" +
                            "Type &cancelpoll to cancel a running poll.")
                    .addField("Options", "```md\n" + toShow + "```", false)
                    .setColor(Color.CYAN)
                    .setThumbnail("https://cdn.pixabay.com/photo/2012/04/14/16/26/question-34499_960_720.png")
                    .setFooter("You have " + Utils.getHumanizedTime(timeout) + " to vote.", author.getAvatarUrl());


            getChannel().sendMessage(builder.build()).queue(this::createPoll);

            InteractiveOperations.createOverriding(getChannel(), timeout, e -> {
                if(e.getAuthor().getId().equals(owner)) {
                    if(e.getMessage().getContentRaw().equalsIgnoreCase("&cancelpoll")) {
                        runningPoll.cancel(true);
                        return Operation.COMPLETED;
                    }
                }
                return Operation.IGNORED;
            });

            runningPolls.put(getChannel().getId(), this);
        } catch(Exception e) {
            getChannel().sendMessage(EmoteReference.ERROR + "An unknown error has occurred while setting up a poll. Maybe try again?").queue();
        }
    }

    private boolean isPollAlreadyRunning(TextChannel channel) {
        return runningPolls.containsKey(channel.getId());
    }

    private String[] reactions(int options) {
        if(options < 2)
            throw new IllegalArgumentException("You need to add a minimum of 2 options.");
        if(options > 9)
            throw new IllegalArgumentException("The maximum amount of options is 9.");

        String[] r = new String[options];
        for(int i = 0; i < options; i++) {
            r[i] = (char) ('\u0031' + i) + "\u20e3";
        }

        return r;
    }

    private Future<Void> createPoll(Message message) {
        runningPoll = ReactionOperations.create(message, TimeUnit.MILLISECONDS.toSeconds(timeout), new ReactionOperation() {
            @Override
            public int add(MessageReactionAddEvent e) {
                int i = e.getReactionEmote().getName().charAt(0) - '\u0030';
                if(i < 1 || i > options.length) return Operation.IGNORED;
                return Operation.IGNORED; //always return false anyway lul
            }

            @Override
            public void onExpire() {
                if(getChannel() == null)
                    return;

                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setTitle("Poll results")
                        .setDescription("**Showing results for the poll started by " + MantaroBot.getInstance().getUserById(owner).getName() + "** with name: *" + name + "*")
                        .setFooter("Thanks for your vote", null);

                AtomicInteger react = new AtomicInteger(0);
                AtomicInteger counter = new AtomicInteger(0);
                String votes = new ArrayList<>(getChannel().getMessageById(message.getIdLong()).complete().getReactions()).stream()
                        .filter(r -> react.getAndIncrement() <= options.length)
                        .map(r -> "+Registered " + (r.getCount() - 1) + " votes for option " + options[counter.getAndIncrement()])
                        .collect(Collectors.joining("\n"));

                embedBuilder.addField("Results", "```diff\n" + votes + "```", false);
                getChannel().sendMessage(embedBuilder.build()).queue();
                getRunningPolls().remove(getChannel().getId());
            }

            @Override
            public void onCancel() {
                getChannel().sendMessage(EmoteReference.CORRECT + "Cancelled poll").queue();
                onExpire();
            }

        }, reactions(options.length));

        return runningPoll;
    }
}
