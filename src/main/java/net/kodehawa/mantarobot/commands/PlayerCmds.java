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
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.commands.currency.item.ItemHelper;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.PlayerEquipment;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.Breakable;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.currency.seasons.helpers.UnifiedPlayer;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.InteractiveOperation;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.base.ITreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.PlayerData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.CustomFinderUtil;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.awt.Color;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Module
public class PlayerCmds {
    @Subscribe
    public void rep(CommandRegistry cr) {
        cr.register("rep", new SimpleCommand(CommandCategory.CURRENCY) {
            final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                    .limit(1)
                    .cooldown(12, TimeUnit.HOURS)
                    .maxCooldown(12, TimeUnit.HOURS)
                    .pool(MantaroData.getDefaultJedisPool())
                    .randomIncrement(false)
                    .prefix("rep")
                    .build();

            @Override
            public void call(Context ctx, String content, String[] args) {
                var rl = rateLimiter.getRemaniningCooldown(ctx.getAuthor());
                var languageContext = ctx.getLanguageContext();

                if (content.isEmpty()) {
                    ctx.send(String.format(languageContext.get("commands.rep.no_mentions"), EmoteReference.ERROR,
                            (rl > 0 ? String.format(languageContext.get("commands.rep.cooldown.waiting"),
                                    Utils.formatDuration(rl)) : languageContext.get("commands.rep.cooldown.pass"))
                            )
                    );

                    return;
                }

                var mentioned = ctx.getMentionedUsers();
                if (!mentioned.isEmpty() && mentioned.size() > 1) {
                    ctx.sendLocalized("commands.rep.more_than_one", EmoteReference.ERROR);
                    return;
                }


                ctx.findMember(content, ctx.getMessage()).onSuccess(members -> {
                    var member = CustomFinderUtil.findMember(content, members, ctx);
                    if (member == null) {
                        return;
                    }

                    var usr = member.getUser();
                    var author = ctx.getAuthor();
                    Predicate<User> oldEnough = (u -> u.getTimeCreated().isBefore(OffsetDateTime.now().minus(5, ChronoUnit.DAYS)));

                    //Didn't want to repeat the code twice, lol.
                    if (!oldEnough.test(usr)) {
                        ctx.sendLocalized("commands.rep.new_account_notice", EmoteReference.ERROR);
                        return;
                    }

                    if (!oldEnough.test(author)) {
                        ctx.sendLocalized("commands.rep.new_account_notice", EmoteReference.ERROR);
                        return;
                    }

                    if (usr.isBot()) {
                        ctx.send(String.format(languageContext.get("commands.rep.rep_bot"), EmoteReference.THINKING,
                                (rl > 0 ? String.format(languageContext.get("commands.rep.cooldown.waiting"), Utils.formatDuration(rl))
                                        : languageContext.get("commands.rep.cooldown.pass"))
                                )
                        );

                        return;
                    }

                    if (usr.equals(ctx.getAuthor())) {
                        ctx.send(String.format(languageContext.get("commands.rep.rep_yourself"), EmoteReference.THINKING,
                                (rl > 0 ? String.format(languageContext.get("commands.rep.cooldown.waiting"), Utils.formatDuration(rl))
                                        : languageContext.get("commands.rep.cooldown.pass"))
                                )
                        );

                        return;
                    }

                    if (!RatelimitUtils.ratelimit(rateLimiter, ctx, false)) {
                        return;
                    }

                    var player = UnifiedPlayer.of(usr, ctx.getConfig().getCurrentSeason());
                    player.addReputation(1L);
                    player.saveUpdating();

                    ctx.sendStrippedLocalized("commands.rep.success", EmoteReference.CORRECT, member.getEffectiveName());
                });
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
    public void equip(CommandRegistry cr) {
        cr.register("equip", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.profile.equip.no_content", EmoteReference.ERROR);
                    return;
                }

                var isSeasonal = ctx.isSeasonal();
                content = Utils.replaceArguments(ctx.getOptionalArguments(), content, "s", "season");

                var item = ItemHelper.fromAnyNoId(content.replace("\"", ""), ctx.getLanguageContext())
                        .orElse(null);

                var player = ctx.getPlayer();
                var dbUser = ctx.getDBUser();
                var userData = dbUser.getData();
                var seasonalPlayer = ctx.getSeasonPlayer();
                var seasonalPlayerData = seasonalPlayer.getData();
                var seasonalPlayerInventory = seasonalPlayer.getInventory();
                var playerInventory = player.getInventory();

                if (item == null) {
                    ctx.sendLocalized("commands.profile.equip.no_item", EmoteReference.ERROR);
                    return;
                }

                var containsItem = isSeasonal ? seasonalPlayerInventory.containsItem(item) : playerInventory.containsItem(item);
                if (!containsItem) {
                    ctx.sendLocalized("commands.profile.equip.not_owned", EmoteReference.ERROR);
                    return;
                }

                var equipment = isSeasonal ? seasonalPlayerData.getEquippedItems() : userData.getEquippedItems();

                var proposedType = equipment.getTypeFor(item);
                if (equipment.getEquipment().containsKey(proposedType)) {
                    ctx.sendLocalized("commands.profile.equip.already_equipped", EmoteReference.ERROR);
                    return;
                }

                if (equipment.equipItem(item)) {
                    if (isSeasonal) {
                        seasonalPlayerInventory.process(new ItemStack(item, -1));
                        seasonalPlayer.save();
                    } else {
                        playerInventory.process(new ItemStack(item, -1));
                        player.save();
                    }

                    dbUser.saveUpdating();
                    ctx.sendLocalized("commands.profile.equip.success", EmoteReference.CORRECT, item.getEmoji(), item.getName());
                } else {
                    ctx.sendLocalized("commands.profile.equip.not_suitable", EmoteReference.ERROR);
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Equips an item into a slot.")
                        .setUsage("`~>equip <item>`.")
                        .addParameter("item", "The name or emoji of the item you want to equip.")
                        .build();
            }
        });
    }

    @Subscribe
    public void unequip(CommandRegistry cr) {
        cr.register("unequip", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.profile.unequip.no_content", EmoteReference.ERROR);
                    return;
                }

                var isSeasonal = ctx.isSeasonal();
                content = Utils.replaceArguments(ctx.getOptionalArguments(), content, "s", "season");

                var seasonalPlayer = ctx.getSeasonPlayer();
                var dbUser = ctx.getDBUser();
                var equipment = isSeasonal ? seasonalPlayer.getData().getEquippedItems() : dbUser.getData().getEquippedItems();
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
                var languageContext = ctx.getLanguageContext();

                ctx.sendLocalized("commands.profile.unequip.confirm", EmoteReference.WARNING, equippedItem.getEmoji(), equippedItem.getName());
                InteractiveOperations.create(ctx.getChannel(), ctx.getAuthor().getIdLong(), 45, interactiveEvent -> {
                    var author = interactiveEvent.getAuthor();
                    if (author.getIdLong() != ctx.getAuthor().getIdLong()) {
                        return InteractiveOperation.IGNORED;
                    }

                    var ct = interactiveEvent.getMessage().getContentRaw();
                    if (ct.equalsIgnoreCase("yes")) {
                        var seasonalPlayerFinal = ctx.getSeasonPlayer(author);
                        var dbUserFinal = ctx.getDBUser(author);
                        var playerFinal = ctx.getPlayer(author);
                        var dbUserData = dbUserFinal.getData();
                        var seasonalPlayerData = seasonalPlayerFinal.getData();

                        var equipmentFinal = isSeasonal ? seasonalPlayerData.getEquippedItems() : dbUserData.getEquippedItems();
                        var equippedFinal = equipmentFinal.getEquipment().get(type);
                        if (equippedFinal == null) {
                            ctx.sendLocalized("commands.profile.unequip.not_equipped", EmoteReference.ERROR);
                            return InteractiveOperation.COMPLETED;
                        }

                        var equippedItemFinal = ItemHelper.fromId(equippedFinal);
                        var part = ""; //Start as an empty string.
                        if (equippedItemFinal instanceof Breakable) {
                            // Gotta check again, just in case...
                            var item = (Breakable) equippedItemFinal;

                            var percentage = ((float) equipmentFinal.getDurability().get(type) / (float) item.getMaxDurability()) * 100.0f;
                            if (percentage == 100) { //Basically never used
                                playerFinal.getInventory().process(new ItemStack(equippedItemFinal, 1));
                                part += String.format(
                                        languageContext.get("commands.profile.unequip.equipment_recover"), equippedItemFinal.getName()
                                );
                            } else {
                                var brokenItem = ItemHelper.getBrokenItemFrom(equippedItemFinal);
                                if (brokenItem != null) {
                                    playerFinal.getInventory().process(new ItemStack(brokenItem, 1));
                                    part += String.format(
                                            languageContext.get("commands.profile.unequip.broken_equipment_recover"), brokenItem.getName()
                                    );
                                } else {
                                    long money = equippedItemFinal.getValue() / 2;
                                    //Brom's Pickaxe, Diamond Pickaxe and normal rod and Diamond Rod will hit this condition.
                                    part += String.format(
                                            languageContext.get("commands.profile.unequip.broken_equipment_recover_none"), money
                                    );
                                }
                            }
                        }

                        equipmentFinal.resetOfType(type);
                        if (isSeasonal) {
                            seasonalPlayerFinal.save();
                        } else {
                            dbUserFinal.save();
                        }

                        playerFinal.save();

                        ctx.sendFormat(languageContext.get("commands.profile.unequip.success") + part,
                                EmoteReference.CORRECT, type.name().toLowerCase()
                        );
                    } else if (ct.equalsIgnoreCase("no")) {
                        ctx.sendLocalized("commands.profile.unequip.cancelled", EmoteReference.WARNING);
                        return InteractiveOperation.COMPLETED;
                    }

                    return InteractiveOperation.IGNORED;
                });
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Unequips and already equipped slot.")
                        .setUsage("`~>unequip <slot>`.\n" +
                                "Unequipping an item causes it to drop a broken version of itself, unless it wasn't used. " +
                                "If there's no broken version of it, it will drop half of its market value.")
                        .addParameter("slot", "Either pick, axe or rod.")
                        .build();
            }
        });
    }

    @Subscribe
    public void badges(CommandRegistry cr) {
        final Random r = new Random();
        ITreeCommand badgeCommand = cr.register("badges", new TreeCommand(CommandCategory.CURRENCY) {
            @Override
            public Command defaultTrigger(Context ctx, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, I18nContext languageContext, String content) {
                        var optionalArguments = ctx.getOptionalArguments();
                        content = Utils.replaceArguments(optionalArguments, content, "brief");
                        // Lambdas strike again.
                        var contentFinal = content;

                        ctx.findMember(content, ctx.getMessage()).onSuccess(members -> {
                            var member = CustomFinderUtil.findMemberDefault(contentFinal, members, ctx, ctx.getMember());
                            if (member == null) {
                                return;
                            }

                            var toLookup = member.getUser();

                            Player player = ctx.getPlayer(toLookup);
                            PlayerData playerData = player.getData();
                            DBUser dbUser = ctx.getDBUser();

                            if (!optionalArguments.isEmpty() && optionalArguments.containsKey("brief")) {
                                ctx.sendLocalized("commands.badges.brief_success", member.getEffectiveName(),
                                        playerData.getBadges().stream()
                                                .sorted()
                                                .map(Badge::getDisplay)
                                                .collect(Collectors.joining(", "))
                                );

                                return;
                            }

                            var badges = playerData.getBadges();
                            Collections.sort(badges);

                            var embed = new EmbedBuilder()
                                    .setAuthor(String.format(languageContext.get("commands.badges.header"), toLookup.getName()))
                                    .setColor(ctx.getMember().getColor() == null ? Color.PINK : ctx.getMember().getColor())
                                    .setThumbnail(toLookup.getEffectiveAvatarUrl());
                            List<MessageEmbed.Field> fields = new LinkedList<>();

                            for (var b : badges) {
                                //God DAMNIT discord, I want it to look cute, stop trimming my spaces.
                                fields.add(
                                        new MessageEmbed.Field(b.toString(), "**\u2009\u2009\u2009\u2009- " + b.description + "**", false)
                                );
                            }

                            if (badges.isEmpty()) {
                                embed.setDescription(languageContext.get("commands.badges.no_badges"));
                                ctx.send(embed.build());
                                return;
                            }

                            var common = languageContext.get("commands.badges.profile_notice") + languageContext.get("commands.badges.info_notice") +
                                    ((r.nextInt(2) == 0 && !dbUser.isPremium() ? languageContext.get("commands.badges.donate_notice") : "\n") +
                                            String.format(languageContext.get("commands.badges.total_badges"), badges.size()) + "\n");

                            DiscordUtils.sendPaginatedEmbed(ctx, embed, DiscordUtils.divideFields(6, fields), common);
                        });
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
                return "Shows info about a badge.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.badges.info.not_specified", EmoteReference.ERROR);
                    return;
                }

                var badge = Badge.lookupFromString(content);
                if (badge == null || badge == Badge.DJ) {
                    ctx.sendLocalized("commands.badges.info.not_found", EmoteReference.ERROR);
                    return;
                }

                var player = ctx.getPlayer();

                var message = new MessageBuilder().setEmbed(new EmbedBuilder()
                        .setAuthor(String.format(languageContext.get("commands.badges.info.header"), badge.display))
                        .setDescription(String.join("\n",
                                EmoteReference.BLUE_SMALL_MARKER + "**" + languageContext.get("general.name") + ":** " + badge.display,
                                EmoteReference.BLUE_SMALL_MARKER + "**" + languageContext.get("general.description") + ":** " + badge.description,
                                EmoteReference.BLUE_SMALL_MARKER + "**" +
                                        languageContext.get("commands.badges.info.achieved") + ":** " +
                                        player.getData().getBadges().stream().anyMatch(b -> b == badge)
                                )
                        )
                        .setThumbnail("attachment://icon.png")
                        .setColor(Color.CYAN)
                        .build()
                ).build();

                ctx.getChannel()
                        .sendMessage(message).addFile(badge.icon, "icon.png")
                        .queue();
            }
        });

        cr.registerAlias("badges", "badge");
    }
}
