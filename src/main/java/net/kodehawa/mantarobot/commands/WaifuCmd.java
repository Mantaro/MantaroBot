/*
 * Copyright (C) 2016-2021 David Rubio Escares / Kodehawa
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
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.Button;
import net.kodehawa.mantarobot.commands.currency.Waifu;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.Category;
import net.kodehawa.mantarobot.core.command.meta.Description;
import net.kodehawa.mantarobot.core.command.meta.Name;
import net.kodehawa.mantarobot.core.command.meta.Options;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.listeners.operations.ButtonOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@Module
public class WaifuCmd {
    private static final long WAIFU_BASE_VALUE = 1000L;
    private static final IncreasingRateLimiter waifuRatelimiter = new IncreasingRateLimiter.Builder()
            .limit(1)
            .spamTolerance(2)
            .cooldown(5, TimeUnit.SECONDS)
            .maxCooldown(5, TimeUnit.SECONDS)
            .randomIncrement(true)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("waifu")
            .build();

    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(WaifuCommand.class);
    }

    //TODO: Add help.
    @Name("waifu")
    @Category(CommandCategory.CURRENCY)
    @Description("Several waifu-related commands.")
    public static class WaifuCommand extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
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

            // This is an empty command, as slash commands can't have a parent command if there's subcommands.
        }

        @Override
        public Predicate<SlashContext> getPredicate() {
            return ctx -> {
                ctx.defer();
                return RatelimitUtils.ratelimit(waifuRatelimiter, ctx, false);
            };
        }

        @Name("list")
        @Description("Show a list of all your waifu(s) and their value.")
        @Options({
                @Options.Option(type = OptionType.BOOLEAN, name = "id", description = "Show IDs")
        })
        public static class ListCommand extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                // Default call will bring out the waifu list.
                final var dbUser = ctx.getDBUser();
                final var userData = dbUser.getData();
                final var player = ctx.getPlayer();
                final var lang = ctx.getLanguageContext();

                if (player.getData().isWaifuout()) {
                    ctx.reply("commands.waifu.optout.notice", EmoteReference.ERROR);
                    return;
                }

                if (!ctx.getSelfMember().hasPermission(ctx.getChannel(), Permission.MESSAGE_EMBED_LINKS)) {
                    ctx.reply("general.missing_embed_permissions");
                    return;
                }

                final var description = userData.getWaifus().isEmpty() ?
                        lang.get("commands.waifu.waifu_header") + "\n" + lang.get("commands.waifu.no_waifu") :
                        lang.get("commands.waifu.waifu_header");


                final var waifusEmbed = new EmbedBuilder()
                        .setAuthor(lang.get("commands.waifu.header"), null, ctx.getAuthor().getEffectiveAvatarUrl())
                        .setThumbnail("https://i.imgur.com/2JlMtCe.png")
                        .setColor(Color.CYAN)
                        .setFooter(lang.get("commands.waifu.footer").formatted(
                                        userData.getWaifus().size(),
                                        userData.getWaifuSlots() - userData.getWaifus().size()),
                                null
                        );

                if (userData.getWaifus().isEmpty()) {
                    waifusEmbed.setDescription(description);
                    ctx.send(waifusEmbed.build());
                    return;
                }

                final var id = ctx.getOptionAsBoolean("id");
                List<String> toRemove = new ArrayList<>();
                List<MessageEmbed.Field> fields = new LinkedList<>();

                for (String waifu : userData.getWaifus().keySet()) {
                    //This fixes the issue of cross-node waifus not appearing.
                    User user = ctx.retrieveUserById(waifu);
                    if (user == null) {
                        fields.add(new MessageEmbed.Field(
                                "%sUnknown User (ID: %s)".formatted(EmoteReference.BLUE_SMALL_MARKER, waifu),
                                lang.get("commands.waifu.value_format") + " unknown\n" +
                                        lang.get("commands.waifu.value_b_format") + " " + userData.getWaifus().get(waifu) +
                                        lang.get("commands.waifu.credits_format"), false)
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
                                (id ? lang.get("commands.waifu.id") + " " + user.getId() + "\n" : "") +
                                        lang.get("commands.waifu.value_format") + " " +
                                        waifuClaimed.getData().getWaifuCachedValue() + " " +
                                        lang.get("commands.waifu.credits_format") + "\n" +
                                        lang.get("commands.waifu.value_b_format") + " " + userData.getWaifus().get(waifu) +
                                        lang.get("commands.waifu.credits_format"), false)
                        );
                    }
                }

                final var toSend = lang.get("commands.waifu.description_header").formatted(userData.getWaifuSlots()) + description;
                DiscordUtils.sendPaginatedEmbed(ctx.getUtilsContext(), waifusEmbed, DiscordUtils.divideFields(4, fields), toSend);

                if (!toRemove.isEmpty()) {
                    for(String remove : toRemove) {
                        dbUser.getData().getWaifus().remove(remove);
                    }

                    player.save();
                }
            }
        }

        @Description("Opt-out of the waifu stuff. This will disable the waifu system permanently.")
        public static class OptOut extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                final var player = ctx.getPlayer();
                if (player.getData().isWaifuout()) {
                    ctx.reply("commands.waifu.optout.notice", EmoteReference.ERROR);
                    return;
                }

                var message = ctx.sendResult(ctx.getLanguageContext().get("commands.waifu.optout.warning").formatted(EmoteReference.WARNING));
                ButtonOperations.create(message, 60, e -> {
                    if (!e.getUser().getId().equals(ctx.getAuthor().getId())) {
                        return Operation.IGNORED;
                    }

                    if (e.getButton() == null) {
                        return Operation.IGNORED;
                    }
                    final var button = e.getButton().getId();

                    if (button.equals("yes")) {
                        player.getData().setWaifuout(true);
                        ctx.edit("commands.waifu.optout.success", EmoteReference.CORRECT);
                        player.saveUpdating();
                        return Operation.COMPLETED;
                    } else if (button.equals("no")) {
                        ctx.edit("commands.waifu.optout.cancelled", EmoteReference.CORRECT);
                        return Operation.COMPLETED;
                    }

                    return Operation.IGNORED;
                    // TODO: Maybe double-check? This might be cursed with buttons.
                    // Well, old one was cursed if you didn't speak english...
                }, Button.danger("yes", ctx.getLanguageContext().get("commands.waifu.optout.yes_button")),
                        Button.primary("no", ctx.getLanguageContext().get("commands.waifu.optout.no_button")));
            }
        }

        @Description("Claim a waifu. Yeah, this is all fiction.")
        @Options({
                @Options.Option(type = OptionType.USER, name = "user", description = "The user to claim.", required = true)
        })
        public static class Claim extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                final var player = ctx.getPlayer();
                if (player.getData().isWaifuout()) {
                    ctx.reply("commands.waifu.optout.notice", EmoteReference.ERROR);
                    return;
                }

                final var toLookup = ctx.getOptionAsUser("user");
                if (toLookup.isBot()) {
                    ctx.reply("commands.waifu.bot", EmoteReference.ERROR);
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
                    ctx.reply("commands.waifu.optout.claim_notice", EmoteReference.ERROR);
                    return;
                }

                //Waifu object declaration.
                final Waifu waifuToClaim = calculateWaifuValue(claimedPlayer, toLookup);
                final long waifuFinalValue = waifuToClaim.getFinalValue();

                //Checks.
                if (toLookup.getIdLong() == ctx.getAuthor().getIdLong()) {
                    ctx.reply("commands.waifu.claim.yourself", EmoteReference.ERROR);
                    return;
                }

                if (claimerUser.getData().getWaifus().entrySet().stream().anyMatch((w) -> w.getKey().equals(toLookup.getId()))) {
                    ctx.reply("commands.waifu.claim.already_claimed", EmoteReference.ERROR);
                    return;
                }

                //If the to-be claimed has the claim key in their inventory, it cannot be claimed.
                if (claimedPlayerData.isClaimLocked()) {
                    ctx.reply("commands.waifu.claim.key_locked", EmoteReference.ERROR);
                    return;
                }

                if (claimerPlayer.isLocked()) {
                    ctx.reply("commands.waifu.claim.locked", EmoteReference.ERROR);
                    return;
                }

                //Deduct from balance and checks for money.
                if (!claimerPlayer.removeMoney(waifuFinalValue)) {
                    ctx.reply("commands.waifu.claim.not_enough_money", EmoteReference.ERROR, waifuFinalValue);
                    return;
                }

                if (claimerUserData.getWaifus().size() >= claimerUserData.getWaifuSlots()) {
                    ctx.reply("commands.waifu.claim.not_enough_slots",
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

                boolean badgesAdded = false;
                //Add badges
                if (claimedUserData.getWaifus().containsKey(ctx.getAuthor().getId()) && claimerUserData.getWaifus().containsKey(toLookup.getId())) {
                    claimerPlayerData.addBadgeIfAbsent(Badge.MUTUAL);
                    badgesAdded = claimedPlayerData.addBadgeIfAbsent(Badge.MUTUAL);
                }

                claimerPlayerData.addBadgeIfAbsent(Badge.WAIFU_CLAIMER);
                if (badgesAdded || claimedPlayerData.addBadgeIfAbsent(Badge.CLAIMED)) {
                    claimedPlayer.saveAsync();
                }

                //Massive saving operation owo.
                claimerPlayer.save();
                claimedUser.save();
                claimerUser.save();

                //Send confirmation message
                ctx.reply("commands.waifu.claim.success",
                        EmoteReference.CORRECT, toLookup.getName(), waifuFinalValue, claimerUserData.getWaifus().size()
                );
            }
        }

        @Description("Un-claims a waifu.")
        @Options({
                @Options.Option(type = OptionType.USER, name = "user", description = "The user to unclaim. If unknown, use the id."),
                @Options.Option(type = OptionType.USER, name = "id", description = "The user id of the user to unclaim.")
        })
        public static class Unclaim extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                final var player = ctx.getPlayer();
                if (player.getData().isWaifuout()) {
                    ctx.reply("commands.waifu.optout.notice", EmoteReference.ERROR);
                    return;
                }

                final var user = ctx.getOptionAsUser("user");
                final var id = ctx.getOptionAsString("id");
                final var isId = !id.isBlank();
                if (user == null && !isId) {
                    ctx.reply("commands.waifu.unclaim.no_user", EmoteReference.ERROR);
                    return;
                }

                final var toLookup = isId ? ctx.retrieveUserById(id) : user;
                final var isUnknown = isId && user == null;
                if (toLookup == null && !isUnknown) {
                    ctx.reply("commands.waifu.unclaim.not_found", EmoteReference.ERROR);
                    return;
                }

                //It'll only be null if -unknown is passed with an unknown ID. This is unclaim, so this check is a bit irrelevant though.
                if (!isUnknown && toLookup.isBot()) {
                    ctx.reply("commands.waifu.bot", EmoteReference.ERROR);
                    return;
                }

                final var userId = isUnknown ? id : toLookup.getId();
                final var name = isUnknown ? "Unknown User" : toLookup.getName();
                final var claimerUser = ctx.getDBUser();
                final var data = claimerUser.getData();
                final var value = data.getWaifus().get(userId);

                if (value == null) {
                    ctx.reply("commands.waifu.not_claimed", EmoteReference.ERROR);
                    return;
                }

                final var claimedPlayer = ctx.getPlayer(toLookup);
                final var currentValue = calculateWaifuValue(claimedPlayer, toLookup).getFinalValue();
                final var valuePayment = (long) (currentValue * 0.15);
                //Send confirmation message.
                var message = ctx.sendResult(ctx.getLanguageContext().get("commands.waifu.unclaim.confirmation").formatted(EmoteReference.MEGA, name, valuePayment, EmoteReference.STOPWATCH));
                ButtonOperations.create(message, 60, (ie) -> {
                    if (!ie.getUser().getId().equals(ctx.getAuthor().getId())) {
                        return Operation.IGNORED;
                    }

                    var button = ie.getButton();
                    if (button == null) {
                        return Operation.IGNORED;
                    }

                    if (button.getId().equals("yes")) {
                        final var p = ctx.getPlayer();
                        final var dbUser = ctx.getDBUser();
                        final var userData = dbUser.getData();

                        if (p.getCurrentMoney() < valuePayment) {
                            ctx.edit("commands.waifu.unclaim.not_enough_money", EmoteReference.ERROR);
                            return Operation.COMPLETED;
                        }

                        if (p.isLocked()) {
                            ctx.edit("commands.waifu.unclaim.player_locked", EmoteReference.ERROR);
                            return Operation.COMPLETED;
                        }

                        p.removeMoney(valuePayment);
                        userData.getWaifus().remove(userId);
                        dbUser.save();
                        p.save();

                        ctx.edit("commands.waifu.unclaim.success", EmoteReference.CORRECT, name, valuePayment);
                        return Operation.COMPLETED;
                    } else if (button.getId().equals("no")) {
                        ctx.edit("commands.waifu.unclaim.scrapped", EmoteReference.CORRECT);
                        return Operation.COMPLETED;
                    }

                    return Operation.IGNORED;
                }, Button.primary("yes", ctx.getLanguageContext().get("buttons.yes")), Button.primary("no", ctx.getLanguageContext().get("buttons.no")));
            }
        }

        @Description("Buys a new waifu slot. Maximum slots are 30, costs get increasingly higher.")
        public static class BuySlot extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                final var baseValue = 3000;
                final var user = ctx.getDBUser();
                final var player = ctx.getPlayer();
                final var userData = user.getData();

                if (player.getData().isWaifuout()) {
                    ctx.reply("commands.waifu.optout.notice", EmoteReference.ERROR);
                    return;
                }

                final var currentSlots = userData.getWaifuSlots();
                final var baseMultiplier = (currentSlots / 3) + 1;
                final var finalValue = baseValue * baseMultiplier;
                if (player.isLocked()) {
                    ctx.reply("commands.waifu.buyslot.locked", EmoteReference.ERROR);
                    return;
                }

                if (player.getCurrentMoney() < finalValue) {
                    ctx.reply("commands.waifu.buyslot.not_enough_money", EmoteReference.ERROR, finalValue);
                    return;
                }

                if (userData.getWaifuSlots() >= 30) {
                    ctx.reply("commands.waifu.buyslot.too_many", EmoteReference.ERROR);
                    return;
                }

                player.removeMoney(finalValue);
                userData.setWaifuSlots(currentSlots + 1);
                user.save();
                player.save();

                ctx.reply("commands.waifu.buyslot.success",
                        EmoteReference.CORRECT, finalValue, userData.getWaifuSlots(), (userData.getWaifuSlots() - userData.getWaifus().size())
                );
            }
        }

        @Description("Shows your waifu stats or the stats or someone else's.")
        @Options({
                @Options.Option(type = OptionType.USER, name = "user", description = "The user to check stats for. Yourself, if nothing specified.")
        })
        public static class Stats extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                final var player = ctx.getPlayer();
                final var playerData = player.getData();
                final var lang = ctx.getLanguageContext();

                if (playerData.isWaifuout()) {
                    ctx.reply("commands.waifu.optout.notice", EmoteReference.ERROR);
                    return;
                }

                if (!ctx.getSelfMember().hasPermission(ctx.getChannel(), Permission.MESSAGE_EMBED_LINKS)) {
                    ctx.reply("general.missing_embed_permissions");
                    return;
                }

                final var toLookup = ctx.getOptionAsUser("user", ctx.getAuthor());
                if (toLookup.isBot()) {
                    ctx.reply("commands.waifu.bot", EmoteReference.ERROR);
                    return;
                }

                final var waifuClaimed = ctx.getPlayer(toLookup);
                if (waifuClaimed.getData().isWaifuout()) {
                    ctx.reply("commands.waifu.optout.lookup_notice", EmoteReference.ERROR);
                    return;
                }

                final var waifuStats = calculateWaifuValue(waifuClaimed, toLookup);
                final var finalValue = waifuStats.getFinalValue();

                EmbedBuilder statsBuilder = new EmbedBuilder()
                        .setThumbnail(toLookup.getEffectiveAvatarUrl())
                        .setAuthor(toLookup == ctx.getAuthor() ?
                                        lang.get("commands.waifu.stats.header") :
                                        lang.get("commands.waifu.stats.header_other").formatted(toLookup.getName()),
                                null, toLookup.getEffectiveAvatarUrl()
                        ).setColor(Color.PINK)
                        .setDescription(lang.get("commands.waifu.stats.format").formatted(
                                EmoteReference.BLUE_SMALL_MARKER,
                                waifuStats.getMoneyValue(),
                                waifuStats.getBadgeValue(),
                                waifuStats.getExperienceValue(),
                                waifuStats.getClaimValue(),
                                waifuStats.getReputationMultiplier())
                        ).addField(EmoteReference.ZAP.toHeaderString() + lang.get("commands.waifu.stats.performance"),
                                waifuStats.getPerformance() + "wp", true
                        ).addField(EmoteReference.MONEY.toHeaderString() + lang.get("commands.waifu.stats.value"),
                                lang.get("commands.waifu.stats.credits").formatted(finalValue),
                                false
                        ).setFooter(lang.get("commands.waifu.notice"), null);

                ctx.reply(statsBuilder.build());
            }
        }
    }

    static Waifu calculateWaifuValue(final Player player, final User user) {
        final var db = MantaroData.db();
        final var waifuPlayerData = player.getData();
        final var waifuUserData = db.getUser(user).getData();

        var waifuValue = WAIFU_BASE_VALUE;
        long performance;
        // For every 135,000 money owned, it increases by 7% base value (base: 1300)
        // For every 3 badges, it increases by 17% base value.
        // For every 2,780 experience, the value increases by 18% of the base value.
        // After all those calculations are complete,
        // the value then is calculated using final * (reputation scale / 10) where reputation scale goes up by 1 every 10 reputation points.
        // For every 3 waifu claims, the final value increases by 5% of the base value.
        // Maximum waifu value is Integer.MAX_VALUE.

        //Money calculation.
        long moneyValue = Math.round(Math.max(1, (int) (player.getCurrentMoney() / 135000)) * calculatePercentage(6));
        //Badge calculation.
        long badgeValue = Math.round(Math.max(1, (waifuPlayerData.getBadges().size() / 3)) * calculatePercentage(17));
        //Experience calculator.
        long experienceValue = Math.round(Math.max(1, (int) (player.getData().getExperience() / 2780)) * calculatePercentage(18));
        //Claim calculator.
        long claimValue = Math.round(Math.max(1, (waifuUserData.getTimesClaimed() / 3)) * calculatePercentage(5));

        //"final" value
        waifuValue += moneyValue + badgeValue + experienceValue + claimValue;

        // what is this lol
        // After all those calculations are complete, the value then is calculated using final *
        // (reputation scale / 20) where reputation scale goes up by 1 every 10 reputation points.
        // At 6,500 reputation points, the waifu value gets multiplied by 1.1. This is the maximum amount it can be multiplied to.
        // to implement later: Reputation scaling is capped at 5k. Then at 6.5k the multiplier is applied.
        var reputation = player.getReputation();
        var reputationScale = reputation;
        if (reputation > 5000) {
            reputationScale = 5000L;
        }

        var reputationScaling = (reputationScale / 4.5) / 30;
        var finalValue = (long) (
                Math.min(Integer.MAX_VALUE, (waifuValue * (reputationScaling > 1 ? reputationScaling : 1) * (reputation > 6500 ? 1.1 : 1)))
        );

        var divide = (int) (moneyValue / 1300);
        performance = ((waifuValue - (WAIFU_BASE_VALUE + 450)) + (long) ((reputationScaling > 1 ? reputationScaling : 1) * 1.2)) / (divide > 1 ? divide : 3);

        //possible?
        if (performance < 0) {
            performance = 0;
        }


        return new Waifu(moneyValue, badgeValue, experienceValue, reputationScaling, claimValue, finalValue, performance);
    }

    // Yes, I had to do it, fuck.
    private static long calculatePercentage(long percentage) {
        return (percentage * WAIFU_BASE_VALUE) / 100;
    }
}
