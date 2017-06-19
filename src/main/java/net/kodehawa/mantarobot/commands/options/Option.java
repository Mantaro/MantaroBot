package net.kodehawa.mantarobot.commands.options;

import com.google.common.base.Preconditions;
import lombok.Getter;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Option {

    public static Map<String, Option> optionMap = new HashMap<>();

    @Getter private final String optionName;
    @Getter private final String description;
    @Getter private final OptionType type;
    @Getter private BiConsumer<GuildMessageReceivedEvent, String[]> eventConsumer;

    public Option(String displayName, String description, OptionType type){
        this.optionName = displayName;
        this.description = description;
        this.type = type;
    }

    public Option setAction(Consumer<GuildMessageReceivedEvent> code){
        eventConsumer = (event, ignored) -> code.accept(event);
        return this;
    }

    public Option setAction(BiConsumer<GuildMessageReceivedEvent, String[]> code){
        eventConsumer = code;
        return this;
    }

    public BiConsumer<GuildMessageReceivedEvent, String[]> getOptionByCallable(String optionName) {
        BiConsumer<GuildMessageReceivedEvent, String[]> c = optionMap.get(optionName).getEventConsumer();
        Preconditions.checkNotNull(c, "Callable for " + optionName + "isn't set!");
        return c;
    }
}
