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
import net.kodehawa.mantarobot.commands.currency.item.*;
import net.kodehawa.mantarobot.commands.currency.item.special.Potion;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.CustomFinderUtil;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils.ratelimit;

@Module
public class CurrencyCmds {
    @Subscribe
    public void inventory(CommandRegistry cr) {
        final Random r = new Random();
        cr.register("inventory", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            public void call(Context ctx, String content, String[] args) {
                var arguments = ctx.getOptionalArguments();
                content = Utils.replaceArguments(arguments, content,
                        "brief", "calculate", "calc", "c", "info", "full", "season", "s");

                // Lambda memes lol
                var finalContent = content;

                ctx.findMember(content, ctx.getMessage()).onSuccess(members -> {
                    var member = CustomFinderUtil.findMemberDefault(finalContent, members, ctx, ctx.getMember());
                    if (member == null)
                        return;

                    if (member.getUser().isBot()) {
                        ctx.sendLocalized("commands.inventory.bot_notice", EmoteReference.ERROR);
                        return;
                    }

                    final var player = ctx.getPlayer(member);
                    final var playerData = player.getData();
                    final var user = ctx.getDBUser(member);
                    final var seasonPlayer = ctx.getSeasonPlayer(member);
                    final var languageContext = ctx.getLanguageContext();
                    var playerInventory = player.getInventory();

                    if (ctx.isSeasonal()) {
                        playerInventory = seasonPlayer.getInventory();
                    }

                    final var inventoryList = playerInventory.asList();

                    if (arguments.containsKey("calculate") || arguments.containsKey("calc") || arguments.containsKey("c")) {
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

                    if (arguments.containsKey("info") || arguments.containsKey("full")) {
                        EmbedBuilder builder = baseEmbed(ctx,
                                languageContext.get("commands.inventory.header").formatted(member.getEffectiveName()),
                                member.getUser().getEffectiveAvatarUrl()
                        );

                        List<MessageEmbed.Field> fields = new LinkedList<>();
                        if (inventoryList.isEmpty())
                            builder.setDescription(languageContext.get("general.dust"));
                        else {
                            playerInventory.asList()
                                    .stream()
                                    .sorted(playerData.getInventorySortType().getSort().getComparator())
                                    .forEach(stack -> {
                                        long buyValue = stack.getItem().isBuyable() ? stack.getItem().getValue() : 0;
                                        long sellValue = stack.getItem().isSellable() ? (long) (stack.getItem().getValue() * 0.9) : 0;
                                        fields.add(new MessageEmbed.Field(
                                                "%s %s x %d".formatted(
                                                        stack.getItem().getEmoji(),
                                                        stack.getItem().getName(),
                                                        stack.getAmount()),
                                                languageContext.get("commands.inventory.format").formatted(
                                                        buyValue, sellValue,
                                                        languageContext.get(stack.getItem().getDesc())
                                                ), false)
                                        );
                                    });
                        }

                        var toShow = languageContext.get("commands.inventory.brief_notice") +
                                (r.nextInt(3) == 0 && !user.isPremium() ? languageContext.get("general.sellout") : "");

                        DiscordUtils.sendPaginatedEmbed(ctx, builder, DiscordUtils.divideFields(6, fields), toShow);
                        return;
                    }

                    var inventory = languageContext.get("commands.inventory.sorted_by")
                            .formatted(playerData
                                    .getInventorySortType()
                                    .toString()
                                    .toLowerCase()
                                    .replace("_", " ")
                            ) + "\n\n" +
                            inventoryList.stream()
                                    .sorted(playerData.getInventorySortType().getSort().getComparator())
                                    .map(is -> is.getItem().getEmoji() + " x" + is.getAmount() + " \u2009\u2009")
                                    .collect(Collectors.joining(" "));

                    var message = ctx.getLanguageContext().get("commands.inventory.brief")
                            .formatted(member.getEffectiveName(), inventory);

                    var toSend = new MessageBuilder().append(message).buildAll(MessageBuilder.SplitPolicy.SPACE);
                    toSend.forEach(ctx::send);
                });
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Shows your current inventory.")
                        .setUsage("""
                                You can mention someone on this command to see their inventory.
                                You can use `~>inventory -full` to a more detailed version.
                                Use `~>inventory -calculate` to see how much you'd get if you sell every sellable item on your inventory.""")
                        .setSeasonal(true)
                        .build();
            }
        });

        cr.registerAlias("inventory", "inv");
    }

