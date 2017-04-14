package net.kodehawa.mantarobot.modules;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public abstract class SimpleCommandCompat implements SimpleCommand {
    private final Category category;
    private final String description;

    public SimpleCommandCompat(Category category, String description) {
        this.category = category;
        this.description = description;
    }

    @Override
    public CommandPermission permission() {
        return permissionRequired();
    }

    public CommandPermission permissionRequired() {
        return CommandPermission.USER;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public Category category() {
        return category;
    }

    @Override
    public boolean isHiddenFromHelp() {
        return false;
    }

    @Override
    public void call(GuildMessageReceivedEvent event, String commandName, String[] args) {
        call(args, commandName, event);
    }

    protected abstract void call(String[] args, String name, GuildMessageReceivedEvent event);
}
