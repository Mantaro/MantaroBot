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
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.item.*;
import net.kodehawa.mantarobot.commands.currency.item.special.Potion;
import net.kodehawa.mantarobot.commands.currency.pets.Pet;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.currency.seasons.SeasonPlayer;
import net.kodehawa.mantarobot.commands.utils.RoundedMetricPrefixFormat;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.RateLimiter;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.security.SecureRandom;
import java.text.ParsePosition;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.utils.Utils.handleIncreasingRatelimit;

@Module
@SuppressWarnings("unused")
public class CurrencyCmds {
    private final int TRANSFER_LIMIT = Integer.MAX_VALUE / 4; //around 536m

    public static void applyPotionEffect(GuildMessageReceivedEvent event, Item item, Player p, Map<String, String> arguments, String content, boolean isPet, I18nContext languageContext) {
        final ManagedDatabase db = MantaroData.db();
        if ((item.getItemType() == ItemType.POTION || item.getItemType() == ItemType.BUFF) && item instanceof Potion) {
            DBUser dbUser = db.getUser(event.getAuthor());
            UserData userData = dbUser.getData();
            Map<String, Pet> profilePets = p.getData().getPets();
            final TextChannel channel = event.getChannel();

            //Yes, parser limitations. Natan change to your parser eta wen :^), really though, we could use some generics on here lol
            // NumberFormatException?
            int amount = 1;
            if(arguments.containsKey("amount")) {
                try {
                    amount = Integer.parseInt(arguments.get("amount"));
                } catch (NumberFormatException e) {
                    channel.sendMessageFormat(languageContext.get("commands.useitem.invalid_amount"), EmoteReference.WARNING).queue();
                    return;
                }
            }
            String petName = isPet ? content : "";

            if (isPet && petName.isEmpty()) {
                channel.sendMessageFormat(languageContext.get("commands.useitem.no_name"), EmoteReference.SAD).queue();
                return;
            }

            final PlayerEquipment equippedItems = isPet ? profilePets.get(petName).getData().getEquippedItems() : userData.getEquippedItems();
            PlayerEquipment.EquipmentType type = equippedItems.getTypeFor(item);

            if (amount < 1) {
                channel.sendMessageFormat(languageContext.get("commands.useitem.too_little"), EmoteReference.SAD).queue();
                return;
            }

            if (p.getInventory().getAmount(item) < amount) {
                channel.sendMessageFormat(languageContext.get("commands.useitem.not_enough_items"), EmoteReference.SAD).queue();
                return;
            }


            if (equippedItems.isEffectActive(type, ((Potion) item).getMaxUses())) {
                PotionEffect currentPotion = equippedItems.getCurrentEffect(type);

                //Currently has a potion equipped, but wants to stack a potion of other type.
                if (currentPotion.getPotion() != Items.idOf(item)) {
                    channel.sendMessageFormat(languageContext.get("general.misc_item_usage.not_same_potion"),
                            EmoteReference.ERROR, Items.fromId(currentPotion.getPotion()).getName(), item.getName()).queue();

                    return;
                }

                //Currently has a potion equipped, and is of the same type.
                if (currentPotion.getAmountEquipped() + amount < 10) {
                    currentPotion.equip(amount);
                    channel.sendMessageFormat(languageContext.get("general.misc_item_usage.potion_applied_multiple"),
                            EmoteReference.CORRECT, item.getName(), Utils.capitalize(type.toString()), currentPotion.getAmountEquipped()).queue();
                } else {
                    //Too many stacked (max: 10).
                    channel.sendMessageFormat(languageContext.get("general.misc_item_usage.max_stack_size"), EmoteReference.ERROR, item.getName()).queue();
                    return;
                }
            } else {
                //No potion stacked.
                PotionEffect effect = new PotionEffect(Items.idOf(item), 0, ItemType.PotionType.PLAYER);

                //If there's more than 1, proceed to equip the stacks.

                if (amount>=10) {
                    //Too many stacked (max: 10).
                    channel.sendMessageFormat(languageContext.get("general.misc_item_usage.max_stack_size_2"), EmoteReference.ERROR, item.getName()).queue();
                    return;
                }
                if (amount > 1)
                    effect.equip(amount - 1); //Amount - 1 because we're technically using one.


                //Apply the effect.
                equippedItems.applyEffect(effect);
                channel.sendMessageFormat(languageContext.get("general.misc_item_usage.potion_applied"),
                        EmoteReference.CORRECT, item.getName(), Utils.capitalize(type.toString()), amount).queue();
            }


            if(amount == 9)
                p.getData().addBadgeIfAbsent(Badge.MAD_SCIENTIST);

            //Default: 1
            p.getInventory().process(new ItemStack(item, -amount));
            p.save();
            dbUser.save();

            return;
        }

        if (!isPet)
            item.getAction().test(event, Pair.of(languageContext, content), false);
    }

