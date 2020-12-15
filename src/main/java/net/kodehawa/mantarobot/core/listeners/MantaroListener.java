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

package net.kodehawa.mantarobot.core.listeners;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheLoader;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.*;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.http.HttpRequestEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.hooks.EventListener;
import net.kodehawa.mantarobot.ExtraRuntimeOptions;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.BirthdayCmd;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.custom.EmbedJSON;
import net.kodehawa.mantarobot.commands.custom.legacy.DynamicModifiers;
import net.kodehawa.mantarobot.core.MantaroCore;
import net.kodehawa.mantarobot.core.MantaroEventManager;
import net.kodehawa.mantarobot.core.listeners.entities.CachedMessage;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.PremiumKey;
import net.kodehawa.mantarobot.log.LogUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.JsonDataManager;
import net.kodehawa.mantarobot.utils.exporters.Metrics;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

public class MantaroListener implements EventListener {
    private static final Logger LOG = LoggerFactory.getLogger(MantaroListener.class);
    private static final Config CONFIG = MantaroData.config().get();
    private static final ManagedDatabase DATABASE = MantaroData.db();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern MODIFIER_PATTERN = Pattern.compile("\\b\\p{L}*:\\b");
    // Channels we could send the greet message to.
    private static final List<String> CHANNEL_NAMES = List.of("general", "general-chat", "chat", "lounge", "main-chat", "main");

    private final ExecutorService threadPool;
    private final Cache<Long, Optional<CachedMessage>> messageCache;
    private final MantaroBot bot;

    public MantaroListener(ExecutorService threadPool, Cache<Long, Optional<CachedMessage>> messageCache) {
        this.threadPool = threadPool;
        this.messageCache = messageCache;
        bot = MantaroBot.getInstance();
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if (event instanceof ReadyEvent) {
            threadPool.execute(() -> this.updateStats(event.getJDA()));
            return;
        }

        if (event instanceof GuildMessageReceivedEvent) {
            Metrics.RECEIVED_MESSAGES.inc();
            return;
        }

        // !! Member events start
        if (event instanceof GuildMemberJoinEvent) {
            threadPool.execute(() -> onUserJoin((GuildMemberJoinEvent) event));
            return;
        }

        if (event instanceof GuildMemberRemoveEvent) {
            threadPool.execute(() -> onUserLeave((GuildMemberRemoveEvent) event));
            return;
        }

        if (event instanceof GuildMemberRoleAddEvent) {
            handleNewPatron((GuildMemberRoleAddEvent) event);
            return;
        }
        // !! Member events end

        // !! Events needed for the log feature start
        if (event instanceof GuildMessageUpdateEvent) {
            threadPool.execute(() -> logEdit((GuildMessageUpdateEvent) event));
            return;
        }

        if (event instanceof GuildMessageDeleteEvent) {
            threadPool.execute(() -> logDelete((GuildMessageDeleteEvent) event));
            return;
        }

        // After this point we always use this variable.
        final var shardManager = bot.getShardManager();
        if (event instanceof GuildJoinEvent) {
            var joinEvent = (GuildJoinEvent) event;
            var self = joinEvent.getGuild().getSelfMember();
            if (self.getTimeJoined().isBefore(OffsetDateTime.now().minusSeconds(30))) {
                return;
            }

            onJoin(joinEvent);

            if (MantaroCore.hasLoadedCompletely()) {
                Metrics.GUILD_COUNT.set(shardManager.getGuildCache().size());
                Metrics.USER_COUNT.set(shardManager.getUserCache().size());
            }
            return;
        }

        if (event instanceof GuildLeaveEvent) {
            onLeave((GuildLeaveEvent) event);
            if (MantaroCore.hasLoadedCompletely()) {
                Metrics.GUILD_COUNT.set(shardManager.getGuildCache().size());
                Metrics.USER_COUNT.set(shardManager.getUserCache().size());
            }
            return;
        }
        // !! Events needed for the log feature end

        // !! Internal event start
        if (event instanceof StatusChangeEvent) {
            logStatusChange((StatusChangeEvent) event);
            return;
        }

        if (event instanceof DisconnectEvent) {
            Metrics.SHARD_EVENTS.labels("disconnect").inc();
            onDisconnect((DisconnectEvent) event);
            return;
        }

        if (event instanceof ResumedEvent) {
            Metrics.SHARD_EVENTS.labels("resume").inc();
            return;
        }

        if (event instanceof HttpRequestEvent) {
            // We've fucked up big time if we reach this
            final var httpRequestEvent = (HttpRequestEvent) event;
            if (httpRequestEvent.isRateLimit()) {
                LOG.error("!!! Reached 429 on: {}", httpRequestEvent.getRoute());
                Metrics.HTTP_429_REQUESTS.inc();
            }
            Metrics.HTTP_REQUESTS.inc();
        }
        // !! Internal event end
    }

