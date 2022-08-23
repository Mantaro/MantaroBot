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
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.kodehawa.mantarobot.commands.currency.item.ItemReference;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.*;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.listeners.operations.ButtonOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.Marriage;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Module
public class MarryCmd {
    private static final long housePrice = 5_000;
    private static final long carPrice = 1_000;
    private static final IncreasingRateLimiter marryRatelimiter = new IncreasingRateLimiter.Builder()
            .limit(1)
            .cooldown(10, TimeUnit.MINUTES)
            .maxCooldown(40, TimeUnit.MINUTES)
            .randomIncrement(false)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("marry")
            .build();

    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(Marry.class);
        cr.registerSlash(Divorce.class);
    }

    @Description("The hub for marriage related commands.")
    @Category(CommandCategory.CURRENCY)
    public static class Marry extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {}

        @Name("user")
        @Defer
        @Description("Marries another user.")
        @Options({@Options.Option(type = OptionType.USER, name = "user", description = "The user to marry.", required = true)})
        @Help(
                description = "Marry another user.",
                usage = "/marry user user:<user>",
                parameters = {
                        @Help.Parameter(name = "user", description = "The user to marry.")
                }
        )
        public static class MarryUser extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var proposingUser = ctx.getAuthor();
                var proposedToUser = ctx.getOptionAsUser("user");
                if (proposedToUser == null) {
                    ctx.reply("general.slash_member_lookup_failure", EmoteReference.ERROR);
                    return;
                }

                // This is just for checking purposes, so we don't need the DBUser itself.
                var proposingUserData = ctx.getDBUser(proposingUser).getData();
                var proposedToUserData = ctx.getDBUser(proposedToUser).getData();

                // Again just for checking, and no need to change.
                final var proposingPlayerInventory = ctx.getPlayer(proposingUser).getInventory();

                // Why would you do this...
                if (proposedToUser.getId().equals(ctx.getAuthor().getId())) {
                    ctx.reply("commands.marry.marry_yourself_notice", EmoteReference.ERROR);
                    return;
                }

                final var proposingMarriage = proposingUserData.getMarriage();
                final var proposedToMarriage = proposedToUserData.getMarriage();

                // We need to conduct a bunch of checks here.
                // You CANNOT marry bots, yourself, people already married, or engage on another marriage if you're married.
                // On the latter, the waifu system will be avaliable on release.
                // Confirmation cannot happen if the rings are missing. Timeout for confirmation is at MOST 2 minutes.
                // If the receipt has more than 5000 rings, remove rings from the person giving it and scrape them.

                // Proposed to is a bot user, cannot marry bots, this is still not 2100.
                if (proposedToUser.isBot()) {
                    ctx.reply("commands.marry.marry_bot_notice", EmoteReference.ERROR);
                    return;
                }

                // Already married to the same person you're proposing to.
                if ((proposingMarriage != null && proposedToMarriage != null) &&
                        proposedToUserData.getMarriage().getId().equals(proposingMarriage.getId())) {
                    ctx.reply("commands.marry.already_married_receipt", EmoteReference.ERROR);
                    return;
                }

                // You're already married. Huh huh.
                if (proposingMarriage != null) {
                    ctx.reply("commands.marry.already_married", EmoteReference.ERROR);
                    return;
                }

                // Receipt is married, cannot continue.
                if (proposedToMarriage != null) {
                    ctx.reply("commands.marry.receipt_married", EmoteReference.ERROR);
                    return;
                }

                // Not enough rings to continue. Buy more rings w.
                if (!proposingPlayerInventory.containsItem(ItemReference.RING) || proposingPlayerInventory.getAmount(ItemReference.RING) < 2) {
                    ctx.reply("commands.marry.no_ring", EmoteReference.ERROR);
                    return;
                }

                // Check for rate limit
                var languageContext = ctx.getLanguageContext();
                if (!RatelimitUtils.ratelimit(marryRatelimiter, ctx, languageContext.get("commands.marry.ratelimit_message"), false))
                    return;

                // Send confirmation message.
                var message = ctx.sendResult(String.format(
                        languageContext.get("commands.marry.confirmation")
                                .formatted(EmoteReference.MEGA, proposedToUser.getName(), ctx.getAuthor().getName(), EmoteReference.STOPWATCH)
                ));

                ButtonOperations.create(message, 120, (e) -> {
                    // Ignore all messages from anyone that isn't the user we already proposed to. Waiting for confirmation...
                    if (!e.getUser().getId().equals(proposedToUser.getId())) {
                        return Operation.IGNORED;
                    }

                    String buttonId = e.getButton().getId();
                    var hook = e.getHook();
                    if (buttonId == null) {
                        return Operation.IGNORED;
                    }

                    // Lovely~ <3
                    if (buttonId.equals("yes")) {
                        // Here we NEED to get the Player,
                        // User and Marriage objects once again
                        // to avoid race conditions or changes on those that might have happened on the 120 seconds that this lasted for.
                        // We need to check if the marriage is empty once again before continuing, also if we have enough rings!
                        // Else we end up with really annoying to debug bugs, lol.
                        var proposingPlayer = ctx.getPlayer(proposingUser);
                        var proposedToPlayer = ctx.getPlayer(proposedToUser);
                        var proposingUserDB = ctx.getDBUser(proposingUser);
                        var proposedToUserDB = ctx.getDBUser(proposedToUser);

                        final var proposingMarriageFinal = proposingUserDB.getData().getMarriage();
                        final var proposedToMarriageFinal = proposedToUserDB.getData().getMarriage();

                        if (proposingMarriageFinal != null) {
                            hook.editOriginal(languageContext.get("commands.marry.already_married").formatted(EmoteReference.ERROR))
                                    .setComponents().queue();
                            return Operation.COMPLETED;
                        }

                        if (proposedToMarriageFinal != null) {
                            hook.editOriginal(languageContext.get("commands.marry.receipt_married").formatted(EmoteReference.ERROR))
                                    .setComponents().queue();
                            return Operation.COMPLETED;
                        }

                        // LAST inventory check and ring assignment is gonna happen using those.
                        final var proposingPlayerFinalInventory = proposingPlayer.getInventory();
                        final var proposedToPlayerInventory = proposedToPlayer.getInventory();

                        if (proposingPlayerFinalInventory.getAmount(ItemReference.RING) < 2) {
                            hook.editOriginal(languageContext.get("commands.marry.ring_check_fail").formatted(EmoteReference.ERROR))
                                    .setComponents().queue();
                            return Operation.COMPLETED;
                        }

                        // Remove the ring from the proposing player inventory.
                        proposingPlayerFinalInventory.process(new ItemStack(ItemReference.RING, -1));

                        // Silently scrape the ring if the receipt has more than 5000 rings.
                        if (proposedToPlayerInventory.getAmount(ItemReference.RING) < 5000) {
                            proposedToPlayerInventory.process(new ItemStack(ItemReference.RING, 1));
                        }

                        final long marriageCreationMillis = Instant.now().toEpochMilli();
                        // Onto the UUID we need to encode userId + timestamp of
                        // the proposing player and the proposed to player after the acceptance is done.
                        var marriageId = new UUID(proposingUser.getIdLong(), proposedToUser.getIdLong()).toString();

                        // Make and save the new marriage object.
                        var actualMarriage = Marriage.of(marriageId, proposingUser, proposedToUser);
                        actualMarriage.getData().setMarriageCreationMillis(marriageCreationMillis);
                        actualMarriage.save();

                        // Assign the marriage ID to the respective users and save it.
                        proposingUserDB.getData().setMarriageId(marriageId);
                        proposedToUserDB.getData().setMarriageId(marriageId);
                        proposingUserDB.save();
                        proposedToUserDB.save();

                        // Send marriage confirmation message.
                        hook.editOriginal(languageContext.get("commands.marry.accepted").formatted(
                                EmoteReference.POPPER, e.getUser().getName(), e.getUser().getDiscriminator(),
                                proposingUser.getName(), proposingUser.getDiscriminator()
                        )).setComponents().queue();

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

                    if (buttonId.equals("no")) {
                        hook.editOriginal(languageContext.get("commands.marry.denied").formatted(EmoteReference.CORRECT, proposingUser.getName()))
                                .setComponents().queue();

                        // Well, we have a badge for this too. Consolation prize I guess.
                        final var proposingPlayer = ctx.getPlayer(proposingUser);
                        if (proposingPlayer.getData().addBadgeIfAbsent(Badge.DENIED)) {
                            proposingPlayer.saveUpdating();
                        }
                        return Operation.COMPLETED;
                    }

                    return Operation.IGNORED;
                }, Button.primary("yes", languageContext.get("buttons.yes")), Button.primary("no", languageContext.get("buttons.no")));
            }
        }

        @Description("Shows the status of your marriage.")
        public static class Status extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                final var author = ctx.getAuthor();
                final var dbUser = ctx.getDBUser();
                final var dbUserData = dbUser.getData();
                final var currentMarriage = dbUserData.getMarriage();
                final var languageContext = ctx.getLanguageContext();
                //What status would we have without marriage? Well, we can be unmarried omegalul.
                if (currentMarriage == null) {
                    ctx.reply("commands.marry.status.no_marriage", EmoteReference.SAD);
                    return;
                }

                final var data = currentMarriage.getData();

                //Can we find the user this is married to?
                final var marriedTo = ctx.retrieveUserById(currentMarriage.getOtherPlayer(author.getId()));
                if (marriedTo == null) {
                    ctx.reply("commands.marry.loveletter.cannot_see", EmoteReference.ERROR);
                    return;
                }

                //Get the current love letter.
                var loveLetter = data.getLoveLetter();
                if (loveLetter == null || loveLetter.isEmpty()) {
                    loveLetter = languageContext.get("general.none");
                }

                final var marriedDBUser = ctx.getDBUser(marriedTo);
                final var dateFormat = Utils.formatDate(data.getMarriageCreationMillis(), dbUserData.getLang());
                final var eitherHasWaifus = !(dbUserData.getWaifus().isEmpty() && marriedDBUser.getData().getWaifus().isEmpty());
                final var marriedToName = dbUserData.isPrivateTag() ? marriedTo.getName() : marriedTo.getAsTag();
                final var authorName = dbUserData.isPrivateTag() ? author.getName() : author.getAsTag();
                final var daysMarried = TimeUnit.of(ChronoUnit.MILLIS).toDays(System.currentTimeMillis() - data.getMarriageCreationMillis());

                var embedBuilder = new EmbedBuilder()
                        .setThumbnail(author.getEffectiveAvatarUrl())
                        .setAuthor(languageContext.get("commands.marry.status.header"), null, author.getEffectiveAvatarUrl())
                        .setColor(ctx.getMemberColor())
                        .setDescription(languageContext.get("commands.marry.status.description_format").formatted(
                                EmoteReference.HEART, authorName, marriedToName)
                        )
                        .addField(EmoteReference.CALENDAR2.toHeaderString() + languageContext.get("commands.marry.status.date"),
                                dateFormat, false)
                        .addField(EmoteReference.CLOCK.toHeaderString() + languageContext.get("commands.marry.status.age"),
                                daysMarried + " " + languageContext.get("general.days"), false
                        )
                        .addField(EmoteReference.LOVE_LETTER.toHeaderString() + languageContext.get("commands.marry.status.love_letter"),
                                loveLetter, false
                        )
                        .addField(EmoteReference.ZAP.toHeaderString() + languageContext.get("commands.marry.status.waifus"),
                                String.valueOf(eitherHasWaifus), false
                        )
                        .setFooter("Marriage ID: " + currentMarriage.getId(), author.getEffectiveAvatarUrl());

                if (data.hasHouse()) {
                    var houseName = data.getHouseName().replace("\n", "").trim();
                    embedBuilder.addField(EmoteReference.HOUSE.toHeaderString() + languageContext.get("commands.marry.status.house"),
                            houseName, true
                    );
                }

                if (data.hasCar()) {
                    var carName = data.getCarName().replace("\n", "").trim();
                    embedBuilder.addField(EmoteReference.CAR.toHeaderString() + languageContext.get("commands.marry.status.car"),
                            carName, true
                    );
                }

                if (data.getPet() != null) {
                    var pet = data.getPet();
                    var petType = data.getPet().getType();

                    embedBuilder.addField(EmoteReference.PET_HOUSE.toHeaderString() + languageContext.get("commands.marry.status.pet"),
                            pet.getName() + " (" + petType.getName() + ")", false
                    );
                }

                ctx.reply(embedBuilder.build());
            }
        }

        @Description("Creates a marriage letter.")
        @Defer
        @Options({@Options.Option(type = OptionType.STRING, name = "content", description = "The content of the letter.", required = true)})
        @Help(
                description = "Creates a marriage letter.",
                usage = "/marry crateletter content:<letter content>",
                parameters = {
                        @Help.Parameter(name = "content", description = "The content of the letter.")
                }
        )
        public static class CreateLetter extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                final var author = ctx.getAuthor();
                final var player = ctx.getPlayer();
                final var playerInventory = player.getInventory();
                final var dbUser = ctx.getDBUser();
                final var content = ctx.getOptionAsString("content");
                final var languageContext = ctx.getLanguageContext();

                //Without one love letter we cannot do much, ya know.
                if (!playerInventory.containsItem(ItemReference.LOVE_LETTER)) {
                    ctx.reply("commands.marry.loveletter.no_letter", EmoteReference.SAD);
                    return;
                }
                final var currentMarriage = dbUser.getData().getMarriage();

                // Check if the user is married,
                // is the proposed player, there's no love letter and
                // that the love letter is less than 1500 characters long.
                if (currentMarriage == null) {
                    ctx.reply("commands.marry.loveletter.no_marriage", EmoteReference.SAD);
                    return;
                }

                if (currentMarriage.getData().getLoveLetter() != null) {
                    ctx.reply("commands.marry.loveletter.already_done", EmoteReference.ERROR);
                    return;
                }

                if (content.isEmpty()) {
                    ctx.reply("commands.marry.loveletter.empty", EmoteReference.ERROR);
                    return;
                }

                if (content.length() > 500) {
                    ctx.reply("commands.marry.loveletter.too_long", EmoteReference.ERROR);
                    return;
                }

                //Can we find the user this is married to?
                final var marriedTo = ctx.retrieveUserById(currentMarriage.getOtherPlayer(author.getId()));
                if (marriedTo == null) {
                    ctx.reply("commands.marry.loveletter.cannot_see", EmoteReference.ERROR);
                    return;
                }

                //Send a confirmation message.
                var finalContent = Utils.DISCORD_INVITE.matcher(content).replaceAll("-invite link-");
                finalContent = Utils.DISCORD_INVITE_2.matcher(finalContent).replaceAll("-invite link-");

                var message = ctx.sendResult(String.format(
                        languageContext.get("commands.marry.loveletter.confirmation"), EmoteReference.TALKING, marriedTo.getName(), marriedTo.getDiscriminator(), finalContent)
                );

                //Start the operation.
                ButtonOperations.create(message, 60, e -> {
                    if (!e.getUser().getId().equals(author.getId())) {
                        return Operation.IGNORED;
                    }

                    var button = e.getButton().getId();
                    var hook = e.getHook();
                    if (button == null) {
                        return Operation.IGNORED;
                    }

                    //Confirmed they want to save this as the permanent love letter.
                    if (button.equals("yes")) {
                        final var playerFinal = ctx.getPlayer();
                        final var inventoryFinal = playerFinal.getInventory();
                        final var currentMarriageFinal = dbUser.getData().getMarriage();

                        //We need to do most of the checks all over again just to make sure nothing important slipped through.
                        if (currentMarriageFinal == null) {
                            hook.editOriginal(languageContext.get("commands.marry.loveletter.no_marriage")
                                    .formatted(EmoteReference.SAD)).setComponents().queue();
                            return Operation.COMPLETED;
                        }

                        if (!inventoryFinal.containsItem(ItemReference.LOVE_LETTER)) {
                            hook.editOriginal(languageContext.get("commands.marry.loveletter.no_letter")
                                    .formatted(EmoteReference.SAD)).setComponents().queue();
                            return Operation.COMPLETED;
                        }

                        //Remove the love letter from the inventory.
                        inventoryFinal.process(new ItemStack(ItemReference.LOVE_LETTER, -1));
                        playerFinal.save();

                        //Save the love letter.
                        currentMarriageFinal.getData().setLoveLetter(content);
                        currentMarriageFinal.save();

                        hook.editOriginal(languageContext.get("commands.marry.loveletter.confirmed")
                                .formatted(EmoteReference.CORRECT))
                                .setComponents().queue();
                        return Operation.COMPLETED;
                    } else if (button.equals("no")) {
                        hook.editOriginal(languageContext.get("commands.marry.loveletter.scrapped")
                                .formatted(EmoteReference.CORRECT))
                                .setComponents().queue();
                        return Operation.COMPLETED;
                    }

                    return Operation.IGNORED;
                }, Button.primary("yes", languageContext.get("buttons.yes")), Button.primary("no", languageContext.get("buttons.no")));
            }
        }

        @Description("Buys a house for the marriage. You need to buy a house in market first.")
        @Defer
        @Options({@Options.Option(type = OptionType.STRING, name = "name", description = "The name of the house.", required = true)})
        @Help(
                description = "Buys a house for the marriage. You need to buy a house in market first.",
                usage = "/marry house name:<name>",
                parameters = {
                        @Help.Parameter(name = "name", description = "Name for the new house.")
                }
        )
        public static class House extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var player = ctx.getPlayer();
                var playerInventory = player.getInventory();
                var dbUser = ctx.getDBUser();
                var marriage = dbUser.getData().getMarriage();
                var name = ctx.getOptionAsString("name");
                var languageContext = ctx.getLanguageContext();

                if (marriage == null) {
                    ctx.reply("commands.marry.buyhouse.not_married", EmoteReference.ERROR);
                    return;
                }

                if (!playerInventory.containsItem(ItemReference.HOUSE)) {
                    ctx.reply("commands.marry.buyhouse.no_house", EmoteReference.ERROR);
                    return;
                }

                if (player.getCurrentMoney() < housePrice) {
                    ctx.reply("commands.marry.buyhouse.not_enough_money", EmoteReference.ERROR, housePrice);
                    return;
                }

                if (name.length() > 150) {
                    ctx.reply("commands.pet.buy.too_long", EmoteReference.ERROR);
                    return;
                }

                var finalContent = Utils.HTTP_URL.matcher(name).replaceAll("-url-");
                var message = ctx.sendResult(String.format(languageContext.get("commands.marry.buyhouse.confirm"), EmoteReference.WARNING, housePrice, finalContent));
                ButtonOperations.create(message, 30, (e) -> {
                    if (!e.getUser().equals(ctx.getAuthor()))
                        return Operation.IGNORED;

                    var button = e.getButton().getId();
                    var hook = e.getHook();
                    if (button == null) {
                        return Operation.IGNORED;
                    }

                    if (button.equals("yes")) {
                        var playerConfirmed = ctx.getPlayer();
                        var playerInventoryConfirmed = playerConfirmed.getInventory();
                        var dbUserConfirmed = ctx.getDBUser();
                        var marriageConfirmed = dbUserConfirmed.getData().getMarriage();

                        // People like to mess around lol.
                        if (!playerInventoryConfirmed.containsItem(ItemReference.HOUSE)) {
                            hook.editOriginal(languageContext.get("commands.marry.buyhouse.no_house")).setComponents().queue();
                            return Operation.COMPLETED;
                        }

                        if (playerConfirmed.getCurrentMoney() < housePrice) {
                            hook.editOriginal(languageContext.get("commands.marry.buyhouse.not_enough_money")).setComponents().queue();
                            return Operation.COMPLETED;
                        }

                        playerInventoryConfirmed.process(new ItemStack(ItemReference.HOUSE, -1));
                        playerConfirmed.removeMoney(housePrice);

                        playerConfirmed.save();

                        marriageConfirmed.getData().setHasHouse(true);
                        marriageConfirmed.getData().setHouseName(finalContent);
                        marriageConfirmed.save();

                        hook.editOriginal(languageContext.get("commands.marry.buyhouse.success").formatted(EmoteReference.POPPER, housePrice, finalContent))
                                .setComponents().queue();
                        return Operation.COMPLETED;
                    }

                    if (button.equals("no")) {
                        hook.editOriginal(languageContext.get("commands.marry.buyhouse.cancel_success").formatted(EmoteReference.CORRECT))
                                .setComponents().queue();
                        return Operation.COMPLETED;
                    }

                    return Operation.IGNORED;
                }, Button.primary("yes", languageContext.get("buttons.yes")), Button.primary("no", languageContext.get("buttons.no")));
            }
        }

        @Description("Buys a car for the marriage. You need to buy a car in market first.")
        @Defer
        @Options({@Options.Option(type = OptionType.STRING, name = "name", description = "The name of the car.", required = true)})
        @Help(
                description = "Buys a car for the marriage. You need to buy a car in market first.",
                usage = "/marry car name:<name>",
                parameters = {
                        @Help.Parameter(name = "name", description = "Name for the new car.")
                }
        )
        public static class Car extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var player = ctx.getPlayer();
                var playerInventory = player.getInventory();
                var dbUser = ctx.getDBUser();
                var marriage = dbUser.getData().getMarriage();
                var name = ctx.getOptionAsString("name");
                var languageContext = ctx.getLanguageContext();

                if (marriage == null) {
                    ctx.reply("commands.marry.general.not_married", EmoteReference.ERROR);
                    return;
                }

                if (!playerInventory.containsItem(ItemReference.CAR)) {
                    ctx.reply("commands.marry.buycar.no_car", EmoteReference.ERROR);
                    return;
                }

                if (player.getCurrentMoney() < carPrice) {
                    ctx.reply("commands.marry.buycar.not_enough_money", EmoteReference.ERROR, carPrice);
                    return;
                }

                if (name.length() > 150) {
                    ctx.reply("commands.pet.buy.too_long", EmoteReference.ERROR);
                    return;
                }

                var finalContent = Utils.HTTP_URL.matcher(name).replaceAll("-url-");
                var message = ctx.sendResult(String.format(languageContext.get("commands.marry.buycar.confirm"), EmoteReference.WARNING, carPrice, finalContent));
                ButtonOperations.create(message, 30, (e) -> {
                    if (!e.getUser().equals(ctx.getAuthor()))
                        return Operation.IGNORED;

                    var button = e.getButton().getId();
                    var hook = e.getHook();
                    if (button == null) {
                        return Operation.IGNORED;
                    }

                    if (button.equals("yes")) {
                        var playerConfirmed = ctx.getPlayer();
                        var playerInventoryConfirmed = playerConfirmed.getInventory();
                        var dbUserConfirmed = ctx.getDBUser();
                        var marriageConfirmed = dbUserConfirmed.getData().getMarriage();

                        // People like to mess around lol.
                        if (!playerInventoryConfirmed.containsItem(ItemReference.CAR)) {
                            hook.editOriginal(languageContext.get("commands.marry.buycar.no_car"))
                                    .setComponents().queue();
                            return Operation.COMPLETED;
                        }

                        if (playerConfirmed.getCurrentMoney() < carPrice) {
                            hook.editOriginal(languageContext.get("commands.marry.buycar.not_enough_money"))
                                    .setComponents().queue();
                            return Operation.COMPLETED;
                        }

                        playerInventoryConfirmed.process(new ItemStack(ItemReference.CAR, -1));
                        playerConfirmed.removeMoney(carPrice);
                        playerConfirmed.save();

                        marriageConfirmed.getData().setHasCar(true);
                        marriageConfirmed.getData().setCarName(finalContent);
                        marriageConfirmed.save();

                        hook.editOriginal(languageContext.get("commands.marry.buycar.success").formatted(EmoteReference.POPPER, carPrice, finalContent))
                                .setComponents().queue();
                        return Operation.COMPLETED;
                    }

                    if (button.equals("no")) {
                        hook.editOriginal(languageContext.get("commands.marry.buycar.cancel_success").formatted(EmoteReference.CORRECT))
                                .setComponents().queue();
                        return Operation.COMPLETED;
                    }

                    return Operation.IGNORED;
                }, Button.primary("yes", languageContext.get("buttons.yes")), Button.primary("no", languageContext.get("buttons.no")));
            }
        }
    }

    @Description("Basically divorces you from whoever you are married to.")
    @Defer
    @Category(CommandCategory.CURRENCY)
    public static class Divorce extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            //We, indeed, have no marriage here.
            if (ctx.getDBUser().getData().getMarriage() == null) {
                ctx.reply("commands.divorce.not_married", EmoteReference.ERROR);
                return;
            }

            final var languageContext = ctx.getLanguageContext();
            final var message = ctx.sendResult(String.format(languageContext.get("commands.divorce.confirm"), EmoteReference.WARNING));
            ButtonOperations.create(message, 45, e -> {
                if (e.getUser().getIdLong() != ctx.getAuthor().getIdLong()) {
                    return Operation.IGNORED;
                }

                String buttonId = e.getButton().getId();
                if (buttonId == null) {
                    return Operation.IGNORED;
                }

                var hook = e.getHook();
                if (buttonId.equals("yes")) {
                    final var divorceeDBUser = ctx.getDBUser();
                    final var marriage = divorceeDBUser.getData().getMarriage();
                    if (marriage == null) {
                        hook.editOriginal(languageContext.get("commands.divorce.not_married").formatted(EmoteReference.ERROR)).setComponents().queue();
                        return Operation.COMPLETED;
                    }

                    final var marriageData = marriage.getData();

                    // We do have a marriage, get rid of it.
                    final var marriedWithDBUser = ctx.getDBUser(marriage.getOtherPlayer(ctx.getAuthor().getId()));
                    final var marriedWithPlayer = ctx.getPlayer(marriedWithDBUser.getId());
                    final var divorceePlayer = ctx.getPlayer();

                    // Save the user of the person they were married with.
                    marriedWithDBUser.getData().setMarriageId(null);
                    marriedWithDBUser.save();

                    // Save the user of themselves.
                    divorceeDBUser.getData().setMarriageId(null);
                    divorceeDBUser.save();

                    // Add the heart broken badge to the user who divorced.
                    divorceePlayer.getData().addBadgeIfAbsent(Badge.HEART_BROKEN);

                    // Add the heart broken badge to the user got dumped.
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

                    // Scrape this marriage.
                    marriage.delete();

                    // Split the money between the two people.
                    var portion = moneySplit / 2;
                    divorceePlayer.addMoney(portion);
                    marriedWithPlayer.addMoney(portion);

                    divorceePlayer.save();
                    marriedWithPlayer.save();

                    var extra = "";
                    if (portion > 1) {
                        extra = languageContext.get("commands.divorce.split").formatted(portion);
                    }

                    hook.editOriginal(languageContext.get("commands.divorce.success").formatted(EmoteReference.CORRECT, extra)).setComponents().queue();
                    return Operation.COMPLETED;
                } else if (buttonId.equals("no")) {
                    hook.editOriginal(languageContext.get("commands.divorce.cancelled").formatted(EmoteReference.CORRECT)).setComponents().queue();
                    return Operation.COMPLETED;
                }

                return Operation.IGNORED;
            }, Button.danger("yes", languageContext.get("buttons.yes")), Button.primary("no", languageContext.get("buttons.no")));
        }
    }
}
