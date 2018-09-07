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

package net.kodehawa.mantarobot.commands.currency.item;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import net.kodehawa.mantarobot.utils.RandomCollection;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.RateLimiter;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.kodehawa.mantarobot.utils.Utils.handleDefaultRatelimit;

@Slf4j
public class Items {
    public static final Item HEADPHONES, BAN_HAMMER, KICK_BOOT, FLOPPY_DISK, MY_MATHS, PING_RACKET,
            LOADED_DICE, FORGOTTEN_MUSIC, CC_PENCIL, OVERFLOWED_BAG, BROM_PICKAXE, MILK, ALCOHOL, LEWD_MAGAZINE, RING,
            LOOT_CRATE_KEY, BOOSTER, BERSERK, ENHANCER, RING_2, COMPANION, LOADED_DICE_2, LOVE_LETTER, CLOTHES, SHOES, DIAMOND, CHOCOLATE, COOKIES,
            NECKLACE, ROSE, DRESS, TUXEDO, LOOT_CRATE, STAR, STAR_2, SLOT_COIN, HOUSE, CAR, BELL_SPECIAL, CHRISTMAS_TREE_SPECIAL, PANTS, POTION_HASTE, POTION_CLEAN,
            POTION_STAMINA, FISHING_ROD, FISH_1, FISH_2, FISH_3, GEM_1, GEM_2, GEM_3, GEM_4, MOP, CLAIM_KEY, COFFEE, WAIFU_PILL, FISHING_BAIT, DIAMOND_PICKAXE,
            TELEVISION, WRENCH, MOTORCYCLE, GEM1_PICKAXE, GEM2_PICKAXE, PIZZA;

    private static final Random r = new Random();
    private static final RateLimiter lootCrateRatelimiter = new RateLimiter(TimeUnit.MINUTES, 15);

