/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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

import br.com.brjdevs.java.utils.texts.StringUtils;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.TextChannelGround;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.info.stats.manager.CommandStatsManager;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.ITreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Marriage;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.RateLimiter;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Module
@SuppressWarnings("unused")
public class FunCmds {

    private final Random r = new Random();
    private final Config config = MantaroData.config().get();

    @Subscribe
    public void coinflip(CommandRegistry cr) {
        cr.register("coinflip", new SimpleCommand(Category.FUN) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                int times;
                if(args.length == 0 || content.length() == 0) times = 1;
                else {
                    try {
                        times = Integer.parseInt(args[0]);
                        if(times > 1000) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.coinflip.over_limit"), EmoteReference.ERROR).queue();
                            return;
                        }
                    } catch(NumberFormatException nfe) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.coinflip.no_repetitions"), EmoteReference.ERROR).queue();
                        return;
                    }
                }

                final int[] heads = {0};
                final int[] tails = {0};

                doTimes(times, () -> {
                    if(r.nextBoolean()) heads[0]++;
                    else tails[0]++;
                });

                event.getChannel().sendMessageFormat(languageContext.get("commands.coinflip.success"), EmoteReference.PENNY, times, heads[0], tails[0]).queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Coinflip command")
                        .setDescription("**Flips a coin with a defined number of repetitions**")
                        .addField("Usage", "`~>coinflip <number of times>` - **Flips a coin x number of times**", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void marry(CommandRegistry cr) {
        ITreeCommand marryCommand = (ITreeCommand) cr.register("marry", new TreeCommand(Category.FUN) {
            @Override
            public Command defaultTrigger(GuildMessageReceivedEvent event, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                        if(event.getMessage().getMentionedUsers().isEmpty()) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.marry.no_mention"), EmoteReference.ERROR).queue();
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
                        final ManagedDatabase managedDatabase = MantaroData.db();
                        final DBGuild dbGuild = managedDatabase.getGuild(event.getGuild());

                        User proposingUser = event.getAuthor();
                        User proposedToUser = event.getMessage().getMentionedUsers().get(0);

                        //This is just for checking purposes, so we don't need the DBUser itself.
                        UserData proposingUserData = managedDatabase.getUser(proposingUser).getData();
                        UserData proposedToUserData = managedDatabase.getUser(proposedToUser).getData();

                        //Again just for checking, and no need to change.
                        final Inventory proposingPlayerInventory = managedDatabase.getPlayer(proposingUser).getInventory();

                        //Why would you do this...
                        if(proposedToUser.getId().equals(event.getAuthor().getId())) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.marry.marry_yourself_notice"), EmoteReference.ERROR).queue();
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
                        if(proposedToUser.isBot()) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.marry.marry_bot_notice"), EmoteReference.ERROR).queue();
                            return;
                        }

                        //Already married to the same person you're proposing to.
                        if((proposingMarriage != null && proposedToMarriage != null) && proposedToUserData.getMarriage().getId().equals(proposingMarriage.getId())) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.marry.already_married_receipt"), EmoteReference.ERROR).queue();
                            return;
                        }

                        //You're already married. Huh huh.
                        if(proposingMarriage != null) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.marry.already_married"), EmoteReference.ERROR).queue();
                            return;
                        }

                        //Receipt is married, cannot continue.
                        if(proposedToMarriage != null) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.marry.receipt_married"), EmoteReference.ERROR).queue();
                            return;
                        }

                        //Not enough rings to continue. Buy more rings w.
                        if(!proposingPlayerInventory.containsItem(Items.RING) || proposingPlayerInventory.getAmount(Items.RING) < 2) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.marry.no_ring"), EmoteReference.ERROR).queue();
                            return;
                        }

                        //Send confirmation message.
                        event.getChannel().sendMessageFormat(languageContext.get("commands.marry.confirmation"), EmoteReference.MEGA,
                                proposedToUser.getName(), event.getAuthor().getName(), EmoteReference.STOPWATCH
                        ).queue();

