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

package net.kodehawa.mantarobot.core.listeners.command;

import com.google.common.cache.Cache;
import com.rethinkdb.gen.exc.ReqlError;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.hooks.EventListener;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.command.processor.CommandProcessor;
import net.kodehawa.mantarobot.core.listeners.entities.CachedMessage;
import net.kodehawa.mantarobot.data.I18n;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.LanguageKeyNotFoundException;
import net.kodehawa.mantarobot.utils.Snow64;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.IllegalFormatException;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

public class CommandListener implements EventListener {
    private static final Logger log = LoggerFactory.getLogger(CommandListener.class);
    // Commands ran this session.
    private static int commandTotal = 0;
    private final CommandProcessor commandProcessor;
    private final ExecutorService threadPool;
    private final Cache<Long, Optional<CachedMessage>> messageCache;

    public CommandListener(CommandProcessor processor, ExecutorService threadPool, Cache<Long, Optional<CachedMessage>> messageCache) {
        this.commandProcessor = processor;
        this.threadPool = threadPool;
        this.messageCache = messageCache;
    }

    public static int getCommandTotal() {
        return commandTotal;
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (event instanceof MessageReceivedEvent msg) {
            if(!msg.isFromGuild()) {
                return;
            }
            
            // Ignore myself and bots.
            // Technically ignoring oneself is an extra step -- we're a bot, and we ignore bots.
            var isSelf = msg.getAuthor().getIdLong() == msg.getJDA().getSelfUser().getIdLong();
            if (msg.getAuthor().isBot() || msg.isWebhookMessage() || isSelf) {
                return;
            }

            // Inserts a cached message into the cache. This only holds the id and the content, and is way lighter than saving the entire jda object.
            messageCache.put(msg.getMessage().getIdLong(), Optional.of(
                    new CachedMessage(msg.getGuild().getIdLong(), msg.getAuthor().getIdLong(), msg.getMessage().getContentDisplay()))
            );

            // We can't talk here, so we don't need to run anything.
            // Run this check before executing on the pool to avoid wasting a thread.
            try {
                if (!msg.getChannel().canTalk()) {
                    return;
                }
            } catch (NullPointerException npe) { // For some reason we fail to get the permission container, assume we can't.
                return;
            } // TODO: remove when they add forums.

            threadPool.execute(() -> onCommand(msg));
        }

        if (event instanceof SlashCommandInteractionEvent) {
            threadPool.execute(() -> onSlash(((SlashCommandInteractionEvent) event)));
        }

        if (event instanceof UserContextInteractionEvent) {
            threadPool.execute(() -> onUserContext(((UserContextInteractionEvent) event)));
        }
    }

    private void onSlash(SlashCommandInteractionEvent event) {
        try {
            if (commandProcessor.runSlash(event)) {
                // Remove running flag
                try (var jedis = MantaroData.getDefaultJedisPool().getResource()) {
                    jedis.del("commands-running-" + event.getUser().getId());
                }

                commandTotal++;
            }
        } catch (ReqlError e) {
            // So much just went wrong...
            e.printStackTrace();
        } catch (CompletionException e) {
            log.error("Missed interaction ack time?", e);
        } catch (LanguageKeyNotFoundException e) {
            var id = Snow64.toSnow64(event.getIdLong());
            log.warn("Missing i18n key. Check this. ID: {}", id, e);

            sendSlashError(event,"%sWrong language key found, please report on the support server (At <https://support.mantaro.site>) with error ID `%s` (On shard %s).\n%sMessage: *%s*",
                    EmoteReference.ERROR, id, event.getJDA().getShardInfo().getShardId(), EmoteReference.ZAP, e.getMessage());
        } catch (IllegalFormatException e) {
            var id = Snow64.toSnow64(event.getIdLong());
            log.warn("Wrong String format. Check this. ID: {}", id, e);

            sendSlashError(event, "%sWe found at error when trying to format a String. Please report on the support server (At <https://support.mantaro.site>) with error ID `%s` (On Shard %s)",
                    EmoteReference.ERROR, id, event.getJDA().getShardInfo().getShardId());
        } catch (PermissionException e) {
            log.warn("Caught unexpected Permission issue?", e);
            if (e.getPermission() != Permission.UNKNOWN) {
                sendSlashError(event, "%sI don't have permission to do this :(\nI need the permission: **%s**", EmoteReference.ERROR, e.getPermission().getName());
            } else {
                sendSlashError(event, "I cannot perform this action due to the lack of permission! " +
                        "Is the role I might be trying to assign higher than my role? Do I have the correct permissions/hierarchy to perform this action?");
            }
        } catch (Exception e) {
            var id = Snow64.toSnow64(event.getIdLong());
            if (event.getGuild() == null) {
                log.error("Error happened on command: {} (Error ID: {}). Guild ID is null?", event.getCommandString(), id, e);
                return;
            }

            log.error("Error happened on command: {} (Error ID: {})", event.getCommandString(), id, e);
            var context = I18n.of(event.getGuild());
            sendSlashError(event, "%s%s (Unexpected error, ID: `%s`): Shard %s\n%s",
                    EmoteReference.ERROR, context.get("general.boom_quotes"), id,
                    event.getJDA().getShardInfo().getShardId(), context.get("general.generic_error")
            );
        }
    }

