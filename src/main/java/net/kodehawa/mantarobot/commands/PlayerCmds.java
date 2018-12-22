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
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.commands.currency.item.PlayerEquipment;
import net.kodehawa.mantarobot.commands.currency.item.special.Potion;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.currency.profile.ProfileComponent;
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
            final RateLimiter rateLimiter = new RateLimiter(TimeUnit.HOURS, 12);

            @Override
            public void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                long rl = rateLimiter.tryAgainIn(event.getMember());
                User user;

                if(content.isEmpty()) {
                    event.getChannel().sendMessage(String.format(languageContext.get("commands.rep.no_mentions"), EmoteReference.ERROR,
                            (rl > 0 ?  String.format(languageContext.get("commands.rep.cooldown.waiting"), Utils.getVerboseTime(rateLimiter.tryAgainIn(event.getMember())))
                             : languageContext.get("commands.rep.cooldown.pass")))).queue();
                    return;
                }

                List<User> mentioned = event.getMessage().getMentionedUsers();
                if(!mentioned.isEmpty() && mentioned.size() > 1) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.rep.more_than_one"), EmoteReference.ERROR).queue();
                    return;
                }

                Member member = Utils.findMember(event, event.getMember(), content);
                if(member == null)
                    return;

                user = member.getUser();

                if(user.isBot()) {
                    event.getChannel().sendMessage(String.format(languageContext.get("commands.rep.rep_bot"), EmoteReference.THINKING,
                            (rl > 0 ? String.format(languageContext.get("commands.rep.cooldown.waiting"), Utils.getVerboseTime(rateLimiter.tryAgainIn(event.getMember())))
                             : languageContext.get("commands.rep.cooldown.pass")))).queue();
                    return;
                }

                if(user.equals(event.getAuthor())) {
                    event.getChannel().sendMessage(String.format(languageContext.get("commands.rep.rep_yourself"), EmoteReference.THINKING,
                            (rl > 0 ?  String.format(languageContext.get("commands.rep.cooldown.waiting"), Utils.getVerboseTime(rateLimiter.tryAgainIn(event.getMember())))
                             : languageContext.get("commands.rep.cooldown.pass")))).queue();
                    return;
                }

                //Check for RL.
                if(!handleDefaultRatelimit(rateLimiter, event.getAuthor(), event, languageContext))
                    return;

                Player player = MantaroData.db().getPlayer(user);
                player.addReputation(1L);
                player.save();
                new MessageBuilder().setContent(String.format(languageContext.get("commands.rep.success"), EmoteReference.CORRECT,  member.getEffectiveName()))
                        .stripMentions(event.getGuild(), Message.MentionType.EVERYONE, Message.MentionType.HERE)
                        .sendTo(event.getChannel())
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
        final ManagedDatabase managedDatabase = MantaroData.db();
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
                        User userLooked = event.getAuthor();
                        Player player = managedDatabase.getPlayer(userLooked);
                        DBUser dbUser = managedDatabase.getUser(userLooked);
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
                        Guild mh = MantaroBot.getInstance().getGuildById("213468583252983809");
                        Member mhMember = mh == null ? null : mh.getMemberById(memberLooked.getUser().getId());

                        //Badge assigning code
                        Badge.assignBadges(player, dbUser);

                        //Manual badges
                        if(MantaroData.config().get().isOwner(userLooked))
                            playerData.addBadgeIfAbsent(Badge.DEVELOPER);
                        if(inv.asList().stream().anyMatch(stack -> stack.getItem().equals(Items.CHRISTMAS_TREE_SPECIAL) || stack.getItem().equals(Items.BELL_SPECIAL)))
                            playerData.addBadgeIfAbsent(Badge.CHRISTMAS);
                        if(mhMember != null && mhMember.getRoles().stream().anyMatch(r -> r.getIdLong() == 406920476259123201L))
                            playerData.addBadgeIfAbsent(Badge.HELPER_2);
                        if(mhMember != null && mhMember.getRoles().stream().anyMatch(r -> r.getIdLong() == 290257037072531466L || r.getIdLong() == 290902183300431872L))
                            playerData.addBadgeIfAbsent(Badge.DONATOR_2);
                        //end of badge assigning

                        List<Badge> badges = playerData.getBadges();
                        Collections.sort(badges);

                        boolean ringHolder = player.getInventory().containsItem(Items.RING) && userData.getMarriage() != null;
                        ProfileComponent.Holder holder = new ProfileComponent.Holder(userLooked, player, dbUser, badges);

                        EmbedBuilder profileBuilder = new EmbedBuilder();
                        profileBuilder.setAuthor((ringHolder ? "" : EmoteReference.RING) +
                                    String.format(languageContext.get("commands.profile.header"), memberLooked.getEffectiveName()), null, userLooked.getEffectiveAvatarUrl())
                                .setDescription(player.getData().getDescription() == null ? languageContext.get("commands.profile.no_desc") : player.getData().getDescription())
                                .setFooter(ProfileComponent.FOOTER.getContent().apply(holder, languageContext), null);

                        boolean hasCustomOrder = dbUser.isPremium() && !playerData.getProfileComponents().isEmpty();
                        List<ProfileComponent> usedOrder = hasCustomOrder ? playerData.getProfileComponents() : defaultOrder;

                        for(ProfileComponent component : usedOrder) {
                            profileBuilder.addField(component.getTitle(languageContext), component.getContent().apply(holder, languageContext), component.isInline());
                        }

                        applyBadge(event.getChannel(),
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
                        .build();
            }
        });


        profileCommand.addSubCommand("equip", new SubCommand() {
            @Override
            public String description() {
                return "Equips an item in your inventory. Usage: `~>profile equip <item name>`";
            }

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                if(content.isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.equip.no_content"), EmoteReference.ERROR).queue();
                    return;
                }

                Item item = Items.fromAnyNoId(content).orElse(null);
                Player player = MantaroData.db().getPlayer(event.getAuthor());
                DBUser user = MantaroData.db().getUser(event.getAuthor());

                if(item == null) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.equip.no_item"), EmoteReference.ERROR).queue();
                    return;
                }

                if(!player.getInventory().containsItem(item)) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.equip.not_owned"), EmoteReference.ERROR).queue();
                    return;
                }

                if(user.getData().getEquippedItems().equipItem(item)) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.equip.success"), EmoteReference.CORRECT, item.getEmoji(), item.getName()).queue();
                    user.save();
                } else {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.equip.not_suitable"), EmoteReference.ERROR).queue();
                }
            }
        });

        profileCommand.addSubCommand("timezone", new SubCommand() {
            @Override
            public String description() {
                return "Sets your profile timezone. Usage: `~>profile timezone <timezone>`";
            }

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                DBUser dbUser = managedDatabase.getUser(event.getAuthor());
                String[] args = content.split(" ");

                if(args.length < 1) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.timezone.not_specified"), EmoteReference.ERROR).queue();
                    return;
                }

                String timezone = args[0].replace("UTC", "GMT").toUpperCase();

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
                    UtilsCmds.dateGMT(event.getGuild(), timezone);
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
            public String description() {
                return "Sets your profile description. Usage: `~>profile description set <description>`\n" +
                        "To reset it, you can use `~>profile description clear`";
            }

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                if(!Utils.handleDefaultIncreasingRatelimit(rateLimiter, event.getAuthor(), event, languageContext))
                    return;

                String[] args = content.split(" ");
                User author = event.getAuthor();
                Player player = managedDatabase.getPlayer(author);

                if(args.length == 0) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.description.no_argument"), EmoteReference.ERROR).queue();
                    return;
                }

                if(args[0].equals("set")) {
                    int MAX_LENGTH = 300;

                    if(managedDatabase.getUser(author).isPremium())
                        MAX_LENGTH = 500;

                    String content1 = SPLIT_PATTERN.split(content, 2)[1];

                    if(content1.length() > MAX_LENGTH) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.profile.description.too_long"), EmoteReference.ERROR).queue();
                        return;
                    }

                    content1 = Utils.DISCORD_INVITE.matcher(content1).replaceAll("-discord invite link-");
                    content1 = Utils.DISCORD_INVITE_2.matcher(content1).replaceAll("-discord invite link-");

                    player.getData().setDescription(content1);

                    new MessageBuilder().setContent(String.format(languageContext.get("commands.profile.description.success"), EmoteReference.POPPER, content1))
                            .stripMentions(event.getGuild(), Message.MentionType.HERE, Message.MentionType.EVERYONE, Message.MentionType.USER)
                            .sendTo(event.getChannel())
                            .queue();

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
            public String description() {
                return "Sets your profile badge. Usage: `~>profile displaybadge <badge name>`\n" +
                        "To reset it use `~>profile displaybadge reset` and to show no badge use `~>profile displaybadge none`";
            }

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                String[] args = content.split(" ");
                if(args.length == 0) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.displaybadge.not_specified"), EmoteReference.ERROR).queue();
                    return;
                }

                Player player = managedDatabase.getPlayer(event.getAuthor());
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
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.displaybadge.important_success"), EmoteReference.CORRECT).queue();
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
            public String description() {
                return "Sets your profile language. Usage: `~>profile lang <language id>`. You can check a list of avaliable languages using `~>lang`";
            }

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                if(content.isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.lang.nothing_specified"), EmoteReference.ERROR).queue();
                    return;
                }

                DBUser dbUser = managedDatabase.getUser(event.getAuthor());

                if(content.equalsIgnoreCase("reset")) {
                    dbUser.getData().setLang(null);
                    dbUser.save();
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.lang.reset_success"), EmoteReference.CORRECT).queue();
                    return;
                }

                if(I18n.isValidLanguage(content)) {
                    dbUser.getData().setLang(content);
                    //Create new I18n context based on the new language choice.
                    I18nContext newContext = new I18nContext(managedDatabase.getGuild(event.getGuild().getId()).getData(), dbUser.getData());

                    dbUser.save();
                    event.getChannel().sendMessageFormat(newContext.get("commands.profile.lang.success"), EmoteReference.CORRECT, content).queue();
                } else {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.lang.invalid"), EmoteReference.ERROR).queue();
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
                Map<String, Optional<String>> t = StringUtils.parse(content.isEmpty() ? new String[]{} : content.split("\\s+"));
                content = Utils.replaceArguments(t, content, "brief");
                Member member = Utils.findMember(event, event.getMember(), content);
                if(member == null) return;

                User toLookup = member.getUser();

                Player player = managedDatabase.getPlayer(toLookup);
                DBUser dbUser = managedDatabase.getUser(toLookup);
                UserData data = dbUser.getData();
                PlayerData playerData = player.getData();
                PlayerStats playerStats = managedDatabase.getPlayerStats(toLookup);

                PlayerEquipment equippedItems = data.getEquippedItems();
                Potion potion = (Potion) equippedItems.getEffectItem(PlayerEquipment.EquipmentType.POTION);
                Potion buff = (Potion) equippedItems.getEffectItem(PlayerEquipment.EquipmentType.BUFF);
                boolean isPotionActive = potion != null && equippedItems.isEffectActive(PlayerEquipment.EquipmentType.POTION, potion.getMaxUses());
                boolean isBuffActive = buff != null && equippedItems.isEffectActive(PlayerEquipment.EquipmentType.BUFF, buff.getMaxUses());
                boolean equipmentEmpty = equippedItems.getEquipment().isEmpty();

                //no need for decimals
                long experienceNext = (long) (player.getLevel() * Math.log10(player.getLevel()) * 1000) + (50 * player.getLevel() / 2);
                boolean noPotion = potion == null || !isPotionActive;
                boolean noBuff = buff == null || !isBuffActive;

                String s = String.join("\n",
                        prettyDisplay(ctx.get("commands.profile.stats.market"), playerData.getMarketUsed() + " " + ctx.get("commands.profile.stats.times")),

                        //Potion display
                        prettyDisplay(ctx.get("commands.profile.stats.potion"), noPotion ? "None" : potion.getName()),
                        "\u2009\u2009\u2009\u2009\u2009\u2009\u2009\u2009" +
                                ctx.get("commands.profile.stats.times_used") +
                                (noPotion ? "Never" : equippedItems.getCurrentEffect(PlayerEquipment.EquipmentType.POTION).getTimesUsed() + " " + ctx.get("commands.profile.stats.times"))),
                        prettyDisplay(ctx.get("commands.profile.stats.buff"), noBuff ? "None" : buff.getName()),
                        "\u2009\u2009\u2009\u2009\u2009\u2009\u2009\u2009" +
                                ctx.get("commands.profile.stats.times_used") +
                                (noBuff ? "None" : equippedItems.getCurrentEffect(PlayerEquipment.EquipmentType.BUFF).getTimesUsed()  + " " + ctx.get("commands.profile.stats.times")),
                        //End of potion display

                        prettyDisplay(ctx.get("commands.profile.stats.equipment"), ((equipmentEmpty) ? "None" :
                                equippedItems.getEquipment().entrySet().stream().map((entry) -> Utils.capitalize(entry.getKey().toString()) + ": " +
                                        Items.fromId(entry.getValue()).toDisplayString()).collect(Collectors.joining(", ")))),
                        prettyDisplay(ctx.get("commands.profile.stats.experience"), playerData.getExperience() + "/" + experienceNext + " XP"),
                        prettyDisplay(ctx.get("commands.profile.stats.daily"), playerData.getDailyStreak() + " " + ctx.get("commands.profile.stats.days")),
                        prettyDisplay(ctx.get("commands.profile.stats.daily_at"), new Date(playerData.getLastDailyAt()).toString()),
                        prettyDisplay(ctx.get("commands.profile.stats.waifu_claimed"), data.getTimesClaimed() + " " + ctx.get("commands.profile.stats.times")),
                        prettyDisplay(ctx.get("commands.profile.stats.dust"), data.getDustLevel() + "%"),
                        prettyDisplay(ctx.get("commands.profile.stats.reminders"), data.getReminderN() + " " + ctx.get("commands.profile.stats.times")),
                        prettyDisplay(ctx.get("commands.profile.stats.lang"), (data.getLang() == null ? "en_US" : data.getLang())),
                        prettyDisplay(ctx.get("commands.profile.stats.wins"),
                                String.format("\n\u2009\u2009\u2009\u2009\u2009\u2009\u2009\u2009\u2009\u2009\u2009" +
                                        "%1$sGamble: %2$d, Slots: %3$d, Game: %4$d (times)", EmoteReference.CREDITCARD, playerStats.getGambleWins(), playerStats.getSlotsWins(), playerData.getGamesWon()))
                        );


                event.getChannel().sendMessage(new EmbedBuilder()
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
                DBUser user = managedDatabase.getUser(event.getAuthor());
                if(!user.isPremium()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.display.not_premium"), EmoteReference.ERROR).queue();
                    return;
                }

                Player player = managedDatabase.getPlayer(event.getAuthor());
                PlayerData data = player.getData();

                if(content.equalsIgnoreCase("ls") || content.equalsIgnoreCase("is")) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.display.ls") + languageContext.get("commands.profile.display.example"), EmoteReference.ZAP,
                            EmoteReference.BLUE_SMALL_MARKER, defaultOrder.stream().map(Enum::name).collect(Collectors.joining(", ")),
                            data.getProfileComponents().size() == 0 ? "Not personalized" : data.getProfileComponents().stream().map(Enum::name).collect(Collectors.joining(", "))
                    ).queue();
                    return;
                }

                if(content.equalsIgnoreCase("reset")) {
                    data.getProfileComponents().clear();
                    player.saveAsync();

                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.display.reset"), EmoteReference.CORRECT).queue();
                    return;
                }

                String[] splitContent = content.replace(",", "").split("\\s+");
                List<ProfileComponent> newComponents = new LinkedList<>(); //new list of profile components

                for(String c : splitContent) {
                    ProfileComponent component = ProfileComponent.lookupFromString(c);
                    if(component != null && component.isAssignable()) {
                        newComponents.add(component);
                    }
                }

                if(newComponents.size() < 3) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.profile.display.not_enough") + languageContext.get("commands.profile.display.example"), EmoteReference.WARNING).queue();
                    return;
                }

                data.setProfileComponents(newComponents);
                player.saveAsync();

                event.getChannel().sendMessageFormat(languageContext.get("commands.profile.display.success"),
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
                        Map<String, Optional<String>> t = StringUtils.parse(content.isEmpty() ? new String[]{} : content.split("\\s+"));
                        content = Utils.replaceArguments(t, content, "brief");
                        Member member = Utils.findMember(event, event.getMember(), content);
                        if(member == null) return;

                        User toLookup = member.getUser();

                        Player player = MantaroData.db().getPlayer(toLookup);
                        PlayerData playerData = player.getData();

                        if(!t.isEmpty() && t.containsKey("brief")) {
                            new MessageBuilder().setContent(String.format(languageContext.get("commands.badges.brief_success"), member.getEffectiveName(),
                                        playerData.getBadges().stream().map(b -> "*" + b.display + "*").collect(Collectors.joining(", "))))
                                    .stripMentions(event.getGuild(), Message.MentionType.EVERYONE, Message.MentionType.HERE)
                                    .sendTo(event.getChannel())
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

                        for(Badge b : badges) {
                            //God DAMNIT discord, I want it to look cute, stop trimming my spaces.
                            fields.add(new MessageEmbed.Field(b.toString(), "**\u2009\u2009\u2009\u2009- " + b.description + "**", false));
                        }

                        if(badges.isEmpty()) {
                            embed.setDescription(languageContext.get("commands.badges.no_badges"));
                            event.getChannel().sendMessage(embed.build()).queue();
                            return;
                        }

                        List<List<MessageEmbed.Field>> splitFields = DiscordUtils.divideFields(6, fields);
                        boolean hasReactionPerms = event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_ADD_REACTION);

                        embed.setFooter(languageContext.get("commands.badges.footer"), null);

                        String common = languageContext.get("commands.badges.profile_notice") + languageContext.get("commands.badges.info_notice") +
                                ((r.nextInt(3) == 0 && !playerData.hasBadge(Badge.UPVOTER) ? languageContext.get("commands.badges.upvote_notice") : "\n")) +
                                ((r.nextInt(2) == 0 ? languageContext.get("commands.badges.donate_notice") : "\n") +
                                        String.format(languageContext.get("commands.badges.total_badges"), badges.size()) + "\n");
                        if(hasReactionPerms) {
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
                if(content.isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.badges.info.not_specified"), EmoteReference.ERROR).queue();
                    return;
                }

                Badge badge = Badge.lookupFromString(content);
                //shouldn't NPE bc null check is done first, in order
                if(badge == null || badge == Badge.DJ) {
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
