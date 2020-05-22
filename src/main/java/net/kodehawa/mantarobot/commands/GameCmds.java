/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
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
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.PremiumKey;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.IncreasingRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.utils.StringUtils.SPLIT_PATTERN;
import static net.kodehawa.mantarobot.utils.Utils.createLinkedList;

@Module
@SuppressWarnings("unused")
public class GameCmds {
    private static final Logger log = LoggerFactory.getLogger(GameCmds.class);
    private final Map<String, Function<TriviaDifficulty, Game<?>>> games = new HashMap<>();

    @Subscribe
    public void game(CommandRegistry cr) {
        final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                .limit(1)
                .spamTolerance(1)
                .cooldown(7, TimeUnit.SECONDS)
                .cooldownPenaltyIncrease(5, TimeUnit.SECONDS)
                .maxCooldown(10, TimeUnit.MINUTES)
                .pool(MantaroData.getDefaultJedisPool())
                .premiumAware(true)
                .prefix("game")
                .build();
        final ManagedDatabase db = MantaroData.db();

        //Does it even make sense to do this if I only had to add a parameter to one? Oh well...
        games.put("pokemon", (d) -> new Pokemon());
        games.put("number", (d) -> new GuessTheNumber());
        games.put("character", (d) -> new Character());
        games.put("trivia", Trivia::new);

        SimpleTreeCommand gameCommand = (SimpleTreeCommand) cr.register("game", new SimpleTreeCommand(Category.GAMES) {
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Plays a little game. Maybe a big game, who knows, life is full of surprises.")
                        .setUsage("`~>game <game> [@user]`")
                        .addParameter("game", "The game you want to play, refer to subcommands.")
                        .addParameterOptional("user", "Whoever you want to play this game with.")
                        .build();
            }
        }.addSubCommand("character", new SubCommand() {
            @Override
            public String description() {
                return "Starts an instance of Guess the character (anime)";
            }

            @Override
            protected void call(Context ctx, String content) {
                startGame(new Character(), ctx);
            }
        }).addSubCommand("pokemon", new SubCommand() {
            @Override
            public String description() {
                return "Starts an instance of \"Guess that Pokemon / Who's that Pokemon?\"";
            }

            @Override
            protected void call(Context ctx, String content) {
                startGame(new Pokemon(), ctx);
            }
        }).addSubCommand("number", new SubCommand() {
            @Override
            public String description() {
                return "Starts an instance of Guess the Number!";
            }

            @Override
            protected void call(Context ctx, String content) {
                startGame(new GuessTheNumber(), ctx);
            }
        }));

        gameCommand.setPredicate(ctx -> Utils.handleIncreasingRatelimit(rateLimiter, ctx.getAuthor(), ctx.getEvent(), null));

        //Sub-commands.
        gameCommand.addSubCommand("wins", new SubCommand() {
            @Override
            public String description() {
                return "Shows how many games you've won.";
            }

            @Override
            protected void call(Context ctx, String content) {
                Member member = Utils.findMember(ctx.getEvent(), ctx.getMember(), content);
                if (member == null)
                    return;

                ctx.sendStrippedLocalized("commands.game.won_games",
                        EmoteReference.POPPER, member.getEffectiveName(), db.getPlayer(member).getData().getGamesWon()
                );
            }
        });

