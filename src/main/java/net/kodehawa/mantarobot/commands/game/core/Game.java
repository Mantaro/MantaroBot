package net.kodehawa.mantarobot.commands.game.core;

import net.dv8tion.jda.core.entities.Member;

import java.util.List;

public abstract class Game {

	public abstract boolean onStart(GameLobby lobby, List<Member> player);

	public abstract void call(GameLobby lobby, List<Member> players);

}