    @Subscribe
    public void level(CommandRegistry cr) {
        cr.register("level", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                ctx.findMember(content, ctx.getMessage()).onSuccess(members -> {
                    var member = ctx.getMember();

                    if (!content.isEmpty()) {
                        member = CustomFinderUtil.findMember(content, members, ctx);
                    }

                    if (member == null) {
                        return;
                    }

                    if (member.getUser().isBot()) {
                        ctx.sendLocalized("commands.level.bot_notice", EmoteReference.ERROR);
                        return;
                    }

                    var player = ctx.getPlayer(member);
                    var experienceNext =
                            (long) (player.getLevel() * Math.log10(player.getLevel()) * 1000) + (50 * player.getLevel() / 2);

                    if (member.getUser().getIdLong() == ctx.getAuthor().getIdLong()) {
                        ctx.sendLocalized("commands.level.own_success",
                                EmoteReference.ZAP, player.getLevel(), player.getData().getExperience(), experienceNext
                        );
                    } else {
                        ctx.sendLocalized("commands.level.success_other",
                                EmoteReference.ZAP, member.getUser().getAsTag(), player.getLevel(),
                                player.getData().getExperience(), experienceNext
                        );
                    }
                });
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Checks your level or the level of another user.")
                        .setUsage("~>level [user]")
                        .addParameterOptional("user",
                                "The user to check the id of. Can be a mention, tag or id.")
                        .build();
            }
        });
    }

    @Subscribe
    public void lootcrate(CommandRegistry registry) {
        registry.register("opencrate", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                var arguments = ctx.getOptionalArguments();
                content = Utils.replaceArguments(arguments, content, "season", "s").trim();

                var isSeasonal = ctx.isSeasonal();

                var player = ctx.getPlayer();
                var seasonPlayer = ctx.getSeasonPlayer();
                var item = ItemHelper.fromAnyNoId(content.replace("\"", ""), ctx.getLanguageContext())
                        .orElse(null);

                //Open default crate if nothing's specified.
                if (item == null || content.isEmpty()) {
                    item = ItemReference.LOOT_CRATE;
                }

                if (item.getItemType() != ItemType.CRATE) {
                    ctx.sendLocalized("commands.opencrate.not_crate", EmoteReference.ERROR);
                    return;
                }

                var containsItem = isSeasonal ?
                        seasonPlayer.getInventory().containsItem(item) :
                        player.getInventory().containsItem(item);

                if (!containsItem) {
                    ctx.sendLocalized("commands.opencrate.no_crate", EmoteReference.SAD, item.getName());
                    return;
                }

                //Ratelimit handled here
                //Check Items.openLootCrate for implementation details.
                item.getAction().test(ctx, isSeasonal);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Opens a loot crate.")
                        .setUsage("`~>opencrate <name>` - Opens a loot crate.\n" +
                                "You need a crate key to open any crate.")
                        .setSeasonal(true)
                        .addParameter("name",
                                "The loot crate name. If you don't provide this, a default loot crate will attempt to open.")
                        .build();
            }
        });
    }

    @Subscribe
    public void openPremiumCrate(CommandRegistry cr) {
        final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                .limit(1)
                .cooldown(24, TimeUnit.HOURS)
                .maxCooldown(24, TimeUnit.HOURS)
                .randomIncrement(false)
                .pool(MantaroData.getDefaultJedisPool())
                .prefix("dailycrate")
                .build();

        cr.register("dailycrate", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (!ctx.getDBUser().isPremium()) {
                    ctx.sendLocalized("commands.dailycrate.not_premium", EmoteReference.ERROR);
                    return;
                }

                if (args.length > 0 && args[0].equalsIgnoreCase("-check")) {
                    long rl = rateLimiter.getRemaniningCooldown(ctx.getAuthor());

                    ctx.sendLocalized("commands.dailycrate.check", EmoteReference.TALKING,
                            (rl) > 0 ? Utils.formatDuration(rl) :
                                    //Yes, this is intended to be daily.about_now, just reusing strings.
                                    ctx.getLanguageContext().get("commands.daily.about_now")
                    );
                    return;
                }

                final var player = ctx.getPlayer();
                final var playerData = player.getData();
                final var inventory = player.getInventory();
                final var languageContext = ctx.getLanguageContext();

                if (!ratelimit(rateLimiter, ctx, false)) {
                    return;
                }

                // Alternate between mine and fish crates instead of doing so at random, since at random
                // it might seem like it only gives one sort of crate.
                var crate = playerData.getLastCrateGiven() == ItemHelper.idOf(ItemReference.MINE_PREMIUM_CRATE) ?
                        ItemReference.FISH_PREMIUM_CRATE : ItemReference.MINE_PREMIUM_CRATE;

                inventory.process(new ItemStack(crate, 1));
                playerData.setLastCrateGiven(ItemHelper.idOf(crate));
                player.save();

                var successMessage =
                        languageContext.get("commands.dailycrate.success")
                                .formatted(
                                        EmoteReference.POPPER,
                                        crate.getName()
                                ) + "\n" + languageContext.get("commands.daily.sellout.already_premium");

                ctx.send(successMessage);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Opens a daily premium loot crate.")
                        .setUsage("""
                                  `~>dailycrate` - Opens a daily premium loot crate.
                                  You need a crate key to open any crate. Use `-check` to check when you can claim it.
                                  """
                        )
                        .addParameterOptional("-check", "Check the time left for you to be able to claim it.")
                        .build();
            }
        });
    }

    @Subscribe
    public void useItem(CommandRegistry cr) {
        TreeCommand ui = cr.register("useitem", new TreeCommand(CommandCategory.CURRENCY) {
            @Override
            public Command defaultTrigger(Context ctx, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, I18nContext languageContext, String content) {
                        String[] args = ctx.getArguments();
                        var arguments = ctx.getOptionalArguments();

                        if (content.isEmpty()) {
                            ctx.sendLocalized("commands.useitem.no_items_specified", EmoteReference.ERROR);
                            return;
                        }

                        var item = ItemHelper.fromAnyNoId(args[0], ctx.getLanguageContext()).orElse(null);

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

                        var player = ctx.getPlayer();
                        if (!player.getInventory().containsItem(item)) {
                            ctx.sendLocalized("commands.useitem.no_item", EmoteReference.SAD);
                            return;
                        }

                        applyPotionEffect(ctx, item, player, arguments);
                    }
                };
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription(
                                """
                                Uses an item.
                                You need to have the item to use it, and the item has to be marked as *interactive*.
                                """
                        )
                        .setUsage("`~>useitem <item> [-amount <number>]` - Uses the specified item")
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
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var interactiveItems = Arrays.stream(ItemReference.ALL).filter(
                        i -> i.getItemType() == ItemType.INTERACTIVE ||
                        i.getItemType() == ItemType.POTION ||
                        i.getItemType() == ItemType.CRATE ||
                        i.getItemType() == ItemType.BUFF
                ).collect(Collectors.toList());
                var show = new StringBuilder();

                show.append(EmoteReference.TALKING)
                        .append(languageContext.get("commands.useitem.ls.desc"))
                        .append("\n\n");

                for (var item : interactiveItems) {
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
                        .setAuthor(languageContext.get("commands.useitem.ls.header"), null,
                                ctx.getAuthor().getEffectiveAvatarUrl()
                        )
                        .setDescription(show.toString())
                        .setColor(Color.PINK)
                        .setFooter(languageContext.get("general.requested_by")
                                        .formatted(ctx.getMember().getEffectiveName()),
                                null
                        ).build()
                );
            }
        });

        ui.createSubCommandAlias("ls", "list");
        ui.createSubCommandAlias("ls", "Is");
    }

    public static void applyPotionEffect(Context ctx, Item item, Player player, Map<String, String> arguments) {

        if ((item.getItemType() == ItemType.POTION || item.getItemType() == ItemType.BUFF) && item instanceof Potion) {
            var dbUser = ctx.getDBUser();
            var userData = dbUser.getData();

            // Yes, parser limitations. Natan change to your parser eta wen :^), really though, we could use some generics on here lol
            // NumberFormatException?
            int amount = 1;
            if (arguments.containsKey("amount")) {
                try {
                    amount = Math.abs(Integer.parseInt(arguments.get("amount")));
                } catch (NumberFormatException e) {
                    ctx.sendLocalized("commands.useitem.invalid_amount", EmoteReference.WARNING);
                    return;
                }
            }

            final var equippedItems = userData.getEquippedItems();

            var type = equippedItems.getTypeFor(item);

            if (amount < 1) {
                ctx.sendLocalized("commands.useitem.too_little", EmoteReference.SAD);
                return;
            }

            if (player.getInventory().getAmount(item) < amount) {
                ctx.sendLocalized("commands.useitem.not_enough_items", EmoteReference.SAD);
                return;
            }

            var currentPotion = equippedItems.getCurrentEffect(type);
            var activePotion = equippedItems.isEffectActive(type, ((Potion) item).getMaxUses());
            var isActive = currentPotion != null && currentPotion.getAmountEquipped() > 1;

            // This used to only check for activePotion.
            // The issue with this was that there could be one potion that was fully used, but there was another potion
            // waiting to be used. In that case the potion would get overridden.
            // In case you have more than a potion equipped, we'll just stack the rest as necessary.
            if (activePotion || isActive) {
                //Currently has a potion equipped, but wants to stack a potion of other type.
                if (currentPotion.getPotion() != ItemHelper.idOf(item)) {
                    ctx.sendLocalized("general.misc_item_usage.not_same_potion",
                            EmoteReference.ERROR,
                            ItemHelper.fromId(currentPotion.getPotion()).getName(),
                            item.getName()
                    );

                    return;
                }

                var amountEquipped = currentPotion.getAmountEquipped();
                var attempted = amountEquipped + amount;

                // Currently has a potion equipped, and is of the same type.
                if (attempted < 16) {
                    currentPotion.equip(activePotion ? amount : Math.max(1, amount - 1));
                    var equipped = currentPotion.getAmountEquipped();

                    ctx.sendLocalized("general.misc_item_usage.potion_applied_multiple",
                            EmoteReference.CORRECT, item.getName(), Utils.capitalize(type.toString()), equipped
                    );
                } else {
                    // Too many stacked (max: 15).
                    ctx.sendLocalized("general.misc_item_usage.max_stack_size", EmoteReference.ERROR, item.getName(), attempted);
                    return;
                }
            } else {
                // No potion stacked.
                var effect = new PotionEffect(ItemHelper.idOf(item), 0, ItemType.PotionType.PLAYER);

                // If there's more than 1, proceed to equip the stacks.
                if (amount > 15) {
                    //Too many stacked (max: 15).
                    ctx.sendLocalized("general.misc_item_usage.max_stack_size_2", EmoteReference.ERROR, item.getName(), amount);
                    return;
                }

                if (amount > 1) {
                    effect.equip(amount - 1); // Amount - 1 because we're technically using one.
                }

                // Apply the effect.
                equippedItems.applyEffect(effect);
                ctx.sendLocalized("general.misc_item_usage.potion_applied",
                        EmoteReference.CORRECT, item.getName(), Utils.capitalize(type.toString()), amount
                );
            }


            if (amount > 12) {
                player.getData().addBadgeIfAbsent(Badge.MAD_SCIENTIST);
            }

            // Default: 1
            player.getInventory().process(new ItemStack(item, -amount));
            player.save();
            dbUser.save();

            return;
        }

        item.getAction().test(ctx, false);
    }
}
