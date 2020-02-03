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

package net.kodehawa.mantarobot.core.listeners;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.DisconnectEvent;
import net.dv8tion.jda.api.events.ExceptionEvent;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ResumedEvent;
import net.dv8tion.jda.api.events.StatusChangeEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.GenericGuildMemberEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.http.HttpRequestEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.hooks.EventListener;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.custom.EmbedJSON;
import net.kodehawa.mantarobot.commands.custom.legacy.DynamicModifiers;
import net.kodehawa.mantarobot.commands.info.stats.manager.GuildStatsManager;
import net.kodehawa.mantarobot.commands.info.stats.manager.GuildStatsManager.LoggedEvent;
import net.kodehawa.mantarobot.core.MantaroCore;
import net.kodehawa.mantarobot.core.MantaroEventManager;
import net.kodehawa.mantarobot.core.listeners.entities.CachedMessage;
import net.kodehawa.mantarobot.core.listeners.events.ShardMonitorEvent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.MantaroObj;
import net.kodehawa.mantarobot.db.entities.PremiumKey;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.log.LogUtils;
import net.kodehawa.mantarobot.utils.SentryHelper;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.GsonDataManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.kodehawa.mantarobot.utils.Utils.DISCORD_INVITE;
import static net.kodehawa.mantarobot.utils.Utils.DISCORD_INVITE_2;
import static net.kodehawa.mantarobot.utils.Utils.THIRD_PARTY_INVITE;

public class MantaroListener implements EventListener {
    private static final Cache<String, Long> INVITES = CacheBuilder.newBuilder()
                                                               .maximumSize(5500)
                                                               .build();
    
    //START OF METRIC COLLECTORS DECLARATION.
    private static final Gauge guildCount = Gauge.build()
                                                    .name("guilds").help("Guild Count")
                                                    .register();
    private static final Gauge userCount = Gauge.build()
                                                   .name("users").help("User Count")
                                                   .register();
    private static final Counter httpRequests = Counter.build()
                                                        .name("http_requests").help("Successful HTTP Requests (JDA)")
                                                        .register();
    private static final Counter receivedMessages = Counter.build()
                                                            .name("messages_received").help("Received messages (all users + bots)")
                                                            .register();
    private static final Counter actions = Counter.build()
                                                   .name("actions").help("Mantaro Actions")
                                                   .labelNames("type")
                                                   .register();
    private static final Counter shardEvents = Counter.build()
                                                       .name("shard_events").help("Shard Events")
                                                       .labelNames("type")
                                                       .register();
    private static final Counter guildActions = Counter.build()
                                                        .name("guild_actions").help("Guild Options")
                                                        .labelNames("type")
                                                        .register();
    private static final Counter patronCounter = Counter.build()
                                                         .name("patrons").help("New patrons")
                                                         .register();
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(MantaroListener.class);
    //END OF METRIC CONNECTORS DECLARATION.
    
    private static int logTotal = 0;
    private final ManagedDatabase db = MantaroData.db();
    private final DateFormat df = new SimpleDateFormat("HH:mm:ss");
    private final SecureRandom rand = new SecureRandom();
    private final int shardId;
    private final ExecutorService threadPool;
    private final Cache<Long, Optional<CachedMessage>> messageCache;
    
    private Pattern modifierPattern = Pattern.compile("\\b\\p{L}*:\\b");
    
    public MantaroListener(int shardId, ExecutorService threadPool, Cache<Long, Optional<CachedMessage>> messageCache) {
        this.shardId = shardId;
        this.threadPool = threadPool;
        this.messageCache = messageCache;
    }
    
    public static int getLogTotalInt() {
        return logTotal;
    }
    
    private static boolean hasInvite(JDA jda, Guild guild, String message) {
        if(THIRD_PARTY_INVITE.matcher(message).find())
            return true;
        Matcher m = DISCORD_INVITE_2.matcher(message);
        if(!m.find())
            return false;
        
        String invite = m.group(0);
        String code = invite.substring(invite.lastIndexOf('/') + 1).trim();
        try {
            return INVITES.get(code, () -> Invite.resolve(jda, code).complete().getGuild().getIdLong()) != guild.getIdLong();
        } catch(ExecutionException e) {
            log.error("Error running invite validator", e);
            return DISCORD_INVITE.matcher(message).find();
        }
    }
    