                        InteractiveOperations.create(event.getChannel(), event.getAuthor().getIdLong(), 120, (ie) -> {
                            //Ignore all messages from anyone that isn't the user we already proposed to. Waiting for confirmation...
                            if(!ie.getAuthor().getId().equals(proposedToUser.getId()))
                                return Operation.IGNORED;

                            //Replace prefix because people seem to think you have to add the prefix before saying yes.
                            String message = ie.getMessage().getContentRaw();
                            for(String s : config.prefix) {
                                if(message.toLowerCase().startsWith(s)) {
                                    message = message.substring(s.length());
                                }
                            }

                            String guildCustomPrefix = dbGuild.getData().getGuildCustomPrefix();
                            if(guildCustomPrefix != null && !guildCustomPrefix.isEmpty() && message.toLowerCase().startsWith(guildCustomPrefix)) {
                                message = message.substring(guildCustomPrefix.length());
                            }
                            //End of prefix replacing.

                            //Lovely~ <3
                            if(message.equalsIgnoreCase("yes")) {
                                //Here we NEED to get the Player,
                                //User and Marriage objects once again to avoid race conditions or changes on those that might have happened on the 120 seconds that this lasted for.
                                //We need to check if the marriage is empty once again before continuing, also if we have enough rings!
                                //USE THOSE VARIABLES TO MODIFY DATA, NOT THE ONES USED TO CHECK BEFORE THE CONFIRMATION MESSAGE. THIS IS EXTREMELY IMPORTANT.
                                //Else we end up with really annoying to debug bugs, lol.
                                Player proposingPlayer = managedDatabase.getPlayer(proposingUser);
                                Player proposedToPlayer = managedDatabase.getPlayer(proposedToUser);
                                DBUser proposingUserDB = managedDatabase.getUser(proposingUser);
                                DBUser proposedToUserDB = managedDatabase.getUser(proposedToUser);


                                // ---------------- START OF FINAL MARRIAGE CHECK ----------------
                                final Marriage proposingMarriageFinal = proposingUserData.getMarriage();
                                final Marriage proposedToMarriageFinal = proposedToUserData.getMarriage();

                                if(proposingMarriageFinal != null) {
                                    event.getChannel().sendMessageFormat(languageContext.get("commands.marry.already_married"), EmoteReference.ERROR).queue();
                                    return Operation.COMPLETED;
                                }

                                if(proposedToMarriageFinal != null) {
                                    event.getChannel().sendMessageFormat(languageContext.get("commands.marry.receipt_married"), EmoteReference.ERROR).queue();
                                    return Operation.COMPLETED;
                                }
                                // ---------------- END OF FINAL MARRIAGE CHECK ----------------

                                // ---------------- START OF INVENTORY CHECKS ----------------
                                //LAST inventory check and ring assignment is gonna happen using those.
                                final Inventory proposingPlayerFinalInventory = proposingPlayer.getInventory();
                                final Inventory proposedToPlayerInventory = proposedToPlayer.getInventory();

                                if(proposingPlayerFinalInventory.getAmount(Items.RING) < 2) {
                                    event.getChannel().sendMessageFormat(languageContext.get("commands.marry.ring_check_fail"), EmoteReference.ERROR).queue();
                                    return Operation.COMPLETED;
                                }

                                //Remove the ring from the proposing player inventory.
                                proposingPlayerFinalInventory.process(new ItemStack(Items.RING, -1));

                                //Silently scrape the rings if the receipt has more than 5000 rings.
                                if(proposedToPlayerInventory.getAmount(Items.RING) < 5000) {
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
                                ie.getChannel().sendMessageFormat(languageContext.get("commands.marry.accepted"),
                                        EmoteReference.POPPER, ie.getAuthor().getName(), ie.getAuthor().getDiscriminator(), proposingUser.getName(), proposingUser.getDiscriminator()
                                ).queue();

                                //Add the badge to the married couple.
                                proposingPlayer.getData().addBadgeIfAbsent(Badge.MARRIED);
                                proposedToPlayer.getData().addBadgeIfAbsent(Badge.MARRIED);

                                //Badge assignment saving.
                                proposingPlayer.save();
                                proposedToPlayer.save();

                                //Drop a love letter to the text channel ground.
                                TextChannelGround.of(event).dropItemWithChance(Items.LOVE_LETTER, 2);
                                return Operation.COMPLETED;
                            }

                            if(message.equalsIgnoreCase("no")) {
                                ie.getChannel().sendMessageFormat(languageContext.get("commands.marry.denied"), EmoteReference.CORRECT, proposingUser.getName()).queue();

                                //Well, we have a badge for this too. Consolation prize I guess.
                                final Player proposingPlayer = managedDatabase.getPlayer(proposingUser);
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
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Marriage command")
                        .setDescription("**Basically marries you with a user.**")
                        .addField("Usage", "`~>marry <@mention>` - **Propose to someone**", false)
                        .addField(
                                "Divorcing", "Well, if you don't want to be married anymore you can just do `~>divorce`",
                                false
                        )
                        .build();
            }
        });

        marryCommand.addSubCommand("createletter", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                final ManagedDatabase db = MantaroData.db();
                final User author = event.getAuthor();

                Player player = db.getPlayer(author);
                Inventory playerInventory = player.getInventory();
                DBUser dbUser = db.getUser(author);

                //Without one love letter we cannot do much, ya know.
                if(playerInventory.containsItem(Items.LOVE_LETTER)) {
                    final Marriage currentMarriage = dbUser.getData().getMarriage();

                    //Check if the user is married, is the proposed player, there's no love letter and that the love letter is less than 1500 characters long.
                    if(currentMarriage == null) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.marry.loveletter.no_marriage"), EmoteReference.SAD).queue();
                        return;
                    }

                    if(!author.getId().equals(currentMarriage.getPlayer1())) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.marry.loveletter.not_proposing_player"), EmoteReference.ERROR).queue();
                        return;
                    }

                    if(currentMarriage.getData().getLoveLetter() != null || !currentMarriage.getData().getLoveLetter().isEmpty()) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.marry.loveletter.already_done"), EmoteReference.ERROR).queue();
                        return;
                    }

