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
import com.jagrosh.jdautilities.utils.FinderUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import net.kodehawa.mantarobot.db.entities.helpers.PlayerData;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.RateLimiter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.awt.*;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.utils.StringUtils.SPLIT_PATTERN;
import static net.kodehawa.mantarobot.utils.Utils.handleDefaultRatelimit;

@Module
public class PlayerCmds {
    private final OkHttpClient client = new OkHttpClient();

    @Subscribe
    public void rep(CommandRegistry cr) {
        cr.register("rep", new SimpleCommand(Category.CURRENCY) {
            final RateLimiter rateLimiter = new RateLimiter(TimeUnit.HOURS, 12);

            @Override
            public void call(GuildMessageReceivedEvent event, String content, String[] args) {
                long rl = rateLimiter.tryAgainIn(event.getMember());
                User user;

                if(content.isEmpty()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You need to mention or put the name of at least one user.\n" +
                            (rl > 0 ? "**You'll be able to use this command again in " +
                                    Utils.getVerboseTime(rateLimiter.tryAgainIn(event.getMember())) + ".**" :
                                    "You can rep someone now.")).queue();
                    return;
                }

                List<User> mentioned = event.getMessage().getMentionedUsers();
                if(!mentioned.isEmpty() && mentioned.size() > 1) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You can only give reputation to one person!").queue();
                    return;
                }

                Member member = Utils.findMember(event, event.getMember(), content);
                if(member == null) return;

                user = member.getUser();

                if(user.isBot()) {
                    event.getChannel().sendMessage(EmoteReference.THINKING + "You cannot rep a bot.\n" +
                            (rl > 0 ? "**You'll be able to use this command again in " +
                                    Utils.getVerboseTime(rateLimiter.tryAgainIn(event.getMember())) + ".**" :
                                    "You can rep someone now.")).queue();
                    return;
                }

                if(user.equals(event.getAuthor())) {
                    event.getChannel().sendMessage(EmoteReference.THINKING + "You cannot rep yourself.\n" +
                            (rl > 0 ? "**You'll be able to use this command again in " +
                                    Utils.getVerboseTime(rateLimiter.tryAgainIn(event.getMember())) + ".**" :
                                    "You can rep someone now.")).queue();
                    return;
                }

                if(!handleDefaultRatelimit(rateLimiter, event.getAuthor(), event)) return;
                Player player = MantaroData.db().getPlayer(user);
                player.addReputation(1L);
                player.save();
                event.getChannel().sendMessage(EmoteReference.CORRECT + "Added reputation to **" + member.getEffectiveName() + "**").queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Reputation command")
                        .setDescription("**Reps an user**")
                        .addField("Usage", "`~>rep <@user>` - **Gives reputation to x user**", false)
                        .addField("Parameters", "`@user` - user to mention", false)
                        .addField("Important", "Only usable every 12 hours.", false)
                        .build();
            }
        });

        cr.registerAlias("rep", "reputation");
    }

    @Subscribe
    public void profile(CommandRegistry cr) {
        cr.register("profile", new SimpleCommand(Category.CURRENCY) {
            @Override
            public void call(GuildMessageReceivedEvent event, String content, String[] args) {
                Player player = MantaroData.db().getPlayer(event.getMember());
                DBUser dbUser = MantaroData.db().getUser(event.getMember());
                User author = event.getAuthor();

                if(args.length > 0 && args[0].equals("timezone")) {
                    if(args.length < 2) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You need to specify the timezone.").queue();
                        return;
                    }

                    if(args[1].equalsIgnoreCase("reset")) {
                        dbUser.getData().setTimezone(null);
                        dbUser.saveAsync();
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Reset timezone.").queue();
                        return;
                    }

                    if(args[1].length() > 5) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Input is too long...").queue();
                        return;
                    }

                    try {
                        UtilsCmds.dateGMT(event.getGuild(), args[1]);
                    } catch(Exception e) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Not a valid timezone.").queue();
                        return;
                    }

                    dbUser.getData().setTimezone(args[1]);
                    dbUser.saveAsync();
                    event.getChannel().sendMessage(String.format("%sSaved timezone, your profile timezone is now: **%s**", EmoteReference.CORRECT, args[1])).queue();
                    return;
                }

                if(args.length > 0 && args[0].equals("description")) {
                    if(args.length == 1) {
                        event.getChannel().sendMessage(EmoteReference.ERROR +
                                "You need to provide an argument! (set or remove)\n" +
                                "for example, ~>profile description set Hi there!").queue();
                        return;
                    }

                    if(args[1].equals("set")) {
                        int MAX_LENGTH = 300;
                        if(MantaroData.db().getUser(author).isPremium()) MAX_LENGTH = 500;
                        String content1 = SPLIT_PATTERN.split(content, 3)[2];

                        if(content1.length() > MAX_LENGTH) {
                            event.getChannel().sendMessage(EmoteReference.ERROR +
                                    "The description is too long! `(Limit of 300 characters for everyone and 500 for premium users)`")
                                    .queue();
                            return;
                        }

                        player.getData().setDescription(content1);
                        event.getChannel().sendMessage(EmoteReference.POPPER + "Set description to: **" + content1 + "**\n" +
                                "Check your shiny new profile with `~>profile`").queue();
                        player.save();
                        return;
                    }

                    if(args[1].equals("clear")) {
                        player.getData().setDescription(null);
                        event.getChannel().sendMessage(EmoteReference.CORRECT + "Successfully cleared description.").queue();
                        player.save();
                        return;
                    }
                }

                UserData user = MantaroData.db().getUser(event.getMember()).getData();
                Member member = event.getMember();

                List<Member> found = FinderUtil.findMembers(content, event.getGuild());

                if(found.isEmpty() && !content.isEmpty()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "Your search yielded no results :(").queue();
                    return;
                }

                if(found.size() > 1 && !content.isEmpty()) {
                    event.getChannel().sendMessage(EmoteReference.THINKING + "Too many users found, maybe refine your search? (ex. use name#discriminator)\n" +
                            "**Users found:** " + found.stream().map(m -> m.getUser().getName() + "#" + m.getUser().getDiscriminator()).collect(Collectors.joining(", "))).queue();
                    return;
                }

                if(found.size() == 1 && !content.isEmpty()) {
                    author = found.get(0).getUser();
                    member = found.get(0);

                    if(author.isBot()) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Bots don't have profiles.").queue();
                        return;
                    }

                    user = MantaroData.db().getUser(author).getData();
                    player = MantaroData.db().getPlayer(member);
                }

                User marriedTo = player.getData().getMarriedWith() == null ? null : MantaroBot.getInstance().getUserById(player.getData().getMarriedWith());

                //Yes, two different things
                if(player.getData().getMarriedWith() != null && marriedTo == null) {
                    player.getData().setMarriedWith(null);
                    player.getData().setMarriedSince(null);
                    player.saveAsync();
                }

                PlayerData playerData = player.getData();
                Inventory inv = player.getInventory();
                boolean saveAfter = false;

                //start of badge assigning
                if(player.getMoney() > 7526527671L && player.getData().addBadgeIfAbsent(Badge.ALTERNATIVE_WORLD))
                    saveAfter = true;
                if(MantaroData.config().get().isOwner(author) && player.getData().addBadgeIfAbsent(Badge.DEVELOPER))
                    saveAfter = true;
                if(inv.asList().stream().anyMatch(stack -> stack.getAmount() == 5000) && player.getData().addBadgeIfAbsent(Badge.SHOPPER))
                    saveAfter = true;
                if(inv.asList().stream().anyMatch(stack -> stack.getItem().equals(Items.CHRISTMAS_TREE_SPECIAL) || stack.getItem().equals(Items.BELL_SPECIAL)) && player.getData().addBadgeIfAbsent(Badge.CHRISTMAS))
                    saveAfter = true;
                if(MantaroBot.getInstance().getShardedMantaro().getDiscordBotsUpvoters().contains(author.getIdLong()) && player.getData().addBadgeIfAbsent(Badge.UPVOTER))
                    saveAfter = true;

                if(saveAfter)
                    player.saveAsync();
                //end of badge assigning

                List<Badge> badges = playerData.getBadges();
                Collections.sort(badges);
                String displayBadges = badges.stream().map(Badge::getUnicode).limit(5).collect(Collectors.joining("  "));

                applyBadge(event.getChannel(), badges.isEmpty() ? null : badges.get(0), author, baseEmbed(event, (marriedTo == null || !player.getInventory().containsItem(Items.RING) ? "" :
                        EmoteReference.RING) + member.getEffectiveName() + "'s Profile", author.getEffectiveAvatarUrl())
                        .setThumbnail(author.getEffectiveAvatarUrl())
                        .setDescription((badges.isEmpty() ? "" : String.format("**%s**\n", badges.get(0)))
                                + (player.getData().getDescription() == null ? "No description set" : player.getData().getDescription()))
                        .addField(EmoteReference.DOLLAR + "Credits", "$ " + player.getMoney(), true)
                        .addField(EmoteReference.ZAP + "Level", player.getLevel() + " (Experience: " + player.getData().getExperience() +
                                ")", true)
                        .addField(EmoteReference.REP + "Reputation", String.valueOf(player.getReputation()), true)
                        .addField(EmoteReference.POPPER + "Birthday", user.getBirthday() != null ? user.getBirthday().substring(0, 5) :
                                "Not specified.", true)
                        .addField(EmoteReference.HEART + "Married with", marriedTo == null ? "Nobody." : marriedTo.getName() + "#" +
                                marriedTo.getDiscriminator(), false)
                        .addField(EmoteReference.POUCH + "Inventory", ItemStack.toString(inv.asList()), false)
                        .addField(EmoteReference.HEART + "Badges", displayBadges.isEmpty() ? "No badges (yet!)" : displayBadges, false)
                        .setFooter("User's timezone: " + (user.getTimezone() == null ? "No timezone set." : user.getTimezone()) + " | " +
                                "Requested by " + event.getAuthor().getName(), null));
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Profile command.")
                        .setDescription("**Retrieves your current user profile.**")
                        .addField("Usage", "To retrieve your profile, `~>profile`\n" +
                                "To change your description do `~>profile description set <description>`\n" +
                                "To clear it, just do `~>profile description clear`\n" +
                                "To set your timezone do `~>profile timezone <timezone>`\n" +
                                "**The profile only shows the 5 most important badges!**", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void badges(CommandRegistry cr) {
        final Random r = new Random();
        cr.register("badges", new SimpleCommand(Category.CURRENCY) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                Member member = Utils.findMember(event, event.getMember(), content);
                if(member == null) return;

                User toLookup = member.getUser();

                Player player = MantaroData.db().getPlayer(toLookup);
                PlayerData playerData = player.getData();

                List<Badge> badges = playerData.getBadges();
                Collections.sort(badges);
                AtomicInteger counter = new AtomicInteger();

                String toShow = (r.nextInt(5) == 0 ? "**You can get a free badge for " +
                        "[up-voting Mantaro on discordbots.org](https://discordbots.org/bot/mantaro)!** (It might take some minutes to process)\n" : "") +
                        badges.stream().map(badge -> String.format("**%d.-** %s\n*%4s*", counter.incrementAndGet(), badge, badge.description)
                ).collect(Collectors.joining("\n"));

                if(toShow.isEmpty()) toShow = "No badges to show (yet!)";
                List<String> parts = DiscordUtils.divideString(1500, toShow);
                DiscordUtils.list(event, 30, false, (current, max) -> new EmbedBuilder()
                        .setAuthor(toLookup.getName() + "'s badges", null, null)
                        .setColor(event.getMember().getColor() == null ? Color.PINK : event.getMember().getColor())
                        .setThumbnail(toLookup.getEffectiveAvatarUrl()), parts);
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Badge list")
                        .setDescription("**Shows your (or another person)'s badges**\n" +
                                "If you want to check out the badges of another person just mention them.")
                        .build();
            }
        });
    }

    private void applyBadge(MessageChannel channel, Badge badge, User author, EmbedBuilder builder) {
        if(badge == null) {
            channel.sendMessage(builder.build()).queue();
            return;
        }

        Message message = new MessageBuilder().setEmbed(builder.setThumbnail("attachment://avatar.png").build()).build();
        byte[] bytes;
        try {
            String url = author.getEffectiveAvatarUrl();

            if(url.endsWith(".gif")) {
                url = url.substring(0, url.length() - 3) + "png";
            }

            Response res = client.newCall(new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", MantaroInfo.USER_AGENT)
                    .build()
            ).execute();

            ResponseBody body = res.body();

            if(body == null)
                throw new IOException("body is null");

            bytes = body.bytes();
            res.close();
        } catch(IOException e) {
            throw new AssertionError("io error", e);
        }

        channel.sendFile(badge.apply(bytes), "avatar.png", message).queue();
    }
}
