package net.kodehawa.mantarobot.commands;

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
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.HashMap;
import java.util.LinkedList;

public class GameCmds extends Module {

	public GameCmds() {
		super(Category.GAMES);
		guess();
		trivia();
	}

	private void guess() {
		super.register("guess", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if (content.isEmpty()) {
					onHelp(event);
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

				onHelp(event);
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Guessing games.")
					.addField("Games", "~>guess character: Starts a instance of Guess the character (anime).\n"
						+ "~>guess pokemon: Starts a instance of who's that pokemon?", false)
					.addField("Rules", "You have 10 attempts and 120 seconds to answer, otherwise the game ends", false)
					.build();
			}
		});
	}

	private void trivia() {
		super.register("trivia", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				startGame(new Trivia(), event);
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Trivia command.")
					.setDescription("Starts an instance of trivia.")
					.addField("Important", "You need to answer 10 questions correctly to win. You have 600 seconds to answer.", false)
					.build();
			}
		});
	}

	private void startGame(Game game, GuildMessageReceivedEvent event) {
		if (!GameLobby.LOBBYS.keySet().contains(event.getChannel())) {

			LinkedList<Game> list = new LinkedList<>();
			list.add(game);

			HashMap<Member, Player> map = new HashMap<>();
			map.put(event.getMember(), MantaroData.db().getPlayer(event.getMember()));
			if (!event.getMessage().getMentionedUsers().isEmpty()) {
				StringBuilder builder = new StringBuilder();
				event.getMessage().getMentionedUsers().forEach(user -> {
					map.put(event.getGuild().getMember(user), MantaroData.db().getPlayer(event.getGuild().getMember(user)));
					builder.append(user.getName()).append(" ");
				});

				event.getChannel().sendMessage(EmoteReference.MEGA + "Started a MP game with users: " + builder.toString()).queue();
			}

			GameLobby lobby = new GameLobby(event, map, list);
			lobby.startFirstGame();
		} else {
			event.getChannel().sendMessage(EmoteReference.ERROR + "There is a lobby already.").queue();
		}
	}
}