    private void onUserContext(UserContextInteractionEvent event) {
        try {
            if (commandProcessor.runContextUser(event)) {
                // Remove running flag
                try (var jedis = MantaroData.getDefaultJedisPool().getResource()) {
                    jedis.del("commands-running-" + event.getUser().getId());
                }

                commandTotal++;
            }
        } catch (ReqlError e) {
            // So much just went wrong...
            e.printStackTrace();
        } catch (CompletionException e) {
            log.error("Missed interaction ack time?", e);
        } catch (LanguageKeyNotFoundException e) {
            var id = Snow64.toSnow64(event.getIdLong());
            log.warn("Missing i18n key. Check this. ID: {}", id, e);
            sendSlashError(event,"%sWrong language key found, please report on the support server (At <https://support.mantaro.site>) with error ID `%s` (On shard %s).\n%sMessage: *%s*",
                    EmoteReference.ERROR, id, event.getJDA().getShardInfo().getShardId(), EmoteReference.ZAP, e.getMessage());
        } catch (IllegalFormatException e) {
            var id = Snow64.toSnow64(event.getIdLong());
            log.warn("Wrong String format. Check this. ID: {}", id, e);
            sendSlashError(event, "%sWe found at error when trying to format a String. Please report on the support server (At <https://support.mantaro.site>) with error ID `%s` (On Shard %s)",
                    EmoteReference.ERROR, id, event.getJDA().getShardInfo().getShardId());
        } catch (PermissionException e) {
            log.warn("Caught unexpected Permission issue?", e);
            if (e.getPermission() != Permission.UNKNOWN) {
                sendSlashError(event, "%sI don't have permission to do this :(\nI need the permission: **%s**", EmoteReference.ERROR, e.getPermission().getName());
            } else {
                sendSlashError(event, "I cannot perform this action due to the lack of permission! " +
                        "Is the role I might be trying to assign higher than my role? Do I have the correct permissions/hierarchy to perform this action?");
            }
        } catch (Exception e) {
            var id = Snow64.toSnow64(event.getIdLong());
            if (event.getGuild() == null) {
                log.error("Error happened on command: {} (Error ID: {}). Guild ID is null?", event.getCommandString(), id, e);
                return;
            }

            log.error("Error happened on command: {} (Error ID: {})", event.getCommandString(), id, e);
            var context = I18n.of(event.getGuild());
            sendSlashError(event, "%s%s (Unexpected error, ID: `%s`): Shard %s\n%s",
                    EmoteReference.ERROR, context.get("general.boom_quotes"), id,
                    event.getJDA().getShardInfo().getShardId(), context.get("general.generic_error")
            );
        }
    }

