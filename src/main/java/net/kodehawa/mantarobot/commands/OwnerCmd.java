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

import com.github.natanbc.javaeval.CompilationException;
import com.github.natanbc.javaeval.CompilationResult;
import com.github.natanbc.javaeval.JavaEvaluator;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.MantaroObj;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.PlayerData;
import net.kodehawa.mantarobot.utils.APIUtils;
import net.kodehawa.mantarobot.utils.Pair;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.utils.StringUtils.SPLIT_PATTERN;

@Module
@SuppressWarnings("unused")
public class OwnerCmd {
    private static final String JAVA_EVAL_IMPORTS = "" +
            "import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;\n" +
            "import net.kodehawa.mantarobot.core.modules.commands.base.Context;\n" +
            "import net.kodehawa.mantarobot.*;\n" +
            "import net.kodehawa.mantarobot.core.listeners.operations.*;\n" +
            "import net.kodehawa.mantarobot.data.*;\n" +
            "import net.kodehawa.mantarobot.db.*;\n" +
            "import net.kodehawa.mantarobot.db.entities.*;\n" +
            "import net.kodehawa.mantarobot.commands.currency.*;\n" +
            "import net.kodehawa.mantarobot.utils.*;\n" +
            "import net.dv8tion.jda.api.entities.*;\n";
    private static final Logger log = LoggerFactory.getLogger(OwnerCmd.class);

