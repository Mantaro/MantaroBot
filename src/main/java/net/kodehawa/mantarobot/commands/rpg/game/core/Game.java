package net.kodehawa.mantarobot.commands.rpg.game.core;

import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.rpg.world.TextChannelWorld;
import net.kodehawa.mantarobot.data.entities.Player;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public abstract class Game {
	private ScheduledExecutorService timer;
	private UUID id;
	private List<Player> guestPlayers;
	//public static final DataManager<List<String>> hangmanWords = new SimpleFileDataManager("assets/mantaro/texts/hangman.txt");
	public Game(){
		timer = Executors.newSingleThreadScheduledExecutor();
		id = new UUID(System.currentTimeMillis(), this.hashCode());
		guestPlayers = new ArrayList<>();
	}

	public abstract void call(GuildMessageReceivedEvent event, Player player);

	public abstract boolean onStart(GuildMessageReceivedEvent event, GameReference type, Player player);

	public abstract GameReference type();

	public boolean check(GuildMessageReceivedEvent event) {
		return TextChannelWorld.of(event.getChannel()).getRunningGames().isEmpty();
	}

	protected void onStart(TextChannelWorld world, GuildMessageReceivedEvent event, Player player){
		guestPlayers.add(player);
		/*timer.schedule(() -> {
			if(!world.getRunningGames().isEmpty() && player.getCurrentGame() != null && player.getCurrentGame().id == id){
				endGame(event, player, true);
			}
		}, 60, TimeUnit.SECONDS);*/
	}

	private void endGame(TextChannelWorld world, GuildMessageReceivedEvent event, Player player, boolean isTimeout) {
		/*player.setCurrentGame(null, event.getChannel());
		player.setGameInstance(this);
		String toSend = isTimeout ? EmoteReference.THINKING + "No correct reply on 60 seconds, ending game." : EmoteReference.CORRECT + "Game has correctly ended.";
		event.getChannel().sendMessage(toSend).queue();
		world.getRunningGames().clear();
		getGuestPlayers().clear();
		getCurrentTimer().shutdownNow();*/
	}

	protected void endGame(GuildMessageReceivedEvent event, Player player, boolean isTimeout) {
		/*player.setCurrentGame(null, event.getChannel());
		player.setGameInstance(null);
		String toSend = isTimeout ? EmoteReference.THINKING + "No correct reply on 60 seconds, ending game." : EmoteReference.CORRECT + "Game has correctly ended.";
		event.getChannel().sendMessage(toSend).queue();
		TextChannelWorld.of(event.getChannel()).getRunningGames().clear();
		getGuestPlayers().clear();
		getCurrentTimer().shutdownNow();*/
	}

	protected void onError(Logger logger, GuildMessageReceivedEvent event, Player player, Exception e) {
		/*TextChannelWorld world = TextChannelWorld.of(event.getChannel());
		event.getChannel().sendMessage(EmoteReference.ERROR + "We cannot start this game due to an unknown error. My owners have been notified.").queue();
		if (e == null) logger.error("Error while setting up/handling a game");
		else logger.error("Error while setting up/handling a game", e);
		endGame(world, event, player, false);
		if(!world.getRunningGames().isEmpty()) world.getRunningGames().clear(); //You might think this is redundant, but it actually happens.
		timer.shutdown();*/
	}

	protected void onSuccess(Player player, GuildMessageReceivedEvent event, double multiplier) {
		/*TextChannelWorld world = TextChannelWorld.of(event.getChannel());
		/long moneyAward = Math.min((long) ((player.getMoney() * multiplier) + new Random().nextInt(350)), 1000);
		event.getChannel().sendMessage(EmoteReference.OK + "That's the correct answer, you won " + moneyAward + " credits for this.").queue();
		player.addMoney(moneyAward);
		endGame(world, event, player, false);
		player.save();
		if(!world.getRunningGames().isEmpty()) world.getRunningGames().clear();*/
	}

	protected void onSuccess(Player player, GuildMessageReceivedEvent event) {
		/*TextChannelWorld world = TextChannelWorld.of(event.getChannel());
		long moneyAward = Math.min((long) ((player.getMoney() * 0.1) + new Random().nextInt(350)), 1000);
		event.getChannel().sendMessage(EmoteReference.OK + "That's the correct answer, you won " + moneyAward + " credits for this.").queue();
		player.addMoney(moneyAward);
		endGame(world, event, player, false);
		player.save();
		if(!world.getRunningGames().isEmpty()) world.getRunningGames().clear();*/
	}

	protected ScheduledExecutorService getCurrentTimer(){
		return timer;
	}

	public UUID getId(){
		return id;
	}

	public List<Player> getGuestPlayers(){
		return guestPlayers;
	}

	public void addPlayer(Player player){
		guestPlayers.add(player);
	}
}