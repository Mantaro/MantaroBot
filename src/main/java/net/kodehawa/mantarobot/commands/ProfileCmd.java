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
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.MantaroInfo;
import net.kodehawa.mantarobot.commands.currency.item.*;
import net.kodehawa.mantarobot.commands.currency.item.special.Potion;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.Breakable;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.currency.profile.ProfileComponent;
import net.kodehawa.mantarobot.commands.currency.seasons.SeasonPlayer;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.base.ITreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.I18n;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.PlayerStats;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import net.kodehawa.mantarobot.db.entities.helpers.PlayerData;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.IncreasingRateLimiter;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.commands.currency.profile.ProfileComponent.*;
import static net.kodehawa.mantarobot.utils.StringUtils.SPLIT_PATTERN;
import static net.kodehawa.mantarobot.utils.Utils.*;

@Module
public class ProfileCmd {
    @Subscribe
    public void profile(CommandRegistry cr) {
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
            public Command defaultTrigger(Context ctx, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, String content) {
                        Map<String, String> t = ctx.getOptionalArguments();
                        content = Utils.replaceArguments(t, content, "season", "s").trim();
                        boolean isSeasonal = ctx.isSeasonal();

                        User userLooked = ctx.getAuthor();
                        Member memberLooked = ctx.getMember();

                        Player player = ctx.getPlayer();
                        SeasonPlayer seasonalPlayer = null;
                        DBUser dbUser = ctx.getDBUser();

                        if(!content.isEmpty()) {
                            List<Member> found = FinderUtil.findMembers(content, ctx.getGuild());
                            if(found != null) {
                                userLooked = found.get(0).getUser();
                                memberLooked = found.get(0);

                                if (userLooked.isBot()) {
                                    ctx.sendLocalized("commands.profile.bot_notice", EmoteReference.ERROR);
                                    return;
                                }

                                //Re-assign.
                                dbUser = ctx.getDBUser(userLooked);
                                player = ctx.getPlayer(userLooked);
                            }
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
                        if (inv.asList().stream()
                                .anyMatch(stack -> stack.getItem().equals(Items.CHRISTMAS_TREE_SPECIAL) ||
                                        stack.getItem().equals(Items.BELL_SPECIAL)))
                            playerData.addBadgeIfAbsent(Badge.CHRISTMAS);
                        if (mhMember != null &&
                                mhMember.getRoles().stream().anyMatch(r -> r.getIdLong() == 406920476259123201L))
                            playerData.addBadgeIfAbsent(Badge.HELPER_2);
                        if (mhMember != null &&
                                mhMember.getRoles().stream().anyMatch(r -> r.getIdLong() == 290257037072531466L ||
                                        r.getIdLong() == 290902183300431872L))
                            playerData.addBadgeIfAbsent(Badge.DONATOR_2);
                        //end of badge assigning

                        List<Badge> badges = playerData.getBadges();
                        Collections.sort(badges);

                        if (isSeasonal)
                            seasonalPlayer = ctx.getSeasonPlayer(userLooked);

                        boolean ringHolder = player.getInventory().containsItem(Items.RING) && userData.getMarriage() != null;
                        ProfileComponent.Holder holder = new ProfileComponent.Holder(userLooked, player, seasonalPlayer, dbUser, badges);
                        I18nContext languageContext = ctx.getLanguageContext();

                        EmbedBuilder profileBuilder = new EmbedBuilder();
                        profileBuilder.setAuthor((ringHolder ? EmoteReference.RING : "") +
                                String.format(languageContext.get("commands.profile.header"),
                                        memberLooked.getEffectiveName()), null, userLooked.getEffectiveAvatarUrl()
                                ).setDescription(player.getData().getDescription() == null ?
                                        languageContext.get("commands.profile.no_desc") : player.getData().getDescription()
                                ).setFooter(ProfileComponent.FOOTER.getContent().apply(holder, languageContext), null);

                        boolean hasCustomOrder = dbUser.isPremium() && !playerData.getProfileComponents().isEmpty();
                        List<ProfileComponent> usedOrder = hasCustomOrder ? playerData.getProfileComponents() : defaultOrder;

                        for (ProfileComponent component : usedOrder) {
                            profileBuilder.addField(
                                    component.getTitle(languageContext), component.getContent().apply(holder, languageContext), component.isInline()
                            );
                        }

                        applyBadge(ctx.getChannel(),
                                badges.isEmpty() ? null :
                                        (playerData.getMainBadge() == null ? badges.get(0) : playerData.getMainBadge()),
                                userLooked, profileBuilder
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
                return "Locks you from being waifu claimed. Needs a claim key. Use `remove` to remove it.";
            }

            @Override
            protected void call(Context ctx, String content) {
                Player player = ctx.getPlayer();

                if (content.equals("remove")) {
                    player.getData().setClaimLocked(false);
                    ctx.sendLocalized("commands.profile.claimlock.removed", EmoteReference.CORRECT);
                    player.save();
                    return;
                }

                Inventory inventory = player.getInventory();
                if(!inventory.containsItem(Items.CLAIM_KEY)) {
                    ctx.sendLocalized("commands.profile.claimlock.no_key", EmoteReference.ERROR);
                    return;
                }

                player.getData().setClaimLocked(true);
                ctx.sendLocalized("commands.profile.claimlock.success", EmoteReference.CORRECT);
                inventory.process(new ItemStack(Items.CLAIM_KEY, -1));
                player.save();
            }
        });

        profileCommand.addSubCommand("autoequip", new SubCommand() {
            @Override
            public String description() {
                return "Sets whether you want or not to autoequip a new tool on break. Use `disable` to disable it.";
            }

            @Override
            protected void call(Context ctx, String content) {
                DBUser user = ctx.getDBUser();
                UserData data = user.getData();

                if (content.equals("disable")) {
                    data.setAutoEquip(false);
                    ctx.sendLocalized("commands.profile.autoequip.disable", EmoteReference.CORRECT);
                    user.save();
                    return;
                }

                data.setAutoEquip(true);
                ctx.sendLocalized("commands.profile.autoequip.success", EmoteReference.CORRECT);
                user.save();
            }
        });

        //Hide tags from profile/waifu list.
        profileCommand.addSubCommand("hidetag", new SubCommand() {
            @Override
            public String description() {
                return "Hide the member tags (and IDs) from profile/waifu ls. This is a switch.";
            }

            @Override
            protected void call(Context ctx, String content) {
                DBUser user = ctx.getDBUser();
                UserData data = user.getData();

                data.setPrivateTag(!data.isPrivateTag());
                user.save();

                ctx.sendLocalized("commands.profile.hide_tag.success", EmoteReference.POPPER, data.isPrivateTag());
            }
        });

        profileCommand.addSubCommand("timezone", new SubCommand() {
            @Override
            public String description() {
                return "Sets the profile timezone. Usage: `~>profile timezone <tz>`";
            }

            @Override
            protected void call(Context ctx, String content) {
                DBUser dbUser = ctx.getDBUser();
                String[] args = ctx.getArguments();

                if (args.length < 1) {
                    ctx.sendLocalized("commands.profile.timezone.not_specified", EmoteReference.ERROR);
                    return;
                }

                String timezone = args[0].replace("UTC", "GMT").toUpperCase();

                if (timezone.equalsIgnoreCase("reset")) {
                    dbUser.getData().setTimezone(null);
                    dbUser.saveAsync();
                    ctx.sendLocalized("commands.profile.timezone.reset_success", EmoteReference.CORRECT);
                    return;
                }

                if (!Utils.isValidTimeZone(timezone)) {
                    ctx.sendLocalized("commands.profile.timezone.invalid", EmoteReference.ERROR);
                    return;
                }

                try {
                    UtilsCmds.dateGMT(ctx.getGuild(), timezone);
                } catch (Exception e) {
                    ctx.sendLocalized("commands.profile.timezone.invalid", EmoteReference.ERROR);
                    return;
                }

                Player player = ctx.getPlayer();
                if(player.getData().addBadgeIfAbsent(Badge.CALENDAR))
                    player.save();

                dbUser.getData().setTimezone(timezone);
                dbUser.saveAsync();
                ctx.sendLocalized("commands.profile.timezone.success", EmoteReference.CORRECT, timezone);
            }
        });

        profileCommand.addSubCommand("description", new SubCommand() {
            @Override
            public String description() {
                return "Sets your profile description. Usage: `~>profile description set <description>`\n" +
                        "Reset with `~>profile description clear`";
            }

            @Override
            protected void call(Context ctx, String content) {
                if (!Utils.handleIncreasingRatelimit(rateLimiter, ctx.getAuthor(), ctx))
                    return;

                String[] args = content.split(" ");
                User author = ctx.getAuthor();
                Player player = ctx.getPlayer();
                DBUser dbUser = ctx.getDBUser();

                if (args.length == 0) {
                    ctx.sendLocalized("commands.profile.description.no_argument", EmoteReference.ERROR);
                    return;
                }

                if (args[0].equals("set")) {
                    int MAX_LENGTH = 300;

                    if (dbUser.isPremium())
                        MAX_LENGTH = 500;

                    if (args.length < 2) {
                        ctx.sendLocalized("commands.profile.description.no_content", EmoteReference.ERROR);
                        return;
                    }

                    String content1 = SPLIT_PATTERN.split(content, 2)[1];

                    if (content1.length() > MAX_LENGTH) {
                        ctx.sendLocalized("commands.profile.description.too_long", EmoteReference.ERROR);
                        return;
                    }

                    content1 = Utils.DISCORD_INVITE.matcher(content1).replaceAll("-discord invite link-");
                    content1 = Utils.DISCORD_INVITE_2.matcher(content1).replaceAll("-discord invite link-");

                    player.getData().setDescription(content1);

                    ctx.sendStrippedLocalized("commands.profile.description.success", EmoteReference.POPPER, content1);

                    player.getData().addBadgeIfAbsent(Badge.WRITER);
                    player.save();
                    return;
                }

                if (args[0].equals("clear")) {
                    player.getData().setDescription(null);
                    ctx.sendLocalized("commands.profile.description.clear_success", EmoteReference.CORRECT);
                    player.save();
                }
            }
        });

        profileCommand.addSubCommand("displaybadge", new SubCommand() {
            @Override
            public String description() {
                return "Sets your profile badge. Usage: `~>profile displaybadge <badge name>`\n" +
                        "Reset with `~>profile displaybadge reset`\n" +
                        "No badge: `~>profile displaybadge none`";
            }

            @Override
            protected void call(Context ctx, String content) {
                String[] args = ctx.getArguments();
                if (args.length == 0) {
                    ctx.sendLocalized("commands.profile.displaybadge.not_specified", EmoteReference.ERROR);
                    return;
                }

                Player player = ctx.getPlayer();
                PlayerData data = player.getData();
                String arg = args[0];

                if (arg.equalsIgnoreCase("none")) {
                    data.setShowBadge(false);
                    ctx.sendLocalized("commands.profile.displaybadge.reset_success", EmoteReference.CORRECT);
                    player.saveAsync();
                    return;
                }

                if (arg.equalsIgnoreCase("reset")) {
                    data.setMainBadge(null);
                    data.setShowBadge(true);
                    ctx.sendLocalized("commands.profile.displaybadge.important_success", EmoteReference.CORRECT);
                    player.saveAsync();
                    return;
                }

                Badge badge = Badge.lookupFromString(content);

                if (badge == null) {
                    ctx.sendLocalized("commands.profile.displaybadge.no_such_badge", EmoteReference.ERROR,
                            player.getData().getBadges().stream()
                                    .map(Badge::getDisplay)
                                    .collect(Collectors.joining(", "))
                    );

                    return;
                }

                if (!data.getBadges().contains(badge)) {
                    ctx.sendLocalized("commands.profile.displaybadge.player_missing_badge", EmoteReference.ERROR,
                            player.getData().getBadges().stream()
                                    .map(Badge::getDisplay)
                                    .collect(Collectors.joining(", "))
                    );

                    return;
                }

                data.setShowBadge(true);
                data.setMainBadge(badge);
                player.saveAsync();
                ctx.sendLocalized("commands.profile.displaybadge.success", EmoteReference.CORRECT, badge.display);
            }
        });

        profileCommand.addSubCommand("lang", new SubCommand() {
            @Override
            public String description() {
                return "Sets your profile language. Usage: `~>profile lang <lang>`. Available langs: `~>lang`";
            }

            @Override
            protected void call(Context ctx, String content) {
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.profile.lang.nothing_specified", EmoteReference.ERROR);
                    return;
                }

                DBUser dbUser = ctx.getDBUser();

                if (content.equalsIgnoreCase("reset")) {
                    dbUser.getData().setLang(null);
                    dbUser.save();
                    ctx.sendLocalized("commands.profile.lang.reset_success", EmoteReference.CORRECT);
                    return;
                }

                if (I18n.isValidLanguage(content)) {
                    dbUser.getData().setLang(content);
                    //Create new I18n context based on the new language choice.
                    I18nContext newContext = new I18nContext(ctx.getDBGuild().getData(), dbUser.getData());

                    dbUser.save();
                    ctx.getChannel().sendMessageFormat(newContext.get("commands.profile.lang.success"), EmoteReference.CORRECT, content).queue();
                } else {
                    ctx.sendLocalized("commands.profile.lang.invalid", EmoteReference.ERROR);
                }
            }
        });

