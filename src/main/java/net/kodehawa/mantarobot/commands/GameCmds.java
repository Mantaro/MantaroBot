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
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.kodehawa.mantarobot.commands.game.Character;
import net.kodehawa.mantarobot.commands.game.GuessTheNumber;
import net.kodehawa.mantarobot.commands.game.Pokemon;
import net.kodehawa.mantarobot.commands.game.Trivia;
import net.kodehawa.mantarobot.commands.game.TriviaDifficulty;
import net.kodehawa.mantarobot.commands.game.core.Game;
import net.kodehawa.mantarobot.commands.game.core.lobby.GameLobby;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.Category;
import net.kodehawa.mantarobot.core.command.meta.Defer;
import net.kodehawa.mantarobot.core.command.meta.Description;
import net.kodehawa.mantarobot.core.command.meta.Help;
import net.kodehawa.mantarobot.core.command.meta.Name;
import net.kodehawa.mantarobot.core.command.meta.Options;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.command.meta.Module;
import net.kodehawa.mantarobot.core.command.helpers.CommandCategory;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static net.kodehawa.mantarobot.utils.Utils.createLinkedList;

@Module
public class GameCmds {
    public static final IncreasingRateLimiter triviaRatelimiter = new IncreasingRateLimiter.Builder()
            .spamTolerance(1)
            .limit(1)
            .cooldown(8, TimeUnit.SECONDS)
            .cooldownPenaltyIncrease(5, TimeUnit.SECONDS)
            .maxCooldown(15, TimeUnit.MINUTES)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("trivia")
            .build();

    public static final IncreasingRateLimiter gameRatelimiter = new IncreasingRateLimiter.Builder()
            .limit(1)
            .spamTolerance(3)
            .cooldown(5, TimeUnit.SECONDS)
            .cooldownPenaltyIncrease(5, TimeUnit.SECONDS)
            .maxCooldown(10, TimeUnit.MINUTES)
            .pool(MantaroData.getDefaultJedisPool())
            .premiumAware(true)
            .prefix("game")
            .build();

    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(GameCommand.class);
        cr.registerSlash(TriviaCommand.class);
    }

    @Name("game")
    @Category(CommandCategory.GAMES)
    @Description("Plays a little game. Maybe a big game, who knows, life is full of surprises.")
    public static class GameCommand extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {}

        @Name("character")
        @Defer
        @Description("Anime character names.")
        public static class CharacterCommand extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                startGame(new Character(), ctx);
            }
        }

        @Name("pokemon")
        @Defer
        @Description("Who's that pokemon?")
        public static class PokemonCommand extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                startGame(new Pokemon(), ctx);
            }
        }

        @Name("number")
        @Defer
        @Description("Guess the number.")
        public static class GuessCommand extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                startGame(new GuessTheNumber(), ctx);
            }
        }

        @Override
        public Predicate<SlashContext> getPredicate() {
            return context -> RatelimitUtils.ratelimit(gameRatelimiter, context, null);
        }
    }

    @Name("trivia")
    @Category(CommandCategory.GAMES)
    @Description("Trivia game.")
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
        @Override
        protected void process(SlashContext ctx) {
            if (!RatelimitUtils.ratelimit(triviaRatelimiter, ctx)) {
                return;
            }

            var diff = ctx.getOptionAsString("difficulty", "");
            var difficulty = Utils.lookupEnumString(diff, TriviaDifficulty.class);
            startGame(new Trivia(difficulty), ctx);
        }
    }

    private static void startGame(Game<?> game, SlashContext ctx) {
        startGames(createLinkedList(game), ctx);
    }

    private static void startGames(LinkedList<Game<?>> games, SlashContext ctx) {
        if (checkRunning(ctx)) {
            return;
        }

        List<String> players = new ArrayList<>();
        players.add(ctx.getAuthor().getId());
        // I don't think this should happen anymore?
        if (games.size() > 1) {
            throw new IllegalArgumentException("There shouldn't be more than one game per lobby session.");
        }

        var lobby = new GameLobby(ctx, ctx.getLanguageContext(), players, games);
        lobby.startFirstGame();
    }

    private static boolean checkRunning(SlashContext ctx) {
        if (GameLobby.LOBBYS.containsKey(ctx.getChannel().getIdLong())) {
            var dbGuild = MantaroData.db().getGuild(ctx.getGuild());
            if (dbGuild.getGameTimeoutExpectedAt() != null &&
                    (Long.parseLong(dbGuild.getGameTimeoutExpectedAt()) < System.currentTimeMillis())) {
                GameLobby.LOBBYS.remove(ctx.getChannel().getIdLong()); // remove old lobby if dropped
                ctx.reply("commands.game.game_timeout_drop", EmoteReference.ERROR);
                return false;
            } else {
                ctx.reply("commands.game.other_lobby_running", EmoteReference.ERROR);
                return true;
            }
        }

        // not currently running
        return false;
    }
}