    @Override
    public void onEvent(@NotNull GenericEvent event) {
        if(event instanceof ShardMonitorEvent) {
            //we can't use Event#getJDA because ShardMonitorEvent has no JDA instance
            var jda = MantaroBot.getInstance().getShard(shardId).getJDA();
            if(((MantaroEventManager)jda.getEventManager()).getLastJDAEventTimeDiff() > 50000)
                return;
            
            ((ShardMonitorEvent) event).alive(shardId, ShardMonitorEvent.MANTARO_LISTENER);
            return;
        }
        
        if(event instanceof GuildMessageReceivedEvent) {
            receivedMessages.inc();
            GuildMessageReceivedEvent e = (GuildMessageReceivedEvent) event;
            onMessage(e);
            return;
        }
        
        if(event instanceof GuildMemberJoinEvent) {
            threadPool.execute(() -> onUserJoin((GuildMemberJoinEvent) event));
            return;
        }
        
        if(event instanceof GuildMemberLeaveEvent) {
            threadPool.execute(() -> onUserLeave((GuildMemberLeaveEvent) event));
            return;
        }
        
        //Log intensifies
        //Doesn't run on the thread pool as there's no need for it.
        if(event instanceof GuildMemberRoleAddEvent) {
            //It only runs on the thread pool if needed.
            handleNewPatron((GuildMemberRoleAddEvent) event);
        }
        
        if(event instanceof GuildMessageUpdateEvent) {
            logEdit((GuildMessageUpdateEvent) event);
            return;
        }
        
        if(event instanceof GuildMessageDeleteEvent) {
            logDelete((GuildMessageDeleteEvent) event);
            return;
        }
        
        if(event instanceof GuildUnbanEvent) {
            logUnban((GuildUnbanEvent) event);
            return;
        }
        
        if(event instanceof GuildBanEvent) {
            logBan((GuildBanEvent) event);
            return;
        }
        
        //Internal events
        if(event instanceof GuildJoinEvent) {
            GuildJoinEvent e = (GuildJoinEvent) event;
            if(e.getGuild().getSelfMember().getTimeJoined().isBefore(OffsetDateTime.now().minusSeconds(30)))
                return;
            
            onJoin(e);
            
            if(MantaroCore.hasLoadedCompletely()) {
                guildCount.set(MantaroBot.getInstance().getShardManager().getGuildCache().size());
                userCount.set(MantaroBot.getInstance().getShardManager().getUserCache().size());
            }
            
            return;
        }
        
        if(event instanceof GuildLeaveEvent) {
            onLeave((GuildLeaveEvent) event);
            
            //Destroy this link.
            MantaroBot.getInstance().getLavalink().getLink(((GuildLeaveEvent) event).getGuild()).destroy();
            
            if(MantaroCore.hasLoadedCompletely()) {
                guildCount.set(MantaroBot.getInstance().getShardManager().getGuildCache().size());
                userCount.set(MantaroBot.getInstance().getShardManager().getUserCache().size());
            }
            
            return;
        }
        
        //debug
        if(event instanceof StatusChangeEvent) {
            logStatusChange((StatusChangeEvent) event);
            return;
        }
        
        if(event instanceof DisconnectEvent) {
            shardEvents.labels("disconnect").inc();
            onDisconnect((DisconnectEvent) event);
            return;
        }
        
        if(event instanceof ResumedEvent) {
            shardEvents.labels("resume").inc();
            return;
        }
        
        
        if(event instanceof ExceptionEvent) {
            onException((ExceptionEvent) event);
            return;
        }
        
        if(event instanceof HttpRequestEvent) {
            httpRequests.inc();
        }
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
    private void handleNewPatron(GenericGuildMemberEvent event) {
        //Only in mantaro's guild...
        if(event.getGuild().getIdLong() == 213468583252983809L && !MantaroData.config().get().isPremiumBot) {
            threadPool.execute(() -> {
                User user = event.getUser();
                //who...
                DBUser dbUser = db.getUser(user);
                PremiumKey currentKey = MantaroData.db().getPremiumKey(dbUser.getData().getPremiumKey());

                if(event.getMember().getRoles().stream().anyMatch(r -> r.getId().equals("290257037072531466"))) {
                    if(!dbUser.getData().hasReceivedFirstKey() && (currentKey == null || currentKey.validFor() < 20)) {
                        //Attempt to open a PM and send a key!
                        user.openPrivateChannel().queue(channel -> {
                            //Sellout message :^)
                            //TODO: Add claimkey whenever it's active.
                            channel.sendMessage(EmoteReference.EYES + "Thanks you for donating, we'll deliver your premium key shortly! :heart:").queue(message -> {
                                message.editMessage(EmoteReference.POPPER + "You received a premium key due to your donation to mantaro. " +
                                                            "If any doubts, please contact Kodehawa#3457.\n" +
                                                            "Instructions: **Apply this key to yourself!**. " +
                                                            "This key is a 365-day long subscription to Mantaro Premium. If you want more keys (>$2 donation) " +
                                                            "or want to enable the patreon bot (>$4 donation) you need to contact Kodehawa to deliver your keys.\n" +
                                                            "To apply this key, run the following command in any channel `~>activatekey " +
                                                            //will eventually get linked (see DBUser.java, lines 103-107), patreon webhook can be slower and clash with what I need here.
                                                            PremiumKey.generatePremiumKey(user.getId(), PremiumKey.Type.USER, false).getId() + "`\n" +
                                                            "Thanks you soo much for donating and helping to keep Mantaro alive! :heart:").queue(sent -> {
                                            dbUser.getData().setHasReceivedFirstKey(true);
                                            dbUser.saveAsync();
                                        }
                                );
                                
                                patronCounter.inc();
                                //Celebrate internally! \ o /
                                LogUtils.log("Delivered premium key to " + user.getName() + "#" + user.getDiscriminator() + "(" + user.getId() + ")");
                            });
                        }, failure -> LogUtils.log(String.format("User: %s (%s#%s) couldn't receive the key, apply manually when asked!", user.getId(), user.getName(), user.getDiscriminator())));
                    }
                }
            });
        }
    }
    
    private void logBan(GuildBanEvent event) {
        String hour = df.format(new Date(System.currentTimeMillis()));
        GuildData data = MantaroData.db().getGuild(event.getGuild()).getData();
        String logChannel = data.getGuildLogChannel();
        if(logChannel != null) {
            TextChannel tc = event.getGuild().getTextChannelById(logChannel);
            if(tc != null) {
                String message;
                if(data.getBannedMemberLog() != null) {
                    message = new DynamicModifiers()
                                      .set("hour", hour)
                                      .mapEvent("event", event)
                                      .mapUser("event.user", event.getUser())
                                      .resolve(data.getBannedMemberLog());
                } else {
                    message = EmoteReference.WARNING + "`[" + hour + "]` " + event.getUser().getName() + "#" + event.getUser().getDiscriminator() + " just got banned.";
                }
                tc.sendMessage(message).queue();
                logTotal++;
            }
        }
    }
    
    private void logDelete(GuildMessageDeleteEvent event) {
        try {
            String hour = df.format(new Date(System.currentTimeMillis()));
            final ManagedDatabase db = MantaroData.db();
            
            final DBGuild dbGuild = db.getGuild(event.getGuild());
            final GuildData data = dbGuild.getData();
            
            String logChannel = data.getGuildLogChannel();
            if(logChannel != null) {
                TextChannel tc = event.getGuild().getTextChannelById(logChannel);
                if(tc == null)
                    return;
                
                CachedMessage deletedMessage = messageCache.get(event.getMessageIdLong(), Optional::empty).orElse(null);
                if(deletedMessage != null && !deletedMessage.getContent().isEmpty() && !event.getChannel().getId().equals(logChannel) && !deletedMessage.getAuthor().getId().equals(event.getJDA().getSelfUser().getId())) {
                    if(data.getModlogBlacklistedPeople().contains(deletedMessage.getAuthor().getId())) {
                        return;
                    }
                    
                    if(data.getLogExcludedChannels().contains(event.getChannel().getId())) {
                        return;
                    }
                    
                    if(!data.getModLogBlacklistWords().isEmpty()) {
                        //This is not efficient at all I'm pretty sure, is there a better way?
                        List<String> splitMessage = Arrays.asList(deletedMessage.getContent().split("\\s+"));
                        if(data.getModLogBlacklistWords().stream().anyMatch(splitMessage::contains)) {
                            return;
                        }
                    }
                    
                    String message;
                    if(data.getDeleteMessageLog() != null) {
                        message = new DynamicModifiers()
                                          .set("hour", hour)
                                          .set("content", deletedMessage.getContent().replace("```", ""))
                                          .mapEvent("event", event)
                                          .mapChannel("event.channel", event.getChannel())
                                          .mapUser("event.user", deletedMessage.getAuthor())
                                          .set("event.message.id", event.getMessageId())
                                          .resolve(data.getDeleteMessageLog());
                    } else {
                        message = String.format(EmoteReference.WARNING + "`[%s]` Message (ID: %s) created by **%s#%s** (ID: %s) in channel **%s** was deleted.\n" +
                                                        "```diff\n-%s```", hour, event.getMessageId(), deletedMessage.getAuthor().getName(), deletedMessage.getAuthor().getDiscriminator(), deletedMessage.getAuthor().getId(), event.getChannel().getName(), deletedMessage.getContent().replace("```", ""));
                    }
                    
                    logTotal++;
                    tc.sendMessage(message).queue();
                }
            }
        } catch(Exception e) {
            if(!(e instanceof IllegalArgumentException) && !(e instanceof NullPointerException) && !(e instanceof CacheLoader.InvalidCacheLoadException) && !(e instanceof PermissionException)) {
                log.warn("Unexpected exception while logging a deleted message.", e);
            }
        }
    }
    
    private void logEdit(GuildMessageUpdateEvent event) {
        try {
            String hour = df.format(new Date(System.currentTimeMillis()));
            final ManagedDatabase db = MantaroData.db();
            final GuildData guildData = db.getGuild(event.getGuild()).getData();
            String logChannel = guildData.getGuildLogChannel();
            
            if(logChannel != null) {
                TextChannel tc = event.getGuild().getTextChannelById(logChannel);
                if(tc == null)
                    return;
                
                User author = event.getAuthor();
                CachedMessage editedMessage = messageCache.get(event.getMessage().getIdLong(), Optional::empty).orElse(null);
                
                if(editedMessage != null && !editedMessage.getContent().isEmpty() && !event.getChannel().getId().equals(logChannel)) {
                    //Update message in cache in any case.
                    messageCache.put(event.getMessage().getIdLong(), Optional.of(new CachedMessage(event.getAuthor().getIdLong(), event.getMessage().getContentDisplay())));
                    
                    if(guildData.getLogExcludedChannels().contains(event.getChannel().getId())) {
                        return;
                    }
                    
                    if(guildData.getModlogBlacklistedPeople().contains(editedMessage.getAuthor().getId())) {
                        return;
                    }
                    
                    //Don't log if content is equal but update in cache (cc: message is still relevant).
                    if(event.getMessage().getContentDisplay().equals(editedMessage.getContent()))
                        return;
                    
                    if(!guildData.getModLogBlacklistWords().isEmpty()) {
                        //This is not efficient at all I'm pretty sure, is there a better way?
                        List<String> splitMessage = Arrays.asList(editedMessage.getContent().split("\\s+"));
                        if(guildData.getModLogBlacklistWords().stream().anyMatch(splitMessage::contains)) {
                            return;
                        }
                    }
                    
                    String message;
                    if(guildData.getEditMessageLog() != null) {
                        message = new DynamicModifiers()
                                          .set("hour", hour)
                                          .set("old", editedMessage.getContent().replace("```", ""))
                                          .set("new", event.getMessage().getContentDisplay().replace("```", ""))
                                          .mapEvent("event", event)
                                          .mapChannel("event.channel", event.getChannel())
                                          .mapUser("event.user", editedMessage.getAuthor())
                                          .mapMessage("event.message", event.getMessage())
                                          .resolve(guildData.getEditMessageLog());
                    } else {
                        message = String.format(EmoteReference.WARNING + "`[%s]` Message (ID: %s) created by **%s#%s** in channel **%s** was modified.\n```diff\n-%s\n+%s```",
                                hour, event.getMessage().getId(), author.getName(), author.getDiscriminator(), event.getChannel().getName(), editedMessage.getContent().replace("```", ""), event.getMessage().getContentDisplay().replace("```", ""));
                    }
                    
                    tc.sendMessage(message).queue();
                    
                    logTotal++;
                }
            }
        } catch(Exception e) {
            if(!(e instanceof NullPointerException) && !(e instanceof IllegalArgumentException) && !(e instanceof CacheLoader.InvalidCacheLoadException) && !(e instanceof PermissionException)) {
                log.warn("Unexpected error while logging a edit.", e);
            }
        }
    }
    
    private void logStatusChange(StatusChangeEvent event) {
        JDA jda = event.getJDA();
        if(jda.getShardInfo() == null)
            return;
        
        log.info(String.format("Shard #%d: Changed from %s to %s", jda.getShardInfo().getShardId(), event.getOldStatus(), event.getNewStatus()));
    }
    
    private void logUnban(GuildUnbanEvent event) {
        try {
            String hour = df.format(new Date(System.currentTimeMillis()));
            GuildData data = MantaroData.db().getGuild(event.getGuild()).getData();
            String logChannel = data.getGuildLogChannel();
            if(logChannel != null) {
                TextChannel tc = event.getGuild().getTextChannelById(logChannel);
                if(tc != null) {
                    String message;
                    if(data.getUnbannedMemberLog() != null) {
                        message = new DynamicModifiers()
                                          .set("hour", hour)
                                          .mapEvent("event", event)
                                          .mapUser("event.user", event.getUser())
                                          .resolve(data.getUnbannedMemberLog());
                    } else {
                        message = String.format(EmoteReference.WARNING + "`[%s]` %s#%s just got unbanned.", hour, event.getUser().getName(), event.getUser().getDiscriminator());
                    }
                    tc.sendMessage(message).queue();
                    logTotal++;
                }
            }
        } catch(Exception e) {
            if(!(e instanceof NullPointerException) && !(e instanceof IllegalArgumentException)) {
                log.warn("Unexpected error while logging an unban.", e);
            }
        }
    }
    
    private void onDisconnect(DisconnectEvent event) {
        if(event.isClosedByServer()) {
            log.warn(String.format("---- DISCONNECT [SERVER] CODE: [%d] %s%n", event.getServiceCloseFrame().getCloseCode(), event.getCloseCode()));
        } else {
            log.warn(String.format("---- DISCONNECT [CLIENT] CODE: [%d] %s%n", event.getClientCloseFrame().getCloseCode(), event.getClientCloseFrame().getCloseReason()));
        }
    }
    
    private void onException(ExceptionEvent event) {
        if(!event.isLogged()) {
            SentryHelper.captureException("Exception captured in un-logged trace", event.getCause(), this.getClass());
        }
    }
    
    private void onJoin(GuildJoinEvent event) {
        if(event.getGuild() == null) {
            log.info("Got a guild join event with null guild? Shard {}", shardId);
        }
        
        final MantaroObj mantaroData = MantaroData.db().getMantaroData();
        
        try {
            if(mantaroData.getBlackListedGuilds().contains(event.getGuild().getId()) || mantaroData.getBlackListedUsers().contains(event.getGuild().getOwner().getUser().getId())) {
                event.getGuild().leave().queue();
                return;
            }
            
            guildActions.labels("join").inc();
            GuildStatsManager.log(LoggedEvent.JOIN);
        } catch(Exception e) {
            if(!(e instanceof NullPointerException) && !(e instanceof IllegalArgumentException)) {
                SentryHelper.captureException("Unexpected error while logging an event", e, this.getClass());
            }
        }
    }
    
    private void onLeave(GuildLeaveEvent event) {
        try {
            final MantaroObj mantaroData = MantaroData.db().getMantaroData();
            
            if(mantaroData.getBlackListedGuilds().contains(event.getGuild().getId()) || mantaroData.getBlackListedUsers().contains(event.getGuild().getOwner().getUser().getId())) {
                log.info("Left " + event.getGuild() + " because of a blacklist entry. (O:" + event.getGuild().getOwner() + ")");
                return;
            }
            
            guildActions.labels("leave").inc();
            MantaroBot.getInstance().getAudioManager().getMusicManagers().remove(event.getGuild().getId());
            GuildStatsManager.log(LoggedEvent.LEAVE);
        } catch(Exception e) {
            if(!(e instanceof NullPointerException) && !(e instanceof IllegalArgumentException)) {
                SentryHelper.captureException("Unexpected error while logging an event", e, this.getClass());
            }
        }
    }
    
    private void onMessage(GuildMessageReceivedEvent event) {
        if(event.getAuthor().isFake())
            return;
        
        //Moderation features
        DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
        GuildData guildData = dbGuild.getData();
        
        //link protection
        if(guildData.isLinkProtection() && !guildData.getLinkProtectionAllowedChannels().contains(event.getChannel().getId()) &&
                   !guildData.getLinkProtectionAllowedUsers().contains(event.getAuthor().getId())) {
            //Has link protection enabled, let's check if they don't have admin stuff.
            if(event.getMember() != null && !event.getMember().hasPermission(Permission.ADMINISTRATOR) && !event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
                //Check if invite is valid. This is async because hasInvite uses complete sometimes.
                threadPool.execute(() -> {
                    //If this message has an invite and it's not an invite to the same guild it was sent on, proceed to delete.
                    if(hasInvite(event.getJDA(), event.getGuild(), event.getMessage().getContentRaw())) {
                        Member bot = event.getGuild().getSelfMember();
                        actions.labels("link_block").inc();
                        if(bot.hasPermission(event.getChannel(), Permission.MESSAGE_MANAGE) || bot.hasPermission(Permission.ADMINISTRATOR)) {
                            User author = event.getAuthor();
                            
                            //Ignore myself.
                            if(event.getAuthor().getId().equals(event.getJDA().getSelfUser().getId())) {
                                return;
                            }
                            
                            //Ignore log channel.
                            if(guildData.getGuildLogChannel() != null && event.getChannel().getId().equals(guildData.getGuildLogChannel())) {
                                return;
                            }
                            
                            //Yes, I know the check previously done is redundant, but in case someone decides to change the laws of nature, it should do	.
                            event.getMessage().delete().queue();
                            event.getChannel().sendMessage(EmoteReference.ERROR + "**You cannot advertise here.** Deleted invite link sent by **" + author.getName() + "#" + author.getDiscriminator() + "**.").queue();
                        } else {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "I cannot remove the invite link because I don't have permission to delete messages!").queue();
                        }
                    }
                });
            }
        }
    }
    
