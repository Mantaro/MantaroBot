package net.kodehawa.mantarobot.commands;

import br.com.brjdevs.java.utils.extensions.Async;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.rpg.entity.player.EntityPlayer;
import net.kodehawa.mantarobot.commands.rpg.game.ImageGuess;
import net.kodehawa.mantarobot.commands.rpg.game.Pokemon;
import net.kodehawa.mantarobot.commands.rpg.game.Trivia;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

public class GameCmds extends Module {

	public GameCmds() {
		super(Category.GAMES);
		guess();
		trivia();
	}

	private void guess(){
		super.register("guess", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {

				if(content.isEmpty()){
					onHelp(event);
					return;
				}

				if(args[0].equals("image")){
					ImageGuess guess = new ImageGuess();
					EntityPlayer player = EntityPlayer.getPlayer(event);
					if(guess.check(event, guess.type())){
						if(event.getJDA().getRegisteredListeners().contains(guess)) event.getJDA().addEventListener(guess);
						else return;
						guess.onStart(event, guess.type(), player);
						Async.thread(120000, () -> {
							if(guess.check(event, guess.type())) return;
							if(EntityPlayer.getPlayer(event.getMember()).getGame() == null) return;
							event.getChannel().sendMessage(EmoteReference.THINKING + "No correct reply on 120 seconds, ending game. Correct reply was **" + guess.getCharacterName() + "**").queue();
							guess.endGame(event, player, guess, true);
						}).run();
					} else {
						event.getChannel().sendMessage(EmoteReference.SAD + "There is someone else playing the same game on this channel. Try later or in another one.").queue();
					}

					return;
				}

				if(args[0].equals("pokemon")){
					Pokemon pokemon = new Pokemon();
					EntityPlayer player = EntityPlayer.getPlayer(event);
					if(pokemon.check(event, pokemon.type())){
						if(event.getJDA().getRegisteredListeners().contains(pokemon)) event.getJDA().addEventListener(pokemon);
						else return;
						pokemon.onStart(event, pokemon.type(), player);
						Async.thread(120000, () -> {
							if(pokemon.check(event, pokemon.type())) return;
							if(EntityPlayer.getPlayer(event.getMember()).getGame() == null) return;
							event.getChannel().sendMessage(EmoteReference.THINKING + "No correct reply on 120 seconds, ending game. Correct reply was **" + pokemon.answer() + "**").queue();
							pokemon.endGame(event, player, pokemon, true);
						}).run();
					} else {
						event.getChannel().sendMessage(EmoteReference.SAD + "There is someone else playing the same game on this channel. Try later or in another one.").queue();
					}
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Guessing games.")
						.addField("Games", "~>guess image: Starts a instance of Guess the image, with anime characters.\n"
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
				Trivia trivia = new Trivia();
				EntityPlayer player = EntityPlayer.getPlayer(event);
				if(trivia.check(event, trivia.type())){
					if(event.getJDA().getRegisteredListeners().contains(trivia)) event.getJDA().addEventListener(trivia);
					else return;
					trivia.onStart(event, trivia.type(), player);
					Async.thread(120000, () -> {
						if(trivia.check(event, trivia.type())) return;
						if(EntityPlayer.getPlayer(event.getMember()).getGame() == null) return;
						event.getChannel().sendMessage(EmoteReference.THINKING + "No correct reply on 120 seconds, ending game. Correct reply was **" + trivia.answer() + "**").queue();
						trivia.endGame(event, player, trivia, true);
					}).run();
				} else {
					event.getChannel().sendMessage(EmoteReference.SAD + "There is someone else playing the same game on this channel. Try later or in another one.").queue();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Trivia command.")
						.setDescription("Starts an instance of trivia.")
						.addField("Important", "You need to answer 10 questions correctly to win. You have 120 seconds to answer.", false)
						.build();
			}
		});
	}
}