    public static final Item[] ALL = {
            HEADPHONES = new Item(ItemType.COLLECTABLE, "\uD83C\uDFA7", "Headphones", "items.headphones", "items.description.headphones", 5, true, false, false),
            BAN_HAMMER = new Item(ItemType.COLLECTABLE, "\uD83D\uDD28", "Ban Hammer", "items.ban_hammer", "items.description.ban_hammer", 15, false, false),
            KICK_BOOT = new Item(ItemType.COLLECTABLE, "\uD83D\uDC62", "Kick Boot",  "items.kick_boot","items.description.kick_boot", 12, false, false),
            FLOPPY_DISK = new Item(ItemType.COLLECTABLE, "\uD83D\uDCBE", "Floppy Disk", "items.floppy", "items.description.floppy", 13, false, false),
            MY_MATHS = new Item(ItemType.COLLECTABLE, "\uD83D\uDCDD", "My Maths", "items.maths", "items.description.maths", 11, false, false),
            PING_RACKET = new Item(ItemType.COLLECTABLE, "\uD83C\uDFD3", "Ping Racket", "items.racket", "items.description.racket", 15, false, false),
            LOADED_DICE = new Item(ItemType.COLLECTABLE, "\uD83C\uDFB2", "Loaded Die", "items.die", "items.description.loaded_die", 45, false, false),
            FORGOTTEN_MUSIC = new Item(ItemType.COLLECTABLE, "\uD83C\uDFB5", "Forgotten Music", "items.forgotten", "items.description.forgotten", 15, false, false),
            CC_PENCIL = new Item(ItemType.COLLECTABLE, "\u270f", "Pencil", "items.pencil", "items.description.pencil", 15, false, false),
            OVERFLOWED_BAG = new Item(ItemType.COLLECTABLE, "\uD83D\uDCB0","Moneybag", "items.moneybag", "items.description.moneybag", 95, true),
            BROM_PICKAXE = new Item(ItemType.INTERACTIVE, "\u26cf","Brom's Pickaxe", "items.pick", "items.description.pick", 100, true),
            MILK = new Item(ItemType.COMMON, EmoteReference.POTION1.getUnicode(),"Milk", "items.milk", "items.description.milk", 25, true),
            ALCOHOL = new Item(ItemType.COMMON, EmoteReference.POTION2.getUnicode(),"Beverage", "items.beverage", "items.description.beverage", 25, true),
            LEWD_MAGAZINE = new Item(ItemType.COMMON, EmoteReference.MAGAZINE.getUnicode(),"Lewd Magazine", "items.lewd", "items.description.lewd", 25, true),
            RING = new Item(ItemType.COMMON, EmoteReference.RING.getUnicode(),"Marriage Ring", "items.ring", "items.description.ring", 60, true),
            LOVE_LETTER = new Item(ItemType.COLLECTABLE, EmoteReference.LOVE_LETTER.getUnicode(),"Love Letter", "items.letter", "items.description.letter", 45, false, false),
            LOOT_CRATE_KEY = new Item(ItemType.COMMON, EmoteReference.KEY.getUnicode(),"Crate Key", "items.key", "items.description.key", 58, true),
            CLOTHES = new Item(ItemType.COMMON, EmoteReference.CLOTHES.getUnicode(),"Clothes", "items.clothes", "items.description.clothes", 30, true),
            DIAMOND = new Item(ItemType.COMMON, EmoteReference.DIAMOND.getUnicode(),"Diamond", "items.diamond", "items.description.diamond", 350, true),
            DRESS = new Item(ItemType.COMMON, EmoteReference.DRESS.getUnicode(),"Wedding Dress", "items.dress", "items.description.dress", 75, true),
            NECKLACE = new Item(ItemType.COMMON, EmoteReference.NECKLACE.getUnicode(),"Necklace", "items.necklace", "items.description.necklace", 17, true),
            TUXEDO = new Item(ItemType.COMMON, EmoteReference.TUXEDO.getUnicode(),"Tuxedo", "items.tuxedo", "items.description.tuxedo", 50, true),
            SHOES = new Item(ItemType.COMMON, EmoteReference.SHOES.getUnicode(),"Shoes", "items.shoes", "items.description.shoes", 10, true),
            ROSE = new Item(ItemType.COMMON, EmoteReference.ROSE.getUnicode(),"Rose", "items.rose", "items.description.rose", 25, true),
            CHOCOLATE = new Item(ItemType.COMMON, EmoteReference.CHOCOLATE.getUnicode(),"Chocolate", "items.chocolate", "items.description.chocolate", 23, true),
            COOKIES = new Item(ItemType.COMMON, EmoteReference.COOKIE.getUnicode(),"Cookie", "items.cookie", "items.description.cookie", 10, true),

            // ---------------------------------- LEFT OVERS FROM CURRENCY V1 STARTS HERE ----------------------------------
            //CANNOT REMOVE BECAUSE WE WERE MEME ENOUGH TO FUCKING SAVE THEM BY THEIR IDS
            LOADED_DICE_2 = new Item("\uD83C\uDFB2","Special Loaded Die", "items.description.die_2"),
            BOOSTER = new Item(EmoteReference.RUNNER.getUnicode(),"Booster", "items.description.booster"),
            BERSERK = new Item(EmoteReference.CROSSED_SWORD.getUnicode(),"Berserk", "items.description.berserk"),
            COMPANION = new Item(EmoteReference.DOG.getUnicode(),"Companion", "items.description.companion"),
            RING_2 = new Item("\uD83D\uDC5A","Special Ring", "items.description.special_ring"),
            ENHANCER = new Item(EmoteReference.MAG.getUnicode(),"Enchancer", "items.description.enchancer"),
            STAR = new Item(ItemType.COLLECTABLE, "\u26A0\uFE0F","Prize", "items.prize", "items.description.prize", 0, false, false, true),

            // ---------------------------------- LEFT OVERS FROM CURRENCY V1 END HERE ----------------------------------
            LOOT_CRATE = new Item(ItemType.INTERACTIVE, EmoteReference.LOOT_CRATE.getDiscordNotation(),"Loot Crate",  "items.crate","items.description.crate", 0, false, false, true, Items::openLootCrate),
            STAR_2 = new Item(ItemType.COMMON, EmoteReference.STAR.getUnicode(),"Prize 2", "items.prize_2", "items.description.prize_2", 500, true, false, true),
            SLOT_COIN = new Item(ItemType.COMMON, "\uD83C\uDF9F","Slot ticket", "items.slot_ticket","items.description.slot_ticket", 65, true, true),
            HOUSE = new Item(ItemType.COMMON, EmoteReference.HOUSE.getUnicode(), "House", "items.house", "items.description.house", 5000, true, true),
            CAR = new Item(ItemType.COMMON, "\uD83D\uDE97","Car",  "items.car","items.description.car", 1000, true, true),

            // ---------------------------------- CHRISTMAS 2017 EVENT STARTS HERE ----------------------------------
            BELL_SPECIAL = new Item(ItemType.RARE, "\uD83D\uDD14","Christmas bell", "items.bell", "items.description.bell", 0, false, false, true),
            CHRISTMAS_TREE_SPECIAL = new Item(ItemType.RARE, "\uD83C\uDF84", "Christmas tree", "items.tree", "items.description.tree", 0, false, false, true),
            // ---------------------------------- CHRISTMAS 2017 EVENT ENDS HERE ----------------------------------

            // ---------------------------------- 5.0 ITEMS START HERE ----------------------------------
            PANTS = new Item(ItemType.COMMON, "\uD83D\uDC56", "Pants", "items.pants", "items.description.pants", 20, true),
            POTION_HASTE = new Item(ItemType.INTERACTIVE, "\uD83C\uDF76","Haste Potion", "items.haste", "items.description.haste", 490, true),
            POTION_CLEAN = new Item(ItemType.INTERACTIVE, "\uD83C\uDF7C","Milky Potion", "items.milky", "items.description.milky", 50, true),
            POTION_STAMINA = new Item(ItemType.INTERACTIVE, "\uD83C\uDFFA","Energy Beverage", "items.energy", "items.description.energy", 450, true),
            FISHING_ROD = new Item(ItemType.INTERACTIVE, "\uD83C\uDFA3","Fishing Rod", "items.rod", "items.description.rod", 65, true),
            FISH_1 = new Item(ItemType.FISHING, "\uD83D\uDC1F","Fish", "items.fish", "items.description.fish", 10, false),
            FISH_2 = new Item(ItemType.FISHING, "\uD83D\uDC20","Tropical Fish", "items.tropical_fish", "items.description.tropical_fish", 30, false),
            FISH_3 = new Item(ItemType.FISHING, "\uD83D\uDC21","Blowfish", "items.blowfish", "items.description.blowfish", 15, false),
            // ---------------------------------- 5.0 MINING ITEMS START HERE ----------------------------------
            GEM_1 = new Item(ItemType.MINE, "\u2604", "Comet Gem", "items.comet_gem", "items.description.comet_gem", 40, false),
            GEM_2 = new Item(ItemType.MINE, EmoteReference.STAR.getUnicode(), "Star Gem", "items.star_gem", "items.description.star_gem", 60, false),
            GEM_3 = new Item(ItemType.MINE, "\uD83D\uDD78", "Cobweb", "items.cobweb", "items.description.cobweb", 10, false),
            GEM_4 = new Item(ItemType.MINE, "\uD83D\uDCAB", "Gem Fragment", "items.fragment", "items.description.fragment", 50, false),
            // ---------------------------------- 5.0 ITEMS START HERE (again lol) ----------------------------------
            MOP = new Item(ItemType.INTERACTIVE, "\u3030","Mop", "items.mop", "items.description.mop", 10, true),
            CLAIM_KEY = new Item(ItemType.COMMON, EmoteReference.KEY.getUnicode(),"Claim Key", "items.claim_key", "items.description.claim_key", 1, false, true),
            COFFEE = new Item(ItemType.COMMON, "\u2615","Coffee", "items.coffee", "items.description.coffee", 10, true),
            WAIFU_PILL = new Item(ItemType.INTERACTIVE, "\ud83d\udc8a","Waifu Pill", "items.waifu_pill", "items.description.waifu_pill", 670, true),
            FISHING_BAIT = new Item(ItemType.INTERACTIVE, "\uD83D\uDC1B","Fishing bait.", "items.bait", "items.description.bait", 15, true),
            DIAMOND_PICKAXE = new Item(ItemType.CAST_MINE, "\u2692\ufe0f","Diamond Pickaxe", "items.diamond_pick", "items.description.diamond_pick", 450, true, false, "1;2", 10, 18),
            TELEVISION = new Item(ItemType.COMMON, "\uD83D\uDCFA","Television", "items.tv", "items.description.tv", 45, true),
            WRENCH = new Item(ItemType.COMMON, "\ud83d\udd27","Wrench", "items.wrench", "items.description.wrench", 50, true),
            //car is 1000 credits, so this is 350
            MOTORCYCLE = new Item(ItemType.COMMON, "\uD83C\uDFCD","Motorcycle",  "items.motorcycle","items.description.motorcycle", 350, true),
            //TODO: proper emojis
            GEM1_PICKAXE = new Item(ItemType.CAST_MINE, "\u2692\ufe0f","Comet Gem Pickaxe", "items.comet_pick", "items.description.comet_pick", 350, true, false, "1;2", 10, 48),
            GEM2_PICKAXE = new Item(ItemType.CAST_MINE, "\u2692\ufe0f","Star Gem Pickaxe", "items.star_pick", "items.description.star_pick", 350, true, false, "1;2", 10, 49),
            PIZZA = new Item(ItemType.COMMON, "\uD83C\uDF55","Pizza", "items.pizza", "items.description.pizza", 15, true, false, "1;2", 10, 49),
    };


