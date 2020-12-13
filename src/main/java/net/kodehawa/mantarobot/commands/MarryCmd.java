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
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.commands.currency.item.ItemReference;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.base.ITreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Marriage;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.awt.Color;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Module
public class MarryCmd {
    private static final long housePrice = 5_000;
    private static final long carPrice = 1_000;

    private static final Pattern offsetRegex =
            Pattern.compile("(?:UTC|GMT)[+-][0-9]{1,2}(:[0-9]{1,2})?", Pattern.CASE_INSENSITIVE);

    @Subscribe
    public void marry(CommandRegistry cr) {
        ITreeCommand marryCommand = cr.register("marry", new TreeCommand(CommandCategory.CURRENCY) {
            @Override
            public Command defaultTrigger(Context ctx, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, I18nContext languageContext, String content) {
                        if (ctx.getMentionedUsers().isEmpty()) {
                            ctx.sendLocalized("commands.marry.no_mention", EmoteReference.ERROR);
                            return;
                        }

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

                        // We need to conduct a bunch of checks here.
                        // You CANNOT marry bots, yourself, people already married, or engage on another marriage if you're married.
                        // On the latter, the waifu system will be avaliable on release.
                        // Confirmation cannot happen if the rings are missing. Timeout for confirmation is at MOST 2 minutes.
                        // If the receipt has more than 5000 rings, remove rings from the person giving it and scrape them.

                        // Proposed to is a bot user, cannot marry bots, this is still not 2100.
                        if (proposedToUser.isBot()) {
                            ctx.sendLocalized("commands.marry.marry_bot_notice", EmoteReference.ERROR);
                            return;
                        }

                        // Already married to the same person you're proposing to.
                        if ((proposingMarriage != null && proposedToMarriage != null) &&
                                proposedToUserData.getMarriage().getId().equals(proposingMarriage.getId())) {
                            ctx.sendLocalized("commands.marry.already_married_receipt", EmoteReference.ERROR);
                            return;
                        }

                        // You're already married. Huh huh.
                        if (proposingMarriage != null) {
                            ctx.sendLocalized("commands.marry.already_married", EmoteReference.ERROR);
                            return;
                        }

                        // Receipt is married, cannot continue.
                        if (proposedToMarriage != null) {
                            ctx.sendLocalized("commands.marry.receipt_married", EmoteReference.ERROR);
                            return;
                        }

                        // Not enough rings to continue. Buy more rings w.
                        if (!proposingPlayerInventory.containsItem(ItemReference.RING) || proposingPlayerInventory.getAmount(ItemReference.RING) < 2) {
                            ctx.sendLocalized("commands.marry.no_ring", EmoteReference.ERROR);
                            return;
                        }

                        // Send confirmation message.
                        ctx.sendLocalized("commands.marry.confirmation", EmoteReference.MEGA,
                                proposedToUser.getName(), ctx.getAuthor().getName(), EmoteReference.STOPWATCH
                        );

                        InteractiveOperations.create(ctx.getChannel(), ctx.getAuthor().getIdLong(), 120, (ie) -> {
                            // Ignore all messages from anyone that isn't the user we already proposed to. Waiting for confirmation...
                            if (!ie.getAuthor().getId().equals(proposedToUser.getId()))
                                return Operation.IGNORED;

                            // Replace prefix because people seem to think you have to add the prefix before saying yes.
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
                            // End of prefix replacing.

                            // Lovely~ <3
                            if (message.equalsIgnoreCase("yes")) {
                                // Here we NEED to get the Player,
                                // User and Marriage objects once again
                                // to avoid race conditions or changes on those that might have happened on the 120 seconds that this lasted for.
                                // We need to check if the marriage is empty once again before continuing, also if we have enough rings!
                                // Else we end up with really annoying to debug bugs, lol.
                                Player proposingPlayer = ctx.getPlayer(proposingUser);
                                Player proposedToPlayer = ctx.getPlayer(proposedToUser);
                                DBUser proposingUserDB = ctx.getDBUser(proposingUser);
                                DBUser proposedToUserDB = ctx.getDBUser(proposedToUser);

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

                                //LAST inventory check and ring assignment is gonna happen using those.
                                final Inventory proposingPlayerFinalInventory = proposingPlayer.getInventory();
                                final Inventory proposedToPlayerInventory = proposedToPlayer.getInventory();

                                if (proposingPlayerFinalInventory.getAmount(ItemReference.RING) < 2) {
                                    ctx.sendLocalized("commands.marry.ring_check_fail", EmoteReference.ERROR);
                                    return Operation.COMPLETED;
                                }

                                //Remove the ring from the proposing player inventory.
                                proposingPlayerFinalInventory.process(new ItemStack(ItemReference.RING, -1));

                                //Silently scrape the rings if the receipt has more than 5000 rings.
                                if (proposedToPlayerInventory.getAmount(ItemReference.RING) < 5000) {
                                    proposedToPlayerInventory.process(new ItemStack(ItemReference.RING, 1));
                                }


                                final long marriageCreationMillis = Instant.now().toEpochMilli();
                                // Onto the UUID we need to encode userId + timestamp of
                                // the proposing player and the proposed to player after the acceptance is done.
                                String marriageId = new UUID(proposingUser.getIdLong(), proposedToUser.getIdLong()).toString();

                                // Make and save the new marriage object.
                                Marriage actualMarriage = Marriage.of(marriageId, proposingUser, proposedToUser);
                                actualMarriage.getData().setMarriageCreationMillis(marriageCreationMillis);
                                actualMarriage.save();

                                // Assign the marriage ID to the respective users and save it.
                                proposingUserDB.getData().setMarriageId(marriageId);
                                proposedToUserDB.getData().setMarriageId(marriageId);
                                proposingUserDB.save();
                                proposedToUserDB.save();

                                // Send marriage confirmation message.
                                ctx.sendLocalized("commands.marry.accepted",
                                        EmoteReference.POPPER, ie.getAuthor().getName(),
                                        ie.getAuthor().getDiscriminator(), proposingUser.getName(), proposingUser.getDiscriminator()
                                );

                                // Add the badge to the married couple.
                                proposingPlayer.getData().addBadgeIfAbsent(Badge.MARRIED);
                                proposedToPlayer.getData().addBadgeIfAbsent(Badge.MARRIED);

                                // Give a love letter both to the proposing player and the one who was proposed to.
                                if (proposingPlayerFinalInventory.getAmount(ItemReference.LOVE_LETTER) < 5000) {
                                    proposingPlayerFinalInventory.process(new ItemStack(ItemReference.LOVE_LETTER, 1));
                                }

                                if (proposedToPlayerInventory.getAmount(ItemReference.LOVE_LETTER) < 5000) {
                                    proposedToPlayerInventory.process(new ItemStack(ItemReference.LOVE_LETTER, 1));
                                }

                                // Badge assignment saving.
                                proposingPlayer.save();
                                proposedToPlayer.save();

                                return Operation.COMPLETED;
                            }

                            if (message.equalsIgnoreCase("no")) {
                                ctx.sendLocalized("commands.marry.denied", EmoteReference.CORRECT, proposingUser.getName());

                                // Well, we have a badge for this too. Consolation prize I guess.
                                final Player proposingPlayer = ctx.getPlayer(proposingUser);
                                proposingPlayer.getData().addBadgeIfAbsent(Badge.DENIED);
                                proposingPlayer.saveUpdating();
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
                        .addParameter("command",
                                "The subcommand you can use. Check the subcommands section for a list and usage of each.")
                        .build();
            }
        });

        marryCommand.addSubCommand("createletter", new SubCommand() {
            @Override
            public String description() {
                return "Create a love letter for your marriage. Usage: `~>marry createletter <content>`";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                final User author = ctx.getAuthor();

                Player player = ctx.getPlayer();
                Inventory playerInventory = player.getInventory();
                DBUser dbUser = ctx.getDBUser();

                //Without one love letter we cannot do much, ya know.
                if (playerInventory.containsItem(ItemReference.LOVE_LETTER)) {
                    final Marriage currentMarriage = dbUser.getData().getMarriage();

                    // Check if the user is married,
                    // is the proposed player, there's no love letter and
                    // that the love letter is less than 1500 characters long.
                    if (currentMarriage == null) {
                        ctx.sendLocalized("commands.marry.loveletter.no_marriage", EmoteReference.SAD);
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
                    final User marriedTo = ctx.retrieveUserById(currentMarriage.getOtherPlayer(author.getId()));
                    if (marriedTo == null) {
                        ctx.sendLocalized("commands.marry.loveletter.cannot_see", EmoteReference.ERROR);
                        return;
                    }

                    //Send a confirmation message.
                    String finalContent = Utils.DISCORD_INVITE.matcher(content).replaceAll("-invite link-");
                    finalContent = Utils.DISCORD_INVITE_2.matcher(finalContent).replaceAll("-invite link-");

                    ctx.sendStrippedLocalized("commands.marry.loveletter.confirmation",
                            EmoteReference.TALKING, marriedTo.getName(),
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

                            if (!inventoryFinal.containsItem(ItemReference.LOVE_LETTER)) {
                                ctx.sendLocalized("commands.marry.loveletter.no_letter", EmoteReference.SAD);
                                return Operation.COMPLETED;
                            }

                            //Remove the love letter from the inventory.
                            inventoryFinal.process(new ItemStack(ItemReference.LOVE_LETTER, -1));
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

        marryCommand.addSubCommand("house", new SubCommand() {
            @Override
            public String description() {
                return "Buys a house to live in. You need to buy a house in market first. Usage: `~>marry buyhouse <name>`";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var player = ctx.getPlayer();
                var playerInventory = player.getInventory();
                var dbUser = ctx.getDBUser();
                var marriage = dbUser.getData().getMarriage();

                if (marriage == null) {
                    ctx.sendLocalized("commands.marry.buyhouse.not_married", EmoteReference.ERROR);
                    return;
                }

                if (!playerInventory.containsItem(ItemReference.HOUSE)) {
                    ctx.sendLocalized("commands.marry.buyhouse.no_house", EmoteReference.ERROR);
                    return;
                }

                if (player.getCurrentMoney() < housePrice) {
                    ctx.sendLocalized("commands.marry.buyhouse.not_enough_money", EmoteReference.ERROR, housePrice);
                    return;
                }

                content = content.replace("\n", "").trim();
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.marry.buyhouse.no_name", EmoteReference.ERROR);
                    return;
                }

                if (content.length() > 150) {
                    ctx.sendLocalized("commands.pet.buy.too_long", EmoteReference.ERROR);
                    return;
                }

                var finalContent = content;
                ctx.sendLocalized("commands.marry.buyhouse.confirm", EmoteReference.WARNING, housePrice, content);
                InteractiveOperations.create(ctx.getChannel(), ctx.getAuthor().getIdLong(), 30, (e) -> {
                    if (!e.getAuthor().equals(ctx.getAuthor()))
                        return Operation.IGNORED;

                    if (e.getMessage().getContentRaw().equalsIgnoreCase("yes")) {
                        var playerConfirmed = ctx.getPlayer();
                        var playerInventoryConfirmed = playerConfirmed.getInventory();
                        var dbUserConfirmed = ctx.getDBUser();
                        var marriageConfirmed = dbUserConfirmed.getData().getMarriage();

                        // People like to mess around lol.
                        if (!playerInventoryConfirmed.containsItem(ItemReference.HOUSE)) {
                            ctx.sendLocalized("commands.marry.buyhouse.no_house");
                            return Operation.COMPLETED;
                        }

                        if (playerConfirmed.getCurrentMoney() < housePrice) {
                            ctx.sendLocalized("commands.marry.buyhouse.not_enough_money");
                            return Operation.COMPLETED;
                        }

                        playerInventoryConfirmed.process(new ItemStack(ItemReference.HOUSE, -1));
                        playerConfirmed.removeMoney(housePrice);

                        playerConfirmed.save();

                        marriageConfirmed.getData().setHasHouse(true);
                        marriageConfirmed.getData().setHouseName(finalContent);
                        marriageConfirmed.save();

                        ctx.sendLocalized("commands.marry.buyhouse.success", EmoteReference.POPPER, housePrice, finalContent);
                        return Operation.COMPLETED;
                    }

                    if (e.getMessage().getContentRaw().equalsIgnoreCase("no")) {
                        ctx.sendLocalized("commands.marry.buyhouse.cancel_success", EmoteReference.CORRECT);
                        return Operation.COMPLETED;
                    }

                    return Operation.IGNORED;
                });
            }
        }).createSubCommandAlias("house", "buyhouse");

        marryCommand.addSubCommand("car", new SubCommand() {
            @Override
            public String description() {
                return "Buys a car to travel in. You need to buy a ~~cat~~ car in market first. Usage: `~>marry buycar <name>`";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var player = ctx.getPlayer();
                var playerInventory = player.getInventory();
                var dbUser = ctx.getDBUser();
                var marriage = dbUser.getData().getMarriage();

                if (marriage == null) {
                    ctx.sendLocalized("commands.marry.general.not_married", EmoteReference.ERROR);
                    return;
                }

                if (!playerInventory.containsItem(ItemReference.CAR)) {
                    ctx.sendLocalized("commands.marry.buycar.no_car", EmoteReference.ERROR);
                    return;
                }

                if (player.getCurrentMoney() < carPrice) {
                    ctx.sendLocalized("commands.marry.buycar.not_enough_money", EmoteReference.ERROR, carPrice);
                    return;
                }

                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.marry.buycar.no_name", EmoteReference.ERROR);
                    return;
                }

                content = content.replace("\n", "").trim();
                if (content.length() > 150) {
                    ctx.sendLocalized("commands.pet.buy.too_long", EmoteReference.ERROR);
                    return;
                }

                var finalContent = content;
                ctx.sendLocalized("commands.marry.buycar.confirm", EmoteReference.WARNING, carPrice, content);
                InteractiveOperations.create(ctx.getChannel(), ctx.getAuthor().getIdLong(), 30, (e) -> {
                    if (!e.getAuthor().equals(ctx.getAuthor()))
                        return Operation.IGNORED;

                    if (e.getMessage().getContentRaw().equalsIgnoreCase("yes")) {
                        var playerConfirmed = ctx.getPlayer();
                        var playerInventoryConfirmed = playerConfirmed.getInventory();
                        var dbUserConfirmed = ctx.getDBUser();
                        var marriageConfirmed = dbUserConfirmed.getData().getMarriage();

                        // People like to mess around lol.
                        if (!playerInventoryConfirmed.containsItem(ItemReference.CAR)) {
                            ctx.sendLocalized("commands.marry.buycar.no_car");
                            return Operation.COMPLETED;
                        }

                        if (playerConfirmed.getCurrentMoney() < carPrice) {
                            ctx.sendLocalized("commands.marry.buycar.not_enough_money");
                            return Operation.COMPLETED;
                        }

                        playerInventoryConfirmed.process(new ItemStack(ItemReference.CAR, -1));
                        playerConfirmed.removeMoney(carPrice);
                        playerConfirmed.save();

                        marriageConfirmed.getData().setHasCar(true);
                        marriageConfirmed.getData().setCarName(finalContent);
                        marriageConfirmed.save();

                        ctx.sendLocalized("commands.marry.buycar.success", EmoteReference.POPPER, carPrice, finalContent);
                        return Operation.COMPLETED;
                    }

                    if (e.getMessage().getContentRaw().equalsIgnoreCase("no")) {
                        ctx.sendLocalized("commands.marry.buycar.cancel_success", EmoteReference.CORRECT);
                        return Operation.COMPLETED;
                    }

                    return Operation.IGNORED;
                });
            }
        }).createSubCommandAlias("car", "buycar");

        IncreasingRateLimiter tzRatelimit = new IncreasingRateLimiter.Builder()
                .limit(1)
                .spamTolerance(2)
                .cooldown(2, TimeUnit.DAYS)
                .maxCooldown(2, TimeUnit.DAYS)
                .randomIncrement(false)
                .premiumAware(false)
                .pool(MantaroData.getDefaultJedisPool())
                .prefix("marriage-tz")
                .build();

        marryCommand.addSubCommand("timezone", new SubCommand() {
            @Override
            public String description() {
                return "Sets the timezone for your marriage. Useful for pet sleep times.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var dbUser = ctx.getDBUser();
                var marriage = dbUser.getData().getMarriage();

                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.marry.timezone.no_content", EmoteReference.ERROR);
                    return;
                }

                if (marriage == null) {
                    ctx.sendLocalized("commands.marry.general.not_married", EmoteReference.ERROR);
                    return;
                }

                String timezone = content;
                if (offsetRegex.matcher(timezone).matches()) // Avoid replacing valid zone IDs / uppercasing them.
                    timezone = content.toUpperCase().replace("UTC", "GMT");

                if (!Utils.isValidTimeZone(timezone)) {
                    ctx.sendLocalized("commands.marry.timezone.invalid", EmoteReference.ERROR);
                    return;
                }

                if (!RatelimitUtils.ratelimit(tzRatelimit, ctx)) {
                    return;
                }

                marriage.getData().setTimezone(timezone);
                marriage.save();
                dbUser.save();
                ctx.sendLocalized("commands.marry.timezone.success", EmoteReference.CORRECT, timezone);
            }
        });

        marryCommand.addSubCommand("status", new SubCommand() {
            @Override
            public String description() {
                return "Check your marriage status.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                final var author = ctx.getAuthor();
                final var dbUser = ctx.getDBUser();
                final var dbUserData = dbUser.getData();
                final var currentMarriage = dbUserData.getMarriage();
                //What status would we have without marriage? Well, we can be unmarried omegalul.
                if (currentMarriage == null) {
                    ctx.sendLocalized("commands.marry.status.no_marriage", EmoteReference.SAD);
                    return;
                }

                final var data = currentMarriage.getData();

                //Can we find the user this is married to?
                final var marriedTo = ctx.retrieveUserById(currentMarriage.getOtherPlayer(author.getId()));
                if (marriedTo == null) {
                    ctx.sendLocalized("commands.marry.loveletter.cannot_see", EmoteReference.ERROR);
                    return;
                }

                //Get the current love letter.
                var loveLetter = data.getLoveLetter();
                if (loveLetter == null || loveLetter.isEmpty()) {
                    loveLetter = "None.";
                }

                final var marriedDBUser = ctx.getDBUser(marriedTo);
                final var dateFormat = Utils.formatDate(data.getMarriageCreationMillis(), dbUserData.getLang());
                final var eitherHasWaifus = !(dbUserData.getWaifus().isEmpty() && marriedDBUser.getData().getWaifus().isEmpty());
                final var marriedToName = dbUserData.isPrivateTag() ? marriedTo.getName() : marriedTo.getAsTag();
                final var authorName = dbUserData.isPrivateTag() ? author.getName() : author.getAsTag();

                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setThumbnail(author.getEffectiveAvatarUrl())
                        .setAuthor(languageContext.get("commands.marry.status.header"), null, author.getEffectiveAvatarUrl())
                        .setColor(ctx.getMember().getColor() == null ? Color.PINK : ctx.getMember().getColor())
                        .setDescription(languageContext.get("commands.marry.status.description_format").formatted(
                                EmoteReference.HEART, authorName, marriedToName)
                        ).addField(languageContext.get("commands.marry.status.date"), dateFormat, false)
                        .addField(languageContext.get("commands.marry.status.love_letter"), loveLetter, false)
                        .addField(languageContext.get("commands.marry.status.waifus"), String.valueOf(eitherHasWaifus), false)
                        .setFooter("Marriage ID: " + currentMarriage.getId(), author.getEffectiveAvatarUrl());

                if (data.hasHouse()) {
                    var houseName = data.getHouseName().replace("\n", "").trim();
                    embedBuilder.addField(languageContext.get("commands.marry.status.house"), houseName, true);
                }

                if (data.hasCar()) {
                    var carName = data.getCarName().replace("\n", "").trim();
                    embedBuilder.addField(languageContext.get("commands.marry.status.car"), carName, true);
                }

                if (data.getPet() != null) {
                    var pet = data.getPet();
                    var petType = data.getPet().getType();

                    embedBuilder.addField(languageContext.get("commands.marry.status.pet"),
                            pet.getName() + " (" + petType.getEmoji() + petType.getName() + ")", false
                    );
                }

                ctx.send(embedBuilder.build());
            }
        });

        cr.registerAlias("marry", "marriage");
    }

    @Subscribe
    public void divorce(CommandRegistry cr) {
        cr.register("divorce", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            protected void call(Context ctx, String cn, String[] args) {
                //We, indeed, have no marriage here.
                if (ctx.getDBUser().getData().getMarriage() == null) {
                    ctx.sendLocalized("commands.divorce.not_married", EmoteReference.ERROR);
                    return;
                }

                ctx.sendLocalized("commands.divorce.confirm", EmoteReference.WARNING);
                InteractiveOperations.create(ctx.getChannel(), ctx.getAuthor().getIdLong(), 45, interactiveEvent -> {
                    if (!interactiveEvent.getAuthor().getId().equals(ctx.getAuthor().getId())) {
                        return Operation.IGNORED;
                    }

                    String content = interactiveEvent.getMessage().getContentRaw();

                    if (content.equalsIgnoreCase("yes")) {
                        final var divorceeDBUser = ctx.getDBUser();
                        final var marriage = divorceeDBUser.getData().getMarriage();

                        final var marriageData = marriage.getData();

                        //We do have a marriage, get rid of it.
                        final var marriedWithDBUser = ctx.getDBUser(marriage.getOtherPlayer(ctx.getAuthor().getId()));
                        final var marriedWithPlayer = ctx.getPlayer(marriedWithDBUser.getId());
                        final var divorceePlayer = ctx.getPlayer();

                        //Save the user of the person they were married with.
                        marriedWithDBUser.getData().setMarriageId(null);
                        marriedWithDBUser.save();

                        //Save the user of themselves.
                        divorceeDBUser.getData().setMarriageId(null);
                        divorceeDBUser.save();

                        //Add the heart broken badge to the user who divorced.
                        divorceePlayer.getData().addBadgeIfAbsent(Badge.HEART_BROKEN);

                        //Add the heart broken badge to the user got dumped.
                        marriedWithPlayer.getData().addBadgeIfAbsent(Badge.HEART_BROKEN);

                        var moneySplit = 0L;

                        if (marriageData.hasHouse()) {
                            moneySplit += housePrice;
                        }

                        if (marriageData.hasCar()) {
                            moneySplit += carPrice;
                        }

                        if (marriageData.getPet() != null) {
                            moneySplit += marriageData.getPet().getType().getCost() * 0.9;
                        }

                        //Scrape this marriage.
                        marriage.delete();

                        // Split the money between the two people.
                        var portion = moneySplit / 2;
                        divorceePlayer.addMoney(portion);
                        marriedWithPlayer.addMoney(portion);

                        divorceePlayer.save();
                        marriedWithPlayer.save();

                        var extra = "";
                        if (portion > 1) {
                            extra = ctx.getLanguageContext().get("commands.divorce.split").formatted(portion);
                        }

                        ctx.sendLocalized("commands.divorce.success", EmoteReference.CORRECT, extra);
                        return Operation.COMPLETED;
                    } else if (content.equalsIgnoreCase("no")) {
                        ctx.sendLocalized("commands.divorce.cancelled", EmoteReference.CORRECT);
                        return Operation.COMPLETED;
                    }

                    return Operation.IGNORED;
                });
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Basically divorces you from whoever you were married to.")
                        .build();
            }
        });
    }
}
