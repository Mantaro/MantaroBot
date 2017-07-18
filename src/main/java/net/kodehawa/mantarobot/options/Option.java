package net.kodehawa.mantarobot.options;

import lombok.Getter;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Option {

    @Getter
    private static Map<String, Option> optionMap = new HashMap<>();
    //Display names + desc in the avaliable options list.
    @Getter
    private static List<String> avaliableOptions = new ArrayList<>();
    @Getter
    private static String shortDescription = "Not set.";
    @Getter
    private final String optionName;
    @Getter
    private final String description;
    @Getter
    private final OptionType type;
    @Getter
    private BiConsumer<GuildMessageReceivedEvent, String[]> eventConsumer;

    public Option(String displayName, String description, OptionType type) {
        this.optionName = displayName;
        this.description = description;
        this.type = type;
    }

    public static void addOption(String name, Option option) {
        Option.optionMap.put(name, option);
        String toAdd = String.format(
                "%-34s" + " | %s",
                name.replace(":", " "),
                getShortDescription()
        );
        Option.avaliableOptions.add(toAdd);
    }

    public Option setAction(Consumer<GuildMessageReceivedEvent> code) {
        eventConsumer = (event, ignored) -> code.accept(event);
        return this;
    }

    public Option setAction(BiConsumer<GuildMessageReceivedEvent, String[]> code) {
        eventConsumer = code;
        return this;
    }

    public Option setShortDescription(String sd) {
        shortDescription = sd;
        return this;
    }
}
