package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.game.Character;
import net.kodehawa.mantarobot.commands.game.Pokemon;
import net.kodehawa.mantarobot.commands.game.Trivia;
import net.kodehawa.mantarobot.commands.game.core.Game;
import net.kodehawa.mantarobot.commands.game.core.GameLobby;
import net.kodehawa.mantarobot.commands.interaction.polls.Poll;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.modules.CommandRegistry;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.PostLoadEvent;
import net.kodehawa.mantarobot.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.modules.commands.base.Category;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.HashMap;
import java.util.LinkedList;

@Slf4j
@Module
public class GameCmds {

	@Subscribe
	public static void guess(CommandRegistry cr) {
		cr.register("game", new SimpleCommand(Category.GAMES) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				if (content.isEmpty()) {
					onError(event);
					return;
				}

				if (args[0].equalsIgnoreCase("character")) {
					startGame(new Character(), event);
					return;
				}

				if (args[0].equalsIgnoreCase("pokemon") || args[0].equalsIgnoreCase("pokémon")) {
					startGame(new Pokemon(), event);
					return;
				}

				onHelp(event);
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Guessing games.")
					.addField("Games", "`~>game character` - **Starts a instance of Guess the character (anime)**.\n"
						+ "`~>game pokemon` - **Starts a instance of who's that pokemon?**", false)
					.addField("Rules", "You have 10 attempts and 120 seconds to answer, otherwise the game ends", false)
					.addField("Considerations", "The pokemon guessing game has around 900 different pokemons to guess, where the anime guessing game has around 60.", false)
					.build();
			}
		});
	}

	@Subscribe
	public static void trivia(CommandRegistry cr) {
		cr.register("trivia", new SimpleCommand(Category.GAMES) {
			@Override
			protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
				startGame(new Trivia(), event);
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Trivia command.")
					.setDescription("**Starts an instance of trivia.**")
					.addField("Rules", "You have 10 attempts and 120 seconds to answer, otherwise the game ends", false)
					.build();
			}
		});
	}

	private static void startGame(Game game, GuildMessageReceivedEvent event) {
		if (GameLobby.LOBBYS.containsKey(event.getChannel())) {
			event.getChannel().sendMessage(EmoteReference.ERROR + "Cannot start a new game when there is a game currently running.").queue();
			return;
		}

		LinkedList<Game> list = new LinkedList<>();
		list.add(game);

		HashMap<Member, Player> map = new HashMap<>();
		map.put(event.getMember(), MantaroData.db().getPlayer(event.getMember()));


		if(!event.getMessage().getMentionedRoles().isEmpty()) {
			StringBuilder b = new StringBuilder();
			event.getMessage().getMentionedRoles().forEach(role ->
				event.getGuild().getMembersWithRoles(role).forEach(user  -> {
					if (!user.getUser().getId().equals(event.getJDA().getSelfUser().getId()))
						map.put(user, MantaroData.db().getPlayer(user));
					b.append(user.getEffectiveName()).append(" ");
				})
			);
			event.getChannel().sendMessage(EmoteReference.MEGA + "Started a MP game with all users with the specfied role: " + b.toString()).queue();
		}

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
}
