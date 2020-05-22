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
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.Waifu;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.base.ITreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Marriage;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import net.kodehawa.mantarobot.db.entities.helpers.PlayerData;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.IncreasingRateLimiter;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Module
//In theory fun category, but created this class to avoid FunCmds to go over 1k lines.
public class RelationshipCmds {
    private static final long waifuBaseValue = 1300L;

    static Waifu calculateWaifuValue(User user) {
        final ManagedDatabase db = MantaroData.db();
        Player waifuPlayer = db.getPlayer(user);
        PlayerData waifuPlayerData = waifuPlayer.getData();
        UserData waifuUserData = db.getUser(user).getData();

        long waifuValue = waifuBaseValue;
        long performance;
        //For every 135000 money owned, it increases by 7% base value (base: 1300)
        //For every 3 badges, it increases by 17% base value.
        //For every 2780 experience, the value increases by 18% of the base value.
        //After all those calculations are complete, the value then is calculated using final * (reputation scale / 10) where reputation scale goes up by 1 every 10 reputation points.
        //For every 3 waifu claims, the final value increases by 5% of the base value.
        //Maximum waifu value is Integer.MAX_VALUE.

        //Money calculation.
        long moneyValue = Math.round(Math.max(1, (int) (waifuPlayer.getMoney() / 135000)) * calculatePercentage(6));
        //Badge calculation.
        long badgeValue = Math.round(Math.max(1, (waifuPlayerData.getBadges().size() / 3)) * calculatePercentage(17));
        //Experience calculator.
        long experienceValue = Math.round(Math.max(1, (int) (waifuPlayer.getData().getExperience() / 2780)) * calculatePercentage(18));
        //Claim calculator.
        long claimValue = Math.round(Math.max(1, (waifuUserData.getTimesClaimed() / 3)) * calculatePercentage(5));

        //"final" value
        waifuValue += moneyValue + badgeValue + experienceValue + claimValue;

        //what is this lol
        //After all those calculations are complete, the value then is calculated using final * (reputation scale / 20) where reputation scale goes up by 1 every 10 reputation points.
        //At 6000 reputation points, the waifu value gets multiplied by 1.1. This is the maximum amount it can be multiplied to.
        //to implement later: Reputation scaling is capped at 3.9k. Then at 6.5k the multiplier is applied.
        long reputation = waifuPlayer.getReputation();
        double reputationScaling = (reputation / 4.5) / 20;
        long finalValue = (long) (
                Math.min(Integer.MAX_VALUE,
                        (waifuValue * (reputationScaling > 1 ? reputationScaling : 1) * (reputation > 6500 ? 1.1 : 1)
                        )
                ));

        //waifu pp, yes btmcLewd
        int divide = (int) (moneyValue / 1348);
        performance = ((waifuValue - (waifuBaseValue + 450)) + (long) ((reputationScaling > 1 ? reputationScaling : 1) * 1.2)) / (divide > 1 ? divide : 3);
        //possible?
        if (performance < 0)
            performance = 0;


        return new Waifu(moneyValue, badgeValue, experienceValue, reputationScaling, claimValue, finalValue, performance);
    }

    //Yes, I had to do it, fuck.
    private static long calculatePercentage(long percentage) {
        return (percentage * RelationshipCmds.waifuBaseValue) / 100;
    }

