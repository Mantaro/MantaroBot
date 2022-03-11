package net.kodehawa.mantarobot.core.command.slash;

import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public abstract class SlashSubCommand extends SlashCommand {
    private final SubcommandData data;
    public SlashSubCommand() {
        super(); // We need to init it like a normal command.
        data = new SubcommandData(super.getName(), super.getDescription());
        data.addOptions(super.getOptions());
    }

    public SubcommandData getData() {
        return data;
    }

    @Override
    protected abstract void process(SlashContext ctx);
}
