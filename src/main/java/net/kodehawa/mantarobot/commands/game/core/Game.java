package net.kodehawa.mantarobot.commands.game.core;

import net.dv8tion.jda.core.entities.Member;

import java.util.List;

public abstract class Game {

	public abstract boolean onStart(Lobby lobby, List<Member> player);

}
