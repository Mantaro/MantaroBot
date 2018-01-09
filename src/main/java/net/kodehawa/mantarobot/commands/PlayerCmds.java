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
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.ITreeCommand;
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
        ITreeCommand profileCommand = (TreeCommand) cr.register("profile", new TreeCommand(Category.CURRENCY) {
            @Override
            public Command defaultTrigger(GuildMessageReceivedEvent event, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(GuildMessageReceivedEvent event, String content) {
                        User author = event.getAuthor();
                        Player player = MantaroData.db().getPlayer(author);

                        UserData user = MantaroData.db().getUser(event.getMember()).getData();
                        Member member = event.getMember();

                        List<Member> found = FinderUtil.findMembers(content, event.getGuild());

                        if(found.isEmpty() && !content.isEmpty()) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "Didn't find any member with your search criteria :(").queue();
                            return;
                        }

                        if(found.size() > 1 && !content.isEmpty()) {
                            event.getChannel().sendMessage(EmoteReference.THINKING + "Too many members found, maybe refine your search? (ex. use name#discriminator)\n" +
                                    "**Members found:** " + found.stream().map(m -> m.getUser().getName() + "#" + m.getUser().getDiscriminator()).collect(Collectors.joining(", "))).queue();
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

                        User marriedTo = (player.getData().getMarriedWith() == null || player.getData().getMarriedWith().isEmpty()) ? null :
                                MantaroBot.getInstance().getUserById(player.getData().getMarriedWith());

                        PlayerData playerData = player.getData();
                        Inventory inv = player.getInventory();
                        boolean saveAfter = false;

                        //start of badge assigning
                        if(player.getMoney() > 7526527671L && playerData.addBadgeIfAbsent(Badge.ALTERNATIVE_WORLD))
                            saveAfter = true;
                        if(MantaroData.config().get().isOwner(author) && playerData.addBadgeIfAbsent(Badge.DEVELOPER))
                            saveAfter = true;
                        if(inv.asList().stream().anyMatch(stack -> stack.getAmount() == 5000) && playerData.addBadgeIfAbsent(Badge.SHOPPER))
                            saveAfter = true;
                        if(inv.asList().stream().anyMatch(stack -> stack.getItem().equals(Items.CHRISTMAS_TREE_SPECIAL) || stack.getItem().equals(Items.BELL_SPECIAL)) && playerData.addBadgeIfAbsent(Badge.CHRISTMAS))
                            saveAfter = true;
                        if(MantaroBot.getInstance().getShardedMantaro().getDiscordBotsUpvoters().contains(author.getIdLong()) && playerData.addBadgeIfAbsent(Badge.UPVOTER))
                            saveAfter = true;
                        if(player.getLevel() >= 10 && playerData.addBadgeIfAbsent(Badge.WALKER))
                            saveAfter = true;
                        if(player.getLevel() >= 50 && playerData.addBadgeIfAbsent(Badge.RUNNER))
                            saveAfter = true;
                        if(player.getLevel() >= 100 && playerData.addBadgeIfAbsent(Badge.FAST_RUNNER))
                            saveAfter = true;
                        if(player.getLevel() >= 150 && playerData.addBadgeIfAbsent(Badge.MARATHON_RUNNER))
                            saveAfter = true;
                        if(player.getLevel() >= 200 && playerData.addBadgeIfAbsent(Badge.MARATHON_WINNER))
                            saveAfter = true;
                        if(playerData.getMarketUsed() > 1000 && playerData.addBadgeIfAbsent(Badge.COMPULSIVE_BUYER))
                            saveAfter = true;

                        if(saveAfter)
                            player.saveAsync();
                        //end of badge assigning

                        List<Badge> badges = playerData.getBadges();
                        Collections.sort(badges);
                        String displayBadges = badges.stream().map(Badge::getUnicode).limit(5).collect(Collectors.joining("  "));

                        applyBadge(event.getChannel(),
                                badges.isEmpty() ? null : (playerData.getMainBadge() == null ? badges.get(0) : playerData.getMainBadge()), author,
                                baseEmbed(event, (marriedTo == null || !player.getInventory().containsItem(Items.RING) ? "" : EmoteReference.RING) + member.getEffectiveName() + "'s Profile", author.getEffectiveAvatarUrl())
                                .setThumbnail(author.getEffectiveAvatarUrl())
                                .setDescription((badges.isEmpty() ? "" : String.format("**%s**\n", (playerData.getMainBadge() == null ? badges.get(0) : playerData.getMainBadge())))
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
                };
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Profile command.")
                        .setDescription("**Retrieves your current user profile.**")
                        .addField("Usage", "- To retrieve your profile, `~>profile`\n" +
                                "- To change your description do `~>profile description set <description>`\n" +
                                "  -- To clear it, just do `~>profile description clear`\n" +
                                "- To set your timezone do `~>profile timezone <timezone>`\n" +
                                "- To set your display badge use `~>profile displaybadge` and `~>profile displaybadge reset` to reset it.\n" +
                                "**The profile only shows the 5 most important badges!.** Use `~>badges` to get a complete list.", false)
                        .build();
            }
        });


        profileCommand.addSubCommand("timezone", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content) {
                DBUser dbUser = MantaroData.db().getUser(event.getAuthor());
                String[] args = content.split(" ");

                if(args.length < 1) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You need to specify the timezone.").queue();
                    return;
                }

                String timezone = args[0];

                if(timezone.equalsIgnoreCase("reset")) {
                    dbUser.getData().setTimezone(null);
                    dbUser.saveAsync();
                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Reset timezone.").queue();
                    return;
                }

                if(!Utils.isValidTimeZone(timezone)) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "Invalid timezone.").queue();
                    return;
                }

                try {
                    UtilsCmds.dateGMT(event.getGuild(),timezone);
                } catch(Exception e) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "Not a valid timezone.").queue();
                    return;
                }

                dbUser.getData().setTimezone(timezone);
                dbUser.saveAsync();
                event.getChannel().sendMessage(String.format("%sSaved timezone, your profile timezone is now: **%s**", EmoteReference.CORRECT, timezone)).queue();
            }
        });

        profileCommand.addSubCommand("description", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content) {
                String[] args = content.split(" ");
                User author = event.getAuthor();
                Player player = MantaroData.db().getPlayer(author);

                if(args.length == 0) {
                    event.getChannel().sendMessage(EmoteReference.ERROR +
                            "You need to provide an argument! (set or remove)\n" +
                            "for example, ~>profile description set Hi there!").queue();
                    return;
                }

                if(args[0].equals("set")) {
                    int MAX_LENGTH = 300;

                    if(MantaroData.db().getUser(author).isPremium())
                        MAX_LENGTH = 500;

                    String content1 = SPLIT_PATTERN.split(content, 2)[1];

                    if(content1.length() > MAX_LENGTH) {
                        event.getChannel().sendMessage(EmoteReference.ERROR +
                                "The description is too long! `(Limit of 300 characters for everyone and 500 for premium users)`").queue();
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
                }
            }
        });

        profileCommand.addSubCommand("displaybadge", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content) {
                String[] args = content.split(" ");
                if(args.length == 0) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You need to specify your main badge!").queue();
                    return;
                }

                Player player = MantaroData.db().getPlayer(event.getAuthor());
                PlayerData data = player.getData();

                if(args[0].equalsIgnoreCase("reset")) {
                    data.setMainBadge(null);
                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Your display badge is now the most important one.").queue();
                    player.saveAsync();
                    return;
                }

                Badge badge = Badge.lookupFromString(content);

                if(badge == null) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "There's no such badge...\n" +
                            "Your available badges: " + player.getData().getBadges().stream().map(Badge::getDisplay).collect(Collectors.joining(", "))).queue();
                    return;
                }

                if(!data.getBadges().contains(badge)) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You don't have that badge.\n" +
                            "Your available badges: " + player.getData().getBadges().stream().map(Badge::getDisplay).collect(Collectors.joining(", "))).queue();
                    return;
                }

                data.setMainBadge(badge);
                player.saveAsync();
                event.getChannel().sendMessage(EmoteReference.CORRECT + "Your display badge is now: **" + badge.display + "**").queue();
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

                //Show the message that tells the person that they can get a free badge for upvoting mantaro one out of 3 times they use this command.
                //The message stops appearing when they upvote.
                String toShow = "If you think you got a new badge and it doesn't appear here, please use `~>profile` and then run this command again.\n" +
                        ((r.nextInt(3) == 0 && !MantaroBot.getInstance().getShardedMantaro().getDiscordBotsUpvoters().contains(event.getAuthor().getIdLong())) ?
                                "**You can get a free badge for [up-voting Mantaro on discordbots.org](https://discordbots.org/bot/mantaro)!**" +
                                        " (It might take some minutes to process)\n\n" : "") +
                        badges.stream().map(badge -> String.format("**%d.-** %s\n*%4s*", counter.incrementAndGet(), badge, badge.description)
                ).collect(Collectors.joining("\n"));

                if(toShow.isEmpty()) toShow = "No badges to show (yet!)";
                List<String> parts = DiscordUtils.divideString(MessageEmbed.TEXT_MAX_LENGTH, toShow);
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
