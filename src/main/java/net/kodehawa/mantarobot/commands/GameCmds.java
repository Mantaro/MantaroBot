/*
 * Copyright (C) 2016 Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.kodehawa.mantarobot.commands.game.Character;
import net.kodehawa.mantarobot.commands.game.*;
import net.kodehawa.mantarobot.commands.game.core.Game;
import net.kodehawa.mantarobot.commands.game.core.GameLobby;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.*;
import net.kodehawa.mantarobot.core.command.slash.IContext;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.modules.Module;
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

import static net.kodehawa.mantarobot.utils.Utils.createLinkedList;

@Module
public class GameCmds {
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

        gameCommand.setPredicate(ctx -> {
            if (!ctx.getSelfMember().hasPermission(ctx.getChannel(), Permission.MESSAGE_EMBED_LINKS)) {
                ctx.sendLocalized("general.missing_embed_permissions");
                return false;
            }

            return RatelimitUtils.ratelimit(rateLimiter, ctx, null);
        });

        //Sub-commands.
        gameCommand.addSubCommand("wins", new SubCommand() {
            @Override
            public String description() {
                return "Shows how many games you've won.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                ctx.findMember(content, members -> {
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

        gameCommand.createSubCommandAlias("pokemon", "pokémon");
        gameCommand.createSubCommandAlias("number", "guessthatnumber");
        gameCommand.createSubCommandAlias("number", "guessthenumber");
    }

    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(TriviaCommand.class);
    }

    @Name("trivia")
    @Category(CommandCategory.GAMES)
    @Description("Starts a trivia game.")
    @Options(@Options.Option(type = OptionType.STRING, name = "difficulty", description = "The game difficulty", choices = {
            @Options.Choice(description = "Easy", value = "easy"),
            @Options.Choice(description = "Medium", value = "medium"),
            @Options.Choice(description = "Hard", value = "hard")
    })
    )
    @Help(description = """
            Starts an instance of a trivia game.
            You have 10 attempts and 60 seconds to answer, otherwise the game ends.
            """)
    public static class TriviaCommand extends SlashCommand {
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
        protected void process(SlashContext ctx) {
            if (!RatelimitUtils.ratelimit(rateLimiter, ctx)) {
                return;
            }

            var diff = ctx.getOptionAsString("difficulty", "");
            var difficulty = Utils.lookupEnumString(diff, TriviaDifficulty.class);
            startGames(createLinkedList(new Trivia(difficulty)), ctx);
        }
    }

    private void startGame(Game<?> game, IContext ctx) {
        startGames(createLinkedList(game), ctx);
    }

    private static void startGames(LinkedList<Game<?>> games, IContext ctx) {
        if (checkRunning(ctx)) {
            return;
        }

        List<String> players = new ArrayList<>();
        players.add(ctx.getAuthor().getId());
        if (games.size() > 1) {
            ctx.sendLocalized("commands.game.lobby_started", EmoteReference.CORRECT, games.stream()
                    .map(Game::name)
                    .collect(Collectors.joining(", "))
            );
        }

        var lobby = new GameLobby(ctx, ctx.getLanguageContext(), players, games);
        lobby.startFirstGame();
    }

    private static boolean checkRunning(IContext ctx) {
        if (GameLobby.LOBBYS.containsKey(ctx.getChannel().getIdLong())) {
            var dbGuild = MantaroData.db().getGuild(ctx.getGuild());

            if (dbGuild.getData().getGameTimeoutExpectedAt() != null &&
                    (Long.parseLong(dbGuild.getData().getGameTimeoutExpectedAt()) < System.currentTimeMillis())) {
                GameLobby.LOBBYS.remove(ctx.getChannel().getIdLong()); // remove old lobby if dropped
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
}
