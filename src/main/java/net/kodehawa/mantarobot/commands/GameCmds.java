package net.kodehawa.mantarobot.commands;

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
		hangman();
	}

	private void guess(){
		super.register("guess", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if(args[0].equals("image")){
					ImageGuess guess = new ImageGuess();
					EntityPlayer player = EntityPlayer.getPlayer(event);
					if(guess.check(event, guess.type())){
						event.getJDA().addEventListener(guess);
						guess.onStart(event, guess.type(), player);
					} else {
						event.getChannel().sendMessage(EmoteReference.SAD + "There is someone else playing the same game on this channel. Try later or in another one.").queue();
					}

					//TODO Make it actually work.
					/*Async.asyncSleepThen(60000, () -> {
						if(!guess.check(event, GameReference.IMAGEGUESS)) return;
						if(EntityPlayer.getPlayer(event.getAuthor()).getGame() == null) return;
						event.getChannel().sendMessage(EmoteReference.THINKING + "No correct reply on 60 seconds, ending game. Correct reply was **" + guess.getCharacterName() + "**").queue();
						guess.endGame(event, player, false);
					}).run();*/
					return;
				}

				if(args[0].equals("pokemon")){
					Pokemon pokemon = new Pokemon();
					EntityPlayer player = EntityPlayer.getPlayer(event);
					if(pokemon.check(event, pokemon.type())){
						event.getJDA().addEventListener(pokemon);
						pokemon.onStart(event, pokemon.type(), player);
					} else {
						event.getChannel().sendMessage(EmoteReference.SAD + "There is someone else playing the same game on this channel. Try later or in another one.").queue();
					}
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return helpEmbed(event, "Guessing games.")
						.addField("Games", "~>guess image: Starts a instance of Guess the image, with anime characters.", false)
						.addField("Rules", "You have 10 attempts and 60 seconds to answer, otherwise the game ends", false)
						.build();
			}
		});
	}

	private void hangman() {
		super.register("hangman", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {

			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return null;
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
					event.getJDA().addEventListener(trivia);
					trivia.onStart(event, trivia.type(), player);
				} else {
					event.getChannel().sendMessage(EmoteReference.SAD + "There is someone else playing the same game on this channel. Try later or in another one.").queue();
				}
			}

			@Override
			public MessageEmbed help(GuildMessageReceivedEvent event) {
				return null;
			}
		});
	}
}
