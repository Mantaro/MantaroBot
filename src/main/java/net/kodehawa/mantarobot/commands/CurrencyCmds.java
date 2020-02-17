/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.item.*;
import net.kodehawa.mantarobot.commands.currency.item.special.Potion;
import net.kodehawa.mantarobot.commands.currency.pets.Pet;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.currency.seasons.SeasonPlayer;
import net.kodehawa.mantarobot.commands.utils.RoundedMetricPrefixFormat;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.RateLimiter;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.*;
import java.text.ParsePosition;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.kodehawa.mantarobot.utils.Utils.handleDefaultIncreasingRatelimit;

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
            public void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                Map<String, String> t = getArguments(args);
                content = Utils.replaceArguments(t, content, "brief", "calculate", "calc", "c", "info", "full", "season", "s");
                Member member = Utils.findMember(event, event.getMember(), content);

                if (member == null)
                    return;

                if (member.getUser().isBot()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.inventory.bot_notice"), EmoteReference.ERROR).queue();
                    return;
                }

                Player player = MantaroData.db().getPlayer(member);
                SeasonPlayer seasonPlayer = MantaroData.db().getPlayerForSeason(member, getConfig().getCurrentSeason());
                Inventory playerInventory = player.getInventory();
                final List<ItemStack> inventoryList = playerInventory.asList();

                if (t.containsKey("season") || t.containsKey("s")) {
                    playerInventory = seasonPlayer.getInventory();
                }

                if (t.containsKey("calculate") || t.containsKey("calc") || t.containsKey("c")) {
                    long all = playerInventory.asList().stream()
                            .filter(item -> item.getItem().isSellable())
                            .mapToLong(value -> (long) (value.getItem().getValue() * value.getAmount() * 0.9d))
                            .sum();

                    event.getChannel().sendMessageFormat(languageContext.get("commands.inventory.calculate"), EmoteReference.DIAMOND, member.getUser().getName(), all).queue();
                    return;
                }

                if (inventoryList.isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.inventory.empty"), EmoteReference.WARNING).queue();
                    return;
                }

                if (t.containsKey("info") || t.containsKey("full")) {
                    EmbedBuilder builder = baseEmbed(event, String.format(languageContext.get("commands.inventory.header"), member.getEffectiveName()), member.getUser().getEffectiveAvatarUrl());

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
                    boolean hasReactionPerms = event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_ADD_REACTION);

                    if (hasReactionPerms) {
                        if (builder.getDescriptionBuilder().length() == 0) {
                            builder.setDescription(String.format(languageContext.get("general.buy_sell_paged_react"), splitFields.size(),
                                    String.format(languageContext.get("general.buy_sell_paged_reference"), EmoteReference.BUY, EmoteReference.SELL))
                                    + "\n" + languageContext.get("commands.inventory.brief_notice") + (r.nextInt(3) == 0 ? languageContext.get("general.sellout") : ""));
                        }
                        DiscordUtils.list(event, 100, false, builder, splitFields);
                    } else {
                        if (builder.getDescriptionBuilder().length() == 0) {
                            builder.setDescription(String.format(languageContext.get("general.buy_sell_paged_text"), splitFields.size(),
                                    String.format(languageContext.get("general.buy_sell_paged_reference"), EmoteReference.BUY, EmoteReference.SELL))
                                    + "\n" + languageContext.get("commands.inventory.brief_notice") + (r.nextInt(3) == 0 ? languageContext.get("general.sellout") : ""));
                        }
                        DiscordUtils.listText(event, 100, false, builder, splitFields);
                    }

                    return;
                }

                new MessageBuilder().setContent(String.format(languageContext.get("commands.inventory.brief"), member.getEffectiveName(), ItemStack.toString(playerInventory.asList())))
                        .stripMentions(event.getGuild(), Message.MentionType.EVERYONE, Message.MentionType.HERE)
                        .sendTo(event.getChannel())
                        .queue();
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
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                Member member = Utils.findMember(event, event.getMember(), content);
                TextChannel channel = event.getChannel();

                if (member == null)
                    return;

                if (member.getUser().isBot()) {
                    channel.sendMessageFormat(languageContext.get("commands.level.bot_notice"), EmoteReference.ERROR).queue();
                    return;
                }

                Player player = MantaroData.db().getPlayer(member);
                long experienceNext = (long) (player.getLevel() * Math.log10(player.getLevel()) * 1000) + (50 * player.getLevel() / 2);

                if (member.getUser().getIdLong() == event.getAuthor().getIdLong()) {
                    channel.sendMessageFormat(languageContext.get("commands.level.own_success"),
                            EmoteReference.ZAP, player.getLevel(), player.getData().getExperience(), experienceNext
                    ).queue();
                } else {
                    channel.sendMessageFormat(languageContext.get("commands.level.success_other"),
                            EmoteReference.ZAP, member.getUser().getAsTag(), player.getLevel(), player.getData().getExperience(), experienceNext
                    ).queue();
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
    public void market(CommandRegistry cr) {
        final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                .limit(1)
                .spamTolerance(2)
                .cooldown(6, TimeUnit.SECONDS)
                .maxCooldown(6, TimeUnit.SECONDS)
                .randomIncrement(true)
                .pool(MantaroData.getDefaultJedisPool())
                .prefix("market")
                .premiumAware(true)
                .build();


        TreeCommand marketCommand = (TreeCommand) cr.register("market", new TreeCommand(Category.CURRENCY) {
            @Override
            public Command defaultTrigger(GuildMessageReceivedEvent event, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                        EmbedBuilder embed = baseEmbed(event, languageContext.get("commands.market.header"))
                                .setThumbnail("https://png.icons8.com/metro/540/shopping-cart.png");
                        List<MessageEmbed.Field> fields = new LinkedList<>();
                        Stream.of(Items.ALL).forEach(item -> {
                            if (!item.isPetOnly() && !item.isHidden() && item.getItemType() != ItemType.PET) {
                                String buyValue = item.isBuyable() ? String.format("$%d", item.getValue()) : "N/A";
                                String sellValue = item.isSellable() ? String.format("$%d", (int) Math.floor(item.getValue() * 0.9)) : "N/A";

                                fields.add(new MessageEmbed.Field(String.format("%s %s", item.getEmoji(), item.getName()),
                                        (languageContext.getContextLanguage().equals("en_US") ? "" : " (" + languageContext.get(item.getTranslatedName()) + ")\n") +
                                                EmoteReference.BUY + buyValue + " " + EmoteReference.SELL + sellValue, true)
                                );
                            }
                        });

                        List<List<MessageEmbed.Field>> splitFields = DiscordUtils.divideFields(8, fields);
                        boolean hasReactionPerms = event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_ADD_REACTION);

                        if (hasReactionPerms) {
                            embed.setDescription(String.format(languageContext.get("general.buy_sell_paged_react"), splitFields.size(),
                                    String.format(languageContext.get("general.buy_sell_paged_reference") + "\n" + String.format(languageContext.get("general.reaction_timeout"), 120), EmoteReference.BUY, EmoteReference.SELL)) + "\n"
                                    + languageContext.get("general.sellout") + languageContext.get("commands.market.reference"));
                            DiscordUtils.list(event, 120, false, embed, splitFields);
                        } else {
                            embed.setDescription(String.format(languageContext.get("general.buy_sell_paged_text"), splitFields.size(),
                                    String.format(languageContext.get("general.buy_sell_paged_reference") + "\n" + String.format(languageContext.get("general.reaction_timeout"), 120), EmoteReference.BUY, EmoteReference.SELL)) + "\n"
                                    + languageContext.get("general.sellout") + languageContext.get("commands.market.reference"));
                            DiscordUtils.listText(event, 120, false, embed, splitFields);
                        }
                    }
                };
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("List current items for buying and selling.")
                        .setUsage("To buy an item do `~>market buy <item>`. It will subtract the value from your money and give you the item.\n" +
                                "To sell do `~>market sell all` to sell all your items or `~>market sell <item>` to sell the specified item.\n" +
                                "If the item name contains spaces, \"wrap it in quotes\".\n" +
                                "To buy and sell multiple items you need to do `~>market <buy/sell> <amount> <item>`\n")
                        .addParameter("item", "The item name or emoji")
                        .setSeasonal(true)
                        .build();
            }
        });

        marketCommand.setPredicate((event) -> {
            if (!handleDefaultIncreasingRatelimit(rateLimiter, event.getAuthor(), event, null, false))
                return false;

            Player player = MantaroData.db().getPlayer(event.getMember());
            if (player.isLocked()) {
                event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot access the market now.").queue();
                return false;
            }

            return true;
        });

        marketCommand.addSubCommand("dump", new SubCommand() {
            @Override
            public String description() {
                return "Dumps an item. Usage: `~>market dump <item>`";
            }

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                TextChannel channel = event.getChannel();

                if (content.isEmpty()) {
                    channel.sendMessageFormat(languageContext.get("commands.market.dump.no_item"), EmoteReference.ERROR).queue();
                    return;
                }

                Map<String, String> t = getArguments(content);
                boolean isSeasonal = t.containsKey("season") || t.containsKey("s");
                content = Utils.replaceArguments(t, content, "season", "s").trim();

                String[] args = content.split(" ");
                String itemName = content;
                int itemNumber = 1;
                boolean isMassive = !itemName.isEmpty() && itemName.split(" ")[0].matches("^[0-9]*$");
                if (isMassive) {
                    try {
                        itemNumber = Math.abs(Integer.parseInt(itemName.split(" ")[0]));
                        itemName = itemName.replace(args[0], "").trim();
                    } catch (NumberFormatException e) {
                        channel.sendMessageFormat(languageContext.get("commands.market.dump.invalid"), EmoteReference.ERROR).queue();
                        return;
                    }
                }

                Item item = Items.fromAny(itemName).orElse(null);

                if (item == null) {
                    channel.sendMessageFormat(languageContext.get("commands.market.dump.non_existent"), EmoteReference.ERROR).queue();
                    return;
                }

                Player player = MantaroData.db().getPlayer(event.getAuthor());
                SeasonPlayer seasonalPlayer = MantaroData.db().getPlayerForSeason(event.getAuthor(), getConfig().getCurrentSeason());

                Inventory playerInventory = isSeasonal ? seasonalPlayer.getInventory() : player.getInventory();

                if (!playerInventory.containsItem(item)) {
                    channel.sendMessageFormat(languageContext.get("commands.market.dump.player_no_item"), EmoteReference.ERROR).queue();
                    return;
                }

                if (playerInventory.getAmount(item) < itemNumber) {
                    channel.sendMessageFormat(languageContext.get("commands.market.dump.more_items_than_player"), EmoteReference.ERROR).queue();
                    return;
                }

                playerInventory.process(new ItemStack(item, -itemNumber));
                if (isSeasonal)
                    seasonalPlayer.saveAsync();
                else
                    player.saveAsync();

                channel.sendMessageFormat(languageContext.get("commands.market.dump.success"),
                        EmoteReference.CORRECT, itemNumber, item.getEmoji(), item.getName()).queue();
            }
        }).createSubCommandAlias("dump", "trash");

        marketCommand.addSubCommand("price", new SubCommand() {
            @Override
            public String description() {
                return "Checks the price of any given item. Usage: `~>market price <item>`";
            }

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                String[] args = content.split(" ");
                String itemName = content.replace(args[0] + " ", "");
                Item item = Items.fromAny(itemName).orElse(null);

                if (item == null) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.market.price.non_existent"), EmoteReference.ERROR).queue();
                    return;
                }

                if (!item.isBuyable() && !item.isSellable()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.market.price.no_price"), EmoteReference.THINKING).queue();
                    return;
                }

                if (!item.isBuyable()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.market.price.collectible"),
                            EmoteReference.EYES, (int) (item.getValue() * 0.9)).queue();
                    return;
                }

                event.getChannel().sendMessageFormat(languageContext.get("commands.market.price.success"),
                        EmoteReference.MARKET, item.getEmoji(), item.getName(), item.getValue(), (int) (item.getValue() * 0.9)).queue();
            }
        });

        marketCommand.addSubCommand("sell", new SubCommand() {
            @Override
            public String description() {
                return "Sells an item. Usage: `~>market sell <item>`. You can sell multiple items if you put the amount before the item.\n" +
                        "Use `~>market sell all` to sell all of your items.";
            }

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                TextChannel channel = event.getChannel();

                if (content.isEmpty()) {
                    channel.sendMessageFormat(languageContext.get("commands.market.sell.no_item_amount"), EmoteReference.ERROR).queue();
                    return;
                }

                Player player = MantaroData.db().getPlayer(event.getMember());
                SeasonPlayer seasonalPlayer = MantaroData.db().getPlayerForSeason(event.getAuthor(), getConfig().getCurrentSeason());
                Map<String, String> t = getArguments(content);
                boolean isSeasonal = t.containsKey("season") || t.containsKey("s");
                content = Utils.replaceArguments(t, content, "season", "s").trim();

                String[] args = content.split(" ");
                String itemName = content;
                int itemNumber = 1;
                String split = args[0];
                boolean isMassive = !itemName.isEmpty() && split.matches("^[0-9]*$");
                if (isMassive) {
                    try {
                        itemNumber = Math.abs(Integer.parseInt(split));
                        itemName = itemName.replace(args[0], "").trim();
                    } catch (NumberFormatException e) {
                        channel.sendMessageFormat(languageContext.get("commands.market.sell.invalid"), EmoteReference.ERROR).queue();
                        return;
                    }
                }

                try {
                    if (args[0].equals("all") && !isSeasonal) {
                        channel.sendMessageFormat(languageContext.get("commands.market.sell.all.confirmation"), EmoteReference.WARNING).queue();
                        //Start the operation.
                        InteractiveOperations.create(channel, event.getAuthor().getIdLong(), 60, e -> {
                            if (!e.getAuthor().getId().equals(event.getAuthor().getId())) {
                                return Operation.IGNORED;
                            }

                            String c = e.getMessage().getContentRaw();

                            if (c.equalsIgnoreCase("yes")) {
                                long all = player.getInventory().asList().stream()
                                        .filter(item -> item.getItem().isSellable())
                                        .mapToLong(value -> (long) (value.getItem().getValue() * value.getAmount() * 0.9d))
                                        .sum();

                                player.getInventory().clearOnlySellables();
                                player.addMoney(all);

                                channel.sendMessageFormat(languageContext.get("commands.market.sell.all.success"), EmoteReference.MONEY, all).queue();

                                player.saveAsync();

                                return Operation.COMPLETED;
                            } else if (c.equalsIgnoreCase("no")) {
                                channel.sendMessageFormat(languageContext.get("commands.market.sell.all.cancelled"), EmoteReference.CORRECT).queue();
                                return Operation.COMPLETED;
                            }

                            return Operation.IGNORED;
                        });

                        return;
                    }

                    Inventory playerInventory = isSeasonal ? seasonalPlayer.getInventory() : player.getInventory();
                    Item toSell = Items.fromAny(itemName.replace("\"", "")).orElse(null);

                    if (toSell == null) {
                        channel.sendMessageFormat(languageContext.get("commands.market.sell.non_existent"), EmoteReference.ERROR).queue();
                        return;
                    }

                    if (!toSell.isSellable()) {
                        channel.sendMessageFormat(languageContext.get("commands.market.sell.no_sell_price"), EmoteReference.ERROR).queue();
                        return;
                    }

                    if (playerInventory.getAmount(toSell) < 1) {
                        channel.sendMessageFormat(languageContext.get("commands.market.sell.no_item_player"), EmoteReference.STOP).queue();
                        return;
                    }

                    if (playerInventory.getAmount(toSell) < itemNumber) {
                        channel.sendMessageFormat(languageContext.get("commands.market.sell.more_items_than_player"), EmoteReference.ERROR).queue();
                        return;
                    }

                    int many = itemNumber * -1;
                    long amount = Math.round((toSell.getValue() * 0.9)) * Math.abs(many);
                    playerInventory.process(new ItemStack(toSell, many));
                    if (isSeasonal)
                        seasonalPlayer.addMoney(amount);
                    else
                        player.addMoney(amount);

                    player.getData().setMarketUsed(player.getData().getMarketUsed() + 1);
                    channel.sendMessageFormat(languageContext.get("commands.market.sell.success"),
                            EmoteReference.CORRECT, Math.abs(many), toSell.getName(), amount).queue();

                    player.saveAsync();

                    if (isSeasonal)
                        seasonalPlayer.saveAsync();
                } catch (Exception e) {
                    channel.sendMessage(EmoteReference.ERROR + languageContext.get("general.invalid_syntax")).queue();
                }
            }
        });

        marketCommand.addSubCommand("buy", new SubCommand() {
            @Override
            public String description() {
                return "Buys an item. Usage: `~>market buy <item>`. You can buy multiple items if you put the amount before the item. " +
                        "You can use all, half and quarter to buy for ex., a quarter of 5000 items.";
            }

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                TextChannel channel = event.getChannel();
                if (content.isEmpty()) {
                    channel.sendMessageFormat(languageContext.get("commands.market.buy.no_item_amount"), EmoteReference.ERROR).queue();
                    return;
                }

                Player player = MantaroData.db().getPlayer(event.getMember());
                SeasonPlayer seasonalPlayer = MantaroData.db().getPlayerForSeason(event.getAuthor(), getConfig().getCurrentSeason());
                Map<String, String> t = getArguments(content);
                boolean isSeasonal = t.containsKey("season") || t.containsKey("s");
                content = Utils.replaceArguments(t, content, "season", "s").trim();

                String[] args = content.split(" ");
                String itemName = content;
                int itemNumber = 1;
                String split = args[0];
                boolean isMassive = !itemName.isEmpty() && split.matches("^[0-9]*$");
                if (isMassive) {
                    try {
                        itemNumber = Math.abs(Integer.parseInt(split));
                        itemName = itemName.replace(args[0], "").trim();
                    } catch (NumberFormatException e) {
                        channel.sendMessageFormat(languageContext.get("commands.market.buy.invalid"), EmoteReference.ERROR).queue();
                        return;
                    }
                } else {
                    //This is silly but works, people can stop asking about this now :o
                    if (!itemName.isEmpty()) {
                        switch (split) {
                            case "all":
                                itemNumber = ItemStack.MAX_STACK_SIZE;
                                break;
                            case "half":
                                itemNumber = ItemStack.MAX_STACK_SIZE / 2;
                                break;
                            case "quarter":
                                itemNumber = ItemStack.MAX_STACK_SIZE / 4;
                                break;
                            default:
                                break;
                        }

                        if (itemNumber > 1)
                            itemName = itemName.replace(args[0], "").trim();
                    }
                }

                final Item itemToBuy = Items.fromAnyNoId(itemName.replace("\"", "")).orElse(null);

                if (itemToBuy == null) {
                    channel.sendMessageFormat(languageContext.get("commands.market.buy.non_existent"), EmoteReference.ERROR).queue();
                    return;
                }

                try {
                    if (!itemToBuy.isBuyable() || itemToBuy.isPetOnly()) {
                        channel.sendMessageFormat(languageContext.get("commands.market.buy.no_buy_price"), EmoteReference.ERROR).queue();
                        return;
                    }

                    Inventory playerInventory = isSeasonal ? seasonalPlayer.getInventory() : player.getInventory();
                    ItemStack stack = playerInventory.getStackOf(itemToBuy);
                    if ((stack != null && !stack.canJoin(new ItemStack(itemToBuy, itemNumber))) || itemNumber > 5000) {
                        //assume overflow
                        channel.sendMessageFormat(languageContext.get("commands.market.buy.item_limit_reached"), EmoteReference.ERROR).queue();
                        return;
                    }

                    boolean removedMoney = isSeasonal ? seasonalPlayer.removeMoney(itemToBuy.getValue() * itemNumber) : player.removeMoney(itemToBuy.getValue() * itemNumber);

                    if (removedMoney) {
                        playerInventory.process(new ItemStack(itemToBuy, itemNumber));
                        player.getData().addBadgeIfAbsent(Badge.BUYER);
                        player.getData().setMarketUsed(player.getData().getMarketUsed() + 1);

                        //Due to player data being updated here too.
                        player.saveAsync();

                        if (isSeasonal)
                            seasonalPlayer.saveAsync();

                        long playerMoney = isSeasonal ? seasonalPlayer.getMoney() : player.getMoney();

                        channel.sendMessageFormat(languageContext.get("commands.market.buy.success"),
                                EmoteReference.OK, itemNumber, itemToBuy.getEmoji(), itemToBuy.getValue() * itemNumber, playerMoney).queue();

                    } else {
                        channel.sendMessageFormat(languageContext.get("commands.market.buy.not_enough_money"), EmoteReference.STOP).queue();
                    }
                } catch (Exception e) {
                    channel.sendMessage(EmoteReference.ERROR + languageContext.get("general.invalid_syntax")).queue();
                }
            }
        });

        cr.registerAlias("market", "shop");
    }

    @Subscribe
    public void transferItems(CommandRegistry cr) {
        cr.register("itemtransfer", new SimpleCommand(Category.CURRENCY) {
            final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                    .spamTolerance(2)
                    .limit(1)
                    .cooldown(20, TimeUnit.SECONDS)
                    .cooldownPenaltyIncrease(5, TimeUnit.SECONDS)
                    .maxCooldown(20, TimeUnit.MINUTES)
                    .pool(MantaroData.getDefaultJedisPool())
                    .prefix("itemtransfer")
                    .build();

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                TextChannel channel = event.getChannel();

                if (args.length < 2) {
                    channel.sendMessageFormat(languageContext.get("commands.itemtransfer.no_item_mention"), EmoteReference.ERROR).queue();
                    return;
                }

                final List<User> mentionedUsers = event.getMessage().getMentionedUsers();
                if (mentionedUsers.size() == 0) {
                    channel.sendMessageFormat(languageContext.get("general.mention_user_required"), EmoteReference.ERROR).queue();
                } else {
                    User giveTo = mentionedUsers.get(0);

                    if (event.getAuthor().getId().equals(giveTo.getId())) {
                        channel.sendMessageFormat(languageContext.get("commands.itemtransfer.transfer_yourself_note"), EmoteReference.ERROR).queue();
                        return;
                    }

                    if (giveTo.isBot()) {
                        channel.sendMessageFormat(languageContext.get("commands.itemtransfer.bot_notice"), EmoteReference.ERROR).queue();
                        return;
                    }

                    if (!handleDefaultIncreasingRatelimit(rateLimiter, event.getAuthor(), event, languageContext))
                        return;

                    Item item = Items.fromAnyNoId(args[1]).orElse(null);
                    if (item == null) {
                        channel.sendMessage(languageContext.get("general.item_lookup.no_item_emoji")).queue();
                    } else {
                        if (item == Items.CLAIM_KEY) {
                            channel.sendMessage(languageContext.get("general.item_lookup.claim_key")).queue();
                            return;
                        }

                        Player player = MantaroData.db().getPlayer(event.getAuthor());
                        Player giveToPlayer = MantaroData.db().getPlayer(giveTo);

                        if (player.isLocked()) {
                            channel.sendMessageFormat(languageContext.get("commands.itemtransfer.locked_notice"), EmoteReference.ERROR).queue();
                            return;
                        }

                        if (args.length == 2) {
                            if (player.getInventory().containsItem(item)) {
                                if (item.isHidden()) {
                                    channel.sendMessageFormat(languageContext.get("commands.itemtransfer.hidden_item"), EmoteReference.ERROR).queue();
                                    return;
                                }

                                if (giveToPlayer.getInventory().asMap().getOrDefault(item, new ItemStack(item, 0)).getAmount() >= 5000) {
                                    channel.sendMessageFormat(languageContext.get("commands.itemtransfer.overflow"), EmoteReference.ERROR).queue();
                                    return;
                                }

                                player.getInventory().process(new ItemStack(item, -1));
                                giveToPlayer.getInventory().process(new ItemStack(item, 1));
                                new MessageBuilder().setContent(String.format(languageContext.get("commands.itemtransfer.success"),
                                        EmoteReference.OK, event.getMember().getEffectiveName(), 1, item.getName(), event.getGuild().getMember(giveTo).getEffectiveName()))
                                        .stripMentions(event.getGuild(), Message.MentionType.EVERYONE, Message.MentionType.HERE)
                                        .sendTo(channel)
                                        .queue();
                            } else {
                                channel.sendMessageFormat(languageContext.get("commands.itemtransfer.multiple_items_error"), EmoteReference.ERROR).queue();
                            }

                            player.saveAsync();
                            giveToPlayer.saveAsync();
                            return;
                        }

                        try {
                            int amount = Math.abs(Integer.parseInt(args[2]));
                            if (player.getInventory().containsItem(item) && player.getInventory().getAmount(item) >= amount) {
                                if (item.isHidden()) {
                                    channel.sendMessageFormat(languageContext.get("commands.itemtransfer.hidden_item"), EmoteReference.ERROR).queue();
                                    return;
                                }

                                if (giveToPlayer.getInventory().asMap().getOrDefault(item, new ItemStack(item, 0)).getAmount() + amount > 5000) {
                                    channel.sendMessageFormat(languageContext.get("commands.itemtransfer.overflow"), EmoteReference.ERROR).queue();
                                    return;
                                }

                                player.getInventory().process(new ItemStack(item, amount * -1));
                                giveToPlayer.getInventory().process(new ItemStack(item, amount));

                                new MessageBuilder().setContent(String.format(languageContext.get("commands.itemtransfer.success"), EmoteReference.OK,
                                        event.getMember().getEffectiveName(), amount, item.getName(), event.getGuild().getMember(giveTo).getEffectiveName()))
                                        .stripMentions(event.getGuild(), Message.MentionType.EVERYONE, Message.MentionType.HERE)
                                        .sendTo(channel)
                                        .queue();
                            } else {
                                channel.sendMessageFormat(languageContext.get("commands.itemtransfer.error"), EmoteReference.ERROR).queue();
                            }
                        } catch (NumberFormatException nfe) {
                            channel.sendMessageFormat(languageContext.get("general.invalid_number") + " " + languageContext.get("general.space_notice"), EmoteReference.ERROR).queue();
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
            RateLimiter partyRateLimiter = new RateLimiter(TimeUnit.MINUTES, 3);

            @Override
            public void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                final TextChannel channel = event.getChannel();

                if (event.getMessage().getMentionedUsers().isEmpty()) {
                    channel.sendMessageFormat(languageContext.get("general.mention_user_required"), EmoteReference.ERROR).queue();
                    return;
                }

                final User giveTo = event.getMessage().getMentionedUsers().get(0);

                if (giveTo.equals(event.getAuthor())) {
                    channel.sendMessageFormat(languageContext.get("commands.transfer.transfer_yourself_note"), EmoteReference.THINKING).queue();
                    return;
                }

                if (giveTo.isBot()) {
                    channel.sendMessageFormat(languageContext.get("commands.transfer.bot_notice"), EmoteReference.ERROR).queue();
                    return;
                }

                if (!handleDefaultIncreasingRatelimit(rateLimiter, event.getAuthor(), event, languageContext))
                    return;

                long toSend; // = 0 at the start

                try {
                    //Convert negative values to absolute.
                    toSend = Math.abs(new RoundedMetricPrefixFormat().parseObject(args[1], new ParsePosition(0)));
                } catch (Exception e) {
                    channel.sendMessageFormat(languageContext.get("commands.transfer.no_amount"), EmoteReference.ERROR).queue();
                    return;
                }

                if (toSend == 0) {
                    channel.sendMessageFormat(languageContext.get("commands.transfer.no_money_specified_notice"), EmoteReference.ERROR).queue();
                    return;
                }

                if (Items.fromAnyNoId(args[1]).isPresent()) {
                    channel.sendMessageFormat(languageContext.get("commands.transfer.item_transfer"), EmoteReference.ERROR).queue();
                    return;
                }

                if (toSend > TRANSFER_LIMIT) {
                    channel.sendMessageFormat(languageContext.get("commands.transfer.over_transfer_limit"),
                            EmoteReference.ERROR, TRANSFER_LIMIT).queue();
                    return;
                }

                Player transferPlayer = MantaroData.db().getPlayer(event.getMember());
                Player toTransfer = MantaroData.db().getPlayer(event.getGuild().getMember(giveTo));

                if (transferPlayer.isLocked()) {
                    channel.sendMessageFormat(languageContext.get("commands.transfer.own_locked_notice"), EmoteReference.ERROR).queue();
                    return;
                }

                if (transferPlayer.getMoney() < toSend) {
                    channel.sendMessageFormat(languageContext.get("commands.transfer.no_money_notice"), EmoteReference.ERROR).queue();
                    return;
                }

                if (toTransfer.isLocked()) {
                    channel.sendMessageFormat(languageContext.get("commands.transfer.receipt_locked_notice"), EmoteReference.ERROR).queue();
                    return;
                }

                if (toTransfer.getMoney() > (long) TRANSFER_LIMIT * 18) {
                    channel.sendMessageFormat(languageContext.get("commands.transfer.receipt_over_limit"), EmoteReference.ERROR).queue();
                    return;
                }

                String partyKey = event.getAuthor().getId() + ":" + giveTo.getId();
                if (!partyRateLimiter.process(partyKey)) {
                    channel.sendMessage(
                            EmoteReference.STOPWATCH +
                                    String.format(languageContext.get("commands.transfer.party"), giveTo.getName()) + " (Ratelimited)" +
                                    "\n **You'll be able to transfer to this user again in " + Utils.getHumanizedTime(partyRateLimiter.tryAgainIn(partyKey))
                                    + ".**"
                    ).queue();

                    Utils.ratelimitedUsers.computeIfAbsent(event.getAuthor().getIdLong(), __ -> new AtomicInteger()).incrementAndGet();
                    return;
                }

                long amountTransfer = Math.round(toSend * 0.92);

                if (toTransfer.addMoney(amountTransfer)) {
                    transferPlayer.removeMoney(toSend);
                    transferPlayer.saveAsync();

                    channel.sendMessageFormat(languageContext.get("commands.transfer.success"), EmoteReference.CORRECT, toSend, amountTransfer,
                            event.getMessage().getMentionedUsers().get(0).getName()
                    ).queue();

                    toTransfer.saveAsync();
                    rateLimiter.limit(toTransfer.getUserId());
                } else {
                    channel.sendMessageFormat(languageContext.get("commands.transfer.receipt_overflow_notice"), EmoteReference.ERROR).queue();
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
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                final TextChannel channel = event.getChannel();

                final ManagedDatabase managedDatabase = MantaroData.db();
                //Argument parsing.
                Map<String, String> t = getArguments(args);
                content = Utils.replaceArguments(t, content, "season", "s").trim();
                boolean isSeasonal = t.containsKey("season") || t.containsKey("s");

                Player p = managedDatabase.getPlayer(event.getAuthor());
                SeasonPlayer sp = managedDatabase.getPlayerForSeason(event.getAuthor(), getConfig().getCurrentSeason());
                Item item = Items.fromAnyNoId(content.replace("\"", "")).orElse(null);

                //Open default crate if nothing's specified.
                if (item == null || content.isEmpty())
                    item = Items.LOOT_CRATE;

                if (item.getItemType() != ItemType.CRATE) {
                    channel.sendMessageFormat(languageContext.get("commands.opencrate.not_crate"), EmoteReference.ERROR).queue();
                    return;
                }

                boolean containsItem = isSeasonal ? sp.getInventory().containsItem(item) : p.getInventory().containsItem(item);
                if (!containsItem) {
                    channel.sendMessageFormat(languageContext.get("commands.opencrate.no_crate"), EmoteReference.SAD, item.getName()).queue();
                    return;
                }

                //Ratelimit handled here
                //Check Items.openLootCrate for implementation details.
                item.getAction().test(event, Pair.of(languageContext, content), isSeasonal);
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

        cr.register("dailycrate", new SimpleCommand(Category.CURRENCY) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                final ManagedDatabase managedDatabase = MantaroData.db();

                if (!managedDatabase.getUser(event.getAuthor()).isPremium()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.dailycrate.not_premium"), EmoteReference.ERROR).queue();
                    return;
                }

                Player p = managedDatabase.getPlayer(event.getAuthor());
                Inventory inventory = p.getInventory();

                if (!handleDefaultIncreasingRatelimit(rateLimiter, event.getAuthor(), event, languageContext))
                    return;

                Random random = new Random();

                Item randomCrate = random.nextBoolean() ? Items.MINE_PREMIUM_CRATE : Items.FISH_PREMIUM_CRATE;

                inventory.process(new ItemStack(randomCrate, 1));
                p.save();

                event.getChannel().sendMessageFormat(languageContext.get("commands.dailycrate.success"), EmoteReference.POPPER, randomCrate.getName()).queue();
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
            public Command defaultTrigger(GuildMessageReceivedEvent event, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                        final ManagedDatabase db = MantaroData.db();
                        String[] args = StringUtils.advancedSplitArgs(content, 2);
                        Map<String, String> t = StringUtils.parse(content.split("\\s+"));
                        final TextChannel channel = event.getChannel();

                        if (content.isEmpty()) {
                            channel.sendMessageFormat(languageContext.get("commands.useitem.no_items_specified"), EmoteReference.ERROR).queue();
                            return;
                        }

                        Item item = Items.fromAnyNoId(args[0]).orElse(null);
                        //Well, shit.
                        if (item == null) {
                            channel.sendMessageFormat(languageContext.get("general.item_lookup.not_found"), EmoteReference.ERROR).queue();
                            return;
                        }

                        if (item.getItemType() != ItemType.INTERACTIVE && item.getItemType() != ItemType.CRATE && item.getItemType() != ItemType.POTION && item.getItemType() != ItemType.BUFF) {
                            channel.sendMessageFormat(languageContext.get("commands.useitem.not_interactive"), EmoteReference.ERROR).queue();
                            return;
                        }

                        if (item.getAction() == null && (item.getItemType() != ItemType.POTION && item.getItemType() != ItemType.BUFF)) {
                            channel.sendMessageFormat(languageContext.get("commands.useitem.interactive_no_action"), EmoteReference.ERROR).queue();
                            return;
                        }

                        Player p = db.getPlayer(event.getAuthor());
                        if (!p.getInventory().containsItem(item)) {
                            channel.sendMessageFormat(languageContext.get("commands.useitem.no_item"), EmoteReference.SAD).queue();
                            return;
                        }

                        applyPotionEffect(event, item, p, t, content, false, languageContext);
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
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                List<Item> interactiveItems = Arrays.stream(Items.ALL).filter(
                        i -> i.getItemType() == ItemType.INTERACTIVE || i.getItemType() == ItemType.POTION || i.getItemType() == ItemType.CRATE || i.getItemType() == ItemType.BUFF
                ).collect(Collectors.toList());

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

                event.getChannel().sendMessage(new EmbedBuilder()
                        .setAuthor(languageContext.get("commands.useitem.ls.header"), null, event.getAuthor().getEffectiveAvatarUrl())
                        .setDescription(show.toString())
                        .setColor(Color.PINK)
                        .setFooter(String.format(languageContext.get("general.requested_by"), event.getMember().getEffectiveName()), null)
                        .build()
                ).queue();
            }
        });

        ui.createSubCommandAlias("ls", "list");
        ui.createSubCommandAlias("ls", "Is");
    }

    @Subscribe
    public void fish(CommandRegistry cr) {
        cr.register("fish", new SimpleCommand(Category.CURRENCY) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                Map<String, String> t = getArguments(content);
                boolean isSeasonal = t.containsKey("season") || t.containsKey("s");
                content = Utils.replaceArguments(t, content, "season", "s").trim().replace("\"", "");

                Items.FISHING_ROD.getAction().test(event, Pair.of(languageContext, content), isSeasonal);
            }


            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Starts a fishing session.")
                        .setUsage("`~>fish <rod>` - Starts fishing. You can gain credits and fish items by fishing, which can be used later on for casting.")
                        .addParameter("rod", "Rod name. Optional, if not provided or not found, will default to the default fishing rod or your equipped rod.")
                        .setSeasonal(true)
                        .build();
            }
        });
    }
}