    @Subscribe
    public void inventory(CommandRegistry cr) {
        final Random r = new Random();
        cr.register("inventory", new SimpleCommand(Category.CURRENCY) {
            @Override
            public void call(Context ctx, String content, String[] args) {
                Map<String, String> t = ctx.getOptionalArguments();
                content = Utils.replaceArguments(t, content, "brief", "calculate", "calc", "c", "info", "full", "season", "s");
                Member member = Utils.findMember(ctx.getEvent(), ctx.getMember(), content);

                if (member == null)
                    return;

                if (member.getUser().isBot()) {
                    ctx.sendLocalized("commands.inventory.bot_notice", EmoteReference.ERROR);
                    return;
                }

                Player player = ctx.getPlayer(member);
                DBUser user = ctx.getDBUser(member);
                SeasonPlayer seasonPlayer = ctx.getSeasonPlayer(member);
                Inventory playerInventory = player.getInventory();
                final List<ItemStack> inventoryList = playerInventory.asList();
                I18nContext languageContext = ctx.getLanguageContext();

                if (ctx.isSeasonal()) {
                    playerInventory = seasonPlayer.getInventory();
                }

                if (t.containsKey("calculate") || t.containsKey("calc") || t.containsKey("c")) {
                    long all = playerInventory.asList().stream()
                            .filter(item -> item.getItem().isSellable())
                            .mapToLong(value -> (long) (value.getItem().getValue() * value.getAmount() * 0.9d))
                            .sum();

                    ctx.sendLocalized("commands.inventory.calculate", EmoteReference.DIAMOND, member.getUser().getName(), all);
                    return;
                }

                if (inventoryList.isEmpty()) {
                    ctx.sendLocalized("commands.inventory.empty", EmoteReference.WARNING);
                    return;
                }

                if (t.containsKey("info") || t.containsKey("full")) {
                    EmbedBuilder builder = baseEmbed(ctx,
                            String.format(languageContext.get("commands.inventory.header"), member.getEffectiveName()), member.getUser().getEffectiveAvatarUrl()
                    );

                    List<MessageEmbed.Field> fields = new LinkedList<>();
                    if (inventoryList.isEmpty())
                        builder.setDescription(languageContext.get("general.dust"));
                    else {
                        playerInventory.asList().forEach(stack -> {
                            long buyValue = stack.getItem().isBuyable() ? stack.getItem().getValue() : 0;
                            long sellValue = stack.getItem().isSellable() ? (long) (stack.getItem().getValue() * 0.9) : 0;
                            fields.add(new MessageEmbed.Field(String.format("%s %s x %d", stack.getItem().getEmoji(), stack.getItem().getName(), stack.getAmount()),
                                    String.format(languageContext.get("commands.inventory.format"), buyValue, sellValue, languageContext.get(stack.getItem().getDesc())), false));
                        });
                    }

                    List<List<MessageEmbed.Field>> splitFields = DiscordUtils.divideFields(6, fields);
                    boolean hasReactionPerms = ctx.hasReactionPerms();

                    if (hasReactionPerms) {
                        if (builder.getDescriptionBuilder().length() == 0) {
                            builder.setDescription(String.format(languageContext.get("general.buy_sell_paged_react"), splitFields.size(),
                                    String.format(languageContext.get("general.buy_sell_paged_reference"), EmoteReference.BUY, EmoteReference.SELL))
                                    + "\n" + languageContext.get("commands.inventory.brief_notice") + (r.nextInt(3) == 0 && !user.isPremium() ? languageContext.get("general.sellout") : ""));
                        }
                        DiscordUtils.list(ctx.getEvent(), 100, false, builder, splitFields);
                    } else {
                        if (builder.getDescriptionBuilder().length() == 0) {
                            builder.setDescription(String.format(languageContext.get("general.buy_sell_paged_text"), splitFields.size(),
                                    String.format(languageContext.get("general.buy_sell_paged_reference"), EmoteReference.BUY, EmoteReference.SELL))
                                    + "\n" + languageContext.get("commands.inventory.brief_notice") + (r.nextInt(3) == 0  && !user.isPremium() ? languageContext.get("general.sellout") : ""));
                        }
                        DiscordUtils.listText(ctx.getEvent(), 100, false, builder, splitFields);
                    }

                    return;
                }

                ctx.sendStrippedLocalized("commands.inventory.brief", member.getEffectiveName(), ItemStack.toString(playerInventory.asList()));
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Shows your current inventory.")
                        .setUsage("You can mention someone on this command to see their inventory.\n" +
                                "You can use `~>inventory -full` to a more detailed version.\n" +
                                "Use `~>inventory -calculate` to see how much you'd get if you sell every sellable item on your inventory.")
                        .setSeasonal(true)
                        .build();
            }
        });

        cr.registerAlias("inventory", "inv");
    }

