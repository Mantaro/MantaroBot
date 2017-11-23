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

package net.kodehawa.mantarobot.core.listeners.command;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.rethinkdb.gen.exc.ReqlError;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.hooks.EventListener;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.MantaroCore;
import net.kodehawa.mantarobot.core.listeners.entities.CachedMessage;
import net.kodehawa.mantarobot.core.listeners.events.ShardMonitorEvent;
import net.kodehawa.mantarobot.core.processor.core.ICommandProcessor;
import net.kodehawa.mantarobot.core.shard.MantaroShard;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.utils.SentryHelper;
import net.kodehawa.mantarobot.utils.Snow64;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class CommandListener implements EventListener {
    private static final Map<String, ICommandProcessor> CUSTOM_PROCESSORS = new ConcurrentHashMap<>();
    //Message cache of 35000 cached messages. If it reaches 20000 it will delete the first one stored, and continue being 35000
    @Getter
    private static final Cache<String, Optional<CachedMessage>> messageCache = CacheBuilder.newBuilder().concurrencyLevel(10).maximumSize(35000).build();
    //Commands ran this session.
    private static int commandTotal = 0;
    private final ICommandProcessor commandProcessor;
    private final Random random = new Random();
    private final MantaroShard shard;
    private final int shardId;
    private final String[] boomQuotes = {
            "Seemingly Megumin exploded our castle...", "Uh-oh, seemingly my master forgot some zeros and ones on the floor :<",
            "W-Wait, what just happened?", "I-I think we got some fire going on here... you might want to tell my master to take a look.",
            "I've mastered explosion magic, you see?", "Maybe something just went wrong on here, but, u-uh, I can fix it!",
            "U-Uhh.. What did you want?"
    };

    public CommandListener(int shardId, MantaroShard shard, ICommandProcessor processor) {
        this.shardId = shardId;
        this.shard = shard;
        commandProcessor = processor;
    }

    public static void clearCustomProcessor(String channelId) {
        CUSTOM_PROCESSORS.remove(channelId);
    }

    public static String getCommandTotal() {
        return String.valueOf(commandTotal);
    }

    public static void setCustomProcessor(String channelId, ICommandProcessor processor) {
        if(processor == null) CUSTOM_PROCESSORS.remove(channelId);
        else CUSTOM_PROCESSORS.put(channelId, processor);
    }

    @Override
    public void onEvent(Event event) {
        if(event instanceof ShardMonitorEvent) {
            if(MantaroBot.getInstance().getShardedMantaro().getShards()[shardId].getEventManager().getLastJDAEventTimeDiff() > 30000) return;

            //Hey, this listener is alive! (This won't pass if somehow this is blocked)
            ((ShardMonitorEvent) event).alive(shardId, ShardMonitorEvent.COMMAND_LISTENER);

            return;
        }

        if(event instanceof GuildMessageReceivedEvent) {
            GuildMessageReceivedEvent msg = (GuildMessageReceivedEvent) event;
            //Inserts a cached message into the cache. This only holds the id and the content, and is way lighter than saving the entire jda object.
            messageCache.put(msg.getMessage().getId(), Optional.of(new CachedMessage(msg.getAuthor().getIdLong(), msg.getMessage().getContent())));

            //Ignore myself and bots.
            if(msg.getAuthor().isBot() || msg.getAuthor().equals(msg.getJDA().getSelfUser())) return;

            shard.getCommandPool().execute(() -> onCommand(msg));
        }
    }

    private void onCommand(GuildMessageReceivedEvent event) {
        try {
            Member self = event.getGuild().getSelfMember();
            if(!self.getPermissions(event.getChannel()).contains(Permission.MESSAGE_WRITE) && !self.hasPermission(Permission.ADMINISTRATOR))
                return;
            if(event.getAuthor().isBot())
                return;
            if(CUSTOM_PROCESSORS.getOrDefault(event.getChannel().getId(), commandProcessor).run(event)) {
                commandTotal++;
            } else {
                //Only run experience if no command has been executed, avoids weird race conditions when saving player status.
                try {
                    if (random.nextInt(15) > 10) {
                        if (event.getMember() == null)
                            return;
                        if (event.getMember().getUser().isBot())
                            return;

                        Player player = MantaroData.db().getPlayer(event.getMember());

                        if (player != null) {
                            if (player.getLevel() == 0) {
                                player.setLevel(1);
                            }

                            player.getData().setExperience(player.getData().getExperience() + Math.round(random.nextInt(6)));

                            if (player.getData().getExperience() > (player.getLevel() * Math.log10(player.getLevel()) * 1000)) {
                                player.setLevel(player.getLevel() + 1);
                            }

                            player.saveAsync();
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch(IndexOutOfBoundsException e) {
            event.getChannel().sendMessage(EmoteReference.ERROR + "Your query returned no results or you used the incorrect arguments, seemingly. Just in case, check command help!").queue();
        } catch(PermissionException e) {
            if(e.getPermission() != Permission.UNKNOWN) {
                event.getChannel().sendMessage(EmoteReference.ERROR + "I don't have permission to do this :<, I need the permission: **" +
                        e.getPermission().getName() + "**" + (e.getMessage() != null ? " | Message: " + e.getMessage() : "")).queue();
            } else {
                event.getChannel().sendMessage(EmoteReference.ERROR + "I cannot perform this action due to the lack of permission! Is the role I might be trying to assign" +
                        " higher than my role? Do I have the correct permissions/hierarchy to perform this action?").queue();
            }
        } catch(IllegalArgumentException e) { //NumberFormatException == IllegalArgumentException
            String id = Snow64.toSnow64(event.getMessage().getIdLong());
            event.getChannel().sendMessage(EmoteReference.ERROR + "I think you forgot something on the floor. (Maybe we threw it there? [Error ID: " + id + "]... I hope we didn't)\n" +
                    "- Incorrect type arguments or the message I'm trying to send exceeds 2048 characters, Just in case, check command help!").queue();
            log.warn("Exception caught and alternate message sent. We should look into this, anyway (ID: " + id + ")", e);
        } catch(ReqlError e) {
            //So much just went wrong...
            e.printStackTrace();
            SentryHelper.captureExceptionContext("Something seems to have broken in the db! Check this out!", e, this.getClass(), "Database");
        } catch(Exception e) {
            String id = Snow64.toSnow64(event.getMessage().getIdLong());
            Random r = new Random();
            event.getChannel().sendMessage(
                    EmoteReference.ERROR + boomQuotes[r.nextInt(boomQuotes.length)] + "\n" +
                            "(Error ID: ``" + id + "``)\n" +
                            "If you want, join our **support guild** (Link on ``~>about``), or check out our GitHub page (/Mantaro/MantaroBot). Please tell them to quit exploding me and " +
                            "please don't forget the Error ID when reporting!"
            ).queue();

            SentryHelper.captureException("Unexpected Exception on Command: " + event.getMessage().getRawContent() + " | (Error ID: ``" + id + "``)", e, this.getClass());
            System.out.println("Error happened with id: " + id);
            e.printStackTrace();
        }
    }
}