    public static void setItemActions() {
        final SecureRandom random = new SecureRandom();
        log.info("Registering item actions...");
        final ManagedDatabase managedDatabase = MantaroData.db();

        MOP.setAction(((event, lang) -> {
            event.getChannel().sendMessageFormat(lang.get("general.misc_item_usage.mop"), EmoteReference.DUST).queue();
            return true;
        }));

        //Basically fish command.
        FISHING_ROD.setAction((event, lang) -> {
            Player p = managedDatabase.getPlayer(event.getAuthor());
            DBUser u = managedDatabase.getUser(event.getAuthor());
            Inventory playerInventory = p.getInventory();

            if(!playerInventory.containsItem(FISHING_ROD))
                return false;

            if(r.nextInt(100) > (handlePotion(POTION_STAMINA, 4, p) ? 90 : 80)) { //20% chance for the rod to break on usage (10% with stamina).
                event.getChannel().sendMessageFormat(lang.get("commands.fish.rod_broke"), EmoteReference.SAD).queue();
                playerInventory.process(new ItemStack(FISHING_ROD, -1));
                p.save();
                return false;
            } else {
                int select = random.nextInt(100);

                if(select < 25) {
                    //we need to continue the dust meme
                    event.getChannel().sendMessageFormat(lang.get("commands.fish.dust"), EmoteReference.TALKING).queue();
                    return false;
                } else if(select < 45) {
                    List<Item> common = Stream.of(ALL)
                            .filter(i -> i.getItemType() == ItemType.COMMON && !i.isHidden() && i.isSellable() && i.value < 45)
                            .collect(Collectors.toList());
                    Item selected = common.get(random.nextInt(common.size()));
                    if(playerInventory.getAmount(selected) == 5000) {
                        event.getChannel().sendMessageFormat(lang.get("commands.fish.trash.overflow"), EmoteReference.SAD).queue();
                        return true;
                    }

                    playerInventory.process(new ItemStack(selected, 1));
                    event.getChannel().sendMessageFormat(lang.get("commands.fish.trash.success"), EmoteReference.EYES, selected.getEmoji()).queue();
                } else if (select > 45) {
                    List<Item> fish = Stream.of(ALL)
                            .filter(i -> i.getItemType() == ItemType.FISHING && !i.isHidden() && i.isSellable())
                            .collect(Collectors.toList());
                    RandomCollection<Item> fishItems = new RandomCollection<>();

                    int money = 0;
                    int amount = handleBuff(FISHING_BAIT, 1, p) ? Math.max(1, random.nextInt(6)) : random.nextInt(4);
                    fish.forEach((item) -> fishItems.add(3, item));

                    if(select > 75) {
                        money = Math.max(5, random.nextInt(85));
                    }

                    boolean waifuHelp = false;
                    if(Items.handlePotion(Items.WAIFU_PILL, 5, p)) {
                        if(u.getData().getWaifus().entrySet().stream().anyMatch((w) -> w.getValue() > 2_000_000L)) {
                            money += Math.max(10, random.nextInt(100));
                            waifuHelp = true;
                        }
                    }

                    List<ItemStack> list = new ArrayList<>(amount);
                    boolean overflow = false;
                    for(int i = 0; i < amount; i++) {
                        Item it = fishItems.next();

                        if(playerInventory.getAmount(it) >= 5000) {
                            overflow = true;
                            continue;
                        }

                        list.add(new ItemStack(it, 1));
                    }

                    List<ItemStack> reducedList = ItemStack.reduce(list);
                    playerInventory.process(reducedList);
                    p.addMoney(money);

                    String itemDisplay = ItemStack.toString(reducedList);
                    boolean foundFish = !reducedList.isEmpty();
                    //I check it down there again, I know, but at the same time the other if statement won't run if there's no money but there are fish.
                    if(foundFish) {
                        p.getData().addBadgeIfAbsent(Badge.FISHER);
                    }

                    if(money > 0 && !foundFish) {
                        event.getChannel().sendMessageFormat(lang.get("commands.fish.success_money_noitem"), EmoteReference.POPPER, money).queue();
                    } else if(money > 0) {
                        event.getChannel().sendMessageFormat(lang.get("commands.fish.success_money"),
                                EmoteReference.POPPER, itemDisplay, money, (waifuHelp ? "\n" + lang.get("commands.fish.waifu_help") : "")
                        ).queue();
                    } else if (foundFish) {
                        event.getChannel().sendMessageFormat(lang.get("commands.fish.success"), EmoteReference.POPPER, itemDisplay).queue();
                    } else {
                        //somehow we go all the way back and it's dust again (forgot to handle it?)
                        event.getChannel().sendMessageFormat(lang.get("commands.fish.dust"), EmoteReference.TALKING).queue();
                    }

                    if(overflow)
                        event.getChannel().sendMessageFormat(lang.get("commands.fish.overflow"), EmoteReference.SAD).queue();
                }

                p.save();
                return true;
            }
        });

        //START OF PICKAXE ACTION DECLARATION
        BROM_PICKAXE.setAction((event, lang) -> {
            Player p = managedDatabase.getPlayer(event.getAuthor());
            return handlePickaxe(event, lang, BROM_PICKAXE, p, 0.40f);
        });

        DIAMOND_PICKAXE.setAction((event, lang) -> {
            Player p = managedDatabase.getPlayer(event.getAuthor());
            return handlePickaxe(event, lang, DIAMOND_PICKAXE, p, 0.29f);
        });

        GEM1_PICKAXE.setAction((event, lang) -> {
            Player p = managedDatabase.getPlayer(event.getAuthor());
            return handlePickaxe(event, lang, GEM1_PICKAXE, p, 0.36f);
        });

        GEM2_PICKAXE.setAction((event, lang) -> {
            Player p = managedDatabase.getPlayer(event.getAuthor());
            return handlePickaxe(event, lang, GEM2_PICKAXE, p, 0.35f);
        });
        //END OF PICKAXE ACTION DECLARATION

        POTION_CLEAN.setAction((event, lang) -> {
            Player p = managedDatabase.getPlayer(event.getAuthor());
            p.getData().setActivePotion(null);
            event.getChannel().sendMessageFormat(lang.get("general.misc_item_usage.milk"), EmoteReference.POPPER).queue();
            p.getInventory().process(new ItemStack(POTION_CLEAN, -1));
            p.save();
            return true;
        });

        POTION_STAMINA.setAction((event, lang) -> {
            Player p = managedDatabase.getPlayer(event.getAuthor());
            p.getData().setActivePotion(new PotionEffect(idOf(POTION_STAMINA), System.currentTimeMillis(), ItemType.PotionType.PLAYER));
            event.getChannel().sendMessageFormat(lang.get("general.misc_item_usage.stamina"), EmoteReference.POPPER).queue();
            p.getInventory().process(new ItemStack(POTION_STAMINA, -1));
            p.save();
            return true;
        });

        POTION_HASTE.setAction((event, lang) -> {
            Player p = managedDatabase.getPlayer(event.getAuthor());
            p.getData().setActivePotion(new PotionEffect(idOf(POTION_HASTE),
                    System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(2), ItemType.PotionType.PLAYER));
            event.getChannel().sendMessageFormat(lang.get("general.misc_item_usage.haste"), EmoteReference.POPPER).queue();
            p.getInventory().process(new ItemStack(POTION_HASTE, -1));
            p.save();
            return true;
        });

        FISHING_BAIT.setAction((event, lang) -> {
            Player p = managedDatabase.getPlayer(event.getAuthor());
            p.getData().setActiveBuff(new PotionEffect(idOf(FISHING_BAIT),
                    System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(2), ItemType.PotionType.PLAYER));
            event.getChannel().sendMessageFormat(lang.get("general.misc_item_usage.bait"), EmoteReference.POPPER).queue();
            p.getInventory().process(new ItemStack(FISHING_BAIT, -1));
            p.save();
            return true;
        });
    }