        gameCommand.addSubCommand("lobby", new SubCommand() {
            @Override
            public String description() {
                return "Starts a game lobby. For example `~>game lobby pokemon, trivia` will start pokemon and then trivia\n" +
                        "If you want to specify the difficulty of trivia, you can use the `-diff` parameter. Example: `~>game lobby pokemon, trivia -diff hard`";
            }

            @Override
            protected void call(Context ctx, String content) {
                GuildData guildData = ctx.getDBGuild().getData();

                if(guildData.isGameMultipleDisabled()) {
                    ctx.sendLocalized("commands.game.disabled_multiple", EmoteReference.ERROR);
                    return;
                }

                String[] args = net.kodehawa.mantarobot.utils.StringUtils.advancedSplitArgs(content, 0);
                Map<String, String> t = ctx.getOptionalArguments();
                String difficultyArgument = "diff";
                content = Utils.replaceArguments(t, content, difficultyArgument);

                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.game.nothing_specified", EmoteReference.ERROR);
                    return;
                }

                //Trivia difficulty handling.
                TriviaDifficulty difficulty = null;
                if (t.containsKey(difficultyArgument) && t.get(difficultyArgument) != null) {
                    String d = t.get(difficultyArgument);
                    TriviaDifficulty enumDiff = TriviaDifficulty.lookupFromString(d);
                    if (enumDiff != null) {
                        difficulty = enumDiff;
                        content = content.replace(d, "").trim();
                    }
                }
                //End of trivia difficulty handling.

                //Stripe all mentions from this.
                String[] split = Utils.mentionPattern.matcher(content).replaceAll("").split(", ");

                if (split.length <= 1) {
                    ctx.sendLocalized("commands.game.not_enough_games", EmoteReference.ERROR);
                    return;
                }

                UserData userData = db.getUser(ctx.getAuthor()).getData();
                PremiumKey key = db.getPremiumKey(userData.getPremiumKey());
                boolean premium = key != null && key.getDurationDays() > 1;
                if (split.length > (premium ? 8 : 5)) {
                    ctx.sendLocalized("commands.game.too_many_games", EmoteReference.ERROR);
                    return;
                }

                LinkedList<Game<?>> gameList = new LinkedList<>();
                for (String s : split) {
                    Function<TriviaDifficulty, Game<?>> f = games.get(s.trim());

                    if (f == null)
                        continue;

                    Game<?> g = f.apply(difficulty);
                    if (g == null)
                        continue;

                    gameList.add(g);
                }

                if (gameList.size() <= 1) {
                    ctx.sendLocalized("commands.game.invalid_selection", EmoteReference.ERROR);
                    return;
                }

                startGames(gameList, ctx);
            }
        });

        gameCommand.addSubCommand("multiple", new SubCommand() {
            @Override
            public String description() {
                return "Starts multiple instances of one game, for example `~>game multiple trivia 5` will start trivia 5 times.";
            }

            @Override
            protected void call(Context ctx, String content) {
                GuildData guildData = ctx.getDBGuild().getData();
                if(guildData.isGameMultipleDisabled()) {
                    ctx.sendLocalized("commands.game.disabled_multiple", EmoteReference.ERROR);
                    return;
                }

                String[] args = net.kodehawa.mantarobot.utils.StringUtils.advancedSplitArgs(content, 0);
                Map<String, String> t = ctx.getOptionalArguments();
                String difficultyArgument = "diff";
                content = Utils.replaceArguments(t, content, difficultyArgument);

                //Trivia difficulty handling.
                TriviaDifficulty difficulty = null;

                if (t.containsKey(difficultyArgument) && t.get(difficultyArgument) != null) {
                    String d = t.get(difficultyArgument);
                    TriviaDifficulty enumDiff = TriviaDifficulty.lookupFromString(d);

                    if (enumDiff != null) {
                        difficulty = enumDiff;
                        content = content.replace(d, "").trim();
                    }
                }
                //End of trivia difficulty handling.

                String strippedContent = Utils.mentionPattern.matcher(content).replaceAll("");
                String[] values = SPLIT_PATTERN.split(strippedContent, 2);

                if (values.length < 2) {
                    ctx.sendLocalized("commands.game.multiple.invalid", EmoteReference.ERROR);
                    return;
                }

                int number;
                try {
                    number = Integer.parseInt(values[1]);
                } catch (Exception e) {
                    ctx.sendLocalized("commands.game.multiple.invalid_times", EmoteReference.ERROR);
                    return;
                }

                if (number > 5) {
                    ctx.sendLocalized("commands.game.multiple.too_many_games", EmoteReference.ERROR);
                    return;
                }

                LinkedList<Game<?>> gameList = new LinkedList<>();
                for (int i = 0; i < number; i++) {
                    String value = values[0];
                    String trimmedValue = value.trim();

                    if (trimmedValue.length() == 0)
                        continue;

                    Function<TriviaDifficulty, Game<?>> f = games.get(trimmedValue);

                    if (f == null)
                        continue;

                    Game<?> g = f.apply(difficulty);
                    gameList.add(g);
                }

                //No games queued?
                if (gameList.isEmpty()) {
                    ctx.sendLocalized("commands.game.multiple.invalid", EmoteReference.ERROR);
                    return;
                }

                startGames(gameList, ctx);
            }
        });

        gameCommand.createSubCommandAlias("pokemon", "pokémon");
        gameCommand.createSubCommandAlias("number", "guessthatnumber");
        gameCommand.createSubCommandAlias("number", "guessthenumber");
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
                    .prefix("trivia")
                    .build();

            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (!Utils.handleIncreasingRatelimit(rateLimiter, ctx.getAuthor(), ctx.getEvent(), ctx.getLanguageContext()))
                    return;

                String diff = "";
                List<User> mentions = ctx.getMentionedUsers();
                List<Role> roleMentions = ctx.getMessage().getMentionedRoles();

                if (args.length > 0)
                    diff = args[0].toLowerCase();

                TriviaDifficulty difficulty = TriviaDifficulty.lookupFromString(diff);

                if (difficulty == null && (mentions.isEmpty() && roleMentions.isEmpty()) && !content.isEmpty()) {
                    ctx.sendLocalized("commands.game.trivia.wrong_diff", EmoteReference.ERROR);
                    return;
                }

                startGames(createLinkedList(new Trivia(difficulty)), ctx);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Starts an instance of trivia. You have 10 attempts and 60 seconds to answer, otherwise the game ends.")
                        .setUsage("`~>trivia [@user] [difficulty]` - Starts a new game of trivia")
                        .addParameterOptional("@user", "Whoever you want to play trivia with.")
                        .addParameterOptional("difficulty", "The difficulty of the game, it can be easy, medium or hard.")
                        .build();
            }
        });
    }

    private void startGame(Game<?> game, Context ctx) {
        startGames(createLinkedList(game), ctx);
    }

    private void startGames(LinkedList<Game<?>> games, Context ctx) {
        if (checkRunning(ctx))
            return;

        List<String> players = new ArrayList<>();
        players.add(ctx.getAuthor().getId());
        final List<Role> mentionedRoles = ctx.getMessage().getMentionedRoles();

        if (!mentionedRoles.isEmpty()) {
            StringBuilder b = new StringBuilder();
            mentionedRoles.forEach(role ->
                    ctx.getGuild().getMembersWithRoles(role).forEach(user -> {
                        if (!user.getUser().getId().equals(ctx.getSelfUser().getId()))
                            players.add(user.getUser().getId());

                        b.append(user.getEffectiveName())
                                .append(" ");
                    })
            );

            ctx.sendLocalized("commands.game.started_mp_role", EmoteReference.MEGA, b.toString());
        }

        final List<User> mentionedUsers = ctx.getMentionedUsers();
        if (!mentionedUsers.isEmpty()) {
            String users = mentionedUsers.stream()
                    .filter(u -> !u.isBot())
                    .map(User::getName)
                    .collect(Collectors.joining("\n"));

            for (User user : mentionedUsers) {
                if (!user.getId().equals(ctx.getSelfUser().getId()) && !user.isBot())
                    players.add(user.getId());
            }

            if (players.size() > 1)
                ctx.sendLocalized("commands.game.started_mp_user", EmoteReference.MEGA, users);
        }

        if (games.size() > 1) {
            ctx.sendLocalized("commands.game.lobby_started", games.stream().map(Game::name).collect(Collectors.joining(", ")));
        }

        GameLobby lobby = new GameLobby(ctx.getEvent(), ctx.getLanguageContext(), players, games);
        lobby.startFirstGame();
    }

    private boolean checkRunning(Context ctx) {
        if (GameLobby.LOBBYS.containsKey(ctx.getChannel().getIdLong())) {
            DBGuild dbGuild = MantaroData.db().getGuild(ctx.getGuild());

            if (dbGuild.getData().getGameTimeoutExpectedAt() != null && (Long.parseLong(dbGuild.getData().getGameTimeoutExpectedAt()) < System.currentTimeMillis())) {
                ctx.sendLocalized("commands.game.game_timeout_drop", EmoteReference.ERROR);
                return false;
            } else {
                ctx.sendLocalized("commands.game.other_lobby_running", EmoteReference.ERROR);
                return true;
            }
        }

        //not currently running
        return false;
    }
}