    /**
     * Handles automatic deliver of patreon keys. Should only deliver keys when
     * - An user was already in the guild or just joined and got the "Patreon" role assigned by the Patreon bot
     * - The user hasn't re-joined to get the role re-assigned
     * - The user hasn't received any keys
     * - The user pledged, obviously
     *
     * @param event The event that says that a role got added, obv.
     */
    private void handleNewPatron(GuildMemberRoleAddEvent event) {
        //Only in mantaro's guild...
        if (event.getGuild().getIdLong() == 213468583252983809L && !CONFIG.isPremiumBot()) {
            threadPool.execute(() -> {
                var hasPatronRole = event.getMember().getRoles().stream().anyMatch(r -> r.getId().equals("290257037072531466"));
                // No patron role to be seen here.
                if (!hasPatronRole) {
                    return;
                }

                // We don't need to fetch anything unless the user got a Patron role.
                var user = event.getUser();
                var dbUser = DATABASE.getUser(user);
                var currentKey = DATABASE.getPremiumKey(dbUser.getData().getPremiumKey());

                // Already received key.
                if (dbUser.getData().hasReceivedFirstKey()) {
                    return;
                }

                // They still have a valid key.
                if (currentKey != null && currentKey.validFor() > 20) {
                    return;
                }

                user.openPrivateChannel().queue(channel -> channel.sendMessage(
                        EmoteReference.EYES + "Thanks you for donating, we'll deliver your premium key shortly! :heart:"
                ).queue(message -> {
                    message.editMessage(
                            """
                            %1$sYou received a premium key due to your donation to Mantaro. 
                            If you have any doubts or questions, please contact Kodehawa#3457 or ask in the support server.
                            
                            Instructions: **Apply this key to yourself!**. This key is a subscription to Mantaro Premium, and will last as long as you pledge.
                            If you want more keys (>$2 donation) or want to enable the patreon bot (>$4 donation) you need to contact Kodehawa to deliver your keys.
                            To apply this key, run the following command in any channel where Mantaro can reply: `~>activatekey %2$s`
                            
                            Thanks you so much for pledging and helping to keep Mantaro alive and well :heart:
                            You should now see a #donators channel in Mantaro Hub. Thanks again for your help!
                            """.formatted(
                                    EmoteReference.POPPER, PremiumKey.generatePremiumKey(user.getId(), PremiumKey.Type.USER, false).getId()
                            )
                    ).queue(sent -> {
                                dbUser.getData().setHasReceivedFirstKey(true);
                                dbUser.saveUpdating();
                            }
                    );

                    Metrics.PATRON_COUNTER.inc();
                    //Celebrate internally! \ o /
                    LogUtils.log("Delivered premium key to " + user.getAsTag() + "(" + user.getId() + ")");
                }));
            });
        }
    }

