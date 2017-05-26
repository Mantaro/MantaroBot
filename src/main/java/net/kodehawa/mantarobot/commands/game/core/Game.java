package net.kodehawa.mantarobot.commands.game.core;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.data.entities.Player;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.HashMap;

public abstract class Game {
	@Setter
	@Getter
	private int attempts = 1;

	public abstract void call(GameLobby lobby, HashMap<Member, Player> players);

	public abstract boolean onStart(GameLobby lobby);

	protected boolean callDefault(GuildMessageReceivedEvent e, GameLobby lobby, HashMap<Member, Player> players, String expectedAnswer, int attempts, int maxAttempts) {
		if (!e.getChannel().getId().equals(lobby.getChannel().getId())) {
			return false;
		}

		if (e.getMessage().getContent().startsWith(MantaroData.config().get().getPrefix())) {
			return false;
		}

		if (MantaroData.db().getGuild(lobby.getChannel().getGuild()).getData().getGuildCustomPrefix() != null &&
			e.getMessage().getContent().startsWith(MantaroData.db().getGuild(lobby.getChannel().getGuild()).getData().getGuildCustomPrefix())) {
			return false;
		}

		if (players.keySet().contains(e.getMember())) {
			if (e.getMessage().getContent().equalsIgnoreCase("end")) {
				lobby.getChannel().sendMessage(EmoteReference.CORRECT + "Ended game. Answer was: " + expectedAnswer).queue();
				lobby.startNextGame();
				GameLobby.LOBBYS.remove(lobby.getChannel());
				return true;
			}

			if (attempts > maxAttempts) {
				lobby.getChannel().sendMessage(EmoteReference.ERROR + "Already used all attempts, ending game. Answer was: " + expectedAnswer).queue();
				lobby.startNextGame(); //This should take care of removing the lobby, actually.
				return true;
			}

			if (e.getMessage().getContent().equalsIgnoreCase(expectedAnswer)) {
				Player player = players.get(e.getMember());
				player.addMoney(10);
				player.save();
				TextChannelGround.of(e).dropItemWithChance(Items.FLOPPY_DISK, 3);
				lobby.getChannel().sendMessage(EmoteReference.MEGA + "**" + e.getMember().getEffectiveName() + "**" + " Just won $10 credits by answering correctly!").queue();
				lobby.startNextGame();
				return true;
			}

			lobby.getChannel().sendMessage(EmoteReference.ERROR + "That's not it, you have " + (maxAttempts - attempts) + " attempts remaning.").queue();
			setAttempts(getAttempts() + 1);
			return false;
		}

		return false;
	}
}
