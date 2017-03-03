package net.kodehawa.mantarobot.commands;

import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.entity.player.EntityPlayer;
import net.kodehawa.mantarobot.commands.currency.game.GameReference;
import net.kodehawa.mantarobot.commands.currency.game.ImageGuess;
import net.kodehawa.mantarobot.modules.Category;
import net.kodehawa.mantarobot.modules.Module;
import net.kodehawa.mantarobot.modules.SimpleCommand;
import net.kodehawa.mantarobot.utils.Async;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

public class GameCmds extends Module {

	public GameCmds() {
		super(Category.GAMES);
		guess();
	}

	private void guess(){
		super.register("guess", new SimpleCommand() {
			@Override
			protected void call(String[] args, String content, GuildMessageReceivedEvent event) {
				if(args[0].equals("image")){
					ImageGuess guess = new ImageGuess();
					EntityPlayer player = EntityPlayer.getPlayer(event.getAuthor());
					if(guess.check(event, player.getGame())){
						event.getJDA().addEventListener(guess);
						guess.onStart(event, GameReference.IMAGEGUESS, player);
					}

					Async.asyncSleepThen(60000, () -> {
						event.getChannel().sendMessage(EmoteReference.THINKING + "No correct reply on 60 seconds, ending game. Correct reply was **" + guess.getCharacterName() + "**").queue();
						guess.endGame(event, player, false);
					}).run();
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
}
