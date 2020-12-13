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
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.CustomFinderUtil;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.utils.StringUtils.SPLIT_PATTERN;
import static net.kodehawa.mantarobot.utils.Utils.createLinkedList;

@Module
public class GameCmds {
    private final Map<String, Function<TriviaDifficulty, Game<?>>> games = new HashMap<>();

    @Subscribe
    public void game(CommandRegistry cr) {
        final var rateLimiter = new IncreasingRateLimiter.Builder()
                .limit(1)
                .spamTolerance(3)
                .cooldown(5, TimeUnit.SECONDS)
                .cooldownPenaltyIncrease(5, TimeUnit.SECONDS)
                .maxCooldown(10, TimeUnit.MINUTES)
                .pool(MantaroData.getDefaultJedisPool())
                .premiumAware(true)
                .prefix("game")
                .build();

        games.put("pokemon", (d) -> new Pokemon());
        games.put("number", (d) -> new GuessTheNumber());
        games.put("character", (d) -> new Character());
        games.put("trivia", Trivia::new);

        SimpleTreeCommand gameCommand = cr.register("game", new SimpleTreeCommand(CommandCategory.GAMES) {
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
            protected void call(Context ctx, I18nContext languageContext, String content) {
                startGame(new Character(), ctx);
            }
        }).addSubCommand("pokemon", new SubCommand() {
            @Override
            public String description() {
                return "Starts an instance of \"Guess that Pokemon / Who's that Pokemon?\"";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                startGame(new Pokemon(), ctx);
            }
        }).addSubCommand("number", new SubCommand() {
            @Override
            public String description() {
                return "Starts an instance of Guess the Number!";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                startGame(new GuessTheNumber(), ctx);
            }
        }));

        gameCommand.setPredicate(ctx -> RatelimitUtils.ratelimit(rateLimiter, ctx, null));