    public static Optional<Item> fromAny(String any) {
        try {
            Item item = fromId(Integer.parseInt(any));

            if(item != null)
                return Optional.of(item);
        } catch(NumberFormatException ignored) {}

        return fromAnyNoId(any);
    }

    public static Optional<Item> fromAnyNoId(String any) {
        Optional<Item> itemOptional;

        itemOptional = fromEmoji(any);
        if(itemOptional.isPresent())
            return itemOptional;

        itemOptional = fromName(any);
        if(itemOptional.isPresent())
            return itemOptional;

        itemOptional = fromPartialName(any);
        return itemOptional;
    }

    public static Optional<Item> fromEmoji(String emoji) {
        return Stream.of(ALL).filter(item -> item.getEmoji().equals(emoji)).findFirst();
    }

    public static Item fromId(int id) {
        return ALL[id];
    }

    public static Optional<Item> fromName(String name) {
        return Arrays.stream(ALL).filter(item -> item.getName().toLowerCase().trim().equals(name.toLowerCase().trim())).findFirst();
    }

    public static Optional<Item> fromPartialName(String name) {
        return Arrays.stream(ALL).filter(item -> item.getName().toLowerCase().trim().contains(name.toLowerCase().trim())).findFirst();
    }

    public static int idOf(Item item) {
        return Arrays.asList(ALL).indexOf(item);
    }

