package net.kodehawa.mantarobot.commands.game;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.commands.game.core.Game;
import net.kodehawa.mantarobot.commands.game.core.GameLobby;
import net.kodehawa.mantarobot.core.listeners.operations.core.InteractiveOperation;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.HashMap;
import java.util.Random;

public class GuessTheNumber extends Game<Object> {

    private final Random r = new Random();
    private int number = 0; //set to random number on game start
    private final int maxAttempts = 5;
    private int attempts = 1;

    @Override
    public void call(GameLobby lobby, HashMap<Member, Player> players) {
        //This class is not using Game<T>#callDefault due to it being custom/way too different from the default ones (aka give hints/etc)
        InteractiveOperations.createOverriding(lobby.getChannel(), 30, new InteractiveOperation() {
            @Override
            public int run(GuildMessageReceivedEvent e) {
                if (!e.getChannel().getId().equals(lobby.getChannel().getId())) {
                    return Operation.IGNORED;
                }

                for(String s : MantaroData.config().get().getPrefix()) {
                    if (e.getMessage().getContent().startsWith(s)) {
                        return Operation.IGNORED;
                    }
                }

                if (MantaroData.db().getGuild(lobby.getChannel().getGuild()).getData().getGuildCustomPrefix() != null &&
                        e.getMessage().getContent().startsWith(MantaroData.db().getGuild(lobby.getChannel().getGuild()).getData().getGuildCustomPrefix())) {
                    return Operation.IGNORED;
                }

                if (players.keySet().contains(e.getMember())) {
                    if (e.getMessage().getContent().equalsIgnoreCase("end")) {
                        lobby.getChannel().sendMessage(EmoteReference.CORRECT + "Ended game. The number was: " + number).queue();
                        lobby.startNextGame();
                        GameLobby.LOBBYS.remove(lobby.getChannel());
                        return Operation.COMPLETED;
                    }

                    int parsedAnswer = 0;

                    try{
                        parsedAnswer = Integer.parseInt(e.getMessage().getRawContent());
                    } catch (NumberFormatException ex){
                        lobby.getChannel().sendMessage(EmoteReference.ERROR + "That's not even a number...").queue();
                        attempts = attempts + 1;
                        return Operation.IGNORED;
                    }

                    if (e.getMessage().getRawContent().equals(String.valueOf(number))) {
                        Player player = MantaroData.db().getPlayer(e.getMember());
                        int gains = 80;
                        player.addMoney(gains);
                        player.save();
                        TextChannelGround.of(e).dropItemWithChance(Items.FLOPPY_DISK, 3);
                        lobby.getChannel().sendMessage(EmoteReference.MEGA + "**" + e.getMember().getEffectiveName() + "**" + " Just won $" + gains +" credits by answering correctly!").queue();
                        lobby.startNextGame();
                        return Operation.COMPLETED;
                    }

                    if (attempts >= maxAttempts) {
                        lobby.getChannel().sendMessage(EmoteReference.ERROR + "Already used all attempts, ending game. The number was: " + number).queue();
                        lobby.startNextGame(); //This should take care of removing the lobby, actually.
                        return Operation.COMPLETED;
                    }


                    lobby.getChannel().sendMessage(EmoteReference.ERROR + "That's not it, you have " +  (maxAttempts - attempts) + " attempts remaning.\n" +
                            "Hint: The number is " + (parsedAnswer < number ? "higher" : "lower") + " than your input number.").queue();
                    attempts = attempts + 1;
                    return Operation.IGNORED;
                }

                return Operation.IGNORED;
            }

            @Override
            public void onExpire() {
                lobby.getChannel().sendMessage(EmoteReference.ERROR + "The time ran out! The number was: " + number).queue();
                GameLobby.LOBBYS.remove(lobby.getChannel());
            }
        });
    }

    @Override
    public boolean onStart(GameLobby lobby) {
        number = r.nextInt(150);
        lobby.getChannel().sendMessage(EmoteReference.THINKING + "Guess the number! **You have 5 attempts and 30 seconds. The number is between 0 and 150**").queue();
        return true;
    }
}
