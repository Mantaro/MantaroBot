/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
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
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.IncreasingRateLimiter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.utils.StringUtils.SPLIT_PATTERN;

@Slf4j
@Module
@SuppressWarnings("unused")
public class GameCmds {
    private final Pattern mentionPattern = Pattern.compile("<(#|@|@&)?.[0-9]{17,21}>");

    @Subscribe
    public void game(CommandRegistry cr) {
        final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                .limit(1)
                .spamTolerance(1)
                .cooldown(10, TimeUnit.SECONDS)
                .cooldownPenaltyIncrease(5, TimeUnit.SECONDS)
                .maxCooldown(10, TimeUnit.MINUTES)
                .pool(MantaroData.getDefaultJedisPool())
                .build();

        SimpleTreeCommand gameCommand = (SimpleTreeCommand) cr.register("game", new SimpleTreeCommand(Category.GAMES) {
            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Guessing games.")
                        .addField("Games", "`~>game character` - **Starts an instance of Guess the character (anime)**.\n"
                                + "`~>game pokemon` - **Starts an instance of who's that pokemon?**\n" +
                                "`~>game number` - **Starts an instance of Guess The Number**`\n" +
                                "`~>game lobby` - **Starts a chunk of different games, for example `~>game lobby pokemon, trivia` will start pokemon and then trivia.**\n" +
                                "`~>game multiple` - **Starts multiple instances of one game, for example `~>game multiple trivia 5` will start trivia 5 times.**\n" +
                                "`~>game wins` - **Shows how many times you've won in games**", false)
                        .addField("Considerations", "The pokemon guessing game has around *900 different pokemon* to guess, " +
                                "where the anime guessing game has around 60. The number in the number guessing game is a random number between 0 and 150.\n" +
                                "To start multiple trivia sessions please use `~>game trivia multiple`, not `~>trivia multiple`", false)
                        .build();
            }
        }.addSubCommand("character", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                startGame(new Character(), event, languageContext);
            }
        }).addSubCommand("pokemon", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                startGame(new Pokemon(), event, languageContext);
            }
        }).addSubCommand("number", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                startGame(new GuessTheNumber(), event, languageContext);
            }
        }));

        gameCommand.setPredicate(event -> Utils.handleDefaultIncreasingRatelimit(rateLimiter, event.getAuthor(), event));
        gameCommand.createSubCommandAlias("pokemon", "pokémon");
        gameCommand.createSubCommandAlias("number", "guessthatnumber");

        gameCommand.addSubCommand("wins", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                Member member = Utils.findMember(event, event.getMember(), content);
                if(member == null)
                    return;

                event.getChannel().sendMessageFormat(languageContext.get("commands.game.won_games"), EmoteReference.POPPER, member.getEffectiveName(), MantaroData.db().getPlayer(member).getData().getGamesWon()).queue();
            }
        });

        gameCommand.addSubCommand("lobby", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                if(content.isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.game.nothing_specified"), EmoteReference.ERROR).queue();
                    return;
                }

                //Stripe all mentions from this.
                String[] split = mentionPattern.matcher(content).replaceAll("").split(", ");

                if(split.length < 1 || split.length == 1) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.game.not_enough_games"), EmoteReference.ERROR).queue();
                    return;
                }

                LinkedList<Game> gameList = new LinkedList<>();
                for(String s : split) {
                    switch(s.replace(" ", "")) {
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
                    event.getChannel().sendMessageFormat(languageContext.get("commands.game.invalid_selection"), EmoteReference.ERROR).queue();
                    return;
                }

                startMultipleGames(gameList, event, languageContext);
            }
        });

        gameCommand.addSubCommand("multiple", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                String strippedContent =  mentionPattern.matcher(content).replaceAll("");
                String[] values = SPLIT_PATTERN.split(strippedContent, 2);
                if(values.length < 2) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.game.multiple.invalid"), EmoteReference.ERROR).queue();
                    return;
                }

                int number;

                try {
                    number = Integer.parseInt(values[1]);
                } catch(Exception e) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.game.multiple.invalid_times"), EmoteReference.ERROR).queue();
                    return;
                }

                if(number > 10) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.game.multiple.too_many_games"), EmoteReference.ERROR).queue();
                    return;
                }

                LinkedList<Game> gameList = new LinkedList<>();
                for(int i = 0; i < number; i++) {
                    switch(values[0].replace(" ", "")) {
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

                if(gameList.isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.game.multiple.invalid"), EmoteReference.ERROR).queue();
                    return;
                }

                startMultipleGames(gameList, event, languageContext);
            }
        });
    }

    @Subscribe
    public void trivia(CommandRegistry cr) {
        cr.register("trivia", new SimpleCommand(Category.GAMES) {
            final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                    .spamTolerance(1)
                    .limit(1)
                    .cooldown(10, TimeUnit.SECONDS)
                    .cooldownPenaltyIncrease(5, TimeUnit.SECONDS)
                    .maxCooldown(15, TimeUnit.MINUTES)
                    .pool(MantaroData.getDefaultJedisPool())
                    .build();

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(!Utils.handleDefaultIncreasingRatelimit(rateLimiter, event.getAuthor(), event))
                    return;

                String difficulty = null;

                List<User> mentions = event.getMessage().getMentionedUsers();
                List<Role> roleMentions = event.getMessage().getMentionedRoles();

                if(args.length == 1) {
                    difficulty = args[0];
                }

                if((difficulty != null && !(difficulty.equals("easy") || difficulty.equals("hard") || difficulty.equals("medium"))) && (mentions.isEmpty() && roleMentions.isEmpty())) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.game.trivia.wrong_diff"), EmoteReference.ERROR).queue();
                    return;
                } else if (!mentions.isEmpty() || !roleMentions.isEmpty()) {
                    difficulty = null;
                }

                startGame(new Trivia(difficulty), event, languageContext);
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Trivia command.")
                        .setDescription("**Starts an instance of trivia.**\n" +
                                "Optionally, you can specify the difficulty (easy, medium or hard) to play.")
                        .addField("Rules", "You have 10 attempts and 60 seconds to answer, otherwise the game ends.", false)
                        .addField("Considerations", "To start multiple trivia sessions please use `~>game multiple trivia`, not `~>trivia multiple`", false)
                        .build();
            }
        });
    }

    private void startMultipleGames(LinkedList<Game> games, GuildMessageReceivedEvent event, I18nContext languageContext) {
        if(checkRunning(event, languageContext))
            return;

        List<String> players = new ArrayList<>();
        players.add(event.getAuthor().getId());

        if(!event.getMessage().getMentionedRoles().isEmpty()) {
            StringBuilder b = new StringBuilder();
            event.getMessage().getMentionedRoles().forEach(role ->
                    event.getGuild().getMembersWithRoles(role).forEach(user -> {
                        if(!user.getUser().getId().equals(event.getJDA().getSelfUser().getId()))
                            players.add(user.getUser().getId());
                        b.append(user.getEffectiveName()).append(" ");
                    })
            );
            event.getChannel().sendMessageFormat(languageContext.get("commands.game.started_mp_role"), EmoteReference.MEGA, b.toString()).queue();
        }

        if(!event.getMessage().getMentionedUsers().isEmpty()) {
            StringBuilder builder = new StringBuilder();
            event.getMessage().getMentionedUsers().forEach(user -> {
                if(!user.getId().equals(event.getJDA().getSelfUser().getId()) && !user.isBot())
                    players.add(user.getId());
                builder.append(user.getName()).append(" ");
            });

            if(players.size() > 1) {
                event.getChannel().sendMessageFormat(languageContext.get("commands.game.started_mp_user"), EmoteReference.MEGA, builder.toString()).queue();
            }
        }

        event.getChannel().sendMessageFormat(languageContext.get("commands.game.lobby_started"), EmoteReference.CORRECT, games.stream().map(Game::name).collect(Collectors.joining(", "))).queue();
        GameLobby lobby = new GameLobby(event, languageContext, players, games);
        lobby.startFirstGame();
    }

    private void startGame(Game game, GuildMessageReceivedEvent event, I18nContext languageContext) {
        if(checkRunning(event, languageContext)) return;

        LinkedList<Game> list = new LinkedList<>();
        list.add(game);

        List<String> players = new ArrayList<>();
        players.add(event.getAuthor().getId());

        if(!event.getMessage().getMentionedRoles().isEmpty()) {
            StringBuilder b = new StringBuilder();
            event.getMessage().getMentionedRoles().forEach(role ->
                    event.getGuild().getMembersWithRoles(role).forEach(user -> {
                        if(!user.getUser().getId().equals(event.getJDA().getSelfUser().getId()))
                            players.add(user.getUser().getId());
                        b.append(user.getEffectiveName()).append(" ");
                    })
            );

            event.getChannel().sendMessageFormat(languageContext.get("commands.game.started_mp_role"), EmoteReference.MEGA, b.toString()).queue();
        }

        if(!event.getMessage().getMentionedUsers().isEmpty()) {
            StringBuilder builder = new StringBuilder();
            event.getMessage().getMentionedUsers().forEach(user -> {
                if(!user.getId().equals(event.getJDA().getSelfUser().getId()) && !user.isBot())
                    players.add(user.getId());
                builder.append(user.getName()).append(" ");
            });

            if(players.size() > 1) {
                event.getChannel().sendMessageFormat(languageContext.get("commands.game.started_mp_user"), EmoteReference.MEGA, builder.toString()).queue();
            }
        }

        GameLobby lobby = new GameLobby(event, languageContext, players, list);
        lobby.startFirstGame();
    }

    private boolean checkRunning(GuildMessageReceivedEvent event, I18nContext languageContext) {
        if(GameLobby.LOBBYS.containsKey(event.getChannel())) {
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            if(dbGuild.getData().getGameTimeoutExpectedAt() != null && (Long.parseLong(dbGuild.getData().getGameTimeoutExpectedAt()) < System.currentTimeMillis())) {
                event.getChannel().sendMessageFormat(languageContext.get("commands.game.game_timeout_drop"), EmoteReference.ERROR).queue();
                return false;
            } else {
                event.getChannel().sendMessageFormat(languageContext.get("commands.game.other_lobby_running"), EmoteReference.ERROR).queue();
                return true;
            }
        }

        //not currently running
        return false;
    }
}
