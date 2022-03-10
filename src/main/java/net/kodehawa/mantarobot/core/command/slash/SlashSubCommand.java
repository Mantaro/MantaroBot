package net.kodehawa.mantarobot.core.command.slash;

import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public abstract class SlashSubCommand extends SlashCommand {
    private final SubcommandData data;
    public SlashSubCommand(SubcommandData data) {
        this.data = data;
    }

    public SubcommandData getData() {
        return data;
    }

    @Override
    protected abstract void process(SlashContext ctx);
}
