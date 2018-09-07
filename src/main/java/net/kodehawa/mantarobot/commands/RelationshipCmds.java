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

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
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
import net.kodehawa.mantarobot.db.entities.helpers.PlayerData;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.RateLimiter;
import sun.reflect.generics.tree.Tree;

import java.awt.*;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Module
//In theory fun category, but created this class to avoid FunCmds to go over 1k lines.
public class RelationshipCmds {

    private final Config config = MantaroData.config().get();
    private static final long waifuBaseValue = 1300L;

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

                                //Give a love letter both to the proposing player and the one who was proposed to.
                                if(proposingPlayerFinalInventory.getAmount(Items.LOVE_LETTER) < 5000)
                                    proposingPlayerFinalInventory.process(new ItemStack(Items.LOVE_LETTER, 1));

                                if(proposedToPlayerInventory.getAmount(Items.LOVE_LETTER) < 5000)
                                    proposedToPlayerInventory.process(new ItemStack(Items.LOVE_LETTER, 1));

                                //Badge assignment saving.
                                proposingPlayer.save();
                                proposedToPlayer.save();

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
                        .addField("Usage", "`~>marry <@mention>` - **Propose to someone**\n" +
                                "`~>marry status` - **Check your marriage status**\n" +
                                "`~>marry createletter <content>` - **Create a love letter for your marriage**", false)
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

