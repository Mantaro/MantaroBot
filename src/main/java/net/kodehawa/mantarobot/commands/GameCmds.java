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

import br.com.brjdevs.java.utils.texts.StringUtils;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.game.Character;
import net.kodehawa.mantarobot.commands.game.*;
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

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.utils.StringUtils.SPLIT_PATTERN;

@Slf4j
@Module
@SuppressWarnings("unused")
public class GameCmds {
    private final Map<String, Function<TriviaDifficulty, Game>> games = new HashMap<>();

    @Subscribe
    public void game(CommandRegistry cr) {
        final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                .limit(1)
                .spamTolerance(1)
                .cooldown(15, TimeUnit.SECONDS)
                .cooldownPenaltyIncrease(5, TimeUnit.SECONDS)
                .maxCooldown(10, TimeUnit.MINUTES)
                .pool(MantaroData.getDefaultJedisPool())
                .build();

        //Does it even make sense to do this if I only had to add a parameter to one? Oh well...
        games.put("pokemon", (d) -> new Pokemon());
        games.put("number", (d) -> new GuessTheNumber());
        games.put("character", (d) -> new Character());
        games.put("trivia", Trivia::new);

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
                        .addField("Multiple games and trivia difficulty", "If you want to specify the difficulty of trivia on a `game lobby` call, you can use the `-diff` parameter.\n" +
                                "Example: `~>game lobby trivia, trivia, pokemon, trivia -diff hard`. You can do the same with `game multiple`.", false)
                        .build();
            }
        }.addSubCommand("character", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                startGames(createLinkedList(new Character()), event, languageContext);
            }
        }).addSubCommand("pokemon", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                startGames(createLinkedList(new Pokemon()), event, languageContext);
            }
        }).addSubCommand("number", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                startGames(createLinkedList(new GuessTheNumber()), event, languageContext);
            }
        }));

        gameCommand.setPredicate(event -> Utils.handleDefaultIncreasingRatelimit(rateLimiter, event.getAuthor(), event));

        //Sub-commands.
        gameCommand.addSubCommand("wins", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                Member member = Utils.findMember(event, event.getMember(), content);
                if(member == null)
                    return;

                event.getChannel().sendMessageFormat(languageContext.get("commands.game.won_games"),
                        EmoteReference.POPPER, member.getEffectiveName(), MantaroData.db().getPlayer(member).getData().getGamesWon()
                ).queue();
            }
        });

        gameCommand.addSubCommand("lobby", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                String[] args = net.kodehawa.mantarobot.utils.StringUtils.advancedSplitArgs(content, 0);
                Map<String, Optional<String>> t = StringUtils.parse(args);
                String difficultyArgument = "diff";
                content = Utils.replaceArguments(t, content, difficultyArgument);

                if(content.isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.game.nothing_specified"), EmoteReference.ERROR).queue();
                    return;
                }

                //Trivia difficulty handling.
                TriviaDifficulty difficulty = null;
                if(t.containsKey(difficultyArgument) && t.get(difficultyArgument).isPresent()) {
                    String d = t.get(difficultyArgument).get();
                    TriviaDifficulty enumDiff = TriviaDifficulty.lookupFromString(d);
                    if(enumDiff != null) {
                        difficulty = enumDiff;
                        content = content.replace(d, "").trim();
                    }
                }
                //End of trivia difficulty handling.

                //Stripe all mentions from this.
                String[] split = Utils.mentionPattern.matcher(content).replaceAll("").split(", ");

                if(split.length <= 1) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.game.not_enough_games"), EmoteReference.ERROR).queue();
                    return;
                }

                LinkedList<Game> gameList = new LinkedList<>();
                for(String s : split) {
                    gameList.add(games.get(s.trim()).apply(difficulty));
                }

                if(gameList.size() <= 1) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.game.invalid_selection"), EmoteReference.ERROR).queue();
                    return;
                }

                startGames(gameList, event, languageContext);
            }
        });

        gameCommand.addSubCommand("multiple", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                String[] args = net.kodehawa.mantarobot.utils.StringUtils.advancedSplitArgs(content, 0);
                Map<String, Optional<String>> t = StringUtils.parse(args);
                String difficultyArgument = "diff";
                content = Utils.replaceArguments(t, content, difficultyArgument);

                //Trivia difficulty handling.
                TriviaDifficulty difficulty = null;

                if(t.containsKey(difficultyArgument) && t.get(difficultyArgument).isPresent()) {
                    String d = t.get(difficultyArgument).get();
                    TriviaDifficulty enumDiff = TriviaDifficulty.lookupFromString(d);

                    if(enumDiff != null) {
                        difficulty = enumDiff;
                        content = content.replace(d, "").trim();
                    }
                }
                //End of trivia difficulty handling.

                String strippedContent = Utils.mentionPattern.matcher(content).replaceAll("");
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
                    gameList.add(games.get(values[0].trim()).apply(difficulty));
                }

                //No games queued?
                if(gameList.isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.game.multiple.invalid"), EmoteReference.ERROR).queue();
                    return;
                }

                startGames(gameList, event, languageContext);
            }
        });

        gameCommand.createSubCommandAlias("pokemon", "pokémon");
        gameCommand.createSubCommandAlias("number", "guessthatnumber");
    }

    @Subscribe
    public void trivia(CommandRegistry cr) {
        cr.register("trivia", new SimpleCommand(Category.GAMES) {
            final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                    .spamTolerance(1)
                    .limit(1)
                    .cooldown(16, TimeUnit.SECONDS)
                    .cooldownPenaltyIncrease(5, TimeUnit.SECONDS)
                    .maxCooldown(15, TimeUnit.MINUTES)
                    .pool(MantaroData.getDefaultJedisPool())
                    .build();

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(!Utils.handleDefaultIncreasingRatelimit(rateLimiter, event.getAuthor(), event))
                    return;

                String diff = "";
                List<User> mentions = event.getMessage().getMentionedUsers();
                List<Role> roleMentions = event.getMessage().getMentionedRoles();

                if(args.length > 0) {
                    diff = args[0].toLowerCase();
                }

                TriviaDifficulty difficulty = TriviaDifficulty.lookupFromString(diff);

                if(difficulty == null && (mentions.isEmpty() && roleMentions.isEmpty())) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.game.trivia.wrong_diff"), EmoteReference.ERROR).queue();
                    return;
                }

                startGames(createLinkedList(new Trivia(difficulty)), event, languageContext);
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

    private void startGames(LinkedList<Game> games, GuildMessageReceivedEvent event, I18nContext languageContext) {
        if(checkRunning(event, languageContext))
            return;

        List<String> players = new ArrayList<>();
        players.add(event.getAuthor().getId());
        final List<Role> mentionedRoles = event.getMessage().getMentionedRoles();

        if(!mentionedRoles.isEmpty()) {
            StringBuilder b = new StringBuilder();
            mentionedRoles.forEach(role ->
                    event.getGuild().getMembersWithRoles(role).forEach(user -> {
                        if(!user.getUser().getId().equals(event.getJDA().getSelfUser().getId()))
                            players.add(user.getUser().getId());
                        b.append(user.getEffectiveName())
                                .append(" ");
                    })
            );
            event.getChannel().sendMessageFormat(languageContext.get("commands.game.started_mp_role"), EmoteReference.MEGA, b.toString()).queue();
        }

        final List<User> mentionedUsers = event.getMessage().getMentionedUsers();
        if(!mentionedUsers.isEmpty()) {
            String users = mentionedUsers.stream()
                    .filter(u -> !u.isBot()).map(User::getName)
                    .collect(Collectors.joining("\n"));

            for(User user : mentionedUsers) {
                if(!user.getId().equals(event.getJDA().getSelfUser().getId()) && !user.isBot())
                    players.add(user.getId());
            }

            if(players.size() > 1) {
                event.getChannel().sendMessageFormat(languageContext.get("commands.game.started_mp_user"), EmoteReference.MEGA, users).queue();
            }
        }

        if(games.size() > 1) {
            event.getChannel().sendMessageFormat(languageContext.get("commands.game.lobby_started"),
                    EmoteReference.CORRECT, games.stream().map(Game::name)
                            .collect(Collectors.joining(", "))
            ).queue();
        }

        GameLobby lobby = new GameLobby(event, languageContext, players, games);
        lobby.startFirstGame();
    }

    private boolean checkRunning(GuildMessageReceivedEvent event, I18nContext languageContext) {
        if(GameLobby.LOBBYS.containsKey(event.getChannel().getIdLong())) {
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

    @SafeVarargs
    private final <T> LinkedList<T> createLinkedList(T... elements) {
        LinkedList<T> newList = new LinkedList<>();
        newList.addAll(Arrays.asList(elements));

        return newList;
    }

}