        //Sub-commands.
        gameCommand.addSubCommand("wins", new SubCommand() {
            @Override
            public String description() {
                return "Shows how many games you've won.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                ctx.findMember(content, ctx.getMessage()).onSuccess(members -> {
                    var member = CustomFinderUtil.findMemberDefault(content, members, ctx, ctx.getMember());
                    if (member == null) {
                        return;
                    }

                    ctx.sendStrippedLocalized("commands.game.won_games",
                            EmoteReference.POPPER, member.getEffectiveName(), ctx.getPlayer(member).getData().getGamesWon()
                    );
                });
            }
        });

        gameCommand.addSubCommand("lobby", new SubCommand() {
            @Override
            public String description() {
                return """
                        Starts a game lobby. For example `~>game lobby pokemon, trivia` will start pokemon and then trivia.
                        If you want to specify the difficulty of trivia, you can use the `-diff` parameter.
                        Example: `~>game lobby pokemon, trivia -diff hard`
                        """;
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var guildData = ctx.getDBGuild().getData();
                if (guildData.isGameMultipleDisabled()) {
                    ctx.sendLocalized("commands.game.disabled_multiple", EmoteReference.ERROR);
                    return;
                }

                var args = ctx.getOptionalArguments();
                var difficulty = getTriviaDifficulty(ctx);
                content = Utils.replaceArguments(args, content, "diff");
                if (difficulty != null) {
                    content = content.replace(args.get("diff"), "").trim();
                }

                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.game.nothing_specified", EmoteReference.ERROR);
                    return;
                }

                var split = Utils.mentionPattern.matcher(content).replaceAll("").split(", ");
                if (split.length <= 1) {
                    ctx.sendLocalized("commands.game.not_enough_games", EmoteReference.ERROR);
                    return;
                }

                var userData = ctx.getDBUser().getData();
                var key = MantaroData.db().getPremiumKey(userData.getPremiumKey());
                var premium = key != null && key.getDurationDays() > 1;
                if (split.length > (premium ? 8 : 5)) {
                    ctx.sendLocalized("commands.game.too_many_games", EmoteReference.ERROR);
                    return;
                }

                LinkedList<Game<?>> gameList = new LinkedList<>();
                for (var arg : split) {
                    var game = games.get(arg.trim());

                    if (game == null) {
                        continue;
                    }

                    var finalGame = game.apply(difficulty);
                    if (finalGame == null) {
                        continue;
                    }

                    gameList.add(finalGame);
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
                return """
                        Starts multiple instances of one game. For example `~>game multiple trivia 5` will start trivia 5 times.
                        To do it with multiple users, you can use `~>game multiple <game> [@user...] <amount>`
                        """;
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var guildData = ctx.getDBGuild().getData();
                if (guildData.isGameMultipleDisabled()) {
                    ctx.sendLocalized("commands.game.disabled_multiple", EmoteReference.ERROR);
                    return;
                }

                content = Utils.replaceArguments(ctx.getOptionalArguments(), content, "diff");
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.game.nothing_specified", EmoteReference.ERROR);
                    return;
                }

                var difficulty = getTriviaDifficulty(ctx);
                var args = ctx.getOptionalArguments();
                if (difficulty != null) {
                    content = content.replace(args.get("diff"), "").trim();
                }

                var strippedContent = Utils.mentionPattern.matcher(content).replaceAll("");
                var values = SPLIT_PATTERN.split(strippedContent, 2);
                if (values.length < 2) {
                    ctx.sendLocalized("commands.game.multiple.invalid", EmoteReference.ERROR);
                    return;
                }

                var number = 0;
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
                for (var i = 0; i < number; i++) {
                    var value = values[0];
                    var trimmedValue = value.trim();

                    if (trimmedValue.length() == 0) {
                        continue;
                    }

                    var game = games.get(trimmedValue);

                    if (game == null) {
                        continue;
                    }

                    var g = game.apply(difficulty);
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
        final var rateLimiter = new IncreasingRateLimiter.Builder()
                .spamTolerance(1)
                .limit(1)
                .cooldown(16, TimeUnit.SECONDS)
                .cooldownPenaltyIncrease(5, TimeUnit.SECONDS)
                .maxCooldown(15, TimeUnit.MINUTES)
                .pool(MantaroData.getDefaultJedisPool())
                .prefix("trivia")
                .build();

        cr.register("trivia", new SimpleCommand(CommandCategory.GAMES) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (!RatelimitUtils.ratelimit(rateLimiter, ctx)) {
                    return;
                }

                var diff = "";
                var mentions = ctx.getMentionedUsers();
                var roleMentions = ctx.getMessage().getMentionedRoles();

                if (args.length > 0) {
                    diff = args[0].toLowerCase();
                }

                var difficulty = Utils.lookupEnumString(diff, TriviaDifficulty.class);

                if (difficulty == null && (mentions.isEmpty() && roleMentions.isEmpty()) && !content.isEmpty()) {
                    ctx.sendLocalized("commands.game.trivia.wrong_diff", EmoteReference.ERROR);
                    return;
                }

                startGames(createLinkedList(new Trivia(difficulty)), ctx);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription(
                                """
                                Starts an instance of a trivia game.
                                You have 10 attempts and 60 seconds to answer, otherwise the game ends.
                                """
                        )
                        .setUsage("`~>trivia [@user] [difficulty]` - Starts a new game of trivia")
                        .addParameterOptional("@user", "Whoever you want to play trivia with.")
                        .addParameterOptional("difficulty",
                                "The difficulty of the game, it can be easy, medium or hard.")
                        .build();
            }
        });
    }

    private void startGame(Game<?> game, Context ctx) {
        startGames(createLinkedList(game), ctx);
    }

    private void startGames(LinkedList<Game<?>> games, Context ctx) {
        if (checkRunning(ctx)) {
            return;
        }

        List<String> players = new ArrayList<>();
        players.add(ctx.getAuthor().getId());
        final var mentionedRoles = ctx.getMessage().getMentionedRoles();

        if (!mentionedRoles.isEmpty()) {
            var strBuilder = new StringBuilder();
            mentionedRoles.forEach(role ->
                    ctx.getGuild().getMembersWithRoles(role).forEach(user -> {
                        if (!user.getUser().getId().equals(ctx.getSelfUser().getId())) {
                            players.add(user.getUser().getId());
                        }

                        strBuilder.append(user.getEffectiveName()).append(" ");
                    })
            );

            ctx.sendLocalized("commands.game.started_mp_role", EmoteReference.MEGA, strBuilder.toString());
        }

        final var mentionedUsers = ctx.getMentionedUsers();
        if (!mentionedUsers.isEmpty()) {
            var users = mentionedUsers.stream()
                    .filter(u -> !u.isBot())
                    .map(User::getName)
                    .collect(Collectors.joining("\n"));

            for (var user : mentionedUsers) {
                if (!user.getId().equals(ctx.getSelfUser().getId()) && !user.isBot())
                    players.add(user.getId());
            }

            if (players.size() > 1)
                ctx.sendLocalized("commands.game.started_mp_user", EmoteReference.MEGA, users);
        }

        if (games.size() > 1) {
            ctx.sendLocalized("commands.game.lobby_started", EmoteReference.CORRECT, games.stream()
                    .map(Game::name)
                    .collect(Collectors.joining(", "))
            );
        }

        var lobby = new GameLobby(ctx.getEvent(), ctx.getLanguageContext(), players, games);
        lobby.startFirstGame();
    }

    private boolean checkRunning(Context ctx) {
        if (GameLobby.LOBBYS.containsKey(ctx.getChannel().getIdLong())) {
            var dbGuild = MantaroData.db().getGuild(ctx.getGuild());

            if (dbGuild.getData().getGameTimeoutExpectedAt() != null &&
                    (Long.parseLong(dbGuild.getData().getGameTimeoutExpectedAt()) < System.currentTimeMillis())) {
                ctx.sendLocalized("commands.game.game_timeout_drop", EmoteReference.ERROR);
                return false;
            } else {
                ctx.sendLocalized("commands.game.other_lobby_running", EmoteReference.ERROR);
                return true;
            }
        }

        // not currently running
        return false;
    }

    private TriviaDifficulty getTriviaDifficulty(Context ctx) {
        var arguments = ctx.getOptionalArguments();
        TriviaDifficulty difficulty = null;
        var arg = arguments.get("diff");
        if (arg != null) {
            var enumDiff = Utils.lookupEnumString(arg, TriviaDifficulty.class);
            if (enumDiff != null) {
                difficulty = enumDiff;
            }
        }

        return difficulty;
    }
}