    private static boolean openLootCrate(GuildMessageReceivedEvent event, I18nContext lang) {
        Player player = MantaroData.db().getPlayer(event.getAuthor());
        Inventory inventory = player.getInventory();
        if(inventory.containsItem(Items.LOOT_CRATE)) {
            if(inventory.containsItem(Items.LOOT_CRATE_KEY)) {
                if(!handleDefaultRatelimit(lootCrateRatelimiter, event.getAuthor(), event))
                    return false;

                inventory.process(new ItemStack(Items.LOOT_CRATE_KEY, -1));
                inventory.process(new ItemStack(Items.LOOT_CRATE, -1));

                player.getData().addBadgeIfAbsent(Badge.THE_SECRET);
                player.save();

                openLootBox(event, true, lang);
                return true;
            } else {
                event.getChannel().sendMessageFormat(lang.get("general.misc_item_usage.crate.no_key"), EmoteReference.ERROR).queue();
                return false;
            }
        } else {
            event.getChannel().sendMessageFormat(lang.get("general.misc_item_usage.crate.no_crate"), EmoteReference.ERROR).queue();
            return false;
        }
    }

    private static void openLootBox(GuildMessageReceivedEvent event, boolean special, I18nContext lang) {
        List<Item> toAdd = selectItems(r.nextInt(3) + 3, special ? ItemType.LootboxType.RARE : ItemType.LootboxType.COMMON);

        Player player = MantaroData.db().getPlayer(event.getMember());
        ArrayList<ItemStack> ita = new ArrayList<>();

        toAdd.forEach(item -> ita.add(new ItemStack(item, 1)));

        boolean overflow = player.getInventory().merge(ita);
        player.saveAsync();

        event.getChannel().sendMessage(String.format(lang.get("general.misc_item_usage.crate.success"),
                EmoteReference.LOOT_CRATE.getDiscordNotation(), toAdd.stream().map(Item::toString).collect(Collectors.joining(", ")),
                overflow ? ". " + lang.get("general.misc_item_usage.crate.overflow") : "")).queue();
    }

