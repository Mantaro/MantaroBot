package net.kodehawa.mantarobot.core.command.slash;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.interactions.AutoCompleteQuery;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.I18n;

import java.util.List;

public class AutocompleteContext {
    private final CommandAutoCompleteInteractionEvent event;
    private final I18nContext i18n;

    public AutocompleteContext(CommandAutoCompleteInteractionEvent event) {
        this.event = event;
        this.i18n = new I18nContext(I18n.of(event.getUserLocale()));
    }

    public CommandAutoCompleteInteractionEvent getEvent() {
        return event;
    }

    public I18nContext getI18n() {
        return i18n;
    }

    public AutoCompleteQuery getFocused() {
        return event.getFocusedOption();
    }

    public void replyChoices(List<Command.Choice> choices) {
        if (choices.size() > OptionData.MAX_CHOICES) {
            choices = choices.subList(0, OptionData.MAX_CHOICES + 1);
        }
        event.replyChoices(choices).queue();
    }

    public OptionMapping getOption(String name) {
        return event.getOption(name);
    }

    public String getSubCommand() {
        return event.getSubcommandName();
    }
}