    private void onUserJoin(GuildMemberJoinEvent event) {
        DBGuild dbg = MantaroData.db().getGuild(event.getGuild());
        GuildData data = dbg.getData();
        final User user = event.getMember().getUser();
        
        try {
            String role = data.getGuildAutoRole();
            
            String hour = df.format(new Date(System.currentTimeMillis()));
            if(role != null) {
                try {
                    if(!(user.isBot() && data.isIgnoreBotsAutoRole())) {
                        Role toAssign = event.getGuild().getRoleById(role);
                        if(toAssign != null) {
                            if(event.getGuild().getSelfMember().canInteract(toAssign)) {
                                event.getGuild().addRoleToMember(event.getMember(), toAssign)
                                        .reason("Autorole assigner.")
                                        .queue(s -> log.debug("Successfully added a new role to " + event.getMember()));
                                
                                actions.labels("join_autorole").inc();
                            }
                        }
                    }
                } catch(Exception ignored) {
                }
            }
            
            String logChannel = data.getGuildLogChannel();
            if(logChannel != null) {
                TextChannel tc = event.getGuild().getTextChannelById(logChannel);
                if(tc != null && tc.canTalk()) {
                    tc.sendMessage(String.format("`[%s]` \uD83D\uDCE3 `%s#%s` just joined `%s` `(User #%d | ID: %s)`",
                            hour, event.getUser().getName(), event.getUser().getDiscriminator(), event.getGuild().getName(), event.getGuild().getMembers().size(), event.getUser().getId())
                    ).queue();
                    logTotal++;
                }
            }
        } catch(Exception e) {
            SentryHelper.captureExceptionContext("Failed to process log join message!", e, MantaroListener.class, "Join Handler");
        }
        
        try {
            if(user.isBot() && data.isIgnoreBotsWelcomeMessage())
                return;
            
            String joinChannel = data.getLogJoinChannel();
            if(joinChannel == null || event.getGuild().getTextChannelById(joinChannel) == null) {
                joinChannel = data.getLogJoinLeaveChannel();
            }
            
            String joinMessage = data.getJoinMessage();
            sendJoinLeaveMessage(event, data.getExtraJoinMessages(), joinMessage, joinChannel);
            actions.labels("join_messages").inc();
        } catch(Exception e) {
            SentryHelper.captureExceptionContext("Failed to send user join message!", e, MantaroListener.class, "Join Handler");
            log.error("Failed to send join message!", e);
        }
    }
    
