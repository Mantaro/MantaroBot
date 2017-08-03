package net.kodehawa.mantarobot.commands.info;

import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.IntSummaryStatistics;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

public class StatsHelper {
	public static <T> DoubleSummaryStatistics calculateDouble(Collection<T> collection, ToDoubleFunction<T> toDouble) {
		return collection.stream().mapToDouble(toDouble).summaryStatistics();
	}

	public static <T> IntSummaryStatistics calculateInt(Collection<T> collection, ToIntFunction<T> toInt) {
		return collection.stream().mapToInt(toInt).summaryStatistics();
	}

	public static void sendStatsMessageAndThen(GuildMessageReceivedEvent e, String message){
		e.getChannel().sendMessage(EmoteReference.EYES + "**[Stats]** Y-Yeah... gathering info, hold on for a bit...").queue(message1 -> message1.editMessage(message).queue());
	}

	public static void sendStatsMessageAndThen(GuildMessageReceivedEvent e, MessageEmbed message){
		e.getChannel().sendMessage(EmoteReference.EYES + "**[Stats]** Y-Yeah... gathering info, hold on for a bit...").queue(message1 -> message1.editMessage(message).queue());
	}
}