    @Subscribe
    public void blacklist(CommandRegistry cr) {
        cr.register("blacklist", new SimpleCommand(Category.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                MantaroObj obj = ctx.db().getMantaroData();

                String context = args[0];
                String action = args[1];

                if (context.equals("guild")) {
                    if (action.equals("add")) {
                        if (MantaroBot.getInstance().getShardManager().getGuildById(args[2]) == null)
                            return;

                        obj.getBlackListedGuilds().add(args[2]);
                        ctx.send(EmoteReference.CORRECT + "Blacklisted Guild: " +
                                MantaroBot.getInstance().getShardManager().getGuildById(args[2]));
                        obj.saveAsync();

                        return;
                    } else if (action.equals("remove")) {
                        if (!obj.getBlackListedGuilds().contains(args[2]))
                            return;

                        obj.getBlackListedGuilds().remove(args[2]);
                        ctx.send(EmoteReference.CORRECT + "Unblacklisted Guild: " + args[2]);
                        obj.saveAsync();

                        return;
                    }

                    ctx.send("Invalid guild scope. (Valid: add, remove)");
                    return;
                }

                if (context.equals("user")) {
                    if (action.equals("add")) {
                        if (MantaroBot.getInstance().getShardManager().getUserById(args[2]) == null) {
                            ctx.send("Can't find user.");
                            return;
                        }

                        obj.getBlackListedUsers().add(args[2]);
                        ctx.send(EmoteReference.CORRECT + "Blacklisted User: " + MantaroBot.getInstance().getShardManager().getUserById(args[2]));
                        obj.saveAsync();

                        return;
                    } else if (action.equals("remove")) {
                        if (!obj.getBlackListedUsers().contains(args[2])) {
                            ctx.send("User not in blacklist.");
                            return;
                        }

                        obj.getBlackListedUsers().remove(args[2]);
                        ctx.send(EmoteReference.CORRECT + "Unblacklisted User: " + MantaroBot.getInstance().getShardManager().getUserById(args[2]));
                        obj.saveAsync();

                        return;
                    }

                    ctx.send("Invalid user scope. (Valid: add, remove)");
                    return;
                }

                ctx.send("Invalid scope. (Valid: user, guild)");
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Blacklists a user (user argument) or a guild (guild argument) by id.\n" +
                                "Examples: ~>blacklist user add/remove 293884638101897216, ~>blacklist guild add/remove 305408763915927552")
                        .build();
            }
        });
    }

    @Subscribe
    public void restoreStreak(CommandRegistry cr) {
        cr.register("restorestreak", new SimpleCommand(Category.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (args.length < 2) {
                    ctx.send("You need to provide the id and the amount");
                    return;
                }

                String id = args[0];
                long amount = Long.parseLong(args[1]);
                User u = MantaroBot.getInstance().getShardManager().getUserById(id);

                if (u == null) {
                    ctx.send("Can't find user");
                    return;
                }

                Player p = MantaroData.db().getPlayer(id);
                PlayerData pd = p.getData();
                pd.setLastDailyAt(System.currentTimeMillis());
                pd.setDailyStreak(amount);

                p.save();

                ctx.send("Done, new streak is " + amount);
            }
        });
    }

    //This is for testing lol
    @Subscribe
    public void giveItem(CommandRegistry cr) {
        cr.register("giveitem", new SimpleCommand(Category.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (content.isEmpty()) {
                    ctx.send(EmoteReference.ERROR + "You need to tell me which item to give you.");
                    return;
                }

                Item i = Items.fromAnyNoId(content).orElse(null);

                if (i == null) {
                    ctx.send(EmoteReference.ERROR + "I didn't find that item.");
                    return;
                }

                Player p = ctx.getPlayer();
                
                if (p.getInventory().getAmount(i) < 5000) {
                    p.getInventory().process(new ItemStack(i, 1));
                } else {
                    ctx.send(EmoteReference.ERROR + "Too many of this item already.");
                }

                p.saveAsync();
                ctx.send("Gave you " + i);
            }
        });
    }

    @Subscribe
    public void transferPlayer(CommandRegistry cr) {
        cr.register("transferplayer", new SimpleCommand(Category.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (content.isEmpty() || args.length < 2) {
                    ctx.send(EmoteReference.ERROR + "You need to tell me the 2 players ids to transfer!");
                    return;
                }

                ctx.send(EmoteReference.WARNING + "You're about to transfer all the player information from " + args[0] + " to " + args[1] + " are you sure you want to continue?");
                InteractiveOperations.create(ctx.getChannel(), ctx.getAuthor().getIdLong(), 30, e -> {
                    if (ctx.getAuthor().getIdLong() != ctx.getAuthor().getIdLong()) {
                        return Operation.IGNORED;
                    }

                    if (ctx.getMessage().getContentRaw().equalsIgnoreCase("yes")) {
                        Player transferred = MantaroData.db().getPlayer(args[0]);
                        Player transferTo = MantaroData.db().getPlayer(args[1]);

                        transferTo.setMoney(transferred.getMoney());
                        transferTo.setLevel(transferred.getLevel());
                        transferTo.setReputation(transferred.getReputation());
                        transferTo.getInventory().merge(transferred.getInventory().asList());

                        PlayerData transferredData = transferred.getData();
                        PlayerData transferToData = transferTo.getData();

                        transferToData.setExperience(transferredData.getExperience());
                        transferToData.setBadges(transferredData.getBadges());
                        transferToData.setShowBadge(transferredData.isShowBadge());
                        transferToData.setMarketUsed(transferredData.getMarketUsed());
                        transferToData.setMainBadge(transferredData.getMainBadge());
                        transferToData.setGamesWon(transferredData.getGamesWon());


                        transferTo.save();
                        Player reset = Player.of(args[0]);
                        reset.save();

                        ctx.send(EmoteReference.CORRECT + "Transfer from " + args[0] + " to " + args[1] + " completed.");

                        return Operation.COMPLETED;
                    }

                    if (e.getMessage().getContentRaw().equalsIgnoreCase("no")) {
                        ctx.send(EmoteReference.CORRECT + "Cancelled.");
                        return Operation.COMPLETED;
                    }

                    return Operation.IGNORED;
                });
            }
        });
    }

    @Subscribe
    public void badge(CommandRegistry cr) {
        cr.register("addbadge", new SimpleCommand(Category.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (args.length != 2) {
                    ctx.send(EmoteReference.ERROR + "Wrong args length");
                    return;
                }

                String b = args[1];
                User user = MantaroBot.getInstance().getShardManager().getUserById(args[0]);

                if (user == null) {
                    ctx.send(EmoteReference.ERROR + "User not found.");
                    return;
                }

                Badge badge = Badge.lookupFromString(b);
                if (badge == null) {
                    ctx.send(EmoteReference.ERROR + "No badge with that enum name! Valid badges: " +
                            Arrays.stream(Badge.values()).map(b1 -> "`" + b1.name() + "`").collect(Collectors.joining(" ,")));
                    return;
                }

                Player p = ctx.getPlayer(user);
                p.getData().addBadgeIfAbsent(badge);
                p.saveAsync();

                ctx.send(EmoteReference.CORRECT + "Added badge " + badge + " to " + user.getAsTag());
            }
        });

        cr.register("removebadge", new SimpleCommand(Category.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                List<User> users = ctx.getMentionedUsers();

                if (users.isEmpty()) {
                    ctx.send(EmoteReference.ERROR + "You need to give me a user to remove the badge from!");
                    return;
                }

                if (args.length != 2) {
                    ctx.send(EmoteReference.ERROR + "Wrong args length");
                    return;
                }

                String b = args[1];
                Badge badge = Badge.lookupFromString(b);
                if (badge == null) {
                    ctx.send(EmoteReference.ERROR + "No badge with that enum name! Valid badges: " +
                            Arrays.stream(Badge.values()).map(b1 -> "`" + b1.name() + "`").collect(Collectors.joining(" ,")));
                    return;
                }

                for (User u : users) {
                    Player p = MantaroData.db().getPlayer(u);
                    p.getData().removeBadge(badge);
                    p.saveAsync();
                }

                ctx.send(
                        String.format("%sRemoved badge %s from %s", EmoteReference.CORRECT, badge, users.stream().map(User::getName).collect(Collectors.joining(" ,")))
                );
            }
        });
    }

    @Subscribe
    public void eval(CommandRegistry cr) {
        //has no state
        JavaEvaluator javaEvaluator = new JavaEvaluator();

        Map<String, Evaluator> evals = new HashMap<>();
        evals.put("java", (ctx, code) -> {
            try {
                CompilationResult r = javaEvaluator.compile()
                        .addCompilerOptions("-Xlint:unchecked")
                        .source("Eval", JAVA_EVAL_IMPORTS + "\n\n" +
                                "public class Eval {\n" +
                                "   public static Object run(Context ctx) throws Throwable {\n" +
                                "       try {\n" +
                                "           return null;\n" +
                                "       } finally {\n" +
                                "           " + (code + ";").replaceAll(";{2,}", ";") + "\n" +
                                "       }\n" +
                                "   }\n" +
                                "}"
                        )
                        .execute();

                EvalClassLoader ecl = new EvalClassLoader();
                r.getClasses().forEach((name, bytes) -> ecl.define(bytes));

                return ecl.loadClass("Eval").getMethod("run", Context.class).invoke(null, ctx);
            } catch (CompilationException e) {
                StringBuilder sb = new StringBuilder("\n");

                if (e.getCompilerOutput() != null)
                    sb.append(e.getCompilerOutput());

                if (!e.getDiagnostics().isEmpty()) {
                    if (sb.length() > 0) sb.append("\n\n");
                    e.getDiagnostics().forEach(d -> sb.append(d).append('\n'));
                }
                return new Error(sb.toString()) {
                    @Override
                    public String toString() {
                        return getMessage();
                    }
                };
            } catch (Exception e) {
                return e;
            }
        });

        cr.register("eval", new SimpleCommand(Category.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                Evaluator evaluator = evals.get(args[0]);
                if (evaluator == null) {
                    ctx.send("That's not a valid evaluator, silly.");
                    return;
                }

                String[] values = SPLIT_PATTERN.split(content, 2);
                if (values.length < 2) {
                    ctx.send("Not enough arguments.");
                    return;
                }

                String v = values[1];

                Object result = evaluator.eval(ctx, v);
                boolean errored = result instanceof Throwable;

                ctx.send(new EmbedBuilder()
                        .setAuthor(
                                "Evaluated " + (errored ? "and errored" : "with success"), null,
                                ctx.getAuthor().getAvatarUrl()
                        )
                        .setColor(errored ? Color.RED : Color.GREEN)
                        .setDescription(
                                result == null ? "Executed successfully with no objects returned" : 
                                        ("Executed " + (errored ? "and errored: " : "successfully and returned: ") + result
                                        .toString())
                        ).setFooter("Asked by: " + ctx.getAuthor().getName(), null)
                        .build()
                );
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Evaluates stuff (A: java).")
                        .build();
            }
        });
    }

    @Subscribe
    public void link(CommandRegistry cr) {
        cr.register("link", new SimpleCommand(Category.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                final Config config = ctx.getConfig();

                if (!config.isPremiumBot()) {
                    ctx.send("This command can only be ran in MP, as it'll link a guild to an MP holder.");
                    return;
                }

                if (args.length < 2) {
                    ctx.send("You need to enter both the user and the guild id (example: 132584525296435200 493297606311542784).");
                    return;
                }

                String userString = args[0];
                String guildString = args[1];
                Guild guild = MantaroBot.getInstance().getShardManager().getGuildById(guildString);
                User user = MantaroBot.getInstance().getShardManager().getUserById(userString);
                if (guild == null || user == null) {
                    ctx.send("User or guild not found.");
                    return;
                }

                final DBGuild dbGuild = MantaroData.db().getGuild(guildString);
                Map<String, String> t = ctx.getOptionalArguments();
                if (t.containsKey("u")) {
                    dbGuild.getData().setMpLinkedTo(null);
                    dbGuild.save();

                    ctx.sendFormat("Un-linked MP for guild %s (%s).", guild.getName(), guild.getId());
                    return;
                }

                Pair<Boolean, String> pledgeInfo = APIUtils.getPledgeInformation(user.getId());
                //guaranteed to be an integer
                if (pledgeInfo == null || !pledgeInfo.getLeft() || Double.parseDouble(pledgeInfo.getRight()) < 4) {
                    ctx.send("Pledge not found, pledge amount not enough or pledge was cancelled.");
                    return;
                }

                //Guild assignment.
                dbGuild.getData().setMpLinkedTo(userString); //Patreon check will run from this user.
                dbGuild.save();

                ctx.sendFormat("Linked MP for guild %s (%s) to user %s (%s). Including this guild in pledge check (id -> user -> pledge).", guild.getName(), guild.getId(), user.getName(), user.getId());
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Links a guild to a patreon owner (user id). Use -u to unlink.")
                        .setUsage("`~>link <user id> <guild id>`")
                        .build();
            }
        });
    }

    @Subscribe
    public void addOwnerPremium(CommandRegistry cr) {
        cr.register("addownerpremium", new SimpleCommand(Category.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (!MantaroData.config().get().isPremiumBot()) {
                    ctx.send("This command can only be ran in MP, as it's only useful there.");
                    return;
                }

                if(args.length < 2) {
                    ctx.send("Wrong amount of arguments. I need the guild id and the amount of days");
                    return;
                }

                String serverId = args[0];
                String days = args[1];

                if(MantaroBot.getInstance().getShardManager().getGuildById(serverId) == null) {
                    ctx.send("Invalid guild.");
                    return;
                }

                long dayAmount;
                try {
                    dayAmount = Long.parseLong(days);
                } catch (NumberFormatException e) {
                    ctx.send("Invalid amount of days.");
                    return;
                }

                DBGuild db = MantaroData.db().getGuild(serverId);
                db.incrementPremium(TimeUnit.DAYS.toMillis(dayAmount));
                db.saveAsync();

                ctx.send(EmoteReference.CORRECT + "The premium feature for guild " + db.getId() + " now is until " + new Date(db.getPremiumUntil()));
            }
        });
    }

    @Subscribe
    public void refreshPledges(CommandRegistry cr) {
        cr.register("refreshpledges", new SimpleCommand(Category.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                try {
                    APIUtils.getFrom("/mantaroapi/bot/patreon/refresh");
                    ctx.send("Refreshed Patreon pledges successfully.");
                } catch (Exception e) {
                    ctx.send("Somehow this failed. Pretty sure that just always returned ok...");
                    e.printStackTrace();
                }
            }
        });
    }

    private interface Evaluator {
        Object eval(Context ctx, String code);
    }

    private static class EvalClassLoader extends ClassLoader {
        public void define(byte[] bytes) {
            super.defineClass(null, bytes, 0, bytes.length);
        }
    }
}
