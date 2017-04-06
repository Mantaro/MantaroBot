package net.kodehawa.mantarobot.commands;

import groovy.util.logging.Slf4j;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

public class GameCmds extends Module {
	private static final Logger LOGGER = LoggerFactory.getLogger("GameCmds");

	public GameCmds() {
		super(Category.GAMES);
		guess();
		trivia();
	}

	private void guess() {
		super.register("game", new SimpleCommand() {
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

				if(args[0].equals("lobby")){
					try{
						//TODO fix... well this used to work and I somewhat broke it but it's 1am so cba lol
						LinkedList<Game> list = new LinkedList<>();
						HashMap<Member, Player> map = new HashMap<>();
						String games = args[1];
						String[] toPlay = games.split(",");
						for (String s : Arrays.asList(toPlay)) {
							if(GameLobby.getTextRepresentation().get(s) != null) list.add(GameLobby.getTextRepresentation().get(s));
						}

						StringBuilder builder = new StringBuilder();
						event.getMessage().getMentionedUsers().forEach(user -> {
							if(!user.getId().equals(event.getJDA().getSelfUser().getId()))
							map.put(event.getGuild().getMember(user), MantaroData.db().getPlayer(event.getGuild().getMember(user)));
							builder.append(user.getName()).append(" ");
						});

						GameLobby lobby = new GameLobby(event, map, list);
						event.getChannel().sendMessage(EmoteReference.MEGA + "Created lobby with games: " + games + " and users: " + builder.toString() + "successfully.").queue();
						lobby.startFirstGame();
						return;
					} catch (Exception e){
						if((e instanceof IndexOutOfBoundsException)){
							event.getChannel().sendMessage(EmoteReference.ERROR + "Incorrect type arguments.").queue();
						} else {
							event.getChannel().sendMessage(EmoteReference.ERROR + "Error while setting up the lobby.").queue();
							LOGGER.warn("Error while setting up a lobby", e);
						}
					}
					return;
				}

				onHelp(event);
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Guessing games.")
					.addField("Games", "~>game character: Starts a instance of Guess the character (anime).\n"
						+ "~>game pokemon: Starts a instance of who's that pokemon?", false)
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
					if(!user.getId().equals(event.getJDA().getSelfUser().getId()))
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
