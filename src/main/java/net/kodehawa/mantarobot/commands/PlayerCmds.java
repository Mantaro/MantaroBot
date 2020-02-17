/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.commands.currency.item.*;
import net.kodehawa.mantarobot.commands.currency.item.special.Potion;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.Breakable;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.currency.profile.ProfileComponent;
import net.kodehawa.mantarobot.commands.currency.seasons.SeasonPlayer;
import net.kodehawa.mantarobot.commands.currency.seasons.helpers.SeasonalPlayerData;
import net.kodehawa.mantarobot.commands.currency.seasons.helpers.UnifiedPlayer;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.ITreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.I18n;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.PlayerStats;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import net.kodehawa.mantarobot.db.entities.helpers.PlayerData;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.IncreasingRateLimiter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.awt.*;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.commands.currency.profile.ProfileComponent.*;
import static net.kodehawa.mantarobot.utils.StringUtils.SPLIT_PATTERN;
import static net.kodehawa.mantarobot.utils.Utils.*;

@Module
@SuppressWarnings("unused")
public class PlayerCmds {
    private final OkHttpClient client = new OkHttpClient();

    @Subscribe
    public void rep(CommandRegistry cr) {
        cr.register("rep", new SimpleCommand(Category.CURRENCY) {
            final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                    .limit(1)
                    .cooldown(12, TimeUnit.HOURS)
                    .maxCooldown(12, TimeUnit.HOURS)
                    .pool(MantaroData.getDefaultJedisPool())
                    .randomIncrement(false)
                    .prefix("rep")
                    .build();

            @Override
            public void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                long rl = rateLimiter.getRemaniningCooldown(event.getAuthor());

                TextChannel channel = event.getChannel();
                User user;

                if (content.isEmpty()) {
                    channel.sendMessage(String.format(languageContext.get("commands.rep.no_mentions"), EmoteReference.ERROR,
                            (rl > 0 ? String.format(languageContext.get("commands.rep.cooldown.waiting"), Utils.getVerboseTime(rl)) : languageContext.get("commands.rep.cooldown.pass")))).queue();
                    return;
                }

                List<User> mentioned = event.getMessage().getMentionedUsers();
                if (!mentioned.isEmpty() && mentioned.size() > 1) {
                    channel.sendMessageFormat(languageContext.get("commands.rep.more_than_one"), EmoteReference.ERROR).queue();
                    return;
                }

                Member member = Utils.findMember(event, event.getMember(), content);
                if (member == null)
                    return;

                user = member.getUser();
                User author = event.getAuthor();
                Predicate<User> oldEnough = (u -> u.getTimeCreated().isBefore(OffsetDateTime.now().minus(5, ChronoUnit.DAYS)));

                //Didn't want to repeat the code twice, lol.
                if (!oldEnough.test(user)) {
                    channel.sendMessageFormat(languageContext.get("commands.rep.new_account_notice"), EmoteReference.ERROR).queue();
                    return;
                }

                if (!oldEnough.test(author)) {
                    channel.sendMessageFormat(languageContext.get("commands.rep.new_account_notice"), EmoteReference.ERROR).queue();
                    return;
                }

                if (user.isBot()) {
                    channel.sendMessage(String.format(languageContext.get("commands.rep.rep_bot"), EmoteReference.THINKING,
                            (rl > 0 ? String.format(languageContext.get("commands.rep.cooldown.waiting"), Utils.getVerboseTime(rl))
                                    : languageContext.get("commands.rep.cooldown.pass")))).queue();
                    return;
                }

                if (user.equals(event.getAuthor())) {
                    channel.sendMessage(String.format(languageContext.get("commands.rep.rep_yourself"), EmoteReference.THINKING,
                            (rl > 0 ? String.format(languageContext.get("commands.rep.cooldown.waiting"), Utils.getVerboseTime(rl))
                                    : languageContext.get("commands.rep.cooldown.pass")))).queue();
                    return;
                }

                //Check for RL.
                if (!handleDefaultIncreasingRatelimit(rateLimiter, event.getAuthor(), event, languageContext, false))
                    return;

                UnifiedPlayer player = UnifiedPlayer.of(user, getConfig().getCurrentSeason());
                player.addReputation(1L);
                player.save();

                new MessageBuilder().setContent(String.format(languageContext.get("commands.rep.success"), EmoteReference.CORRECT, member.getEffectiveName()))
                        .stripMentions(event.getGuild(), Message.MentionType.EVERYONE, Message.MentionType.HERE)
                        .sendTo(channel)
                        .queue();
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Gives 1 reputation to an user")
                        .setUsage("`~>rep <@user>` - Gives reputation to x user\n" +
                                "This command is only usable every 12 hours")
                        .addParameter("@user", "User to mention")
                        .build();
            }
        });