    @Subscribe
    public void level(CommandRegistry cr) {
        cr.register("level", new SimpleCommand(Category.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                Member member = Utils.findMember(ctx.getEvent(), ctx.getMember(), content);
                if (member == null)
                    return;

                if (member.getUser().isBot()) {
                    ctx.sendLocalized("commands.level.bot_notice", EmoteReference.ERROR);
                    return;
                }

                Player player = MantaroData.db().getPlayer(member);
                long experienceNext = (long) (player.getLevel() * Math.log10(player.getLevel()) * 1000) + (50 * player.getLevel() / 2);

                if (member.getUser().getIdLong() == ctx.getAuthor().getIdLong()) {
                    ctx.sendLocalized("commands.level.own_success",
                            EmoteReference.ZAP, player.getLevel(), player.getData().getExperience(), experienceNext
                    );
                } else {
                    ctx.sendLocalized("commands.level.success_other",
                            EmoteReference.ZAP, member.getUser().getAsTag(), player.getLevel(), player.getData().getExperience(), experienceNext
                    );
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Checks your level or the level of another user.")
                        .setUsage("~>level [user]")
                        .addParameterOptional("user", "The user to check the id of. Can be a mention, tag or id.")
                        .build();
            }
        });
    }

    @Subscribe
    public void transferItems(CommandRegistry cr) {
        cr.register("itemtransfer", new SimpleCommand(Category.CURRENCY) {
            final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                    .spamTolerance(2)
                    .limit(1)
                    .cooldown(15, TimeUnit.SECONDS)
                    .cooldownPenaltyIncrease(5, TimeUnit.SECONDS)
                    .maxCooldown(20, TimeUnit.MINUTES)
                    .pool(MantaroData.getDefaultJedisPool())
                    .premiumAware(true)
                    .prefix("itemtransfer")
                    .build();

            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (args.length < 2) {
                    ctx.sendLocalized("commands.itemtransfer.no_item_mention", EmoteReference.ERROR);
                    return;
                }

                final List<Member> mentionedMembers = ctx.getMentionedMembers();
                if (mentionedMembers.size() == 0) {
                    ctx.sendLocalized("general.mention_user_required", EmoteReference.ERROR);
                } else {
                    Member giveTo = mentionedMembers.get(0);

                    if (ctx.getAuthor().getId().equals(giveTo.getId())) {
                        ctx.sendLocalized("commands.itemtransfer.transfer_yourself_note", EmoteReference.ERROR);
                        return;
                    }

                    if (giveTo.getUser().isBot()) {
                        ctx.sendLocalized("commands.itemtransfer.bot_notice", EmoteReference.ERROR);
                        return;
                    }

                    if (!handleIncreasingRatelimit(rateLimiter, ctx.getAuthor(), ctx))
                        return;

                    Item item = Items.fromAnyNoId(args[1]).orElse(null);
                    if (item == null) {
                        ctx.sendLocalized("general.item_lookup.no_item_emoji");
                    } else {
                        if (item == Items.CLAIM_KEY) {
                            ctx.sendLocalized("general.item_lookup.claim_key");
                            return;
                        }

                        Player player = ctx.getPlayer();
                        Player giveToPlayer = ctx.getPlayer(giveTo);

                        if (player.isLocked()) {
                            ctx.sendLocalized("commands.itemtransfer.locked_notice", EmoteReference.ERROR);
                            return;
                        }

                        if (args.length == 2) {
                            if (player.getInventory().containsItem(item)) {
                                if (item.isHidden()) {
                                    ctx.sendLocalized("commands.itemtransfer.hidden_item", EmoteReference.ERROR);
                                    return;
                                }

                                if (giveToPlayer.getInventory().asMap().getOrDefault(item, new ItemStack(item, 0)).getAmount() >= 5000) {
                                    ctx.sendLocalized("commands.itemtransfer.overflow", EmoteReference.ERROR);
                                    return;
                                }

                                player.getInventory().process(new ItemStack(item, -1));
                                giveToPlayer.getInventory().process(new ItemStack(item, 1));
                                ctx.sendStrippedLocalized("commands.itemtransfer.success",
                                        EmoteReference.OK, ctx.getMember().getEffectiveName(), 1, item.getName(), giveTo.getEffectiveName()
                                );
                            } else {
                                ctx.sendLocalized("commands.itemtransfer.multiple_items_error", EmoteReference.ERROR);
                            }

                            player.saveAsync();
                            giveToPlayer.saveAsync();
                            return;
                        }

                        try {
                            int amount = Math.abs(Integer.parseInt(args[2]));
                            if (player.getInventory().containsItem(item) && player.getInventory().getAmount(item) >= amount) {
                                if (item.isHidden()) {
                                    ctx.sendLocalized("commands.itemtransfer.hidden_item", EmoteReference.ERROR);
                                    return;
                                }

                                if (giveToPlayer.getInventory().asMap().getOrDefault(item, new ItemStack(item, 0)).getAmount() + amount > 5000) {
                                    ctx.sendLocalized("commands.itemtransfer.overflow", EmoteReference.ERROR);
                                    return;
                                }

                                player.getInventory().process(new ItemStack(item, amount * -1));
                                giveToPlayer.getInventory().process(new ItemStack(item, amount));

                                ctx.sendStrippedLocalized("commands.itemtransfer.success", EmoteReference.OK,
                                        ctx.getMember().getEffectiveName(), amount, item.getName(), giveTo.getEffectiveName()
                                );
                            } else {
                                ctx.sendLocalized("commands.itemtransfer.error", EmoteReference.ERROR);
                            }
                        } catch (NumberFormatException nfe) {
                            ctx.send(String.format(ctx.getLanguageContext().get("general.invalid_number") + " " +
                                    ctx.getLanguageContext().get("general.space_notice"), EmoteReference.ERROR)
                            );
                        }

                        player.saveAsync();
                        giveToPlayer.saveAsync();
                    }
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Transfers items from you to another player.")
                        .setUsage("`~>itemtransfer <@user> <item> <amount>` - Transfers the item to a user.")
                        .addParameter("@user", "User mention or name.")
                        .addParameter("item", "The item emoji or name. If the name contains spaces \"wrap it in quotes\"")
                        .addParameter("amount", "The amount of items you want to transfer. This is optional.")
                        .build();
            }
        });

        cr.registerAlias("itemtransfer", "transferitems");
    }