    private void logDelete(GuildMessageDeleteEvent event) {
        try {
            final var dbGuild = MantaroData.db().getGuild(event.getGuild());
            final var data = dbGuild.getData();
            final var logChannel = data.getGuildLogChannel();

            if (logChannel != null) {
                final var hour = Utils.formatHours(OffsetDateTime.now(), data.getLang());
                final var tc = event.getGuild().getTextChannelById(logChannel);
                if (tc == null) {
                    return;
                }

                final var deletedMessage = messageCache.get(event.getMessageIdLong(), Optional::empty).orElse(null);
                if (deletedMessage == null) {
                    return;
                }

                final var selfUser = event.getJDA().getSelfUser();
                final var textChannel = event.getChannel();
                final var content = deletedMessage.getContent();
                final var author = deletedMessage.getAuthor();
                final var authorId = author.getId();
                if (content.isEmpty() || textChannel.getId().equals(logChannel) || authorId.equals(selfUser.getId())) {
                    return;
                }

                if (data.getModlogBlacklistedPeople().contains(authorId)) {
                    return;
                }

                if (data.getLogExcludedChannels().contains(textChannel.getId())) {
                    return;
                }

                if (!data.getModLogBlacklistWords().isEmpty()) {
                    // This is not efficient at all I'm pretty sure, is there a better way?
                    List<String> splitMessage = Arrays.asList(content.split("\\s+"));
                    if (data.getModLogBlacklistWords().stream().anyMatch(splitMessage::contains)) {
                        return;
                    }
                }

                String message;
                if (data.getDeleteMessageLog() != null) {
                    message = new DynamicModifiers()
                            .set("hour", hour)
                            .set("content", content.replace("```", ""))
                            .mapEvent("event", event)
                            .mapChannel("event.channel", textChannel)
                            .mapUser("event.user", author)
                            .set("event.message.id", event.getMessageId())
                            .resolve(data.getDeleteMessageLog());
                } else {
                    message = String.format(EmoteReference.WARNING +
                                    "`[%s]` Message (ID: %s) created by **%s#%s** (ID: %s) in channel **%s** was deleted.\n" +
                                    "```diff\n-%s```",
                            hour, event.getMessageId(), author.getName(), author.getDiscriminator(),
                            authorId, textChannel.getName(), content.replace("```", "")
                    );
                }

                tc.sendMessage(message).queue();
            }
        } catch (NullPointerException | IllegalArgumentException |
                CacheLoader.InvalidCacheLoadException | PermissionException | ErrorResponseException ignored) {
            // ignore
        } catch (Exception e) {
            LOG.warn("Unexpected error while logging a deleted message.", e);
        }
    }

    private void logEdit(GuildMessageUpdateEvent event) {
        try {
            final var guildData = MantaroData.db().getGuild(event.getGuild()).getData();
            final var logChannel = guildData.getGuildLogChannel();

            if (logChannel != null) {
                final var hour = Utils.formatHours(OffsetDateTime.now(), guildData.getLang());
                final var tc = event.getGuild().getTextChannelById(logChannel);
                if (tc == null) {
                    return;
                }

                final var originalMessage = event.getMessage();
                final var editedMessage = messageCache.get(originalMessage.getIdLong(), Optional::empty).orElse(null);
                if (editedMessage == null) {
                    return;
                }

                final var selfUser = event.getJDA().getSelfUser();
                final var channel = event.getChannel();
                final var content = editedMessage.getContent();
                final var author = editedMessage.getAuthor();
                if (content.isEmpty() || channel.getId().equals(logChannel) || author.getId().equals(selfUser.getId())) {
                    return;
                }

                // Update message in cache in any case.
                messageCache.put(originalMessage.getIdLong(), Optional.of(
                        new CachedMessage(event.getGuild().getIdLong(), event.getAuthor().getIdLong(), originalMessage.getContentDisplay()))
                );

                if (guildData.getLogExcludedChannels().contains(channel.getId())) {
                    return;
                }

                if (guildData.getModlogBlacklistedPeople().contains(author.getId())) {
                    return;
                }

                // Don't log if content is equal but update in cache (cc: message is still relevant).
                if (originalMessage.getContentDisplay().equals(content)) {
                    return;
                }

                if (!guildData.getModLogBlacklistWords().isEmpty()) {
                    // This is not efficient at all I'm pretty sure, is there a better way?
                    List<String> splitMessage = Arrays.asList(content.split("\\s+"));
                    if (guildData.getModLogBlacklistWords().stream().anyMatch(splitMessage::contains)) {
                        return;
                    }
                }

                String message;
                if (guildData.getEditMessageLog() != null) {
                    message = new DynamicModifiers()
                            .set("hour", hour)
                            .set("old", content.replace("```", ""))
                            .set("new", originalMessage.getContentDisplay().replace("```", ""))
                            .mapEvent("event", event)
                            .mapChannel("event.channel", channel)
                            .mapUser("event.user", author)
                            .mapMessage("event.message", originalMessage)
                            .resolve(guildData.getEditMessageLog());
                } else {
                    message = String.format(EmoteReference.WARNING +
                                    "`[%s]` Message (ID: %s) created by **%s#%s** in channel **%s** was modified." +
                                    "\n```diff\n-%s\n+%s```",
                            hour, originalMessage.getId(), author.getName(), author.getDiscriminator(),
                            channel.getName(), content.replace("```", ""),
                            originalMessage.getContentDisplay().replace("```", "")
                    );
                }

                tc.sendMessage(message).queue();
            }
        } catch (NullPointerException | IllegalArgumentException |
                CacheLoader.InvalidCacheLoadException | PermissionException | ErrorResponseException ignored) {
            // ignore
        } catch (Exception e) {
            LOG.warn("Unexpected error while logging a edit.", e);
        }
    }

