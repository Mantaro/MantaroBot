package net.kodehawa.mantarobot.commands.game.core;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.data.entities.Player;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.HashMap;
import java.util.List;

public abstract class Game {
	@Setter
	@Getter
	private int attempts = 1;

	public abstract boolean onStart(GameLobby lobby);

	public abstract void call(GameLobby lobby, HashMap<Member, Player> players);

	protected boolean callDefault(GuildMessageReceivedEvent e, GameLobby lobby, HashMap<Member, Player> players, String expectedAnswer, int attempts, int maxAttempts){
		if(!e.getChannel().getId().equals(lobby.getChannel().getId())){
			return false;
		}

		if (e.getMessage().getContent().startsWith(MantaroData.db().getGuild(lobby.getChannel().getGuild()).getData().getGuildCustomPrefix())
				|| e.getMessage().getContent().startsWith(MantaroData.config().get().getPrefix())) {
			return false;
		}

		if(players.keySet().contains(e.getMember())) {
			if (e.getMessage().getContent().equalsIgnoreCase("end")) {
				lobby.getChannel().sendMessage(EmoteReference.CORRECT + "Ended game.").queue();
				if (lobby.startNextGame()) {
					lobby.getChannel().sendMessage("Starting next game...").queue();
				}
				return true;
			}

			if (attempts >= maxAttempts) {
				lobby.getChannel().sendMessage(EmoteReference.ERROR + "Already used all attempts, ending game. Answer was: " + expectedAnswer).queue();
				if (lobby.startNextGame()) {
					lobby.getChannel().sendMessage("Starting next game...").queue();
				}
				return true;
			}
			if (e.getMessage().getContent().equalsIgnoreCase(expectedAnswer)) {
				Player player = players.get(e.getMember());
				player.addMoney(150);
				player.save();
				lobby.getChannel().sendMessage(EmoteReference.MEGA + "**" + e.getMember().getEffectiveName() + "**" + " Just won $150 credits by answering correctly!").queue();
				if (lobby.startNextGame()) {
					lobby.getChannel().sendMessage("Starting next game...").queue();
				}
				return true;
			}

			lobby.getChannel().sendMessage(EmoteReference.ERROR + "That's not it, you have " + (maxAttempts - attempts) + " attempts remaning.").queue();
			setAttempts(getAttempts() + 1);
			return false;
		}

		return false;
	}
}