    @Subscribe
    //Should be called return land tbh, what the fuck.
    public void transfer(CommandRegistry cr) {
        cr.register("transfer", new SimpleCommand(Category.CURRENCY) {
            final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                    .spamTolerance(2)
                    .limit(1)
                    .cooldown(10, TimeUnit.SECONDS)
                    .cooldownPenaltyIncrease(5, TimeUnit.SECONDS)
                    .maxCooldown(10, TimeUnit.MINUTES)
                    .pool(MantaroData.getDefaultJedisPool())
                    .prefix("transfer")
                    .build();

            //this still uses a normal RL
            final RateLimiter partyRateLimiter = new RateLimiter(TimeUnit.MINUTES, 3);

            @Override
            public void call(Context ctx, String content, String[] args) {
                if (ctx.getMentionedUsers().isEmpty()) {
                    ctx.sendLocalized("general.mention_user_required", EmoteReference.ERROR);
                    return;
                }

                final User giveTo = ctx.getMentionedUsers().get(0);

                if (giveTo.equals(ctx.getAuthor())) {
                    ctx.sendLocalized("commands.transfer.transfer_yourself_note", EmoteReference.THINKING);
                    return;
                }

                if (giveTo.isBot()) {
                    ctx.sendLocalized("commands.transfer.bot_notice", EmoteReference.ERROR);
                    return;
                }

                if (!handleIncreasingRatelimit(rateLimiter, ctx.getAuthor(), ctx))
                    return;

                long toSend; // = 0 at the start

                try {
                    //Convert negative values to absolute.
                    toSend = Math.abs(new RoundedMetricPrefixFormat().parseObject(args[1], new ParsePosition(0)));
                } catch (Exception e) {
                    ctx.sendLocalized("commands.transfer.no_amount", EmoteReference.ERROR);
                    return;
                }

                if (toSend == 0) {
                    ctx.sendLocalized("commands.transfer.no_money_specified_notice", EmoteReference.ERROR);
                    return;
                }

                if (Items.fromAnyNoId(args[1]).isPresent()) {
                    ctx.sendLocalized("commands.transfer.item_transfer", EmoteReference.ERROR);
                    return;
                }

                if (toSend > TRANSFER_LIMIT) {
                    ctx.sendLocalized("commands.transfer.over_transfer_limit", EmoteReference.ERROR, TRANSFER_LIMIT);
                    return;
                }

                Player transferPlayer = ctx.getPlayer();
                Player toTransfer = ctx.getPlayer(giveTo);

                if (transferPlayer.isLocked()) {
                    ctx.sendLocalized("commands.transfer.own_locked_notice", EmoteReference.ERROR);
                    return;
                }

                if (transferPlayer.getMoney() < toSend) {
                    ctx.sendLocalized("commands.transfer.no_money_notice", EmoteReference.ERROR);
                    return;
                }

                if (toTransfer.isLocked()) {
                    ctx.sendLocalized("commands.transfer.receipt_locked_notice", EmoteReference.ERROR);
                    return;
                }

                if (toTransfer.getMoney() > (long) TRANSFER_LIMIT * 18) {
                    ctx.sendLocalized("commands.transfer.receipt_over_limit", EmoteReference.ERROR);
                    return;
                }

                String partyKey = ctx.getAuthor().getId() + ":" + giveTo.getId();
                if (!partyRateLimiter.process(partyKey)) {
                    ctx.getChannel().sendMessage(
                            EmoteReference.STOPWATCH +
                                    String.format(ctx.getLanguageContext().get("commands.transfer.party"), giveTo.getName()) + " (Ratelimited)" +
                                    "\n **You'll be able to transfer to this user again in " + Utils.formatDuration(partyRateLimiter.tryAgainIn(partyKey))
                                    + ".**"
                    ).queue();

                    Utils.ratelimitedUsers.computeIfAbsent(ctx.getAuthor().getIdLong(), __ -> new AtomicInteger()).incrementAndGet();
                    return;
                }

                long amountTransfer = Math.round(toSend * 0.92);

                if (toTransfer.addMoney(amountTransfer)) {
                    transferPlayer.removeMoney(toSend);
                    transferPlayer.saveAsync();

                    ctx.sendLocalized("commands.transfer.success", EmoteReference.CORRECT, toSend, amountTransfer, giveTo.getName());
                    toTransfer.saveAsync();
                    rateLimiter.limit(toTransfer.getUserId());
                } else {
                    ctx.sendLocalized("commands.transfer.receipt_overflow_notice", EmoteReference.ERROR);
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Transfers money from you to another player.\n" +
                                "The maximum amount you can transfer at once is " + TRANSFER_LIMIT + " credits.")
                        .setUsage("`~>transfer <@user> <money>` - Transfers money to x player")
                        .addParameter("@user", "The user to send the money to. You have to mention (ping) the user.")
                        .addParameter("money", "How much money to transfer.")
                        .build();
            }
        });
    }

