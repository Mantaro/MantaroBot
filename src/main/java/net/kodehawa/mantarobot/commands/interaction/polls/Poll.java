/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands.interaction.polls;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.interaction.Lobby;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.ReactionOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.core.listeners.operations.core.ReactionOperation;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Poll extends Lobby {
    private static final Map<String, Poll> runningPolls = new HashMap<>();
    private final String id;
    private final long timeout;
    private final String[] options;
    private final I18nContext languageContext;
    private final String image;
    private Future<Void> runningPoll;
    private boolean isCompliant = true;
    private String name;
    private String owner;
    
    public Poll(String id, String guildId, String channelId, String ownerId, String name, long timeout, I18nContext languageContext, String image, String... options) {
        super(guildId, channelId);
        this.id = id;
        this.options = options;
        this.timeout = timeout;
        this.name = name;
        this.owner = ownerId;
        this.languageContext = languageContext;
        this.image = image;
        
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
                getChannel().sendMessageFormat(languageContext.get("commands.poll.invalid"), EmoteReference.WARNING).queue();
                getRunningPolls().remove(getChannel().getId());
                return;
            }
            
            if(isPollAlreadyRunning(getChannel())) {
                getChannel().sendMessageFormat(languageContext.get("commands.poll.other_poll_running"), EmoteReference.WARNING).queue();
                return;
            }
            
            if(!getGuild().getSelfMember().hasPermission(getChannel(), Permission.MESSAGE_ADD_REACTION)) {
                getChannel().sendMessageFormat(languageContext.get("commands.poll.no_reaction_perms"), EmoteReference.ERROR).queue();
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
                toShow = String.format(languageContext.get("commands.poll.too_long"), Utils.paste(toShow));
            }
            
            User author = MantaroBot.getInstance().getShardManager().getUserById(owner);
            
            EmbedBuilder builder = new EmbedBuilder().setAuthor(String.format(languageContext.get("commands.poll.header"),
                    data.getRanPolls(), author.getName()), null, author.getAvatarUrl())
                                           .setDescription(String.format(languageContext.get("commands.poll.success"), name))
                                           .addField(languageContext.get("general.options"), "```md\n" + toShow + "```", false)
                                           .setColor(Color.CYAN)
                                           .setThumbnail("https://cdn.pixabay.com/photo/2012/04/14/16/26/question-34499_960_720.png")
                                           .setFooter(String.format(languageContext.get("commands.poll.time"), Utils.getHumanizedTime(timeout)), author.getAvatarUrl());
            
            
            if(image != null && EmbedBuilder.URL_PATTERN.asPredicate().test(image))
                builder.setImage(image);
            
            getChannel().sendMessage(builder.build()).queue(message -> createPoll(message, languageContext));
            
            InteractiveOperations.create(getChannel(), Long.parseLong(owner), timeout, e -> {
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
            e.printStackTrace();
            getChannel().sendMessageFormat(languageContext.get("commands.poll.error"), EmoteReference.ERROR).queue();
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
    
    private Future<Void> createPoll(Message message, I18nContext languageContext) {
        runningPoll = ReactionOperations.create(message, TimeUnit.MILLISECONDS.toSeconds(timeout), new ReactionOperation() {
            @Override
            public int add(MessageReactionAddEvent e) {
                int i = e.getReactionEmote().getName().charAt(0) - '\u0030';
                return Operation.IGNORED; //always return false anyway lul
            }
            
            @Override
            public void onExpire() {
                if(getChannel() == null)
                    return;
                
                EmbedBuilder embedBuilder = new EmbedBuilder()
                                                    .setTitle(languageContext.get("commands.poll.result_header"))
                                                    .setDescription(String.format(languageContext.get("commands.poll.result_screen"),
                                                            MantaroBot.getInstance().getShardManager().getUserById(owner).getName(), name))
                                                    .setFooter(languageContext.get("commands.poll.thank_note"), null);
                
                AtomicInteger react = new AtomicInteger(0);
                AtomicInteger counter = new AtomicInteger(0);
                
                getChannel().retrieveMessageById(message.getIdLong()).queue(message -> {
                    String votes = message.getReactions().stream()
                                           .filter(r -> react.getAndIncrement() <= options.length)
                                           .map(r -> String.format(languageContext.get("commands.poll.vote_results"), r.getCount() - 1, options[counter.getAndIncrement()]))
                                           .collect(Collectors.joining("\n"));
                    
                    embedBuilder.addField(languageContext.get("commands.poll.results"), "```diff\n" + votes + "```", false);
                    getChannel().sendMessage(embedBuilder.build()).queue();
                });
                
                getRunningPolls().remove(getChannel().getId());
            }
            
            @Override
            public void onCancel() {
                getChannel().sendMessageFormat(languageContext.get("commands.poll.cancelled"), EmoteReference.CORRECT).queue();
                onExpire();
            }
            
        }, reactions(options.length));
        
        return runningPoll;
    }
    
    public String getId() {
        return this.id;
    }
}
