package net.kodehawa.mantarobot.commands.interaction;

import lombok.Getter;
import net.dv8tion.jda.core.entities.TextChannel;

public class Lobby {
	@Getter
	private final TextChannel channel;

	public Lobby(TextChannel channel) {
		this.channel = channel;
	}
}