    private void logStatusChange(StatusChangeEvent event) {
        var shardId = event.getJDA().getShardInfo().getShardId();

        if (ExtraRuntimeOptions.VERBOSE_SHARD_LOGS || ExtraRuntimeOptions.VERBOSE) {
            LOG.info("Shard #{}: Changed from {} to {}", shardId, event.getOldStatus(), event.getNewStatus());
        } else {
            // Very janky solution lol.
            if (event.getNewStatus().ordinal() > JDA.Status.LOADING_SUBSYSTEMS.ordinal()) {
                LOG.info("Shard #{}: {}", shardId, event.getNewStatus());
            } else {
                LOG.debug("Shard #{}: Changed from {} to {}", shardId, event.getOldStatus(), event.getNewStatus());
            }
        }
    }

    private void onDisconnect(DisconnectEvent event) {
        if (event.isClosedByServer()) {
            final var clientCloseFrame = event.getClientCloseFrame();
            if (clientCloseFrame == null) {
                LOG.warn("!! SHARD DISCONNECT [SERVER] CODE: [null close frame], disconnected with code {}",
                        event.getCloseCode());
            } else {
                LOG.warn("!! SHARD DISCONNECT [SERVER] CODE: [%d] %s%n"
                        .formatted(clientCloseFrame.getCloseCode(), event.getCloseCode()));
            }
        } else {
            final var clientCloseFrame = event.getClientCloseFrame();
            if (clientCloseFrame == null) {
                LOG.warn("!! SHARD DISCONNECT [CLIENT] CODE: [null close frame?]");
            } else {
                LOG.warn("!! SHARD DISCONNECT [CLIENT] CODE: [%d] %s%n"
                        .formatted(clientCloseFrame.getCloseCode(), clientCloseFrame.getCloseReason()));
            }
        }
    }

