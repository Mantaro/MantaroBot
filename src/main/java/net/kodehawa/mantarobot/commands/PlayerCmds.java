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
import com.jagrosh.jdautilities.utils.FinderUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.commands.currency.item.Item;
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
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.I18n;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Marriage;
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
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.utils.StringUtils.SPLIT_PATTERN;
import static net.kodehawa.mantarobot.utils.Utils.handleDefaultRatelimit;

@Module
@SuppressWarnings("unused")
public class PlayerCmds {
    private final OkHttpClient client = new OkHttpClient();

    @Subscribe
    public void rep(CommandRegistry cr) {
        cr.register("rep", new SimpleCommand(Category.CURRENCY) {
            final RateLimiter rateLimiter = new RateLimiter(TimeUnit.HOURS, 12);

            @Override
            public void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                long rl = rateLimiter.tryAgainIn(event.getMember());
                User user;

                if(content.isEmpty()) {
                    event.getChannel().sendMessage(String.format(languageContext.get("commands.rep.no_mentions"), EmoteReference.ERROR,
                            (rl > 0 ?  String.format(languageContext.get("commands.rep.cooldown.wait"), Utils.getVerboseTime(rateLimiter.tryAgainIn(event.getMember())))
                             : languageContext.get("commands.rep.cooldown.pass")))).queue();
                    return;
                }

                List<User> mentioned = event.getMessage().getMentionedUsers();
                if(!mentioned.isEmpty() && mentioned.size() > 1) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.rep.more_than_one"), EmoteReference.ERROR).queue();
                    return;
                }

                Member member = Utils.findMember(event, event.getMember(), content);
                if(member == null) return;

                user = member.getUser();

                if(user.isBot()) {
                    event.getChannel().sendMessage(String.format(languageContext.get("commands.rep.rep_bot"), EmoteReference.THINKING,
                            (rl > 0 ? String.format(languageContext.get("commands.rep.cooldown.wait"), Utils.getVerboseTime(rateLimiter.tryAgainIn(event.getMember())))
                             : languageContext.get("commands.rep.cooldown.pass")))).queue();
                    return;
                }

                if(user.equals(event.getAuthor())) {
                    event.getChannel().sendMessage(String.format(languageContext.get("commands.rep.rep_yourself"), EmoteReference.THINKING,
                            (rl > 0 ?  String.format(languageContext.get("commands.rep.cooldown.wait"), Utils.getVerboseTime(rateLimiter.tryAgainIn(event.getMember())))
                             : languageContext.get("commands.rep.cooldown.pass")))).queue();
                    return;
                }

                if(!handleDefaultRatelimit(rateLimiter, event.getAuthor(), event)) return;
                Player player = MantaroData.db().getPlayer(user);
                player.addReputation(1L);
                player.save();
                event.getChannel().sendMessageFormat(languageContext.get("commands.rep.success"), EmoteReference.CORRECT,  member.getEffectiveName()).queue();
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
                    protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                        User userLooked = event.getAuthor();
                        final ManagedDatabase db = MantaroData.db();
                        Player player = db.getPlayer(userLooked);

                        UserData user = db.getUser(event.getMember()).getData();
                        Member memberLooked = event.getMember();

                        List<Member> found = FinderUtil.findMembers(content, event.getGuild());

                        if(found.isEmpty() && !content.isEmpty()) {
                            event.getChannel().sendMessageFormat(languageContext.get("general.find_members_failure"), EmoteReference.ERROR).queue();
                            return;
                        }

                        if(found.size() > 1 && !content.isEmpty()) {
                            event.getChannel().sendMessageFormat(languageContext.get("general.too_many_members"), EmoteReference.THINKING, found.stream().map(m -> String.format("%s#%s", m.getUser().getName(), m.getUser().getDiscriminator())).collect(Collectors.joining(", "))).queue();
                            return;
                        }

                        if(found.size() == 1 && !content.isEmpty()) {
                            userLooked = found.get(0).getUser();
                            memberLooked = found.get(0);

                            if(userLooked.isBot()) {
                                event.getChannel().sendMessageFormat(languageContext.get("commands.profile.bot_notice"), EmoteReference.ERROR).queue();
                                return;
                            }

                            user = db.getUser(userLooked).getData();
                            player = db.getPlayer(memberLooked);
                        }

                        //LEGACY SUPPORT
                        User marriedTo = (player.getData().getMarriedWith() == null || player.getData().getMarriedWith().isEmpty()) ? null : MantaroBot.getInstance().getUserById(player.getData().getMarriedWith());
                        //New marriage support.
                        Marriage currentMarriage = db.getUser(event.getAuthor()).getData().getMarriage();
                        User marriedToNew = null;
                        boolean isNewMarriage = false;
                        if(currentMarriage != null) {
                            String marriedToId = currentMarriage.getOtherPlayer(memberLooked.getUser().getId());
                            if(marriedToId != null) {
                                marriedToNew = MantaroBot.getInstance().getUserById(marriedToId);
                                isNewMarriage = true;
                            }
                        }

                        PlayerData playerData = player.getData();
                        Inventory inv = player.getInventory();

                        //start of badge assigning
                        Guild mh = MantaroBot.getInstance().getGuildById("213468583252983809");
                        Member mhMember = mh == null ? null : mh.getMemberById(memberLooked.getUser().getId());
                        boolean saveAfter = false;

                        if(player.getMoney() > 7526527671L && playerData.addBadgeIfAbsent(Badge.ALTERNATIVE_WORLD))
                            saveAfter = true;
                        if(MantaroData.config().get().isOwner(userLooked) && playerData.addBadgeIfAbsent(Badge.DEVELOPER))
                            saveAfter = true;
                        if(inv.asList().stream().anyMatch(stack -> stack.getAmount() == 5000) && playerData.addBadgeIfAbsent(Badge.SHOPPER))
                            saveAfter = true;
                        if(inv.asList().stream().anyMatch(stack -> stack.getItem().equals(Items.CHRISTMAS_TREE_SPECIAL) || stack.getItem().equals(Items.BELL_SPECIAL)) && playerData.addBadgeIfAbsent(Badge.CHRISTMAS))
                            saveAfter = true;
                        if(MantaroBot.getInstance().getShardedMantaro().getDiscordBotsUpvoters().contains(userLooked.getIdLong()) && playerData.addBadgeIfAbsent(Badge.UPVOTER))
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
                        if(player.getReputation() >= 10 && playerData.addBadgeIfAbsent(Badge.POPULAR))
                            saveAfter = true;
                        if(player.getReputation() >= 100 && playerData.addBadgeIfAbsent(Badge.CELEBRITY))
                            saveAfter = true;
                        if(player.getReputation() >= 1000 && playerData.addBadgeIfAbsent(Badge.MOST_KNOWN))
                            saveAfter = true;
                        if(playerData.getMarketUsed() > 1000 && playerData.addBadgeIfAbsent(Badge.COMPULSIVE_BUYER))
                            saveAfter = true;
                        if(mhMember != null && mhMember.getRoles().stream().anyMatch(r -> r.getIdLong() == 406920476259123201L) && playerData.addBadgeIfAbsent(Badge.HELPER_2))
                            saveAfter = true;
                        if(mhMember != null && mhMember.getRoles().stream().anyMatch(r -> r.getIdLong() == 290257037072531466L || r.getIdLong() == 290902183300431872L) && playerData.addBadgeIfAbsent(Badge.DONATOR_2))
                            saveAfter = true;


                        if(saveAfter)
                            player.saveAsync();
                        //end of badge assigning

                        List<Badge> badges = playerData.getBadges();
                        Collections.sort(badges);
                        String displayBadges = badges.stream().map(Badge::getUnicode).limit(5).collect(Collectors.joining("  "));

                        EmbedBuilder builder = baseEmbed(event,
                                (marriedTo == null || !player.getInventory().containsItem(Items.RING) ? "" : EmoteReference.RING
                                ) + String.format(languageContext.get("commands.profile.header"), memberLooked.getEffectiveName()), userLooked.getEffectiveAvatarUrl()
                                )
                                .setThumbnail(userLooked.getEffectiveAvatarUrl())
                                .setDescription(
                                        (player.getData().isShowBadge() ? (badges.isEmpty() ?
                                                "" : String.format("**%s**\n", (playerData.getMainBadge() == null ? badges.get(0) : playerData.getMainBadge()))) : ""
                                        ) +
                                                (player.getData().getDescription() == null ?
                                                        languageContext.get("commands.profile.no_desc") : player.getData().getDescription()
                                                )
                                )
                                .addField(EmoteReference.DOLLAR + languageContext.get("commands.profile.credits"),
                                        "$ " + player.getMoney(), true
                                )
                                .addField(EmoteReference.ZAP + languageContext.get("commands.profile.level"),
                                        String.format("%d (%s: %d)", player.getLevel(), languageContext.get("commands.profile.xp"), player.getData().getExperience()), true
                                )
                                .addField(EmoteReference.REP + languageContext.get("commands.profile.rep"),
                                        String.valueOf(player.getReputation()), true
                                )
                                .addField(EmoteReference.POPPER + languageContext.get("commands.profile.birthday"),
                                        user.getBirthday() != null ? user.getBirthday().substring(0, 5) : languageContext.get("commands.profile.not_specified"), true
                                )
                                //VERY readable stuff. God fuck I need to rewrite the profile command SOME day.
                                .addField(EmoteReference.HEART + languageContext.get("commands.profile.married"), (marriedTo == null && marriedToNew == null) ?
                                        languageContext.get("commands.profile.nobody") :
                                        isNewMarriage ? String.format("%s#%s", marriedToNew.getName(), marriedToNew.getDiscriminator()) :
                                                String.format("%s#%s", marriedTo.getName(), marriedTo.getDiscriminator()), false

                                )
                                .addField(EmoteReference.POUCH + languageContext.get("commands.profile.inventory"),
                                        ItemStack.toString(inv.asList()), false
                                )
                                .addField(EmoteReference.HEART + languageContext.get("commands.profile.badges"),
                                        displayBadges.isEmpty() ? languageContext.get("commands.profile.no_badges") : displayBadges, false
                                )
                                .setFooter(String.format("%s | %s", String.format(languageContext.get("commands.profile.timezone_user"),
                                        (user.getTimezone() == null ? languageContext.get("commands.profile.no_timezone") : user.getTimezone())), String.format(languageContext.get("general.requested_by"), event.getAuthor().getName())), null
                                );


                        if(player.getData().getActivePotion() != null) {
                            Item potion = Items.fromId(player.getData().getActivePotion().getPotion());
                            builder.addField(EmoteReference.RUNNER + languageContext.get("commands.profile.potion"),
                                   potion.getEmoji() + " " + potion.getName(), false);
                        }


                        applyBadge(event.getChannel(),
                                badges.isEmpty() ? null : (playerData.getMainBadge() == null ? badges.get(0) : playerData.getMainBadge()), userLooked,
                                builder
                        );
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
                                "  -- You can also use `~>profile displaybadge none` to display no badge on your profile.\n" +
                                "**The profile only shows the 5 most important badges!.** Use `~>badges` to get a complete list.", false)
                        .build();
            }
        });


        profileCommand.addSubCommand("timezone", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                DBUser dbUser = MantaroData.db().getUser(event.getAuthor());
                String[] args = content.split(" ");

                if(args.length < 1) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.timezone.not_specified"), EmoteReference.ERROR).queue();
                    return;
                }

                String timezone = args[0];

                if(timezone.equalsIgnoreCase("reset")) {
                    dbUser.getData().setTimezone(null);
                    dbUser.saveAsync();
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.timezone.reset_success"), EmoteReference.CORRECT).queue();
                    return;
                }

                if(!Utils.isValidTimeZone(timezone)) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.timezone.invalid"), EmoteReference.ERROR).queue();
                    return;
                }

                try {
                    UtilsCmds.dateGMT(event.getGuild(),timezone);
                } catch(Exception e) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.timezone.invalid"), EmoteReference.ERROR).queue();
                    return;
                }

                dbUser.getData().setTimezone(timezone);
                dbUser.saveAsync();
                event.getChannel().sendMessage(String.format(languageContext.get("commands.profile.timezone.success"), EmoteReference.CORRECT, timezone)).queue();
            }
        });

        profileCommand.addSubCommand("description", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                String[] args = content.split(" ");
                User author = event.getAuthor();
                Player player = MantaroData.db().getPlayer(author);

                if(args.length == 0) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.description.no_argument"), EmoteReference.ERROR).queue();
                    return;
                }

                if(args[0].equals("set")) {
                    int MAX_LENGTH = 300;

                    if(MantaroData.db().getUser(author).isPremium())
                        MAX_LENGTH = 500;

                    String content1 = SPLIT_PATTERN.split(content, 2)[1];

                    if(content1.length() > MAX_LENGTH) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.profile.description.too_long"), EmoteReference.ERROR).queue();
                        return;
                    }

                    player.getData().setDescription(content1);
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.description.success"), EmoteReference.POPPER, content1).queue();
                    player.getData().addBadgeIfAbsent(Badge.WRITER);
                    player.save();
                    return;
                }

                if(args[0].equals("clear")) {
                    player.getData().setDescription(null);
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.description.clear_success"), EmoteReference.CORRECT).queue();
                    player.save();
                }
            }
        });

        profileCommand.addSubCommand("displaybadge", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                String[] args = content.split(" ");
                if(args.length == 0) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.displaybadge.not_specified"), EmoteReference.ERROR).queue();
                    return;
                }

                Player player = MantaroData.db().getPlayer(event.getAuthor());
                PlayerData data = player.getData();

                if(args[0].equalsIgnoreCase("none")) {
                    data.setShowBadge(false);
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.displaybadge.reset_success"), EmoteReference.CORRECT).queue();
                    player.saveAsync();
                    return;
                }

                if(args[0].equalsIgnoreCase("reset")) {
                    data.setMainBadge(null);
                    data.setShowBadge(true);
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.displaybadge.important_sucess"), EmoteReference.CORRECT).queue();
                    player.saveAsync();
                    return;
                }

                Badge badge = Badge.lookupFromString(content);

                if(badge == null) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.displaybadge.no_such_badge"), EmoteReference.ERROR, player.getData().getBadges().stream().map(Badge::getDisplay).collect(Collectors.joining(", "))).queue();
                    return;
                }

                if(!data.getBadges().contains(badge)) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.displaybadge.player_missing_badge"), EmoteReference.ERROR, player.getData().getBadges().stream().map(Badge::getDisplay).collect(Collectors.joining(", "))).queue();
                    return;
                }

                data.setShowBadge(true);
                data.setMainBadge(badge);
                player.saveAsync();
                event.getChannel().sendMessageFormat(languageContext.get("commands.profile.displaybadge.success"), EmoteReference.CORRECT, badge.display).queue();
            }
        });

        profileCommand.addSubCommand("lang", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                if(content.isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.lang.nothing_specified"), EmoteReference.ERROR).queue();
                    return;
                }

                DBUser dbUser = MantaroData.db().getUser(event.getAuthor());

                if(content.equalsIgnoreCase("reset")) {
                    dbUser.getData().setLang(null);
                    dbUser.save();
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.lang.reset_success"), EmoteReference.CORRECT).queue();
                    return;
                }

                if(I18n.isValidLanguage(content)) {
                    dbUser.getData().setLang(content);
                    dbUser.save();
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.lang.success"), EmoteReference.CORRECT, content).queue();
                } else {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.lang.invalid"), EmoteReference.ERROR).queue();
                }
            }
        });
    }

    @Subscribe
    public void badges(CommandRegistry cr) {
        final Random r = new Random();
        ITreeCommand badgeCommand = (ITreeCommand) cr.register("badges", new TreeCommand(Category.CURRENCY) {
            @Override
            public Command defaultTrigger(GuildMessageReceivedEvent event, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                        Map<String, Optional<String>> t = StringUtils.parse(content.isEmpty() ? new String[]{} : content.split("\\s+"));
                        content = Utils.replaceArguments(t, content, "brief");
                        Member member = Utils.findMember(event, event.getMember(), content);
                        if(member == null) return;

                        User toLookup = member.getUser();

                        Player player = MantaroData.db().getPlayer(toLookup);
                        PlayerData playerData = player.getData();

                        if(!t.isEmpty() && t.containsKey("brief")) {
                            event.getChannel().sendMessageFormat(
                                    languageContext.get("commands.badges.brief_success"), member.getEffectiveName(),
                                    playerData.getBadges().stream().map(b -> "*" + b.display + "*").collect(Collectors.joining(", "))
                            ).queue();

                            return;
                        }

                        List<Badge> badges = playerData.getBadges();
                        Collections.sort(badges);

                        //Show the message that tells the person that they can get a free badge for upvoting mantaro one out of 3 times they use this command.
                        //The message stops appearing when they upvote.
                        String toShow = languageContext.get("commands.badges.profile_notice") + languageContext.get("commands.badges.info_notice") +
                                ((r.nextInt(3) == 0 && !playerData.hasBadge(Badge.UPVOTER) ? languageContext.get("commands.badges.upvote_notice") : "\n"))
                                + badges.stream().map(badge -> String.format("**%s:** *%s*", badge, badge.description)).collect(Collectors.joining("\n"));

                        if(toShow.isEmpty()) toShow = languageContext.get("commands.badges.no_badges");
                        List<String> parts = DiscordUtils.divideString(MessageEmbed.TEXT_MAX_LENGTH, toShow);
                        DiscordUtils.list(event, 30, false, (current, max) -> new EmbedBuilder()
                                .setAuthor(String.format(languageContext.get("commands.badges.header"), toLookup.getName()))
                                .setColor(event.getMember().getColor() == null ? Color.PINK : event.getMember().getColor())
                                .setThumbnail(toLookup.getEffectiveAvatarUrl()), parts);
                    }
                };
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Badge list")
                        .setDescription("**Shows your (or another person)'s badges**\n" +
                                "If you want to check out the badges of another person just mention them.\n" +
                                "`~>badges info <name>` - Shows info about a badge.\n" +
                                "You can use `~>badges -brief` to get a brief versions of the badge showcase.")
                        .build();
            }
        });

        badgeCommand.addSubCommand("info", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                if(content.isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.badges.info.not_specified"), EmoteReference.ERROR).queue();
                    return;
                }

                Badge badge = Badge.lookupFromString(content);
                if(badge == null) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.badges.info.not_found"), EmoteReference.ERROR).queue();
                    return;
                }

                Player p = MantaroData.db().getPlayer(event.getAuthor());
                Message message = new MessageBuilder().setEmbed(new EmbedBuilder()
                        .setAuthor(String.format(languageContext.get("commands.badges.info.header"), badge.display))
                        .setDescription(String.join("\n",
                                EmoteReference.BLUE_SMALL_MARKER + "**" + languageContext.get("general.name") + ":** " + badge.display,
                                EmoteReference.BLUE_SMALL_MARKER + "**" + languageContext.get("general.description") + ":** " + badge.description,
                                EmoteReference.BLUE_SMALL_MARKER + "**" + languageContext.get("commands.badges.info.achieved") + ":** " + p.getData().getBadges().stream().anyMatch(b -> b == badge))
                        )
                        .setThumbnail("attachment://icon.png")
                        .setColor(Color.CYAN)
                        .build()
                ).build();

                event.getChannel().sendFile(badge.icon, "icon.png", message).queue();
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
