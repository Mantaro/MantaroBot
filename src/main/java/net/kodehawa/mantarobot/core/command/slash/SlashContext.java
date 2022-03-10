package net.kodehawa.mantarobot.core.command.slash;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;

import java.util.EnumSet;
import java.util.List;

public class SlashContext {
    private final ManagedDatabase managedDatabase = MantaroData.db();
    private final Config config = MantaroData.config().get();
    private final SlashCommandEvent slash;
    private final I18nContext i18n;

    public SlashContext(SlashCommandEvent event, I18nContext i18n) {
        this.slash = event;
        this.i18n = i18n;
    }

    public I18nContext getI18nContext() {
        return i18n;
    }

    public String getName() {
        return slash.getName();
    }

    public String getSubCommand() {
        return slash.getSubcommandName();
    }

    public OptionMapping getOption(String name) {
        return slash.getOption(name);
    }

    // This is a little cursed, but I guess we can make do.
    public List<OptionMapping> getOptions() {
        return slash.getOptions();
    }

    public TextChannel getChannel() throws IllegalStateException {
        return slash.getTextChannel();
    }

    public Member getMember() {
        return slash.getMember();
    }

    public User getAuthor() {
        return slash.getUser();
    }

    public Guild getGuild() {
        return getChannel().getGuild();
    }

    public void reply(String source, Object... args) {
        slash.reply(i18n.get(source).formatted(args))
                .allowedMentions(EnumSet.noneOf(Message.MentionType.class))
                .queue();;
    }

    public void reply(String text) {
        slash.reply(text)
                .allowedMentions(EnumSet.noneOf(Message.MentionType.class))
                .queue();
    }

    public ManagedDatabase getDatabase() {
        return managedDatabase;
    }

    public Config getConfig() {
        return config;
    }
}