                    if(content.length() > 500) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.marry.loveletter.too_long"), EmoteReference.ERROR).queue();
                        return;
                    }

                    //Can we find the user this is married to?
                    final User marriedTo = MantaroBot.getInstance().getUserById(currentMarriage.getOtherPlayer(author.getId()));
                    if(marriedTo == null) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.marry.loveletter.cannot_see"), EmoteReference.ERROR).queue();
                        return;
                    }

                    event.getChannel().sendMessageFormat(languageContext.get("commands.marry.loveletter.confirmation"),
                            EmoteReference.TALKING, marriedTo.getName(), marriedTo.getDiscriminator(), content
                    ).queue();

                    //Start the operation.
                    InteractiveOperations.create(event.getChannel(), author.getIdLong(), 60, e -> {
                        if(!e.getAuthor().getId().equals(author.getId())) {
                            return Operation.IGNORED;
                        }

                        //Replace prefix because people seem to think you have to add the prefix before saying yes.
                        String c = e.getMessage().getContentRaw();
                        for(String s : config.prefix) {
                            if(c.toLowerCase().startsWith(s)) {
                                c = c.substring(s.length());
                            }
                        }

                        String guildCustomPrefix = db.getGuild(e.getGuild()).getData().getGuildCustomPrefix();
                        if(guildCustomPrefix != null && !guildCustomPrefix.isEmpty() && c.toLowerCase().startsWith(guildCustomPrefix)) {
                            c = c.substring(guildCustomPrefix.length());
                        }
                        //End of prefix replacing.

                        //Confirmed they want to save this as the permanent love letter.
                        if(c.equalsIgnoreCase("yes")) {
                            final Player playerFinal = db.getPlayer(author);
                            final Inventory inventoryFinal = playerFinal.getInventory();
                            final DBUser dbUserFinal = db.getUser(author);
                            final Marriage currentMarriageFinal = dbUser.getData().getMarriage();

                            //We need to do most of the checks all over again just to make sure nothing important slipped through.
                            if(currentMarriageFinal == null) {
                                event.getChannel().sendMessageFormat(languageContext.get("commands.marry.loveletter.no_marriage"), EmoteReference.SAD).queue();
                                return Operation.COMPLETED;
                            }

                            if(!inventoryFinal.containsItem(Items.LOVE_LETTER)) {
                                event.getChannel().sendMessageFormat(languageContext.get("commands.marry.loveletter.no_letter"), EmoteReference.SAD).queue();
                                return Operation.COMPLETED;
                            }

                            //Remove the love letter from the inventory.
                            inventoryFinal.process(new ItemStack(Items.LOVE_LETTER, -1));
                            player.save();

                            //Save the love letter. The content variable is the actual letter, while c is the content of the operation itself.
                            //Yes it's confusing.
                            currentMarriageFinal.getData().setLoveLetter(content);
                            currentMarriageFinal.save();

                            event.getChannel().sendMessageFormat(languageContext.get("commands.marry.loveletter.confirmed"), EmoteReference.CORRECT).queue();
                            return Operation.COMPLETED;
                        } else if (c.equalsIgnoreCase("no")) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.marry.loveletter.scrapped"), EmoteReference.CORRECT).queue();
                            return Operation.COMPLETED;
                        }

                        return Operation.IGNORED;
                    });
                } else {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.marry.loveletter.no_letter"), EmoteReference.SAD).queue();
                }
            }
        });

        marryCommand.addSubCommand("status", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                final ManagedDatabase db = MantaroData.db();
                final User author = event.getAuthor();
                DBUser dbUser = db.getUser(author);
                final Marriage currentMarriage = dbUser.getData().getMarriage();

                //What status would we have without marriage? Well, we can be unmarried omegalul.
                if (currentMarriage == null) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.marry.status.no_marriage"), EmoteReference.SAD).queue();
                    return;
                }

                //Can we find the user this is married to?
                final User marriedTo = MantaroBot.getInstance().getUserById(currentMarriage.getOtherPlayer(author.getId()));
                if (marriedTo == null) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.marry.loveletter.cannot_see"), EmoteReference.ERROR).queue();
                    return;
                }

                //Get the current love letter.
                String loveLetter = currentMarriage.getData().getLoveLetter();
                if(loveLetter == null || loveLetter.isEmpty()) {
                    loveLetter = "None.";
                }

                //This would be good if it was 2008. But it works.
                Date marriageDate = new Date(currentMarriage.getData().getMarriageCreationMillis());
                boolean eitherHasWaifus = !(dbUser.getData().getWaifus().isEmpty() && db.getUser(marriedTo).getData().getWaifus().isEmpty());
                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setThumbnail("http://www.hey.fr/fun/emoji/twitter/en/twitter/469-emoji_twitter_sparkling_heart.png")
                        .setAuthor(languageContext.get("commands.marry.status.header"), null, event.getAuthor().getEffectiveAvatarUrl())
                        .setDescription(String.format(languageContext.get("commands.marry.status.description_format"),
                                EmoteReference.HEART, author.getName(), author.getDiscriminator(), marriedTo.getName(), marriedTo.getDiscriminator())
                        )
                        .addField(languageContext.get("commands.marry.status.date"), marriageDate.toString(), false)
                        .addField(languageContext.get("commands.marry.status.love_letter"), loveLetter, false)
                        .addField(languageContext.get("commands.marry.status.waifus"), String.valueOf(eitherHasWaifus), false)
                        .setFooter("Marriage ID: " + currentMarriage.getId(), null);


                event.getChannel().sendMessage(embedBuilder.build()).queue();
            }
        });
    }

    @Subscribe
    public void divorce(CommandRegistry cr) {
        cr.register("divorce", new SimpleCommand(Category.FUN) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                final ManagedDatabase managedDatabase = MantaroData.db();
                final Player divorceePlayer = managedDatabase.getPlayer(event.getAuthor());
                //Assume we're dealing with a new marriage?
                if (divorceePlayer.getData().getMarriedWith() == null) {
                    final DBUser divorceeDBUser = managedDatabase.getUser(event.getAuthor());
                    final Marriage marriage = divorceeDBUser.getData().getMarriage();

                    //We, indeed, have no marriage here.
                    if (marriage == null) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.divorce.not_married"), EmoteReference.ERROR).queue();
                        return;
                    }

                    //We do have a marriage, get rid of it.
                    String marriageId = marriage.getId();
                    DBUser marriedWithDBUser = managedDatabase.getUser(marriage.getOtherPlayer(event.getAuthor().getId()));
                    final Player marriedWithPlayer = managedDatabase.getPlayer(marriedWithDBUser.getId());

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
                    event.getChannel().sendMessageFormat(languageContext.get("commands.divorce.success"), EmoteReference.CORRECT).queue();

                    return;
                }

                // ---------------- START OF LEGACY MARRIAGE SUPPORT ----------------
                User userMarriedWith = divorceePlayer.getData().getMarriedWith() == null ? null : MantaroBot.getInstance().getUserById(divorceePlayer.getData().getMarriedWith());

                if(userMarriedWith == null) {
                    divorceePlayer.getData().setMarriedWith(null);
                    divorceePlayer.getData().setMarriedSince(0L);
                    divorceePlayer.getData().addBadgeIfAbsent(Badge.HEART_BROKEN);
                    divorceePlayer.saveAsync();
                    event.getChannel().sendMessageFormat(languageContext.get("commands.divorce.success"), EmoteReference.CORRECT).queue();
                    return;
                }

                Player marriedWith = managedDatabase.getPlayer(userMarriedWith);

                marriedWith.getData().setMarriedWith(null);
                marriedWith.getData().setMarriedSince(0L);
                marriedWith.getData().addBadgeIfAbsent(Badge.HEART_BROKEN);
                marriedWith.save();

                divorceePlayer.getData().setMarriedWith(null);
                divorceePlayer.getData().setMarriedSince(0L);
                divorceePlayer.getData().addBadgeIfAbsent(Badge.HEART_BROKEN);
                divorceePlayer.save();
                // ---------------- END OF LEGACY MARRIAGE SUPPORT ----------------

                event.getChannel().sendMessageFormat(languageContext.get("commands.divorce.success"), EmoteReference.CORRECT).queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Divorce command")
                        .setDescription("**Basically divorces you from whoever you were married to.**")
                        .build();
            }
        });
    }

    @Subscribe
    public void ratewaifu(CommandRegistry cr) {
        cr.register("ratewaifu", new SimpleCommand(Category.FUN) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {

                if(args.length == 0) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.love.nothing_specified"), EmoteReference.ERROR).queue();
                    return;
                }

                int waifuRate = content.replaceAll("\\s+", " ").replaceAll("<@!?(\\d+)>", "<@$1>").chars().sum() % 101;
                if(content.equalsIgnoreCase("mantaro")) waifuRate = 100;

                new MessageBuilder().setContent(String.format(languageContext.get("commands.ratewaifu.success"), EmoteReference.THINKING, content, waifuRate))
                        .stripMentions(event.getGuild(), Message.MentionType.EVERYONE, Message.MentionType.HERE).sendTo(event.getChannel()).queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Rate your waifu")
                        .setDescription("**Just rates your waifu from zero to 100. Results may vary.**")
                        .build();
            }
        });

        cr.registerAlias("ratewaifu", "rw");
    }

    @Subscribe
    public void roll(CommandRegistry registry) {
        final RateLimiter rateLimiter = new RateLimiter(TimeUnit.SECONDS, 10);

        registry.register("roll", new SimpleCommand(Category.FUN) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(!Utils.handleDefaultRatelimit(rateLimiter, event.getAuthor(), event))
                    return;

                Map<String, Optional<String>> opts = StringUtils.parse(args);
                int size = 6, amount = 1;

                if(opts.containsKey("size")) {
                    try {
                        size = Integer.parseInt(opts.get("size").orElse(""));
                    } catch(Exception ignored) { }
                }

                if(opts.containsKey("amount")) {
                    try {
                        amount = Integer.parseInt(opts.get("amount").orElse(""));
                    } catch(Exception ignored) { }
                } else if(opts.containsKey(null)) { //Backwards Compatibility
                    try {
                        amount = Integer.parseInt(opts.get(null).orElse(""));
                    } catch(Exception ignored) { }
                }

                if(amount >= 100) amount = 100;
                event.getChannel().sendMessageFormat(languageContext.get("commands.roll.success"), EmoteReference.DICE, diceRoll(size, amount), amount == 1 ? "!" : (String.format("\nDoing **%d** rolls.", amount))).queue();

                TextChannelGround.of(event.getChannel()).dropItemWithChance(Items.LOADED_DICE, 5);
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Dice command")
                        .setDescription(
                                "Roll a any-sided dice a 1 or more times\n" +
                                        "`~>roll [-amount <number>] [-size <number>]`: Rolls a dice of the specified size the specified times.\n" +
                                        "(By default, this command will roll a 6-sized dice 1 time.)"
                        )
                        .build();
            }
        });
    }

    @Subscribe
    public void love(CommandRegistry registry) {
        final SecureRandom random = new SecureRandom();
        registry.register("love", new SimpleCommand(Category.FUN) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                List<User> mentioned = event.getMessage().getMentionedUsers();
                String result;

                if(mentioned.size() < 1) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.love.no_mention"), EmoteReference.ERROR).queue();
                    return;
                }

                long[] ids = new long[2];
                List<String> listDisplay = new ArrayList<>();
                String toDisplay;
                listDisplay.add(String.format("\uD83D\uDC97  %s#%s", mentioned.get(0).getName(), mentioned.get(0).getDiscriminator()));
                listDisplay.add(String.format("\uD83D\uDC97  %s#%s", event.getAuthor().getName(), event.getAuthor().getDiscriminator()));
                toDisplay = listDisplay.stream().collect(Collectors.joining("\n"));

                if(mentioned.size() > 1) {
                    ids[0] = mentioned.get(0).getIdLong();
                    ids[1] = mentioned.get(1).getIdLong();
                    toDisplay = mentioned.stream()
                            .map(user -> "\uD83D\uDC97  " + user.getName() + "#" + user.getDiscriminator()).collect(Collectors.joining("\n"));
                } else {
                    ids[0] = event.getAuthor().getIdLong();
                    ids[1] = mentioned.get(0).getIdLong();
                }

                int percentage = (ids[0] == ids[1] ? 101 : random.nextInt(101)); //last value is exclusive, so 101.

                if(percentage < 45) {
                    result = languageContext.get("commands.love.not_ideal");
                } else if(percentage < 75) {
                    result = languageContext.get("commands.love.decent");
                } else if(percentage < 100) {
                    result = languageContext.get("commands.love.nice");
                } else {
                    result = languageContext.get("commands.love.perfect");
                    if(percentage == 101) {
                        result = languageContext.get("commands.love.yourself_note");
                    }
                }

                MessageEmbed loveEmbed = new EmbedBuilder()
                        .setAuthor("\u2764 " + languageContext.get("commands.love.header") + " \u2764", null, event.getAuthor().getEffectiveAvatarUrl())
                        .setThumbnail("http://www.hey.fr/fun/emoji/twitter/en/twitter/469-emoji_twitter_sparkling_heart.png")
                        .setDescription("\n**" + toDisplay + "**\n\n" +
                                percentage + "% ||  " + CommandStatsManager.bar(percentage, 40) + "  **||** \n\n" +
                                "**" + languageContext.get("commands.love.result") + "** `"
                                + result + "`")
                        .setColor(event.getMember().getColor())
                        .build();

                event.getChannel().sendMessage(loveEmbed).queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Love Meter")
                        .setDescription("**Calculates the love between 2 discord users**")
                        .addField("Considerations", "You can either mention one user (matches with yourself) or two (matches 2 users)", false)
                        .build();
            }
        });
    }

    private long diceRoll(int size, int amount) {
        long sum = 0;
        for(int i = 0; i < amount; i++) sum += r.nextInt(size) + 1;
        return sum;
    }
}
