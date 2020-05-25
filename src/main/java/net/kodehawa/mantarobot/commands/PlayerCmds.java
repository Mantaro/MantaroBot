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
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.commands.currency.item.PlayerEquipment;
import net.kodehawa.mantarobot.commands.currency.item.special.helpers.Breakable;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
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
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.base.ITreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.PlayerData;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.IncreasingRateLimiter;

import java.awt.*;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.utils.Utils.handleIncreasingRatelimit;

@Module
@SuppressWarnings("unused")
public class PlayerCmds {
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
            public void call(Context ctx, String content, String[] args) {
                long rl = rateLimiter.getRemaniningCooldown(ctx.getAuthor());

                User user;
                I18nContext languageContext = ctx.getLanguageContext();

                if (content.isEmpty()) {
                    ctx.send(String.format(languageContext.get("commands.rep.no_mentions"), EmoteReference.ERROR,
                            (rl > 0 ? String.format(languageContext.get("commands.rep.cooldown.waiting"),
                                    Utils.formatDuration(rl)) : languageContext.get("commands.rep.cooldown.pass"))
                            )
                    );

                    return;
                }

                List<User> mentioned = ctx.getMentionedUsers();
                if (!mentioned.isEmpty() && mentioned.size() > 1) {
                    ctx.sendLocalized("commands.rep.more_than_one", EmoteReference.ERROR);
                    return;
                }

                Member member = Utils.findMember(ctx.getEvent(), ctx.getMember(), content);
                if (member == null)
                    return;

                user = member.getUser();
                User author = ctx.getAuthor();
                Predicate<User> oldEnough = (u -> u.getTimeCreated().isBefore(OffsetDateTime.now().minus(5, ChronoUnit.DAYS)));

                //Didn't want to repeat the code twice, lol.
                if (!oldEnough.test(user)) {
                    ctx.sendLocalized("commands.rep.new_account_notice", EmoteReference.ERROR);
                    return;
                }

                if (!oldEnough.test(author)) {
                    ctx.sendLocalized("commands.rep.new_account_notice", EmoteReference.ERROR);
                    return;
                }

                if (user.isBot()) {
                    ctx.send(String.format(languageContext.get("commands.rep.rep_bot"), EmoteReference.THINKING,
                            (rl > 0 ? String.format(languageContext.get("commands.rep.cooldown.waiting"), Utils.formatDuration(rl))
                                    : languageContext.get("commands.rep.cooldown.pass"))
                            )
                    );

                    return;
                }

                if (user.equals(ctx.getAuthor())) {
                    ctx.send(String.format(languageContext.get("commands.rep.rep_yourself"), EmoteReference.THINKING,
                            (rl > 0 ? String.format(languageContext.get("commands.rep.cooldown.waiting"), Utils.formatDuration(rl))
                                    : languageContext.get("commands.rep.cooldown.pass"))
                            )
                    );

                    return;
                }

                //Check for RL.
                if (!Utils.handleIncreasingRatelimit(rateLimiter, ctx.getAuthor(), ctx.getEvent(), languageContext, false))
                    return;

                UnifiedPlayer player = UnifiedPlayer.of(user, ctx.getConfig().getCurrentSeason());
                player.addReputation(1L);
                player.save();

                ctx.sendStrippedLocalized("commands.rep.success", EmoteReference.CORRECT, member.getEffectiveName());
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
        cr.register("equip", new SimpleCommand(Category.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.profile.equip.no_content", EmoteReference.ERROR);
                    return;
                }

                boolean isSeasonal = ctx.isSeasonal();
                content = Utils.replaceArguments(ctx.getOptionalArguments(), content, "s", "season");

                Item item = Items.fromAnyNoId(content.replace("\"", "")).orElse(null);

                Player player = ctx.getPlayer();
                DBUser dbUser = ctx.getDBUser();
                UserData data = dbUser.getData();
                SeasonPlayer seasonalPlayer = ctx.getSeasonPlayer();
                SeasonalPlayerData seasonalPlayerData = seasonalPlayer.getData();

                if (item == null) {
                    ctx.sendLocalized("commands.profile.equip.no_item", EmoteReference.ERROR);
                    return;
                }

                boolean containsItem = isSeasonal ? seasonalPlayer.getInventory().containsItem(item) : player.getInventory().containsItem(item);
                if (!containsItem) {
                    ctx.sendLocalized("commands.profile.equip.not_owned", EmoteReference.ERROR);
                    return;
                }

                PlayerEquipment equipment = isSeasonal ? seasonalPlayerData.getEquippedItems() : data.getEquippedItems();

                PlayerEquipment.EquipmentType proposedType = equipment.getTypeFor(item);
                if (equipment.getEquipment().containsKey(proposedType)) {
                    ctx.sendLocalized("commands.profile.equip.already_equipped", EmoteReference.ERROR);
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

                    dbUser.save();

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
        cr.register("unequip", new SimpleCommand(Category.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.profile.unequip.no_content", EmoteReference.ERROR);
                    return;
                }

                boolean isSeasonal = ctx.isSeasonal();
                content = Utils.replaceArguments(ctx.getOptionalArguments(), content, "s", "season");

                DBUser dbUser = ctx.getDBUser();
                Player player = ctx.getPlayer();
                UserData data = dbUser.getData();

                SeasonPlayer seasonalPlayer = ctx.getSeasonPlayer();
                SeasonalPlayerData seasonalPlayerData = seasonalPlayer.getData();

                PlayerEquipment equipment = isSeasonal ? seasonalPlayerData.getEquippedItems() : data.getEquippedItems();
                PlayerEquipment.EquipmentType type = PlayerEquipment.EquipmentType.fromString(content);
                if (type == null) {
                    ctx.sendLocalized("commands.profile.unequip.invalid_type", EmoteReference.ERROR);
                    return;
                }

                I18nContext languageContext = ctx.getLanguageContext();

                String part = ""; //Start as an empty string.
                if(type == PlayerEquipment.EquipmentType.PICK || type == PlayerEquipment.EquipmentType.ROD) {
                    Item equippedItem = Items.fromId(equipment.getEquipment().get(type));

                    if(equippedItem == null) {
                        ctx.sendLocalized("commands.profile.unequip.not_equipped", EmoteReference.ERROR);
                        return;
                    }

                    Breakable item = (Breakable) equippedItem;

                    float percentage = ((float) equipment.getDurability().get(type) / (float) item.getMaxDurability()) * 100.0f;
                    if(percentage == 100) { //Basically never used
                        player.getInventory().process(new ItemStack(equippedItem, 1));
                        part += String.format(languageContext.get("commands.profile.unequip.equipment_recover"), equippedItem.getName());
                    } else {
                        Item brokenItem = Items.getBrokenItemFrom(equippedItem);
                        if(brokenItem != null) {
                            player.getInventory().process(new ItemStack(brokenItem, 1));
                            part += String.format(languageContext.get("commands.profile.unequip.broken_equipment_recover"), brokenItem.getName());
                        } else {
                            long money = equippedItem.getValue() / 2;
                            //Brom's Pickaxe, Diamond Pickaxe and normal rod and Diamond Rod will hit this condition.
                            part += String.format(languageContext.get("commands.profile.unequip.broken_equipment_recover_none"), money);
                        }
                    }

                    player.save();
                }

                equipment.resetOfType(type);
                if (isSeasonal)
                    seasonalPlayer.save();
                else
                    dbUser.save();

                ctx.sendFormat(languageContext.get("commands.profile.unequip.success") + part, EmoteReference.CORRECT, type.name().toLowerCase());
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Unequips and already equipped slot.")
                        .setUsage("`~>unequip <slot>`.\n" +
                                "Unequipping an item causes it to drop a broken version of itself, unless it wasn't used. " +
                                "If there's no broken version of it, it will drop half of its market value.")
                        .addParameter("slot", "Either pick or rod.")
                        .build();
            }
        });
    }

