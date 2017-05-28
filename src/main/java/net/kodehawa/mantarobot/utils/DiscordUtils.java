package net.kodehawa.mantarobot.utils;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.ReactionOperations;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;

public class DiscordUtils {
	public static <T> Pair<String, Integer> embedList(List<T> list, Function<T, String> toString) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			String s = toString.apply(list.get(i));
			if (b.length() + s.length() + 5 > MessageEmbed.TEXT_MAX_LENGTH) return Pair.of(b.toString(), i);
			b.append("**").append(i + 1).append(".** ");
			b.append(s);
			b.append("\n");
		}

		return Pair.of(b.toString(), list.size());
	}


	public static boolean selectInt(GuildMessageReceivedEvent event, int max, IntConsumer valueConsumer) {
		return InteractiveOperations.create(event.getChannel(), "Selection", 20000, OptionalInt.empty(), (e) -> {
			if (!e.getAuthor().equals(event.getAuthor())) return false;

			try {
				int choose = Integer.parseInt(e.getMessage().getContent());
				if (choose < 1 || choose >= max) return false;
				valueConsumer.accept(choose);
				return true;
			} catch (Exception ignored) {}
			return false;
		});
	}

	public static <T> boolean selectList(GuildMessageReceivedEvent event, List<T> list, Function<T, String> toString, Function<String, MessageEmbed> toEmbed, Consumer<T> valueConsumer) {
		Pair<String, Integer> r = embedList(list, toString);
		event.getChannel().sendMessage(toEmbed.apply(r.getLeft())).queue();
		return selectInt(event, r.getRight() + 1, i -> valueConsumer.accept(list.get(i - 1)));
	}

	public static <T> boolean selectList(GuildMessageReceivedEvent event, T[] list, Function<T, String> toString, Function<String, MessageEmbed> toEmbed, Consumer<T> valueConsumer) {
		Pair<String, Integer> r = embedList(Arrays.asList(list), toString);
		event.getChannel().sendMessage(toEmbed.apply(r.getLeft())).queue();
		return selectInt(event, r.getRight() + 1, i -> valueConsumer.accept(list[i - 1]));
	}

	public static <T> T selectListSync(GuildMessageReceivedEvent event, List<T> list, Function<T, String> toString, Function<String, MessageEmbed> toEmbed) {
		CompletableFuture<T> future = new CompletableFuture<T>();
		Object notify = new Object();

		if (!selectList(event, list, toString, toEmbed, (value) -> {
			future.complete(value);
			synchronized (notify) {
				notify.notify();
			}
		})) throw new IllegalStateException();

		synchronized (notify) {
			try {
				notify.wait(10000);
			} catch (InterruptedException ignored) {}
		}

		if (!future.isDone()) future.complete(null);

		try {
			return future.get();
		} catch (InterruptedException | ExecutionException ignored) {
			return null;
		}
	}

    public static Future<Void> list(GuildMessageReceivedEvent event, int timeoutSeconds, boolean canEveryoneUse, IntIntObjectFunction<EmbedBuilder> supplier, String... parts) {
        if(parts.length == 0) return null;
        List<MessageEmbed> embeds = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        int total; {
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
            int l = s.length()+1;
            if(l > MessageEmbed.TEXT_MAX_LENGTH) throw new IllegalArgumentException("Length for one of the pages is greater than the maximum");
            if(sb.length() + l > MessageEmbed.TEXT_MAX_LENGTH) {
                EmbedBuilder eb = supplier.apply(embeds.size()+1, total);
                eb.setDescription(sb.toString());
                embeds.add(eb.build());
                sb = new StringBuilder();
            }
            sb.append(s).append('\n');
        }
        if(sb.length() > 0) {
            EmbedBuilder eb = supplier.apply(embeds.size()+1, total);
            eb.setDescription(sb.toString());
            embeds.add(eb.build());
        }
        AtomicInteger index = new AtomicInteger();
        Message m = event.getChannel().sendMessage(embeds.get(0)).complete();
        return ReactionOperations.create(m, timeoutSeconds, (e)->{
            if(!canEveryoneUse && e.getUser().getIdLong() != event.getAuthor().getIdLong()) return false;
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
            return false;
        }, "\u2b05", "\u27a1");
    }

    public static Future<Void> listUpdatable(GuildMessageReceivedEvent event, int timeoutSeconds, boolean canEveryoneUse, IntIntObjectFunction<EmbedBuilder> supplier, String... parts) {
        if(parts.length == 0) return null;
        List<StringBuilder> embeds = new ArrayList<>();
        AtomicInteger index = new AtomicInteger();
        StringBuilder sb = new StringBuilder();
        int total; {
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
            int l = s.length()+1;
            if(l > MessageEmbed.TEXT_MAX_LENGTH) throw new IllegalArgumentException("Length for one of the pages is greater than the maximum");
            if(sb.length() + l > MessageEmbed.TEXT_MAX_LENGTH) {
                embeds.add(sb);
                sb = new StringBuilder();
            }
            sb.append(s).append('\n');
        }
        if(sb.length() > 0) {
            embeds.add(sb);
        }
        Message m = event.getChannel().sendMessage(supplier.apply(1, total).setDescription(embeds.get(0)).build()).complete();
        return ReactionOperations.create(m, timeoutSeconds, (e)->{
            if(!canEveryoneUse && e.getUser().getIdLong() != event.getAuthor().getIdLong()) return false;
            switch(e.getReactionEmote().getName()) {
                case "\u2b05": {//left arrow
                    if (index.get() == 0) break;
                    int i = index.decrementAndGet();
                    m.editMessage(supplier.apply(i+1, total).setDescription(embeds.get(i)).build()).queue();
                } break;
                case "\u27a1": {//right arrow
                    if (index.get() + 1 >= embeds.size()) break;
                    int i = index.incrementAndGet();
                    m.editMessage(supplier.apply(i+1, total).setDescription(embeds.get(i)).build()).queue();
                } break;
            }
            if(event.getGuild().getSelfMember().hasPermission(e.getTextChannel(), Permission.MESSAGE_MANAGE)) {
                e.getReaction().removeReaction(e.getUser()).queue();
            }
            return false;
        }, "\u2b05", "\u27a1");
    }
}
