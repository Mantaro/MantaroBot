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
import com.github.natanbc.javaeval.JavaEvaluator;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.item.ItemHelper;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.currency.seasons.Season;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.NewCommand;
import net.kodehawa.mantarobot.core.command.NewContext;
import net.kodehawa.mantarobot.core.command.argument.Parsers;
import net.kodehawa.mantarobot.core.command.meta.Category;
import net.kodehawa.mantarobot.core.command.meta.Permission;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.utils.APIUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.JsonDataManager;

import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Module
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
            "import net.kodehawa.mantarobot.commands.currency.item.*;\n" +
            "import net.kodehawa.mantarobot.commands.currency.item.special.*;\n" +
            "import net.kodehawa.mantarobot.utils.*;\n" +
            "import net.dv8tion.jda.api.entities.*;\n" +
            "import java.util.*;\n" +
            "import java.util.stream.*;\n" +
            "import java.util.function.*;\n" +
            "import java.lang.reflect.*;\n" +
            "import java.lang.management.*;\n";

    @Subscribe
    public void blacklist(CommandRegistry cr) {
        cr.register("blacklist", new SimpleCommand(CommandCategory.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                var obj = ctx.db().getMantaroData();

                var context = args[0];
                var action = args[1];

                if (context.equals("guild")) {
                    if (action.equals("add")) {
                        if (MantaroBot.getInstance().getShardManager().getGuildById(args[2]) == null) {
                            ctx.send(EmoteReference.ERROR + "Guild is already blacklisted?");
                            return;
                        }

                        obj.getBlackListedGuilds().add(args[2]);
                        ctx.send(EmoteReference.CORRECT + "Blacklisted Guild: " +
                                MantaroBot.getInstance().getShardManager().getGuildById(args[2]));
                        obj.saveAsync();

                        return;
                    } else if (action.equals("remove")) {
                        if (!obj.getBlackListedGuilds().contains(args[2])) {
                            ctx.send(EmoteReference.ERROR + "Guild is not blacklisted?");
                            return;
                        }

                        obj.getBlackListedGuilds().remove(args[2]);
                        ctx.send(EmoteReference.CORRECT + "Unblacklisted Guild: " + args[2]);
                        obj.saveAsync();

                        return;
                    }

                    ctx.send("Invalid guild scope. (Valid: add, remove)");
                    return;
                }

                if (context.equals("user")) {
                    var user = ctx.retrieveUserById(args[2]);
                    if (action.equals("add")) {
                        if (user == null) {
                            ctx.send("Can't find user?");
                            return;
                        }

                        obj.getBlackListedUsers().add(args[2]);
                        ctx.send(EmoteReference.CORRECT + "Blacklisted User: " + user.getAsTag() + " - " + user.getIdLong());
                        obj.saveAsync();

                        return;
                    } else if (action.equals("remove")) {
                        if (!obj.getBlackListedUsers().contains(args[2])) {
                            ctx.send("User not in blacklist.");
                            return;
                        }

                        obj.getBlackListedUsers().remove(args[2]);
                        ctx.send(EmoteReference.CORRECT + "Unblacklisted User: " + user.getAsTag() + " - " + user.getIdLong());
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

    @Permission(CommandPermission.OWNER)
    @Category(CommandCategory.OWNER)
    public static class RestoreStreak extends NewCommand {
        @Override
        protected void process(NewContext ctx) {
            var id = ctx.argument(Parsers.strictLong()
                    .map(String::valueOf), "Invalid id");
            var amount = ctx.argument(Parsers.strictLong(), "Invalid amount");

            var u = ctx.retrieveUserById(id);

            if (u == null) {
                ctx.send("Can't find user");
                return;
            }

            var p = MantaroData.db().getPlayer(id);
            var pd = p.getData();

            pd.setLastDailyAt(System.currentTimeMillis());
            pd.setDailyStreak(amount);

            p.save();

            ctx.send("Done, new streak is " + amount);
        }
    }

    @Subscribe
    public void dataRequest(CommandRegistry cr) {
        cr.register(DataRequest.class);
    }

    @Permission(CommandPermission.OWNER)
    @Category(CommandCategory.OWNER)
    public static class DataRequest extends NewCommand {
        @Override
        protected void process(NewContext ctx) {
            var db = MantaroData.db();
            var id = ctx.argument(Parsers.strictLong()
                    .map(String::valueOf), "Invalid id");

            var user = ctx.retrieveUserById(id);

            if (user == null) {
                ctx.send("Can't find user");
                return;
            }

            var player = db.getPlayer(user);
            var dbUser = db.getUser(user);
            var seasonalPlayerData = db.getPlayerForSeason(user, Season.SECOND);

            try {
                var jsonPlayer = JsonDataManager.toJson(player);
                var jsonUser = JsonDataManager.toJson(dbUser);
                var jsonSeason = JsonDataManager.toJson(seasonalPlayerData);

                var total = String.format("Player:\n%s\n ---- \nUser:\n%s\n ---- \nSeason:\n%s", jsonPlayer, jsonUser, jsonSeason);
                byte[] bytes = total.getBytes(StandardCharsets.UTF_8);

                if (bytes.length > 7_800_000) {
                    ctx.send("Result too big!");
                } else {
                    ctx.sendFile(bytes, "result.json");
                }
            } catch (Exception e) {
                ctx.send("Error. Check logs. " + e.getMessage());
                e.printStackTrace();
            }
        }
    }


    @Subscribe
    public void restoreStreak(CommandRegistry cr) {
        cr.register(RestoreStreak.class);
    }

    //This is for testing lol
    @Subscribe
    public void giveItem(CommandRegistry cr) {
        cr.register("giveitem", new SimpleCommand(CommandCategory.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (content.isEmpty()) {
                    ctx.send(EmoteReference.ERROR + "You need to tell me which item to give you.");
                    return;
                }

                var item = ItemHelper.fromAnyNoId(content).orElse(null);

                if (item == null) {
                    ctx.send(EmoteReference.ERROR + "I didn't find that item.");
                    return;
                }

                var player = ctx.getPlayer();
                
                if (player.getInventory().getAmount(item) < 5000) {
                    player.getInventory().process(new ItemStack(item, 1));
                } else {
                    ctx.send(EmoteReference.ERROR + "Too many of this item already.");
                }

                player.saveAsync();
                ctx.send("Gave you " + item);
            }
        });
    }

    @Subscribe
    public void transferPlayer(CommandRegistry cr) {
        cr.register("transferplayer", new SimpleCommand(CommandCategory.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (content.isEmpty() || args.length < 2) {
                    ctx.send(EmoteReference.ERROR + "You need to tell me the 2 players ids to transfer!");
                    return;
                }

                ctx.send(EmoteReference.WARNING + "You're about to transfer all the player information from " + args[0] + " to " + args[1] + " are you sure you want to continue?");
                InteractiveOperations.create(ctx.getChannel(), ctx.getAuthor().getIdLong(), 30, e -> {
                    if (e.getAuthor().getIdLong() != ctx.getAuthor().getIdLong()) {
                        return Operation.IGNORED;
                    }

                    if (e.getMessage().getContentRaw().equalsIgnoreCase("yes")) {
                        var transferred = MantaroData.db().getPlayer(args[0]);
                        var transferTo = MantaroData.db().getPlayer(args[1]);

                        transferTo.setCurrentMoney(transferred.getCurrentMoney());
                        transferTo.setLevel(transferred.getLevel());
                        transferTo.setReputation(transferred.getReputation());
                        transferTo.getInventory().merge(transferred.getInventory().asList());

                        var transferredData = transferred.getData();
                        var transferToData = transferTo.getData();

                        transferToData.setExperience(transferredData.getExperience());
                        transferToData.setBadges(transferredData.getBadges());
                        transferToData.setShowBadge(transferredData.isShowBadge());
                        transferToData.setMarketUsed(transferredData.getMarketUsed());
                        transferToData.setMainBadge(transferredData.getMainBadge());
                        transferToData.setGamesWon(transferredData.getGamesWon());
                        transferToData.setMiningExperience(transferredData.getMiningExperience());
                        transferToData.setSharksCaught(transferredData.getSharksCaught());
                        transferToData.setFishingExperience(transferredData.getFishingExperience());
                        transferToData.setCratesOpened(transferredData.getCratesOpened());
                        transferToData.setTimesMopped(transferredData.getTimesMopped());

                        transferTo.save();

                        var reset = Player.of(args[0]);
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
        cr.register("addbadge", new SimpleCommand(CommandCategory.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (args.length != 2) {
                    ctx.send(EmoteReference.ERROR + "Wrong args length");
                    return;
                }

                var toAdd = args[1];
                var user = ctx.retrieveUserById(args[0]);

                if (user == null) {
                    ctx.send(EmoteReference.ERROR + "User not found.");
                    return;
                }

                var badge = Badge.lookupFromString(toAdd);
                if (badge == null) {
                    ctx.send(EmoteReference.ERROR + "No badge with that enum name! Valid badges: " +
                            Arrays.stream(Badge.values()).map(b1 -> "`" + b1.name() + "`").collect(Collectors.joining(" ,")));
                    return;
                }

                var player = ctx.getPlayer(user);
                player.getData().addBadgeIfAbsent(badge);
                player.saveAsync();

                ctx.send(EmoteReference.CORRECT + "Added badge " + badge + " to " + user.getAsTag());
            }
        });

        cr.register("removebadge", new SimpleCommand(CommandCategory.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                var users = ctx.getMentionedUsers();

                if (users.isEmpty()) {
                    ctx.send(EmoteReference.ERROR + "You need to give me a user to remove the badge from!");
                    return;
                }

                if (args.length != 2) {
                    ctx.send(EmoteReference.ERROR + "Wrong args length");
                    return;
                }

                var toRemove = args[1];
                var badge = Badge.lookupFromString(toRemove);

                if (badge == null) {
                    ctx.send(EmoteReference.ERROR + "No badge with that enum name! Valid badges: " +
                            Arrays.stream(Badge.values()).map(b1 -> "`" + b1.name() + "`").collect(Collectors.joining(" ,")));
                    return;
                }

                for (var user : users) {
                    Player player = MantaroData.db().getPlayer(user);
                    player.getData().removeBadge(badge);
                    player.saveAsync();
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
        var javaEvaluator = new JavaEvaluator();
        Evaluator eval = (ctx, code) -> {
            try {
                var result = javaEvaluator.compile()
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

                var ecl = new EvalClassLoader();
                result.getClasses().forEach((name, bytes) -> ecl.define(bytes));

                return ecl.loadClass("Eval").getMethod("run", Context.class).invoke(null, ctx);
            } catch (CompilationException ex) {
                var sb = new StringBuilder("\n");

                if (ex.getCompilerOutput() != null) {
                    sb.append(ex.getCompilerOutput());
                }

                if (!ex.getDiagnostics().isEmpty()) {
                    if (sb.length() > 0) {
                        sb.append("\n\n");
                    }

                    ex.getDiagnostics().forEach(d -> sb.append(d).append('\n'));
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
        };

        cr.register("eval", new SimpleCommand(CommandCategory.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (args.length < 1) {
                    ctx.send("Give me something to eval.");
                    return;
                }

                // eval.eval, yes
                var result = eval.eval(ctx, content);
                var errored = result instanceof Throwable;

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
                        .setDescription("Evaluates stuff.")
                        .build();
            }
        });
    }

    @Subscribe
    public void link(CommandRegistry cr) {
        cr.register("link", new SimpleCommand(CommandCategory.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                final var config = ctx.getConfig();

                if (!config.isPremiumBot()) {
                    ctx.send("This command can only be ran in MP, as it'll link a guild to an MP holder.");
                    return;
                }

                if (args.length < 2) {
                    ctx.send("You need to enter both the user and the guild id (example: 132584525296435200 493297606311542784).");
                    return;
                }

                var userString = args[0];
                var guildString = args[1];
                var guild = MantaroBot.getInstance().getShardManager().getGuildById(guildString);
                var user = ctx.retrieveUserById(userString);

                if (guild == null || user == null) {
                    ctx.send("User or guild not found.");
                    return;
                }

                final var dbGuild = MantaroData.db().getGuild(guildString);
                var optionalArguments = ctx.getOptionalArguments();

                if (optionalArguments.containsKey("u")) {
                    dbGuild.getData().setMpLinkedTo(null);
                    dbGuild.save();

                    ctx.sendFormat("Un-linked MP for guild %s (%s).", guild.getName(), guild.getId());
                    return;
                }

                var pledgeInfo = APIUtils.getPledgeInformation(user.getId());

                // Guaranteed to be an integer
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
        cr.register("addownerpremium", new SimpleCommand(CommandCategory.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (!MantaroData.config().get().isPremiumBot()) {
                    ctx.send("This command can only be ran in MP, as it's only useful there.");
                    return;
                }

                if (args.length < 2) {
                    ctx.send("Wrong amount of arguments. I need the guild id and the amount of days");
                    return;
                }

                var serverId = args[0];
                var days = args[1];

                if (MantaroBot.getInstance().getShardManager().getGuildById(serverId) == null) {
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

                var dbGuild = MantaroData.db().getGuild(serverId);
                dbGuild.incrementPremium(TimeUnit.DAYS.toMillis(dayAmount));
                dbGuild.saveAsync();

                ctx.send(EmoteReference.CORRECT +
                        "The premium feature for guild " + dbGuild.getId() + " now is until " + new Date(dbGuild.getPremiumUntil())
                );
            }
        });
    }

    @Subscribe
    public void refreshPledges(CommandRegistry cr) {
        cr.register("refreshpledges", new SimpleCommand(CommandCategory.OWNER, CommandPermission.OWNER) {
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
