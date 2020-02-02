/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.game.Character;
import net.kodehawa.mantarobot.commands.game.GuessTheNumber;
import net.kodehawa.mantarobot.commands.game.Pokemon;
import net.kodehawa.mantarobot.commands.game.Trivia;
import net.kodehawa.mantarobot.commands.game.TriviaDifficulty;
import net.kodehawa.mantarobot.commands.game.core.Game;
import net.kodehawa.mantarobot.commands.game.core.GameLobby;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SimpleTreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.PremiumKey;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.IncreasingRateLimiter;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.utils.StringUtils.SPLIT_PATTERN;
import static net.kodehawa.mantarobot.utils.Utils.createLinkedList;

@Module
@SuppressWarnings("unused")
public class GameCmds {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GameCmds.class);
    private final Map<String, Function<TriviaDifficulty, Game<?>>> games = new HashMap<>();
    
    @Subscribe
    public void game(CommandRegistry cr) {
        final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                                                          .limit(1)
                                                          .spamTolerance(1)
                                                          .cooldown(15, TimeUnit.SECONDS)
                                                          .cooldownPenaltyIncrease(5, TimeUnit.SECONDS)
                                                          .maxCooldown(10, TimeUnit.MINUTES)
                                                          .pool(MantaroData.getDefaultJedisPool())
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
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                startGames(createLinkedList(new Character()), event, languageContext);
            }
        }).addSubCommand("pokemon", new SubCommand() {
            @Override
            public String description() {
                return "Starts an instance of \"Guess that Pokemon / Who's that Pokemon?\"";
            }
            
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                startGames(createLinkedList(new Pokemon()), event, languageContext);
            }
        }).addSubCommand("number", new SubCommand() {
            @Override
            public String description() {
                return "Starts an instance of Guess the Number!";
            }
            
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                startGames(createLinkedList(new GuessTheNumber()), event, languageContext);
            }
        }));
        
        gameCommand.setPredicate(event -> Utils.handleDefaultIncreasingRatelimit(rateLimiter, event.getAuthor(), event, null));
        
        //Sub-commands.
        gameCommand.addSubCommand("wins", new SubCommand() {
            @Override
            public String description() {
                return "Shows how many games you've won.";
            }
            
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                Member member = Utils.findMember(event, event.getMember(), content);
                if(member == null)
                    return;
                
                new MessageBuilder().setContent(String.format(languageContext.get("commands.game.won_games"),
                        EmoteReference.POPPER, member.getEffectiveName(), db.getPlayer(member).getData().getGamesWon()))
                        .stripMentions(event.getGuild(), Message.MentionType.HERE, Message.MentionType.EVERYONE)
                        .sendTo(event.getChannel())
                        .queue();
            }
        });
        
        gameCommand.addSubCommand("lobby", new SubCommand() {
            @Override
            public String description() {
                return "Starts a game lobby. For example `~>game lobby pokemon, trivia` will start pokemon and then trivia\n" +
                               "If you want to specify the difficulty of trivia, you can use the `-diff` parameter. Example: `~>game lobby pokemon, trivia -diff hard`";
            }
            
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                String[] args = net.kodehawa.mantarobot.utils.StringUtils.advancedSplitArgs(content, 0);
                Map<String, String> t = getArguments(content);
                String difficultyArgument = "diff";
                content = Utils.replaceArguments(t, content, difficultyArgument);
                
                if(content.isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.game.nothing_specified"), EmoteReference.ERROR).queue();
                    return;
                }
                
                if(content.contains("character")) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.game.multiple.no_character"), EmoteReference.ERROR).queue();
                    return;
                }
                
                //Trivia difficulty handling.
                TriviaDifficulty difficulty = null;
                if(t.containsKey(difficultyArgument) && t.get(difficultyArgument) != null) {
                    String d = t.get(difficultyArgument);
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
                
                UserData userData = db.getUser(event.getAuthor()).getData();
                PremiumKey key = db.getPremiumKey(userData.getPremiumKey());
                boolean premium = key != null && key.getDurationDays() > 1;
                if(split.length > (premium ? 8 : 5)) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.game.too_many_games"), EmoteReference.ERROR).queue();
                    return;
                }
                
                LinkedList<Game<?>> gameList = new LinkedList<>();
                for(String s : split) {
                    Function<TriviaDifficulty, Game<?>> f = games.get(s.trim());
                    
                    if(f == null)
                        continue;
                    
                    Game<?> g = f.apply(difficulty);
                    if(g == null)
                        continue;
                    
                    gameList.add(g);
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
            public String description() {
                return "Starts multiple instances of one game, for example `~>game multiple trivia 5` will start trivia 5 times.";
            }
            
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                String[] args = net.kodehawa.mantarobot.utils.StringUtils.advancedSplitArgs(content, 0);
                Map<String, String> t = getArguments(content);
                String difficultyArgument = "diff";
                content = Utils.replaceArguments(t, content, difficultyArgument);
                
                //Trivia difficulty handling.
                TriviaDifficulty difficulty = null;
                
                if(t.containsKey(difficultyArgument) && t.get(difficultyArgument) != null) {
                    String d = t.get(difficultyArgument);
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
                
                if(content.contains("character")) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.game.multiple.no_character"), EmoteReference.ERROR).queue();
                    return;
                }
                
                int number;
                try {
                    number = Integer.parseInt(values[1]);
                } catch(Exception e) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.game.multiple.invalid_times"), EmoteReference.ERROR).queue();
                    return;
                }
                
                if(number > 5) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.game.multiple.too_many_games"), EmoteReference.ERROR).queue();
                    return;
                }
                
                LinkedList<Game<?>> gameList = new LinkedList<>();
                for(int i = 0; i < number; i++) {
                    String value = values[0];
                    String trimmedValue = value.trim();
                    
                    if(trimmedValue.length() == 0)
                        continue;
                    
                    Function<TriviaDifficulty, Game<?>> f = games.get(trimmedValue);
                    
                    if(f == null)
                        continue;
                    
                    Game<?> g = f.apply(difficulty);
                    gameList.add(g);
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
                                                              .prefix("trivia")
                                                              .build();
            
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(!Utils.handleDefaultIncreasingRatelimit(rateLimiter, event.getAuthor(), event, languageContext))
                    return;
                
                String diff = "";
                List<User> mentions = event.getMessage().getMentionedUsers();
                List<Role> roleMentions = event.getMessage().getMentionedRoles();
                
                if(args.length > 0) {
                    diff = args[0].toLowerCase();
                }
                
                TriviaDifficulty difficulty = TriviaDifficulty.lookupFromString(diff);
                
                if(difficulty == null && (mentions.isEmpty() && roleMentions.isEmpty()) && !content.isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.game.trivia.wrong_diff"), EmoteReference.ERROR).queue();
                    return;
                }
                
                startGames(createLinkedList(new Trivia(difficulty)), event, languageContext);
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
    
    private void startGames(LinkedList<Game<?>> games, GuildMessageReceivedEvent event, I18nContext languageContext) {
        if(checkRunning(event, languageContext))
            return;
        
        TextChannel channel = event.getChannel();
        
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
            channel.sendMessageFormat(languageContext.get("commands.game.started_mp_role"), EmoteReference.MEGA, b.toString()).queue();
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
                channel.sendMessageFormat(languageContext.get("commands.game.started_mp_user"), EmoteReference.MEGA, users).queue();
            }
        }
        
        if(games.size() > 1) {
            channel.sendMessageFormat(languageContext.get("commands.game.lobby_started"),
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
}
