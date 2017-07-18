package net.kodehawa.mantarobot.utils;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.listeners.operations.old.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.old.OperationListener;
import net.kodehawa.mantarobot.core.listeners.operations.old.ReactionOperations;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;

public class DiscordUtils {
    public static <T> Pair<String, Integer> embedList(List<T> list, Function<T, String> toString) {
        StringBuilder b = new StringBuilder();
        for(int i = 0; i < list.size(); i++) {
            String s = toString.apply(list.get(i));
            if(b.length() + s.length() + 5 > MessageEmbed.TEXT_MAX_LENGTH) return Pair.of(b.toString(), i);
            b.append("**").append(i + 1).append(".** ");
            b.append(s);
            b.append("\n");
        }

        return Pair.of(b.toString(), list.size());
    }


    public static Future<Void> selectInt(GuildMessageReceivedEvent event, int max, IntConsumer valueConsumer) {
        return InteractiveOperations.createOverriding(event.getChannel(), 20, (e) -> {
            if(!e.getAuthor().equals(event.getAuthor())) return OperationListener.IGNORED;

            try {
                int choose = Integer.parseInt(e.getMessage().getContent());
                if(choose < 1 || choose >= max) return OperationListener.RESET_TIMEOUT;
                valueConsumer.accept(choose);
                return OperationListener.COMPLETED;
            } catch(Exception ignored) {
            }
            return OperationListener.RESET_TIMEOUT;
        });
    }

    public static <T> Future<Void> selectList(GuildMessageReceivedEvent event, List<T> list, Function<T, String> toString, Function<String, MessageEmbed> toEmbed, Consumer<T> valueConsumer) {
        Pair<String, Integer> r = embedList(list, toString);
        event.getChannel().sendMessage(toEmbed.apply(r.getLeft())).queue();
        return selectInt(event, r.getRight() + 1, i -> valueConsumer.accept(list.get(i - 1)));
    }

    public static <T> Future<Void> selectList(GuildMessageReceivedEvent event, T[] list, Function<T, String> toString, Function<String, MessageEmbed> toEmbed, Consumer<T> valueConsumer) {
        Pair<String, Integer> r = embedList(Arrays.asList(list), toString);
        event.getChannel().sendMessage(toEmbed.apply(r.getLeft())).queue();
        return selectInt(event, r.getRight() + 1, i -> valueConsumer.accept(list[i - 1]));
    }

    public static Future<Void> list(GuildMessageReceivedEvent event, int timeoutSeconds, boolean canEveryoneUse, IntIntObjectFunction<EmbedBuilder> supplier, String... parts) {
        if(parts.length == 0) return null;
        List<MessageEmbed> embeds = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        int total;
        {
            int t = 0;
            int c = 0;
            for(String s : parts) {
                if(s.length() + c + 1 > MessageEmbed.TEXT_MAX_LENGTH) {
                    t++;
                    c = 0;
                }
                c += s.length() + 1;
            }
            if(c > 0) t++;
            total = t;
        }
        for(String s : parts) {
            int l = s.length() + 1;
            if(l > MessageEmbed.TEXT_MAX_LENGTH)
                throw new IllegalArgumentException("Length for one of the pages is greater than the maximum");
            if(sb.length() + l > MessageEmbed.TEXT_MAX_LENGTH) {
                EmbedBuilder eb = supplier.apply(embeds.size() + 1, total);
                eb.setDescription(sb.toString());
                embeds.add(eb.build());
                sb = new StringBuilder();
            }
            sb.append(s).append('\n');
        }
        if(sb.length() > 0) {
            EmbedBuilder eb = supplier.apply(embeds.size() + 1, total);
            eb.setDescription(sb.toString());
            embeds.add(eb.build());
        }
        AtomicInteger index = new AtomicInteger();
        Message m = event.getChannel().sendMessage(embeds.get(0)).complete();
        return ReactionOperations.create(m, timeoutSeconds, (e) -> {
            if(!canEveryoneUse && e.getUser().getIdLong() != event.getAuthor().getIdLong())
                return OperationListener.IGNORED;
            switch(e.getReactionEmote().getName()) {
                case "\u2b05": //left arrow
                    if(index.get() == 0) break;
                    m.editMessage(embeds.get(index.decrementAndGet())).queue();
                    break;
                case "\u27a1": //right arrow
                    if(index.get() + 1 >= embeds.size()) break;
                    m.editMessage(embeds.get(index.incrementAndGet())).queue();
                    break;
            }
            if(event.getGuild().getSelfMember().hasPermission(e.getTextChannel(), Permission.MESSAGE_MANAGE)) {
                e.getReaction().removeReaction(e.getUser()).queue();
            }
            return OperationListener.RESET_TIMEOUT;
        }, "\u2b05", "\u27a1");
    }
}