    private void onCommand(MessageReceivedEvent event) {
        try {
            if (commandProcessor.run(event)) {
                // Remove running flag
                try (var jedis = MantaroData.getDefaultJedisPool().getResource()) {
                    jedis.del("commands-running-" + event.getAuthor().getId());
                }

                commandTotal++;
            }
        } catch (IllegalFormatException e) {
            var id = Snow64.toSnow64(event.getMessage().getIdLong());
            event.getChannel().sendMessageFormat(
                    "%sWe found at error when trying to format a String. Please report on the support server (At <https://support.mantaro.site>) with error ID `%s` (On Shard %s)",
                    EmoteReference.ERROR, id, event.getJDA().getShardInfo().getShardId()
            ).queue();

            log.warn("Wrong String format. Check this. ID: {}", id, e);
        } catch (IndexOutOfBoundsException e) {
            var id = Snow64.toSnow64(event.getMessage().getIdLong());
            event.getChannel().sendMessageFormat(
                    "%sYour query returned no results or you used the incorrect arguments, seemingly (Error ID: `%s`): Shard %s. Just in case, check command help!",
                    EmoteReference.ERROR, id, event.getJDA().getShardInfo().getShardId()
            ).queue();

            log.warn("Exception caught and alternate message sent. We should look into this, anyway (ID: {})", id, e);
        } catch (PermissionException e) {
            if (e.getPermission() != Permission.UNKNOWN) {
                event.getChannel().sendMessageFormat(
                        "%sI don't have permission to do this :(\nI need the permission: **%s**",
                        EmoteReference.ERROR, e.getPermission().getName()
                ).queue();
            } else {
                event.getChannel().sendMessage(
                        EmoteReference.ERROR +
                        "I cannot perform this action due to the lack of permission! Is the role I might be trying to assign " +
                        "higher than my role? Do I have the correct permissions/hierarchy to perform this action?"
                ).queue();
            }
        } catch (LanguageKeyNotFoundException e) {
            var id = Snow64.toSnow64(event.getMessage().getIdLong());
            event.getChannel().sendMessageFormat(
                    "%sWrong I18n key found, please report on the support server (At <https://support.mantaro.site>) with error ID `%s` (On Shard %s).\n%sMessage: *%s*",
                    EmoteReference.ERROR, id, event.getJDA().getShardInfo().getShardId(),
                    EmoteReference.ZAP, e.getMessage()
            ).queue();

            log.warn("Missing i18n key. Check this. ID: {}", id, e);
        } catch (IllegalArgumentException e) { //NumberFormatException == IllegalArgumentException
            var id = Snow64.toSnow64(event.getMessage().getIdLong());
            event.getChannel().sendMessageFormat(
                    "%sI think you forgot something on the floor. (Error ID: `%s`): Shard %s\n" +
                    "%sCould be an internal error, but check the command arguments or maybe the message I'm trying to send exceeds 2048 characters, " +
                    "Just in case, check command help! (If you need further help, go to <https://support.mantaro.site>)",
                    EmoteReference.ERROR, id, event.getJDA().getShardInfo().getShardId(), EmoteReference.WARNING
            ).queue();

            log.warn("Exception caught and alternate message sent. We should look into this, anyway (ID: {})", id, e);
        } catch (ReqlError e) {
            // So much just went wrong...
            e.printStackTrace();
        } catch (Exception e) {
            var context = I18n.of(event.getGuild());
            var id = Snow64.toSnow64(event.getMessage().getIdLong());
            var player = MantaroData.db().getPlayer(event.getAuthor());

            event.getChannel().sendMessageFormat(
                    "%s%s (Unexpected error, ID: `%s`): Shard %s\n%s",
                    EmoteReference.ERROR, context.get("general.boom_quotes"), id,
                    event.getJDA().getShardInfo().getShardId(), context.get("general.generic_error")
            ).queue();

            if (player.getData().addBadgeIfAbsent(Badge.FIRE)) {
                player.saveUpdating();
            }

            log.error("Error happened on command: {} (Error ID: {})", event.getMessage().getContentRaw(), id, e);
        }
    }

    private void sendSlashError(GenericCommandInteractionEvent event, String message, Object... args) {
        if (event.isAcknowledged()) {
            event.getHook().sendMessage(String.format(message, args)).queue();
        } else {
            event.reply(String.format(message, args)).setEphemeral(true).queue();
        }
    }
}
