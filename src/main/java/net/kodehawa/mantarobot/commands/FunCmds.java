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
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.RateLimiter;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Module
public class FunCmds {

    private final Random r = new Random();
    private final Config config = MantaroData.config().get();

    @Subscribe
    public void coinflip(CommandRegistry cr) {
        cr.register("coinflip", new SimpleCommand(Category.FUN) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                int times;
                if(args.length == 0 || content.length() == 0) times = 1;
                else {
                    try {
                        times = Integer.parseInt(args[0]);
                        if(times > 1000) {
                            event.getChannel().sendMessage(
                                    EmoteReference.ERROR + "Hold in there! The limit is 1,000 coin flips").queue();
                            return;
                        }
                    } catch(NumberFormatException nfe) {
                        event.getChannel().sendMessage(
                                EmoteReference.ERROR + "You need to specify an Integer for the amount of repetitions").queue();
                        return;
                    }
                }

                final int[] heads = {0};
                final int[] tails = {0};

                doTimes(times, () -> {
                    if(r.nextBoolean()) heads[0]++;
                    else tails[0]++;
                });

                String flips = times == 1 ? "time" : "times";
                event.getChannel().sendMessage(
                        String.format("%s Your result from **%d** %s yielded **%d** heads and **%d** tails", EmoteReference.PENNY, times, flips, heads[0], tails[0])).queue();
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
        cr.register("marry", new SimpleCommand(Category.FUN) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                if(event.getMessage().getMentionedUsers().isEmpty()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "Mention the user you want to marry.").queue();
                    return;
                }

                DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                User proposing = event.getAuthor();
                User proposedTo = event.getMessage().getMentionedUsers().get(0);
                Player proposingPlayer = MantaroData.db().getPlayer(proposing);
                Player proposedPlayer = MantaroData.db().getPlayer(proposedTo);
                User proposingMarriedWith = proposingPlayer.getData().getMarriedWith() == null ? null : MantaroBot.getInstance().getUserById(proposingPlayer.getData().getMarriedWith());

                Inventory playerInventory = proposingPlayer.getInventory();

                if(proposedTo.getId().equals(event.getAuthor().getId())) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot marry yourself, as much as you may want to.").queue();
                    return;
                }

                if(proposedTo.isBot()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot marry a bot.").queue();
                    return;
                }

                if(proposingMarriedWith != null && proposingMarriedWith.getId().equals(proposedTo.getId())) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You're married with them already, aww.").queue();
                    return;
                }