                    if(currentMarriage.getData().getLoveLetter() != null) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.marry.loveletter.already_done"), EmoteReference.ERROR).queue();
                        return;
                    }

                    if(content.isEmpty()) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.marry.loveletter.empty"), EmoteReference.ERROR).queue();
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

                    //Send a confirmation message.
                    String finalContent = Utils.DISCORD_INVITE.matcher(content).replaceAll("-invite link-");
                    finalContent = Utils.DISCORD_INVITE_2.matcher(finalContent).replaceAll("-invite link-");

                    new MessageBuilder()
                            .setContent(String.format(languageContext.get("commands.marry.loveletter.confirmation"), EmoteReference.TALKING, marriedTo.getName(),
                                    marriedTo.getDiscriminator(), finalContent))
                            .stripMentions(event.getGuild(), Message.MentionType.EVERYONE, Message.MentionType.HERE, Message.MentionType.USER)
                            .sendTo(event.getChannel())
                            .queue();

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
                            playerFinal.save();

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

        cr.registerAlias("marry", "marriage");
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
    public void waifu(CommandRegistry cr) {
        RateLimiter rl = new RateLimiter(TimeUnit.SECONDS, 5);

        TreeCommand waifu = (TreeCommand) cr.register("waifu", new TreeCommand(Category.FUN) {
            @Override
            public Command defaultTrigger(GuildMessageReceivedEvent event, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
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

                        //Default call will bring out the waifu list.
                        DBUser dbUser = MantaroData.db().getUser(event.getAuthor());
                        UserData userData = dbUser.getData();

                        StringBuilder waifus = new StringBuilder();

                        for(String waifu : userData.getWaifus().keySet()) {
                            User user = MantaroBot.getInstance().getUserById(waifu);
                            if(user == null)
                                continue;

                            waifus.append(EmoteReference.BLUE_SMALL_MARKER)
                                    .append(user.getName())
                                    .append("#")
                                    .append(user.getDiscriminator())
                                    .append(".\n       ")
                                    .append(languageContext.get("commands.waifu.value_format"))
                                    .append(" ")
                                    .append(calculateWaifuValue(user).getFinalValue())
                                    .append(languageContext.get("commands.waifu.credits_format"))
                                    .append("\n       ")
                                    .append(languageContext.get("commands.waifu.value_b_format"))
                                    .append(" ")
                                    .append(userData.getWaifus().get(waifu))
                                    .append(languageContext.get("commands.waifu.credits_format"));

                            //New line owo
                            waifus.append("\n");
                        }

                        String description = userData.getWaifus().isEmpty() ? languageContext.get("commands.waifu.no_waifu") : waifus.toString();

                        EmbedBuilder waifusEmbed = new EmbedBuilder()
                                .setAuthor(languageContext.get("commands.waifu.header"), null, event.getAuthor().getEffectiveAvatarUrl())
                                .setDescription(String.format(languageContext.get("commands.waifu.description_header"), userData.getWaifuSlots()) + description)
                                .setThumbnail("http://www.hey.fr/fun/emoji/twitter/en/twitter/469-emoji_twitter_sparkling_heart.png")
                                .setColor(Color.CYAN)
                                .setFooter(String.format(languageContext.get("commands.waifu.footer"), userData.getWaifus().size(), userData.getWaifuSlots() - userData.getWaifus().size()), null);

                        event.getChannel().sendMessage(waifusEmbed.build()).queue();
                    }
                };
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Waifu Command")
                        .setDescription("**This command is the hub for all waifu operations*\n" +
                                "`~>waifu` - Shows a list of all your waifus and their current value.\n" +
                                "`~>waifu stats` - Shows your waifu stats or the stats or someone\n" +
                                "`~>waifu claim` - Claim a waifu.\n" +
                                "`~>waifu unclaim` - Unclaim a waifu.\n" +
                                "`~>waifu buyslot` - Buy a waifu slots. Maximum possible slots are 20.")
                        .addField("Notice",  "This command is not meant to represent any real life situation, whether real or fake.", true)
                        .build();
            }
        });

        waifu.setPredicate(event -> Utils.handleDefaultRatelimit(rl, event.getAuthor(), event));

        waifu.addSubCommand("stats", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                Member member = Utils.findMember(event, event.getMember(), content);
                if(member == null)
                    return;

                User toLookup = member.getUser();
                if(toLookup.isBot()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.waifu.bot"), EmoteReference.ERROR).queue();
                    return;
                }

                Waifu waifuStats = calculateWaifuValue(toLookup);

                EmbedBuilder statsBuilder = new EmbedBuilder()
                        .setThumbnail(toLookup.getEffectiveAvatarUrl())
                        .setAuthor(toLookup == event.getAuthor() ? languageContext.get("commands.waifu.stats.header") : String.format(languageContext.get("commands.waifu.stats.header_other"), toLookup.getName()),
                                null, toLookup.getEffectiveAvatarUrl()
                        )
                        .setColor(Color.PINK)
                        .setDescription(String.format(languageContext.get("commands.waifu.stats.format"),
                                 EmoteReference.BLUE_SMALL_MARKER, waifuStats.getMoneyValue(), waifuStats.getBadgeValue(), waifuStats.getExperienceValue(), waifuStats.getClaimValue(), waifuStats.getReputationMultiplier())
                        )
                        .addField(languageContext.get("commands.waifu.stats.performance"), EmoteReference.ZAP.toString() + waifuStats.getPerformance() + "wp", true)
                        .addField(languageContext.get("commands.waifu.stats.value"), EmoteReference.BUY + String.format(languageContext.get("commands.waifu.stats.credits"), waifuStats.getFinalValue()), false)
                        .setFooter(languageContext.get("commands.waifu.notice"), null);

                event.getChannel().sendMessage(statsBuilder.build()).queue();
            }
        });

        waifu.addSubCommand("claim", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                if(event.getMessage().getMentionedUsers().isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.waifu.claim.no_user"), EmoteReference.ERROR).queue();
                    return;
                }

                final ManagedDatabase db = MantaroData.db();
                User toLookup = event.getMessage().getMentionedUsers().get(0);

                if(toLookup.isBot()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.waifu.bot"), EmoteReference.ERROR).queue();
                    return;
                }

                final Player claimerPlayer = db.getPlayer(event.getAuthor());
                final DBUser claimerUser = db.getUser(event.getAuthor());
                final UserData claimerUserData = claimerUser.getData();

                final Player claimedPlayer = db.getPlayer(toLookup);
                final DBUser claimedUser = db.getUser(toLookup);
                final UserData claimedUserData = claimedUser.getData();

                //Waifu object declaration.
                final Waifu waifuToClaim = calculateWaifuValue(toLookup);
                final long waifuFinalValue = waifuToClaim.getFinalValue();

                //Checks.

                if(toLookup.getIdLong() == event.getAuthor().getIdLong()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.waifu.claim.yourself"), EmoteReference.ERROR).queue();
                    return;
                }

                //If the to-be claimed has the claim key in their inventory, it cannot be claimed.
                if(claimedPlayer.getInventory().containsItem(Items.CLAIM_KEY)) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.waifu.claim.key_locked"), EmoteReference.ERROR).queue();
                    return;
                }

                if(claimerPlayer.isLocked()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.waifu.claim.locked"), EmoteReference.ERROR).queue();
                    return;
                }

                //Deduct from balance and checks for money.
                if(!claimerPlayer.removeMoney(waifuFinalValue)) {
                    event.getChannel().sendMessageFormat(
                            languageContext.get("commands.waifu.claim.not_enough_money"), EmoteReference.ERROR, waifuFinalValue
                    ).queue();
                    return;
                }

                if(claimerUserData.getWaifus().size() >= claimerUserData.getWaifuSlots()) {
                    event.getChannel().sendMessageFormat(
                            languageContext.get("commands.waifu.claim.not_enough_slots"),
                            EmoteReference.ERROR, claimerUserData.getWaifuSlots(), claimerUserData.getWaifus().size()
                    ).queue();
                    return;
                }

                //Add waifu to claimer list.
                claimerUser.getData().getWaifus().put(toLookup.getId(), waifuFinalValue);
                claimedUserData.setTimesClaimed(claimedUserData.getTimesClaimed() + 1);

                //Add badges
                if(claimedUserData.getWaifus().containsKey(claimerPlayer.getId())) {
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
                event.getChannel().sendMessageFormat(
                        languageContext.get("commands.waifu.claim.success"), EmoteReference.CORRECT, toLookup.getName(), waifuFinalValue, claimerUserData.getWaifus().size()
                ).queue();
            }
        });

        waifu.addSubCommand("unclaim", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                if(content.isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.waifu.unclaim.no_user"), EmoteReference.ERROR).queue();
                    return;
                }

                Member member = Utils.findMember(event, event.getMember(), content);
                if(member == null)
                    return;

                final ManagedDatabase db = MantaroData.db();
                User toLookup = member.getUser();
                if(toLookup.isBot()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.waifu.bot"), EmoteReference.ERROR).queue();
                    return;
                }

                final DBUser claimerUser = db.getUser(event.getAuthor());
                final UserData data = claimerUser.getData();

                long value = data.getWaifus().get(toLookup.getId());
                long valuePayment = (long) (value * 0.15);

                //Send confirmation message.
                event.getChannel().sendMessageFormat(languageContext.get("commands.waifu.unclaim.confirmation"), EmoteReference.MEGA,
                        toLookup.getName(), valuePayment, EmoteReference.STOPWATCH
                ).queue();

                InteractiveOperations.create(event.getChannel(), event.getAuthor().getIdLong(), 60, (ie) -> {
                    if(!ie.getAuthor().getId().equals(event.getAuthor().getId())) {
                        return Operation.IGNORED;
                    }

                    //Replace prefix because people seem to think you have to add the prefix before saying yes.
                    String c = ie.getMessage().getContentRaw();
                    for(String s : config.prefix) {
                        if(c.toLowerCase().startsWith(s)) {
                            c = c.substring(s.length());
                        }
                    }

                    String guildCustomPrefix = db.getGuild(ie.getGuild()).getData().getGuildCustomPrefix();
                    if(guildCustomPrefix != null && !guildCustomPrefix.isEmpty() && c.toLowerCase().startsWith(guildCustomPrefix)) {
                        c = c.substring(guildCustomPrefix.length());
                    }
                    //End of prefix replacing.

                    if(c.equalsIgnoreCase("yes")) {
                        Player p = MantaroData.db().getPlayer(ie.getMember());
                        final DBUser user = db.getUser(event.getAuthor());
                        final UserData userData = user.getData();

                        if(p.getMoney() < valuePayment) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.waifu.unclaim.not_enough_money"), EmoteReference.ERROR).queue();
                            return Operation.COMPLETED;
                        }

                        if(p.isLocked()) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.waifu.unclaim.player_locked"), EmoteReference.ERROR).queue();
                            return Operation.COMPLETED;
                        }

                        p.removeMoney(valuePayment);
                        userData.getWaifus().remove(toLookup.getId());
                        user.save();
                        p.save();

                        event.getChannel().sendMessageFormat(languageContext.get("commands.waifu.unclaim.success"), EmoteReference.CORRECT, toLookup.getName(), valuePayment).queue();
                        return Operation.COMPLETED;
                    } else if (c.equalsIgnoreCase("no")) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.waifu.unclaim.scrapped"), EmoteReference.CORRECT).queue();
                        return Operation.COMPLETED;
                    }

                    return Operation.IGNORED;
                });
            }
        });

        waifu.addSubCommand("buyslot", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                final ManagedDatabase db = MantaroData.db();
                int baseValue = 3000;

                DBUser user = db.getUser(event.getAuthor());
                Player player = db.getPlayer(event.getAuthor());
                final UserData userData = user.getData();

                int currentSlots = userData.getWaifuSlots();
                int baseMultiplier = (currentSlots / 3) + 1;
                int finalValue = baseValue * baseMultiplier;

                if(player.isLocked()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.waifu.buyslot.locked"), EmoteReference.ERROR).queue();
                    return;
                }

                if(player.getMoney() < finalValue) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.waifu.buyslot.not_enough_money"), EmoteReference.ERROR, finalValue).queue();
                    return;
                }

                if(userData.getWaifuSlots() >= 20) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.waifu.buyslot.too_many"), EmoteReference.ERROR).queue();
                    return;
                }

                player.removeMoney(finalValue);
                userData.setWaifuSlots(currentSlots + 1);
                user.save();
                player.save();

                event.getChannel().sendMessageFormat(languageContext.get("commands.waifu.buyslot.success"), EmoteReference.CORRECT, finalValue, userData.getWaifuSlots(), (userData.getWaifuSlots() - userData.getWaifus().size())).queue();
            }
        });
    }

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
        long moneyValue = Math.round(Math.max(1, (int) (waifuPlayer.getMoney() / 135000)) * calculatePercentage(6, waifuBaseValue));
        //Badge calculation.
        long badgeValue = Math.round(Math.max(1, (waifuPlayerData.getBadges().size() / 3)) * calculatePercentage(17, waifuBaseValue));
        //Experience calculator.
        long experienceValue = Math.round(Math.max(1, (int) (waifuPlayer.getData().getExperience() / 2780)) * calculatePercentage(18, waifuBaseValue));
        //Claim calculator.
        long claimValue = Math.round(Math.max(1, (waifuUserData.getTimesClaimed() / 3)) * calculatePercentage(5, waifuBaseValue));

        //"final" value
        waifuValue += moneyValue + badgeValue + experienceValue + claimValue;

        //what is this lol
        //After all those calculations are complete, the value then is calculated using final * (reputation scale / 20) where reputation scale goes up by 1 every 10 reputation points.
        //At 6000 reputation points, the waifu value gets multiplied by 1.1. This is the maximum amount it can be multiplied to.
        long reputation = waifuPlayer.getReputation();
        double reputationScaling = (reputation / 4.5) / 20;
        long finalValue = (long) (
                Math.min(Integer.MAX_VALUE,
                        (waifuValue * (reputationScaling > 1 ? reputationScaling : 1) * (reputation > 6500 ? 1.1 : 1)
                )
        ));

        //waifu pp, yes btmcLewd
        int divide = (int) (moneyValue / 1348);
        performance = ((waifuValue - (waifuBaseValue + 450)) + (long)((reputationScaling > 1 ? reputationScaling : 1) * 1.2)) / (divide > 1 ? divide : 3);
        //possible?
        if(performance < 0)
            performance = 0;


        return new Waifu(moneyValue, badgeValue, experienceValue, reputationScaling, claimValue, finalValue, performance);
    }

    //Yes, I had to do it, fuck.
    private static long calculatePercentage(long percentage, long number) {
        return (percentage * number) / 100;
    }
}