        profileCommand.addSubCommand("stats", new SubCommand() {
            @Override
            public String description() {
                return "Checks profile statistics. Usage: `~>profile stats [@mention]`";
            }

            @Override
            protected void call(Context ctx, String content) {
                Member member = Utils.findMember(ctx.getEvent(), ctx.getMember(), content);
                if (member == null)
                    return;

                User toLookup = member.getUser();

                Player player = ctx.getPlayer(toLookup);
                DBUser dbUser = ctx.getDBUser(toLookup);
                UserData data = dbUser.getData();
                PlayerData playerData = player.getData();
                PlayerStats playerStats = ctx.db().getPlayerStats(toLookup);
                SeasonPlayer seasonPlayer = ctx.getSeasonPlayer(toLookup);

                PlayerEquipment equippedItems = data.getEquippedItems();
                PlayerEquipment seasonalEquippedItems = seasonPlayer.getData().getEquippedItems();

                Potion potion = (Potion) equippedItems.getEffectItem(PlayerEquipment.EquipmentType.POTION);
                Potion buff = (Potion) equippedItems.getEffectItem(PlayerEquipment.EquipmentType.BUFF);
                PotionEffect potionEffect = equippedItems.getCurrentEffect(PlayerEquipment.EquipmentType.POTION);
                PotionEffect buffEffect = equippedItems.getCurrentEffect(PlayerEquipment.EquipmentType.BUFF);

                boolean isPotionActive =
                        potion != null && 
                        (equippedItems.isEffectActive(PlayerEquipment.EquipmentType.POTION, potion.getMaxUses()) ||
                                potionEffect.getAmountEquipped() > 1);
                boolean isBuffActive = 
                        buff != null && 
                        (equippedItems.isEffectActive(PlayerEquipment.EquipmentType.BUFF, buff.getMaxUses()) ||
                                buffEffect.getAmountEquipped() > 1);

                long potionEquipped = 0;
                long buffEquipped = 0;

                if (potion != null)
                    potionEquipped = 
                            equippedItems.isEffectActive(PlayerEquipment.EquipmentType.POTION, potion.getMaxUses()) ? 
                            potionEffect.getAmountEquipped() : potionEffect.getAmountEquipped() - 1;
                if (buff != null)
                    buffEquipped = 
                            equippedItems.isEffectActive(PlayerEquipment.EquipmentType.BUFF, buff.getMaxUses()) ? 
                            buffEffect.getAmountEquipped() : buffEffect.getAmountEquipped() - 1;

                //no need for decimals
                long experienceNext = (long) (player.getLevel() * Math.log10(player.getLevel()) * 1000) +
                        (50 * player.getLevel() / 2);

                boolean noPotion = potion == null || !isPotionActive;
                boolean noBuff = buff == null || !isBuffActive;

                String equipment = parsePlayerEquipment(equippedItems);
                String seasonalEquipment = parsePlayerEquipment(seasonalEquippedItems);

                I18nContext languageContext = ctx.getLanguageContext();

                //This whole thing is a massive mess, lmfao.
                //This is definitely painful and goes on for 100 lines lol
                String s = String.join("\n",
                        prettyDisplay(languageContext.get("commands.profile.stats.market"),
                                playerData.getMarketUsed() + " " + languageContext.get("commands.profile.stats.times")
                        ),

                        //Potion display
                        prettyDisplay(languageContext.get("commands.profile.stats.potion"),
                                noPotion ? "None" : String.format("%s (%dx)", potion.getName(), potionEquipped)
                        ),

                        "\u3000 " +
                                EmoteReference.BOOSTER + languageContext.get("commands.profile.stats.times_used") + ": " +
                                (noPotion ? "Not equipped" :
                                        potionEffect.getTimesUsed() + " " +
                                        languageContext.get("commands.profile.stats.times")),

                        prettyDisplay(languageContext.get("commands.profile.stats.buff"), noBuff ? "None" :
                                String.format("%s (%dx)", buff.getName(), buffEquipped)
                        ),

                        "\u3000 " +
                                EmoteReference.BOOSTER + languageContext.get("commands.profile.stats.times_used") + ": " +
                                (noBuff ? "Not equipped" : buffEffect.getTimesUsed() + " " +
                                        languageContext.get("commands.profile.stats.times")),
                        //End of potion display

                        prettyDisplayLine(languageContext.get("commands.profile.stats.equipment"),
                                equipment
                        ),

                        prettyDisplayLine(languageContext.get("commands.profile.stats.seasonal_equipment"),
                                seasonalEquipment
                        ),

                        prettyDisplay(languageContext.get("commands.profile.stats.autoequip"),
                                String.valueOf(data.isAutoEquip())
                        ),

                        prettyDisplay(languageContext.get("commands.profile.stats.experience"),
                                playerData.getExperience() + "/" + experienceNext + " XP"
                        ),

                        prettyDisplay(languageContext.get("commands.profile.stats.mine_xp"),
                                playerData.getMiningExperience() + " XP"
                        ),

                        prettyDisplay(languageContext.get("commands.profile.stats.fish_xp"),
                                playerData.getFishingExperience() + " XP"
                        ),

                        prettyDisplay(languageContext.get("commands.profile.stats.sharks_caught"),
                                String.valueOf(playerData.getSharksCaught())
                        ),

                        prettyDisplay(languageContext.get("commands.profile.stats.crates_open"),
                                String.valueOf(playerData.getCratesOpened())
                        ),

                        prettyDisplay(languageContext.get("commands.profile.stats.times_mop"),
                                String.valueOf(playerData.getTimesMopped())
                        ),

                        prettyDisplay(languageContext.get("commands.profile.stats.daily"),
                                playerData.getDailyStreak() + " " + languageContext.get("commands.profile.stats.days")
                        ),

                        prettyDisplay(languageContext.get("commands.profile.stats.daily_at"),
                                playerData.getLastDailyAt() == 0 ?
                                        languageContext.get("commands.profile.stats.never") :
                                        new Date(playerData.getLastDailyAt()).toString()
                        ),

                        prettyDisplay(languageContext.get("commands.profile.stats.waifu_claimed"),
                                data.getTimesClaimed() + " " + languageContext.get("commands.profile.stats.times")
                        ),

                        prettyDisplay(languageContext.get("commands.profile.stats.waifu_locked"),
                                String.valueOf(playerData.isClaimLocked())
                        ),

                        prettyDisplay(languageContext.get("commands.profile.stats.dust"),
                                data.getDustLevel() + "%"
                        ),

                        prettyDisplay(languageContext.get("commands.profile.stats.reminders"),
                                data.getRemindedTimes() + " " + languageContext.get("commands.profile.stats.times")
                        ),

                        prettyDisplay(languageContext.get("commands.profile.stats.lang"),
                                (data.getLang() == null ? "en_US" : data.getLang())
                        ),

                        prettyDisplay(languageContext.get("commands.profile.stats.wins"),
                                String.format("\n\u3000\u2009\u2009\u2009\u2009" +
                                        "%1$sGamble: %2$d, Slots: %3$d, Game: %4$d (times)",
                                        EmoteReference.CREDITCARD, playerStats.getGambleWins(), playerStats.getSlotsWins(), playerData.getGamesWon())
                        )
                );


                ctx.send(new EmbedBuilder()
                        .setThumbnail(toLookup.getEffectiveAvatarUrl())
                        .setAuthor(String.format(languageContext.get("commands.profile.stats.header"), toLookup.getName()), null, toLookup.getEffectiveAvatarUrl())
                        .setDescription("\n" + s)
                        .setFooter("This shows stuff usually not shown on the profile card. Content might change", null)
                        .build()
                );
            }
        });