                if(proposingMarriedWith != null) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You're married already.").queue();
                    return;
                }

                if(proposedPlayer.getData().isMarried()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "That user is married already.").queue();
                    return;
                }

                if(!playerInventory.containsItem(Items.RING) || playerInventory.getAmount(Items.RING) < 2) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot propose without two marriage rings! You can buy them by doing `~>market buy 2 ring` and then try proposing again <3").queue();
                    return;
                }

                event.getChannel().sendMessage(String.format("%s%s, type with **yes** or **no** to the marriage proposal from %s.", EmoteReference.MEGA,
                        proposedTo.getName(), event.getAuthor().getName())).queue();
                InteractiveOperations.createOverriding(event.getChannel(), 120, (ie) -> {
                    if(!ie.getAuthor().getId().equals(proposedTo.getId()))
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

                    if(message.equalsIgnoreCase("yes")) {
                        Player proposed = MantaroData.db().getPlayer(proposedTo);
                        Player author = MantaroData.db().getPlayer(proposing);
                        Inventory authorInventory = author.getInventory();

                        if(authorInventory.getAmount(Items.RING) < 2) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot marry with less than two rings on your inventory!").queue();
                            return Operation.COMPLETED;
                        }

                        proposed.getData().setMarriedWith(proposing.getId());
                        proposed.getData().setMarriedSince(System.currentTimeMillis());
                        proposed.getData().addBadgeIfAbsent(Badge.MARRIED);

                        author.getData().setMarriedWith(proposedTo.getId());
                        author.getData().setMarriedSince(System.currentTimeMillis());
                        author.getData().addBadgeIfAbsent(Badge.MARRIED);

                        Inventory proposedInventory = proposed.getInventory();

                        authorInventory.process(new ItemStack(Items.RING, -1));

                        if(proposedInventory.getAmount(Items.RING) < 5000) {
                            proposedInventory.process(new ItemStack(Items.RING, 1));
                        }

                        ie.getChannel().sendMessage(String.format("%s%s accepted the proposal of %s!", EmoteReference.POPPER, ie.getAuthor().getName(), proposing.getName())).queue();
                        proposed.save();
                        author.save();

                        TextChannelGround.of(event).dropItemWithChance(Items.LOVE_LETTER, 2);
                        return Operation.COMPLETED;
                    }

                    if(message.equalsIgnoreCase("no")) {
                        ie.getChannel().sendMessage(EmoteReference.CORRECT + "Denied proposal from " + proposing.getName()).queue();
                        proposingPlayer.getData().addBadgeIfAbsent(Badge.DENIED);
                        proposingPlayer.saveAsync();
                        return Operation.COMPLETED;
                    }

                    return Operation.IGNORED;
                });
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
    }

    @Subscribe
    public void divorce(CommandRegistry cr) {
        cr.register("divorce", new SimpleCommand(Category.FUN) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                Player divorcee = MantaroData.db().getPlayer(event.getMember());

                if(divorcee.getData().getMarriedWith() == null) {
                    event.getChannel().sendMessage(
                            EmoteReference.ERROR + "You aren't married with anyone, why don't you find that special someone?")
                            .queue();
                    return;
                }

                User userMarriedWith = divorcee.getData().getMarriedWith() == null ? null : MantaroBot.getInstance().getUserById(divorcee.getData().getMarriedWith());

                if(userMarriedWith == null) {
                    divorcee.getData().setMarriedWith(null);
                    divorcee.getData().setMarriedSince(0L);
                    divorcee.saveAsync();
                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Now you're single. That's nice I guess.").queue();
                    return;
                }

                Player marriedWith = MantaroData.db().getPlayer(userMarriedWith);

                marriedWith.getData().setMarriedWith(null);
                marriedWith.getData().setMarriedSince(0L);
                marriedWith.getData().addBadgeIfAbsent(Badge.HEART_BROKEN);
                marriedWith.save();

                divorcee.getData().setMarriedWith(null);
                divorcee.getData().setMarriedSince(0L);
                divorcee.getData().addBadgeIfAbsent(Badge.HEART_BROKEN);
                divorcee.save();

                event.getChannel().sendMessage(EmoteReference.CORRECT + "Now you're single. That's nice I guess.").queue();
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
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {

                if(args.length == 0) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "Give me a waifu to rate!").queue();
                    return;
                }

                int waifuRate = r.nextInt(100);
                if(content.equalsIgnoreCase("mantaro")) waifuRate = 100;

                new MessageBuilder().setContent(String.format("%sI rate %s with a **%d/100**", EmoteReference.THINKING, content, waifuRate))
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
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
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
                event.getChannel().sendMessage(
                        String.format("%sYou got **%d**%s", EmoteReference.DICE, diceRoll(size, amount), amount == 1 ? "!" : (", doing **" + amount + "** rolls."))
                ).queue();

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
        registry.register("love", new SimpleCommand(Category.FUN) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                List<User> mentioned = event.getMessage().getMentionedUsers();
                String result;

                if(mentioned.size() < 1) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You need to mention at least 1 user.").queue();
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

                int percentage = (int)(ids[0] == ids[1] ? 101 : (ids[0] + ids[1]) % 101L);

                if(percentage < 45) {
                    result = "Try again next time...";
                } else if(percentage < 75) {
                    result = "Good enough!";
                } else if(percentage < 100) {
                    result = "Good match!";
                } else {
                    result = "Perfect match!";
                    if(percentage == 101) {
                        result = "You're a special creature and you should love yourself more than anyone <3";
                    }
                }

                MessageEmbed loveEmbed = new EmbedBuilder()
                        .setAuthor("\u2764 Love Meter \u2764", null, event.getAuthor().getEffectiveAvatarUrl())
                        .setThumbnail("http://www.hey.fr/fun/emoji/twitter/en/twitter/469-emoji_twitter_sparkling_heart.png")
                        .setDescription("\n**" + toDisplay + "**\n\n" +
                                percentage + "% ||  " + CommandStatsManager.bar(percentage, 40) + "  **||** \n\n" +
                                "**Result:** `"
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
