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
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.item.ItemHelper;
import net.kodehawa.mantarobot.commands.currency.item.ItemReference;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.PlayerEquipment;
import net.kodehawa.mantarobot.commands.currency.item.special.Potion;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.Breakable;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.currency.profile.ProfileComponent;
import net.kodehawa.mantarobot.commands.currency.profile.inventory.InventorySortType;
import net.kodehawa.mantarobot.commands.currency.seasons.SeasonPlayer;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.base.ITreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.I18n;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.CustomFinderUtil;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.commands.currency.profile.ProfileComponent.*;
import static net.kodehawa.mantarobot.utils.StringUtils.SPLIT_PATTERN;
import static net.kodehawa.mantarobot.utils.Utils.*;

@Module
public class ProfileCmd {
    private static final Pattern offsetRegex = Pattern.compile("(?:UTC|GMT)[+-][0-9]{1,2}(:[0-9]{1,2})?", Pattern.CASE_INSENSITIVE);

    // Discord makes it so multiple spaces get only rendered as one, but half-width spaces don't.
    // So therefore you get this cursed *thing* for formatting.
    private static final String SEPARATOR = "\u2009\u2009\u2009\u2009\u2009\u2009\u2009\u2009";
    private static final String SEPARATOR_MID = "\u2009\u2009\u2009\u2009";
    private static final String SEPARATOR_ONE = "\u2009\u2009";
    private static final String SEPARATOR_HALF = "\u2009";

    // A small white square.
    private static final String LIST_MARKER = "\u25AB\uFE0F";

