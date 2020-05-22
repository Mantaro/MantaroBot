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

package net.kodehawa.mantarobot.core.listeners.command;

import com.google.common.cache.Cache;
import com.rethinkdb.gen.exc.ReqlError;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.hooks.EventListener;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.custom.EmbedJSON;
import net.kodehawa.mantarobot.commands.custom.legacy.DynamicModifiers;
import net.kodehawa.mantarobot.core.listeners.entities.CachedMessage;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.processor.core.ICommandProcessor;
import net.kodehawa.mantarobot.data.I18n;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.db.entities.helpers.PlayerData;
import net.kodehawa.mantarobot.utils.LanguageKeyNotFoundException;
import net.kodehawa.mantarobot.utils.SentryHelper;
import net.kodehawa.mantarobot.utils.Snow64;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.RateLimiter;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class CommandListener implements EventListener {
    private static final RateLimiter experienceRatelimiter = new RateLimiter(TimeUnit.SECONDS, 18);
    private static final Logger log = LoggerFactory.getLogger(CommandListener.class);
    //Commands ran this session.
    private static int commandTotal = 0;
    private final Random random = new Random();
    private final ICommandProcessor commandProcessor;
    private final ExecutorService threadPool;
    private final Cache<Long, Optional<CachedMessage>> messageCache;

    public CommandListener(ICommandProcessor processor, ExecutorService threadPool, Cache<Long, Optional<CachedMessage>> messageCache) {
        this.commandProcessor = processor;
        this.threadPool = threadPool;
        this.messageCache = messageCache;
    }

    public static String getCommandTotal() {
        return String.valueOf(commandTotal);
    }

    public static int getCommandTotalInt() {
        return commandTotal;
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (event instanceof GuildMessageReceivedEvent) {
            GuildMessageReceivedEvent msg = (GuildMessageReceivedEvent) event;
            //Inserts a cached message into the cache. This only holds the id and the content, and is way lighter than saving the entire jda object.
            messageCache.put(msg.getMessage().getIdLong(), Optional.of(new CachedMessage(msg.getAuthor().getIdLong(), msg.getMessage().getContentDisplay())));

            //Ignore myself and bots.
            if (msg.getAuthor().isBot() || msg.isWebhookMessage() || msg.getAuthor().equals(msg.getJDA().getSelfUser()))
                return;

            threadPool.execute(() -> onCommand(msg));
        }
    }

    private void onCommand(GuildMessageReceivedEvent event) {
        try {
            Member self = event.getGuild().getSelfMember();
            if (!self.getPermissions(event.getChannel()).contains(Permission.MESSAGE_WRITE) && !self.hasPermission(Permission.ADMINISTRATOR))
                return;

            if (commandProcessor.run(event)) {
                commandTotal++;
            } else {
                //Only run experience if no command has been executed, avoids weird race conditions when saving player status.
                try {
                    //Only run experience if the user is not rate limited (clears every 30 seconds)
                    if (random.nextInt(15) > 7 && !event.getAuthor().isBot() && experienceRatelimiter.process(event.getAuthor())) {
                        if (event.getMember() == null)
                            return;

                        //Don't run the experience handler on this channel if there's an InteractiveOperation running as there might be issues with
                        //some nasty race conditions involving player save.
                        if (InteractiveOperations.get(event.getChannel()).size() > 0)
                            return;

                        Player player = MantaroData.db().getPlayer(event.getAuthor());
                        PlayerData data = player.getData();
                        DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                        GuildData guildData = dbGuild.getData();

                        if (player.isLocked())
                            return;

                        // ---------- GLOBAL EXPERIENCE CHECK ---------- //

                        //Set level to 1 if level is zero.
                        if (player.getLevel() == 0)
                            player.setLevel(1);

                        //Set player experience to a random number between 1 and 5.
                        data.setExperience(data.getExperience() + Math.round(random.nextInt(5)));

                        //Apply some black magic.
                        if (data.getExperience() > (player.getLevel() * Math.log10(player.getLevel()) * 1000) + (50 * player.getLevel() / 2D)) {
                            player.setLevel(player.getLevel() + 1);
                            //Check if the member is not null, just to be sure it happened in-between.
                            if (player.getLevel() > 1 && event.getGuild().getMemberById(player.getUserId()) != null) {
                                if (guildData.isEnabledLevelUpMessages()) {
                                    String levelUpChannel = guildData.getLevelUpChannel();
                                    String levelUpMessage = guildData.getLevelUpMessage();

                                    //Player has leveled up!
                                    if (levelUpMessage != null && levelUpChannel != null) {
                                        processMessage(String.valueOf(player.getLevel()), levelUpMessage, levelUpChannel, event);
                                    }
                                }
                            }
                        }

                        //This time, actually remember to save the player so you don't have to restart 102 shards to fix it.
                        player.saveAsync();
                    }
                } catch (Exception ignored) { }
            }
        } catch (IndexOutOfBoundsException e) {
            event.getChannel().sendMessage(EmoteReference.ERROR + "Your query returned no results or you used the incorrect arguments, seemingly. Just in case, check command help!").queue();
        } catch (PermissionException e) {
            if (e.getPermission() != Permission.UNKNOWN) {
                event.getChannel().sendMessage(String.format("%sI don't have permission to do this :(, I need the permission: **%s**%s", EmoteReference.ERROR, e.getPermission().getName(), e.getMessage() != null ? String.format(" | Message: %s", e.getMessage()) : "")).queue();
            } else {
                event.getChannel().sendMessage(EmoteReference.ERROR + "I cannot perform this action due to the lack of permission! Is the role I might be trying to assign" +
                        " higher than my role? Do I have the correct permissions/hierarchy to perform this action?").queue();
            }
        } catch (LanguageKeyNotFoundException e) {
            String id = Snow64.toSnow64(event.getMessage().getIdLong());
            event.getChannel().sendMessageFormat("%sWrong I18n key found, please report on the support server " +
                    "(Link at `support.mantaro.site`) with error ID `%s`.\n%sMessage: *%s*", EmoteReference.ERROR, id, EmoteReference.ZAP, e.getMessage()).queue();
            log.warn("Missing i18n key. Check this. ID: {}", id, e);
        } catch (IllegalArgumentException e) { //NumberFormatException == IllegalArgumentException
            String id = Snow64.toSnow64(event.getMessage().getIdLong());
            event.getChannel().sendMessageFormat("%sI think you forgot something on the floor. (Maybe we threw it there? Just in case, the error id is `%s`)\n" +
                    "%sCould be an internal error, but check the command arguments or maybe the message I'm trying to send exceeds 2048 characters, Just in case, check command help! " +
                    "(Support server link can be found at `support.mantaro.site`)", EmoteReference.ERROR, id, EmoteReference.WARNING).queue();
            log.warn("Exception caught and alternate message sent. We should look into this, anyway (ID: {})", id, e);
        } catch (ReqlError e) {
            //So much just went wrong...
            e.printStackTrace();
            SentryHelper.captureExceptionContext("Something seems to have broken in the db! Check this out!", e, this.getClass(), "Database");
        } catch (Exception e) {
            I18n context = I18n.of(event.getGuild());

            String id = Snow64.toSnow64(event.getMessage().getIdLong());
            Player player = MantaroData.db().getPlayer(event.getAuthor());
            event.getChannel().sendMessageFormat(
                    "%s%s\n(Error ID: `%s`)\n" + context.get("general.generic_error"), EmoteReference.ERROR, context.get("general.boom_quotes"), id
            ).queue();

            if (player.getData().addBadgeIfAbsent(Badge.FIRE))
                player.saveAsync();

            SentryHelper.captureException(String.format("Unexpected Exception on Command: %s | (Error ID: ``%s``)", event.getMessage().getContentRaw(), id), e, this.getClass());
            log.error("Error happened with id: {} (Error ID: {})", event.getMessage().getContentRaw(), id, e);
        }
    }

    private void processMessage(String level, String message, String channel, GuildMessageReceivedEvent event) {
        TextChannel tc = event.getGuild().getTextChannelById(channel);

        if (tc == null) {
            return;
        }

        if (message.contains("$(")) {
            message = new DynamicModifiers()
                    .mapEvent("", "event", event)
                    .set("level", level)
                    .resolve(message);
        }

        int c = message.indexOf(':');
        if (c != -1) {
            String m = message.substring(0, c);
            String v = message.substring(c + 1);

            if (m.equals("embed")) {
                EmbedJSON embed;
                try {
                    embed = GsonDataManager.gson(false).fromJson('{' + v + '}', EmbedJSON.class);
                } catch (Exception ignored) {
                    tc.sendMessage(EmoteReference.ERROR2 + "The string ``{" + v + "}`` isn't a valid JSON.").queue();
                    return;
                }

                tc.sendMessage(embed.gen(event.getMember())).queue();
                return;
            }
        }

        tc.sendMessage(message).queue();
    }
}
