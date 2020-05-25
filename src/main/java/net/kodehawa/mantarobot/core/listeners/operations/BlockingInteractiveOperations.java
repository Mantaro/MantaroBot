package net.kodehawa.mantarobot.core.listeners.operations;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageActivity;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.pagination.ReactionPaginationAction;
import net.kodehawa.mantarobot.core.listeners.operations.core.BlockingOperationFilter;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import org.apache.commons.collections4.Bag;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

public class BlockingInteractiveOperations {
    private static final Message RECHECK_CONDITIONS = new RecheckConditions();
    private static final ConcurrentMap<Long, Set<RunningOperation>> OPS = new ConcurrentHashMap<>();
    private static final EventListener LISTENER = new InteractiveListener();
    
    @Nonnull
    @CheckReturnValue
    public static EventListener listener() {
        return LISTENER;
    }
    
    /**
     * @return null on timeout, valid message on success.
     * @throws java.util.concurrent.CancellationException if this operation is cancelled
     *         by starting another one for the same user on the same channel.
     */
    @Nullable
    public static Message waitFromUser(@Nonnull Context context, long timeout, @Nonnull TimeUnit unit) {
        return waitFromUser(context, null, timeout, unit);
    }
    
    /**
     * @return null on timeout, valid message on success.
     * @throws java.util.concurrent.CancellationException if this operation is cancelled
     *         by starting another one for the same user on the same channel.
     */
    @Nullable
    public static Message waitFromUser(@Nonnull Context context, @Nullable BlockingOperationFilter filter, long timeout, @Nonnull TimeUnit unit) {
        return waitFromUser(context.getChannel().getIdLong(), context.getAuthor().getIdLong(), null, timeout, unit);
    }
    
    /**
     * @return null on timeout, valid message on success.
     * @throws java.util.concurrent.CancellationException if this operation is cancelled
     *         by starting another one for the same user on the same channel.
     */
    @Nullable
    public static Message waitFromUser(long channelId, long userId, long timeout, @Nonnull TimeUnit unit) {
        return waitFromUser(channelId, userId, null, timeout, unit);
    }
    
    /**
     * @return null on timeout, valid message on success.
     * @throws java.util.concurrent.CancellationException if this operation is cancelled
     *         by starting another one for the same user on the same channel.
     */
    @Nullable
    public static Message waitFromUser(long channelId, long userId, @Nullable BlockingOperationFilter filter, long timeout, @Nonnull TimeUnit unit) {
        var userFilter = BlockingOperationFilter.fromUser(userId);
        if(filter != null) userFilter = userFilter.andThen(filter);
        return wait(channelId, userId, userFilter, timeout, unit);
    }
    
    /**
     * @return null on timeout, valid message on success.
     * @throws java.util.concurrent.CancellationException if this operation is cancelled
     *         by starting another one for the same user on the same channel.
     */
    @Nullable
    public static Message wait(@Nonnull Context context, long timeout, @Nonnull TimeUnit unit) {
        return wait(context, null, timeout, unit);
    }
    
    /**
     * @return null on timeout, valid message on success.
     * @throws java.util.concurrent.CancellationException if this operation is cancelled
     *         by starting another one for the same user on the same channel.
     */
    @Nullable
    public static Message wait(@Nonnull Context context, @Nullable BlockingOperationFilter filter, long timeout, @Nonnull TimeUnit unit) {
        return wait(context.getChannel().getIdLong(), context.getAuthor().getIdLong(), null, timeout, unit);
    }
    
    /**
     * @return null on timeout, valid message on success.
     * @throws java.util.concurrent.CancellationException if this operation is cancelled
     *         by starting another one for the same user on the same channel.
     */
    @Nullable
    public static Message wait(long channelId, long userId, long timeout, @Nonnull TimeUnit unit) {
        return wait(channelId, userId, null, timeout, unit);
    }
    