    @Subscribe
    public void marry(CommandRegistry cr) {
        ITreeCommand marryCommand = (ITreeCommand) cr.register("marry", new TreeCommand(Category.FUN) {
            @Override
            public Command defaultTrigger(Context ctx, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, String content) {
                        if (ctx.getMentionedUsers().isEmpty()) {
                            ctx.sendLocalized("commands.marry.no_mention", EmoteReference.ERROR);
                            return;
                        }

                        //MARRIAGE SYSTEM EXPLANATION.
                        //UserData stores marriage UUID, Marriages table on rethink stores the Marriage document based on the UUID assigned.
                        //Onto the UUID we need to encode userId + timestamp of the proposing player and the proposed to player after the acceptance is done.
                        //Scrapping a marriage is easy, just remove the document from the db and reset marriageId field on the User.
                        //A marriage IS only between 2 people, but a waifu system will be implemented down the road (waifus field in UserData being a List of User)
                        //To check whether the person is married to the person proposing to, we can check if their Marriage ID is the same instead of all this bullshit.
                        //If the marriageId field exists but it's missing the document on the marriages db, we can scrape that as non existent (race condition?)
                        //Already-married people will just be checked whether the getMarriage is not null on UserData (that redirects to the db call to get the Marriage object,
                        //not only the id.
                        //Person proposing NEEDS 2 rings and them deducted from their inventory. A love letter can be randomly dropped.
                        //After the love letter is dropped, the proposing person can write on it and transfer the written letter to their loved one, as a read-only receipt that
                        //will be scrapped completely after the marriage ends.
                        //Marriage date will be saved as Instant.now().toEpochMilli().
                        //A denied marriage will give the denied badge, a successful one will give the married badge.
                        //A successfully delivered love letter will give you the lover badge.
                        //CANNOT marry bots, yourself, people already married, if you're married.
                        //Confirmation cannot happen if the rings are missing. Timeout for confirmation is at MOST 2 minutes.
                        //If the receipt has more than 5000 rings, remove rings from the person giving it and scrape them.

                        //We don't need to change those. I sure fucking hope we don't.
                        final DBGuild dbGuild = ctx.getDBGuild();

                        User proposingUser = ctx.getAuthor();
                        User proposedToUser = ctx.getMentionedUsers().get(0);

                        //This is just for checking purposes, so we don't need the DBUser itself.
                        UserData proposingUserData = ctx.getDBUser(proposingUser).getData();
                        UserData proposedToUserData = ctx.getDBUser(proposedToUser).getData();

                        //Again just for checking, and no need to change.
                        final Inventory proposingPlayerInventory = ctx.getPlayer(proposingUser).getInventory();

                        //Why would you do this...
                        if (proposedToUser.getId().equals(ctx.getAuthor().getId())) {
                            ctx.sendLocalized("commands.marry.marry_yourself_notice", EmoteReference.ERROR);
                            return;
                        }

                        final Marriage proposingMarriage = proposingUserData.getMarriage();
                        final Marriage proposedToMarriage = proposedToUserData.getMarriage();

                        //We need to conduct a bunch of checks here.
                        //You CANNOT marry bots, yourself, people already married, or engage on another marriage if you're married.
                        //On the latter, the waifu system will be avaliable on release.
                        //Confirmation cannot happen if the rings are missing. Timeout for confirmation is at MOST 2 minutes.
                        //If the receipt has more than 5000 rings, remove rings from the person giving it and scrape them.

                        //Proposed to is a bot user, cannot marry bots, this is still not 2100.
                        if (proposedToUser.isBot()) {
                            ctx.sendLocalized("commands.marry.marry_bot_notice", EmoteReference.ERROR);
                            return;
                        }

                        //Already married to the same person you're proposing to.
                        if ((proposingMarriage != null && proposedToMarriage != null) && proposedToUserData.getMarriage().getId().equals(proposingMarriage.getId())) {
                            ctx.sendLocalized("commands.marry.already_married_receipt", EmoteReference.ERROR);
                            return;
                        }

                        //You're already married. Huh huh.
                        if (proposingMarriage != null) {
                            ctx.sendLocalized("commands.marry.already_married", EmoteReference.ERROR);
                            return;
                        }

                        //Receipt is married, cannot continue.
                        if (proposedToMarriage != null) {
                            ctx.sendLocalized("commands.marry.receipt_married", EmoteReference.ERROR);
                            return;
                        }

                        //Not enough rings to continue. Buy more rings w.
                        if (!proposingPlayerInventory.containsItem(Items.RING) || proposingPlayerInventory.getAmount(Items.RING) < 2) {
                            ctx.sendLocalized("commands.marry.no_ring", EmoteReference.ERROR);
                            return;
                        }

                        //Send confirmation message.
                        ctx.sendLocalized("commands.marry.confirmation", EmoteReference.MEGA,
                                proposedToUser.getName(), ctx.getAuthor().getName(), EmoteReference.STOPWATCH
                        );

                        InteractiveOperations.create(ctx.getChannel(), ctx.getAuthor().getIdLong(), 120, (ie) -> {
                            //Ignore all messages from anyone that isn't the user we already proposed to. Waiting for confirmation...
                            if (!ie.getAuthor().getId().equals(proposedToUser.getId()))
                                return Operation.IGNORED;

                            //Replace prefix because people seem to think you have to add the prefix before saying yes.
                            String message = ie.getMessage().getContentRaw();
                            for (String s : ctx.getConfig().prefix) {
                                if (message.toLowerCase().startsWith(s)) {
                                    message = message.substring(s.length());
                                }
                            }

                            String guildCustomPrefix = dbGuild.getData().getGuildCustomPrefix();
                            if (guildCustomPrefix != null && !guildCustomPrefix.isEmpty() && message.toLowerCase().startsWith(guildCustomPrefix)) {
                                message = message.substring(guildCustomPrefix.length());
                            }
                            //End of prefix replacing.

                            //Lovely~ <3
                            if (message.equalsIgnoreCase("yes")) {
                                //Here we NEED to get the Player,
                                //User and Marriage objects once again to avoid race conditions or changes on those that might have happened on the 120 seconds that this lasted for.
                                //We need to check if the marriage is empty once again before continuing, also if we have enough rings!
                                //USE THOSE VARIABLES TO MODIFY DATA, NOT THE ONES USED TO CHECK BEFORE THE CONFIRMATION MESSAGE. THIS IS EXTREMELY IMPORTANT.
                                //Else we end up with really annoying to debug bugs, lol.
                                Player proposingPlayer = ctx.getPlayer(proposingUser);
                                Player proposedToPlayer = ctx.getPlayer(proposedToUser);
                                DBUser proposingUserDB = ctx.getDBUser(proposingUser);
                                DBUser proposedToUserDB = ctx.getDBUser(proposedToUser);

                                // ---------------- START OF FINAL MARRIAGE CHECK ----------------
                                final Marriage proposingMarriageFinal = proposingUserDB.getData().getMarriage();
                                final Marriage proposedToMarriageFinal = proposedToUserDB.getData().getMarriage();

                                if (proposingMarriageFinal != null) {
                                    ctx.sendLocalized("commands.marry.already_married", EmoteReference.ERROR);
                                    return Operation.COMPLETED;
                                }

                                if (proposedToMarriageFinal != null) {
                                    ctx.sendLocalized("commands.marry.receipt_married", EmoteReference.ERROR);
                                    return Operation.COMPLETED;
                                }
                                // ---------------- END OF FINAL MARRIAGE CHECK ----------------

                                // ---------------- START OF INVENTORY CHECKS ----------------
                                //LAST inventory check and ring assignment is gonna happen using those.
                                final Inventory proposingPlayerFinalInventory = proposingPlayer.getInventory();
                                final Inventory proposedToPlayerInventory = proposedToPlayer.getInventory();

                                if (proposingPlayerFinalInventory.getAmount(Items.RING) < 2) {
                                    ctx.sendLocalized("commands.marry.ring_check_fail", EmoteReference.ERROR);
                                    return Operation.COMPLETED;
                                }

                                //Remove the ring from the proposing player inventory.
                                proposingPlayerFinalInventory.process(new ItemStack(Items.RING, -1));

                                //Silently scrape the rings if the receipt has more than 5000 rings.
                                if (proposedToPlayerInventory.getAmount(Items.RING) < 5000) {
                                    proposedToPlayerInventory.process(new ItemStack(Items.RING, 1));
                                }
                                // ---------------- END OF INVENTORY CHECKS ----------------

                                // ---------------- START OF MARRIAGE ASSIGNMENT ----------------
                                final long marriageCreationMillis = Instant.now().toEpochMilli();
                                //Onto the UUID we need to encode userId + timestamp of the proposing player and the proposed to player after the acceptance is done.
                                String marriageId = new UUID(proposingUser.getIdLong(), proposedToUser.getIdLong()).toString();

                                //Make and save the new marriage object.
                                Marriage actualMarriage = Marriage.of(marriageId, proposingUser, proposedToUser);
                                actualMarriage.getData().setMarriageCreationMillis(marriageCreationMillis);
                                actualMarriage.save();

                                //Assign the marriage ID to the respective users and save it.
                                proposingUserDB.getData().setMarriageId(marriageId);
                                proposedToUserDB.getData().setMarriageId(marriageId);
                                proposingUserDB.save();
                                proposedToUserDB.save();
                                //---------------- END OF MARRIAGE ASSIGNMENT ----------------

                                //Send marriage confirmation message.
                                ctx.sendLocalized("commands.marry.accepted",
                                        EmoteReference.POPPER, ie.getAuthor().getName(),
                                        ie.getAuthor().getDiscriminator(), proposingUser.getName(), proposingUser.getDiscriminator()
                                );

                                //Add the badge to the married couple.
                                proposingPlayer.getData().addBadgeIfAbsent(Badge.MARRIED);
                                proposedToPlayer.getData().addBadgeIfAbsent(Badge.MARRIED);

                                //Give a love letter both to the proposing player and the one who was proposed to.
                                if (proposingPlayerFinalInventory.getAmount(Items.LOVE_LETTER) < 5000)
                                    proposingPlayerFinalInventory.process(new ItemStack(Items.LOVE_LETTER, 1));

                                if (proposedToPlayerInventory.getAmount(Items.LOVE_LETTER) < 5000)
                                    proposedToPlayerInventory.process(new ItemStack(Items.LOVE_LETTER, 1));

                                //Badge assignment saving.
                                proposingPlayer.save();
                                proposedToPlayer.save();

                                return Operation.COMPLETED;
                            }

                            if (message.equalsIgnoreCase("no")) {
                                ctx.sendLocalized("commands.marry.denied", EmoteReference.CORRECT, proposingUser.getName());

                                //Well, we have a badge for this too. Consolation prize I guess.
                                final Player proposingPlayer = ctx.getPlayer(proposingUser);
                                proposingPlayer.getData().addBadgeIfAbsent(Badge.DENIED);
                                proposingPlayer.saveAsync();
                                return Operation.COMPLETED;
                            }

                            return Operation.IGNORED;
                        });
                    }
                };
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Basically marries you with a user.")
                        .setUsage("`~>marry <@mention>` - Propose to someone\n" +
                                "`~>marry <command>`")
                        .addParameter("@mention", "The person to propose to")
                        .addParameter("command", "The subcommand you can use. Check the subcommands section for a list and usage of each.")
                        .build();
            }
        });

        marryCommand.addSubCommand("createletter", new SubCommand() {
            @Override
            public String description() {
                return "Create a love letter for your marriage. Usage: `~>marry createletter <content>`";
            }

            @Override
            protected void call(Context ctx, String content) {
                final User author = ctx.getAuthor();

                Player player = ctx.getPlayer();
                Inventory playerInventory = player.getInventory();
                DBUser dbUser = ctx.getDBUser();

                //Without one love letter we cannot do much, ya know.
                if (playerInventory.containsItem(Items.LOVE_LETTER)) {
                    final Marriage currentMarriage = dbUser.getData().getMarriage();

                    //Check if the user is married, is the proposed player, there's no love letter and that the love letter is less than 1500 characters long.
                    if (currentMarriage == null) {
                        ctx.sendLocalized("commands.marry.loveletter.no_marriage", EmoteReference.SAD);
                        return;
                    }

                    if (!author.getId().equals(currentMarriage.getPlayer1())) {
                        ctx.sendLocalized("commands.marry.loveletter.not_proposing_player", EmoteReference.ERROR);
                        return;
                    }

                    if (currentMarriage.getData().getLoveLetter() != null) {
                        ctx.sendLocalized("commands.marry.loveletter.already_done", EmoteReference.ERROR);
                        return;
                    }

                    if (content.isEmpty()) {
                        ctx.sendLocalized("commands.marry.loveletter.empty", EmoteReference.ERROR);
                        return;
                    }

                    if (content.length() > 500) {
                        ctx.sendLocalized("commands.marry.loveletter.too_long", EmoteReference.ERROR);
                        return;
                    }

                    //Can we find the user this is married to?
                    final User marriedTo = MantaroBot.getInstance().getShardManager().getUserById(currentMarriage.getOtherPlayer(author.getId()));
                    if (marriedTo == null) {
                        ctx.sendLocalized("commands.marry.loveletter.cannot_see", EmoteReference.ERROR);
                        return;
                    }

                    //Send a confirmation message.
                    String finalContent = Utils.DISCORD_INVITE.matcher(content).replaceAll("-invite link-");
                    finalContent = Utils.DISCORD_INVITE_2.matcher(finalContent).replaceAll("-invite link-");

                    ctx.sendStrippedLocalized("commands.marry.loveletter.confirmation", EmoteReference.TALKING, marriedTo.getName(),
                            marriedTo.getDiscriminator(), finalContent
                    );

                    //Start the operation.
                    InteractiveOperations.create(ctx.getChannel(), author.getIdLong(), 60, e -> {
                        if (!e.getAuthor().getId().equals(author.getId())) {
                            return Operation.IGNORED;
                        }

                        //Replace prefix because people seem to think you have to add the prefix before saying yes.
                        String c = e.getMessage().getContentRaw();
                        for (String s : ctx.getConfig().prefix) {
                            if (c.toLowerCase().startsWith(s)) {
                                c = c.substring(s.length());
                            }
                        }

                        String guildCustomPrefix = ctx.getDBGuild().getData().getGuildCustomPrefix();
                        if (guildCustomPrefix != null && !guildCustomPrefix.isEmpty() && c.toLowerCase().startsWith(guildCustomPrefix)) {
                            c = c.substring(guildCustomPrefix.length());
                        }
                        //End of prefix replacing.

                        //Confirmed they want to save this as the permanent love letter.
                        if (c.equalsIgnoreCase("yes")) {
                            final Player playerFinal = ctx.getPlayer();
                            final Inventory inventoryFinal = playerFinal.getInventory();
                            final Marriage currentMarriageFinal = dbUser.getData().getMarriage();

                            //We need to do most of the checks all over again just to make sure nothing important slipped through.
                            if (currentMarriageFinal == null) {
                                ctx.sendLocalized("commands.marry.loveletter.no_marriage", EmoteReference.SAD);
                                return Operation.COMPLETED;
                            }

                            if (!inventoryFinal.containsItem(Items.LOVE_LETTER)) {
                                ctx.sendLocalized("commands.marry.loveletter.no_letter", EmoteReference.SAD);
                                return Operation.COMPLETED;
                            }

                            //Remove the love letter from the inventory.
                            inventoryFinal.process(new ItemStack(Items.LOVE_LETTER, -1));
                            playerFinal.save();

                            //Save the love letter. The content variable is the actual letter, while c is the content of the operation itself.
                            //Yes it's confusing.
                            currentMarriageFinal.getData().setLoveLetter(content);
                            currentMarriageFinal.save();

                            ctx.sendLocalized("commands.marry.loveletter.confirmed", EmoteReference.CORRECT);
                            return Operation.COMPLETED;
                        } else if (c.equalsIgnoreCase("no")) {
                            ctx.sendLocalized("commands.marry.loveletter.scrapped", EmoteReference.CORRECT);
                            return Operation.COMPLETED;
                        }

                        return Operation.IGNORED;
                    });
                } else {
                    ctx.sendLocalized("commands.marry.loveletter.no_letter", EmoteReference.SAD);
                }
            }
        });

        marryCommand.addSubCommand("status", new SubCommand() {
            @Override
            public String description() {
                return "Check your marriage status.";
            }

            @Override
            protected void call(Context ctx, String content) {
                final User author = ctx.getAuthor();
                DBUser dbUser = ctx.getDBUser();
                final Marriage currentMarriage = dbUser.getData().getMarriage();

                //What status would we have without marriage? Well, we can be unmarried omegalul.
                if (currentMarriage == null) {
                    ctx.sendLocalized("commands.marry.status.no_marriage", EmoteReference.SAD);
                    return;
                }

                //Can we find the user this is married to?
                final User marriedTo = MantaroBot.getInstance().getShardManager().getUserById(currentMarriage.getOtherPlayer(author.getId()));
                if (marriedTo == null) {
                    ctx.sendLocalized("commands.marry.loveletter.cannot_see", EmoteReference.ERROR);
                    return;
                }

                //Get the current love letter.
                String loveLetter = currentMarriage.getData().getLoveLetter();
                if (loveLetter == null || loveLetter.isEmpty()) {
                    loveLetter = "None.";
                }

                I18nContext languageContext = ctx.getLanguageContext();
                DBUser marriedDBUser = ctx.getDBUser(marriedTo);

                //This would be good if it was 2008. But it works.
                Date marriageDate = new Date(currentMarriage.getData().getMarriageCreationMillis());
                boolean eitherHasWaifus = !(dbUser.getData().getWaifus().isEmpty() && marriedDBUser.getData().getWaifus().isEmpty());

                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setThumbnail("http://www.hey.fr/fun/emoji/twitter/en/twitter/469-emoji_twitter_sparkling_heart.png")
                        .setAuthor(languageContext.get("commands.marry.status.header"), null, ctx.getAuthor().getEffectiveAvatarUrl())
                        .setDescription(String.format(languageContext.get("commands.marry.status.description_format"),
                                EmoteReference.HEART, author.getName(), author.getDiscriminator(), marriedTo.getName(), marriedTo.getDiscriminator())
                        ).addField(languageContext.get("commands.marry.status.date"), marriageDate.toString(), false)
                        .addField(languageContext.get("commands.marry.status.love_letter"), loveLetter, false)
                        .addField(languageContext.get("commands.marry.status.waifus"), String.valueOf(eitherHasWaifus), false)
                        .setFooter("Marriage ID: " + currentMarriage.getId(), null);


                ctx.send(embedBuilder.build());
            }
        });

        cr.registerAlias("marry", "marriage");
    }

    @Subscribe
    public void divorce(CommandRegistry cr) {
        cr.register("divorce", new SimpleCommand(Category.FUN) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                final Player divorceePlayer = ctx.getPlayer();
                //Assume we're dealing with a new marriage?
                final DBUser divorceeDBUser = ctx.getDBUser();
                final Marriage marriage = divorceeDBUser.getData().getMarriage();

                //We, indeed, have no marriage here.
                if (marriage == null) {
                    ctx.sendLocalized("commands.divorce.not_married", EmoteReference.ERROR);
                    return;
                }

                //We do have a marriage, get rid of it.
                DBUser marriedWithDBUser = ctx.getDBUser(marriage.getOtherPlayer(ctx.getAuthor().getId()));
                final Player marriedWithPlayer = ctx.getPlayer(marriedWithDBUser.getId());

                //Save the user of the person they were married with.
                marriedWithDBUser.getData().setMarriageId(null);
                marriedWithDBUser.save();

                //Save the user of themselves.
                divorceeDBUser.getData().setMarriageId(null);
                divorceeDBUser.save();

                //Add the heart broken badge to the user who divorced.
                divorceePlayer.getData().addBadgeIfAbsent(Badge.HEART_BROKEN);
                divorceePlayer.save();

                //Add the heart broken badge to the user got dumped.
                marriedWithPlayer.getData().addBadgeIfAbsent(Badge.HEART_BROKEN);
                marriedWithPlayer.save();

                //Scrape this marriage.
                marriage.delete();
                ctx.sendLocalized("commands.divorce.success", EmoteReference.CORRECT);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Basically divorces you from whoever you were married to.")
                        .build();
            }
        });
    }

    @Subscribe
    public void waifu(CommandRegistry cr) {
        IncreasingRateLimiter rl = new IncreasingRateLimiter.Builder()
                .limit(1)
                .spamTolerance(2)
                .cooldown(5, TimeUnit.SECONDS)
                .maxCooldown(5, TimeUnit.SECONDS)
                .randomIncrement(true)
                .pool(MantaroData.getDefaultJedisPool())
                .prefix("waifu")
                .build();


        TreeCommand waifu = (TreeCommand) cr.register("waifu", new TreeCommand(Category.FUN) {
            @Override
            public Command defaultTrigger(Context ctx, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, String content) {
                        //IMPLEMENTATION NOTES FOR THE WAIFU SYSTEM
                        //You get 3 free slots to put "waifus" in. Each extra slot (up to 9) costs exponentially more than the last one (2x more than the costs of the last one)
                        //Every waifu has a "claim" price which increases in the following situations:
                        //For every 100000 money owned, it increases by 3% base value (base: 1500)
                        //For every 10 badges, it increases by 20% base value.
                        //For every 1000 experience, the value increases by 20% of the base value.
                        //After all those calculations are complete, the value then is calculated using final * (reputation scale / 10) where reputation scale goes up by 1 every 10 reputation points.
                        //Maximum waifu value is Integer.MAX_VALUE.
                        //Having a common waifu with your married partner will increase some marriage stats.
                        //If you claim a waifu, and then your waifu claims you, that will unlock the "Mutual" achievement.
                        //If the waifu status is mutual, the MP game boost will go up by 20% and giving your daily to that waifu will increase the amount of money that your
                        //waifu will receive.

                        Map<String, String> opts = ctx.getOptionalArguments();

                        //Default call will bring out the waifu list.
                        DBUser dbUser = ctx.getDBUser();
                        UserData userData = dbUser.getData();
                        I18nContext languageContext = ctx.getLanguageContext();
                        Player player = ctx.getPlayer();
                        if(player.getData().isWaifuout()) {
                            ctx.sendLocalized("commands.waifu.optout.notice", EmoteReference.ERROR);
                            return;
                        }

                        String description = userData.getWaifus().isEmpty() ?
                                languageContext.get("commands.waifu.waifu_header") + "\n" + languageContext.get("commands.waifu.no_waifu") :
                                languageContext.get("commands.waifu.waifu_header");

                        EmbedBuilder waifusEmbed = new EmbedBuilder()
                                .setAuthor(languageContext.get("commands.waifu.header"), null, ctx.getAuthor().getEffectiveAvatarUrl())
                                .setThumbnail("https://i.imgur.com/2JlMtCe.png")
                                .setColor(Color.CYAN)
                                .setFooter(String.format(languageContext.get("commands.waifu.footer"), userData.getWaifus().size(), userData.getWaifuSlots() - userData.getWaifus().size()), null);

                        if (userData.getWaifus().isEmpty()) {
                            waifusEmbed.setDescription(description);
                            ctx.send(waifusEmbed.build());
                            return;
                        }

                        boolean id = opts.containsKey("id");
                        List<String> toRemove = new ArrayList<>();

                        List<MessageEmbed.Field> fields = new LinkedList<>();
                        for (String waifu : userData.getWaifus().keySet()) {
                            User user = MantaroBot.getInstance().getShardManager().getUserById(waifu);
                            if (user == null) {
                                fields.add(new MessageEmbed.Field(EmoteReference.BLUE_SMALL_MARKER + String.format("Unknown User (ID: %s)", waifu),
                                        languageContext.get("commands.waifu.value_format") + " unknown\n" +
                                                languageContext.get("commands.waifu.value_b_format") + " " + userData.getWaifus().get(waifu) +
                                                languageContext.get("commands.waifu.credits_format"), false)
                                );
                            } else {
                                Player waifuClaimed = ctx.getPlayer(user);
                                if(waifuClaimed.getData().isWaifuout()) {
                                    toRemove.add(waifu);
                                    continue;
                                }

                                fields.add(new MessageEmbed.Field(EmoteReference.BLUE_SMALL_MARKER + user.getName() + (!userData.isPrivateTag() ? "#" + user.getDiscriminator() : ""),
                                        (id ? languageContext.get("commands.waifu.id") + " " + user.getId() + "\n" : "") +
                                                languageContext.get("commands.waifu.value_format") + " " + calculateWaifuValue(user).getFinalValue() + " " +
                                                languageContext.get("commands.waifu.credits_format") + "\n" +
                                                languageContext.get("commands.waifu.value_b_format") + " " + userData.getWaifus().get(waifu) +
                                                languageContext.get("commands.waifu.credits_format"), false)
                                );
                            }
                        }

                        List<List<MessageEmbed.Field>> splitFields = DiscordUtils.divideFields(4, fields);
                        boolean hasReactionPerms = ctx.hasReactionPerms();

                        if (hasReactionPerms) {
                            waifusEmbed.setDescription(
                                    languageContext.get("general.arrow_react") + "\n" +
                                            String.format(languageContext.get("commands.waifu.description_header"), userData.getWaifuSlots()) + description
                            );

                            DiscordUtils.list(ctx.getEvent(), 60, false, waifusEmbed, splitFields);
                        } else {
                            waifusEmbed.setDescription(
                                    languageContext.get("general.text_menu") + "\n" +
                                            String.format(languageContext.get("commands.waifu.description_header"), userData.getWaifuSlots()) + description
                            );

                            DiscordUtils.listText(ctx.getEvent(), 60, false, waifusEmbed, splitFields);
                        }

                        if(!toRemove.isEmpty()) {
                            for(String remove : toRemove) {
                                dbUser.getData().getWaifus().remove(remove);
                            }

                            player.saveAsync();
                        }
                    }
                };
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("This command is the hub for all waifu operations. Yeah, it's all fiction.")
                        .setUsage("`~>waifu` - Shows a list of all your waifus and their current value.\n" +
                                "`~>waifu [command] [@user]`")
                        .addParameterOptional("command", "The subcommand to use. Check the sub-command section for more information on which ones you can use.")
                        .addParameterOptional("@user", "The user you want to do the action with.")
                        .addParameterOptional("-id", "Shows the user id.")
                        .build();
            }
        });

        cr.registerAlias("waifu", "waifus");
        waifu.setPredicate(ctx -> Utils.handleIncreasingRatelimit(rl, ctx.getAuthor(), ctx.getEvent(), null, false));

        waifu.addSubCommand("optout", new SubCommand() {
            @Override
            public String description() {
                return "Opt-out of the waifu stuff. This will disable the waifu system, remove all of your claims and make you unable to be claimed.";
            }

            @Override
            protected void call(Context ctx, String content) {
                ctx.sendLocalized("commands.waifu.optout.warning", EmoteReference.WARNING);
                Player player = ctx.getPlayer();
                if(player.getData().isWaifuout()) {
                    ctx.sendLocalized("commands.waifu.optout.notice", EmoteReference.ERROR);
                    return;
                }

                InteractiveOperations.create(ctx.getChannel(), ctx.getAuthor().getIdLong(), 60, e -> {
                    if (!e.getAuthor().getId().equals(ctx.getAuthor().getId())) {
                        return Operation.IGNORED;
                    }

                    String c = e.getMessage().getContentRaw();

                    if (c.equalsIgnoreCase("yes")) {
                        player.getData().setWaifuout(true);
                        ctx.sendLocalized("commands.waifu.optout.success", EmoteReference.CORRECT);
                        player.saveAsync();
                        return Operation.COMPLETED;
                    } else if (c.equalsIgnoreCase("no")) {
                        ctx.sendLocalized("commands.waifu.optout.cancelled", EmoteReference.CORRECT);
                        return Operation.COMPLETED;
                    }

                    return Operation.IGNORED;
                });
            }
        });

        waifu.addSubCommand("stats", new SubCommand() {
            @Override
            public String description() {
                return "Shows your waifu stats or the stats or someone's (by mentioning them)";
            }

            @Override
            protected void call(Context ctx, String content) {
                Player player = ctx.getPlayer();
                if(player.getData().isWaifuout()) {
                    ctx.sendLocalized("commands.waifu.optout.notice", EmoteReference.ERROR);
                    return;
                }

                Member member = Utils.findMember(ctx.getEvent(), ctx.getMember(), content);
                if (member == null)
                    return;

                User toLookup = member.getUser();
                if (toLookup.isBot()) {
                    ctx.sendLocalized("commands.waifu.bot", EmoteReference.ERROR);
                    return;
                }

                Waifu waifuStats = calculateWaifuValue(toLookup);
                I18nContext languageContext = ctx.getLanguageContext();

                EmbedBuilder statsBuilder = new EmbedBuilder()
                        .setThumbnail(toLookup.getEffectiveAvatarUrl())
                        .setAuthor(toLookup == ctx.getAuthor() ?
                                        languageContext.get("commands.waifu.stats.header") :
                                        String.format(languageContext.get("commands.waifu.stats.header_other"), toLookup.getName()),
                                null, toLookup.getEffectiveAvatarUrl()
                        ).setColor(Color.PINK)
                        .setDescription(String.format(languageContext.get("commands.waifu.stats.format"),
                                EmoteReference.BLUE_SMALL_MARKER, waifuStats.getMoneyValue(), waifuStats.getBadgeValue(),
                                waifuStats.getExperienceValue(), waifuStats.getClaimValue(), waifuStats.getReputationMultiplier())
                        ).addField(languageContext.get("commands.waifu.stats.performance"),
                                EmoteReference.ZAP.toString() + waifuStats.getPerformance() + "wp", true
                        ).addField(languageContext.get("commands.waifu.stats.value"), EmoteReference.BUY +
                                String.format(languageContext.get("commands.waifu.stats.credits"), waifuStats.getFinalValue()), false
                        ).setFooter(languageContext.get("commands.waifu.notice"), null);

                ctx.send(statsBuilder.build());
            }
        });

        waifu.addSubCommand("claim", new SubCommand() {
            @Override
            public String description() {
                return "Claim a waifu. You need to mention the person you want to claim. Usage: `~>waifu claim <@mention>`";
            }

            @Override
            protected void call(Context ctx, String content) {
                Player player = ctx.getPlayer();
                if(player.getData().isWaifuout()) {
                    ctx.sendLocalized("commands.waifu.optout.notice", EmoteReference.ERROR);
                    return;
                }

                if (ctx.getMentionedUsers().isEmpty()) {
                    ctx.sendLocalized("commands.waifu.claim.no_user", EmoteReference.ERROR);
                    return;
                }

                User toLookup = ctx.getMentionedUsers().get(0);

                if (toLookup.isBot()) {
                    ctx.sendLocalized("commands.waifu.bot", EmoteReference.ERROR);
                    return;
                }

                final Player claimerPlayer = ctx.getPlayer();
                final DBUser claimerUser = ctx.getDBUser();
                final UserData claimerUserData = claimerUser.getData();

                final Player claimedPlayer = ctx.getPlayer(toLookup);
                final DBUser claimedUser = ctx.getDBUser(toLookup);
                final UserData claimedUserData = claimedUser.getData();

                if(claimedPlayer.getData().isWaifuout()) {
                    ctx.sendLocalized("commands.waifu.optout.claim_notice", EmoteReference.ERROR);
                    return;
                }


                //Waifu object declaration.
                final Waifu waifuToClaim = calculateWaifuValue(toLookup);
                final long waifuFinalValue = waifuToClaim.getFinalValue();

                //Checks.
                if (toLookup.getIdLong() == ctx.getAuthor().getIdLong()) {
                    ctx.sendLocalized("commands.waifu.claim.yourself", EmoteReference.ERROR);
                    return;
                }

                if (claimerUser.getData().getWaifus().entrySet().stream().anyMatch((w) -> w.getKey().equals(toLookup.getId()))) {
                    ctx.sendLocalized("commands.waifu.claim.already_claimed", EmoteReference.ERROR);
                    return;
                }

                //If the to-be claimed has the claim key in their inventory, it cannot be claimed.
                if (claimedPlayer.getData().isClaimLocked()) {
                    ctx.sendLocalized("commands.waifu.claim.key_locked", EmoteReference.ERROR);
                    return;
                }

                if (claimerPlayer.isLocked()) {
                    ctx.sendLocalized("commands.waifu.claim.locked", EmoteReference.ERROR);
                    return;
                }

                //Deduct from balance and checks for money.
                if (!claimerPlayer.removeMoney(waifuFinalValue)) {
                    ctx.sendLocalized("commands.waifu.claim.not_enough_money", EmoteReference.ERROR, waifuFinalValue);
                    return;
                }

                if (claimerUserData.getWaifus().size() >= claimerUserData.getWaifuSlots()) {
                    ctx.sendLocalized("commands.waifu.claim.not_enough_slots",
                            EmoteReference.ERROR, claimerUserData.getWaifuSlots(), claimerUserData.getWaifus().size()
                    );

                    return;
                }

                if (waifuFinalValue > 1000000000) {
                    claimerPlayer.getData().addBadgeIfAbsent(Badge.GOLD_VALUE);
                }

                //Add waifu to claimer list.
                claimerUser.getData().getWaifus().put(toLookup.getId(), waifuFinalValue);
                claimedUserData.setTimesClaimed(claimedUserData.getTimesClaimed() + 1);

                //Add badges
                if (claimedUserData.getWaifus().containsKey(claimerPlayer.getId()) || claimerUserData.getWaifus().containsKey(claimedPlayer.getId())) {
                    claimerPlayer.getData().addBadgeIfAbsent(Badge.MUTUAL);
                    claimedPlayer.getData().addBadgeIfAbsent(Badge.MUTUAL);
                }

                claimerPlayer.getData().addBadgeIfAbsent(Badge.WAIFU_CLAIMER);
                claimedPlayer.getData().addBadgeIfAbsent(Badge.CLAIMED);

                //Massive saving operation owo.
                claimerPlayer.saveAsync();
                claimedPlayer.saveAsync();
                claimedUser.saveAsync();
                claimerUser.saveAsync();

                //Send confirmation message
                ctx.sendLocalized("commands.waifu.claim.success",
                        EmoteReference.CORRECT, toLookup.getName(), waifuFinalValue, claimerUserData.getWaifus().size()
                );
            }
        });

        waifu.addSubCommand("unclaim", new SubCommand() {
            @Override
            public String description() {
                return "Unclaims a waifu. You need to mention them, or you can also use their user id if they're not in any servers you share. Usage: `~>waifu unclaim <@mention>`";
            }

            @Override
            protected void call(Context ctx, String content) {
                Map<String, String> t = ctx.getOptionalArguments();
                content = Utils.replaceArguments(t, content, "unknown");
                boolean isId = content.matches("\\d{16,20}");
                Player player = ctx.getPlayer();
                if(player.getData().isWaifuout()) {
                    ctx.sendLocalized("commands.waifu.optout.notice", EmoteReference.ERROR);
                    return;
                }

                if (content.isEmpty() && !isId) {
                    ctx.sendLocalized("commands.waifu.unclaim.no_user", EmoteReference.ERROR);
                    return;
                }

                //We don't look this up if it's by-id.
                Member member = null;
                if (!isId) {
                    member = Utils.findMember(ctx.getEvent(), ctx.getMember(), content);
                    if (member == null)
                        return;
                }

                User toLookup = isId ? MantaroBot.getInstance().getShardManager().getUserById(content) : member.getUser();
                boolean isUnknown = isId && t.containsKey("unknown") && toLookup == null;
                if (toLookup == null && !isUnknown) {
                    ctx.sendLocalized("commands.waifu.unclaim.not_found", EmoteReference.ERROR);
                    return;
                }

                //It'll only be null if -unknown is passed with an unknown ID. This is unclaim, so this check is a bit irrelevant though.
                if (!isUnknown && toLookup.isBot()) {
                    ctx.sendLocalized("commands.waifu.bot", EmoteReference.ERROR);
                    return;
                }

                String userId = isUnknown ? content : toLookup.getId();
                String name = isUnknown ? "Unknown User" : toLookup.getName();
                final DBUser claimerUser = ctx.getDBUser();
                final UserData data = claimerUser.getData();

                Long value = data.getWaifus().get(userId);

                if (value == null) {
                    ctx.sendLocalized("commands.waifu.not_claimed", EmoteReference.ERROR);
                    return;
                }

                long valuePayment = (long) (value * 0.15);

                //Send confirmation message.
                ctx.sendLocalized("commands.waifu.unclaim.confirmation", EmoteReference.MEGA, name, valuePayment, EmoteReference.STOPWATCH);

                InteractiveOperations.create(ctx.getChannel(), ctx.getAuthor().getIdLong(), 60, (ie) -> {
                    if (!ie.getAuthor().getId().equals(ctx.getAuthor().getId())) {
                        return Operation.IGNORED;
                    }

                    //Replace prefix because people seem to think you have to add the prefix before saying yes.
                    String c = ie.getMessage().getContentRaw();
                    for (String s : ctx.getConfig().prefix) {
                        if (c.toLowerCase().startsWith(s)) {
                            c = c.substring(s.length());
                        }
                    }

                    String guildCustomPrefix = ctx.getDBGuild().getData().getGuildCustomPrefix();
                    if (guildCustomPrefix != null && !guildCustomPrefix.isEmpty() && c.toLowerCase().startsWith(guildCustomPrefix)) {
                        c = c.substring(guildCustomPrefix.length());
                    }
                    //End of prefix replacing.

                    if (c.equalsIgnoreCase("yes")) {
                        Player p = ctx.getPlayer();
                        final DBUser user = ctx.getDBUser();
                        final UserData userData = user.getData();

                        if (p.getMoney() < valuePayment) {
                            ctx.sendLocalized("commands.waifu.unclaim.not_enough_money", EmoteReference.ERROR);
                            return Operation.COMPLETED;
                        }

                        if (p.isLocked()) {
                            ctx.sendLocalized("commands.waifu.unclaim.player_locked", EmoteReference.ERROR);
                            return Operation.COMPLETED;
                        }

                        p.removeMoney(valuePayment);
                        userData.getWaifus().remove(userId);
                        user.save();
                        p.save();

                        ctx.sendLocalized("commands.waifu.unclaim.success", EmoteReference.CORRECT, name, valuePayment);
                        return Operation.COMPLETED;
                    } else if (c.equalsIgnoreCase("no")) {
                        ctx.sendLocalized("commands.waifu.unclaim.scrapped", EmoteReference.CORRECT);
                        return Operation.COMPLETED;
                    }

                    return Operation.IGNORED;
                });
            }
        });

        waifu.addSubCommand("buyslot", new SubCommand() {
            @Override
            public String description() {
                return "Buys a new waifu slot. Maximum slots are 20, costs get increasingly higher.";
            }

            @Override
            protected void call(Context ctx, String content) {
                int baseValue = 3000;

                DBUser user = ctx.getDBUser();
                Player player = ctx.getPlayer();
                final UserData userData = user.getData();

                if(player.getData().isWaifuout()) {
                    ctx.sendLocalized("commands.waifu.optout.notice", EmoteReference.ERROR);
                    return;
                }

                int currentSlots = userData.getWaifuSlots();
                int baseMultiplier = (currentSlots / 3) + 1;
                int finalValue = baseValue * baseMultiplier;

                if (player.isLocked()) {
                    ctx.sendLocalized("commands.waifu.buyslot.locked", EmoteReference.ERROR);
                    return;
                }

                if (player.getMoney() < finalValue) {
                    ctx.sendLocalized("commands.waifu.buyslot.not_enough_money", EmoteReference.ERROR, finalValue);
                    return;
                }

                if (userData.getWaifuSlots() >= 20) {
                    ctx.sendLocalized("commands.waifu.buyslot.too_many", EmoteReference.ERROR);
                    return;
                }

                player.removeMoney(finalValue);
                userData.setWaifuSlots(currentSlots + 1);
                user.save();
                player.save();

                ctx.sendLocalized("commands.waifu.buyslot.success",
                        EmoteReference.CORRECT, finalValue, userData.getWaifuSlots(), (userData.getWaifuSlots() - userData.getWaifus().size())
                );
            }
        });
    }
}
