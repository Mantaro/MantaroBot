package net.kodehawa.mantarobot.core.command;

import net.dv8tion.jda.api.entities.*;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.command.argument.ArgumentParseError;
import net.kodehawa.mantarobot.core.command.argument.Arguments;
import net.kodehawa.mantarobot.core.command.argument.MarkedBlock;
import net.kodehawa.mantarobot.core.command.argument.Parser;
import net.kodehawa.mantarobot.core.command.argument.split.StringSplitter;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

public class NewContext {
    private static final StringSplitter SPLITTER = new StringSplitter();

    private final Message message;
    private final I18nContext i18n;
    private final Arguments args;

    private NewContext(@Nonnull Message message, @Nonnull I18nContext i18n, @Nonnull Arguments args) {
        this.message = message;
        this.i18n = i18n;
        this.args = args;
    }

    public NewContext(@Nonnull Message message, @Nonnull I18nContext i18n, @Nonnull String contentAfterPrefix) {
        this(message, i18n, new Arguments(SPLITTER.split(contentAfterPrefix), 0));
    }

    public Arguments arguments() {
        return args;
    }

    public NewContext snapshot() {
        return new NewContext(message, i18n, args.snapshot());
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
        if(!args.hasNext()) {
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
        if(!args.hasNext()) return Optional.empty();
        MarkedBlock block = args.marked();
        Optional<T> optional = parser.parse(this);
        if(optional.isEmpty()) {
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
    public <T> List<T> many(@Nonnull Parser<T> parser) {
        List<T> list = new ArrayList<>();
        for(Optional<T> parsed = tryArgument(parser); parsed.isPresent(); parsed = tryArgument(parser)) {
            list.add(parsed.get());
        }
        return list;
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
        if(tryArgument(delimiter).isPresent()) {
            block.reset();
            return list;
        }
        for(Optional<T> parsed = tryArgument(valueParser); parsed.isPresent(); parsed = tryArgument(valueParser)) {
            list.add(parsed.get());
            block.mark();
            if(tryArgument(delimiter).isPresent()) {
                block.reset();
                return list;
            }
        }
        return list;
    }

    public MessageChannel getChannel() {
        return message.getChannel();
    }

    public Guild getGuild() {
        return message.getGuild();
    }

    public void send(Message message) {
        getChannel().sendMessage(message).queue();
    }

    public void send(String message) {
        getChannel().sendMessage(message).queue();
    }

    public void sendFile(byte[] bytes, String name) {
        getChannel().sendFile(bytes, name);
    }

    public void sendFormat(String message, Object... format) {
        getChannel().sendMessageFormat(message, format).queue();
    }

    public void send(MessageEmbed embed) {
        getChannel().sendMessage(embed).queue();
    }

    public void sendLocalized(String localizedMessage, Object... args) {
        getChannel().sendMessageFormat(i18n.get(localizedMessage), args).queue();
    }

    public void sendLocalized(String localizedMessage) {
        getChannel().sendMessage(i18n.get(localizedMessage)).queue();
    }

    public void sendStripped(String message) {
        getChannel().sendMessageFormat(message)
                .allowedMentions(EnumSet.noneOf(Message.MentionType.class))
                .queue();
    }

    public void sendStrippedLocalized(String localizedMessage, Object... args) {
        getChannel().sendMessageFormat(i18n.get(localizedMessage), args)
                .allowedMentions(EnumSet.noneOf(Message.MentionType.class))
                .queue();
    }

    public User retrieveUserById(String id) {
        User user = null;
        try {
            user = MantaroBot.getInstance().getShardManager().retrieveUserById(id).complete();
        } catch (Exception ignored) { }

        return user;
    }

    public Member retrieveMemberById(Guild guild, String id, boolean update) {
        Member member = null;
        try {
            member = guild.retrieveMemberById(id, update).complete();
        } catch (Exception ignored) { }

        return member;
    }

    public Member retrieveMemberById(String id, boolean update) {
        Member member = null;
        try {
            member = getGuild().retrieveMemberById(id, update).complete();
        } catch (Exception ignored) { }

        return member;
    }
}