    private void onJoin(GuildJoinEvent event) {
        final var guild = event.getGuild();
        final var jda = event.getJDA();
        // Post bot statistics to the main API.
        this.updateStats(jda);
        Metrics.GUILD_ACTIONS.labels("join").inc();

        try {
            // Don't send greet message for MP. Not necessary.
            if (!CONFIG.isPremiumBot()) {
                final var embedBuilder = new EmbedBuilder()
                        .setThumbnail(jda.getSelfUser().getEffectiveAvatarUrl())
                        .setColor(Color.PINK)
                        .setDescription("""
                                Welcome to **Mantaro**, a fun, quirky and complete Discord bot! Thanks for adding me to your server, I highly appreciate it <3
                                We have music, currency (money/economy), games and way more stuff you can check out!
                                Make sure you use the `~>help` command to make yourself comfy and to get started with the bot!

                                If you're interested in supporting Mantaro, check out our Patreon page below, it'll greatly help to improve the bot. 
                                Check out the links below for some help resources and quick start guides.
                                This message will only be shown once.""")
                        .addField("Important Links",
                        """
                                [Support Server](https://support.mantaro.site) - The place to check if you're lost or if there's an issue with the bot.
                                [Official Wiki](https://github.com/Mantaro/MantaroBot/wiki/) - Good place to check if you're lost.
                                [Custom Commands](https://github.com/Mantaro/MantaroBot/wiki/Custom-Command-%22v3%22) - Great customizability for your server needs!
                                [Currency Guide](https://github.com/Mantaro/MantaroBot/wiki/Currency-101) - A lot of fun to be had!
                                [Configuration](https://github.com/Mantaro/MantaroBot/wiki/Configuration) -  Customizability for your server needs!
                                [Patreon](https://patreon.com/mantaro) - Help Mantaro's development directly by donating a small amount of money each month.
                                [Official Website](https://mantaro.site) - A cool website.""",
                                true
                        ).setFooter("We hope you enjoy using Mantaro! For any questions, go to our support server.");

                final var dbGuild = DATABASE.getGuild(guild);
                final var guildData = dbGuild.getData();
                final var guildChannels = guild.getChannels();

                // Find a suitable channel to greeet send the message to.
                guildChannels.stream().filter(
                        channel -> channel.getType() == ChannelType.TEXT &&
                        CHANNEL_NAMES.contains(channel.getName())
                ).findFirst().ifPresentOrElse(ch -> {
                    var channel = (TextChannel) ch;
                    if (channel.canTalk() && !guildData.hasReceivedGreet()) {
                        channel.sendMessage(embedBuilder.build()).queue();
                        guildData.setHasReceivedGreet(true);
                        dbGuild.save();
                    }
                }, () -> {
                    // Attempt to find the first channel we can talk to.
                    var channel = (TextChannel) guildChannels.stream()
                            .filter(guildChannel -> guildChannel.getType() == ChannelType.TEXT && ((TextChannel) guildChannel).canTalk())
                            .findFirst()
                            .orElse(null);

                    // Basically same code as above, but w/e.
                    if (channel != null && !guildData.hasReceivedGreet()) {
                        channel.sendMessage(embedBuilder.build()).queue();
                        guildData.setHasReceivedGreet(true);
                        dbGuild.save();
                    }
                });
            }
        } catch (InsufficientPermissionException | NullPointerException | IllegalArgumentException ignored) {
            // We don't need to catch those
        } catch (Exception e) {
            LOG.error("Unexpected error while processing a join event", e);
        }
    }

    private void onLeave(GuildLeaveEvent event) {
        try {
            final var jda = event.getJDA();
            final var guild = event.getGuild();
            final var guildBirthdayCache = BirthdayCmd.getGuildBirthdayCache();
            final var manager = bot.getAudioManager().getMusicManagers().get(guild.getId());

            // Clear internal data we don't need anymore.
            guild.getTextChannelCache().stream().forEach(TextChannelGround::delete);
            guildBirthdayCache.invalidate(guild.getId());
            guildBirthdayCache.cleanUp();

            // Clean the internal music data.
            if (manager != null) {
                manager.getLavaLink().destroy();
                bot.getAudioManager().getMusicManagers().remove(guild.getId());
            }

            // Post bot statistics to the main API.
            this.updateStats(jda);
            Metrics.GUILD_ACTIONS.labels("leave").inc();
        } catch (NullPointerException | IllegalArgumentException ignored) {
            // ignore
        } catch (Exception e) {
            LOG.error("Unexpected error while processing a leave event", e);
        }
    }

