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
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemHelper;
import net.kodehawa.mantarobot.commands.currency.item.ItemReference;
import net.kodehawa.mantarobot.commands.currency.item.special.Food;
import net.kodehawa.mantarobot.commands.currency.pets.HousePet;
import net.kodehawa.mantarobot.commands.currency.pets.HousePetType;
import net.kodehawa.mantarobot.commands.currency.pets.PetChoice;
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
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.Marriage;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.utils.Pair;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.awt.Color;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Module
public class PetCmds {
    private static final IncreasingRateLimiter rl = new IncreasingRateLimiter.Builder()
            .limit(1)
            .spamTolerance(2)
            .cooldown(3, TimeUnit.SECONDS)
            .maxCooldown(5, TimeUnit.SECONDS)
            .randomIncrement(true)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("pet")
            .build();

    private static final IncreasingRateLimiter petRemoveRatelimiter = new IncreasingRateLimiter.Builder()
            .limit(1)
            .spamTolerance(2)
            .cooldown(1, TimeUnit.HOURS)
            .maxCooldown(2, TimeUnit.HOURS)
            .randomIncrement(false)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("pet-remove")
            .build();

    private static final IncreasingRateLimiter petpetRatelimiter = new IncreasingRateLimiter.Builder()
            .limit(25)
            .spamTolerance(5)
            .cooldown(1, TimeUnit.HOURS)
            .maxCooldown(2, TimeUnit.HOURS)
            .randomIncrement(false)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("pet-pet")
            .build();

