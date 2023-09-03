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
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.SplitUtil;
import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemHelper;
import net.kodehawa.mantarobot.commands.currency.item.ItemReference;
import net.kodehawa.mantarobot.commands.currency.item.ItemType;
import net.kodehawa.mantarobot.commands.currency.item.PotionEffect;
import net.kodehawa.mantarobot.commands.currency.item.special.Potion;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.commands.currency.profile.inventory.InventorySortType;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.Category;
import net.kodehawa.mantarobot.core.command.meta.Defer;
import net.kodehawa.mantarobot.core.command.meta.Description;
import net.kodehawa.mantarobot.core.command.meta.Help;
import net.kodehawa.mantarobot.core.command.meta.Name;
import net.kodehawa.mantarobot.core.command.meta.Options;
import net.kodehawa.mantarobot.core.command.slash.AutocompleteContext;
import net.kodehawa.mantarobot.core.command.slash.IContext;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
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
import net.kodehawa.mantarobot.db.entities.MongoUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.CustomFinderUtil;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.awt.Color;
import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils.ratelimit;

@Module
public class CurrencyCmds {
    private static final SecureRandom random = new SecureRandom();
    private static final IncreasingRateLimiter dailyCrateRatelimiter = new IncreasingRateLimiter.Builder()
            .limit(1)
            .cooldown(24, TimeUnit.HOURS)
            .maxCooldown(24, TimeUnit.HOURS)
            .randomIncrement(false)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("dailycrate")
            .build();
    private static final IncreasingRateLimiter toolsRatelimiter = new IncreasingRateLimiter.Builder()
            .spamTolerance(1)
            .limit(1)
            .cooldown(3, TimeUnit.SECONDS)
            .cooldownPenaltyIncrease(5, TimeUnit.SECONDS)
            .maxCooldown(5, TimeUnit.MINUTES)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("tools")
            .build();


    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(InventoryCommand.class);
        cr.registerSlash(OpenCrate.class);
        cr.registerSlash(DailyCrate.class);
        cr.registerSlash(Tools.class);
        cr.registerSlash(Use.class);
    }

    @Name("inventory")
    @Description("The hub for inventory related commands.")
    @Help(description = "The hub for inventory related commands. See the subcommands for more information.")
    @Category(CommandCategory.CURRENCY)
    public static class InventoryCommand extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {}

        @Name("show")
        @Description("Shows your inventory or a user's.")
        @Options({
                @Options.Option(type = OptionType.USER, name = "user", description = "The user to get the inventory of.")
        })
        @Help(description = "Shows your inventory or a user's inventory",
                usage = "`/inventory user:[user]`",
                parameters = {
                        @Help.Parameter(name = "user", description = "The user to get the inventory of.", optional = true)
                })
        public static class Show extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var user = ctx.getOptionAsUser("user", ctx.getAuthor());
                final var player = ctx.getPlayer(user);
                final var dbUser = ctx.getDBUser(user);
                showInventory(ctx, user, player, dbUser, false);
            }
        }

        @Name("brief")
        @Description("Shows your brief inventory or a user's.")
        @Options({
                @Options.Option(type = OptionType.USER, name = "user", description = "The user to get the inventory of.")
        })
        @Help(description = "Shows your brief inventory or a user's inventory",
                usage = "`/inventory brief user:[user]`",
                parameters = {
                        @Help.Parameter(name = "user", description = "The user to get the inventory of.", optional = true)
                })
        public static class Brief extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var user = ctx.getOptionAsUser("user", ctx.getAuthor());
                final var player = ctx.getPlayer(user);
                final var dbUser = ctx.getDBUser(user);
                showInventory(ctx, user, player, dbUser, true);
            }
        }

        @Name("sort")
        @Description("Sort your inventory.")
        @Options({@Options.Option(
                type = OptionType.STRING,
                name = "type",
                description = "The sort type.",
                choices = {
                        @Options.Choice(description = "Sort by individual value", value = "value"),
                        @Options.Choice(description = "Sort by amount", value = "amount"),
                        @Options.Choice(description = "Sort by type", value = "type"),
                        @Options.Choice(description = "Sort by total value", value = "value_total"),
                        @Options.Choice(description = "Sort randomly", value = "random"),
                },
                required = true
        )})
        @Help(
                description = "Lets you sort your inventory using specified presets.",
                usage = "`/inventory sort type:[preset]`",
                parameters = {
                        @Help.Parameter(name = "type", description = "The sort type to use.")
                }
        )
        public static class InventorySort extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var typeString = ctx.getOptionAsString("type");
                final var type = Utils.lookupEnumString(typeString, InventorySortType.class);
                if (type == null) { // This should literally never happen, though.
                    ctx.replyEphemeral("commands.profile.inventorysort.not_valid", EmoteReference.ERROR);
                    return;
                }

                final var player = ctx.getPlayer();
                player.inventorySortType(type);
                player.updateAllChanged();

                ctx.replyEphemeral("commands.profile.inventorysort.success", EmoteReference.CORRECT, ctx.getLanguageContext().get(type.getTranslate()));
            }
        }


        @Name("calculate")
        @Description("Calculate an inventory's worth.")
        @Options({
                @Options.Option(type = OptionType.USER, name = "user", description = "The user to get the inventory value of.")
        })
        @Help(description = "Calculate an inventory's worth.",
                usage = "`/inventory calculate user:[user]`",
                parameters = {
                        @Help.Parameter(name = "user", description = "The user to get the inventory value of.", optional = true)
                })
        public static class Calculate extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var member = ctx.getOptionAsMember("user", ctx.getMember());
                var player = ctx.getPlayer(member);
                calculateInventory(ctx, member, player);
            }
        }
    }

    @Name("opencrate")
    @Defer
    @Description("Opens a loot crate.")
    @Category(CommandCategory.CURRENCY)
    @Options({
            @Options.Option(type = OptionType.STRING, name = "crate", description = "The crate to open")
    })
    @Help(
            description = "Opens a loot crate.",
            usage = "`/opencrate crate:[crate name]`",
            parameters = {
                    @Help.Parameter(name = "crate", description = "The crate to open", optional = true)
            }
    )
    public static class OpenCrate extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var content = ctx.getOptionAsString("crate", "");
            var player = ctx.getPlayer();
            openCrate(ctx, content, player);
        }
    }

    @Name("dailycrate")
    @Defer
    @Description("Opens a daily premium loot crate.")
    @Category(CommandCategory.CURRENCY)
    @Options(@Options.Option(type = OptionType.BOOLEAN, name = "check", description = "Check whether you can claim one or not."))
    @Help(description = "Opens a daily premium loot crate.", usage = "`/dailycrate` - You need a crate key to open any crate.")
    public static class DailyCrate extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            if (!ctx.getDBUser().isPremium()) {
                ctx.reply("commands.dailycrate.not_premium", EmoteReference.ERROR);
                return;
            }

            if (ctx.getOptionAsBoolean("check")) {
                long rl = dailyCrateRatelimiter.getRemaniningCooldown(ctx.getAuthor());

                ctx.reply("commands.dailycrate.check", EmoteReference.TALKING,
                        (rl) > 0 ? Utils.formatDuration(ctx.getLanguageContext(), rl) :
                                //Yes, this is intended to be daily.about_now, just reusing strings.
                                ctx.getLanguageContext().get("commands.daily.about_now")
                );
                return;
            }

            final var player = ctx.getPlayer();
            dailyCrate(ctx, player);
        }
    }

    @Name("tools")
    @Description("Shows your equipped tools")
    @Category(CommandCategory.CURRENCY)
    @Help(description = "Shows your equipped tools")
    public static class Tools extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            if (!RatelimitUtils.ratelimit(toolsRatelimiter, ctx)) {
                return;
            }

            tools(ctx, ctx.getDBUser());
        }
    }

    @Name("use")
    @Defer
    @Description("Use an item or show all usable items.")
    @Category(CommandCategory.CURRENCY)
    public static class Use extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {}

        @Name("item")
        @Description("Use a interactive item.")
        @Options({
                @Options.Option(type = OptionType.STRING, name = "item", description = "The item to use", required = true, autocomplete = true),
                @Options.Option(type = OptionType.INTEGER, name = "amount", description = "The amount of the item to use")
        })
        @Help(
                description = """
                    Uses an item.
                    You need to have the item to use it, and the item has to be marked as *interactive*.
                    """,
                usage = "`/use item item:[item name] amount:[amount]`",
                parameters = {
                        @Help.Parameter(name = "item", description = "The item to use"),
                        @Help.Parameter(name = "amount", description = "The amount of the item to use. Maximum of 15.", optional = true)
                }
        )
        public static class Item extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                useItem(
                        ctx, ctx.getDBUser(), ctx.getPlayer(),
                        ctx.getOptionAsString("item"), ctx.getOptionAsInteger("amount", 1)
                );
            }

            @Override
            public void onAutocomplete(AutocompleteContext event) {
                ItemHelper.autoCompleteUsable(event);
            }
        }

        @Name("list")
        @Description("Shows all interactive items")
        @Help(description = "Shows all *interactive* items")
        public static class List extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                useItemList(ctx);
            }
        }
    }

    @Subscribe
    public void inventory(CommandRegistry cr) {
        var inv = (TreeCommand) cr.register("inventory", new TreeCommand(CommandCategory.CURRENCY) {
            @Override
            public Command defaultTrigger(Context context, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, I18nContext lang, String content) {
                        var arguments = ctx.getOptionalArguments();
                        // We don't really use most of them, but we kinda need to show a warning else users don't know what to do
                        content = Utils.replaceArguments(arguments, content, "calculate", "calc", "c", "b", "brief", "season", "s");

                        // Lambda memes lol
                        var finalContent = content;
                        ctx.findMember(content, members -> {
                            var member = CustomFinderUtil.findMemberDefault(finalContent, members, ctx, ctx.getMember());
                            if (member == null)
                                return;

                            final var player = ctx.getPlayer(member);
                            final var user = ctx.getDBUser(member);
                            showInventory(ctx, member.getUser(), player, user, false);
                        });
                    }
                };
            }
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Shows your current inventory.")
                        .setUsage("""
                                  You can mention someone on this command to see their inventory.
                                  Use `~>inventory -calculate` to see how much you'd get if you sell every sellable item on your inventory.
                                  """
                        )
                        .build();
            }
        });

        inv.addSubCommand("calculate", new SubCommand() {
            @Override
            public String description() {
                return "Calculates the value of your or someone's inventory.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                ctx.findMember(content, members -> {
                    var member = CustomFinderUtil.findMemberDefault(content, members, ctx, ctx.getMember());
                    if (member == null)
                        return;

                    var player = ctx.getPlayer(member);
                    calculateInventory(ctx, member, player);
                });
            }
        });

        inv.addSubCommand("brief", new SubCommand() {
            @Override
            public String description() {
                return "Gives a brief view of your inventory in a single message.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                ctx.findMember(content, members -> {
                    var member = CustomFinderUtil.findMemberDefault(content, members, ctx, ctx.getMember());
                    if (member == null)
                        return;

                    final var player = ctx.getPlayer(member);
                    final var user = ctx.getDBUser(member);
                    showInventory(ctx, member.getUser(), player, user, true);
                });
            }
        });

        inv.createSubCommandAlias("brief", "mobile");
        inv.createSubCommandAlias("brief", "b");

        cr.registerAlias("inventory", "inv");
        cr.registerAlias("inventory", "backpack");
    }

    @Subscribe
    public void lootcrate(CommandRegistry registry) {
        registry.register("opencrate", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                openCrate(ctx, content, ctx.getPlayer());
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

        registry.registerAlias("opencrate", "crate");
    }

    @Subscribe
    public void openPremiumCrate(CommandRegistry cr) {
        cr.register("dailycrate", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (!ctx.getDBUser().isPremium()) {
                    ctx.sendLocalized("commands.dailycrate.not_premium", EmoteReference.ERROR);
                    return;
                }

                if (args.length > 0 && args[0].equalsIgnoreCase("-check")) {
                    long rl = dailyCrateRatelimiter.getRemaniningCooldown(ctx.getAuthor());

                    ctx.sendLocalized("commands.dailycrate.check", EmoteReference.TALKING,
                            (rl) > 0 ? Utils.formatDuration(ctx.getLanguageContext(), rl) :
                                    //Yes, this is intended to be daily.about_now, just reusing strings.
                                    ctx.getLanguageContext().get("commands.daily.about_now")
                    );
                    return;
                }

                final var player = ctx.getPlayer();
                dailyCrate(ctx, player);
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
                        // Yes, parser limitations. Natan change to your parser eta wen :^), really though, we could use some generics on here lol
                        // NumberFormatException?
                        int amount = 1;
                        if (arguments.containsKey("amount")) {
                            try {
                                amount = Math.max(1, Math.abs(Integer.parseInt(arguments.get("amount"))));
                            } catch (NumberFormatException e) {
                                ctx.sendLocalized("commands.useitem.invalid_amount", EmoteReference.WARNING);
                                return;
                            }
                        }

                        if (content.isEmpty()) {
                            ctx.sendLocalized("commands.useitem.no_items_specified", EmoteReference.ERROR);
                            return;
                        }

                        useItem(ctx, ctx.getDBUser(), ctx.getPlayer(), args[0], amount);
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
                        .addParameterOptional("-amount", "The amount of items you want to use. Only works with potions/buffs. The maximum is 15.")
                        .build();
            }
        });

        cr.registerAlias("useitem", "use");
        ui.addSubCommand("list", new SubCommand() {
            @Override
            public String description() {
                return "Lists all usable (interactive) items.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                useItemList(ctx);
            }
        });

        ui.createSubCommandAlias("list", "ls");
        ui.createSubCommandAlias("list", "1s");
        ui.createSubCommandAlias("list", "Is");
    }

    @Subscribe
    public void tools(CommandRegistry cr) {
        cr.register("tools", new SimpleCommand(CommandCategory.CURRENCY) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (!RatelimitUtils.ratelimit(toolsRatelimiter, ctx)) {
                    return;
                }

                var dbUser = ctx.getDBUser();
                tools(ctx, dbUser);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Check the durability and status of your tools.")
                        .build();
            }
        });
    }

    private static void useItemList(IContext ctx) {
        var lang = ctx.getLanguageContext();
        var interactiveItems = ItemHelper.getUsableItems();
        List<MessageEmbed.Field> fields = new LinkedList<>();

        for (var item : interactiveItems) {
            fields.add(new MessageEmbed.Field(EmoteReference.BLUE_SMALL_MARKER + item.getEmoji() + "\u2009\u2009\u2009" + item.getName(),
                    "**" + lang.get("general.description") + ":**\u2009 *" + lang.get(item.getDesc()) + "*",
                    false
            ));
        }

        var builder = new EmbedBuilder()
                .setAuthor(lang.get("commands.useitem.ls.header"), null, ctx.getAuthor().getEffectiveAvatarUrl())
                .setColor(Color.PINK)
                .setFooter(lang.get("general.requested_by").formatted(ctx.getMember().getEffectiveName()), null);

        DiscordUtils.sendPaginatedEmbed(
                ctx.getUtilsContext(), builder, DiscordUtils.divideFields(5, fields), lang.get("commands.useitem.ls.desc")
        );
    }

    private static void useItem(IContext ctx, MongoUser dbUser, Player player, String itemString, int amount) {
        var item = ItemHelper.fromAnyNoId(itemString, ctx.getLanguageContext()).orElse(null);
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

        if (!player.containsItem(item)) {
            ctx.sendLocalized("commands.useitem.no_item", EmoteReference.SAD);
            return;
        }

        applyPotionEffect(ctx, dbUser, item, player, amount);
    }

    private static void tools(IContext ctx, MongoUser dbUser) {
        var equippedItems = dbUser.getEquippedItems();
        var equipment = ProfileCmd.parsePlayerEquipment(equippedItems, ctx.getLanguageContext());

        ctx.send(equipment);
    }

    private static void dailyCrate(IContext ctx, Player player) {
        if (!ratelimit(dailyCrateRatelimiter, ctx, false)) {
            return;
        }

        var languageContext = ctx.getLanguageContext();
        // Alternate between mine and fish crates instead of doing so at random, since at random
        // it might seem like it only gives one sort of crate.
        var lastCrateGiven = player.getLastCrateGiven();
        var crate = ItemReference.MINE_PREMIUM_CRATE;
        if (lastCrateGiven == ItemHelper.idOf(ItemReference.MINE_PREMIUM_CRATE)) {
            crate = ItemReference.FISH_PREMIUM_CRATE;
        }

        if (lastCrateGiven == ItemHelper.idOf(ItemReference.FISH_PREMIUM_CRATE)) {
            crate = ItemReference.CHOP_PREMIUM_CRATE;
        }

        player.processItem(crate, 1);
        player.lastCrateGiven(ItemHelper.idOf(crate));
        player.updateAllChanged();

        var successMessage = languageContext.get("commands.dailycrate.success")
                .formatted(
                        EmoteReference.POPPER,
                        crate.getName()
                ) + "\n" + languageContext.get("commands.daily.sellout.already_premium");

        ctx.send(successMessage);
    }

    private static void openCrate(IContext ctx, String content, Player player) {
        var item = ItemHelper.fromAnyNoId(content.replace("\"", ""), ctx.getLanguageContext())
                .orElse(null);

        //Open default crate if nothing's specified.
        if (content.isBlank()) {
            item = ItemReference.LOOT_CRATE;
        }

        if (item == null) {
            ctx.sendLocalized("commands.opencrate.nothing_found", EmoteReference.ERROR);
            return;
        }

        if (item.getItemType() != ItemType.CRATE) {
            ctx.sendLocalized("commands.opencrate.not_crate", EmoteReference.ERROR);
            return;
        }

        var containsItem = player.containsItem(item);
        if (!containsItem) {
            ctx.sendLocalized("commands.opencrate.no_crate", EmoteReference.SAD, item.getName());
            return;
        }

        //Ratelimit handled here
        //Check ItemHelper.openLootCrate for implementation details.
        item.getAction().test(ctx, false);
    }

    private static void calculateInventory(IContext ctx, Member member, Player player) {
        if (member.getUser().isBot()) {
            ctx.sendLocalized("commands.inventory.bot_notice", EmoteReference.ERROR);
            return;
        }

        long all = player.getInventoryList().stream()
                .filter(item -> item.getItem().isSellable())
                .mapToLong(value -> Math.round(value.getItem().getValue() * value.getAmount() * 0.9d))
                .sum();

        ctx.sendLocalized("commands.inventory.calculate", EmoteReference.DIAMOND, member.getEffectiveName(), all);
    }

    private static void showInventory(IContext ctx, User user, Player player, MongoUser dbUser, boolean brief) {
        if (user.isBot()) {
            ctx.sendLocalized("commands.inventory.bot_notice", EmoteReference.ERROR);
            return;
        }

        var lang = ctx.getLanguageContext();
        final var inventoryList = player.getInventoryList();
        if (inventoryList.isEmpty()) {
            ctx.sendLocalized("commands.inventory.empty", EmoteReference.WARNING);
            return;
        }

        if (brief) {
            var inventory = lang.get("commands.inventory.sorted_by").formatted(lang.get(player.getInventorySortType().getTranslate()))
                    + "\n\n" +
                    inventoryList.stream()
                            .sorted(player.getInventorySortType().getSort().comparator())
                            .map(is -> is.getItem().getEmoji() + "\u2009 x" + is.getAmount() + " \u2009\u2009")
                            .collect(Collectors.joining(" "));

            var message = ctx.getLanguageContext().get("commands.inventory.brief")
                    .formatted(user.getName(), inventory);

            var toSend = SplitUtil.split(message, 2000, SplitUtil.Strategy.NEWLINE, SplitUtil.Strategy.WHITESPACE);
            DiscordUtils.listButtons(ctx.getUtilsContext(), 60, toSend);
            return;
        }

        var builder = new EmbedBuilder()
                .setAuthor(lang.get("commands.inventory.header").formatted(ctx.getAuthor().getName()),
                        null, ctx.getMember().getEffectiveAvatarUrl()
                )
                .setColor(ctx.getMember().getColor());

        List<MessageEmbed.Field> fields = new LinkedList<>();
        if (inventoryList.isEmpty())
            builder.setDescription(lang.get("general.dust"));
        else {
            inventoryList.stream()
                    .sorted(player.getInventorySortType().getSort().comparator())
                    .forEach(stack -> {
                        long buyValue = stack.getItem().isBuyable() ? stack.getItem().getValue() : 0;
                        long sellValue = stack.getItem().isSellable() ? Math.round(stack.getItem().getValue() * 0.9) : 0;
                        // Thin spaces are gonna haunt me.
                        fields.add(new MessageEmbed.Field(
                                "%s\u2009 %s\u2009 x %d".formatted(
                                        stack.getItem().getEmoji() + "\u2009",
                                        stack.getItem().getName(),
                                        stack.getAmount()),
                                lang.get("commands.inventory.format").formatted(
                                        EmoteReference.MONEY.toHeaderString() + "\u2009",
                                        "\u2009", buyValue, "\u2009", sellValue,
                                        EmoteReference.TALKING.toHeaderString() + "\u2009",
                                        lang.get(stack.getItem().getDesc())
                                ), false)
                        );
                    });
        }

        var toShow = random.nextInt(3) == 0 && !dbUser.isPremium() ? lang.get("general.sellout") : "";
        DiscordUtils.sendPaginatedEmbed(ctx.getUtilsContext(), builder, DiscordUtils.divideFields(7, fields), toShow);
    }

    public static void applyPotionEffect(IContext ctx, MongoUser dbUser, Item item, Player player, int amount) {
        if ((item.getItemType() == ItemType.POTION || item.getItemType() == ItemType.BUFF) && item instanceof Potion potion) {
            final var equippedItems = dbUser.getEquippedItems();
            var type = equippedItems.getTypeFor(item);

            if (amount < 1) {
                ctx.sendLocalized("commands.useitem.too_little", EmoteReference.SAD);
                return;
            }

            if (player.getItemAmount(item) < amount) {
                ctx.sendLocalized("commands.useitem.not_enough_items", EmoteReference.SAD);
                return;
            }

            var currentPotion = equippedItems.getCurrentEffect(type);
            var activePotion = equippedItems.isEffectActive(type, potion.getMaxUses());
            var isActive = currentPotion != null && currentPotion.getAmountEquipped() > 1;

            // This used to only check for activePotion.
            // The issue with this was that there could be one potion that was fully used, but there was another potion
            // waiting to be used. In that case the potion would get overridden.
            // In case you have more than a potion equipped, we'll just stack the rest as necessary.
            if (activePotion || isActive) { // currentPotion is NOT null here (both activePotion and isActive would mean a potion exists)
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
                    equippedItems.equipEffect(type, activePotion ? amount : Math.max(1, amount - 1));
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
                player.addBadgeIfAbsent(Badge.MAD_SCIENTIST);
            }

            // Default: 1
            player.processItem(item, -amount);
            player.updateAllChanged();
            equippedItems.updateAllChanged(dbUser);

            return;
        }

        item.getAction().test(ctx, false);
    }
}
