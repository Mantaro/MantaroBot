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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.commands.currency.Waifu;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.CustomFinderUtil;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Module
public class WaifuCmd {
    private static final long WAIFU_BASE_VALUE = 1000L;

    @Subscribe
    public void waifu(CommandRegistry cr) {
        IncreasingRateLimiter rl = new IncreasingRateLimiter.Builder()
                .limit(1)
                .spamTolerance(2)
                .cooldown(5, TimeUnit.SECONDS)
                .maxCooldown(5, TimeUnit.SECONDS)
                .randomIncrement(true)
                .pool(MantaroData.getDefaultJedisPool())
                .prefix("waifu")
                .build();


        TreeCommand waifu = cr.register("waifu", new TreeCommand(CommandCategory.CURRENCY) {
            @Override
            public Command defaultTrigger(Context ctx, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, I18nContext languageContext, String content) {
                        // IMPLEMENTATION NOTES FOR THE WAIFU SYSTEM
                        // You get 3 free slots to put "waifus" in.
                        // Each extra slot (up to 9) costs exponentially more than the last one (2x more than the costs of the last one)
                        // Every waifu has a "claim" price which increases in the following situations:
                        // For every 100000 money owned, it increases by 3% base value (base: 1500)
                        // For every 10 badges, it increases by 20% base value.
                        // For every 1000 experience, the value increases by 20% of the base value.
                        // After all those calculations are complete,
                        // the value then is calculated using final * (reputation scale / 10)
                        // where reputation scale goes up by 1 every 10 reputation points.
                        // Maximum waifu value is Integer.MAX_VALUE.
                        // Having a common waifu with your married partner will increase some marriage stats.
                        // If you claim a waifu, and then your waifu claims you, that will unlock the "Mutual" achievement.
                        // If the waifu status is mutual,
                        // the MP game boost will go up by 20% and giving your daily to that waifu will increase the amount of money that your
                        // waifu will receive.

                        Map<String, String> opts = ctx.getOptionalArguments();

                        //Default call will bring out the waifu list.
                        DBUser dbUser = ctx.getDBUser();
                        UserData userData = dbUser.getData();
                        Player player = ctx.getPlayer();

                        if (player.getData().isWaifuout()) {
                            ctx.sendLocalized("commands.waifu.optout.notice", EmoteReference.ERROR);
                            return;
                        }

                        String description = userData.getWaifus().isEmpty() ?
                                languageContext.get("commands.waifu.waifu_header") + "\n" + languageContext.get("commands.waifu.no_waifu") :
                                languageContext.get("commands.waifu.waifu_header");

                        EmbedBuilder waifusEmbed = new EmbedBuilder()
                                .setAuthor(languageContext.get("commands.waifu.header"), null, ctx.getAuthor().getEffectiveAvatarUrl())
                                .setThumbnail("https://i.imgur.com/2JlMtCe.png")
                                .setColor(Color.CYAN)
                                .setFooter(languageContext.get("commands.waifu.footer").formatted(
                                        userData.getWaifus().size(),
                                        userData.getWaifuSlots() - userData.getWaifus().size()),
                                        null
                                );

                        if (userData.getWaifus().isEmpty()) {
                            waifusEmbed.setDescription(description);
                            ctx.send(waifusEmbed.build());
                            return;
                        }

                        boolean id = opts.containsKey("id");
                        java.util.List<String> toRemove = new ArrayList<>();

                        List<MessageEmbed.Field> fields = new LinkedList<>();
                        for (String waifu : userData.getWaifus().keySet()) {
                            //This fixes the issue of cross-node waifus not appearing.
                            User user = ctx.retrieveUserById(waifu);
                            if (user == null) {
                                fields.add(new MessageEmbed.Field(
                                        "%sUnknown User (ID: %s)".formatted(EmoteReference.BLUE_SMALL_MARKER, waifu),
                                        languageContext.get("commands.waifu.value_format") + " unknown\n" +
                                                languageContext.get("commands.waifu.value_b_format") + " " + userData.getWaifus().get(waifu) +
                                                languageContext.get("commands.waifu.credits_format"), false)
                                );
                            } else {
                                Player waifuClaimed = ctx.getPlayer(user);
                                if (waifuClaimed.getData().isWaifuout()) {
                                    toRemove.add(waifu);
                                    continue;
                                }

                                fields.add(new MessageEmbed.Field(
                                        EmoteReference.BLUE_SMALL_MARKER + user.getName() +
                                                (!userData.isPrivateTag() ? "#" + user.getDiscriminator() : ""),
                                        (id ? languageContext.get("commands.waifu.id") + " " + user.getId() + "\n" : "") +
                                                languageContext.get("commands.waifu.value_format") + " " +
                                                calculateWaifuValue(user).getFinalValue() + " " +
                                                languageContext.get("commands.waifu.credits_format") + "\n" +
                                                languageContext.get("commands.waifu.value_b_format") + " " + userData.getWaifus().get(waifu) +
                                                languageContext.get("commands.waifu.credits_format"), false)
                                );
                            }
                        }

                        var toSend = languageContext.get("commands.waifu.description_header").formatted(userData.getWaifuSlots()) + description;
                        DiscordUtils.sendPaginatedEmbed(ctx, waifusEmbed, DiscordUtils.divideFields(4, fields), toSend);

                        if (!toRemove.isEmpty()) {
                            for(String remove : toRemove) {
                                dbUser.getData().getWaifus().remove(remove);
                            }

                            player.saveAsync();
                        }
                    }
                };
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("This command is the hub for all waifu operations. Yeah, it's all fiction.")
                        .setUsage("`~>waifu` - Shows a list of all your waifus and their current value.\n" +
                                "`~>waifu [command] [@user]`")
                        .addParameterOptional("command", "The subcommand to use." +
                                " Check the sub-command section for more information on which ones you can use.")
                        .addParameterOptional("@user", "The user you want to do the action with.")
                        .addParameterOptional("-id", "Shows the user id.")
                        .build();
            }
        });

        cr.registerAlias("waifu", "waifus");
        waifu.setPredicate(ctx -> RatelimitUtils.ratelimit(rl, ctx, null, false));

        waifu.addSubCommand("optout", new SubCommand() {
            @Override
            public String description() {
                return "Opt-out of the waifu stuff. This will disable the waifu system, remove all of your claims and make you unable to be claimed.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                ctx.sendLocalized("commands.waifu.optout.warning", EmoteReference.WARNING);
                Player player = ctx.getPlayer();
                if (player.getData().isWaifuout()) {
                    ctx.sendLocalized("commands.waifu.optout.notice", EmoteReference.ERROR);
                    return;
                }

                InteractiveOperations.create(ctx.getChannel(), ctx.getAuthor().getIdLong(), 60, e -> {
                    if (!e.getAuthor().getId().equals(ctx.getAuthor().getId())) {
                        return Operation.IGNORED;
                    }

                    String c = e.getMessage().getContentRaw();

                    if (c.equalsIgnoreCase("Yes, I want to opt out of the waifu system completely and irreversibly")) {
                        player.getData().setWaifuout(true);
                        ctx.sendLocalized("commands.waifu.optout.success", EmoteReference.CORRECT);
                        player.saveUpdating();
                        return Operation.COMPLETED;
                    } else if (c.equalsIgnoreCase("no")) {
                        ctx.sendLocalized("commands.waifu.optout.cancelled", EmoteReference.CORRECT);
                        return Operation.COMPLETED;
                    }

                    return Operation.IGNORED;
                });
            }
        });

        waifu.addSubCommand("stats", new SubCommand() {
            @Override
            public String description() {
                return "Shows your waifu stats or the stats or someone's (by mentioning them)";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                Player player = ctx.getPlayer();
                if (player.getData().isWaifuout()) {
                    ctx.sendLocalized("commands.waifu.optout.notice", EmoteReference.ERROR);
                    return;
                }

                ctx.findMember(content, ctx.getMessage()).onSuccess(members -> {
                    Member member = CustomFinderUtil.findMemberDefault(content, members, ctx, ctx.getMember());
                    if (member == null)
                        return;

                    User toLookup = member.getUser();
                    if (toLookup.isBot()) {
                        ctx.sendLocalized("commands.waifu.bot", EmoteReference.ERROR);
                        return;
                    }

                    Waifu waifuStats = calculateWaifuValue(toLookup);
                    EmbedBuilder statsBuilder = new EmbedBuilder()
                            .setThumbnail(toLookup.getEffectiveAvatarUrl())
                            .setAuthor(toLookup == ctx.getAuthor() ?
                                            languageContext.get("commands.waifu.stats.header") :
                                            languageContext.get("commands.waifu.stats.header_other").formatted(toLookup.getName()),
                                    null, toLookup.getEffectiveAvatarUrl()
                            ).setColor(Color.PINK)
                            .setDescription(languageContext.get("commands.waifu.stats.format").formatted(
                                    EmoteReference.BLUE_SMALL_MARKER,
                                    waifuStats.getMoneyValue(),
                                    waifuStats.getBadgeValue(),
                                    waifuStats.getExperienceValue(),
                                    waifuStats.getClaimValue(),
                                    waifuStats.getReputationMultiplier())
                            ).addField(languageContext.get("commands.waifu.stats.performance"),
                                    EmoteReference.ZAP.toString() + waifuStats.getPerformance() + "wp", true
                            ).addField(languageContext.get("commands.waifu.stats.value"), EmoteReference.BUY +
                                            languageContext.get("commands.waifu.stats.credits").formatted(waifuStats.getFinalValue()),
                                    false
                            ).setFooter(languageContext.get("commands.waifu.notice"), null);

                    ctx.send(statsBuilder.build());
                });
            }
        });

        waifu.addSubCommand("claim", new SubCommand() {
            @Override
            public String description() {
                return "Claim a waifu. You need to mention the person you want to claim. Usage: `~>waifu claim <@mention>`";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                Player player = ctx.getPlayer();
                if (player.getData().isWaifuout()) {
                    ctx.sendLocalized("commands.waifu.optout.notice", EmoteReference.ERROR);
                    return;
                }

                if (ctx.getMentionedUsers().isEmpty()) {
                    ctx.sendLocalized("commands.waifu.claim.no_user", EmoteReference.ERROR);
                    return;
                }

                User toLookup = ctx.getMentionedUsers().get(0);

                if (toLookup.isBot()) {
                    ctx.sendLocalized("commands.waifu.bot", EmoteReference.ERROR);
                    return;
                }

                final var claimerPlayer = ctx.getPlayer();
                final var claimerPlayerData = claimerPlayer.getData();
                final var claimerUser = ctx.getDBUser();
                final var claimerUserData = claimerUser.getData();

                final var claimedPlayer = ctx.getPlayer(toLookup);
                final var claimedPlayerData = claimedPlayer.getData();
                final var claimedUser = ctx.getDBUser(toLookup);
                final var claimedUserData = claimedUser.getData();

                if (claimedPlayerData.isWaifuout()) {
                    ctx.sendLocalized("commands.waifu.optout.claim_notice", EmoteReference.ERROR);
                    return;
                }


                //Waifu object declaration.
                final Waifu waifuToClaim = calculateWaifuValue(toLookup);
                final long waifuFinalValue = waifuToClaim.getFinalValue();

                //Checks.
                if (toLookup.getIdLong() == ctx.getAuthor().getIdLong()) {
                    ctx.sendLocalized("commands.waifu.claim.yourself", EmoteReference.ERROR);
                    return;
                }

                if (claimerUser.getData().getWaifus().entrySet().stream().anyMatch((w) -> w.getKey().equals(toLookup.getId()))) {
                    ctx.sendLocalized("commands.waifu.claim.already_claimed", EmoteReference.ERROR);
                    return;
                }

                //If the to-be claimed has the claim key in their inventory, it cannot be claimed.
                if (claimedPlayerData.isClaimLocked()) {
                    ctx.sendLocalized("commands.waifu.claim.key_locked", EmoteReference.ERROR);
                    return;
                }

                if (claimerPlayer.isLocked()) {
                    ctx.sendLocalized("commands.waifu.claim.locked", EmoteReference.ERROR);
                    return;
                }

                //Deduct from balance and checks for money.
                if (!claimerPlayer.removeMoney(waifuFinalValue)) {
                    ctx.sendLocalized("commands.waifu.claim.not_enough_money", EmoteReference.ERROR, waifuFinalValue);
                    return;
                }

                if (claimerUserData.getWaifus().size() >= claimerUserData.getWaifuSlots()) {
                    ctx.sendLocalized("commands.waifu.claim.not_enough_slots",
                            EmoteReference.ERROR, claimerUserData.getWaifuSlots(), claimerUserData.getWaifus().size()
                    );

                    return;
                }

                if (waifuFinalValue > 100_000) {
                    claimerPlayerData.addBadgeIfAbsent(Badge.GOLD_VALUE);
                }

                //Add waifu to claimer list.
                claimerUserData.getWaifus().put(toLookup.getId(), waifuFinalValue);
                claimedUserData.setTimesClaimed(claimedUserData.getTimesClaimed() + 1);

                //Add badges
                if (claimedUserData.getWaifus().containsKey(ctx.getAuthor().getId()) || claimerUserData.getWaifus().containsKey(toLookup.getId())) {
                    claimerPlayerData.addBadgeIfAbsent(Badge.MUTUAL);
                    claimedPlayerData.addBadgeIfAbsent(Badge.MUTUAL);
                }

                claimerPlayerData.addBadgeIfAbsent(Badge.WAIFU_CLAIMER);
                claimedPlayerData.addBadgeIfAbsent(Badge.CLAIMED);

                //Massive saving operation owo.
                claimerPlayer.saveAsync();
                claimedPlayer.saveAsync();
                claimedUser.saveAsync();
                claimerUser.saveAsync();

                //Send confirmation message
                ctx.sendLocalized("commands.waifu.claim.success",
                        EmoteReference.CORRECT, toLookup.getName(), waifuFinalValue, claimerUserData.getWaifus().size()
                );
            }
        });

        waifu.addSubCommand("unclaim", new SubCommand() {
            @Override
            public String description() {
                return "Unclaims a waifu. You need to mention them, or you can also use their user id if they're not in any servers you share. Usage: `~>waifu unclaim <@mention>`";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                Map<String, String> t = ctx.getOptionalArguments();
                content = Utils.replaceArguments(t, content, "unknown");
                boolean isId = content.matches("\\d{16,20}");
                Player player = ctx.getPlayer();
                if (player.getData().isWaifuout()) {
                    ctx.sendLocalized("commands.waifu.optout.notice", EmoteReference.ERROR);
                    return;
                }

                if (content.isEmpty() && !isId) {
                    ctx.sendLocalized("commands.waifu.unclaim.no_user", EmoteReference.ERROR);
                    return;
                }

                // This is hacky as heck, but assures us we get an empty result on id lookup.
                var lookup = isId ? "" : content;
                // Lambdas strike again.
                var finalContent = content;
                ctx.findMember(lookup, ctx.getMessage()).onSuccess(members -> {
                    // This is hacky again, but search *will* fail if we pass a empty list to this method.
                    Member member = isId ? null : CustomFinderUtil.findMember(lookup, members, ctx);
                    if (member == null && !isId) {
                        return;
                    }

                    User toLookup = isId ? ctx.retrieveUserById(finalContent) : member.getUser();
                    boolean isUnknown = isId && t.containsKey("unknown") && toLookup == null;
                    if (toLookup == null && !isUnknown) {
                        ctx.sendLocalized("commands.waifu.unclaim.not_found", EmoteReference.ERROR);
                        return;
                    }

                    //It'll only be null if -unknown is passed with an unknown ID. This is unclaim, so this check is a bit irrelevant though.
                    if (!isUnknown && toLookup.isBot()) {
                        ctx.sendLocalized("commands.waifu.bot", EmoteReference.ERROR);
                        return;
                    }

                    String userId = isUnknown ? finalContent : toLookup.getId();
                    String name = isUnknown ? "Unknown User" : toLookup.getName();
                    final DBUser claimerUser = ctx.getDBUser();
                    final UserData data = claimerUser.getData();

                    Long value = data.getWaifus().get(userId);

                    if (value == null) {
                        ctx.sendLocalized("commands.waifu.not_claimed", EmoteReference.ERROR);
                        return;
                    }

                    var currentValue = calculateWaifuValue(toLookup).getFinalValue();
                    long valuePayment = (long) (currentValue * 0.15);

                    //Send confirmation message.
                    ctx.sendLocalized("commands.waifu.unclaim.confirmation", EmoteReference.MEGA, name, valuePayment, EmoteReference.STOPWATCH);

                    InteractiveOperations.create(ctx.getChannel(), ctx.getAuthor().getIdLong(), 60, (ie) -> {
                        if (!ie.getAuthor().getId().equals(ctx.getAuthor().getId())) {
                            return Operation.IGNORED;
                        }

                        //Replace prefix because people seem to think you have to add the prefix before saying yes.
                        String c = ie.getMessage().getContentRaw();
                        for (String s : ctx.getConfig().prefix) {
                            if (c.toLowerCase().startsWith(s)) {
                                c = c.substring(s.length());
                            }
                        }

                        String guildCustomPrefix = ctx.getDBGuild().getData().getGuildCustomPrefix();
                        if (guildCustomPrefix != null && !guildCustomPrefix.isEmpty() && c.toLowerCase().startsWith(guildCustomPrefix)) {
                            c = c.substring(guildCustomPrefix.length());
                        }
                        //End of prefix replacing.

                        if (c.equalsIgnoreCase("yes")) {
                            Player p = ctx.getPlayer();
                            final DBUser user = ctx.getDBUser();
                            final UserData userData = user.getData();

                            if (p.getCurrentMoney() < valuePayment) {
                                ctx.sendLocalized("commands.waifu.unclaim.not_enough_money", EmoteReference.ERROR);
                                return Operation.COMPLETED;
                            }

                            if (p.isLocked()) {
                                ctx.sendLocalized("commands.waifu.unclaim.player_locked", EmoteReference.ERROR);
                                return Operation.COMPLETED;
                            }

                            p.removeMoney(valuePayment);
                            userData.getWaifus().remove(userId);
                            user.save();
                            p.save();

                            ctx.sendLocalized("commands.waifu.unclaim.success", EmoteReference.CORRECT, name, valuePayment);
                            return Operation.COMPLETED;
                        } else if (c.equalsIgnoreCase("no")) {
                            ctx.sendLocalized("commands.waifu.unclaim.scrapped", EmoteReference.CORRECT);
                            return Operation.COMPLETED;
                        }

                        return Operation.IGNORED;
                    });
                });
            }
        });

        waifu.addSubCommand("buyslot", new SubCommand() {
            @Override
            public String description() {
                return "Buys a new waifu slot. Maximum slots are 30, costs get increasingly higher.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                int baseValue = 3000;

                DBUser user = ctx.getDBUser();
                Player player = ctx.getPlayer();
                final UserData userData = user.getData();

                if (player.getData().isWaifuout()) {
                    ctx.sendLocalized("commands.waifu.optout.notice", EmoteReference.ERROR);
                    return;
                }

                int currentSlots = userData.getWaifuSlots();
                int baseMultiplier = (currentSlots / 3) + 1;
                int finalValue = baseValue * baseMultiplier;

                if (player.isLocked()) {
                    ctx.sendLocalized("commands.waifu.buyslot.locked", EmoteReference.ERROR);
                    return;
                }

                if (player.getCurrentMoney() < finalValue) {
                    ctx.sendLocalized("commands.waifu.buyslot.not_enough_money", EmoteReference.ERROR, finalValue);
                    return;
                }

                if (userData.getWaifuSlots() >= 30) {
                    ctx.sendLocalized("commands.waifu.buyslot.too_many", EmoteReference.ERROR);
                    return;
                }

                player.removeMoney(finalValue);
                userData.setWaifuSlots(currentSlots + 1);
                user.save();
                player.save();

                ctx.sendLocalized("commands.waifu.buyslot.success",
                        EmoteReference.CORRECT, finalValue, userData.getWaifuSlots(), (userData.getWaifuSlots() - userData.getWaifus().size())
                );
            }
        });
    }

    static Waifu calculateWaifuValue(final User user) {
        final var db = MantaroData.db();
        var waifuPlayer = db.getPlayer(user);
        var waifuPlayerData = waifuPlayer.getData();
        var waifuUserData = db.getUser(user).getData();

        var waifuValue = WAIFU_BASE_VALUE;
        long performance;
        // For every 135000 money owned, it increases by 7% base value (base: 1300)
        // For every 3 badges, it increases by 17% base value.
        // For every 2780 experience, the value increases by 18% of the base value.
        // After all those calculations are complete,
        // the value then is calculated using final * (reputation scale / 10) where reputation scale goes up by 1 every 10 reputation points.
        // For every 3 waifu claims, the final value increases by 5% of the base value.
        // Maximum waifu value is Integer.MAX_VALUE.

        //Money calculation.
        long moneyValue = Math.round(Math.max(1, (int) (waifuPlayer.getCurrentMoney() / 135000)) * calculatePercentage(6));
        //Badge calculation.
        long badgeValue = Math.round(Math.max(1, (waifuPlayerData.getBadges().size() / 3)) * calculatePercentage(17));
        //Experience calculator.
        long experienceValue = Math.round(Math.max(1, (int) (waifuPlayer.getData().getExperience() / 2780)) * calculatePercentage(18));
        //Claim calculator.
        long claimValue = Math.round(Math.max(1, (waifuUserData.getTimesClaimed() / 3)) * calculatePercentage(5));

        //"final" value
        waifuValue += moneyValue + badgeValue + experienceValue + claimValue;

        // what is this lol
        // After all those calculations are complete, the value then is calculated using final *
        // (reputation scale / 20) where reputation scale goes up by 1 every 10 reputation points.
        // At 6000 reputation points, the waifu value gets multiplied by 1.1. This is the maximum amount it can be multiplied to.
        // to implement later: Reputation scaling is capped at 3.9k. Then at 6.5k the multiplier is applied.
        var reputation = waifuPlayer.getReputation();
        var reputationScaling = (reputation / 4.5) / 30;
        var finalValue = (long) (
                Math.min (
                        Integer.MAX_VALUE,
                        (waifuValue * (reputationScaling > 1 ? reputationScaling : 1) * (reputation > 6500 ? 1.1 : 1))
                )
        );

        var divide = (int) (moneyValue / 1300);
        performance = ((waifuValue - (WAIFU_BASE_VALUE + 450)) + (long)
                ((reputationScaling > 1 ? reputationScaling : 1) * 1.2)) / (divide > 1 ? divide : 3);

        //possible?
        if (performance < 0) {
            performance = 0;
        }


        return new Waifu(moneyValue, badgeValue, experienceValue, reputationScaling, claimValue, finalValue, performance);
    }

    //Yes, I had to do it, fuck.
    private static long calculatePercentage(long percentage) {
        return (percentage * WAIFU_BASE_VALUE) / 100;
    }
}
