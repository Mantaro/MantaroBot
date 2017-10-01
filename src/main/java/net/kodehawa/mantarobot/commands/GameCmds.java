/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.game.Character;
import net.kodehawa.mantarobot.commands.game.GuessTheNumber;
import net.kodehawa.mantarobot.commands.game.Pokemon;
import net.kodehawa.mantarobot.commands.game.Trivia;
import net.kodehawa.mantarobot.commands.game.core.Game;
import net.kodehawa.mantarobot.commands.game.core.GameLobby;
import net.kodehawa.mantarobot.core.CommandRegistry;

import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SimpleTreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Module
public class GameCmds {

    private final Pattern mentionPattern = Pattern.compile("<(#|@|@&)?.[0-9]{17,21}>");

    //addSubCommand()...
    @Subscribe
    public void guess(CommandRegistry cr) {
        cr.register("game", new SimpleTreeCommand(Category.GAMES) {
            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Guessing games.")
                        .addField("Games", "`~>game character` - **Starts an instance of Guess the character (anime)**.\n"
                                + "`~>game pokemon` - **Starts an instance of who's that pokemon?**\n" +
                                "`~>game number` - **Starts an instance of Guess The Number**`\n" +
                                "`~>game lobby` - **Starts a chunk of different games, for example `~>game lobby pokemon, trivia` will start pokemon and then trivia.**", false)
                        .addField("Considerations", "The pokemon guessing game has around 900 different pokemon to guess, " +
                                "where the anime guessing game has around 60. The number in the number guessing game is a random number between 0 and 150.", false)
                        .build();
            }
        }.addSubCommand("character", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content) {
                startGame(new Character(), event);
            }
        }).addSubCommand("pokemon", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content) {
                startGame(new Pokemon(), event);
            }
        }).addSubCommand("number", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content) {
                startGame(new GuessTheNumber(), event);
            }
        }).addSubCommand("lobby", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content) {
                if(content.isEmpty()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You didn't specify anything to play!").queue();
                    return;
                }

                //Stripe all mentions from this.
                String[] split = mentionPattern.matcher(content).replaceAll("").split(", ");

                if(split.length < 1 || split.length == 1) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You need to specify two games at least!").queue();
                    return;
                }

                LinkedList<Game> gameList = new LinkedList<>();
                for(String s : split) {
                    switch (s.replace(" ", "")) {
                        case "pokemon":
                            gameList.add(new Pokemon());
                            break;
                        case "trivia":
                            gameList.add(new Trivia(null));
                            break;
                        case "number":
                            gameList.add(new GuessTheNumber());
                            break;
                        case "character":
                            gameList.add(new Character());
                            break;
                    }
                }

                if(gameList.isEmpty() || gameList.size() == 1) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You need to specify two games at least (Valid games: character, pokemon, number, trivia)!").queue();
                    return;
                }

                startMultipleGames(gameList, String.join(", ", split), event);
            }
        }).createSubCommandAlias("pokemon", "pokémon")
          .createSubCommandAlias("number", "guessthatnumber"));
    }

    @Subscribe
    public void trivia(CommandRegistry cr) {
        cr.register("trivia", new SimpleCommand(Category.GAMES) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                String difficulty = null;

                if(args.length > 0) {
                    difficulty = args[0];
                }

                if(difficulty != null && !(difficulty.equals("easy") || difficulty.equals("hard") || difficulty.equals("medium"))) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "Wrong difficulty specified! (Supported: easy, medium and hard)").queue();
                    return;
                }

                startGame(new Trivia(difficulty), event);
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Trivia command.")
                        .setDescription("**Starts an instance of trivia.**\n" +
                                "Optionally, you can specify the difficulty (easy, medium or hard) to play.")
                        .addField("Rules", "You have 10 attempts and 60 seconds to answer, otherwise the game ends.", false)
                        .build();
            }
        });
    }

    private void startMultipleGames(LinkedList<Game> games, String names, GuildMessageReceivedEvent event) {
        if(checkRunning(event)) return;

        List<String> players = new ArrayList<>();
        players.add(event.getAuthor().getId());

        if(!event.getMessage().getMentionedRoles().isEmpty()) {
            StringBuilder b = new StringBuilder();
            event.getMessage().getMentionedRoles().forEach(role ->
                    event.getGuild().getMembersWithRoles(role).forEach(user  -> {
                        if (!user.getUser().getId().equals(event.getJDA().getSelfUser().getId()))
                            players.add(user.getUser().getId());
                        b.append(user.getEffectiveName()).append(" ");
                    })
            );
            event.getChannel().sendMessage(EmoteReference.MEGA + "Started a MP lobby with all users with the specfied role: " + b.toString()).queue();
        }

        if (!event.getMessage().getMentionedUsers().isEmpty()) {
            StringBuilder builder = new StringBuilder();
            event.getMessage().getMentionedUsers().forEach(user -> {
                if (!user.getId().equals(event.getJDA().getSelfUser().getId()) && !user.isBot())
                    players.add(user.getId());
                builder.append(user.getName()).append(" ");
            });

            if(players.size() > 1) {
                event.getChannel().sendMessage(EmoteReference.MEGA + "Started a MP lobby with users: " + builder.toString()).queue();
            }
        }

        event.getChannel().sendMessage(EmoteReference.CORRECT + "Started a new lobby! **Games: " + names + "**").queue();
        GameLobby lobby = new GameLobby(event, players, games);
        lobby.startFirstGame();
    }

    private void startGame(Game game, GuildMessageReceivedEvent event) {
        if(checkRunning(event)) return;

        LinkedList<Game> list = (LinkedList<Game>) Collections.singletonList(game);

		List<String> players = new ArrayList<>();
		players.add(event.getAuthor().getId());

		if(!event.getMessage().getMentionedRoles().isEmpty()) {
			StringBuilder b = new StringBuilder();
			event.getMessage().getMentionedRoles().forEach(role ->
				event.getGuild().getMembersWithRoles(role).forEach(user  -> {
					if (!user.getUser().getId().equals(event.getJDA().getSelfUser().getId()))
						players.add(user.getUser().getId());
					b.append(user.getEffectiveName()).append(" ");
				})
			);
			event.getChannel().sendMessage(EmoteReference.MEGA + "Started a MP game with all users with the specfied role: " + b.toString()).queue();
		}

		if (!event.getMessage().getMentionedUsers().isEmpty()) {
			StringBuilder builder = new StringBuilder();
			event.getMessage().getMentionedUsers().forEach(user -> {
                if (!user.getId().equals(event.getJDA().getSelfUser().getId()) && !user.isBot())
					players.add(user.getId());
				builder.append(user.getName()).append(" ");
			});

			if(players.size() > 1) {
                event.getChannel().sendMessage(EmoteReference.MEGA + "Started a MP game with users: " + builder.toString()).queue();
            }
        }

		GameLobby lobby = new GameLobby(event, players, list);
        lobby.startFirstGame();
    }

    private boolean checkRunning(GuildMessageReceivedEvent event) {
        if(GameLobby.LOBBYS.containsKey(event.getChannel())) {
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            if (dbGuild.getData().getGameTimeoutExpectedAt() != null &&
                    (Long.parseLong(dbGuild.getData().getGameTimeoutExpectedAt()) > System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(75))) {
                event.getChannel().sendMessage(EmoteReference.ERROR + "Seems like I dropped a game here, " +
                        "but forgot to pick it up... I'll start your new game right up!").queue();
                return false;
            } else {
                event.getChannel().sendMessage(EmoteReference.ERROR + "Cannot start a new game lobby when there is a game currently running.").queue();
                return true;
            }
        }

        //not currently running
        return false;
    }
}
