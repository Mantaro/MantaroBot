package net.kodehawa.mantarobot.modules.commands.base;

import net.kodehawa.mantarobot.modules.commands.CommandPermission;

public abstract class AbstractCommand implements AssistedCommand {
    private final Category category;
    private final CommandPermission permission;

    public AbstractCommand(Category category) {
        this(category, CommandPermission.USER);
    }

    public AbstractCommand(Category category, CommandPermission permission) {
        this.category = category;
        this.permission = permission;
    }

    @Override
    public Category category() {
        return category;
    }

    @Override
    public CommandPermission permission() {
        return permission;
    }
}
