package net.kodehawa.mantarobot.commands;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.game.ImageGuess;
import net.kodehawa.mantarobot.commands.game.Pokemon;
import net.kodehawa.mantarobot.commands.game.Trivia;
import net.kodehawa.mantarobot.commands.game.core.Game;
import net.kodehawa.mantarobot.commands.game.core.GameLobby;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.data.entities.Player;
import net.kodehawa.mantarobot.modules.*;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

@Slf4j
@RegisterCommand.Class
public class GameCmds {

	@RegisterCommand
	public static void guess(CommandRegistry cr) {
		cr.register("game", SimpleCommand.builder(Category.GAMES)
				.permission(CommandPermission.USER)
				.code((thiz, event, content, args) -> {
					if (content.isEmpty()) {
						thiz.onHelp(event);
						return;
					}

					if (args[0].equals("character")) {
						startGame(new ImageGuess(), event);
						return;
					}

					if (args[0].equals("pokemon")) {
						startGame(new Pokemon(), event);
						return;
					}

					if (args[0].equals("lobby")) {
						try {
							//TODO fix... well this used to work and I somewhat broke it but it's 1am so cba lol
							LinkedList<Game> list = new LinkedList<>();
							HashMap<Member, Player> map = new HashMap<>();
							String games = args[1];
							String[] toPlay = games.split(",");
							for (String s : Arrays.asList(toPlay)) {
								if (GameLobby.getTextRepresentation().get(s) != null)
									list.add(GameLobby.getTextRepresentation().get(s));
							}

							StringBuilder builder = new StringBuilder();
							event.getMessage().getMentionedUsers().forEach(user -> {
								if (!user.getId().equals(event.getJDA().getSelfUser().getId()))
									map.put(event.getGuild().getMember(user), MantaroData.db().getPlayer(event.getGuild().getMember(user)));
								builder.append(user.getName()).append(" ");
							});

							GameLobby lobby = new GameLobby(event, map, list);
							event.getChannel().sendMessage(EmoteReference.MEGA + "Created lobby with games: " + games + " and users: " +
									builder.toString() + "successfully.").queue();
							lobby.startFirstGame();
							return;
						} catch (Exception e) {
							if ((e instanceof IndexOutOfBoundsException)) {
								event.getChannel().sendMessage(EmoteReference.ERROR + "Incorrect type arguments.").queue();
							} else {
								event.getChannel().sendMessage(EmoteReference.ERROR + "Error while setting up the lobby.").queue();
								log.warn("Error while setting up a lobby", e);
							}
						}
						return;
					}

					thiz.onHelp(event);
				})
				.help((thiz, event) -> thiz.helpEmbed(event, "Guessing games.")
						.addField("Games", "~>game character: Starts a instance of Guess the character (anime).\n"
								+ "~>game pokemon: Starts a instance of who's that pokemon?", false)
						.addField("Rules", "You have 10 attempts and 120 seconds to answer, otherwise the game ends", false)
						.build())
				.build());
	}

	private static void startGame(Game game, GuildMessageReceivedEvent event) {
		//TODO The Timeout is done (Override the (default) onExpire on InteractiveOperation), do the above ^^
		if(GameLobby.LOBBYS.containsKey(event.getChannel())){
			event.getChannel().sendMessage(EmoteReference.ERROR + "Cannot start a new game when there is a game currently running.").queue();
			return;
		}

		LinkedList<Game> list = new LinkedList<>();
		list.add(game);

		HashMap<Member, Player> map = new HashMap<>();
		map.put(event.getMember(), MantaroData.db().getPlayer(event.getMember()));
		if (!event.getMessage().getMentionedUsers().isEmpty()) {
			StringBuilder builder = new StringBuilder();
			event.getMessage().getMentionedUsers().forEach(user -> {
				if (!user.getId().equals(event.getJDA().getSelfUser().getId()))
					map.put(event.getGuild().getMember(user), MantaroData.db().getPlayer(event.getGuild().getMember(user)));
				builder.append(user.getName()).append(" ");
			});

			event.getChannel().sendMessage(EmoteReference.MEGA + "Started a MP game with users: " + builder.toString()).queue();
		}

		GameLobby lobby = new GameLobby(event, map, list);

		lobby.startFirstGame();
	}

	@RegisterCommand
	public static void trivia(CommandRegistry cr) {
		cr.register("name", SimpleCommand.builder(Category.GAMES)
				.permission(CommandPermission.USER)
				.code(event -> startGame(new Trivia(), event))
				.help((thiz, event) -> thiz.helpEmbed(event, "Trivia command.")
						.setDescription("Starts an instance of trivia.")
						.build())
				.build());
	}
}