    /**
     * @return null on timeout, valid message on success.
     * @throws java.util.concurrent.CancellationException if this operation is cancelled
     *         by starting another one for the same user on the same channel.
     */
    @Nullable
    public static Message wait(long channelId, long userId, @Nullable BlockingOperationFilter filter, long timeout, @Nonnull TimeUnit unit) {
        if(!Thread.currentThread().isVirtual()) {
            throw new IllegalStateException("BlockingInteractiveOperation should only be used from virtual threads");
        }
        if (unit.toSeconds(timeout) < 1)
            throw new IllegalArgumentException("Timeout is less than 1 second");
        if(filter == null) {
            filter = m -> BlockingOperationFilter.Result.ACCEPT;
        }
        var op = new RunningOperation(userId, filter);
        var set = OPS.compute(channelId, (__, s) -> {
            if(s == null) {
                s = ConcurrentHashMap.newKeySet(1);
            } else {
                s.removeIf(o -> {
                    if(o.userId == userId) {
                        o.cancelled = true;
                        o.queue.offer(RECHECK_CONDITIONS);
                        return true;
                    }
                    return false;
                });
            }
            s.add(op);
            return s;
        });
    
        try {
            Message m;
            do {
                if(op.cancelled) {
                    throw new CancellationException();
                }
                m = op.queue.poll(timeout, unit);
            } while(m == RECHECK_CONDITIONS);
            return m;
        } catch(InterruptedException e) {
            throw new IllegalStateException(e);
        } finally {
            set.remove(op);
            OPS.compute(channelId, (__, curr) -> {
                if(curr == set && set.isEmpty()) return null;
                return curr;
            });
        }
    }
    
    private static class RunningOperation {
        private final SynchronousQueue<Message> queue = new SynchronousQueue<>();
        private final long userId;
        private final BlockingOperationFilter filter;
        private volatile boolean cancelled;
        private volatile boolean done;
    
        private RunningOperation(long userId, BlockingOperationFilter filter) {
            this.userId = userId;
            this.filter = filter;
        }
    }
    
    private static class InteractiveListener implements EventListener {
        @Override
        public void onEvent(@Nonnull GenericEvent e) {
            if (!(e instanceof MessageReceivedEvent))
                return;
            
            var message = ((MessageReceivedEvent) e).getMessage();
            
            //Don't listen to ourselves...
            if (message.getAuthor().equals(message.getJDA().getSelfUser()))
                return;
            
            var channelId = message.getChannel().getIdLong();
            var set = OPS.get(channelId);
            
            if (set == null || set.isEmpty())
                return;
    
            for(var op : set) {
                //don't risk blocking forever on a finished operation
                if(op.done) continue;
                var res = op.filter.test(message);
                switch(res) {
                    case ACCEPT: op.done = true; op.queue.offer(message); break;
                    case IGNORE: break;
                    case RESET_TIMEOUT: op.queue.offer(RECHECK_CONDITIONS); break;
                }
            }
        }
    }
    
