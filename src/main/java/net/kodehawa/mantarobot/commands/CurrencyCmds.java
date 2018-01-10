/*
 * Copyright (C) 2016-2018 David Alejandro Rubio Escares / Kodehawa
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
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands;

import br.com.brjdevs.java.utils.texts.StringUtils;
import com.google.common.eventbus.Subscribe;
import com.rethinkdb.model.OptArgs;
import com.rethinkdb.net.Connection;
import com.rethinkdb.net.Cursor;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.RateLimiter;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.rethinkdb.RethinkDB.r;
import static net.kodehawa.mantarobot.utils.Utils.handleDefaultRatelimit;

@Module
public class CurrencyCmds {
    private final int TRANSFER_LIMIT = Integer.MAX_VALUE / 3; //around 715m
    private final Random random = new Random();

    @Subscribe
    public void inventory(CommandRegistry cr) {
        cr.register("inventory", new SimpleCommand(Category.CURRENCY) {
            @Override
            public void call(GuildMessageReceivedEvent event, String content, String[] args) {
                Map<String, Optional<String>> t = StringUtils.parse(args);
                content = Utils.replaceArguments(t, content, "brief", "calculate");
                Member member = Utils.findMember(event, event.getMember(), content);

                if(member == null)
                    return;

                Player player = MantaroData.db().getPlayer(member);

                if(t.containsKey("brief")) {
                    event.getChannel().sendMessage(String.format("**%s's inventory:** %s", member.getEffectiveName(), ItemStack.toString(player.getInventory().asList()))).queue();
                    return;
                }

                if(t.containsKey("calculate")) {
                    long all = player.getInventory().asList().stream()
                            .filter(item -> item.getItem().isSellable())
                            .mapToLong(value -> (long) (value.getItem().getValue() * value.getAmount() * 0.9d))
                            .sum();

                    event.getChannel().sendMessage(String.format("%sYou will get **%d credits** if you sell all of your items!", EmoteReference.DIAMOND, all)).queue();
                    return;
                }

                EmbedBuilder builder = baseEmbed(event, member.getEffectiveName() + "'s Inventory", member.getUser().getEffectiveAvatarUrl());
                List<ItemStack> list = player.getInventory().asList();
                List<MessageEmbed.Field> fields = new LinkedList<>();
                if(list.isEmpty())
                    builder.setDescription("There is only dust here.");
                else
                    player.getInventory().asList().forEach(stack -> {
                        long buyValue = stack.getItem().isBuyable() ? stack.getItem().getValue() : 0;
                        long sellValue = stack.getItem().isSellable() ? (long) (stack.getItem().getValue() * 0.9) : 0;
                        fields.add(new MessageEmbed.Field(stack.getItem().getEmoji() + " " + stack.getItem().getName() + " x " + stack.getAmount(), String
                                        .format("**Price**: \uD83D\uDCE5 %d \uD83D\uDCE4 %d\n%s", buyValue, sellValue, stack.getItem()
                                                .getDesc())
                                , false));
                    });

                List<List<MessageEmbed.Field>> splitFields = DiscordUtils.divideFields(18, fields);
                boolean hasReactionPerms = event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_ADD_REACTION);

                if(hasReactionPerms) {
                    DiscordUtils.list(event, 45, false, builder, splitFields);
                } else {
                    DiscordUtils.listText(event, 45, false, builder, splitFields);
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Inventory command")
                        .setDescription("**Shows your current inventory.**\n" +
                                "You can use `~>inventory -brief` to get a mobile friendly version.\n" +
                                "Use `~>inventory -calculate` to see how much you'd get if you sell every sellable item on your inventory!").build();
            }
        });

        cr.registerAlias("inventory", "inv");
    }

    @Subscribe
    public void market(CommandRegistry cr) {
        final RateLimiter rateLimiter = new RateLimiter(TimeUnit.SECONDS, 8);

        TreeCommand marketCommand = (TreeCommand) cr.register("market", new TreeCommand(Category.CURRENCY) {
            @Override
            public Command defaultTrigger(GuildMessageReceivedEvent event, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(GuildMessageReceivedEvent event, String content) {
                        EmbedBuilder embed = baseEmbed(event, "Mantaro's Market")
                                .setThumbnail("https://png.icons8.com/metro/540/shopping-cart.png");
                        List<MessageEmbed.Field> fields = new LinkedList<>();
                        Stream.of(Items.ALL).forEach(item -> {
                            if(!item.isHidden()) {
                                String buyValue = item.isBuyable() ? String.format("$%d", item.getValue()) : "N/A";
                                String sellValue = item.isSellable() ? String.format("$%d", (int) Math.floor(item.getValue() * 0.9)) : "N/A";

                                fields.add(new MessageEmbed.Field(String.format("%s %s", item.getEmoji(), item.getName()),
                                        EmoteReference.BUY + buyValue + " " + EmoteReference.SELL + sellValue, true)
                                );
                            }
                        });

                        List<List<MessageEmbed.Field>> splitFields = DiscordUtils.divideFields(8, fields);
                        boolean hasReactionPerms = event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_ADD_REACTION);

                        if(hasReactionPerms) {
                            DiscordUtils.list(event, 45, false, embed, splitFields);
                        } else {
                            DiscordUtils.listText(event, 45, false, embed, splitFields);
                        }
                    }
                };
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Mantaro's market")
                        .setDescription("**List current items for buying and selling.**")
                        .addField("Buying and selling", "To buy do ~>market buy <item emoji>. It will subtract the value from your money" +
                                " and give you the item.\n" +
                                "To sell do `~>market sell all` to sell all your items or `~>market sell <item emoji>` to sell the specified item. " +
                                "**You'll get the sell value of the item on coins to spend.**\n" +
                                "You can check the value of a single item using `~>market price <item emoji>`\n" +
                                "You can send an item to the trash using `~>market dump <amount> <item emoji>`\n" +
                                "Use `~>inventory -calculate` to check how much is your inventory worth.", false)
                        .addField("To know", "If you don't have enough money you cannot buy the items.\n" +
                                "Note: Don't use the item id, it's just for aesthetic reasons, the internal IDs are different than the ones shown here!", false)
                        .addField("Information", "To buy and sell multiple items you need to do `~>market <buy/sell> <amount> <item>`",
                                false)
                        .build();
            }
        });

        marketCommand.setPredicate((event) -> {
            if(!handleDefaultRatelimit(rateLimiter, event.getAuthor(), event))
                return false;

            Player player = MantaroData.db().getPlayer(event.getMember());
            if(player.isLocked()) {
                event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot access the market now.").queue();
                return false;
            }

            return true;
        });

        marketCommand.addSubCommand("dump", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content) {
                String[] args = content.split(" ");
                String itemName = content;
                int itemNumber = 1;
                boolean isMassive = !itemName.isEmpty() && itemName.split(" ")[0].matches("^[0-9]*$");
                if(isMassive) {
                    try {
                        itemNumber = Math.abs(Integer.valueOf(itemName.split(" ")[0]));
                        itemName = itemName.replace(args[0], "").trim();
                    } catch (NumberFormatException e) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Not a valid number of items to buy.").queue();
                        return;
                    } catch (Exception e) {
                        onHelp(event);
                        return;
                    }
                }

                Item item = Items.fromAny(itemName).orElse(null);

                if(item == null) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "Cannot check the dump a non-existent item!").queue();
                    return;
                }

                Player player = MantaroData.db().getPlayer(event.getAuthor());

                if(!player.getInventory().containsItem(item)) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "Cannot dump an item you don't have!").queue();
                    return;
                }

                if(player.getInventory().getAmount(item) < itemNumber) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot dump more items than what you have.").queue();
                    return;
                }

                player.getInventory().process(new ItemStack(item, -itemNumber));
                player.saveAsync();
                event.getChannel().sendMessage(String.format("%sSent %dx **%s %s** to the trash!", EmoteReference.CORRECT, itemNumber, item.getEmoji(), item.getName())).queue();
            }
        }).createSubCommandAlias("dump", "trash");

        marketCommand.addSubCommand("price", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content) {
                String[] args = content.split(" ");
                String itemName = content.replace(args[0] + " ", "");
                Item item = Items.fromAny(itemName).orElse(null);

                if(item == null) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "Cannot check the price of a non-existent item!").queue();
                    return;
                }

                if(!item.isBuyable() && !item.isSellable()) {
                    event.getChannel().sendMessage(EmoteReference.THINKING + "This item is not available neither for sell or buy (could be an exclusive collectible)").queue();
                    return;
                }

                if(!item.isBuyable()) {
                    event.getChannel().sendMessage(EmoteReference.EYES + "This is a collectible item. (Sell value: " + ((int) (item.getValue() * 0.9)) + " credits)").queue();
                    return;
                }

                event.getChannel().sendMessage(String.format("%sThe market value of %s**%s** is %s credits to buy it and you can get %s credits if you sell it.",
                        EmoteReference.MARKET, item.getEmoji(), item.getName(), item.getValue(), (int) (item.getValue() * 0.9))).queue();
            }
        });

        marketCommand.addSubCommand("sell", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content) {
                Player player = MantaroData.db().getPlayer(event.getMember());
                String[] args = content.split(" ");
                String itemName = content;
                int itemNumber = 1;
                boolean isMassive = !itemName.isEmpty() && itemName.split(" ")[0].matches("^[0-9]*$");
                if(isMassive) {
                    try {
                        itemNumber = Math.abs(Integer.valueOf(itemName.split(" ")[0]));
                        itemName = itemName.replace(args[0], "").trim();
                    } catch (NumberFormatException e) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Not a valid number of items to buy.").queue();
                        return;
                    } catch (Exception e) {
                        onHelp(event);
                        return;
                    }
                }

                try {
                    if(args[0].equals("all")) {
                        long all = player.getInventory().asList().stream()
                                .filter(item -> item.getItem().isSellable())
                                .mapToLong(value -> (long) (value.getItem().getValue() * value.getAmount() * 0.9d))
                                .sum();

                        player.getInventory().clearOnlySellables();
                        player.addMoney(all);

                        event.getChannel().sendMessage(String.format("%sYou sold all your inventory items and gained %d credits!", EmoteReference.MONEY, all)).queue();

                        player.saveAsync();
                        return;
                    }

                    Item toSell = Items.fromAny(itemName).orElse(null);

                    if(toSell == null) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot sell a non-existant item.").queue();
                        return;
                    }

                    if(!toSell.isSellable()) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot sell an item that cannot be sold.").queue();
                        return;
                    }

                    if(player.getInventory().getAmount(toSell) < 1) {
                        event.getChannel().sendMessage(EmoteReference.STOP + "You cannot sell an item you don't have.").queue();
                        return;
                    }

                    if(player.getInventory().getAmount(toSell) < itemNumber) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot sell more items than what you have.").queue();
                        return;
                    }

                    int many = itemNumber * -1;
                    long amount = Math.round((toSell.getValue() * 0.9)) * Math.abs(many);
                    player.getInventory().process(new ItemStack(toSell, many));
                    player.addMoney(amount);
                    player.getData().setMarketUsed(player.getData().getMarketUsed() + 1);
                    event.getChannel().sendMessage(String.format("%sYou sold %d **%s** and gained %d credits!", EmoteReference.CORRECT, Math.abs(many), toSell.getName(), amount)).queue();

                    player.saveAsync();
                } catch(Exception e) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "Invalid syntax.").queue();
                }
            }
        });

        marketCommand.addSubCommand("buy", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content) {
                Player player = MantaroData.db().getPlayer(event.getMember());
                String[] args = content.split(" ");
                String itemName = content;
                int itemNumber = 1;
                boolean isMassive = !itemName.isEmpty() && itemName.split(" ")[0].matches("^[0-9]*$");
                if(isMassive) {
                    try {
                        itemNumber = Math.abs(Integer.valueOf(itemName.split(" ")[0]));
                        itemName = itemName.replace(args[0], "").trim();
                    } catch (Exception e) {
                        if (e instanceof NumberFormatException) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "Not a valid number of items to buy.").queue();
                        } else {
                            onHelp(event);
                            return;
                        }
                    }
                }

                Item itemToBuy = Items.fromAnyNoId(itemName).orElse(null);

                if(itemToBuy == null) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot buy an unexistant item.").queue();
                    return;
                }

                try {
                    if(!itemToBuy.isBuyable()) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot buy an item that cannot be bought.")
                                .queue();
                        return;
                    }

                    ItemStack stack = player.getInventory().getStackOf(itemToBuy);
                    if(stack != null && !stack.canJoin(new ItemStack(itemToBuy, itemNumber))) {
                        //assume overflow
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot buy more of that object!").queue();
                        return;
                    }

                    if(player.removeMoney(itemToBuy.getValue() * itemNumber)) {
                        player.getInventory().process(new ItemStack(itemToBuy, itemNumber));
                        player.getData().addBadgeIfAbsent(Badge.BUYER);
                        player.getData().setMarketUsed(player.getData().getMarketUsed() + 1);
                        player.saveAsync();

                        event.getChannel().sendMessage(String.format("%sBought %d %s for %d credits successfully. You now have %d credits.",
                                EmoteReference.OK, itemNumber, itemToBuy.getEmoji(), itemToBuy.getValue() * itemNumber, player.getMoney())).queue();

                    } else {
                        event.getChannel().sendMessage(EmoteReference.STOP + "You don't have enough money to buy this item.").queue();
                    }
                } catch(Exception e) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "Invalid syntax.").queue();
                }
            }
        });
    }

    @Subscribe
    public void transferItems(CommandRegistry cr) {
        cr.register("itemtransfer", new SimpleCommand(Category.CURRENCY) {
            RateLimiter rl = new RateLimiter(TimeUnit.SECONDS, 10);

            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                if(args.length < 2) {
                    onError(event);
                    return;
                }

                List<User> mentionedUsers = event.getMessage().getMentionedUsers();
                if(mentionedUsers.size() == 0) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You need to mention a user").queue();
                }
                else {
                    User giveTo = mentionedUsers.get(0);

                    if(event.getAuthor().getId().equals(giveTo.getId())) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot transfer items to yourself!").queue();
                        return;
                    }

                    if(giveTo.isBot()) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot transfer items to a bot.").queue();
                        return;
                    }

                    if(!handleDefaultRatelimit(rl, event.getAuthor(), event))
                        return;

                    Item item = Items.fromAny(args[1]).orElse(null);
                    if(item == null) {
                        event.getChannel().sendMessage("There isn't an item associated with this emoji.").queue();
                    } else {
                        Player player = MantaroData.db().getPlayer(event.getAuthor());
                        Player giveToPlayer = MantaroData.db().getPlayer(giveTo);

                        if(player.isLocked()) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot transfer items now.").queue();
                            return;
                        }

                        if(args.length == 2) {
                            if(player.getInventory().containsItem(item)) {
                                if(item.isHidden()) {
                                    event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot transfer this item!").queue();
                                    return;
                                }

                                if(giveToPlayer.getInventory().asMap().getOrDefault(item, new ItemStack(item, 0)).getAmount() >= 5000) {
                                    event.getChannel().sendMessage(EmoteReference.ERROR + "This player has the maximum possible amount of this item (5000).").queue();
                                    return;
                                }

                                player.getInventory().process(new ItemStack(item, -1));
                                giveToPlayer.getInventory().process(new ItemStack(item, 1));
                                event.getChannel().sendMessage(String.format("%s%s gave 1 **%s** to %s", EmoteReference.OK, event.getMember().getEffectiveName(),
                                        item.getName(), event.getGuild().getMember(giveTo).getEffectiveName())).queue();
                            } else {
                                event.getChannel().sendMessage(EmoteReference.ERROR + "You don't have any of these items in your inventory").queue();
                            }

                            player.saveAsync();
                            giveToPlayer.saveAsync();
                            return;
                        }

                        try {
                            int amount = Math.abs(Integer.parseInt(args[2]));
                            if(player.getInventory().containsItem(item) && player.getInventory().getAmount(item) >= amount) {
                                if(item.isHidden()) {
                                    event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot transfer this item!").queue();
                                    return;
                                }

                                if(giveToPlayer.getInventory().asMap().getOrDefault(item, new ItemStack(item, 0)).getAmount() + amount > 5000) {
                                    event.getChannel().sendMessage(EmoteReference.ERROR + "This player would exceed the maximum possible amount of this item (5000).").queue();
                                    return;
                                }

                                player.getInventory().process(new ItemStack(item, amount * -1));
                                giveToPlayer.getInventory().process(new ItemStack(item, amount));

                                event.getChannel().sendMessage(String.format("%s%s gave %d **%s** to %s", EmoteReference.OK,
                                        event.getMember().getEffectiveName(), amount, item.getName(), event.getGuild().getMember(giveTo).getEffectiveName())).queue();
                            } else {
                                event.getChannel().sendMessage(EmoteReference.ERROR + "You don't have enough of this item to do that").queue();
                            }
                        } catch(NumberFormatException nfe) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "Invalid number provided").queue();
                        }

                        player.saveAsync();
                        giveToPlayer.saveAsync();
                    }
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Transfer Items command")
                        .setDescription("**Transfers items from you to another player.**")
                        .addField("Usage", "`~>itemtransfer <@user> <item emoji or part of the name> <amount (optional)>` - **Transfers the item to player x**", false)
                        .addField("Parameters", "`@user` - user to send the item to\n" +
                                "`item emoji` - write out the emoji of the item you want to send, or you can just use part of its name.\n" +
                                "`amount` - optional, send a specific amount of an item to someone.", false)
                        .addField("Important", "You cannot send more items than what you already have", false)
                        .build();
            }
        });

        cr.registerAlias("itemtransfer", "transferitems");
    }

    @Subscribe
    //Should be called return land tbh, what the fuck.
    public void transfer(CommandRegistry cr) {
        cr.register("transfer", new SimpleCommand(Category.CURRENCY) {
            RateLimiter rl = new RateLimiter(TimeUnit.SECONDS, 10);

            @Override
            public void call(GuildMessageReceivedEvent event, String content, String[] args) {
                if(event.getMessage().getMentionedUsers().isEmpty()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You need to mention one user.").queue();
                    return;
                }

                User giveTo = event.getMessage().getMentionedUsers().get(0);

                if(giveTo.equals(event.getAuthor())) {
                    event.getChannel().sendMessage(EmoteReference.THINKING + "You cannot transfer money to yourself.").queue();
                    return;
                }

                if(giveTo.isBot()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot transfer money to a bot.").queue();
                    return;
                }

                if(!handleDefaultRatelimit(rl, event.getAuthor(), event)) return;

                long toSend; // = 0 at the start

                try {
                    //Convert negative values to absolute.
                    toSend = Math.abs(Long.parseLong(args[1]));
                } catch(Exception e) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You need to specify the amount.").queue();
                    return;
                }

                if(toSend == 0) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot transfer no money :P").queue();
                    return;
                }

                if(toSend > TRANSFER_LIMIT) {
                    event.getChannel().sendMessage(String.format("%sYou cannot transfer this much money. (Limit: %d credits)", EmoteReference.ERROR, TRANSFER_LIMIT)).queue();
                    return;
                }

                Player transferPlayer = MantaroData.db().getPlayer(event.getMember());
                Player toTransfer = MantaroData.db().getPlayer(event.getGuild().getMember(giveTo));

                if(transferPlayer.isLocked()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot transfer money now.").queue();
                    return;
                }

                if(transferPlayer.getMoney() < toSend) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot transfer money you don't have.").queue();
                    return;
                }

                if(toTransfer.isLocked()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "That user cannot receive money now.").queue();
                    return;
                }

                if(toTransfer.getMoney() > (long) TRANSFER_LIMIT * 20) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "This user already has too much money...").queue();
                    return;
                }

                if(toTransfer.addMoney(toSend)) {
                    transferPlayer.removeMoney(toSend);
                    transferPlayer.saveAsync();

                    event.getChannel().sendMessage(String.format("%sTransferred **%d** to *%s* successfully.", EmoteReference.CORRECT, toSend,
                            event.getMessage().getMentionedUsers().get(0).getName())).queue();

                    toTransfer.saveAsync();
                    rl.process(toTransfer.getUserId());
                } else {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "Cannot send money to this player.").queue();
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Transfer command")
                        .setDescription("**Transfers money from you to another player.**")
                        .addField("Usage", "`~>transfer <@user> <money>` - **Transfers money to x player**", false)
                        .addField("Parameters", "`@user` - user to send money to\n" +
                                "`money` - money to transfer.", false)
                        .addField("Important", "You cannot send more money than what you already have\n" +
                                "The maximum amount you can transfer at once is " + TRANSFER_LIMIT + " credits.", false)
                        .build();
            }
        });
    }


    @Subscribe
    public void lootcrate(CommandRegistry registry) {
        registry.register("opencrate", new SimpleCommand(Category.CURRENCY) {

            final RateLimiter rateLimiter = new RateLimiter(TimeUnit.HOURS, 1);

            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                Player player = MantaroData.db().getPlayer(event.getAuthor());
                Inventory inventory = player.getInventory();
                if(inventory.containsItem(Items.LOOT_CRATE)) {
                    if(inventory.containsItem(Items.LOOT_CRATE_KEY)) {
                        if(!handleDefaultRatelimit(rateLimiter, event.getAuthor(), event)) return;

                        inventory.process(new ItemStack(Items.LOOT_CRATE_KEY, -1));
                        inventory.process(new ItemStack(Items.LOOT_CRATE, -1));
                        player.getData().addBadgeIfAbsent(Badge.THE_SECRET);
                        player.save();
                        openLootBox(event, true);
                    } else {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You need a loot crate key to open a crate. It's locked!").queue();
                    }
                } else {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You need a loot crate! How else would you use your key >.>").queue();
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Open loot crates")
                        .setDescription("**Yep. It's really that simple**.\n" +
                                "You need a crate key to open a loot crate. Loot crates are acquired rarely from the loot command.")
                        .build();
            }
        });
    }

    //TODO find an efficient way to do this that doesn't require a shit ton of db reads.
    //@Subscribe
    @SuppressWarnings("unchecked")
    public void rank(CommandRegistry registry) {
        registry.register("rank", new SimpleCommand(Category.CURRENCY) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {

                Member m = Utils.findMember(event, event.getMember(), content);
                if(m == null)
                    return;
                User user = m.getUser();

                long moneyRank, levelRank, reputationRank, streakRank;
                long count;
                try(Connection conn = Utils.newDbConnection()) {
                    moneyRank = ((Cursor<Long>) r.table("players").orderBy()
                            .optArg("index", r.desc("money"))
                            .offsetsOf(player -> player.g("id").eq(user.getId() + ":g"))
                            .run(conn, OptArgs.of("read_mode", "outdated"))).next() + 1;

                    levelRank = ((Cursor<Long>) r.table("players").orderBy()
                            .optArg("index", r.desc("level"))
                            .offsetsOf(player -> player.g("id").eq(user.getId() + ":g"))
                            .run(conn, OptArgs.of("read_mode", "outdated"))).next() + 1;

                    reputationRank = ((Cursor<Long>) r.table("players").orderBy()
                            .optArg("index", r.desc("reputation"))
                            .offsetsOf(player -> player.g("id").eq(user.getId() + ":g"))
                            .run(conn, OptArgs.of("read_mode", "outdated"))).next() + 1;

                    streakRank = ((Cursor<Long>) r.table("players").orderBy()
                            .optArg("index", r.desc("userDailyStreak"))
                            .offsetsOf(player -> player.g("id").eq(user.getId() + ":g"))
                            .run(conn, OptArgs.of("read_mode", "outdated"))).next() + 1;

                    count = r.table("players").count().run(conn);
                }

                event.getChannel().sendMessage(new EmbedBuilder()
                        .setTitle(user.getName() + "'s Leaderboard Rank")
                        .setDescription(
                                EmoteReference.BLUE_SMALL_MARKER + "**Money rank:** " + moneyRank + "/" + count + "\n" +
                                EmoteReference.BLUE_SMALL_MARKER + "**Level rank:** " + levelRank + "/" + count + "\n" +
                                EmoteReference.BLUE_SMALL_MARKER + "**Streak rank:** " + streakRank + "/" + count + "\n" +
                                EmoteReference.BLUE_SMALL_MARKER + "**Reputation rank:** " + reputationRank + "/" + count
                        )
                        .setColor(Color.PINK)
                        .build()).queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Rank command")
                        .setDescription("**Shows your ranks someone else's ranks (Position in the leaderboard)")
                        .build();
            }
        });
    }

    private void openLootBox(GuildMessageReceivedEvent event, boolean special) {
        List<Item> toAdd = new ArrayList<>();
        int amtItems = random.nextInt(3) + 3;
        List<Item> items = new ArrayList<>();
        items.addAll(Arrays.asList(Items.ALL));
        items.removeIf(item -> item.isHidden() || !item.isBuyable() || !item.isSellable());
        items.sort((o1, o2) -> {
            if(o1.getValue() > o2.getValue())
                return 1;
            if(o1.getValue() == o2.getValue())
                return 0;

            return -1;
        });

        if(!special) {
            for(Item i : Items.ALL) if(i.isHidden() || !i.isBuyable() || i.isSellable()) items.add(i);
        }
        for(int i = 0; i < amtItems; i++)
            toAdd.add(selectReverseWeighted(items));

        Player player = MantaroData.db().getPlayer(event.getMember());
        ArrayList<ItemStack> ita = new ArrayList<>();

        toAdd.forEach(item -> ita.add(new ItemStack(item, 1)));

        boolean overflow = player.getInventory().merge(ita);
        player.saveAsync();

        event.getChannel().sendMessage(String.format("%s**You won:** %s%s",
                EmoteReference.LOOT_CRATE.getDiscordNotation(), toAdd.stream().map(Item::toString).collect(Collectors.joining(", ")),
                overflow ? ". But you already had too much, so you decided to throw away the excess" : "")).queue();
    }

    private Item selectReverseWeighted(List<Item> items) {
        Map<Integer, Item> weights = new HashMap<>();
        int weightedTotal = 0;

        for(int i = 0; i < items.size(); i++) {
            int t = items.size() - i;
            weightedTotal += t;
            weights.put(t, items.get(i));
        }

        final int[] selected = {
                random.nextInt(weightedTotal)
        };

        for(Map.Entry<Integer, Item> i : weights.entrySet()) {
            if((selected[0] -= i.getKey()) <= 0) {
                return i.getValue();
            }
        }
        return null;
    }
}