    @Subscribe
    public void lootcrate(CommandRegistry registry) {
        registry.register("opencrate", new SimpleCommand(Category.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                //Argument parsing.
                Map<String, String> t = ctx.getOptionalArguments();
                content = Utils.replaceArguments(t, content, "season", "s").trim();
                boolean isSeasonal = ctx.isSeasonal();

                Player p = ctx.getPlayer();
                SeasonPlayer sp = ctx.getSeasonPlayer();
                Item item = Items.fromAnyNoId(content.replace("\"", "")).orElse(null);

                //Open default crate if nothing's specified.
                if (item == null || content.isEmpty())
                    item = Items.LOOT_CRATE;

                if (item.getItemType() != ItemType.CRATE) {
                    ctx.sendLocalized("commands.opencrate.not_crate", EmoteReference.ERROR);
                    return;
                }

                boolean containsItem = isSeasonal ? sp.getInventory().containsItem(item) : p.getInventory().containsItem(item);
                if (!containsItem) {
                    ctx.sendLocalized("commands.opencrate.no_crate", EmoteReference.SAD, item.getName());
                    return;
                }

                //Ratelimit handled here
                //Check Items.openLootCrate for implementation details.
                item.getAction().test(ctx.getEvent(), Pair.of(ctx.getLanguageContext(), content), isSeasonal);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Opens a loot crate.")
                        .setUsage("`~>opencrate <name>` - Opens a loot crate.\n" +
                                "You need a crate key to open any crate.")
                        .setSeasonal(true)
                        .addParameter("name", "The loot crate name. If you don't provide this, a default loot crate will attempt to open.")
                        .build();
            }
        });
    }

    @Subscribe
    public void openPremiumCrate(CommandRegistry cr) {
        final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                .limit(1)
                .spamTolerance(2)
                .cooldown(24, TimeUnit.HOURS)
                .maxCooldown(24, TimeUnit.HOURS)
                .randomIncrement(false)
                .pool(MantaroData.getDefaultJedisPool())
                .prefix("dailycrate")
                .build();

        SecureRandom random = new SecureRandom();
        cr.register("dailycrate", new SimpleCommand(Category.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (!ctx.getDBUser().isPremium()) {
                    ctx.sendLocalized("commands.dailycrate.not_premium", EmoteReference.ERROR);
                    return;
                }

                Player p = ctx.getPlayer();
                Inventory inventory = p.getInventory();
                I18nContext languageContext = ctx.getLanguageContext();

                if (!handleIncreasingRatelimit(rateLimiter, ctx.getAuthor(), ctx))
                    return;

                Item randomCrate = random.nextBoolean() ? Items.MINE_PREMIUM_CRATE : Items.FISH_PREMIUM_CRATE;

                inventory.process(new ItemStack(randomCrate, 1));
                p.save();

                var successMessage = String.format(languageContext.get("commands.dailycrate.success"), EmoteReference.POPPER, randomCrate.getName()) +
                        "\n" + languageContext.get("commands.daily.sellout.already_premium");

                ctx.send(successMessage);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Opens a daily premium loot crate.")
                        .setUsage("`~>dailycrate` - Opens daily premium loot crate.\n" +
                                "You need a crate key to open any crate.")
                        .build();
            }
        });
    }

    @Subscribe
    public void useItem(CommandRegistry cr) {
        TreeCommand ui = (TreeCommand) cr.register("useitem", new TreeCommand(Category.CURRENCY) {
            @Override
            public Command defaultTrigger(Context ctx, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, String content) {
                        String[] args = ctx.getArguments();
                        Map<String, String> t = ctx.getOptionalArguments();

                        if (content.isEmpty()) {
                            ctx.sendLocalized("commands.useitem.no_items_specified", EmoteReference.ERROR);
                            return;
                        }

                        Item item = Items.fromAnyNoId(args[0]).orElse(null);
                        //Well, shit.
                        if (item == null) {
                            ctx.sendLocalized("general.item_lookup.not_found", EmoteReference.ERROR);
                            return;
                        }

                        if (item.getItemType() != ItemType.INTERACTIVE && item.getItemType() != ItemType.CRATE &&
                                item.getItemType() != ItemType.POTION && item.getItemType() != ItemType.BUFF) {
                            ctx.sendLocalized("commands.useitem.not_interactive", EmoteReference.ERROR);
                            return;
                        }

                        if (item.getAction() == null && (item.getItemType() != ItemType.POTION && item.getItemType() != ItemType.BUFF)) {
                            ctx.sendLocalized("commands.useitem.interactive_no_action", EmoteReference.ERROR);
                            return;
                        }

                        Player p = ctx.getPlayer();
                        if (!p.getInventory().containsItem(item)) {
                            ctx.sendLocalized("commands.useitem.no_item", EmoteReference.SAD);
                            return;
                        }

                        applyPotionEffect(ctx.getEvent(), item, p, t, content, false, ctx.getLanguageContext());
                    }
                };
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Uses an item.\n" +
                                "You need to have the item to use it, and the item has to be marked as *interactive*.")
                        .setUsage("`~>useitem <item> [-amount]` - Uses the specified item")
                        .addParameter("item", "The item name or emoji. If the name contains spaces \"wrap it in quotes\"")
                        .addParameterOptional("-amount", "The amount of items you want to use. Only works with potions/buffs.")
                        .build();
            }
        });

        ui.addSubCommand("ls", new SubCommand() {
            @Override
            public String description() {
                return "Lists all usable (interactive) items.";
            }

            @Override
            protected void call(Context ctx, String content) {
                List<Item> interactiveItems = Arrays.stream(Items.ALL).filter(
                        i -> i.getItemType() == ItemType.INTERACTIVE || i.getItemType() == ItemType.POTION || i.getItemType() == ItemType.CRATE || i.getItemType() == ItemType.BUFF
                ).collect(Collectors.toList());

                I18nContext languageContext = ctx.getLanguageContext();
                StringBuilder show = new StringBuilder();

                show.append(EmoteReference.TALKING)
                        .append(languageContext.get("commands.useitem.ls.desc"))
                        .append("\n\n");

                for (Item item : interactiveItems) {
                    show.append(EmoteReference.BLUE_SMALL_MARKER)
                            .append(item.getEmoji())
                            .append(" **")
                            .append(item.getName())
                            .append("**\n")
                            .append("**")
                            .append(languageContext.get("general.description"))
                            .append(": **\u2009*")
                            .append(languageContext.get(item.getDesc()))
                            .append("*")
                            .append("\n");
                }

                ctx.send(new EmbedBuilder()
                        .setAuthor(languageContext.get("commands.useitem.ls.header"), null, ctx.getAuthor().getEffectiveAvatarUrl())
                        .setDescription(show.toString())
                        .setColor(Color.PINK)
                        .setFooter(String.format(languageContext.get("general.requested_by"), ctx.getMember().getEffectiveName()), null)
                        .build()
                );
            }
        });

        ui.createSubCommandAlias("ls", "list");
        ui.createSubCommandAlias("ls", "Is");
    }
}