    private void onUserJoin(GuildMemberJoinEvent event) {
        final var guild = event.getGuild();
        final var dbGuild = MantaroData.db().getGuild(guild);
        final var guildData = dbGuild.getData();
        final var role = guildData.getGuildAutoRole();
        final var hour = Utils.formatHours(OffsetDateTime.now(), guildData.getLang());
        final var user = event.getUser();
        final var member = event.getMember();

        if (role != null &&  !(user.isBot() && guildData.isIgnoreBotsAutoRole())) {
            var toAssign = guild.getRoleById(role);
            if (toAssign != null && guild.getSelfMember().canInteract(toAssign)) {
                // This only throws if member == null (can't be!) or if role == null
                // which we check above.
                guild.addRoleToMember(member, toAssign).reason("Autorole assigner").queue();
                Metrics.ACTIONS.labels("join_autorole").inc();
            }
        }

        final var logChannel = guildData.getGuildLogChannel();
        if (logChannel != null) {
            var tc = guild.getTextChannelById(logChannel);
            if (tc != null && tc.canTalk()) {
                tc.sendMessage(String.format("`[%s]` \uD83D\uDCE3 `%s#%s` just joined `%s` `(ID: %s)`",
                        hour, event.getUser().getName(), event.getUser().getDiscriminator(),
                        guild.getName(), event.getUser().getId())
                ).queue();
            }
        }

        if (user.isBot() && guildData.isIgnoreBotsWelcomeMessage()) {
            return;
        }

        try {
            var joinChannel = guildData.getLogJoinChannel();
            if (joinChannel == null || guild.getTextChannelById(joinChannel) == null) {
                joinChannel = guildData.getLogJoinLeaveChannel();
            }

            if (joinChannel == null) {
                return;
            }

            final var joinMessage = guildData.getJoinMessage();
            sendJoinLeaveMessage(event.getUser(), guild, guild.getTextChannelById(joinChannel), guildData.getExtraJoinMessages(), joinMessage);
            Metrics.ACTIONS.labels("join_messages").inc();
        } catch (Exception e) {
            LOG.error("Failed to send join message!", e);
        }
    }

