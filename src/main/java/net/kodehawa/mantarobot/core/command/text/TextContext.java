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

package net.kodehawa.mantarobot.core.command.text;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.attribute.IAgeRestrictedChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.music.MantaroAudioManager;
import net.kodehawa.mantarobot.core.command.argument.ArgumentParseError;
import net.kodehawa.mantarobot.core.command.argument.Arguments;
import net.kodehawa.mantarobot.core.command.argument.MarkedBlock;
import net.kodehawa.mantarobot.core.command.argument.Parser;
import net.kodehawa.mantarobot.core.command.argument.Parsers;
import net.kodehawa.mantarobot.core.command.argument.split.StringSplitter;
import net.kodehawa.mantarobot.core.command.i18n.I18nContext;
import net.kodehawa.mantarobot.core.command.helpers.IContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.MantaroObject;
import net.kodehawa.mantarobot.db.entities.Marriage;
import net.kodehawa.mantarobot.db.entities.MongoGuild;
import net.kodehawa.mantarobot.db.entities.MongoUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.PlayerStats;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.CustomFinderUtil;
import net.kodehawa.mantarobot.utils.commands.UtilsContext;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RateLimitContext;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class TextContext implements IContext {
    private final ManagedDatabase managedDatabase = MantaroData.db();
    private final Config config = MantaroData.config().get();
    private static final StringSplitter SPLITTER = new StringSplitter();

    private final MessageReceivedEvent event;
    private final Arguments args;
    private final boolean isMentionPrefix;
    private I18nContext i18n;
    private String customContent; // opts moment

    private TextContext(@Nonnull MessageReceivedEvent event, @Nonnull I18nContext i18n, @Nonnull Arguments args, boolean isMentionPrefix) {
        this.event = event;
        this.i18n = i18n;
        this.args = args;
        this.isMentionPrefix = isMentionPrefix;
    }

    public TextContext(@Nonnull MessageReceivedEvent event, @Nonnull I18nContext i18n, @Nonnull String contentAfterPrefix, boolean isMentionPrefix) {
        this(event, i18n, new Arguments(SPLITTER.split(contentAfterPrefix), 0), isMentionPrefix);
    }

    public Arguments arguments() {
        return args;
    }

    public TextContext snapshot() {
        return new TextContext(event, i18n, args.snapshot(), isMentionPrefix);
    }

    public MessageReceivedEvent getEvent() {
        return event;
    }

    /**
     * Attempts to parse an argument with the provided {@link net.kodehawa.mantarobot.core.command.argument.Parser parser}.
     * <br>If the parser returns {@link java.util.Optional#empty() nothing} or there are
     * no more arguments to read, an exception is thrown.
     *
     * @param parser Parser to use.
     * @param <T> Type of the object returned by the parser.
     *
     * @return The parsed object.
     *
     * @throws ArgumentParseError If there are no more arguments to read or the parser
     *                            returned nothing.
     */
    @Nonnull
    @CheckReturnValue
    public <T> T argument(@Nonnull Parser<T> parser) {
        return argument(parser, null);
    }

    /**
     * Attempts to parse an argument with the provided {@link Parser parser}.
     * <br>If the parser returns {@link java.util.Optional#empty() nothing} or there are
     * no more arguments to read, an exception is thrown.
     *
     * @param parser Parser to use.
     * @param failureMessage Message to provide to the {@link net.kodehawa.mantarobot.core.command.argument.ArgumentParseError error}
     *                       thrown on parse failure.
     * @param <T> Type of the object returned by the parser.
     *
     * @return The parsed object.
     *
     * @throws ArgumentParseError If there are no more arguments to read or the parser
     *                            returned nothing.
     */
    @Nonnull
    @CheckReturnValue
    public <T> T argument(@Nonnull Parser<T> parser, @Nullable String failureMessage) {
        return argument(parser, "Missing argument", failureMessage);
    }

    /**
     * Attempts to parse an argument with the provided {@link Parser parser}.
     * <br>If the parser returns {@link java.util.Optional#empty() nothing} or there are
     * no more arguments to read, an exception is thrown.
     *
     * @param parser Parser to use.
     * @param failureMessage Message to provide to the {@link net.kodehawa.mantarobot.core.command.argument.ArgumentParseError error}
     *                       thrown on parse failure.
     * @param <T> Type of the object returned by the parser.
     *
     * @return The parsed object.
     *
     * @throws ArgumentParseError If there are no more arguments to read or the parser
     *                            returned nothing.
     */
    @Nonnull
    @CheckReturnValue
    public <T> T argument(@Nonnull Parser<T> parser, @Nullable String missingMessage, @Nullable String failureMessage) {
        int offset = args.getOffset();
        Optional<T> optional;
        if (!args.hasNext()) {
            throw new ArgumentParseError(missingMessage, this, parser, args.snapshot());
        } else {
            optional = parser.parse(this);
        }
        return optional.orElseThrow(()->{
            Arguments copy = args.snapshot();
            copy.setOffset(offset);
            return new ArgumentParseError(failureMessage, this, parser, copy);
        });
    }

    /**
     * Attempts to parse an argument, returning to the previous state if parsing fails.
     * <br>Returns {@link Optional#empty() empty} if parsing fails or there are no more
     * arguments to read.
     * <br>If parsing fails, all arguments read by the parser are unread.
     *
     * @param parser Parser to use.
     * @param <T> Type of the object returned by the parser.
     *
     * @return An optional parsed argument.
     */
    @Nonnull
    @CheckReturnValue
    public <T> Optional<T> tryArgument(@Nonnull Parser<T> parser) {
        if (!args.hasNext()) return Optional.empty();
        MarkedBlock block = args.marked();
        Optional<T> optional = parser.parse(this);
        if (optional.isEmpty()) {
            block.reset();
        }
        return optional;
    }

    /**
     * Parses as many arguments as possible with the provided parser, stopping when parsing fails.
     * <br>Example:
     * Given the arguments <code>[1, 2, "abc"]</code>:
     * <pre><code>
     * List&lt;Integer&gt; ints = context.many({@link net.kodehawa.mantarobot.core.command.argument.Parsers#strictInt()} Parsers.strictInt()});
     * assertEquals(ints.size(), 2);
     * assertEquals(ints.get(0), 1);
     * assertEquals(ints.get(1), 2);
     * String string = context.argument({@link net.kodehawa.mantarobot.core.command.argument.Parsers#string() Parsers.string()});
     * assertEquals(string, "abc");
     * </code></pre>
     *
     * @param parser Parser to use.
     * @param <T> Type of the objects returned by the parser.
     *
     * @return A possibly empty list of arguments returned by the parser.
     */
    @Nonnull
    @CheckReturnValue
    public <T> List<T> takeMany(@Nonnull Parser<T> parser) {
        List<T> list = new ArrayList<>();
        for(Optional<T> parsed = tryArgument(parser); parsed.isPresent(); parsed = tryArgument(parser)) {
            list.add(parsed.get());
        }
        return list;
    }

    /**
     * Parses as many arguments as possible from the remainder, stopping when there is no more arguments to read.
     * <p>
     * This will reduce the arguments to a single String value, which can be passed around to arguments -- this is useful for stuff like
     * Member lookups, or simply as an aid when porting old commands, or when we expect an uninterrupted stream of arguments of the same type.
     * This does not preserve new lines, for that use {@link Parsers#remainingContent()}
     * </p>
     * <p>
     * You can also use {@link Parsers#remainingContent()}, but it will fail if all arguments are empty. This will never fail.
     * Use the method above if you want the text as-is (new lines included), but make sure to handle failures.
     * <p>
     * This will use {@link Parsers#remainingArguments()}, returning an empty String on failure.
     * </p>
     * @return A possibly-empty String value
     */
    @Nonnull
    @CheckReturnValue
    public <T> String takeAllString() {
        var arg = tryArgument(Parsers.remainingArguments());
        return arg.orElse("");
    }

    /**
     * Returns whether or not the current argument can be parsed by the provided parser.
     * <br>Consumes the argument if it was parsed successfully.
     * <br>If parsing fails, all arguments read by the parser are unread.
     *
     * @param parser Parser to use.
     *
     * @return True if the current argument matched the parser.
     */
    @CheckReturnValue
    public boolean matches(@Nonnull Parser<?> parser) {
        return tryArgument(parser).isPresent();
    }

    /**
     * Reads arguments matching the provided parser, until either parsing fails or a delimiter is matched.
     * <br>Example:
     * Given the arguments <code>[1, 2, -1]</code>:
     * <pre><code>
     * List&lt;Integer&gt; ints = context.takeUntil({@link net.kodehawa.mantarobot.core.command.argument.Parsers#strictInt() Parsers.strictInt()}, {@link net.kodehawa.mantarobot.core.command.argument.Parsers#strictInt() Parsers.strictInt()}.{@link Parser#filter(java.util.function.Predicate) filter(x-&gt;x &lt; 0)});
     * assertEquals(ints.size(), 2);
     * assertEquals(ints.get(0), 1);
     * assertEquals(ints.get(1), 2);
     * Integer last = context.argument({@link net.kodehawa.mantarobot.core.command.argument.Parsers#strictInt() Parsers.strictInt()});
     * assertEquals(last, -1);
     * </code></pre>
     *
     * @param valueParser Parser used for values.
     * @param delimiter Parser used for delimiter checking.
     * @param <T> Type of the objects returned by the value parser.
     *
     * @return Possibly empty list of arguments matching.
     */
    @Nonnull
    @CheckReturnValue
    public <T> List<T> takeUntil(Parser<T> valueParser, Parser<?> delimiter) {
        List<T> list = new ArrayList<>();
        MarkedBlock block = args.marked();
        if (tryArgument(delimiter).isPresent()) {
            block.reset();
            return list;
        }
        for(Optional<T> parsed = tryArgument(valueParser); parsed.isPresent(); parsed = tryArgument(valueParser)) {
            list.add(parsed.get());
            block.mark();
            if (tryArgument(delimiter).isPresent()) {
                block.reset();
                return list;
            }
        }
        return list;
    }

    public Message getMessage() {
        return event.getMessage();
    }

    @Override
    public GuildMessageChannel getChannel() {
        if (getMessage().getChannel() instanceof GuildMessageChannel c) {
            return c;
        }
        return null;
    }

    @Override
    public Guild getGuild() {
        return getMessage().getGuild();
    }

    @Override
    public void send(MessageCreateData message) {
        getChannel().sendMessage(message).queue();
    }

    @Override
    public void send(String message) {
        getChannel().sendMessage(message).queue();
    }

    public void sendFile(byte[] bytes, String name) {
        getChannel().sendFiles(FileUpload.fromData(bytes, name)).queue();
    }

    @Override
    public ManagedDatabase db() {
        return managedDatabase;
    }

    @Override
    public Message sendResult(String s) {
        return getChannel().sendMessage(s).complete();
    }

    @Override
    public Message sendResult(MessageEmbed e) {
        return getChannel().sendMessageEmbeds(e).complete();
    }

    @Override
    public void sendLocalized(String localizedMessage, Object... args) {
        getChannel().sendMessageFormat(i18n.get(localizedMessage), args).queue();
    }

    @Override
    public void sendLocalizedStripped(String s, Object... args) {
        getChannel().sendMessage(getLanguageContext().get(s).formatted(args))
                .setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
                .queue();
    }

    @Override
    public void sendFormat(String message, Object... format) {
        getChannel().sendMessage(
                String.format(Utils.getLocaleFromLanguage(getLanguageContext()), message, format)
        ).queue();
    }

    @Override
    public void sendFormatStripped(String message, Object... format) {
        getChannel().sendMessage(
                String.format(Utils.getLocaleFromLanguage(getLanguageContext()), message, format)
        ).setAllowedMentions(EnumSet.noneOf(Message.MentionType.class)).queue();
    }

    @Override
    public void sendFormat(String message, Collection<ActionRow> actionRow, Object... format) {
        getChannel().sendMessage(
                String.format(Utils.getLocaleFromLanguage(getLanguageContext()), message, format)
        ).setComponents(actionRow).queue();
    }

    public void sendLocalized(String localizedMessage) {
        getChannel().sendMessage(i18n.get(localizedMessage)).queue();
    }

    @Override
    public void sendStripped(String message) {
        getChannel().sendMessageFormat(message)
                .setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
                .queue();
    }

    @Override
    public void send(MessageEmbed e) {
        // Sending embeds while supressing the failure callbacks leads to very hard
        // to debug bugs, so enable it.
        getChannel().sendMessageEmbeds(e)
                .queue(success -> {}, Throwable::printStackTrace);
    }

    @Override
    public void send(MessageEmbed embed, ActionRow... actionRow) {
        // Sending embeds while supressing the failure callbacks leads to very hard
        // to debug bugs, so enable it.
        getChannel().sendMessageEmbeds(embed)
                .setComponents(actionRow).queue(success -> {}, Throwable::printStackTrace);
    }

    public void sendStrippedLocalized(String localizedMessage, Object... args) {
        getChannel().sendMessageFormat(i18n.get(localizedMessage), args)
                .setAllowedMentions(EnumSet.noneOf(Message.MentionType.class))
                .queue();
    }

    public User retrieveUserById(String id) {
        User user = null;
        try {
            user = MantaroBot.getInstance().getShardManager().retrieveUserById(id).complete();
        } catch (Exception ignored) { }

        return user;
    }

    public User retrieveUserById(long id) {
        User user = null;
        try {
            user = MantaroBot.getInstance().getShardManager().retrieveUserById(id).complete();
        } catch (Exception ignored) { }

        return user;
    }

    public String getTagOrDisplay(User user) {
        if (user.getGlobalName() != null) {
            return user.getGlobalName();
        } else {
            return user.getAsTag();
        }
    }

    public Member retrieveMemberById(Guild guild, String id, boolean update) {
        Member member = null;
        try {
            member = guild.retrieveMemberById(id).useCache(true).complete();
        } catch (Exception ignored) { }

        return member;
    }

    public Member retrieveMemberById(String id, boolean update) {
        Member member = null;
        try {
            member = getGuild().retrieveMemberById(id).useCache(update).complete();
        } catch (Exception ignored) { }

        return member;
    }

    @Override
    public Member getMember() {
        return getMessage().getMember();
    }

    public SelfUser getSelfUser() {
        return getChannel().getJDA().getSelfUser();
    }

    public Member getSelfMember() {
        return getGuild().getSelfMember();
    }

    public MantaroAudioManager getAudioManager() {
        return getBot().getAudioManager();
    }

    @Override
    public ShardManager getShardManager() {
        return getBot().getShardManager();
    }

    @Override
    public MantaroObject getMantaroData() {
        return managedDatabase.getMantaroData();
    }

    @Override
    public Config getConfig() {
        return config;
    }

    @Override
    public MongoGuild getDBGuild() {
        return managedDatabase.getGuild(getGuild());
    }

    @Override
    public MongoUser getDBUser() {
        return managedDatabase.getUser(getAuthor());
    }

    @Override
    public MongoUser getDBUser(User user) {
        return managedDatabase.getUser(user);
    }

    public MongoUser getDBUser(Member member) {
        return managedDatabase.getUser(member);
    }

    public MongoUser getDBUser(String id) {
        return managedDatabase.getUser(id);
    }

    @Override
    public Player getPlayer() {
        return managedDatabase.getPlayer(getAuthor());
    }

    @Override
    public Player getPlayer(User user) {
        return managedDatabase.getPlayer(user);
    }

    public Player getPlayer(Member member) {
        return managedDatabase.getPlayer(member);
    }

    public Player getPlayer(String id) {
        return managedDatabase.getPlayer(id);
    }

    public PlayerStats getPlayerStats() {
        return managedDatabase.getPlayerStats(getMember());
    }

    public PlayerStats getPlayerStats(String id) {
        return managedDatabase.getPlayerStats(id);
    }

    public PlayerStats getPlayerStats(User user) {
        return managedDatabase.getPlayerStats(user);
    }

    public PlayerStats getPlayerStats(Member member) {
        return managedDatabase.getPlayerStats(member);
    }

    public MantaroBot getBot() {
        return MantaroBot.getInstance();
    }

    @Override
    public User getAuthor() {
        return getMessage().getAuthor();
    }

    public Marriage getMarriage(MongoUser userData) {
        return MantaroData.db().getMarriage(userData.getMarriageId());
    }

    public void findMember(String query, Consumer<List<Member>> success) {
        CustomFinderUtil.lookupMember(getGuild(), this, query).onSuccess(s -> {
            try {
                success.accept(s);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    @Override
    public RateLimitContext ratelimitContext() {
        return new RateLimitContext(getGuild(), getMessage(), getChannel(), event, null);
    }

    @Override
    public UtilsContext getUtilsContext() {
        return new UtilsContext(getGuild(), getMember(), getChannel(), i18n, null);
    }

    public List<Member> getMentionedMembers() {
        var members = getEvent().getMessage().getMentions().getMembers();
        if (isMentionPrefix) {
            var mutable = new LinkedList<>(members);
            return mutable.subList(1, mutable.size());
        }

        return members;
    }

    public List<User> getMentionedUsers() {
        var users = getEvent().getMessage().getMentions().getUsers();
        if (isMentionPrefix) {
            var mutable = new LinkedList<>(users);
            return mutable.subList(1, mutable.size());
        }

        return users;
    }

    @Override
    public I18nContext getLanguageContext() {
        return i18n;
    }

    public void setLanguageContext(I18nContext languageContext) {
        this.i18n = languageContext;
    }

    public I18nContext getGuildLanguageContext() {
        return new I18nContext(getDBGuild(), null);
    }

    public Color getMemberColor(Member member) {
        return member.getColor() == null ? Color.PINK : member.getColor();
    }

    public Color getMemberColor() {
        return getMemberColor(getMember());
    }

    // Both used for options.
    public void setCustomContent(String str) {
        this.customContent = str;
    }

    /**
     * Get the custom (usually filtered) content. This is only used in options, do not call it anywhere else.
     * @return The custom content that has been set using Context#setCustomContent
     */
    public String getCustomContent() {
        return this.customContent;
    }

    /**
     * Get all the content in a message.
     * @return all the content in a message, including new lines, or empty if none.
     */
    public String getContent() {
         var content = tryArgument(Parsers.remainingContent());
        return content.map(String::trim).orElse("");
    }

    public boolean isChannelNSFW() {
        if (getChannel() instanceof IAgeRestrictedChannel txtChannel) {
            return txtChannel.isNSFW();
        }

        return true;
    }

    public boolean isMentionPrefix() {
        return isMentionPrefix;
    }
}
