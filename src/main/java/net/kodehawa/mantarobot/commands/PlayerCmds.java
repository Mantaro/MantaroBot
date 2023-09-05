/*
 * Copyright (C) 2016 Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import net.kodehawa.mantarobot.commands.currency.item.ItemHelper;
import net.kodehawa.mantarobot.commands.currency.item.ItemReference;
import net.kodehawa.mantarobot.commands.currency.item.PlayerEquipment;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.Breakable;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.Category;
import net.kodehawa.mantarobot.core.command.meta.Defer;
import net.kodehawa.mantarobot.core.command.meta.Description;
import net.kodehawa.mantarobot.core.command.meta.Help;
import net.kodehawa.mantarobot.core.command.meta.Name;
import net.kodehawa.mantarobot.core.command.meta.Options;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.listeners.operations.ButtonOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.InteractiveOperation;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.awt.Color;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Module
public class PlayerCmds {
    private static final IncreasingRateLimiter repRatelimiter = new IncreasingRateLimiter.Builder()
            .limit(1)
            .cooldown(12, TimeUnit.HOURS)
            .maxCooldown(12, TimeUnit.HOURS)
            .pool(MantaroData.getDefaultJedisPool())
            .randomIncrement(false)
            .prefix("rep")
            .build();

    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(Reputation.class);
        cr.registerSlash(Equip.class);
        cr.registerSlash(Unequip.class);
        cr.registerSlash(Badges.class);
    }

    @Defer
    @Description("Gives 1 reputation to a user")
    @Category(CommandCategory.CURRENCY)
    @Options({
            @Options.Option(type = OptionType.USER, name = "user", description = "The user to give a reputation point to.", required = true),
            @Options.Option(type = OptionType.BOOLEAN, name = "check", description = "Check if you can give a reputation point.")
    })
    @Help(
            description = "Gives a reputation point to a user. This command is only usable every 12 hours.",
            usage = "`/reputation user:<user> check:[true/false]` - Give a reputation point to a specified user. Use check if you wanna check if you can give it.",
            parameters = {
                    @Help.Parameter(name = "user", description = "The user to give reputation to"),
                    @Help.Parameter(name = "check", description = "Check if you can give reputation", optional = true)
            }
    )
    public static class Reputation extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var rl = repRatelimiter.getRemaniningCooldown(ctx.getAuthor());
            var lang = ctx.getLanguageContext();

            if (ctx.getOptionAsBoolean("check")) {
                ctx.send(String.format(lang.get("commands.rep.no_mentions"), EmoteReference.ERROR,
                                (rl > 0 ? String.format(lang.get("commands.rep.cooldown.waiting"),
                                        Utils.formatDuration(lang, rl)) : lang.get("commands.rep.cooldown.pass"))
                        )
                );

                return;
            }

            var usr = ctx.getOptionAsUser("user");
            if (usr == null) {
                ctx.reply("general.slash_member_lookup_failure", EmoteReference.ERROR);
                return;
            }

            var author = ctx.getAuthor();

            // Didn't want to repeat the code twice, lol.
            if (!Utils.isAccountOldEnough(usr, 30, ChronoUnit.DAYS)) {
                ctx.reply("commands.rep.new_account_notice", EmoteReference.ERROR);
                return;
            }

            if (!Utils.isAccountOldEnough(author, 30, ChronoUnit.DAYS)) {
                ctx.reply("commands.rep.new_account_notice", EmoteReference.ERROR);
                return;
            }

            if (usr.isBot()) {
                ctx.reply(String.format(lang.get("commands.rep.rep_bot"), EmoteReference.THINKING,
                                (rl > 0 ? String.format(lang.get("commands.rep.cooldown.waiting"), Utils.formatDuration(lang, rl))
                                        : lang.get("commands.rep.cooldown.pass"))
                        )
                );

                return;
            }

            if (usr.equals(ctx.getAuthor())) {
                ctx.reply(String.format(lang.get("commands.rep.rep_yourself"), EmoteReference.THINKING,
                                (rl > 0 ? String.format(lang.get("commands.rep.cooldown.waiting"), Utils.formatDuration(lang, rl))
                                        : lang.get("commands.rep.cooldown.pass"))
                        )
                );

                return;
            }

            if (ctx.isUserBlacklisted(usr.getId())) {
                ctx.reply("commands.rep.blacklisted_rep", EmoteReference.ERROR);
                return;
            }

            if (!RatelimitUtils.ratelimit(repRatelimiter, ctx, lang.get("commands.rep.cooldown.explanation"), false)) {
                return;
            }

            var player = ctx.getPlayer(usr);
            player.addReputation(1L);
            player.updateAllChanged();

            ctx.reply("commands.rep.success", EmoteReference.CORRECT, usr.getAsMention());
        }
    }

    @Defer
    @Description("Equips an item into a slot.")
    @Category(CommandCategory.CURRENCY)
    @Options({
            @Options.Option(type = OptionType.STRING, name = "item", description = "The item to equip.", required = true)
    })
    @Help(
            description = "Equips an item into a slot.",
            usage = "`/equip item:<item name>`",
            parameters = {
                    @Help.Parameter(name = "item", description = "The item to equip.")
            }
    )
    public static class Equip extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var content = ctx.getOptionAsString("item");
            var item = ItemHelper.fromAnyNoId(content.replace("\"", ""), ctx.getLanguageContext())
                    .orElse(null);

            var player = ctx.getPlayer();
            var dbUser = ctx.getDBUser();
            if (item == null) {
                ctx.reply("commands.profile.equip.no_item", EmoteReference.ERROR);
                return;
            }

            var containsItem = player.containsItem(item);
            if (!containsItem) {
                ctx.reply("commands.profile.equip.not_owned", EmoteReference.ERROR);
                return;
            }

            var equipment = dbUser.getEquippedItems();

            var proposedType = equipment.getTypeFor(item);
            if (equipment.getEquipment().containsKey(proposedType)) {
                ctx.reply("commands.profile.equip.already_equipped", EmoteReference.ERROR);
                return;
            }

            if (equipment.equipItem(item)) {
                if (item == ItemReference.HELLFIRE_PICK)
                    player.addBadgeIfAbsent(Badge.HOT_MINER);
                if (item == ItemReference.HELLFIRE_ROD)
                    player.addBadgeIfAbsent(Badge.HOT_FISHER);
                if (item == ItemReference.HELLFIRE_AXE)
                    player.addBadgeIfAbsent(Badge.HOT_CHOPPER);

                player.processItem(item, -1);
                player.updateAllChanged();

                equipment.updateAllChanged(dbUser);
                ctx.reply("commands.profile.equip.success", EmoteReference.CORRECT, item.getEmoji(), item.getName());
            } else {
                ctx.reply("commands.profile.equip.not_suitable", EmoteReference.ERROR);
            }
        }
    }

    @Defer
    @Description("Unequips an item from a slot.")
    @Category(CommandCategory.CURRENCY)
    @Options({
            @Options.Option(
                    type = OptionType.STRING,
                    name = "item",
                    description = "The item to unequip.",
                    required = true,
                    choices = {
                            @Options.Choice(description = "Equipped Pickaxe", value = "pick"),
                            @Options.Choice(description = "Equipped Axe", value = "axe"),
                            @Options.Choice(description = "Equipped Fishing Rod", value = "rod"),
                            @Options.Choice(description = "Equipped Wrench", value = "wrench")
                    }
            )
    })
    @Help(
            description = "Unequips an item from a slot.",
            usage = "`/unequip item:<pick/rod/axe/wrench>`",
            parameters = {
                    @Help.Parameter(name = "item", description = "The item to unequip.")
            }
    )
    public static class Unequip extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var content = ctx.getOptionAsString("item");
            var dbUser = ctx.getDBUser();
            var equipment = dbUser.getEquippedItems();
            var type = PlayerEquipment.EquipmentType.fromString(content);
            if (type == null) {
                ctx.sendLocalized("commands.profile.unequip.invalid_type", EmoteReference.ERROR);
                return;
            }

            var equipped = equipment.getEquipment().get(type);
            if (equipped == null) {
                ctx.sendLocalized("commands.profile.unequip.not_equipped", EmoteReference.ERROR);
                return;
            }

            var equippedItem = ItemHelper.fromId(equipped);
            var lang = ctx.getLanguageContext();

            var message = ctx.sendResult(lang.get("commands.profile.unequip.confirm").formatted(EmoteReference.WARNING, equippedItem.getEmoji(), equippedItem.getName()));
            ButtonOperations.create(message, ctx.getAuthor().getIdLong(), event -> {
                var author = event.getUser();
                if (author.getIdLong() != ctx.getAuthor().getIdLong()) {
                    return InteractiveOperation.IGNORED;
                }

                var button = event.getButton();
                if (button.getId() == null) {
                    return Operation.IGNORED;
                }

                InteractionHook hook = event.getHook();
                if (button.getId().equalsIgnoreCase("yes")) {
                    var dbUserFinal = ctx.getDBUser(author);
                    var playerFinal = ctx.getPlayer(author);
                    var equipmentFinal = dbUserFinal.getEquippedItems();
                    var equippedFinal = equipmentFinal.getEquipment().get(type);
                    if (equippedFinal == null) {
                        hook.editOriginal(lang.get("commands.profile.unequip.not_equipped").formatted(EmoteReference.ERROR))
                                .setComponents()
                                .queue();
                        return InteractiveOperation.COMPLETED;
                    }

                    var equippedItemFinal = ItemHelper.fromId(equippedFinal);
                    var part = ""; //Start as an empty string.
                    if (equippedItemFinal instanceof Breakable item) {
                        // Gotta check again, just in case...

                        var percentage = ((float) equipmentFinal.getDurability().get(type) / (float) item.getMaxDurability()) * 100.0f;
                        if (percentage == 100) { //Basically never used
                            playerFinal.processItem(equippedItemFinal, 1);
                            part += String.format(
                                    lang.get("commands.profile.unequip.equipment_recover"), equippedItemFinal.getName()
                            );
                        } else {
                            var brokenItem = ItemHelper.getBrokenItemFrom(equippedItemFinal);
                            if (brokenItem != null) {
                                playerFinal.processItem(brokenItem, 1);
                                part += String.format(
                                        lang.get("commands.profile.unequip.broken_equipment_recover"), brokenItem.getName()
                                );
                            } else {
                                long money = equippedItemFinal.getValue() / 2;
                                //Brom's Pickaxe, Diamond Pickaxe and normal rod and Diamond Rod will hit this condition.
                                part += String.format(
                                        lang.get("commands.profile.unequip.broken_equipment_recover_none"), money
                                );
                            }
                        }
                    }

                    equipmentFinal.resetOfType(type);
                    equipmentFinal.updateAllChanged(dbUserFinal);
                    playerFinal.updateAllChanged();

                    hook.editOriginal(lang.get("commands.profile.unequip.success").formatted(EmoteReference.CORRECT, type.name().toLowerCase()) + part)
                            .setComponents()
                            .queue();
                } else if (button.getId().equalsIgnoreCase("no")) {
                    hook.editOriginal(lang.get("commands.profile.unequip.cancelled").formatted(EmoteReference.WARNING))
                            .setComponents()
                            .queue();
                    return InteractiveOperation.COMPLETED;
                }

                return InteractiveOperation.IGNORED;
            }, Button.danger("yes", ctx.getLanguageContext().get("buttons.yes")), Button.primary("no", ctx.getLanguageContext().get("buttons.no")));
        }
    }

    @Description("The hub for badge-related commands")
    @Category(CommandCategory.CURRENCY)
    public static class Badges extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {}

        @Description("Show your badge list, or someone else's badge list.")
        @Category(CommandCategory.CURRENCY)
        @Options({
                @Options.Option(type = OptionType.USER, name = "user", description = "The user to see the badges of."),
                @Options.Option(type = OptionType.BOOLEAN, name = "brief", description = "Whether to see it in brief format.")
        })
        @Help(description = "Shows your badge list, or someone else's list.",
                usage = "`/badges show user:[user] brief:[true/false]`",
                parameters = {
                        @Help.Parameter(name = "user", description = "The user to check. If none, you.", optional = true),
                        @Help.Parameter(name = "brief", description = "Whether to show this in brief format. Default is false.", optional = true),
                }
        )
        public static class Show extends SlashCommand {
            final Random r = new Random();
            @Override
            protected void process(SlashContext ctx) {
                var toLookup = ctx.getOptionAsUser("user", ctx.getAuthor());
                var member = ctx.getGuild().getMember(toLookup);
                var player = ctx.getPlayer(toLookup);
                var dbUser = ctx.getDBUser();

                if (ctx.getOptionAsBoolean("brief")) {
                    ctx.sendLocalized("commands.badges.brief_success", member.getEffectiveName(),
                            player.getBadges().stream()
                                    .sorted()
                                    .map(Badge::getDisplay)
                                    .collect(Collectors.joining(", "))
                    );

                    return;
                }

                var badges = player.getBadges();
                Collections.sort(badges);

                var lang = ctx.getLanguageContext();
                var embed = new EmbedBuilder()
                        .setAuthor(String.format(lang.get("commands.badges.header"), toLookup.getName()))
                        .setColor(ctx.getMemberColor())
                        .setThumbnail(toLookup.getEffectiveAvatarUrl());
                List<MessageEmbed.Field> fields = new LinkedList<>();

                for (var b : badges) {
                    //God DAMNIT discord, I want it to look cute, stop trimming my spaces.
                    fields.add(
                            new MessageEmbed.Field(b.toString(), "**\u2009\u2009\u2009\u2009- " + b.description + "**", false)
                    );
                }

                if (badges.isEmpty()) {
                    embed.setDescription(lang.get("commands.badges.no_badges"));
                    ctx.send(embed.build());
                    return;
                }

                var common = lang.get("commands.badges.profile_notice") + lang.get("commands.badges.info_notice") +
                        ((r.nextInt(2) == 0 && !dbUser.isPremium() ? lang.get("commands.badges.donate_notice") : "\n") +
                                String.format(lang.get("commands.badges.total_badges"), badges.size())
                        );

                DiscordUtils.sendPaginatedEmbed(ctx.getUtilsContext(), embed, DiscordUtils.divideFields(6, fields), common);
            }
        }

        @Name("display")
        @Description("Sets your display badge.")
        @Options({@Options.Option(type = OptionType.STRING, name = "badge", description = "The badge to display, reset/none to reset it or no badge. If nothing is specified, it prints a list.")})
        @Help(
                description = "Sets your profile display badge.",
                usage = "`/badges display badge:[badge name]` - Use reset to reset the badge to the default one and use none to show no badge.",
                parameters = {@Help.Parameter(name = "badge", description = "The badge to use.")}
        )
        public static class DisplayBadge extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var player = ctx.getPlayer();
                var badgeString = ctx.getOptionAsString("badge", "");
                var badge = Badge.lookupFromString(badgeString);
                if (badge == null) {
                    ctx.replyEphemeral("commands.profile.displaybadge.no_such_badge", EmoteReference.ERROR,
                            player.getBadges().stream()
                                    .map(Badge::getDisplay)
                                    .collect(Collectors.joining(", "))
                    );

                    return;
                }

                if (badgeString.equalsIgnoreCase("none")) {
                    player.showBadge(false);
                    player.updateAllChanged();

                    ctx.replyEphemeral("commands.profile.displaybadge.reset_success", EmoteReference.CORRECT);
                    return;
                }

                if (badgeString.equalsIgnoreCase("reset")) {
                    player.mainBadge(null);
                    player.showBadge(true);
                    player.updateAllChanged();

                    ctx.replyEphemeral("commands.profile.displaybadge.important_success", EmoteReference.CORRECT);
                    return;
                }

                if (!player.getBadges().contains(badge)) {
                    ctx.replyEphemeral("commands.profile.displaybadge.player_missing_badge", EmoteReference.ERROR,
                            player.getBadges().stream()
                                    .map(Badge::getDisplay)
                                    .collect(Collectors.joining(", "))
                    );

                    return;
                }

                player.showBadge(true);
                player.mainBadge(badge);
                player.updateAllChanged();
                ctx.replyEphemeral("commands.profile.displaybadge.success", EmoteReference.CORRECT, badge.display);
            }
        }

        @Name("list")
        @Description("Lists all the obtainable badges.")
        @Category(CommandCategory.CURRENCY)
        public static class ListBadges extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var badges = Badge.values();
                var lang = ctx.getLanguageContext();
                var builder = new EmbedBuilder()
                        .setAuthor(lang.get("commands.badges.ls.header"),
                                null, ctx.getAuthor().getEffectiveAvatarUrl()
                        )
                        .setColor(Color.PINK)
                        .setFooter(lang.get("general.requested_by").formatted(ctx.getMember().getEffectiveName()), null);

                var player = ctx.getPlayer();
                List<MessageEmbed.Field> fields = new LinkedList<>();
                for (Badge badge : badges) {
                    if (!badge.isObtainable()) {
                        continue;
                    }

                    fields.add(new MessageEmbed.Field("%s\u2009\u2009\u2009%s".formatted(badge.unicode, badge.display),
                            badge.getDescription() + "\n" +
                                    String.format(lang.get("commands.badges.ls.obtained"), player.hasBadge(badge)),
                            false)
                    );
                }

                DiscordUtils.sendPaginatedEmbed(ctx.getUtilsContext(), builder, DiscordUtils.divideFields(7, fields), lang.get("commands.badges.ls.desc"));
            }
        }

        @Description("Shows info about a badge.")
        @Category(CommandCategory.CURRENCY)
        @Options({
                @Options.Option(type = OptionType.STRING, name = "badge", description = "The badge to check info for.", required = true)
        })
        public static class Info extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var content = ctx.getOptionAsString("badge");
                var badge = Badge.lookupFromString(content);
                if (badge == null || badge == Badge.DJ) {
                    ctx.reply("commands.badges.info.not_found", EmoteReference.ERROR);
                    return;
                }

                var player = ctx.getPlayer();
                var lang = ctx.getLanguageContext();
                ctx.getEvent().replyEmbeds(new EmbedBuilder()
                        .setAuthor(String.format(lang.get("commands.badges.info.header"), badge.display))
                        .setDescription(String.join("\n",
                                        EmoteReference.BLUE_SMALL_MARKER + "**" + lang.get("general.name") + ":** " + badge.display,
                                        EmoteReference.BLUE_SMALL_MARKER + "**" + lang.get("general.description") + ":** " + badge.description,
                                        EmoteReference.BLUE_SMALL_MARKER + "**" +
                                                lang.get("commands.badges.info.achieved") + ":** " +
                                                player.getBadges().stream().anyMatch(b -> b == badge)
                                )
                        )
                        .setThumbnail("attachment://icon.png")
                        .setColor(Color.CYAN)
                        .build()
                    ).addFiles(FileUpload.fromData(badge.icon, "icon.png"))
                    .queue();
            }
        }
    }
}
