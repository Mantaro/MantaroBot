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

import bsh.Interpreter;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.MantaroObj;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.utils.StringUtils.SPLIT_PATTERN;

@Slf4j
@Module
public class OwnerCmd {

    @Subscribe
    public void blacklist(CommandRegistry cr) {
        cr.register("blacklist", new SimpleCommand(Category.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                MantaroObj obj = MantaroData.db().getMantaroData();
                if(args[0].equals("guild")) {
                    if(args[1].equals("add")) {
                        if(MantaroBot.getInstance().getGuildById(args[2]) == null) return;
                        obj.getBlackListedGuilds().add(args[2]);
                        event.getChannel().sendMessage(
                                EmoteReference.CORRECT + "Blacklisted Guild: " + event.getJDA().getGuildById(args[2]))
                                .queue();
                        obj.save();
                    } else if(args[1].equals("remove")) {
                        if(!obj.getBlackListedGuilds().contains(args[2])) return;
                        obj.getBlackListedGuilds().remove(args[2]);
                        event.getChannel().sendMessage(EmoteReference.CORRECT + "Unblacklisted Guild: " + args[2])
                                .queue();
                        obj.save();
                    }
                    return;
                }

                if(args[0].equals("user")) {
                    if(args[1].equals("add")) {
                        if(MantaroBot.getInstance().getUserById(args[2]) == null) return;
                        obj.getBlackListedUsers().add(args[2]);
                        event.getChannel().sendMessage(
                                EmoteReference.CORRECT + "Blacklisted User: " + event.getJDA().getUserById(args[2]))
                                .queue();
                        obj.save();
                    } else if(args[1].equals("remove")) {
                        if(!obj.getBlackListedUsers().contains(args[2])) return;
                        obj.getBlackListedUsers().remove(args[2]);
                        event.getChannel().sendMessage(
                                EmoteReference.CORRECT + "Unblacklisted User: " + event.getJDA().getUserById(args[2]))
                                .queue();
                        obj.save();
                    }
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Blacklist command")
                        .setDescription("**Blacklists a user (user argument) or a guild (guild argument) by id.**")
                        .addField("Examples", "~>blacklist user add/remove 293884638101897216\n" +
                                "~>blacklist guild add/remove 305408763915927552", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void badge(CommandRegistry cr) {
        cr.register("addbadge", new SimpleCommand(Category.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                if(event.getMessage().getMentionedUsers().isEmpty()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You need to give me an user to apply the badge to!").queue();
                    return;
                }

                if(args.length != 2) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "Wrong args length").queue();
                    return;
                }

                String b = args[1];
                List<User> users = event.getMessage().getMentionedUsers();
                Badge badge = Badge.lookupFromString(b);
                if(badge == null) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "No badge with that enum name! Valid badges: " +
                            Arrays.stream(Badge.values()).map(b1 -> "`" + b1.name() + "`").collect(Collectors.joining(" ,"))).queue();
                    return;
                }

                for(User u : users) {
                    Player p = MantaroData.db().getPlayer(u);
                    p.getData().addBadge(badge);
                    p.saveAsync();
                }

                event.getChannel().sendMessage(
                        EmoteReference.CORRECT + "Added badge " + badge + " to " + users.stream().map(User::getName).collect(Collectors.joining(" ,"))
                ).queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return null;
            }
        });

        cr.register("removebadge", new SimpleCommand(Category.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                if(event.getMessage().getMentionedUsers().isEmpty()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You need to give me an user to remove the badge from!").queue();
                    return;
                }

                if(args.length != 2) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "Wrong args length").queue();
                    return;
                }

                String b = args[1];
                List<User> users = event.getMessage().getMentionedUsers();
                Badge badge = Badge.lookupFromString(b);
                if(badge == null) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "No badge with that enum name! Valid badges: " +
                            Arrays.stream(Badge.values()).map(b1 -> "`" + b1.name() + "`").collect(Collectors.joining(" ,"))).queue();
                    return;
                }

                for(User u : users) {
                    Player p = MantaroData.db().getPlayer(u);
                    p.getData().removeBadge(badge);
                    p.saveAsync();
                }

                event.getChannel().sendMessage(
                        EmoteReference.CORRECT + "Removed badge " + badge + " from " + users.stream().map(User::getName).collect(Collectors.joining(" ,"))
                ).queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return null;
            }
        });
    }

    @Subscribe
    public void eval(CommandRegistry cr) {
        Map<String, Evaluator> evals = new HashMap<>();
        evals.put("js", (event, code) -> {
            ScriptEngine script = new ScriptEngineManager().getEngineByName("nashorn");
            script.put("mantaro", MantaroBot.getInstance());
            script.put("db", MantaroData.db());
            script.put("jda", event.getJDA());
            script.put("event", event);
            script.put("guild", event.getGuild());
            script.put("channel", event.getChannel());

            try {
                return script.eval(String.join(
                        "\n",
                        "load(\"nashorn:mozilla_compat.js\");",
                        "imports = new JavaImporter(java.util, java.io, java.net);",
                        "(function() {",
                        "with(imports) {",
                        code,
                        "}",
                        "})()"
                ));
            } catch(Exception e) {
                return e;
            }
        });

        evals.put("bsh", (event, code) -> {
            Interpreter interpreter = new Interpreter();
            try {
                interpreter.set("mantaro", MantaroBot.getInstance());
                interpreter.set("db", MantaroData.db());
                interpreter.set("jda", event.getJDA());
                interpreter.set("event", event);
                interpreter.set("guild", event.getGuild());
                interpreter.set("channel", event.getChannel());

                return interpreter.eval(String.join(
                        "\n",
                        "import *;",
                        code
                ));
            } catch(Exception e) {
                return e;
            }
        });

        cr.register("eval", new SimpleCommand(Category.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                Evaluator evaluator = evals.get(args[0]);
                if(evaluator == null) {
                    onHelp(event);
                    return;
                }

                String[] values = SPLIT_PATTERN.split(content, 2);
                if(values.length < 2) {
                    onHelp(event);
                    return;
                }

                String v = values[1];

                Object result = evaluator.eval(event, v);
                boolean errored = result instanceof Throwable;

                event.getChannel().sendMessage(new EmbedBuilder()
                        .setAuthor(
                                "Evaluated " + (errored ? "and errored" : "with success"), null,
                                event.getAuthor().getAvatarUrl()
                        )
                        .setColor(errored ? Color.RED : Color.GREEN)
                        .setDescription(
                                result == null ? "Executed successfully with no objects returned" : ("Executed " + (errored ? "and errored: " : "successfully and returned: ") + result
                                        .toString()))
                        .setFooter("Asked by: " + event.getAuthor().getName(), null)
                        .build()
                ).queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Eval cmd")
                        .setDescription("**Evaluates stuff (A: js/bsh)**")
                        .build();
            }
        });
    }

    @Subscribe
    public void owner(CommandRegistry cr) {
        cr.register("owner", new SimpleCommand(Category.OWNER) {
            @Override
            public CommandPermission permission() {
                return CommandPermission.OWNER;
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Owner command")
                        .setDescription("`~>owner shutdown/forceshutdown`: Shutdowns the bot\n" +
                                "`~>owner restart/forcerestart`: Restarts the bot.\n" +
                                "`~>owner scheduleshutdown time <time>` - Schedules a fixed amount of seconds the bot will wait to be shutted down.\n" +
                                "`~>owner premium add <id> <days>` - Adds premium to the specified user for x days.")
                        .build();
            }

            @Override
            public void call(GuildMessageReceivedEvent event, String content, String[] args) {
                if(args.length < 1) {
                    onHelp(event);
                    return;
                }

                String option = args[0];

                if(option.equals("premium")) {
                    String sub = args[1].substring(0, args[1].indexOf(' '));
                    if(sub.equals("add")) {
                        try {
                            String userId;
                            String[] values = SPLIT_PATTERN.split(args[1], 3);
                            try {
                                Long.parseLong(values[1]);
                                userId = values[1];
                            } catch(Exception e) {
                                if(!event.getMessage().getMentionedUsers().isEmpty()) {
                                    userId = event.getMessage().getMentionedUsers().get(0).getId();
                                    return;
                                } else {
                                    event.getChannel().sendMessage(EmoteReference.ERROR + "Not a valid user id").queue();
                                    return;
                                }
                            }
                            DBUser db = MantaroData.db().getUser(userId);
                            db.incrementPremium(TimeUnit.DAYS.toMillis(Long.parseLong(values[2])));
                            db.saveAsync();
                            event.getChannel().sendMessage(EmoteReference.CORRECT +
                                    "The premium feature for user " + db.getId() + " now is until " +
                                    new Date(db.getPremiumUntil())).queue();
                            return;
                        } catch(IndexOutOfBoundsException e) {
                            event.getChannel().sendMessage(
                                    EmoteReference.ERROR + "You need to specify id and number of days").queue();
                            e.printStackTrace();
                            return;
                        }
                    }

                    if(sub.equals("guild")) {
                        try {
                            String[] values = SPLIT_PATTERN.split(args[1], 3);
                            DBGuild db = MantaroData.db().getGuild(values[1]);
                            db.incrementPremium(TimeUnit.DAYS.toMillis(Long.parseLong(values[2])));
                            db.saveAsync();
                            event.getChannel().sendMessage(EmoteReference.CORRECT +
                                    "The premium feature for guild " + db.getId() + " now is until " +
                                    new Date(db.getPremiumUntil())).queue();
                            return;
                        } catch(IndexOutOfBoundsException e) {
                            event.getChannel().sendMessage(
                                    EmoteReference.ERROR + "You need to specify id and number of days").queue();
                            e.printStackTrace();
                            return;
                        }
                    }
                }

                onHelp(event);
            }

            @Override
            public String[] splitArgs(String content) {
                return SPLIT_PATTERN.split(content, 2);
            }
        });
    }

    private interface Evaluator {
        Object eval(GuildMessageReceivedEvent event, String code);
    }
}