    @Subscribe
    public void badges(CommandRegistry cr) {
        final Random r = new Random();
        ITreeCommand badgeCommand = (ITreeCommand) cr.register("badges", new TreeCommand(Category.CURRENCY) {
            @Override
            public Command defaultTrigger(Context ctx, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, String content) {
                        Map<String, String> optionalArguments = ctx.getOptionalArguments();
                        content = Utils.replaceArguments(optionalArguments, content, "brief");
                        Member member = Utils.findMember(ctx.getEvent(), ctx.getMember(), content);
                        if (member == null) return;

                        User toLookup = member.getUser();

                        Player player = ctx.getPlayer(toLookup);
                        PlayerData playerData = player.getData();
                        DBUser dbUser = ctx.getDBUser();

                        I18nContext languageContext = ctx.getLanguageContext();

                        if (!optionalArguments.isEmpty() && optionalArguments.containsKey("brief")) {
                            ctx.sendLocalized("commands.badges.brief_success", member.getEffectiveName(),
                                    playerData.getBadges().stream()
                                            .sorted()
                                            .map(Badge::getDisplay)
                                            .collect(Collectors.joining(", "))
                            );
                        }

                        List<Badge> badges = playerData.getBadges();
                        Collections.sort(badges);

                        EmbedBuilder embed = new EmbedBuilder()
                                .setAuthor(String.format(languageContext.get("commands.badges.header"), toLookup.getName()))
                                .setColor(ctx.getMember().getColor() == null ? Color.PINK : ctx.getMember().getColor())
                                .setThumbnail(toLookup.getEffectiveAvatarUrl());
                        List<MessageEmbed.Field> fields = new LinkedList<>();

                        for (Badge b : badges) {
                            //God DAMNIT discord, I want it to look cute, stop trimming my spaces.
                            fields.add(new MessageEmbed.Field(b.toString(), "**\u2009\u2009\u2009\u2009- " + b.description + "**", false));
                        }

                        if (badges.isEmpty()) {
                            embed.setDescription(languageContext.get("commands.badges.no_badges"));
                            ctx.send(embed.build());
                            return;
                        }

                        List<List<MessageEmbed.Field>> splitFields = DiscordUtils.divideFields(6, fields);
                        boolean hasReactionPerms = ctx.hasReactionPerms();

                        embed.setFooter(languageContext.get("commands.badges.footer"), null);

                        String common = languageContext.get("commands.badges.profile_notice") + languageContext.get("commands.badges.info_notice") +
                                ((r.nextInt(3) == 0 && !playerData.hasBadge(Badge.UPVOTER) ? languageContext.get("commands.badges.upvote_notice") : "\n")) +
                                ((r.nextInt(2) == 0 && !dbUser.isPremium() ? languageContext.get("commands.badges.donate_notice") : "\n") +
                                        String.format(languageContext.get("commands.badges.total_badges"), badges.size()) + "\n");
                        if (hasReactionPerms) {
                            embed.setDescription(languageContext.get("general.arrow_react") + "\n" + common);
                            DiscordUtils.list(ctx.getEvent(), 60, false, embed, splitFields);
                        } else {
                            embed.setDescription(languageContext.get("general.text_menu") + "\n" + common);
                            DiscordUtils.listText(ctx.getEvent(), 60, false, embed, splitFields);
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
            protected void call(Context ctx, String content) {
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.badges.info.not_specified", EmoteReference.ERROR);
                    return;
                }

                Badge badge = Badge.lookupFromString(content);
                //shouldn't NPE bc null check is done first, in order
                if (badge == null || badge == Badge.DJ) {
                    ctx.sendLocalized("commands.badges.info.not_found", EmoteReference.ERROR);
                    return;
                }

                Player p = ctx.getPlayer();
                final I18nContext languageContext = ctx.getLanguageContext();

                Message message = new MessageBuilder().setEmbed(new EmbedBuilder()
                        .setAuthor(String.format(languageContext.get("commands.badges.info.header"), badge.display))
                        .setDescription(String.join("\n",
                                EmoteReference.BLUE_SMALL_MARKER + "**" + languageContext.get("general.name") + ":** " + badge.display,
                                EmoteReference.BLUE_SMALL_MARKER + "**" + languageContext.get("general.description") + ":** " + badge.description,
                                EmoteReference.BLUE_SMALL_MARKER + "**" + languageContext.get("commands.badges.info.achieved") + ":** " + p.getData().getBadges().stream().anyMatch(b -> b == badge))
                        ).setThumbnail("attachment://icon.png")
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