    private static class RecheckConditions implements Message {
        @Nonnull
        @Override
        public List<User> getMentionedUsers() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public Bag<User> getMentionedUsersBag() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public List<TextChannel> getMentionedChannels() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public Bag<TextChannel> getMentionedChannelsBag() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public List<Role> getMentionedRoles() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public Bag<Role> getMentionedRolesBag() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public List<Member> getMentionedMembers(@Nonnull Guild guild) {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public List<Member> getMentionedMembers() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public List<IMentionable> getMentions(@Nonnull MentionType... types) {
            throw new UnsupportedOperationException();
        }
    
        @Override
        public boolean isMentioned(@Nonnull IMentionable mentionable, @Nonnull MentionType... types) {
            throw new UnsupportedOperationException();
        }
    
        @Override
        public boolean mentionsEveryone() {
            throw new UnsupportedOperationException();
        }
    
        @Override
        public boolean isEdited() {
            throw new UnsupportedOperationException();
        }
    
        @Nullable
        @Override
        public OffsetDateTime getTimeEdited() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public User getAuthor() {
            throw new UnsupportedOperationException();
        }
    
        @Nullable
        @Override
        public Member getMember() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public String getJumpUrl() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public String getContentDisplay() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public String getContentRaw() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public String getContentStripped() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public List<String> getInvites() {
            throw new UnsupportedOperationException();
        }
    
        @Nullable
        @Override
        public String getNonce() {
            throw new UnsupportedOperationException();
        }
    
        @Override
        public boolean isFromType(@Nonnull ChannelType type) {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public ChannelType getChannelType() {
            throw new UnsupportedOperationException();
        }
    
        @Override
        public boolean isWebhookMessage() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public MessageChannel getChannel() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public PrivateChannel getPrivateChannel() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public TextChannel getTextChannel() {
            throw new UnsupportedOperationException();
        }
    
        @Nullable
        @Override
        public Category getCategory() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public Guild getGuild() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public List<Attachment> getAttachments() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public List<MessageEmbed> getEmbeds() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public List<Emote> getEmotes() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public Bag<Emote> getEmotesBag() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public List<MessageReaction> getReactions() {
            throw new UnsupportedOperationException();
        }
    
        @Override
        public boolean isTTS() {
            throw new UnsupportedOperationException();
        }
    
        @Nullable
        @Override
        public MessageActivity getActivity() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public MessageAction editMessage(@Nonnull CharSequence newContent) {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public MessageAction editMessage(@Nonnull MessageEmbed newContent) {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public MessageAction editMessageFormat(@Nonnull String format, @Nonnull Object... args) {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public MessageAction editMessage(@Nonnull Message newContent) {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public AuditableRestAction<Void> delete() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public JDA getJDA() {
            throw new UnsupportedOperationException();
        }
    
        @Override
        public boolean isPinned() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public RestAction<Void> pin() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public RestAction<Void> unpin() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public RestAction<Void> addReaction(@Nonnull Emote emote) {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public RestAction<Void> addReaction(@Nonnull String unicode) {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public RestAction<Void> clearReactions() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public RestAction<Void> clearReactions(@Nonnull String unicode) {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public RestAction<Void> clearReactions(@Nonnull Emote emote) {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public RestAction<Void> removeReaction(@Nonnull Emote emote) {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public RestAction<Void> removeReaction(@Nonnull Emote emote, @Nonnull User user) {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public RestAction<Void> removeReaction(@Nonnull String unicode) {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public RestAction<Void> removeReaction(@Nonnull String unicode, @Nonnull User user) {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public ReactionPaginationAction retrieveReactionUsers(@Nonnull Emote emote) {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public ReactionPaginationAction retrieveReactionUsers(@Nonnull String unicode) {
            throw new UnsupportedOperationException();
        }
    
        @Nullable
        @Override
        public MessageReaction.ReactionEmote getReactionByUnicode(@Nonnull String unicode) {
            throw new UnsupportedOperationException();
        }
    
        @Nullable
        @Override
        public MessageReaction.ReactionEmote getReactionById(@Nonnull String id) {
            throw new UnsupportedOperationException();
        }
    
        @Nullable
        @Override
        public MessageReaction.ReactionEmote getReactionById(long id) {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public AuditableRestAction<Void> suppressEmbeds(boolean suppressed) {
            throw new UnsupportedOperationException();
        }
    
        @Override
        public boolean isSuppressedEmbeds() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public EnumSet<MessageFlag> getFlags() {
            throw new UnsupportedOperationException();
        }
    
        @Nonnull
        @Override
        public MessageType getType() {
            throw new UnsupportedOperationException();
        }
    
        @Override
        public void formatTo(Formatter formatter, int flags, int width, int precision) {
            throw new UnsupportedOperationException();
        }
    
        @Override
        public long getIdLong() {
            throw new UnsupportedOperationException();
        }
    }
}