    @Subscribe
    public void profile(CommandRegistry cr) {
        final var rateLimiter = new IncreasingRateLimiter.Builder()
                .limit(2) //twice every 10m
                .spamTolerance(2)
                .cooldown(10, TimeUnit.MINUTES)
                .cooldownPenaltyIncrease(10, TimeUnit.SECONDS)
                .maxCooldown(15, TimeUnit.MINUTES)
                .pool(MantaroData.getDefaultJedisPool())
                .prefix("profile")
                .build();

        final var config = MantaroData.config().get();

        List<ProfileComponent> defaultOrder;
        if (config.isPremiumBot() || config.isSelfHost()) {
            defaultOrder = createLinkedList(HEADER, CREDITS, LEVEL, REPUTATION, BIRTHDAY, MARRIAGE, INVENTORY, BADGES);
        } else {
            defaultOrder = createLinkedList(HEADER, CREDITS, OLD_CREDITS, LEVEL, REPUTATION, BIRTHDAY, MARRIAGE, INVENTORY, BADGES);
        }

        List<ProfileComponent> noOldOlder = createLinkedList(HEADER, CREDITS, LEVEL, REPUTATION, BIRTHDAY, MARRIAGE, INVENTORY, BADGES);

        ITreeCommand profileCommand = cr.register("profile", new TreeCommand(CommandCategory.CURRENCY) {
            @Override
            public Command defaultTrigger(Context ctx, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, I18nContext languageContext, String content) {
                        var optionalArguments = ctx.getOptionalArguments();
                        content = Utils.replaceArguments(optionalArguments, content, "season", "s").trim();
                        var isSeasonal = ctx.isSeasonal();


                        var finalContent = content;
                        ctx.findMember(content, ctx.getMessage()).onSuccess(members -> {
                            SeasonPlayer seasonalPlayer = null;
                            var userLooked = ctx.getAuthor();
                            var memberLooked = ctx.getMember();

                            if (!finalContent.isEmpty()) {
                                var found = CustomFinderUtil.findMember(finalContent, members, ctx);
                                if (found == null) {
                                    return;
                                }

                                userLooked = found.getUser();
                                memberLooked = found;
                            }

                            if (userLooked.isBot()) {
                                ctx.sendLocalized("commands.profile.bot_notice", EmoteReference.ERROR);
                                return;
                            }

                            var player = ctx.getPlayer(userLooked);
                            var dbUser = ctx.getDBUser(userLooked);

                            var playerData = player.getData();
                            var userData = dbUser.getData();
                            var inv = player.getInventory();

                            // Cache waifu value.
                            playerData.setWaifuCachedValue(WaifuCmd.calculateWaifuValue(userLooked).getFinalValue());

                            // start of badge assigning
                            var mh = MantaroBot.getInstance().getShardManager().getGuildById("213468583252983809");
                            var mhMember = mh == null ? null : ctx.retrieveMemberById(memberLooked.getUser().getId(), false);

                            Badge.assignBadges(player, dbUser);
                            var christmasBadgeAssign = inv.asList()
                                    .stream()
                                    .map(ItemStack::getItem)
                                    .anyMatch(it -> it.equals(ItemReference.CHRISTMAS_TREE_SPECIAL) || it.equals(ItemReference.BELL_SPECIAL));

                            // Manual badges
                            if (config.isOwner(userLooked)) {
                                playerData.addBadgeIfAbsent(Badge.DEVELOPER);
                            }

                            if (christmasBadgeAssign) {
                                playerData.addBadgeIfAbsent(Badge.CHRISTMAS);
                            }

                            // Requires a valid Member in Mantaro Hub.
                            if (mhMember != null) {
                                // Admin
                                if (containsRole(mhMember, 315910951994130432L, 642089477828902912L)) {
                                    playerData.addBadgeIfAbsent(Badge.COMMUNITY_ADMIN);
                                }

                                // Patron - Donator
                                if (containsRole(mhMember, 290902183300431872L, 290257037072531466L)) {
                                    playerData.addBadgeIfAbsent(Badge.DONATOR_2);
                                }

                                // Translator
                                if (containsRole(mhMember, 407156441812828162L)) {
                                    playerData.addBadgeIfAbsent(Badge.TRANSLATOR);
                                }
                            }
                            // end of badge assigning

                            var badges = playerData.getBadges();
                            Collections.sort(badges);

                            if (isSeasonal) {
                                seasonalPlayer = ctx.getSeasonPlayer(userLooked);
                            }

                            var ringHolder = player.getInventory().containsItem(ItemReference.RING) && userData.getMarriage() != null;
                            var holder = new ProfileComponent.Holder(userLooked, player, seasonalPlayer, dbUser, badges);
                            var profileBuilder = new EmbedBuilder();
                            var description = languageContext.get("commands.profile.no_desc");

                            if (playerData.getDescription() != null) {
                                description = player.getData().getDescription();
                            }

                            profileBuilder.setAuthor(
                                    (ringHolder ? EmoteReference.RING : "") + String.format(languageContext.get("commands.profile.header"),
                                            memberLooked.getEffectiveName()), null, userLooked.getEffectiveAvatarUrl())
                                    .setDescription(description)
                                    .setThumbnail(userLooked.getEffectiveAvatarUrl())
                                    .setColor(memberLooked.getColor() == null ? Color.PINK : memberLooked.getColor())
                                    .setFooter(ProfileComponent.FOOTER.getContent().apply(holder, languageContext),
                                            ctx.getAuthor().getEffectiveAvatarUrl()
                                    );

                            var hasCustomOrder = dbUser.isPremium() && !playerData.getProfileComponents().isEmpty();
                            var usedOrder = hasCustomOrder ? playerData.getProfileComponents() : defaultOrder;
                            if ((!config.isPremiumBot() && player.getOldMoney() < 5000 && !hasCustomOrder) || playerData.isHiddenLegacy()) {
                                usedOrder = noOldOlder;
                            }

                            for (var component : usedOrder) {
                                profileBuilder.addField(
                                        component.getTitle(languageContext), component.getContent().apply(holder, languageContext), component.isInline()
                                );
                            }

                            ctx.send(profileBuilder.build());
                            player.saveUpdating();
                        });

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
            protected void call(Context ctx, I18nContext languageContext, String content) {
                final var player = ctx.getPlayer();
                final var playerData = player.getData();

                if (content.equals("remove")) {
                    playerData.setClaimLocked(false);
                    ctx.sendLocalized("commands.profile.claimlock.removed", EmoteReference.CORRECT);
                    player.saveUpdating();
                    return;
                }

                if (playerData.isClaimLocked()) {
                    ctx.sendLocalized("commands.profile.claimlock.already_locked", EmoteReference.CORRECT);
                    return;
                }

                var inventory = player.getInventory();
                if (!inventory.containsItem(ItemReference.CLAIM_KEY)) {
                    ctx.sendLocalized("commands.profile.claimlock.no_key", EmoteReference.ERROR);
                    return;
                }

                playerData.setClaimLocked(true);
                ctx.sendLocalized("commands.profile.claimlock.success", EmoteReference.CORRECT);
                inventory.process(new ItemStack(ItemReference.CLAIM_KEY, -1));
                player.saveUpdating();
            }
        });

        if (!config.isPremiumBot()) {
            profileCommand.addSubCommand("togglelegacy", new SubCommand() {
                @Override
                public String description() {
                    return "Hides/unhides legacy credits.";
                }

                @Override
                protected void call(Context ctx, I18nContext languageContext, String content) {
                    final var player = ctx.getPlayer();
                    final var data = player.getData();
                    data.setHiddenLegacy(!data.isHiddenLegacy());

                    ctx.sendLocalized("commands.profile.hidelegacy", EmoteReference.CORRECT, data.isHiddenLegacy());
                }
            });
        }

        profileCommand.addSubCommand("inventorysort", new SubCommand() {
            @Override
            public String description() {
                return "Sets how you wanna sort your inventory. Possible values: `VALUE, VALUE_TOTAL, AMOUNT, TYPE, RANDOM`.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                final var type = Utils.lookupEnumString(content, InventorySortType.class);

                if (type == null) {
                    ctx.sendLocalized("commands.profile.inventorysort.not_valid",
                            EmoteReference.ERROR, Arrays.stream(InventorySortType.values())
                                    .map(b1 -> b1.toString().toLowerCase())
                                    .collect(Collectors.joining(", "))
                    );
                    return;
                }

                final var player = ctx.getPlayer();
                final var playerData = player.getData();

                playerData.setInventorySortType(type);
                player.saveUpdating();

                ctx.sendLocalized("commands.profile.inventorysort.success", EmoteReference.CORRECT, type.toString().toLowerCase());
            }
        });

        profileCommand.addSubCommand("autoequip", new SubCommand() {
            @Override
            public String description() {
                return "Sets whether you want or not to autoequip a new tool on break. Use `disable` to disable it.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var user = ctx.getDBUser();
                var data = user.getData();

                if (content.equals("disable")) {
                    data.setAutoEquip(false);
                    ctx.sendLocalized("commands.profile.autoequip.disable", EmoteReference.CORRECT);
                    user.saveUpdating();
                    return;
                }

                data.setAutoEquip(true);
                ctx.sendLocalized("commands.profile.autoequip.success", EmoteReference.CORRECT);
                user.saveUpdating();
            }
        });

        //Hide tags from profile/waifu list.
        profileCommand.addSubCommand("hidetag", new SubCommand() {
            @Override
            public String description() {
                return "Hide the member tags (and IDs) from profile/waifu ls. This is a switch.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var user = ctx.getDBUser();
                var data = user.getData();

                data.setPrivateTag(!data.isPrivateTag());
                user.saveUpdating();

                ctx.sendLocalized("commands.profile.hide_tag.success", EmoteReference.POPPER, data.isPrivateTag());
            }
        });

        profileCommand.addSubCommand("timezone", new SubCommand() {
            @Override
            public String description() {
                return "Sets the profile timezone.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var dbUser = ctx.getDBUser();
                var args = ctx.getArguments();

                if (args.length < 1) {
                    ctx.sendLocalized("commands.profile.timezone.not_specified", EmoteReference.ERROR);
                    return;
                }

                var timezone = content;
                if (offsetRegex.matcher(timezone).matches()) {
                    timezone = content.toUpperCase().replace("UTC", "GMT");
                }

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

                var player = ctx.getPlayer();
                if (player.getData().addBadgeIfAbsent(Badge.CALENDAR)) {
                    player.saveUpdating();
                }

                dbUser.getData().setTimezone(timezone);
                dbUser.saveUpdating();
                ctx.sendLocalized("commands.profile.timezone.success", EmoteReference.CORRECT, timezone);
            }
        });

        profileCommand.addSubCommand("description", new SubCommand() {
            @Override
            public String description() {
                return "Sets your profile description.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                if (!RatelimitUtils.ratelimit(rateLimiter, ctx)) {
                    return;
                }

                var args = content.split(" ");
                var player = ctx.getPlayer();
                var dbUser = ctx.getDBUser();

                if (args.length == 0) {
                    ctx.sendLocalized("commands.profile.description.no_argument", EmoteReference.ERROR);
                    return;
                }

                if (args[0].equals("clear")) {
                    player.getData().setDescription(null);
                    ctx.sendLocalized("commands.profile.description.clear_success", EmoteReference.CORRECT);
                    player.saveUpdating();
                }

                var split = SPLIT_PATTERN.split(content, 2);
                var desc = content;
                var old = false;
                if (split[0].equals("set")) {
                    desc = content.replaceFirst("set ", "");
                    old = true;
                }

                var MAX_LENGTH = 300;

                if (dbUser.isPremium()) {
                    MAX_LENGTH = 500;
                }

                if (args.length < (old ? 2 : 1)) {
                    ctx.sendLocalized("commands.profile.description.no_content", EmoteReference.ERROR);
                    return;
                }

                if (desc.length() > MAX_LENGTH) {
                    ctx.sendLocalized("commands.profile.description.too_long", EmoteReference.ERROR);
                    return;
                }

                desc = Utils.DISCORD_INVITE.matcher(desc).replaceAll("-discord invite link-");
                desc = Utils.DISCORD_INVITE_2.matcher(desc).replaceAll("-discord invite link-");

                player.getData().setDescription(desc);

                ctx.sendStrippedLocalized("commands.profile.description.success", EmoteReference.POPPER, desc);

                player.getData().addBadgeIfAbsent(Badge.WRITER);
                player.saveUpdating();
            }
        });

        profileCommand.addSubCommand("displaybadge", new SubCommand() {
            @Override
            public String description() {
                return """
                        Sets your profile badge.
                        Reset with `~>profile displaybadge reset`
                        No badge: `~>profile displaybadge none`""";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var args = ctx.getArguments();
                if (args.length == 0) {
                    ctx.sendLocalized("commands.profile.displaybadge.not_specified", EmoteReference.ERROR);
                    return;
                }

                var player = ctx.getPlayer();
                var data = player.getData();
                var arg = args[0];

                if (arg.equalsIgnoreCase("none")) {
                    data.setShowBadge(false);
                    ctx.sendLocalized("commands.profile.displaybadge.reset_success", EmoteReference.CORRECT);
                    player.saveUpdating();
                    return;
                }

                if (arg.equalsIgnoreCase("reset")) {
                    data.setMainBadge(null);
                    data.setShowBadge(true);
                    ctx.sendLocalized("commands.profile.displaybadge.important_success", EmoteReference.CORRECT);
                    player.saveUpdating();
                    return;
                }

                var badge = Badge.lookupFromString(content);

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
                player.saveUpdating();
                ctx.sendLocalized("commands.profile.displaybadge.success", EmoteReference.CORRECT, badge.display);
            }
        });

        profileCommand.addSubCommand("language", new SubCommand() {
            @Override
            public String description() {
                return "Sets your profile language. Available langs: `~>lang`";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.profile.lang.nothing_specified", EmoteReference.ERROR);
                    return;
                }

                var dbUser = ctx.getDBUser();

                if (content.equalsIgnoreCase("reset")) {
                    dbUser.getData().setLang(null);
                    dbUser.saveUpdating();
                    ctx.sendLocalized("commands.profile.lang.reset_success", EmoteReference.CORRECT);
                    return;
                }

                if (I18n.isValidLanguage(content)) {
                    dbUser.getData().setLang(content);
                    //Create new I18n context based on the new language choice.
                    var newContext = new I18nContext(ctx.getDBGuild().getData(), dbUser.getData());

                    dbUser.saveUpdating();
                    ctx.getChannel().sendMessageFormat(newContext.get("commands.profile.lang.success"), EmoteReference.CORRECT, content).queue();
                } else {
                    ctx.sendLocalized("commands.profile.lang.invalid", EmoteReference.ERROR);
                }
            }
        }).createSubCommandAlias("language", "lang");