    private void onUserLeave(GuildMemberRemoveEvent event) {
        final var guild = event.getGuild();
        final var user = event.getUser();
        final var dbGuild = MantaroData.db().getGuild(guild);
        final var guildData = dbGuild.getData();

        try {
            final var hour = Utils.formatHours(OffsetDateTime.now(), guildData.getLang());
            if (user.isBot() && guildData.isIgnoreBotsWelcomeMessage()) {
                return;
            }

            var logChannel = guildData.getGuildLogChannel();
            if (logChannel != null) {
                final var tc = guild.getTextChannelById(logChannel);
                if (tc != null && tc.canTalk()) {
                    tc.sendMessage(String.format(
                            "`[%s]` \uD83D\uDCE3 `%s#%s` just left `%s` `(ID: %s)`",
                            hour, user.getName(), user.getDiscriminator(),
                            guild.getName(), user.getId())
                    ).queue();
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to process log leave message!", e);
        }

        try {
            if (user.isBot() && guildData.isIgnoreBotsWelcomeMessage()) {
                return;
            }

            var leaveChannel = guildData.getLogLeaveChannel();
            if (leaveChannel == null || guild.getTextChannelById(leaveChannel) == null) {
                leaveChannel = guildData.getLogJoinLeaveChannel();
            }

            if (leaveChannel == null) {
                return;
            }

            final var leaveMessage = guildData.getLeaveMessage();
            sendJoinLeaveMessage(user, guild, guild.getTextChannelById(leaveChannel), guildData.getExtraLeaveMessages(), leaveMessage);
            Metrics.ACTIONS.labels("leave_messages").inc();
        } catch (Exception e) {
            LOG.error("Failed to send leave message!", e);
        }

        var allowedBirthdays = guildData.getAllowedBirthdays();
        if (allowedBirthdays.contains(user.getId())) {
            allowedBirthdays.remove(user.getId());
            dbGuild.saveAsync();

            var bdCacheMap = BirthdayCmd.getGuildBirthdayCache().getIfPresent(guild.getId());
            if (bdCacheMap != null) {
                bdCacheMap.remove(user.getId());
            }
        }
    }

    private void sendJoinLeaveMessage(User user, Guild guild, TextChannel tc, List<String> extraMessages, String msg) {
        var select = extraMessages.isEmpty() ? 0 : RANDOM.nextInt(extraMessages.size());
        var message = RANDOM.nextBoolean() ? msg : extraMessages.isEmpty() ? msg : extraMessages.get(select);

        if (tc != null && message != null) {
            if (!tc.canTalk()) {
                return;
            }

            if (message.contains("$(")) {
                message = new DynamicModifiers()
                        .mapFromJoinLeave("event", tc, user, guild)
                        .resolve(message);
            }

            var modIndex = message.indexOf(':');
            if (modIndex != -1) {
                // Wonky?
                var matcher = MODIFIER_PATTERN.matcher(message);
                var modifier = "none";
                // Find the first occurrence of a modifier (word:)
                if (matcher.find()) {
                    modifier = matcher.group().replace(":", "");
                }

                var json = message.substring(modIndex + 1);
                var extra = "";

                // Somehow (?) this fails sometimes? I really dunno how, but sure.
                try {
                    extra = message.substring(0, modIndex - modifier.length()).trim();
                } catch (Exception ignored) { }

                try {
                    if (modifier.equals("embed")) {
                        EmbedJSON embed;
                        try {
                            embed = JsonDataManager.fromJson('{' + json + '}', EmbedJSON.class);
                        } catch (Exception e) {
                            tc.sendMessage(EmoteReference.ERROR2 +
                                    "The string\n```json\n{" + json + "}```\nIs not a valid JSON (failed to Convert to EmbedJSON).").queue();
                            e.printStackTrace();
                            return;
                        }

                        var builder = new MessageBuilder().setEmbed(embed.gen(null));
                        if (!extra.isEmpty()) {
                            builder.append(extra);
                        }

                        tc.sendMessage(builder.build())
                                // Allow role mentions here, per popular request :P
                                .allowedMentions(EnumSet.of(Message.MentionType.USER, Message.MentionType.ROLE))
                                .queue(success -> { }, error -> tc.sendMessage("Failed to send join/leave message.").queue()
                        );

                        return;
                    }
                } catch (Exception e) {
                    if (e.getMessage().toLowerCase().contains("url must be a valid")) {
                        tc.sendMessage("Failed to send join/leave message: Wrong image URL in thumbnail, image, footer and/or author.").queue();
                    } else {
                        tc.sendMessage("Failed to send join/leave message: Unknown error, try checking your message.").queue();
                        e.printStackTrace();
                    }
                }
            }

            tc.sendMessage(message)
                    .allowedMentions(EnumSet.of(Message.MentionType.USER, Message.MentionType.ROLE))
                    .queue(success -> { }, failure -> tc.sendMessage("Failed to send join/leave message.").queue());
        }
    }

    private void updateStats(JDA jda) {
        // This screws up with our shard stats, so we just need to ignore it.
        if (jda.getStatus() == JDA.Status.INITIALIZED) {
            return;
        }

        try(var jedis = MantaroData.getDefaultJedisPool().getResource()) {
            var json = new JSONObject()
                    .put("guild_count", jda.getGuildCache().size())
                    .put("cached_users", jda.getUserCache().size())
                    .put("gateway_ping", jda.getGatewayPing())
                    .put("shard_status", jda.getStatus())
                    .put("last_ping_diff", ((MantaroEventManager) jda.getEventManager()).lastJDAEventDiff())
                    .put("node_number", bot.getNodeNumber())
                    .toString();

            jedis.hset("shardstats-" + CONFIG.getClientId(), String.valueOf(jda.getShardInfo().getShardId()), json);
            LOG.debug("Sent process shard stats to redis -> {}", json);
        }
    }
}