        cr.registerAlias("rep", "reputation");
    }

    @Subscribe
    public void profile(CommandRegistry cr) {
        final ManagedDatabase db = MantaroData.db();
        final ManagedDatabase managedDatabase = db;
        final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                .limit(2) //twice every 10m
                .spamTolerance(1)
                .cooldown(10, TimeUnit.MINUTES)
                .cooldownPenaltyIncrease(10, TimeUnit.SECONDS)
                .maxCooldown(15, TimeUnit.MINUTES)
                .pool(MantaroData.getDefaultJedisPool())
                .prefix("profile")
                .build();

        //I actually do need this, sob.
        final LinkedList<ProfileComponent> defaultOrder = createLinkedList(HEADER, CREDITS, LEVEL, REPUTATION, BIRTHDAY, MARRIAGE, INVENTORY, BADGES);

        ITreeCommand profileCommand = (TreeCommand) cr.register("profile", new TreeCommand(Category.CURRENCY) {

            @Override
            public Command defaultTrigger(GuildMessageReceivedEvent event, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                        TextChannel channel = event.getChannel();

                        Map<String, String> t = getArguments(content);
                        content = Utils.replaceArguments(t, content, "season", "s").trim();
                        boolean isSeasonal = t.containsKey("season") || t.containsKey("s");

                        User userLooked = event.getAuthor();
                        Player player = managedDatabase.getPlayer(userLooked);
                        SeasonPlayer seasonalPlayer = null;
                        DBUser dbUser = managedDatabase.getUser(userLooked);
                        Member memberLooked = event.getMember();

                        List<Member> found = FinderUtil.findMembers(content, event.getGuild());

                        if (found.isEmpty() && !content.isEmpty()) {
                            channel.sendMessageFormat(languageContext.get("general.find_members_failure"), EmoteReference.ERROR).queue();
                            return;
                        }

                        if (found.size() > 1 && !content.isEmpty()) {
                            channel.sendMessageFormat(languageContext.get("general.too_many_members"), EmoteReference.THINKING, found.stream().limit(7).map(m -> String.format("%s#%s", m.getUser().getName(), m.getUser().getDiscriminator())).collect(Collectors.joining(", "))).queue();
                            return;
                        }

                        if (found.size() == 1 && !content.isEmpty()) {
                            userLooked = found.get(0).getUser();
                            memberLooked = found.get(0);

                            if (userLooked.isBot()) {
                                channel.sendMessageFormat(languageContext.get("commands.profile.bot_notice"), EmoteReference.ERROR).queue();
                                return;
                            }

                            //Re-assign.
                            dbUser = managedDatabase.getUser(userLooked);
                            player = managedDatabase.getPlayer(memberLooked);
                        }

                        PlayerData playerData = player.getData();
                        UserData userData = dbUser.getData();
                        Inventory inv = player.getInventory();

                        //Cache waifu value.
                        playerData.setWaifuCachedValue(RelationshipCmds.calculateWaifuValue(userLooked).getFinalValue());

                        //start of badge assigning
                        Guild mh = MantaroBot.getInstance().getShardManager().getGuildById("213468583252983809");
                        Member mhMember = mh == null ? null : mh.getMemberById(memberLooked.getUser().getId());

                        //Badge assigning code
                        Badge.assignBadges(player, dbUser);

                        //Manual badges
                        if (MantaroData.config().get().isOwner(userLooked))
                            playerData.addBadgeIfAbsent(Badge.DEVELOPER);
                        if (inv.asList().stream().anyMatch(stack -> stack.getItem().equals(Items.CHRISTMAS_TREE_SPECIAL) || stack.getItem().equals(Items.BELL_SPECIAL)))
                            playerData.addBadgeIfAbsent(Badge.CHRISTMAS);
                        if (mhMember != null && mhMember.getRoles().stream().anyMatch(r -> r.getIdLong() == 406920476259123201L))
                            playerData.addBadgeIfAbsent(Badge.HELPER_2);
                        if (mhMember != null && mhMember.getRoles().stream().anyMatch(r -> r.getIdLong() == 290257037072531466L || r.getIdLong() == 290902183300431872L))
                            playerData.addBadgeIfAbsent(Badge.DONATOR_2);
                        //end of badge assigning

                        List<Badge> badges = playerData.getBadges();
                        Collections.sort(badges);

                        if (isSeasonal)
                            seasonalPlayer = managedDatabase.getPlayerForSeason(userLooked, getConfig().getCurrentSeason());

                        boolean ringHolder = player.getInventory().containsItem(Items.RING) && userData.getMarriage() != null;
                        ProfileComponent.Holder holder = new ProfileComponent.Holder(userLooked, player, seasonalPlayer, dbUser, badges);

                        EmbedBuilder profileBuilder = new EmbedBuilder();
                        profileBuilder.setAuthor((ringHolder ? EmoteReference.RING : "") +
                                String.format(languageContext.get("commands.profile.header"), memberLooked.getEffectiveName()), null, userLooked.getEffectiveAvatarUrl())
                                .setDescription(player.getData().getDescription() == null ? languageContext.get("commands.profile.no_desc") : player.getData().getDescription())
                                .setFooter(ProfileComponent.FOOTER.getContent().apply(holder, languageContext), null);

                        boolean hasCustomOrder = dbUser.isPremium() && !playerData.getProfileComponents().isEmpty();
                        List<ProfileComponent> usedOrder = hasCustomOrder ? playerData.getProfileComponents() : defaultOrder;

                        for (ProfileComponent component : usedOrder) {
                            profileBuilder.addField(component.getTitle(languageContext), component.getContent().apply(holder, languageContext), component.isInline());
                        }

                        applyBadge(channel,
                                badges.isEmpty() ? null : (playerData.getMainBadge() == null ? badges.get(0) : playerData.getMainBadge()), userLooked, profileBuilder
                        );

                        player.saveAsync();
                    }
                };
            }

            //If you wonder why is this so short compared to before, subcommand descriptions will do the trick on telling me what they do.
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Retrieves your current user profile.")
                        .setUsage("To retrieve your profile use `~>profile`. You can also use `~>profile @mention`\n" +
                                "*The profile command only shows the 5 most important badges.* Use `~>badges` to get a complete list!")
                        .addParameter("@mention", "A user mention (ping)")
                        .setSeasonal(true)
                        .build();
            }
        });

        profileCommand.addSubCommand("claimlock", new SubCommand() {
            @Override
            public String description() {
                return "Locks you from being waifu claimed. Needs a claim key to be in your inventory.";
            }

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                Player player = db.getPlayer(event.getAuthor());

                if (content.equals("remove")) {
                    player.getData().setClaimLocked(false);
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.claimlock.removed"), EmoteReference.CORRECT).queue();
                    player.save();
                    return;
                }

                Inventory inventory = player.getInventory();
                if (inventory.containsItem(Items.CLAIM_KEY)) {
                    player.getData().setClaimLocked(true);
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.claimlock.success"), EmoteReference.CORRECT).queue();
                    inventory.process(new ItemStack(Items.CLAIM_KEY, -1));
                    player.save();
                } else {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.claimlock.no_key"), EmoteReference.ERROR).queue();
                }
            }
        });

        //Hide tags from profile/waifu list.
        profileCommand.addSubCommand("hidetag", new SubCommand() {
            @Override
            public String description() {
                return "Hide the member tags (and IDs) from profile/waifu list. This is a switch.";
            }

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                DBUser user = db.getUser(event.getAuthor());
                UserData data = user.getData();

                data.setPrivateTag(!data.isPrivateTag());
                user.save();

                event.getChannel().sendMessageFormat(languageContext.get("commands.profile.hide_tag.success"), EmoteReference.POPPER, data.isPrivateTag()).queue();
            }
        });

        profileCommand.addSubCommand("equip", new SubCommand() {
            @Override
            public String description() {
                return "Equips an item in your inventory. Usage: `~>profile equip <item name>`. Use `-s` to equip it on your seasonal inventory.";
            }

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                TextChannel channel = event.getChannel();
                if (content.isEmpty()) {
                    channel.sendMessageFormat(languageContext.get("commands.profile.equip.no_content"), EmoteReference.ERROR).queue();
                    return;
                }

                Map<String, String> t = getArguments(content);
                boolean isSeasonal = t.containsKey("season") || t.containsKey("s");
                content = Utils.replaceArguments(t, content, "s", "season");

                Item item = Items.fromAnyNoId(content.replace("\"", "")).orElse(null);
                Player player = db.getPlayer(event.getAuthor());
                DBUser user = db.getUser(event.getAuthor());
                UserData data = user.getData();
                SeasonPlayer seasonalPlayer = db.getPlayerForSeason(event.getAuthor(), getConfig().getCurrentSeason());
                SeasonalPlayerData seasonalPlayerData = seasonalPlayer.getData();

                if (item == null) {
                    channel.sendMessageFormat(languageContext.get("commands.profile.equip.no_item"), EmoteReference.ERROR).queue();
                    return;
                }

                boolean containsItem = isSeasonal ? seasonalPlayer.getInventory().containsItem(item) : player.getInventory().containsItem(item);
                if (!containsItem) {
                    channel.sendMessageFormat(languageContext.get("commands.profile.equip.not_owned"), EmoteReference.ERROR).queue();
                    return;
                }

                PlayerEquipment equipment = isSeasonal ? seasonalPlayerData.getEquippedItems() : data.getEquippedItems();

                PlayerEquipment.EquipmentType proposedType = equipment.getTypeFor(item);
                if (equipment.getEquipment().containsKey(proposedType)) {
                    channel.sendMessageFormat(languageContext.get("commands.profile.equip.already_equipped"), EmoteReference.ERROR).queue();
                    return;
                }

                if (equipment.equipItem(item)) {
                    if (isSeasonal) {
                        seasonalPlayer.getInventory().process(new ItemStack(item, -1));
                        seasonalPlayer.save();
                    } else {
                        player.getInventory().process(new ItemStack(item, -1));
                        player.save();
                    }

                    user.save();

                    channel.sendMessageFormat(languageContext.get("commands.profile.equip.success"), EmoteReference.CORRECT, item.getEmoji(), item.getName()).queue();
                } else {
                    channel.sendMessageFormat(languageContext.get("commands.profile.equip.not_suitable"), EmoteReference.ERROR).queue();
                }
            }
        });

        profileCommand.addSubCommand("unequip", new SubCommand() {
            @Override
            public String description() {
                return "Unequips an equipped slot (pick/rod).";
            }

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                TextChannel channel = event.getChannel();

                if (content.isEmpty()) {
                    channel.sendMessageFormat(languageContext.get("commands.profile.unequip.no_content"), EmoteReference.ERROR).queue();
                    return;
                }

                Map<String, String> t = getArguments(content);
                boolean isSeasonal = t.containsKey("season") || t.containsKey("s");
                content = Utils.replaceArguments(t, content, "s", "season");

                DBUser user = db.getUser(event.getAuthor());
                UserData data = user.getData();
                SeasonPlayer seasonalPlayer = db.getPlayerForSeason(event.getAuthor(), getConfig().getCurrentSeason());
                SeasonalPlayerData seasonalPlayerData = seasonalPlayer.getData();

                PlayerEquipment equipment = isSeasonal ? seasonalPlayerData.getEquippedItems() : data.getEquippedItems();
                PlayerEquipment.EquipmentType type = PlayerEquipment.EquipmentType.fromString(content);
                if (type == null) {
                    channel.sendMessageFormat(languageContext.get("commands.profile.unequip.invalid_type"), EmoteReference.ERROR).queue();
                    return;
                }

                equipment.resetOfType(type);
                if (isSeasonal)
                    seasonalPlayer.save();
                else
                    user.save();

                channel.sendMessageFormat(languageContext.get("commands.profile.unequip.success"), EmoteReference.CORRECT, type.name().toLowerCase()).queue();
            }
        });

        profileCommand.addSubCommand("timezone", new SubCommand() {
            @Override
            public String description() {
                return "Sets your profile timezone. Usage: `~>profile timezone <timezone>`";
            }

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                TextChannel channel = event.getChannel();

                DBUser dbUser = managedDatabase.getUser(event.getAuthor());
                String[] args = content.split(" ");

                if (args.length < 1) {
                    channel.sendMessageFormat(languageContext.get("commands.profile.timezone.not_specified"), EmoteReference.ERROR).queue();
                    return;
                }

                String timezone = args[0].replace("UTC", "GMT").toUpperCase();

                if (timezone.equalsIgnoreCase("reset")) {
                    dbUser.getData().setTimezone(null);
                    dbUser.saveAsync();
                    channel.sendMessageFormat(languageContext.get("commands.profile.timezone.reset_success"), EmoteReference.CORRECT).queue();
                    return;
                }

                if (!Utils.isValidTimeZone(timezone)) {
                    channel.sendMessageFormat(languageContext.get("commands.profile.timezone.invalid"), EmoteReference.ERROR).queue();
                    return;
                }

                try {
                    UtilsCmds.dateGMT(event.getGuild(), timezone);
                } catch (Exception e) {
                    channel.sendMessageFormat(languageContext.get("commands.profile.timezone.invalid"), EmoteReference.ERROR).queue();
                    return;
                }

                dbUser.getData().setTimezone(timezone);
                dbUser.saveAsync();
                channel.sendMessage(String.format(languageContext.get("commands.profile.timezone.success"), EmoteReference.CORRECT, timezone)).queue();
            }
        });

        profileCommand.addSubCommand("description", new SubCommand() {
            @Override
            public String description() {
                return "Sets your profile description. Usage: `~>profile description set <description>`\n" +
                        "To reset it, you can use `~>profile description clear`";
            }

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                if (!Utils.handleDefaultIncreasingRatelimit(rateLimiter, event.getAuthor(), event, languageContext))
                    return;

                TextChannel channel = event.getChannel();

                String[] args = content.split(" ");
                User author = event.getAuthor();
                Player player = managedDatabase.getPlayer(author);

                if (args.length == 0) {
                    channel.sendMessageFormat(languageContext.get("commands.profile.description.no_argument"), EmoteReference.ERROR).queue();
                    return;
                }

                if (args[0].equals("set")) {
                    int MAX_LENGTH = 300;

                    if (managedDatabase.getUser(author).isPremium())
                        MAX_LENGTH = 500;

                    if (args.length < 2) {
                        channel.sendMessageFormat(languageContext.get("commands.profile.description.no_content"), EmoteReference.ERROR).queue();
                        return;
                    }

                    String content1 = SPLIT_PATTERN.split(content, 2)[1];

                    if (content1.length() > MAX_LENGTH) {
                        channel.sendMessageFormat(languageContext.get("commands.profile.description.too_long"), EmoteReference.ERROR).queue();
                        return;
                    }

                    content1 = Utils.DISCORD_INVITE.matcher(content1).replaceAll("-discord invite link-");
                    content1 = Utils.DISCORD_INVITE_2.matcher(content1).replaceAll("-discord invite link-");

                    player.getData().setDescription(content1);

                    new MessageBuilder().setContent(String.format(languageContext.get("commands.profile.description.success"), EmoteReference.POPPER, content1))
                            .stripMentions(event.getGuild(), Message.MentionType.HERE, Message.MentionType.EVERYONE, Message.MentionType.USER)
                            .sendTo(channel)
                            .queue();

                    player.getData().addBadgeIfAbsent(Badge.WRITER);
                    player.save();
                    return;
                }

                if (args[0].equals("clear")) {
                    player.getData().setDescription(null);
                    channel.sendMessageFormat(languageContext.get("commands.profile.description.clear_success"), EmoteReference.CORRECT).queue();
                    player.save();
                }
            }
        });

        profileCommand.addSubCommand("displaybadge", new SubCommand() {
            @Override
            public String description() {
                return "Sets your profile badge. Usage: `~>profile displaybadge <badge name>`\n" +
                        "To reset it use `~>profile displaybadge reset` and to show no badge use `~>profile displaybadge none`";
            }

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                TextChannel channel = event.getChannel();

                String[] args = content.split(" ");
                if (args.length == 0) {
                    channel.sendMessageFormat(languageContext.get("commands.profile.displaybadge.not_specified"), EmoteReference.ERROR).queue();
                    return;
                }

                Player player = managedDatabase.getPlayer(event.getAuthor());
                PlayerData data = player.getData();

                if (args[0].equalsIgnoreCase("none")) {
                    data.setShowBadge(false);
                    channel.sendMessageFormat(languageContext.get("commands.profile.displaybadge.reset_success"), EmoteReference.CORRECT).queue();
                    player.saveAsync();
                    return;
                }

                if (args[0].equalsIgnoreCase("reset")) {
                    data.setMainBadge(null);
                    data.setShowBadge(true);
                    channel.sendMessageFormat(languageContext.get("commands.profile.displaybadge.important_success"), EmoteReference.CORRECT).queue();
                    player.saveAsync();
                    return;
                }

                Badge badge = Badge.lookupFromString(content);

                if (badge == null) {
                    channel.sendMessageFormat(languageContext.get("commands.profile.displaybadge.no_such_badge"), EmoteReference.ERROR, player.getData().getBadges().stream().map(Badge::getDisplay).collect(Collectors.joining(", "))).queue();
                    return;
                }

                if (!data.getBadges().contains(badge)) {
                    channel.sendMessageFormat(languageContext.get("commands.profile.displaybadge.player_missing_badge"), EmoteReference.ERROR, player.getData().getBadges().stream().map(Badge::getDisplay).collect(Collectors.joining(", "))).queue();
                    return;
                }

                data.setShowBadge(true);
                data.setMainBadge(badge);
                player.saveAsync();
                channel.sendMessageFormat(languageContext.get("commands.profile.displaybadge.success"), EmoteReference.CORRECT, badge.display).queue();
            }
        });

        profileCommand.addSubCommand("lang", new SubCommand() {
            @Override
            public String description() {
                return "Sets your profile language. Usage: `~>profile lang <language id>`. You can check a list of avaliable languages using `~>lang`";
            }

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                TextChannel channel = event.getChannel();

                if (content.isEmpty()) {
                    channel.sendMessageFormat(languageContext.get("commands.profile.lang.nothing_specified"), EmoteReference.ERROR).queue();
                    return;
                }

                DBUser dbUser = managedDatabase.getUser(event.getAuthor());

                if (content.equalsIgnoreCase("reset")) {
                    dbUser.getData().setLang(null);
                    dbUser.save();
                    channel.sendMessageFormat(languageContext.get("commands.profile.lang.reset_success"), EmoteReference.CORRECT).queue();
                    return;
                }

                if (I18n.isValidLanguage(content)) {
                    dbUser.getData().setLang(content);
                    //Create new I18n context based on the new language choice.
                    I18nContext newContext = new I18nContext(managedDatabase.getGuild(event.getGuild().getId()).getData(), dbUser.getData());

                    dbUser.save();
                    channel.sendMessageFormat(newContext.get("commands.profile.lang.success"), EmoteReference.CORRECT, content).queue();
                } else {
                    channel.sendMessageFormat(languageContext.get("commands.profile.lang.invalid"), EmoteReference.ERROR).queue();
                }
            }
        });

        profileCommand.addSubCommand("stats", new SubCommand() {
            @Override
            public String description() {
                return "Checks your profile stats or the stats of other players. Usage: `~>profile stats [@mention]`";
            }

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext ctx, String content) {
                TextChannel channel = event.getChannel();
                Member member = Utils.findMember(event, event.getMember(), content);

                if (member == null)
                    return;

                User toLookup = member.getUser();

                Player player = managedDatabase.getPlayer(toLookup);
                DBUser dbUser = managedDatabase.getUser(toLookup);
                UserData data = dbUser.getData();
                PlayerData playerData = player.getData();
                PlayerStats playerStats = managedDatabase.getPlayerStats(toLookup);
                SeasonPlayer seasonPlayer = managedDatabase.getPlayerForSeason(toLookup, getConfig().getCurrentSeason());

                PlayerEquipment equippedItems = data.getEquippedItems();
                PlayerEquipment seasonalEquippedItems = seasonPlayer.getData().getEquippedItems();

                Potion potion = (Potion) equippedItems.getEffectItem(PlayerEquipment.EquipmentType.POTION);
                Potion buff = (Potion) equippedItems.getEffectItem(PlayerEquipment.EquipmentType.BUFF);
                PotionEffect potionEffect = equippedItems.getCurrentEffect(PlayerEquipment.EquipmentType.POTION);
                PotionEffect buffEffect = equippedItems.getCurrentEffect(PlayerEquipment.EquipmentType.BUFF);

                boolean isPotionActive = potion != null && (equippedItems.isEffectActive(PlayerEquipment.EquipmentType.POTION, potion.getMaxUses()) || potionEffect.getAmountEquipped() > 1);
                boolean isBuffActive = buff != null && (equippedItems.isEffectActive(PlayerEquipment.EquipmentType.BUFF, buff.getMaxUses()) || buffEffect.getAmountEquipped() > 1);

                long potionEquipped = 0;
                long buffEquipped = 0;

                if (potion != null)
                    potionEquipped = equippedItems.isEffectActive(PlayerEquipment.EquipmentType.POTION, potion.getMaxUses()) ? potionEffect.getAmountEquipped() : potionEffect.getAmountEquipped() - 1;
                if (buff != null)
                    buffEquipped = equippedItems.isEffectActive(PlayerEquipment.EquipmentType.BUFF, buff.getMaxUses()) ? buffEffect.getAmountEquipped() : buffEffect.getAmountEquipped() - 1;

                //no need for decimals
                long experienceNext = (long) (player.getLevel() * Math.log10(player.getLevel()) * 1000) + (50 * player.getLevel() / 2);
                boolean noPotion = potion == null || !isPotionActive;
                boolean noBuff = buff == null || !isBuffActive;

                String equipment = parsePlayerEquipment(equippedItems);
                String seasonalEquipment = parsePlayerEquipment(seasonalEquippedItems);

                //This whole thing is a massive mess lmfao.
                String s = String.join("\n",
                        prettyDisplay(ctx.get("commands.profile.stats.market"), playerData.getMarketUsed() + " " + ctx.get("commands.profile.stats.times")),

                        //Potion display
                        prettyDisplay(ctx.get("commands.profile.stats.potion"), noPotion ? "None" : String.format("%s (%dx)", potion.getName(), potionEquipped)),
                        "\u3000 " +
                                EmoteReference.BOOSTER + ctx.get("commands.profile.stats.times_used") + ": " +
                                (noPotion ? "Not equipped" : potionEffect.getTimesUsed() + " " + ctx.get("commands.profile.stats.times")),
                        prettyDisplay(ctx.get("commands.profile.stats.buff"), noBuff ? "None" : String.format("%s (%dx)", buff.getName(), buffEquipped)),
                        "\u3000 " +
                                EmoteReference.BOOSTER + ctx.get("commands.profile.stats.times_used") + ": " +
                                (noBuff ? "Not equipped" : buffEffect.getTimesUsed() + " " + ctx.get("commands.profile.stats.times")),
                        //End of potion display

                        prettyDisplayLine(ctx.get("commands.profile.stats.equipment"), equipment),
                        prettyDisplayLine(ctx.get("commands.profile.stats.seasonal_equipment"), seasonalEquipment),
                        prettyDisplay(ctx.get("commands.profile.stats.experience"), playerData.getExperience() + "/" + experienceNext + " XP"),
                        prettyDisplay(ctx.get("commands.profile.stats.daily"), playerData.getDailyStreak() + " " + ctx.get("commands.profile.stats.days")),
                        prettyDisplay(ctx.get("commands.profile.stats.daily_at"), new Date(playerData.getLastDailyAt()).toString()),
                        prettyDisplay(ctx.get("commands.profile.stats.waifu_claimed"), data.getTimesClaimed() + " " + ctx.get("commands.profile.stats.times")),
                        prettyDisplay(ctx.get("commands.profile.stats.dust"), data.getDustLevel() + "%"),
                        prettyDisplay(ctx.get("commands.profile.stats.reminders"), data.getReminderN() + " " + ctx.get("commands.profile.stats.times")),
                        prettyDisplay(ctx.get("commands.profile.stats.lang"), (data.getLang() == null ? "en_US" : data.getLang())),
                        prettyDisplay(ctx.get("commands.profile.stats.wins"),
                                String.format("\n\u3000\u2009\u2009\u2009\u2009" +
                                        "%1$sGamble: %2$d, Slots: %3$d, Game: %4$d (times)", EmoteReference.CREDITCARD, playerStats.getGambleWins(), playerStats.getSlotsWins(), playerData.getGamesWon()))
                );


                channel.sendMessage(new EmbedBuilder()
                        .setThumbnail(toLookup.getEffectiveAvatarUrl())
                        .setAuthor(String.format(ctx.get("commands.profile.stats.header"), toLookup.getName()), null, toLookup.getEffectiveAvatarUrl())
                        .setDescription("\n" + s)
                        .setFooter("This shows stuff usually not shown on the profile card. Content might change", null)
                        .build()
                ).queue();
            }
        });

        profileCommand.addSubCommand("widgets", new SubCommand() {
            @Override
            public String description() {
                return "Sets the profile widget order. Usage: `~>profile widgets <name of widget>` or `~>profile widgets <ls/reset>`";
            }

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                TextChannel channel = event.getChannel();

                DBUser user = managedDatabase.getUser(event.getAuthor());
                if (!user.isPremium()) {
                    channel.sendMessageFormat(languageContext.get("commands.profile.display.not_premium"), EmoteReference.ERROR).queue();
                    return;
                }

                Player player = managedDatabase.getPlayer(event.getAuthor());
                PlayerData data = player.getData();

                if (content.equalsIgnoreCase("ls") || content.equalsIgnoreCase("is")) {
                    channel.sendMessageFormat(languageContext.get("commands.profile.display.ls") + languageContext.get("commands.profile.display.example"), EmoteReference.ZAP,
                            EmoteReference.BLUE_SMALL_MARKER, defaultOrder.stream().map(Enum::name).collect(Collectors.joining(", ")),
                            data.getProfileComponents().size() == 0 ? "Not personalized" : data.getProfileComponents().stream().map(Enum::name).collect(Collectors.joining(", "))
                    ).queue();
                    return;
                }

                if (content.equalsIgnoreCase("reset")) {
                    data.getProfileComponents().clear();
                    player.saveAsync();

                    channel.sendMessageFormat(languageContext.get("commands.profile.display.reset"), EmoteReference.CORRECT).queue();
                    return;
                }

                String[] splitContent = content.replace(",", "").split("\\s+");
                List<ProfileComponent> newComponents = new LinkedList<>(); //new list of profile components

                for (String c : splitContent) {
                    ProfileComponent component = ProfileComponent.lookupFromString(c);
                    if (component != null && component.isAssignable()) {
                        newComponents.add(component);
                    }
                }

                if (newComponents.size() < 3) {
                    channel.sendMessageFormat(languageContext.get("commands.profile.display.not_enough") + languageContext.get("commands.profile.display.example"), EmoteReference.WARNING).queue();
                    return;
                }

                data.setProfileComponents(newComponents);
                player.saveAsync();

                channel.sendMessageFormat(languageContext.get("commands.profile.display.success"),
                        EmoteReference.CORRECT, newComponents.stream().map(Enum::name).collect(Collectors.joining(", "))
                ).queue();
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
                        TextChannel channel = event.getChannel();

                        Map<String, String> t = getArguments(content);
                        content = Utils.replaceArguments(t, content, "brief");
                        Member member = Utils.findMember(event, event.getMember(), content);
                        if (member == null) return;

                        User toLookup = member.getUser();

                        Player player = MantaroData.db().getPlayer(toLookup);
                        PlayerData playerData = player.getData();

                        if (!t.isEmpty() && t.containsKey("brief")) {
                            new MessageBuilder().setContent(String.format(languageContext.get("commands.badges.brief_success"), member.getEffectiveName(),
                                    playerData.getBadges().stream().map(b -> "*" + b.display + "*").collect(Collectors.joining(", "))))
                                    .stripMentions(event.getGuild(), Message.MentionType.EVERYONE, Message.MentionType.HERE)
                                    .sendTo(channel)
                                    .queue();
                            return;
                        }

                        List<Badge> badges = playerData.getBadges();
                        Collections.sort(badges);

                        EmbedBuilder embed = new EmbedBuilder()
                                .setAuthor(String.format(languageContext.get("commands.badges.header"), toLookup.getName()))
                                .setColor(event.getMember().getColor() == null ? Color.PINK : event.getMember().getColor())
                                .setThumbnail(toLookup.getEffectiveAvatarUrl());
                        List<MessageEmbed.Field> fields = new LinkedList<>();

                        for (Badge b : badges) {
                            //God DAMNIT discord, I want it to look cute, stop trimming my spaces.
                            fields.add(new MessageEmbed.Field(b.toString(), "**\u2009\u2009\u2009\u2009- " + b.description + "**", false));
                        }

                        if (badges.isEmpty()) {
                            embed.setDescription(languageContext.get("commands.badges.no_badges"));
                            channel.sendMessage(embed.build()).queue();
                            return;
                        }

                        List<List<MessageEmbed.Field>> splitFields = DiscordUtils.divideFields(6, fields);
                        boolean hasReactionPerms = event.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_ADD_REACTION);

                        embed.setFooter(languageContext.get("commands.badges.footer"), null);

                        String common = languageContext.get("commands.badges.profile_notice") + languageContext.get("commands.badges.info_notice") +
                                ((r.nextInt(3) == 0 && !playerData.hasBadge(Badge.UPVOTER) ? languageContext.get("commands.badges.upvote_notice") : "\n")) +
                                ((r.nextInt(2) == 0 ? languageContext.get("commands.badges.donate_notice") : "\n") +
                                        String.format(languageContext.get("commands.badges.total_badges"), badges.size()) + "\n");
                        if (hasReactionPerms) {
                            embed.setDescription(languageContext.get("general.arrow_react") + "\n" + common);
                            DiscordUtils.list(event, 60, false, embed, splitFields);
                        } else {
                            embed.setDescription(languageContext.get("general.text_menu") + "\n" + common);
                            DiscordUtils.listText(event, 60, false, embed, splitFields);
                        }
                    }
                };
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Shows your (or another person)'s badges.")
                        .setUsage("If you want to check out the badges of another person just mention them.\n" +
                                "You can use `~>badges -brief` to get a brief versions of the badge showcase.")
                        .build();
            }

        });

        badgeCommand.addSubCommand("info", new SubCommand() {
            @Override
            public String description() {
                return "Shows info about a badge. Usage: `~>badges info <name>`";
            }

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                TextChannel channel = event.getChannel();

                if (content.isEmpty()) {
                    channel.sendMessageFormat(languageContext.get("commands.badges.info.not_specified"), EmoteReference.ERROR).queue();
                    return;
                }

                Badge badge = Badge.lookupFromString(content);
                //shouldn't NPE bc null check is done first, in order
                if (badge == null || badge == Badge.DJ) {
                    channel.sendMessageFormat(languageContext.get("commands.badges.info.not_found"), EmoteReference.ERROR).queue();
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

                channel.sendMessage(message).addFile(badge.icon, "icon.png").queue();
            }
        });
    }

    private void applyBadge(MessageChannel channel, Badge badge, User author, EmbedBuilder builder) {
        if (badge == null) {
            channel.sendMessage(builder.build()).queue();
            return;
        }

        Message message = new MessageBuilder().setEmbed(builder.setThumbnail("attachment://avatar.png").build()).build();
        byte[] bytes;
        try {
            String url = author.getEffectiveAvatarUrl();

            if (url.endsWith(".gif")) {
                url = url.substring(0, url.length() - 3) + "png";
            }

            Response res = client.newCall(new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", MantaroInfo.USER_AGENT)
                    .build()
            ).execute();

            ResponseBody body = res.body();

            if (body == null)
                throw new IOException("body is null");

            bytes = body.bytes();
            res.close();
        } catch (IOException e) {
            throw new AssertionError("io error", e);
        }

        channel.sendMessage(message).addFile(badge.apply(bytes), "avatar.png").queue();
    }

    public String parsePlayerEquipment(PlayerEquipment equipment) {
        Map<PlayerEquipment.EquipmentType, Integer> toolsEquipment = equipment.getEquipment();

        if (toolsEquipment.isEmpty()) {
            return "None";
        }

        return toolsEquipment.entrySet().stream().map((entry) -> {
            Item item = Items.fromId(entry.getValue());

            return "- " +
                    Utils.capitalize(entry.getKey().toString()) + ": " +
                    item.toDisplayString() +
                    " [" + equipment.getDurability().get(entry.getKey()) + " / " + ((Breakable) item).getMaxDurability() + "]";
        }).collect(Collectors.joining("\n"));
    }
}