    private static final IncreasingRateLimiter petChoiceRatelimiter = new IncreasingRateLimiter.Builder()
            .limit(3)
            .spamTolerance(5)
            .cooldown(10, TimeUnit.MINUTES)
            .maxCooldown(10, TimeUnit.MINUTES)
            .randomIncrement(false)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("pet-choice")
            .build();

    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(Pet.class);
    }

    @Description("The hub for pet related commands. Not the pat kind.")
    @Category(CommandCategory.CURRENCY)
    @Help(description =
                """
                Pet commands.
                For a better explanation of the pet system, check the [wiki](https://www.mantaro.site/mantaro-wiki).
                This command contains an explanation of what pets are. Check subcommands for the available actions.
                """)
    public static class Pet extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {}

        @Override
        public Predicate<SlashContext> getPredicate() {
            return ctx -> RatelimitUtils.ratelimit(rl, ctx, false);
        }

        @Description("Shows an explanation about the pet system.")
        public static class Explanation extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                ctx.sendLocalized("commands.pet.explanation");
            }
        }

        @Name("list")
        @Description("Lists the available pet types.")
        public static class PetList extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var pets = Arrays
                        .stream(HousePetType.values())
                        .sorted(Comparator.comparingLong(HousePetType::getCost))
                        .filter(HousePetType::isBuyable)
                        .map(pet -> {
                            var emoji = pet.getEmoji();
                            var name = pet.getName();
                            var abilities = pet.getStringAbilities();
                            var value = pet.getCost();

                            return String.format(ctx.getLanguageContext().get("commands.pet.list.summary"),
                                    emoji, name, abilities, value
                            );
                        })
                        .collect(Collectors.joining("\n"));

                ctx.reply("commands.pet.list.header",
                        EmoteReference.TALKING, pets, EmoteReference.PENCIL, ctx.getLanguageContext().get("commands.pet.list.abilities")
                );
            }
        }

        @Description("Lets you choose whether you want to use a personal or marriage pet.")
        @Defer
        @Options(@Options.Option(type = OptionType.STRING, name = "type", description = "The type to use. Either marriage or personal", required = true, choices = {
                @Options.Choice(description = "Marriage Pet", value = "marriage"),
                @Options.Choice(description = "Personal Pet", value = "personal")
        }))
        public static class Choice extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var choice = Utils.lookupEnumString(ctx.getOptionAsString("type"), PetChoice.class);
                if (choice == null) {
                    ctx.reply("commands.pet.choice.invalid_choice", EmoteReference.ERROR);
                    return;
                }

                var player = ctx.getPlayer();
                if (choice == player.getPetChoice()) {
                    ctx.reply("commands.pet.choice.already_chosen", EmoteReference.ERROR);
                    return;
                }

                if (!RatelimitUtils.ratelimit(petChoiceRatelimiter, ctx, ctx.getLanguageContext().get("commands.pet.choice.ratelimit_message"), false))
                    return;

                player.petChoice(choice);
                player.updateAllChanged();

                ctx.reply("commands.pet.choice.success", EmoteReference.CORRECT, Utils.capitalize(choice.toString()), EmoteReference.POPPER);
            }
        }

        @Description("Shows the level and experience of your current pet.")
        public static class Level extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var dbUser = ctx.getDBUser();
                var marriage = dbUser.getMarriage();
                var pet = getCurrentPet(ctx, ctx.getPlayer(), marriage, "commands.pet.level.no_pet");
                if (pet == null) {
                    return;
                }

                ctx.replyStripped("commands.pet.level.success",
                        EmoteReference.ZAP, pet.getName(), pet.getLevel(), pet.getExperience(), pet.experienceToNextLevel()
                );
            }
        }

        @Description("Shows the status of your current pet.")
        public static class Status extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var language = ctx.getLanguageContext();
                var dbUser = ctx.getDBUser();
                var marriage = dbUser.getMarriage();
                var player = ctx.getPlayer();
                var pet = getCurrentPet(ctx, player, marriage, "commands.pet.status.no_pet");
                if (pet == null) {
                    return;
                }

                var name = pet.getName().replace("\n", "").trim();
                var baseAbilities = pet.getType().getAbilities().stream()
                        .filter(ability -> ability != HousePetType.HousePetAbility.CHEER).toList();
                var hasItemBuildup = baseAbilities.contains(HousePetType.HousePetAbility.CATCH);

                EmbedBuilder status = new EmbedBuilder()
                        .setAuthor(String.format(language.get("commands.pet.status.header"), name), null, ctx.getAuthor().getEffectiveAvatarUrl())
                        .setColor(Color.PINK)
                        .setDescription(language.get("commands.pet.status.description"))
                        .addField(
                                EmoteReference.ZAP.toHeaderString() + language.get("commands.pet.status.choice"),
                                "%s".formatted(player.getActivePetChoice(marriage).getReadableName()), true
                        )
                        .addField(
                                EmoteReference.MONEY.toHeaderString() + language.get("commands.pet.status.cost"),
                                "%,d".formatted(pet.getType().getCost()), true
                        )
                        .addField(
                                EmoteReference.ZAP.toHeaderString() + language.get("commands.pet.status.type"),
                                pet.getType().getEmoji() + pet.getType().getName(), true
                        )
                        .addField(
                                EmoteReference.WRENCH.toHeaderString() + language.get("commands.pet.status.abilities"),
                                pet.getType().getStringAbilities(), false
                        )
                        .addField(EmoteReference.ZAP.toHeaderString() + language.get("commands.pet.status.level"),
                                "**%,d** (XP: %,d)".formatted(pet.getLevel(), pet.getExperience()), false
                        );

                // This is needed else we'll run into people thinking pets with no catch ability have a item buildup.
                // They don't.
                if (hasItemBuildup) {
                    status.addField(EmoteReference.STAR.toHeaderString() + language.get("commands.pet.status.buildup"),
                            language.get("commands.pet.status.buildup_stats")
                                    .formatted(
                                            pet.getType().getMaxCoinBuildup(pet.getLevel()),
                                            pet.getType().getMaxItemBuildup(pet.getLevel())
                                    ), false
                    );
                } else {
                    status.addField(EmoteReference.POUCH.toHeaderString() + language.get("commands.pet.status.buildup_coin"),
                            language.get("commands.pet.status.buildup_stats_credits").formatted(pet.getType().getMaxCoinBuildup(pet.getLevel())
                            ), false
                    );
                }

                status.addField(
                                EmoteReference.DUST.toHeaderString() + language.get("commands.pet.status.dust"),
                                "**%d%%**".formatted(pet.getDust()), true
                        )
                        .addField(
                                EmoteReference.HEART.toHeaderString() + language.get("commands.pet.status.health"),
                                "**%,d / 100**".formatted(pet.getHealth()), true
                        )
                        .addField(
                                EmoteReference.RUNNER.toHeaderString() + language.get("commands.pet.status.stamina"),
                                "**%,d / 100**".formatted(pet.getStamina()), true
                        )
                        .addField(
                                EmoteReference.DROPLET.toHeaderString() + language.get("commands.pet.status.thirst"),
                                "**%,d / 100**".formatted(pet.getThirst()), true
                        )
                        .addField(
                                EmoteReference.FORK.toHeaderString() + language.get("commands.pet.status.hunger"),
                                "**%,d / 100**".formatted(pet.getHunger()), true
                        )
                        .setThumbnail(ctx.getAuthor().getEffectiveAvatarUrl())
                        .setFooter(language.get("commands.pet.status.footer"));

                ctx.reply(status.build());
            }
        }

        @Description("Check thirst, hunger and dust of your current pet.")
        public static class Check extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var dbUser = ctx.getDBUser();
                var marriage = dbUser.getMarriage();
                var player = ctx.getPlayer();
                var pet = getCurrentPet(ctx, player, marriage, "commands.pet.status.no_pet");
                if (pet == null) {
                    return;
                }

                ctx.replyStripped("commands.pet.check.success",
                        pet.getName(), EmoteReference.DROPLET, pet.getThirst(),
                        EmoteReference.FORK, pet.getHunger(), EmoteReference.DUST, pet.getDust(), player.getActivePetChoice(marriage)
                );
            }
        }

        @Name("pet")
        @Defer
        @Description("Pets your pet. Cute.")
        public static class PetPet extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var dbUser = ctx.getDBUser();
                var player = ctx.getPlayer();
                var marriage = dbUser.getMarriage();
                var pet = player.getPet();
                var choice = player.getActivePetChoice(marriage);

                if (choice == PetChoice.MARRIAGE) {
                    if (marriage == null) {
                        ctx.reply("commands.pet.no_marriage", EmoteReference.ERROR);
                        return;
                    }

                    pet = marriage.getPet();
                }

                if (pet == null) {
                    ctx.reply("commands.pet.pat.no_pet", EmoteReference.ERROR, choice.getReadableName());
                    return;
                }

                var lang = ctx.getLanguageContext();
                if (!RatelimitUtils.ratelimit(petpetRatelimiter, ctx, lang.get("commands.pet.pat.ratelimit_message"), false))
                    return;

                var message = pet.handlePat().getMessage();
                var extraMessage = "";
                ctx.replyStripped(message, pet.getType().getEmoji(), pet.getName(), extraMessage);
            }
        }

        @Defer
        @Description("Cleans your pet when it's too dusty. Costs 600 credits.")
        public static class Clean extends SlashCommand {
            static final int basePrice = 600;

            @Override
            protected void process(SlashContext ctx) {
                var player = ctx.getPlayer();
                var dbUser = ctx.getDBUser();
                var marriage = dbUser.getMarriage();
                var price = basePrice;

                var pet = getCurrentPet(ctx, player, marriage, "commands.pet.status.no_pet");
                if (pet == null) {
                    return;
                }

                if (pet.getDust() <= 50) {
                    price = price / 2;
                }

                if (player.getCurrentMoney() < price) {
                    ctx.replyStripped("commands.pet.clean.not_enough_money", EmoteReference.ERROR, price, pet.getName());
                    return;
                }

                if (pet.getDust() < 20) {
                    ctx.replyStripped("commands.pet.clean.not_dusty", EmoteReference.ERROR, pet.getName(), pet.getDust());
                    return;
                }

                pet.setDust(0);
                player.removeMoney(price);
                player.markPetChange();
                player.updateAllChanged();

                if (player.getActivePetChoice(marriage) == PetChoice.MARRIAGE) {
                    marriage.markPetChange();
                    marriage.updateAllChanged();
                }

                ctx.replyStripped("commands.pet.clean.success", EmoteReference.CORRECT, pet.getName(), price);
            }
        }

        @Defer
        @Description("Sells this pet. This will *reset all pet stats*. Just like buying a new tamagotchi.")
        public static class Sell extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var dbUser = ctx.getDBUser();
                var marriage = dbUser.getMarriage();
                var player = ctx.getPlayer();
                var pet = getCurrentPet(ctx, player, marriage, "commands.pet.remove.no_pet");
                if (pet == null) {
                    return;
                }

                if (player.isLocked()) {
                    ctx.reply("commands.pet.locked_notice", EmoteReference.ERROR);
                    return;
                }

                if (!RatelimitUtils.ratelimit(petRemoveRatelimiter, ctx, false))
                    return;

                var toRefund = (long) ((pet.getType().getCost() / 2) * 0.9);
                var toRefundPersonal = (long) (pet.getType().getCost() * 0.9);

                Message message;
                var lang = ctx.getLanguageContext();
                if (player.getActivePetChoice(marriage) == PetChoice.MARRIAGE) {
                    if (marriage.isLocked()) {
                        ctx.reply("commands.pet.locked_notice", EmoteReference.ERROR);
                        return;
                    }

                    message = ctx.sendResult(lang.get("commands.pet.remove.confirm").formatted(EmoteReference.WARNING, toRefund));
                    marriage.locked(true);
                    marriage.updateAllChanged();
                } else {
                    message = ctx.sendResult(lang.get("commands.pet.remove.confirm_personal").formatted(EmoteReference.WARNING, toRefundPersonal));
                }

                player.locked(true);
                player.updateAllChanged();

                ButtonOperations.create(message, 60, event -> {
                    if (event.getUser().getIdLong() != ctx.getAuthor().getIdLong()) {
                        return Operation.IGNORED;
                    }

                    var button = event.getButton();
                    if (button.getId() == null) {
                        return Operation.IGNORED;
                    }

                    var hook = event.getHook();
                    if (button.getId().equals("yes-button")) {
                        final var playerFinal = ctx.getPlayer();
                        final var marriageConfirmed = ctx.getDBUser().getMarriage();
                        var petFinal = getCurrentPet(ctx, playerFinal, marriageConfirmed, "commands.pet.remove.no_pet");
                        if (petFinal == null) {
                            return Operation.COMPLETED;
                        }

                        var toRefundFinal = (long) ((petFinal.getType().getCost() / 2) * 0.9);
                        var toRefundPersonalFinal = (long) (petFinal.getType().getCost() * 0.9);

                        if (playerFinal.getActivePetChoice(marriageConfirmed) == PetChoice.MARRIAGE) {
                            if (marriageConfirmed == null) {
                                hook.editOriginal(lang.get("commands.pet.buy.no_marriage_marry").formatted(EmoteReference.ERROR)).setComponents().queue();
                                return Operation.COMPLETED;
                            }

                            final var marriedWithPlayer = ctx.getPlayer(marriageConfirmed.getOtherPlayer(ctx.getAuthor().getId()));
                            if (marriageConfirmed.getPet() == null) {
                                hook.editOriginal(lang.get("commands.pet.remove.no_pet_confirm").formatted(EmoteReference.ERROR)).setComponents().queue();
                                return Operation.COMPLETED;
                            }

                            marriageConfirmed.pet(null);
                            marriageConfirmed.locked(false);
                            marriageConfirmed.updateAllChanged();

                            marriedWithPlayer.addMoney(toRefundFinal);
                            playerFinal.addMoney(toRefundFinal);
                            playerFinal.locked(false);

                            playerFinal.markPetChange();
                            marriedWithPlayer.markPetChange();

                            playerFinal.updateAllChanged();
                            marriedWithPlayer.updateAllChanged();
                            hook.editOriginal(lang.get("commands.pet.remove.success").formatted(EmoteReference.CORRECT, toRefundFinal)).setComponents().queue();
                        } else {
                            if (playerFinal.getPet() == null) {
                                hook.editOriginal(lang.get("commands.pet.remove.no_pet_confirm").formatted(EmoteReference.ERROR)).setComponents().queue();
                                return Operation.COMPLETED;
                            }

                            playerFinal.setPet(null);
                            playerFinal.addMoney(toRefundPersonalFinal);
                            playerFinal.locked(false);
                            playerFinal.markPetChange();

                            playerFinal.updateAllChanged();
                            hook.editOriginal(lang.get("commands.pet.remove.success_personal").formatted(EmoteReference.CORRECT, toRefundPersonalFinal)).setComponents().queue();
                        }

                        return Operation.COMPLETED;
                    }

                    if (button.getId().equals("no-button")) {
                        var marriageConfirmed = ctx.getDBUser().getMarriage();
                        var playerFinal = ctx.getPlayer();
                        playerFinal.locked(false);
                        playerFinal.markPetChange();
                        playerFinal.updateAllChanged();

                        if (player.getActivePetChoice(marriage) == PetChoice.MARRIAGE && marriageConfirmed != null) {
                            marriageConfirmed.locked(false);
                            marriageConfirmed.updateAllChanged();
                        }

                        // This is reusing the string, nothing wrong here.
                        hook.editOriginal(lang.get("commands.pet.buy.cancel_success").formatted(EmoteReference.CORRECT)).setComponents().queue();
                        return Operation.COMPLETED;
                    }

                    return Operation.IGNORED;
                }, Button.danger("yes-button", lang.get("buttons.yes")),
                        Button.primary("no-button", lang.get("buttons.no"))
                );
            }
        }

        @Defer
        @Description("Buys a pet to have adventures with. You need to buy a pet house in market first.")
        @Options({
                @Options.Option(type = OptionType.STRING, name = "type", description = "The pet type. Use /pet list for a list.", required = true),
                @Options.Option(type = OptionType.STRING, name = "name", description = "The pet name.", required = true)
        })
        public static class Buy extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var player = ctx.getPlayer();
                var dbUser = ctx.getDBUser();
                var marriage = dbUser.getMarriage();
                var name = ctx.getOptionAsString("name").replace("\n", "").trim();
                var type = ctx.getOptionAsString("type");

                final var petChoice = player.getActivePetChoice(marriage);
                if (petChoice == PetChoice.MARRIAGE) {
                    if (marriage == null) {
                        ctx.reply("commands.pet.no_marriage", EmoteReference.ERROR);
                        return;
                    }

                    if (!marriage.hasCar() || !marriage.hasHouse()) {
                        ctx.reply("commands.pet.buy.no_requirements", EmoteReference.ERROR, marriage.hasHouse(), marriage.hasCar());
                        return;
                    }

                    if (marriage.isLocked()) {
                        ctx.reply("commands.pet.locked_notice", EmoteReference.ERROR);
                        return;
                    }
                }

                if (petChoice == PetChoice.PERSONAL && !player.containsItem(ItemReference.INCUBATOR_EGG)) {
                    ctx.reply("commands.pet.buy.no_egg", EmoteReference.ERROR);
                    return;
                }

                if (!player.containsItem(ItemReference.PET_HOUSE)) {
                    ctx.reply("commands.pet.buy.no_house", EmoteReference.ERROR);
                    return;
                }

                if (getCurrentPet(ctx) != null) {
                    ctx.reply("commands.pet.buy.already_has_pet", EmoteReference.ERROR, petChoice);
                    return;
                }

                if (player.isLocked()) {
                    ctx.reply("commands.pet.locked_notice", EmoteReference.ERROR);
                    return;
                }

                var toBuy = HousePetType.lookupFromString(type);
                if (toBuy == null) {
                    ctx.reply("commands.pet.buy.nothing_found", EmoteReference.ERROR, type,
                            Arrays.stream(HousePetType.values())
                                    .filter(HousePetType::isBuyable)
                                    .map(HousePetType::getName)
                                    .collect(Collectors.joining(", "))
                    );
                    return;
                }

                if (player.getCurrentMoney() < toBuy.getCost()) {
                    ctx.reply("commands.pet.buy.not_enough_money", EmoteReference.ERROR, toBuy.getCost(), player.getCurrentMoney());
                    return;
                }

                if (name.length() > 150) {
                    ctx.reply("commands.pet.buy.too_long", EmoteReference.ERROR);
                    return;
                }

                if (petChoice == PetChoice.MARRIAGE) {
                    marriage.locked(true);
                    marriage.updateAllChanged();
                }

                name = Utils.HTTP_URL.matcher(name).replaceAll("-url-");

                player.locked(true);
                player.updateAllChanged();

                var finalName = name;
                var message = ctx.sendResult(
                        String.format(ctx.getLanguageContext().get("commands.pet.buy.confirm"), EmoteReference.WARNING, name, type, toBuy.getCost(), petChoice.getReadableName())
                );
                ButtonOperations.create(message, 60, event -> {
                    if (event.getUser().getIdLong() != ctx.getAuthor().getIdLong()) {
                        return Operation.IGNORED;
                    }

                    var button = event.getButton();
                    if (button.getId() == null) {
                        return Operation.IGNORED;
                    }

                    var lang = ctx.getLanguageContext();
                    var hook = event.getHook();
                    if (button.getId().equals("yes-button")) {
                        var playerConfirmed = ctx.getPlayer();
                        var dbUserConfirmed = ctx.getDBUser();
                        var marriageConfirmed = dbUserConfirmed.getMarriage();
                        var petChoiceConfirmed = playerConfirmed.getActivePetChoice(marriageConfirmed);

                        if (petChoiceConfirmed == PetChoice.PERSONAL && !playerConfirmed.containsItem(ItemReference.INCUBATOR_EGG)) {
                            playerConfirmed.locked(false);
                            playerConfirmed.updateAllChanged();

                            hook.editOriginal(lang.get("commands.pet.buy.no_egg").formatted(EmoteReference.ERROR)).setComponents().queue();
                            return Operation.COMPLETED;
                        }

                        // People like to mess around lol.
                        if (!playerConfirmed.containsItem(ItemReference.PET_HOUSE)) {
                            playerConfirmed.locked(false);
                            playerConfirmed.updateAllChanged();

                            marriageConfirmed.locked(false);
                            marriageConfirmed.updateAllChanged();
                            hook.editOriginal(lang.get("commands.pet.buy.no_house").formatted(EmoteReference.ERROR)).setComponents().queue();
                            return Operation.COMPLETED;
                        }

                        if (playerConfirmed.getCurrentMoney() < toBuy.getCost()) {
                            playerConfirmed.locked(false);
                            playerConfirmed.updateAllChanged();

                            marriageConfirmed.locked(false);
                            marriageConfirmed.updateAllChanged();
                            hook.editOriginal(lang.get("commands.pet.buy.not_enough_money").formatted(EmoteReference.ERROR, toBuy.getCost())).setComponents().queue();
                            return Operation.COMPLETED;
                        }

                        if (petChoiceConfirmed == PetChoice.MARRIAGE) {
                            if (marriageConfirmed == null) {
                                hook.editOriginal(lang.get("commands.pet.buy.no_marriage_marry").formatted(EmoteReference.ERROR)).setComponents().queue();
                                return Operation.COMPLETED;
                            }
                            if (!marriageConfirmed.hasCar() || !marriageConfirmed.hasHouse()) {
                                playerConfirmed.locked(false);

                                hook.editOriginal(lang.get("commands.pet.buy.no_requirements").formatted(
                                        EmoteReference.ERROR, marriageConfirmed.hasHouse(), marriageConfirmed.hasCar()
                                )).setComponents().queue();
                                return Operation.COMPLETED;
                            }

                            marriageConfirmed.locked(false);
                            marriageConfirmed.pet(new HousePet(finalName, toBuy));
                            marriageConfirmed.updateAllChanged();
                        }

                        playerConfirmed.removeMoney(toBuy.getCost());
                        playerConfirmed.processItem(ItemReference.PET_HOUSE, -1);

                        if (petChoiceConfirmed == PetChoice.PERSONAL) {
                            playerConfirmed.processItem(ItemReference.INCUBATOR_EGG, -1);
                            playerConfirmed.setPet(new HousePet(finalName, toBuy));
                        }

                        if (petChoiceConfirmed == PetChoice.MARRIAGE) {
                            playerConfirmed.addBadgeIfAbsent(Badge.BEST_FRIEND_MARRY);
                        } else {
                            playerConfirmed.addBadgeIfAbsent(Badge.BEST_FRIEND);
                        }

                        playerConfirmed.locked(false);
                        playerConfirmed.markPetChange();
                        playerConfirmed.updateAllChanged();

                        if (petChoiceConfirmed == PetChoice.MARRIAGE) {
                            hook.editOriginal(lang.get("commands.pet.buy.success").formatted(
                                    EmoteReference.POPPER, toBuy.getEmoji(), toBuy.getName(), finalName,
                                    toBuy.getCost(), petChoiceConfirmed.getReadableName()
                            )).setComponents().queue();
                        } else {
                            hook.editOriginal(lang.get("commands.pet.buy.success_personal").formatted(
                                    EmoteReference.POPPER, toBuy.getEmoji(), toBuy.getName(), finalName,
                                    toBuy.getCost(), petChoiceConfirmed.getReadableName()
                            )).setComponents().queue();
                        }

                        return Operation.COMPLETED;
                    }

                    if (button.getId().equals("no-button")) {
                        var playerConfirmed = ctx.getPlayer();
                        var marriageConfirmed = ctx.getDBUser().getMarriage();
                        // Original player is fine, we checked it originally with this.
                        if (player.getActivePetChoice(marriage) == PetChoice.MARRIAGE && marriageConfirmed != null) {
                            marriageConfirmed.locked(false);
                            marriageConfirmed.updateAllChanged();
                        }

                        playerConfirmed.locked(false);
                        hook.editOriginal(lang.get("commands.pet.buy.cancel_success").formatted(EmoteReference.CORRECT)).setComponents().queue();
                        return Operation.COMPLETED;
                    }

                    return Operation.IGNORED;
                }, Button.primary("yes-button", ctx.getLanguageContext().get("buttons.yes")),
                        Button.primary("no-button", ctx.getLanguageContext().get("buttons.no"))
                );
            }
        }

        @Defer
        @Description("Renames your pet.")
        @Options(@Options.Option(type = OptionType.STRING, name = "name", description = "The new name.", required = true))
        public static class Rename extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var player = ctx.getPlayer();
                var dbUser = ctx.getDBUser();
                var marriage = dbUser.getMarriage();
                var cost = 3000;

                var pet = getCurrentPet(ctx, player, marriage, "commands.pet.rename.no_pet");
                if (pet == null) {
                    return;
                }

                var newName = ctx.getOptionAsString("name");
                newName = newName.replace("\n", "").trim();
                if (newName.isEmpty()) {
                    ctx.reply("commands.pet.rename.no_content", EmoteReference.ERROR);
                    return;
                }

                if (newName.length() > 150) {
                    ctx.reply("commands.pet.buy.too_long", EmoteReference.ERROR);
                    return;
                }

                if (newName.equals(pet.getName())) {
                    ctx.reply("commands.pet.rename.same_name", EmoteReference.ERROR);
                    return;
                }

                if (player.getCurrentMoney() < cost) {
                    ctx.reply("commands.pet.rename.not_enough_money", EmoteReference.ERROR, cost, player.getCurrentMoney());
                    return;
                }

                newName = Utils.HTTP_URL.matcher(newName).replaceAll("-url-");

                var oldName = pet.getName();
                pet.setName(newName);
                player.removeMoney(cost);

                if (player.getActivePetChoice(marriage) == PetChoice.MARRIAGE) {
                    marriage.markPetChange();
                    marriage.updateAllChanged();
                } else {
                    player.markPetChange();
                    player.updateAllChanged();
                }

                ctx.replyStripped("commands.pet.rename.success", EmoteReference.POPPER, oldName, newName, cost);
            }
        }

        @Defer
        @Description("Feeds your pet.")
        @Options({
                @Options.Option(type = OptionType.STRING, name = "item", description = "The item to feed your pet with.", required = true),
                @Options.Option(type = OptionType.INTEGER, name = "amount", description = "The amount of food to give the pet. Defaults to 1.", maxValue = 10),
                @Options.Option(type = OptionType.BOOLEAN, name = "full", description = "Give all the food possible. Makes it so amount is ignored.")
        })
        public static class Feed extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var player = ctx.getPlayer();
                var dbUser = ctx.getDBUser();
                var food = ctx.getOptionAsString("item");
                var amount = ctx.getOptionAsInteger("amount", 1);
                var marriage = dbUser.getMarriage();

                var isFull = ctx.getOptionAsBoolean("full");
                var pet = getCurrentPet(ctx, player, marriage, "commands.pet.feed.no_pet");
                if (pet == null) {
                    return;
                }

                if (food.isEmpty()) {
                    ctx.reply("commands.pet.feed.no_content", EmoteReference.ERROR);
                    return;
                }

                var item = ItemHelper.fromAnyNoId(food, ctx.getLanguageContext());
                if (item.isEmpty()) {
                    ctx.reply("commands.pet.feed.no_item", EmoteReference.ERROR);
                    return;
                }

                var itemObject = item.get();
                if (!(itemObject instanceof Food foodItem)) {
                    ctx.reply("commands.pet.feed.not_food", EmoteReference.ERROR);
                    return;
                }

                if (pet.getHunger() > 95) {
                    ctx.reply("commands.pet.feed.no_need", EmoteReference.ERROR);
                    return;
                }

                var baseline = foodItem.getHungerLevel();
                var increase = baseline * amount;

                if (isFull) {
                    amount = (100 - pet.getHunger()) / baseline;
                    if (pet.getHunger() + (baseline * amount) < 100 || amount == 0) {
                        amount += 1;
                    }

                    increase = baseline * amount;
                }

                if (amount > player.getItemAmount(itemObject)) {
                    ctx.reply("commands.pet.feed.not_inventory", EmoteReference.ERROR, amount);
                    return;
                }

                if (foodItem.getType().getApplicableType() != pet.getType() &&
                        foodItem.getType() != Food.FoodType.GENERAL) {
                    ctx.reply("commands.pet.feed.not_applicable", EmoteReference.ERROR);
                    return;
                }


                if ((pet.getHunger() + increase) > (91 + foodItem.getHungerLevel())) {
                    ctx.reply("commands.pet.feed.too_much", EmoteReference.ERROR);
                    return;
                }

                pet.increaseHunger(increase);
                pet.increaseHealth();
                pet.increaseStamina();

                player.processItem(itemObject, -amount);
                player.markPetChange();
                player.updateAllChanged();

                if (player.getActivePetChoice(marriage) == PetChoice.MARRIAGE) {
                    marriage.markPetChange();
                    marriage.updateAllChanged();
                }

                ctx.reply("commands.pet.feed.success", EmoteReference.POPPER, foodItem.getName(), amount, increase, pet.getHunger());
            }
        }

        @Defer
        @Description("Hydrates your pet.")
        @Options({
                @Options.Option(type = OptionType.INTEGER, name = "amount", description = "The amount of water to give the pet. Defaults to 1.", maxValue = 10),
                @Options.Option(type = OptionType.BOOLEAN, name = "full", description = "Give all the water possible. Makes it so amount is ignored.")
        })
        public static class Hydrate extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var player = ctx.getPlayer();
                var dbUser = ctx.getDBUser();
                var marriage = dbUser.getMarriage();
                var amount = ctx.getOptionAsInteger("amount", 1);
                var baseline = 15;

                var isFull = ctx.getOptionAsBoolean("full");
                var pet = getCurrentPet(ctx, player, marriage, "commands.pet.water.no_pet");
                if (pet == null) {
                    return;
                }

                if (pet.getThirst() > 95) {
                    ctx.reply("commands.pet.water.no_need", EmoteReference.ERROR);
                    return;
                }

                var item = ItemReference.WATER_BOTTLE;
                if (isFull) {
                    // Reassign.
                    amount = (100 - pet.getThirst()) / baseline;
                    if (pet.getThirst() + (baseline * amount) < 100 || amount == 0) {
                        amount += 1;
                    }
                }

                var increase = baseline * amount;
                if (!player.containsItem(item)) {
                    ctx.reply("commands.pet.water.not_inventory", EmoteReference.ERROR);
                    return;
                }

                if (player.getItemAmount(item) < amount) {
                    ctx.reply("commands.pet.water.not_enough_inventory", EmoteReference.ERROR, amount);
                    return;
                }

                if ((pet.getThirst() + increase) > 110) {
                    ctx.reply("commands.pet.water.too_much", EmoteReference.ERROR);
                    return;
                }

                pet.increaseThirst(increase);
                pet.increaseHealth();
                pet.increaseStamina();

                player.processItem(item, -amount);

                player.markPetChange();
                player.updateAllChanged();

                if (player.getActivePetChoice(marriage) == PetChoice.MARRIAGE) {
                    marriage.markPetChange();
                    marriage.updateAllChanged();
                }

                ctx.reply("commands.pet.water.success", EmoteReference.POPPER, amount, increase, pet.getThirst());
            }
        }

        @Description("Shows info about a pet type.")
        @Options(@Options.Option(type = OptionType.STRING, name = "type", description = "The pet type to check.", required = true))
        public static class Info extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var lookup = HousePetType.lookupFromString(ctx.getOptionAsString("type"));
                if (lookup == null) {
                    ctx.reply("commands.pet.info.not_found", EmoteReference.ERROR);
                    return;
                }

                var languageContext = ctx.getLanguageContext();
                var emoji = lookup.getEmoji();
                var name = lookup.getName();
                var cost = lookup.getCost();
                var abilities = lookup.getStringAbilities();
                var coinBuildup = lookup.getMaxCoinBuildup(1);
                var coinBuildup100 = lookup.getMaxCoinBuildup(100);
                var itemBuildup = lookup.getMaxItemBuildup(1);
                var itemBuildup100 = lookup.getMaxItemBuildup(100);
                var food = Arrays.stream(ItemReference.ALL)
                        .filter(Food.class::isInstance)
                        .map(Food.class::cast)
                        .filter(f -> (f.getType().getApplicableType() == lookup) || f.getType() == Food.FoodType.GENERAL)
                        .map(Item::toDisplayString)
                        .collect(Collectors.joining(", "));
                var baseAbilities = lookup.getAbilities().stream()
                        .filter(ability -> ability != HousePetType.HousePetAbility.CHEER).toList();

                var embed = new EmbedBuilder()
                        .setAuthor(String.format(languageContext.get("commands.pet.info.author"), emoji, name),
                                null, ctx.getAuthor().getEffectiveAvatarUrl()
                        )
                        .addField(EmoteReference.PENCIL.toHeaderString() +
                                languageContext.get("commands.pet.info.name"), name, true
                        )
                        .addField(EmoteReference.MONEY.toHeaderString() +
                                languageContext.get("commands.pet.info.cost"), "%,d credits".formatted(cost), true
                        )
                        .addField(EmoteReference.STAR.toHeaderString() +
                                languageContext.get("commands.pet.info.abilities"), abilities, false
                        )
                        .addField(EmoteReference.FORK.toHeaderString() +
                                languageContext.get("commands.pet.info.food"), food, false
                        )
                        .addField(EmoteReference.POUCH.toHeaderString() +
                                        languageContext.get("commands.pet.info.coin_buildup"),
                                "%,d credits / %,d credits".formatted(coinBuildup, coinBuildup100), false
                        )
                        .setColor(Color.PINK)
                        .setThumbnail(ctx.getAuthor().getEffectiveAvatarUrl());

                if (!baseAbilities.stream().allMatch(ability -> ability == HousePetType.HousePetAbility.CATCH)) {
                    embed.addField(EmoteReference.ZAP.toHeaderString() +
                                    languageContext.get("commands.pet.info.item_buildup"),
                            "%,d items / %,d items".formatted(itemBuildup, itemBuildup100), false);
                }

                ctx.reply(embed.build());
            }
        }
    }

    private static HousePet getCurrentPet(SlashContext ctx) {
        final var playerData = ctx.getPlayer();
        final var marriage = ctx.getMarriage(ctx.getDBUser());

        if (playerData.getActivePetChoice(marriage) == PetChoice.PERSONAL) {
            return playerData.getPet();
        } else {
            if (marriage == null) {
                return null;
            }

            return marriage.getPet();
        }
    }

    private static Pair<PetChoice, HousePet> getPetOpposite(Player player, Marriage marriage) {
        final var petChoice = player.getActivePetChoice(marriage);
        if (petChoice == PetChoice.PERSONAL) {
            if (marriage == null) {
                // Technically the opposite...
                return Pair.of(PetChoice.MARRIAGE, null);
            }

            return Pair.of(PetChoice.MARRIAGE, marriage.getPet());
        } else {
            return Pair.of(PetChoice.PERSONAL, player.getPet());
        }
    }

    private static HousePet getCurrentPet(SlashContext ctx, Player player, Marriage marriage, String missing) {
        final var petChoice = player.getActivePetChoice(marriage);
        final var languageContext = ctx.getLanguageContext();

        if (petChoice == PetChoice.PERSONAL) {
            final var personalPet = player.getPet();
            if (personalPet == null) {
                var opposite = getPetOpposite(player, marriage);
                var oppositePet = opposite.right();
                var extra = oppositePet == null ? "" :
                     languageContext.get("commands.pet.status.pet_in_other_category")
                            .formatted(EmoteReference.WARNING, Utils.capitalize(opposite.left().toString()), oppositePet.getName());

                ctx.replyStripped(missing, EmoteReference.ERROR, petChoice.getReadableName(), extra);
                return null;
            }

            return personalPet;
        } else {
            if (marriage == null) {
                ctx.reply("commands.pet.no_marriage", EmoteReference.ERROR);
                return null;
            }

            final var marriagePet = marriage.getPet();
            if (marriagePet == null) {
                var opposite = getPetOpposite(player, marriage);
                var oppositePet = opposite.right();
                var extra = oppositePet == null ? "" :
                        languageContext.get("commands.pet.status.pet_in_other_category")
                             .formatted(EmoteReference.WARNING, Utils.capitalize(opposite.left().toString()), oppositePet.getName());

                ctx.replyStripped(missing, EmoteReference.ERROR, petChoice.getReadableName(), extra);
                return null;
            }

            return marriagePet;
        }
    }
}