    private void onUserLeave(GuildMemberLeaveEvent event) {
        DBGuild dbg = MantaroData.db().getGuild(event.getGuild());
        GuildData data = dbg.getData();
        
        try {
            String hour = df.format(new Date(System.currentTimeMillis()));
            
            if(event.getMember().getUser().isBot() && data.isIgnoreBotsWelcomeMessage()) {
                return;
            }
            
            String logChannel = data.getGuildLogChannel();
            if(logChannel != null) {
                TextChannel tc = event.getGuild().getTextChannelById(logChannel);
                if(tc != null && tc.canTalk()) {
                    tc.sendMessage(String.format("`[%s]` \uD83D\uDCE3 `%s#%s` just left `%s` `(User #%d)`",
                            hour, event.getUser().getName(), event.getUser().getDiscriminator(), event.getGuild().getName(), event.getGuild().getMembers().size())
                    ).queue();
                    logTotal++;
                }
            }
        } catch(Exception e) {
            SentryHelper.captureExceptionContext("Failed to process log leave message!", e, MantaroListener.class, "Join Handler");
        }
        
        try {
            if(event.getMember().getUser().isBot() && data.isIgnoreBotsWelcomeMessage())
                return;
            
            String leaveChannel = data.getLogLeaveChannel();
            if(leaveChannel == null || event.getGuild().getTextChannelById(leaveChannel) == null) {
                leaveChannel = data.getLogJoinLeaveChannel();
            }
            
            String leaveMessage = data.getLeaveMessage();
            sendJoinLeaveMessage(event, data.getExtraLeaveMessages(), leaveMessage, leaveChannel);
            actions.labels("leave_messages").inc();
        } catch(Exception e) {
            SentryHelper.captureExceptionContext("Failed to send user leave message!", e, MantaroListener.class, "Join Handler");
            log.error("Failed to send leave message!", e);
        }
    }
    