    private static List<Item> selectItems(int amount, ItemType.LootboxType type) {
        List<Item> all = Arrays.stream(Items.ALL).filter(i->i.isBuyable() || i.isSellable()).collect(Collectors.toList());

        List<Item> common = all.stream()
                .filter(i->i.getItemType() == ItemType.COMMON)
                .sorted(Comparator.comparingLong(i->i.value))
                .collect(Collectors.toList());
        List<Item> rare = all.stream()
                .filter(i->i.getItemType() == ItemType.RARE)
                .sorted(Comparator.comparingLong(i->i.value))
                .collect(Collectors.toList());
        List<Item> premium = all.stream()
                .filter(i->i.getItemType() == ItemType.PREMIUM)
                .sorted(Comparator.comparingLong(i->i.value))
                .collect(Collectors.toList());

        RandomCollection<Item> items = new RandomCollection<>();

        //fallthrough intended
        switch(type) {
            case EPIC:
                throw new UnsupportedOperationException();
            case PREMIUM:
                premium.forEach(i-> items.add(2, i));
            case RARE:
                rare.forEach(i-> items.add(5, i));
            case COMMON:
                common.forEach(i-> items.add(20, i));
        }

        List<Item> list = new ArrayList<>(amount);
        for(int i = 0; i < amount; i++) {
            list.add(items.next());
        }

        return list;
    }

