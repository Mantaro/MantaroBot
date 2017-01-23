package net.kodehawa.mantarobot.module;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public interface Callback extends Container {
	void onCommand(String[] args, String content, GuildMessageReceivedEvent event);
}
