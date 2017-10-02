/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
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
import com.jagrosh.jdautilities.utils.FinderUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.RateLimiter;
import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.kodehawa.mantarobot.utils.Utils.handleDefaultRatelimit;

@Module
public class CurrencyCmds {
    private final Random random = new Random();
    private final int TRANSFER_LIMIT = Integer.MAX_VALUE / 3; //around 715m

    @Subscribe
    public void inventory(CommandRegistry cr) {
        cr.register("inventory", new SimpleCommand(Category.CURRENCY) {
            @Override
            public void call(GuildMessageReceivedEvent event, String content, String[] args) {
                Member member = event.getMember();
                Map<String, Optional<String>> t = StringUtils.parse(args);

                if(t.containsKey("brief")) {
                    content = content.replace(" -brief", "").replace("-brief", "");
                }

                List<Member> found = FinderUtil.findMembers(content, event.getGuild());
                if(found.isEmpty() && !content.isEmpty()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "Your search yielded no results :(").queue();
                    return;
                }

                if(found.size() > 1 && !content.isEmpty()) {
                    event.getChannel().sendMessage(EmoteReference.THINKING + "Too many users found, maybe refine your search? (ex. use name#discriminator)\n" +
                            "**Users found:** " + found.stream().map(m -> m.getUser().getName() + "#" + m.getUser().getDiscriminator()).collect(Collectors.joining(", "))).queue();
                    return;
                }

                if(found.size() == 1) {
                    member = found.get(0);
                }

                Player player = MantaroData.db().getPlayer(member);

                if(t.containsKey("brief")){
                    event.getChannel().sendMessage("**" + member.getEffectiveName() + "'s inventory:** " + ItemStack.toString(player.getInventory().asList())).queue();
                    return;
                }

                EmbedBuilder builder = baseEmbed(event, member.getEffectiveName() + "'s Inventory", member.getUser().getEffectiveAvatarUrl());
                List<ItemStack> list = player.getInventory().asList();
                if(list.isEmpty()) builder.setDescription("There is only dust.");
                else
                    player.getInventory().asList().forEach(stack -> {
                        long buyValue = stack.getItem().isBuyable() ? (long) (stack.getItem().getValue() * 1.1) : 0;
                        long sellValue = stack.getItem().isSellable() ? (long) (stack.getItem().getValue() * 0.9) : 0;
                        builder.addField(stack.getItem().getEmoji() + " " + stack.getItem().getName() + " x " + stack.getAmount(), String
                                        .format("**Price**: \uD83D\uDCE5 %d \uD83D\uDCE4 %d\n%s", buyValue, sellValue, stack.getItem()
                                                .getDesc())
                                , false);
                    });

                event.getChannel().sendMessage(builder.build()).queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Inventory command")
                        .setDescription("**Shows your current inventory.**\n").build();
            }
        });
    }

    @Subscribe
    public void market(CommandRegistry cr) {
        cr.register("market", new SimpleCommand(Category.CURRENCY) {
            final RateLimiter rateLimiter = new RateLimiter(TimeUnit.SECONDS, 5);

            @Override
            public void call(GuildMessageReceivedEvent event, String content, String[] args) {
                if(!handleDefaultRatelimit(rateLimiter, event.getAuthor(), event)) return;

                Player player = MantaroData.db().getPlayer(event.getMember());

                if(player.isLocked()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot access the market now.").queue();
                    return;
                }

                if(args.length > 0) {
                    int itemNumber = 1;
                    String itemName = content.replace(args[0] + " ", "");
                    boolean isMassive = !itemName.isEmpty() && itemName.split(" ")[0].matches("^[0-9]*$");
                    if(isMassive) {
                        try {
                            itemNumber = Math.abs(Integer.valueOf(itemName.split(" ")[0]));
                            itemName = itemName.replace(args[1] + " ", "");
                        } catch(Exception e) {
                            if(e instanceof NumberFormatException) {
                                event.getChannel().sendMessage(EmoteReference.ERROR + "Not a valid number of items to buy.").queue();
                            } else {
                                onHelp(event);
                                return;
                            }
                        }
                    }

                    if(itemNumber > 5000) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You can't buy more than 5000 items").queue();
                        return;
                    }

                    if(args[0].equals("sell")) {
                        try {
                            if(args[1].equals("all")) {
                                long all = player.getInventory().asList().stream()
                                        .filter(item -> item.getItem().isSellable())
                                        .mapToLong(value -> (long) (value.getItem().getValue() * value.getAmount() * 0.9d))
                                        .sum();

                                if(args.length > 2 && args[2].equals("calculate")) {
                                    event.getChannel().sendMessage(EmoteReference.THINKING + "You'll get **" + all + "** credits if you " +
                                            "sell all of your items").queue();
                                    return;
                                }

                                player.getInventory().clearOnlySellables();

                                if(player.addMoney(all)) {
                                    event.getChannel().sendMessage(EmoteReference.MONEY + "You sold all your inventory items and gained "
                                            + all + " credits!").queue();
                                } else {
                                    event.getChannel().sendMessage(EmoteReference.MONEY + "You sold all your inventory items and gained "
                                            + all + " credits. But you already had too many credits. Your bag overflowed" +
                                            ".\nCongratulations, you exploded a Java long (how??). Here's a buggy money bag for you.")
                                            .queue();
                                }

                                player.save();
                                return;
                            }

                            Item toSell = Items.fromAny(itemName).orElse(null);

                            if(toSell == null) {
                                event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot sell a non-existant item.").queue();
                                return;
                            }

                            if(!toSell.isSellable()) {
                                event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot sell an item that cannot be sold.")
                                        .queue();
                                return;
                            }

                            if(player.getInventory().asMap().getOrDefault(toSell, null) == null) {
                                event.getChannel().sendMessage(EmoteReference.STOP + "You cannot sell an item you don't have.").queue();
                                return;
                            }

                            if(player.getInventory().getAmount(toSell) < itemNumber) {
                                event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot sell more items than what you have.")
                                        .queue();
                                return;
                            }

                            int many = itemNumber * -1;
                            long amount = Math.round((toSell.getValue() * 0.9)) * Math.abs(many);
                            player.getInventory().process(new ItemStack(toSell, many));

                            if(player.addMoney(amount)) {
                                event.getChannel().sendMessage(EmoteReference.CORRECT + "You sold " + Math.abs(many) + " **" + toSell
                                        .getName() +
                                        "** and gained " + amount + " credits!").queue();
                            } else {
                                event.getChannel().sendMessage(EmoteReference.CORRECT + "You sold **" + toSell.getName() +
                                        "** and gained" + amount + " credits. But you already had too many credits. Your bag overflowed" +
                                        ".\nCongratulations, you exploded a Java long (how??). Here's a buggy money bag for you.").queue();
                            }

                            player.save();
                            return;
                        } catch(Exception e) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "Item doesn't exist or invalid syntax").queue();
                            e.printStackTrace();
                        }
                        return;
                    }

                    if(args[0].equals("buy")) {
                        Item itemToBuy = Items.fromAny(itemName).orElse(null);

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
                                player.save();
                                event.getChannel().sendMessage(EmoteReference.OK + "Bought " + itemNumber + " " + itemToBuy.getEmoji() +
                                        " successfully. You now have " + player.getMoney() + " credits.").queue();

                            } else {
                                event.getChannel().sendMessage(EmoteReference.STOP + "You don't have enough money to buy this item.")
                                        .queue();
                            }
                            return;
                        } catch(Exception e) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "Item doesn't exist or invalid syntax.").queue();
                        }
                        return;
                    }
                }

                EmbedBuilder embed = baseEmbed(event, EmoteReference.MARKET + "Mantaro Market");

                StringBuilder items = new StringBuilder();
                StringBuilder prices = new StringBuilder();
                AtomicInteger atomicInteger = new AtomicInteger();
                Stream.of(Items.ALL).forEach(item -> {
                    if(!item.isHidden()) {
                        String buyValue = item.isBuyable() ? String.format("$%d", (int) Math.floor(item.getValue() *
                                1.1)) : "N/A";
                        String sellValue = item.isSellable() ? String.format("$%d", (int) Math.floor(item.getValue
                                () * 0.9)) : "N/A";

                        items.append(String.format("**%02d.-** %s *%s*    ", atomicInteger.incrementAndGet(), item.getEmoji(), item.getName())).append("\n");
                        prices.append(String.format("%s **%s, %s**", "\uD83D\uDCB2", buyValue, sellValue)).append("\n");
                    }
                });

                event.getChannel().sendMessage(
                        embed.addField("Items", items.toString(), true)
                                .addField("Value (Buy/Sell)", prices.toString(), true)
                                .build()).queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Mantaro's market")
                        .setDescription("**List current items for buying and selling.**")
                        .addField("Buying and selling", "To buy do ~>market buy <item emoji>. It will substract the value from your money" +
                                " and give you the item.\n" +
                                "To sell do `~>market sell all` to sell all your items or `~>market sell <item emoji>` to sell the " +
                                "specified item. " +
                                "**You'll get the sell value of the item on coins to spend.**", false)
                        .addField("To know", "If you don't have enough money you cannot buy the items.\n" +
                                "Note: Don't use the item id, it's just for aesthetic reasons, the internal IDs are different than the ones shown here!", false)
                        .addField("Information", "To buy and sell multiple items you need to do `~>market <buy/sell> <amount> <item>`",
                                false)
                        .build();
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
                if(mentionedUsers.size() == 0)
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You need to mention a user").queue();
                else {
                    User giveTo = mentionedUsers.get(0);

                    if(event.getAuthor().getId().equals(giveTo.getId())) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot transfer an item to yourself!").queue();
                        return;
                    }

                    if(!handleDefaultRatelimit(rl, event.getAuthor(), event)) return;

                    Item item = Items.fromAny(args[1]).orElse(null);
                    if(item == null) {
                        event.getChannel().sendMessage("There isn't an item associated with this emoji.").queue();
                    } else {
                        Player player = MantaroData.db().getPlayer(event.getAuthor());
                        Player giveToPlayer = MantaroData.db().getPlayer(giveTo);
                        if(args.length == 2) {
                            if(player.getInventory().containsItem(item)) {
                                if(item.isHidden()) {
                                    event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot transfer this item!").queue();
                                    return;
                                }

                                if(giveToPlayer.getInventory().asMap().getOrDefault(item, new ItemStack(item, 0)).getAmount() >= 5000) {
                                    event.getChannel().sendMessage(EmoteReference.ERROR + "Don't do that").queue();
                                    return;
                                }

                                player.getInventory().process(new ItemStack(item, -1));
                                giveToPlayer.getInventory().process(new ItemStack(item, 1));
                                event.getChannel().sendMessage(EmoteReference.OK + event.getAuthor().getAsMention() + " gave 1 " + item
                                        .getName() + " to " + giveTo.getAsMention()).queue();
                            } else {
                                event.getChannel().sendMessage(EmoteReference.ERROR + "You don't have any of these items in your inventory")
                                        .queue();
                            }
                            player.save();
                            giveToPlayer.save();
                            return;
                        }

                        try {
                            int amount = Math.abs(Integer.parseInt(args[2]));
                            if(player.getInventory().containsItem(item) && player.getInventory().getAmount(item) >= amount) {
                                if(item.isHidden()) {
                                    event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot transfer this item!").queue();
                                    return;
                                }

                                if(giveToPlayer.getInventory().asMap().getOrDefault(item, new ItemStack(item, 0)).getAmount() + amount >= 5000) {
                                    event.getChannel().sendMessage(EmoteReference.ERROR + "Don't do that").queue();
                                    return;
                                }

                                player.getInventory().process(new ItemStack(item, amount * -1));
                                giveToPlayer.getInventory().process(new ItemStack(item, amount));
                                event.getChannel().sendMessage(EmoteReference.OK + event.getAuthor().getAsMention() + " gave " + amount +
                                        " " + item.getName() + " to " + giveTo.getAsMention()).queue();
                            } else
                                event.getChannel().sendMessage(EmoteReference.ERROR + "You don't have enough of this item " +
                                        "to do that").queue();
                        } catch(NumberFormatException nfe) {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "Invalid number provided").queue();
                        }
                        player.save();
                        giveToPlayer.save();
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
    public void transfer(CommandRegistry cr) {
        cr.register("transfer", new SimpleCommand(Category.CURRENCY) {

            RateLimiter rl = new RateLimiter(TimeUnit.SECONDS, 10);

            @Override
            public void call(GuildMessageReceivedEvent event, String content, String[] args) {
                if(event.getMessage().getMentionedUsers().isEmpty()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You need to mention one user.").queue();
                    return;
                }

                if(event.getMessage().getMentionedUsers().get(0).equals(event.getAuthor())) {
                    event.getChannel().sendMessage(EmoteReference.THINKING + "You cannot transfer money to yourself.").queue();
                    return;
                }

                if(!handleDefaultRatelimit(rl, event.getAuthor(), event)) return;

                long toSend;

                try {
                    toSend = Math.abs(Long.parseLong(args[1]));
                } catch(Exception e) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You need to specify the amount.").queue();
                    return;
                }

                if(toSend == 0) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot transfer no money :P").queue();
                    return;
                }

                if(toSend > TRANSFER_LIMIT){
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot transfer this much money. (Limit: " + TRANSFER_LIMIT + ")").queue();
                    return;
                }

                Player transferPlayer = MantaroData.db().getPlayer(event.getMember());

                if(transferPlayer.isLocked()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot transfer money now.").queue();
                    return;
                }

                if(transferPlayer.getMoney() < toSend) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot transfer money you don't have.").queue();
                    return;
                }

                User user = event.getMessage().getMentionedUsers().get(0);
                if(user.isBot() && !user.getId().equals("224662505157427200")) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You cannot transfer money to a bot.").queue();
                    return;
                }

                Player toTransfer = MantaroData.db().getPlayer(event.getGuild().getMember(user));

                if(toTransfer.isLocked()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "That user cannot receive money now.").queue();
                    return;
                }

                if(toTransfer.getMoney() + toSend < 0) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "Don't do that.").queue();
                    return;
                }

                if(toTransfer.getMoney() > (long)TRANSFER_LIMIT * 20) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "This user already has too much money...").queue();
                    return;
                }

                if(toTransfer.addMoney(toSend)) {
                    transferPlayer.removeMoney(toSend);
                    transferPlayer.saveAsync();

                    if(user.getId().equals("224662505157427200")) {
                        MantaroBot.getInstance().getTextChannelById(329013929890283541L).
                                sendMessage(event.getAuthor().getId() + " transferred **" + toSend + "** to you successfully.").queue();
                    }

                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Transferred **" + toSend + "** to *" + event.getMessage()
                            .getMentionedUsers().get(0).getName() + "* successfully.").queue();

                    toTransfer.saveAsync();

                    rl.process(toTransfer.getUserId());
                } else {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "Don't do that.").queue();
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Transfer command")
                        .setDescription("**Transfers money from you to another player.**")
                        .addField("Usage", "`~>transfer <@user> <money>` - **Tranfers money to player x**", false)
                        .addField("Parameters", "`@user` - user to send money to\n" +
                                "`money` - money to transfer.", false)
                        .addField("Important", "You cannot send more money than what you already have", false)
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
                        player.save();
                        openLootBox(event, true);
                    } else {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You need a loot crate key to open a crate. It's locked!")
                                .queue();
                    }
                } else {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You need a loot crate! How else would you use your key >" +
                            ".>").queue();
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Open loot crates")
                        .setDescription("**Yep. It's really that simple**")
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
            if(o1.getValue() > o2.getValue()) return 1;
            if(o1.getValue() == o2.getValue()) return 0;
            return -1;
        });
        if(!special) {
            for(Item i : Items.ALL) if(i.isHidden() || !i.isBuyable() || i.isSellable()) items.add(i);
        }
        for(int i = 0; i < amtItems; i++) toAdd.add(selectReverseWeighted(items));
        Player player = MantaroData.db().getPlayer(event.getMember());
        ArrayList<ItemStack> ita = new ArrayList<>();
        toAdd.forEach(item -> ita.add(new ItemStack(item, 1)));
        boolean overflow = player.getInventory().merge(ita);
        player.save();
        event.getChannel().sendMessage(EmoteReference.LOOT_CRATE.getDiscordNotation() + "**You won:** " +
                toAdd.stream().map(Item::toString).collect(Collectors.joining(", ")) + (
                overflow ? ". But you already had too much, so you decided to throw away the excess" : ""
        )).queue();
    }

    private Item selectReverseWeighted(List<Item> items) {
        Map<Integer, Item> weights = new HashMap<>();
        int weightedTotal = 0;
        for(int i = 0; i < items.size(); i++) {
            int t = items.size() - i;
            weightedTotal += t;
            weights.put(t, items.get(i));
        }
        final int[] selected = {random.nextInt(weightedTotal)};
        for(Map.Entry<Integer, Item> i : weights.entrySet()) {
            if((selected[0] -= i.getKey()) <= 0) {
                return i.getValue();
            }
        }
        return null;
    }
}
