package net.kodehawa.mantarobot.modules.commands.base;

public abstract class AbstractCommand implements AssistedCommand {
	private final Category category;

	public AbstractCommand(Category category) {
		this.category = category;
	}

	@Override
	public Category category() {
		return category;
	}
}
