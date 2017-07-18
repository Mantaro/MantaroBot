package net.kodehawa.mantarobot.commands.info;

import net.dv8tion.jda.core.entities.TextChannel;
import net.kodehawa.mantarobot.core.listeners.command.CommandListener;
import net.kodehawa.mantarobot.db.entities.helpers.ExtraGuildData;
import net.kodehawa.mantarobot.modules.commands.base.Category;

import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class HelpUtils {
    public static String forType(TextChannel channel, ExtraGuildData guildData, Category category) {
        return forType(
                CommandListener.PROCESSOR.commands().entrySet().stream()
                        .filter(entry -> entry.getValue().category() == category)
                        .filter(entry -> !guildData.getDisabledCategories().contains(entry.getValue().category()))
                        .filter(c -> !guildData.getDisabledCommands().contains(c.getKey()))
                        .filter(c -> guildData.getChannelSpecificDisabledCommands().get(channel.getId()) == null || !guildData.getChannelSpecificDisabledCommands().get(channel.getId()).contains(c.getKey()))
                        .map(Entry::getKey)
                        .collect(Collectors.toList())
        );
    }

    public static String forType(List<String> values) {
        if(values.size() == 0) return "`Disabled`";

        return "``" + values.stream().sorted()
                .collect(Collectors.joining("`` ``")) + "``";
    }
}