    private void sendJoinLeaveMessage(GenericGuildMemberEvent event, List<String> extraMessages, String msg, String channel) {
        int select = extraMessages.isEmpty() ? 0 : rand.nextInt(extraMessages.size());
        String message = rand.nextBoolean() ? msg : extraMessages.isEmpty() ? msg : extraMessages.get(select);
        
        if(channel != null && message != null) {
            TextChannel tc = event.getGuild().getTextChannelById(channel);
            if(tc == null) {
                return;
            }
            
            if(!tc.canTalk()) {
                return;
            }
            
            if(message.contains("$(")) {
                message = new DynamicModifiers()
                                  .mapEvent("event", event)
                                  .resolve(message);
            }
            
            int c = message.indexOf(':');
            if(c != -1) {
                
                //Wonky?
                Matcher matcher = modifierPattern.matcher(message);
                String m = "none";
                //Find the first occurrence of a modifier (word:)
                if(matcher.find()) {
                    m = matcher.group().replace(":", "");
                }
                
                String v = message.substring(c + 1);
                String r = message.substring(0, c - m.length()).trim();
                
                if(m.equals("embed")) {
                    EmbedJSON embed;
                    try {
                        embed = GsonDataManager.gson(false).fromJson('{' + v + '}', EmbedJSON.class);
                    } catch(Exception ignored) {
                        tc.sendMessage(EmoteReference.ERROR2 + "The string ``{" + v + "}`` isn't a valid JSON.").queue();
                        return;
                    }
                    
                    MessageBuilder builder = new MessageBuilder()
                                                     .setEmbed(embed.gen(event.getMember()));
                    
                    if(!r.isEmpty())
                        builder.append(r);
                    
                    builder.sendTo(tc)
                            .queue(success -> {
                                    }, error ->
                                               tc.sendMessage("Failed to send join/leave message.").queue()
                            );
                    
                    return;
                }
            }
            
            tc.sendMessage(message).queue(success -> {
            }, failure -> tc.sendMessage("Failed to send join/leave message.").queue());
        }
    }
}