    public static boolean handlePotion(Item i, int maxTimes, Player p) {
        boolean isPotionPresent = p.getData().getActivePotion() != null && fromId(p.getData().getActivePotion().getPotion()) == i;
        if (isPotionPresent) {
            //counter starts at 0
            if (p.getData().getActivePotion().getTimesUsed() >= maxTimes) {
                p.getData().setActivePotion(null);
                p.save();
            } else {
                long timesUsed = p.getData().getActivePotion().getTimesUsed();
                p.getData().getActivePotion().setTimesUsed(timesUsed + 1);
                p.save();
            }
        }

        return isPotionPresent;
    }

    public static boolean handleBuff(Item i, int maxTimes, Player p) {
        boolean isBuffPresent = p.getData().getActiveBuff() != null && fromId(p.getData().getActiveBuff().getPotion()) == i;
        if (isBuffPresent) {
            //counter starts at 0
            if (p.getData().getActiveBuff().getTimesUsed() >= maxTimes) {
                p.getData().setActiveBuff(null);
                p.save();
            } else {
                long timesUsed = p.getData().getActiveBuff().getTimesUsed();
                p.getData().getActiveBuff().setTimesUsed(timesUsed + 1);
                p.save();
            }
        }

        return isBuffPresent;
    }

    private static boolean handlePickaxe(GuildMessageReceivedEvent event, I18nContext lang, Item item, Player player, float chance) {
        Inventory playerInventory = player.getInventory();

        //Defensive programming :D
        if(!playerInventory.containsItem(item))
            return false;

        if(r.nextFloat() < (handlePotion(POTION_STAMINA, 4, player) ? (chance) + 0.05 : chance)) {
            event.getChannel().sendMessageFormat(lang.get("commands.mine.pick_broke"), EmoteReference.SAD).queue();
            playerInventory.process(new ItemStack(item, -1));
            player.save();
            return false;
        } else {
            return true;
        }

    }
}