        profileCommand.addSubCommand("widgets", new SubCommand() {
            @Override
            public String description() {
                return "Sets profile widgets and order. Usage: `~>profile widgets <widget/ls/reset>`";
            }

            @Override
            protected void call(Context ctx, String content) {
                DBUser user = ctx.getDBUser();
                if (!user.isPremium()) {
                    ctx.sendLocalized("commands.profile.display.not_premium", EmoteReference.ERROR);
                    return;
                }

                Player player = ctx.getPlayer();
                PlayerData data = player.getData();
                I18nContext languageContext = ctx.getLanguageContext();

                if (content.equalsIgnoreCase("ls") || content.equalsIgnoreCase("Is")) {
                    ctx.sendFormat(languageContext.get("commands.profile.display.ls") + languageContext.get("commands.profile.display.example"),
                            EmoteReference.ZAP,
                            EmoteReference.BLUE_SMALL_MARKER,

                            defaultOrder.stream()
                                    .map(Enum::name)
                                    .collect(Collectors.joining(", ")),

                            data.getProfileComponents().size() == 0 ? "Not personalized" :
                                    data.getProfileComponents().stream()
                                            .map(Enum::name)
                                            .collect(Collectors.joining(", "))
                    );
                    return;
                }

                if (content.equalsIgnoreCase("reset")) {
                    data.getProfileComponents().clear();
                    player.saveAsync();

                    ctx.sendLocalized("commands.profile.display.reset", EmoteReference.CORRECT);
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
                    ctx.sendFormat(languageContext.get("commands.profile.display.not_enough") +
                            languageContext.get("commands.profile.display.example"), EmoteReference.WARNING
                    );

                    return;
                }

                data.setProfileComponents(newComponents);
                player.saveAsync();

                ctx.sendLocalized("commands.profile.display.success",
                        EmoteReference.CORRECT, newComponents.stream().map(Enum::name).collect(Collectors.joining(", "))
                );
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

            Response res = httpClient.newCall(new Request.Builder()
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