        profileCommand.addSubCommand("stats", new SubCommand() {
            @Override
            public String description() {
                return "Checks profile statistics.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                ctx.findMember(content, ctx.getMessage()).onSuccess(members -> {
                    var member = CustomFinderUtil.findMemberDefault(content, members, ctx, ctx.getMember());
                    if (member == null) {
                        return;
                    }

                    var toLookup = member.getUser();
                    if (toLookup.isBot()) {
                        ctx.sendLocalized("commands.profile.bot_notice", EmoteReference.ERROR);
                        return;
                    }

                    var player = ctx.getPlayer(toLookup);
                    var dbUser = ctx.getDBUser(toLookup);
                    var data = dbUser.getData();
                    var playerData = player.getData();
                    var playerStats = ctx.db().getPlayerStats(toLookup);
                    var seasonPlayer = ctx.getSeasonPlayer(toLookup);

                    var equippedItems = data.getEquippedItems();
                    var seasonalEquippedItems = seasonPlayer.getData().getEquippedItems();

                    var potion = (Potion) equippedItems.getEffectItem(PlayerEquipment.EquipmentType.POTION);
                    var buff = (Potion) equippedItems.getEffectItem(PlayerEquipment.EquipmentType.BUFF);
                    var potionEffect = equippedItems.getCurrentEffect(PlayerEquipment.EquipmentType.POTION);
                    var buffEffect = equippedItems.getCurrentEffect(PlayerEquipment.EquipmentType.BUFF);

                    var isPotionActive = potion != null &&
                            (equippedItems.isEffectActive(PlayerEquipment.EquipmentType.POTION, potion.getMaxUses())
                            || potionEffect.getAmountEquipped() > 1);

                    var isBuffActive = buff != null &&
                            (equippedItems.isEffectActive(PlayerEquipment.EquipmentType.BUFF, buff.getMaxUses())
                            || buffEffect.getAmountEquipped() > 1);


                    var potionEquipped = 0L;
                    var buffEquipped = 0L;

                    if (potion != null) {
                        potionEquipped = equippedItems.isEffectActive(
                                PlayerEquipment.EquipmentType.POTION, potion.getMaxUses()
                        ) ? potionEffect.getAmountEquipped() : potionEffect.getAmountEquipped() - 1;
                    }

                    if (buff != null) {
                        buffEquipped = equippedItems.isEffectActive(
                                PlayerEquipment.EquipmentType.BUFF, buff.getMaxUses()
                        ) ? buffEffect.getAmountEquipped() : buffEffect.getAmountEquipped() - 1;
                    }

                    //no need for decimals
                    var experienceNext = (long) (player.getLevel() * Math.log10(player.getLevel()) * 1000) +
                            (50 * player.getLevel() / 2);

                    var noPotion = potion == null || !isPotionActive;
                    var noBuff = buff == null || !isBuffActive;

                    var equipment = parsePlayerEquipment(equippedItems);
                    var seasonalEquipment = parsePlayerEquipment(seasonalEquippedItems);

                    //This whole thing is a massive mess, lmfao.
                    //This is definitely painful and goes on for 100 lines lol
                    var s = String.join("\n",
                            prettyDisplay(languageContext.get("commands.profile.stats.market"),
                                    "%,d %s".formatted(playerData.getMarketUsed(), languageContext.get("commands.profile.stats.times"))
                            ),

                            //Potion display
                            prettyDisplay(languageContext.get("commands.profile.stats.potion"),
                                    noPotion ? "None" : String.format("%s (%dx)", potion.getName(), potionEquipped)
                            ),

                            "\u3000 " + SEPARATOR_HALF +
                                    EmoteReference.BOOSTER + languageContext.get("commands.profile.stats.times_used") + ": " +
                                    (noPotion ? "Not equipped" : potionEffect.getTimesUsed() + " " +
                                            languageContext.get("commands.profile.stats.times")),

                            prettyDisplay(languageContext.get("commands.profile.stats.buff"), noBuff ? "None" :
                                    String.format("%s (%dx)", buff.getName(), buffEquipped)
                            ),

                            "\u3000 " + SEPARATOR_HALF +
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
                                    "%,d/%,d XP".formatted(playerData.getExperience(), experienceNext)
                            ),

                            prettyDisplay(languageContext.get("commands.profile.stats.mine_xp"),
                                    "%,d XP".formatted(playerData.getMiningExperience())
                            ),

                            prettyDisplay(languageContext.get("commands.profile.stats.chop_xp"),
                                    "%,d XP".formatted(playerData.getChopExperience())
                            ),

                            prettyDisplay(languageContext.get("commands.profile.stats.fish_xp"),
                                    "%,d XP".formatted(playerData.getFishingExperience())
                            ),

                            prettyDisplay(languageContext.get("commands.profile.stats.sharks_caught"),
                                    "%,d".formatted(playerData.getSharksCaught())
                            ),

                            prettyDisplay(languageContext.get("commands.profile.stats.crates_open"),
                                    "%,d".formatted(playerData.getCratesOpened())
                            ),

                            prettyDisplay(languageContext.get("commands.profile.stats.times_mop"),
                                    "%,d".formatted(playerData.getTimesMopped())
                            ),

                            prettyDisplay(languageContext.get("commands.profile.stats.daily"),
                                    "%,d %s".formatted(playerData.getDailyStreak(), languageContext.get("commands.profile.stats.days"))
                            ),

                            prettyDisplay(languageContext.get("commands.profile.stats.daily_at"),
                                    playerData.getLastDailyAt() == 0 ?
                                            languageContext.get("commands.profile.stats.never") :
                                            Utils.formatDate(playerData.getLastDailyAt(), dbUser.getData().getLang())
                            ),

                            prettyDisplay(languageContext.get("commands.profile.stats.waifu_claimed"),
                                    "%,d %s".formatted(data.getTimesClaimed(), languageContext.get("commands.profile.stats.times"))
                            ),

                            prettyDisplay(languageContext.get("commands.profile.stats.waifu_locked"),
                                    String.valueOf(playerData.isClaimLocked())
                            ),

                            prettyDisplay(languageContext.get("commands.profile.stats.dust"),
                                    "%d".formatted(data.getDustLevel())
                            ),

                            prettyDisplay(languageContext.get("commands.profile.stats.reminders"),
                                    "%,d %s".formatted(data.getRemindedTimes(), languageContext.get("commands.profile.stats.times"))
                            ),

                            prettyDisplay(languageContext.get("commands.profile.stats.lang"),
                                    (data.getLang() == null ? "en_US" : data.getLang())
                            ),

                            prettyDisplay(languageContext.get("commands.profile.stats.wins"),
                                    String.format("\n\u3000%1$s" +
                                                    "%2$s%3$sGamble: %4$,d, Slots: %5$,d, Game: %6$,d (times)",
                                            SEPARATOR_ONE, EmoteReference.CREDITCARD, SEPARATOR_ONE,
                                            playerStats.getGambleWins(), playerStats.getSlotsWins(),
                                            playerData.getGamesWon()
                                    )
                            )
                    );

                    ctx.send(new EmbedBuilder()
                            .setThumbnail(toLookup.getEffectiveAvatarUrl())
                            .setAuthor(languageContext.get("commands.profile.stats.header").formatted(toLookup.getName()),
                                    null, toLookup.getEffectiveAvatarUrl()
                            )
                            .setDescription("\n" + s)
                            .setFooter("Thanks for using Mantaro! %s".formatted(EmoteReference.HEART), ctx.getGuild().getIconUrl())
                            .build()
                    );
                });
            }
        });

        profileCommand.addSubCommand("widgets", new SubCommand() {
            @Override
            public String description() {
                return "Sets profile widgets and order. Arguments: widget, ls or reset";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var user = ctx.getDBUser();
                if (!user.isPremium()) {
                    ctx.sendLocalized("commands.profile.display.not_premium", EmoteReference.ERROR);
                    return;
                }

                var player = ctx.getPlayer();
                var playerData = player.getData();

                if (content.equalsIgnoreCase("ls") || content.equalsIgnoreCase("Is")) {
                    ctx.sendFormat(
                            languageContext.get("commands.profile.display.ls") +
                                    languageContext.get("commands.profile.display.example"),
                            EmoteReference.ZAP, EmoteReference.BLUE_SMALL_MARKER,
                            defaultOrder.stream().map(Enum::name).collect(Collectors.joining(", ")),
                            playerData.getProfileComponents().size() == 0 ?
                                    "Not personalized" :
                                    playerData.getProfileComponents().stream()
                                            .map(Enum::name)
                                            .collect(Collectors.joining(", "))
                    );
                    return;
                }

                if (content.equalsIgnoreCase("reset")) {
                    playerData.getProfileComponents().clear();
                    player.save();

                    ctx.sendLocalized("commands.profile.display.reset", EmoteReference.CORRECT);
                    return;
                }

                var splitContent = content.replace(",", "").split("\\s+");
                List<ProfileComponent> newComponents = new LinkedList<>(); //new list of profile components

                for (var cmt : splitContent) {
                    var component = ProfileComponent.lookupFromString(cmt);
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

                playerData.setProfileComponents(newComponents);
                player.save();

                ctx.sendLocalized("commands.profile.display.success",
                        EmoteReference.CORRECT, newComponents.stream().map(Enum::name).collect(Collectors.joining(", "))
                );
            }
        });
    }

    public String parsePlayerEquipment(PlayerEquipment equipment) {
        var toolsEquipment = equipment.getEquipment();
        var separator = SEPARATOR + SEPARATOR_HALF + LIST_MARKER + SEPARATOR_HALF;

        if (toolsEquipment.isEmpty()) {
            return separator + "None";
        }

        return toolsEquipment.entrySet().stream().map((entry) -> {
            var item = ItemHelper.fromId(entry.getValue());

            return separator + Utils.capitalize(entry.getKey().toString()) + ": " + SEPARATOR_ONE +
                    item.toDisplayString() + SEPARATOR_HALF + " [%,d / %,d]"
                    .formatted(equipment.getDurability().get(entry.getKey()), ((Breakable) item).getMaxDurability());
        }).collect(Collectors.joining("\n"));
    }

    private boolean containsRole(Member member, long... roles) {
        return member.getRoles().stream().map(Role::getIdLong).anyMatch(id -> Arrays.stream(roles).anyMatch(i -> i == id));
    }
}
