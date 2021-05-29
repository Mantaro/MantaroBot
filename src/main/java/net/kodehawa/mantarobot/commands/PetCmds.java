/*
 * Copyright (C) 2016-2021 David Rubio Escares / Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemHelper;
import net.kodehawa.mantarobot.commands.currency.item.ItemReference;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.special.Food;
import net.kodehawa.mantarobot.commands.currency.pets.HousePet;
import net.kodehawa.mantarobot.commands.currency.pets.HousePetType;
import net.kodehawa.mantarobot.commands.currency.pets.PetChoice;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.Marriage;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.utils.Pair;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.CustomFinderUtil;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.ratelimit.IncreasingRateLimiter;
import net.kodehawa.mantarobot.utils.commands.ratelimit.RatelimitUtils;

import java.awt.Color;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Module
public class PetCmds {
    private final IncreasingRateLimiter rl = new IncreasingRateLimiter.Builder()
            .limit(1)
            .spamTolerance(2)
            .cooldown(3, TimeUnit.SECONDS)
            .maxCooldown(5, TimeUnit.SECONDS)
            .randomIncrement(true)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("pet")
            .build();

    private final IncreasingRateLimiter petRemoveRatelimiter = new IncreasingRateLimiter.Builder()
            .limit(1)
            .spamTolerance(2)
            .cooldown(1, TimeUnit.HOURS)
            .maxCooldown(2, TimeUnit.HOURS)
            .randomIncrement(false)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("pet-remove")
            .build();

    private final IncreasingRateLimiter petpetRatelimiter = new IncreasingRateLimiter.Builder()
            .limit(25)
            .spamTolerance(5)
            .cooldown(1, TimeUnit.HOURS)
            .maxCooldown(2, TimeUnit.HOURS)
            .randomIncrement(false)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("pet-pet")
            .build();

    private final IncreasingRateLimiter petChoiceRatelimiter = new IncreasingRateLimiter.Builder()
            .limit(3)
            .spamTolerance(5)
            .cooldown(10, TimeUnit.MINUTES)
            .maxCooldown(10, TimeUnit.MINUTES)
            .randomIncrement(false)
            .pool(MantaroData.getDefaultJedisPool())
            .prefix("pet-choice")
            .build();

    @Subscribe
    public void pet(CommandRegistry cr) {
        TreeCommand pet = cr.register("pet", new TreeCommand(CommandCategory.CURRENCY) {
            @Override
            public Command defaultTrigger(Context ctx, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, I18nContext languageContext, String content) {
                        ctx.sendLocalized("commands.pet.explanation");
                    }
                };
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription(
                            """
                            Pet commands.
                            For a better explanation of the pet system, check the [wiki](https://github.com/Mantaro/MantaroBot/wiki).
                            This command contains an explanation of what pets are. Check subcommands for the available actions.
                            """
                        )
                        .build();
            }
        });

        pet.setPredicate(ctx -> RatelimitUtils.ratelimit(rl, ctx, false));

        pet.addSubCommand("list", new SubCommand() {
            @Override
            public String description() {
                return "Lists the available pet types.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
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

                ctx.sendLocalized("commands.pet.list.header",
                        EmoteReference.TALKING, pets, EmoteReference.PENCIL, ctx.getLanguageContext().get("commands.pet.list.abilities")
                );
            }
        });

        // I swear to god
        pet.createSubCommandAlias("list", "ls");
        pet.createSubCommandAlias("list", "Is");
        pet.createSubCommandAlias("list", "1s");

        pet.addSubCommand("choice", new SubCommand() {
            @Override
            public String description() {
                return "Lets you choose whether you want to use a personal or marriage pet.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var args = ctx.getArguments();
                if (args.length < 1) {
                    ctx.sendLocalized("commands.pet.choice.no_choice", EmoteReference.ERROR);
                    return;
                }

                var choice = Utils.lookupEnumString(content, PetChoice.class);
                if (choice == null) {
                    ctx.sendLocalized("commands.pet.choice.invalid_choice", EmoteReference.ERROR);
                    return;
                }

                var player = ctx.getPlayer();
                if (choice == player.getData().getPetChoice()) {
                    ctx.sendLocalized("commands.pet.choice.already_chosen", EmoteReference.ERROR);
                    return;
                }

                if (!RatelimitUtils.ratelimit(petChoiceRatelimiter, ctx, languageContext.get("commands.pet.choice.ratelimit_message"), false))
                    return;

                player.getData().setPetChoice(choice);
                player.saveUpdating();

                ctx.sendLocalized("commands.pet.choice.success", EmoteReference.CORRECT, Utils.capitalize(choice.toString()), EmoteReference.POPPER);
            }
        }).createSubCommandAlias("choice", "select")
            .createSubCommandAlias("choice", "choose");

        pet.addSubCommand("level", new SubCommand() {
            @Override
            public String description() {
                return "Shows the level and experience of your current pet.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var dbUser = ctx.getDBUser();
                var marriage = dbUser.getData().getMarriage();
                var pet = getCurrentPet(ctx, ctx.getPlayer(), marriage, "commands.pet.level.no_pet");
                if (pet == null) {
                    return;
                }

                ctx.sendLocalized("commands.pet.level.success",
                        EmoteReference.ZAP, pet.getName(), pet.getLevel(), pet.getExperience(), pet.experienceToNextLevel()
                );
            }
        });

        pet.addSubCommand("status", new SubCommand() {
            @Override
            public String description() {
                return "Shows the status of your current pet.";
            }

            @Override
            protected void call(Context ctx, I18nContext language, String content) {
                var dbUser = ctx.getDBUser();
                var marriage = dbUser.getData().getMarriage();
                var pet = getCurrentPet(ctx, ctx.getPlayer(), marriage, "commands.pet.status.no_pet");
                if (pet == null) {
                    return;
                }

                var name = pet.getName().replace("\n", "").trim();
                var baseAbilities = pet.getType().getAbilities().stream()
                        .filter(ability -> ability != HousePetType.HousePetAbility.CHEER)
                        .collect(Collectors.toList());
                var hasItemBuildup = baseAbilities.contains(HousePetType.HousePetAbility.CATCH);

                EmbedBuilder status = new EmbedBuilder()
                        .setAuthor(String.format(language.get("commands.pet.status.header"), name), null, ctx.getUser().getEffectiveAvatarUrl())
                        .setColor(Color.PINK)
                        .setDescription(language.get("commands.pet.status.description"))
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
                        EmoteReference.BLUE_HEART.toHeaderString() + language.get("commands.pet.status.pet"),
                        "**%,d**".formatted(pet.getPatCounter()), true
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

                ctx.send(status.build());
            }
        }).createSubCommandAlias("status", "stats");

        pet.addSubCommand("check", new SubCommand() {
            @Override
            public String description() {
                return "Check thirst, hunger and dust of your current pet.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var dbUser = ctx.getDBUser();
                var marriage = dbUser.getData().getMarriage();
                var player = ctx.getPlayer();
                var pet = getCurrentPet(ctx, player, marriage, "commands.pet.status.no_pet");
                if (pet == null) {
                    return;
                }

                ctx.sendLocalized("commands.pet.check.success",
                        pet.getName(), EmoteReference.DROPLET, pet.getThirst(),
                        EmoteReference.FORK, pet.getHunger(), EmoteReference.DUST, pet.getDust()
                );
            }
        });

        pet.addSubCommand("sell", new SubCommand() {
            @Override
            public String description() {
                return "Sells this pet. This will *reset all pet stats*. Just like buying a new tamagotchi.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var dbUser = ctx.getDBUser();
                var marriage = dbUser.getData().getMarriage();
                var player = ctx.getPlayer();
                var pet = getCurrentPet(ctx, player, marriage, "commands.pet.remove.no_pet");
                if (pet == null) {
                    return;
                }

                if (player.isLocked()) {
                    ctx.sendLocalized("commands.pet.locked_notice", EmoteReference.ERROR);
                    return;
                }

                if (!RatelimitUtils.ratelimit(petRemoveRatelimiter, ctx, false))
                    return;

                var toRefund = (long) ((pet.getType().getCost() / 2) * 0.9);
                var toRefundPersonal = (long) (pet.getType().getCost() * 0.9);

                if (player.getData().getActiveChoice(marriage) == PetChoice.MARRIAGE) {
                    if (marriage.isLocked()) {
                        ctx.sendLocalized("commands.pet.locked_notice", EmoteReference.ERROR);
                        return;
                    }

                    ctx.sendLocalized("commands.pet.remove.confirm", EmoteReference.WARNING, toRefund);

                    marriage.setLocked(true);
                    marriage.save();
                } else {
                    ctx.sendLocalized("commands.pet.remove.confirm_personal", EmoteReference.WARNING, toRefundPersonal);
                }

                player.setLocked(true);
                player.save();

                InteractiveOperations.create(ctx.getChannel(), ctx.getAuthor().getIdLong(), 50, (e) -> {
                    if (!e.getAuthor().equals(ctx.getAuthor()))
                        return Operation.IGNORED;

                    if (e.getMessage().getContentRaw().equalsIgnoreCase("yes")) {
                        final var playerFinal = ctx.getPlayer();
                        final var marriageConfirmed = ctx.getDBUser().getData().getMarriage();

                        var petFinal = getCurrentPet(ctx, playerFinal, marriageConfirmed, "commands.pet.remove.no_pet");
                        if (petFinal == null) {
                            return Operation.COMPLETED;
                        }

                        var toRefundFinal = (long) ((petFinal.getType().getCost() / 2) * 0.9);
                        var toRefundPersonalFinal = (long) (petFinal.getType().getCost() * 0.9);

                        if (playerFinal.getData().getActiveChoice(marriageConfirmed) == PetChoice.MARRIAGE) {
                            if (marriageConfirmed == null) {
                                ctx.sendLocalized("commands.pet.buy.no_marriage_marry", EmoteReference.ERROR);
                                return Operation.COMPLETED;
                            }

                            final var marriedWithPlayer = ctx.getPlayer(marriageConfirmed.getOtherPlayer(ctx.getAuthor().getId()));
                            if (marriageConfirmed.getData().getPet() == null) {
                                ctx.sendLocalized("commands.pet.remove.no_pet_confirm", EmoteReference.ERROR);
                                return Operation.COMPLETED;
                            }

                            marriageConfirmed.getData().setPet(null);
                            marriageConfirmed.setLocked(false);
                            marriageConfirmed.save();

                            marriedWithPlayer.addMoney(toRefundFinal);
                            playerFinal.addMoney(toRefundFinal);
                            playerFinal.setLocked(false);

                            playerFinal.save();
                            marriedWithPlayer.save();
                            ctx.sendLocalized("commands.pet.remove.success", EmoteReference.CORRECT, toRefundFinal);
                        } else {
                            var playerData = playerFinal.getData();
                            if (playerData.getPet() == null) {
                                ctx.sendLocalized("commands.pet.remove.no_pet_confirm", EmoteReference.ERROR);
                                return Operation.COMPLETED;
                            }

                            playerData.setPet(null);
                            playerFinal.addMoney(toRefundPersonalFinal);
                            playerFinal.setLocked(false);
                            playerFinal.save();
                            ctx.sendLocalized("commands.pet.remove.success_personal", EmoteReference.CORRECT, toRefundPersonalFinal);
                        }

                        return Operation.COMPLETED;
                    }

                    if (e.getMessage().getContentRaw().equalsIgnoreCase("no")) {
                        var marriageConfirmed = ctx.getDBUser().getData().getMarriage();
                        var playerFinal = ctx.getPlayer();
                        playerFinal.setLocked(false);
                        playerFinal.save();

                        marriageConfirmed.setLocked(false);
                        marriageConfirmed.save();

                        // This is reusing the string, nothing wrong here.
                        ctx.sendLocalized("commands.pet.buy.cancel_success", EmoteReference.CORRECT);
                        return Operation.COMPLETED;
                    }

                    return Operation.IGNORED;
                });

            }
        }).createSubCommandAlias("sell", "remove");

        pet.addSubCommand("pet", new SubCommand() {
            @Override
            public String description() {
                return "Pets your pet or someone else's pet. Usage: `~>pet pet [user]`. Cute.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                ctx.findMember(content, members -> {
                    var member = CustomFinderUtil.findMemberDefault(content, members, ctx, ctx.getMember());
                    if (member == null) {
                        return;
                    }

                    var dbUser = ctx.getDBUser(member);
                    var player = ctx.getPlayer(member);
                    var playerData = player.getData();
                    var marriage = dbUser.getData().getMarriage();
                    var isCallerPetOwner = member.getId().equals(ctx.getUser().getId()) ||
                        (marriage != null && member.getId().equals(marriage.getOtherPlayer(ctx.getUser().getId())));
                    var pet = playerData.getPet();
                    var choice = playerData.getActiveChoice(marriage);

                    if (choice == PetChoice.MARRIAGE) {
                        if (marriage == null) {
                            if (isCallerPetOwner) {
                                ctx.sendLocalized("commands.pet.no_marriage", EmoteReference.ERROR);
                            } else {
                                ctx.sendLocalized("commands.pet.no_marriage_other", EmoteReference.ERROR, member.getEffectiveName());
                            }

                            return;
                        }

                        pet = marriage.getData().getPet();
                    }

                    if (!isCallerPetOwner && choice == PetChoice.PERSONAL) {
                        ctx.sendLocalized("commands.pet.pat.personal_pet_other", EmoteReference.ERROR);
                        return;
                    }

                    if (pet == null) {
                        if (isCallerPetOwner) {
                            ctx.sendLocalized("commands.pet.pat.no_pet", EmoteReference.ERROR, choice.getReadableName());
                        } else {
                            ctx.sendLocalized("commands.pet.pat.no_pet_other", EmoteReference.ERROR, member.getEffectiveName());
                        }

                        return;
                    }

                    if (!RatelimitUtils.ratelimit(petpetRatelimiter, ctx, languageContext.get("commands.pet.pat.ratelimit_message"), false))
                        return;

                    var message = pet.handlePat().getMessage();
                    var extraMessage = "";
                    pet.increasePats();

                    if (pet.getPatCounter() > 50_000_000) { // how?
                        ctx.sendLocalized("commands.pet.pat.too_many");
                        return;
                    }

                    if (pet.getPatCounter() % 100 == 0) {
                        extraMessage += "\n\n" + String.format(ctx.getLanguageContext().get("commands.pet.pet_reactions.counter_100"), EmoteReference.BLUE_HEART);
                    }

                    if (choice == PetChoice.MARRIAGE) {
                        marriage.saveUpdating();
                    } else {
                        player.saveUpdating();
                    }

                    ctx.sendLocalized(message, pet.getType().getEmoji(), pet.getName(), pet.getPatCounter(), extraMessage);
                });
            }
        });

        pet.addSubCommand("clean", new SubCommand() {
            final long basePrice = 600L;

            @Override
            public String description() {
                return "Cleans your pet when it's too dusty. Costs %s credits.".formatted(basePrice);
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var player = ctx.getPlayer();
                var dbUser = ctx.getDBUser();
                var marriage = dbUser.getData().getMarriage();
                var price = basePrice;

                var pet = getCurrentPet(ctx, player, marriage, "commands.pet.status.no_pet");
                if (pet == null) {
                    return;
                }

                if (pet.getDust() <= 50) {
                    price = price / 2;
                }

                if (player.getCurrentMoney() < price) {
                    ctx.sendLocalized("commands.pet.clean.not_enough_money", EmoteReference.ERROR, price, pet.getName());
                    return;
                }

                if (pet.getDust() < 20) {
                    ctx.sendLocalized("commands.pet.clean.not_dusty", EmoteReference.ERROR, pet.getName(), pet.getDust());
                    return;
                }

                pet.setDust(0);
                player.removeMoney(price);
                player.saveUpdating();
                if (player.getData().getActiveChoice(marriage) == PetChoice.MARRIAGE) {
                    marriage.saveUpdating();
                }

                ctx.sendLocalized("commands.pet.clean.success", EmoteReference.CORRECT, pet.getName(), price);
            }
        });

        pet.addSubCommand("buy", new SubCommand() {
            @Override
            public String description() {
                return "Buys a pet to have adventures with. You need to buy a pet house in market first. " +
                       "Usage: `~>pet buy <name> <type>`";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var player = ctx.getPlayer();
                var playerData = player.getData();
                var playerInventory = player.getInventory();
                var dbUser = ctx.getDBUser();
                var marriage = dbUser.getData().getMarriage();
                var args = ctx.getArguments();

                if (args.length < 2) {
                    ctx.sendLocalized("commands.pet.buy.not_enough_arguments", EmoteReference.ERROR);
                    return;
                }
                var name = args[0].replace("\n", "").trim();
                var type = args[1];

                final var petChoice = playerData.getActiveChoice(marriage);
                if (petChoice == PetChoice.MARRIAGE) {
                    if (marriage == null) {
                        ctx.sendLocalized("commands.pet.no_marriage", EmoteReference.ERROR);
                        return;
                    }

                    var marriageData = marriage.getData();
                    if (!marriageData.hasCar() || !marriageData.hasHouse()) {
                        ctx.sendLocalized("commands.pet.buy.no_requirements", EmoteReference.ERROR, marriageData.hasHouse(), marriageData.hasCar());
                        return;
                    }

                    if (marriage.isLocked()) {
                        ctx.sendLocalized("commands.pet.locked_notice", EmoteReference.ERROR);
                        return;
                    }
                }

                if (petChoice == PetChoice.PERSONAL && !playerInventory.containsItem(ItemReference.INCUBATOR_EGG)) {
                    ctx.sendLocalized("commands.pet.buy.no_egg", EmoteReference.ERROR);
                    return;
                }

                if (!playerInventory.containsItem(ItemReference.PET_HOUSE)) {
                    ctx.sendLocalized("commands.pet.buy.no_house", EmoteReference.ERROR);
                    return;
                }

                if (getCurrentPet(ctx) != null) {
                    ctx.sendLocalized("commands.pet.buy.already_has_pet", EmoteReference.ERROR, petChoice);
                    return;
                }

                if (player.isLocked()) {
                    ctx.sendLocalized("commands.pet.locked_notice", EmoteReference.ERROR);
                    return;
                }

                var toBuy = HousePetType.lookupFromString(type);
                if (toBuy == null) {
                    ctx.sendLocalized("commands.pet.buy.nothing_found", EmoteReference.ERROR, type,
                            Arrays.stream(HousePetType.values())
                                    .filter(HousePetType::isBuyable)
                                    .map(HousePetType::getName)
                                    .collect(Collectors.joining(", "))
                    );
                    return;
                }

                if (player.getCurrentMoney() < toBuy.getCost()) {
                    ctx.sendLocalized("commands.pet.buy.not_enough_money", EmoteReference.ERROR, toBuy.getCost(), player.getCurrentMoney());
                    return;
                }

                if (name.length() > 150) {
                    ctx.sendLocalized("commands.pet.buy.too_long", EmoteReference.ERROR);
                    return;
                }

                if (petChoice == PetChoice.MARRIAGE) {
                    marriage.setLocked(true);
                    marriage.save();
                }

                name = Utils.HTTP_URL.matcher(name).replaceAll("-url-");

                player.setLocked(true);
                player.saveUpdating();

                var finalName = name;
                ctx.sendLocalized("commands.pet.buy.confirm", EmoteReference.WARNING, name, type, toBuy.getCost(), petChoice.getReadableName());
                InteractiveOperations.create(ctx.getChannel(), ctx.getAuthor().getIdLong(), 30, (e) -> {
                    if (!e.getAuthor().equals(ctx.getAuthor())) {
                        return Operation.IGNORED;
                    }

                    if (e.getMessage().getContentRaw().equalsIgnoreCase("yes")) {
                        var playerConfirmed = ctx.getPlayer();
                        var playerInventoryConfirmed = playerConfirmed.getInventory();
                        var playerDataConfirmed = playerConfirmed.getData();
                        var dbUserConfirmed = ctx.getDBUser();
                        var marriageConfirmed = dbUserConfirmed.getData().getMarriage();
                        var petChoiceConfirmed = playerDataConfirmed.getActiveChoice(marriageConfirmed);

                        if (petChoiceConfirmed == PetChoice.PERSONAL && !playerInventoryConfirmed.containsItem(ItemReference.INCUBATOR_EGG)) {
                            playerConfirmed.setLocked(false);
                            playerConfirmed.saveUpdating();

                            ctx.sendLocalized("commands.pet.buy.no_egg", EmoteReference.ERROR);
                            return Operation.COMPLETED;
                        }

                        // People like to mess around lol.
                        if (!playerInventoryConfirmed.containsItem(ItemReference.PET_HOUSE)) {
                            playerConfirmed.setLocked(false);
                            playerConfirmed.saveUpdating();

                            marriageConfirmed.setLocked(false);
                            marriageConfirmed.saveUpdating();

                            ctx.sendLocalized("commands.pet.buy.no_house", EmoteReference.ERROR);
                            return Operation.COMPLETED;
                        }

                        if (playerConfirmed.getCurrentMoney() < toBuy.getCost()) {
                            playerConfirmed.setLocked(false);
                            playerConfirmed.saveUpdating();

                            marriageConfirmed.setLocked(false);
                            marriageConfirmed.saveUpdating();

                            ctx.sendLocalized("commands.pet.buy.not_enough_money", EmoteReference.ERROR, toBuy.getCost());
                            return Operation.COMPLETED;
                        }

                        if (petChoiceConfirmed == PetChoice.MARRIAGE) {
                            if (marriageConfirmed == null) {
                                ctx.sendLocalized("commands.pet.buy.no_marriage_marry", EmoteReference.ERROR);
                                return Operation.COMPLETED;
                            }
                            var marriageDataConfirmed = marriageConfirmed.getData();
                            if (!marriageDataConfirmed.hasCar() || !marriageDataConfirmed.hasHouse()) {
                                playerConfirmed.setLocked(false);
                                playerConfirmed.saveUpdating();

                                ctx.sendLocalized("commands.pet.buy.no_requirements",
                                        EmoteReference.ERROR, marriageDataConfirmed.hasHouse(), marriageDataConfirmed.hasCar()
                                );
                                return Operation.COMPLETED;
                            }

                            marriageConfirmed.setLocked(false);
                            marriageDataConfirmed.setPet(new HousePet(finalName, toBuy));
                            marriageConfirmed.save();
                        }

                        playerConfirmed.removeMoney(toBuy.getCost());
                        playerInventoryConfirmed.process(new ItemStack(ItemReference.PET_HOUSE, -1));

                        if (petChoiceConfirmed == PetChoice.PERSONAL) {
                            playerInventoryConfirmed.process(new ItemStack(ItemReference.INCUBATOR_EGG, -1));
                            playerDataConfirmed.setPet(new HousePet(finalName, toBuy));
                        }

                        if (petChoiceConfirmed == PetChoice.MARRIAGE) {
                            playerDataConfirmed.addBadgeIfAbsent(Badge.BEST_FRIEND_MARRY);
                        } else {
                            playerDataConfirmed.addBadgeIfAbsent(Badge.BEST_FRIEND);
                        }

                        playerConfirmed.setLocked(false);
                        playerConfirmed.save();

                        if (petChoiceConfirmed == PetChoice.MARRIAGE) {
                            ctx.sendLocalized("commands.pet.buy.success",
                                    EmoteReference.POPPER, toBuy.getEmoji(), toBuy.getName(), finalName,
                                    toBuy.getCost(), petChoiceConfirmed.getReadableName()
                            );
                        } else {
                            ctx.sendLocalized("commands.pet.buy.success_personal",
                                    EmoteReference.POPPER, toBuy.getEmoji(), toBuy.getName(), finalName,
                                    toBuy.getCost(), petChoiceConfirmed.getReadableName()
                            );
                        }

                        return Operation.COMPLETED;
                    }

                    if (e.getMessage().getContentRaw().equalsIgnoreCase("no")) {
                        var playerConfirmed = ctx.getPlayer();
                        var marriageConfirmed = ctx.getDBUser().getData().getMarriage();

                        marriageConfirmed.setLocked(false);
                        marriageConfirmed.save();

                        playerConfirmed.setLocked(false);
                        playerConfirmed.save();

                        ctx.sendLocalized("commands.pet.buy.cancel_success", EmoteReference.CORRECT);
                        return Operation.COMPLETED;
                    }

                    return Operation.IGNORED;
                });
            }
        });

        pet.addSubCommand("rename", new SubCommand() {
            @Override
            public String description() {
                return "Renames your pet. Usage: `~>pet rename <name>`";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var player = ctx.getPlayer();
                var dbUser = ctx.getDBUser();
                var marriage = dbUser.getData().getMarriage();
                var cost = 3000;

                var pet = getCurrentPet(ctx, player, marriage, "commands.pet.rename.no_pet");
                if (pet == null) {
                    return;
                }

                content = content.replace("\n", "").trim();
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.pet.rename.no_content", EmoteReference.ERROR);
                    return;
                }

                if (content.length() > 150) {
                    ctx.sendLocalized("commands.pet.buy.too_long", EmoteReference.ERROR);
                    return;
                }

                if (content.equals(pet.getName())) {
                    ctx.sendLocalized("commands.pet.rename.same_name", EmoteReference.ERROR);
                    return;
                }

                if (player.getCurrentMoney() < cost) {
                    ctx.sendLocalized("commands.pet.rename.not_enough_money", EmoteReference.ERROR, cost, player.getCurrentMoney());
                    return;
                }

                content = Utils.HTTP_URL.matcher(content).replaceAll("-url-");

                var oldName = pet.getName();
                pet.setName(content);
                player.removeMoney(cost);

                if (player.getData().getActiveChoice(marriage) == PetChoice.MARRIAGE) {
                    marriage.saveUpdating();
                } else {
                    player.saveUpdating();
                }

                ctx.sendLocalized("commands.pet.rename.success", EmoteReference.POPPER, oldName, content, cost);
            }
        });

        pet.addSubCommand("feed", new SubCommand() {
            @Override
            public String description() {
                return "Feeds your pet. Needed food may vary per pet. Usage: `~>pet feed <food> [amount]`";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var player = ctx.getPlayer();
                var playerInventory = player.getInventory();
                var dbUser = ctx.getDBUser();
                var args = ctx.getArguments();
                var food = content;
                var amount = 1;
                var marriage = dbUser.getData().getMarriage();

                var isFull = false;
                if (args.length > 1) {
                    if (args[1].equalsIgnoreCase("full")) {
                        food = args[0];
                        isFull = true;
                    } else {
                        try {
                            amount = Math.abs(Integer.parseInt(args[1]));
                            food = args[0];
                        } catch (Exception ignored) {
                            food = content;
                        }
                    }
                }

                var pet = getCurrentPet(ctx, player, marriage, "commands.pet.feed.no_pet");
                if (pet == null) {
                    return;
                }

                if (food.isEmpty()) {
                    ctx.sendLocalized("commands.pet.feed.no_content", EmoteReference.ERROR);
                    return;
                }

                var item = ItemHelper.fromAnyNoId(food, ctx.getLanguageContext());
                if (item.isEmpty()) {
                    ctx.sendLocalized("commands.pet.feed.no_item", EmoteReference.ERROR);
                    return;
                }

                var itemObject = item.get();
                if (!(itemObject instanceof Food)) {
                    ctx.sendLocalized("commands.pet.feed.not_food", EmoteReference.ERROR);
                    return;
                }

                if (pet.getHunger() > 95) {
                    ctx.sendLocalized("commands.pet.feed.no_need", EmoteReference.ERROR);
                    return;
                }

                var foodItem = (Food) itemObject;
                var baseline = foodItem.getHungerLevel();
                var increase = baseline * amount;

                if (isFull) {
                    amount = (100 - pet.getHunger()) / baseline;
                    if (pet.getHunger() + (baseline * amount) < 100 || amount == 0) {
                        amount += 1;
                    }

                    increase = baseline * amount;
                }

                if (amount > playerInventory.getAmount(itemObject)) {
                    ctx.sendLocalized("commands.pet.feed.not_inventory", EmoteReference.ERROR, amount);
                    return;
                }

                if (foodItem.getType().getApplicableType() != pet.getType() &&
                        foodItem.getType() != Food.FoodType.GENERAL) {
                    ctx.sendLocalized("commands.pet.feed.not_applicable", EmoteReference.ERROR);
                    return;
                }


                if ((pet.getHunger() + increase) > (91 + foodItem.getHungerLevel())) {
                    ctx.sendLocalized("commands.pet.feed.too_much", EmoteReference.ERROR);
                    return;
                }

                pet.increaseHunger(increase);
                pet.increaseHealth();
                pet.increaseStamina();

                playerInventory.process(new ItemStack(itemObject, -amount));
                player.save();

                if (player.getData().getActiveChoice(marriage) == PetChoice.MARRIAGE) {
                    marriage.saveUpdating();
                }

                ctx.sendLocalized("commands.pet.feed.success", EmoteReference.POPPER, foodItem.getName(), amount, increase, pet.getHunger());
            }
        });

        pet.addSubCommand("hydrate", new SubCommand() {
            @Override
            public String description() {
                return "Hydrates your pet. Usage: `~>pet hydrate [amount]`";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var player = ctx.getPlayer();
                var playerInventory = player.getInventory();
                var dbUser = ctx.getDBUser();
                var marriage = dbUser.getData().getMarriage();
                int amount = 1;
                var baseline = 15;

                var isFull = false;
                if (!content.isEmpty()) {
                    if (content.equalsIgnoreCase("full")) {
                        isFull = true;
                    } else {
                        try {
                            amount = Math.abs(Integer.parseInt(content));
                        } catch (Exception ignored) { }
                    }
                }

                var pet = getCurrentPet(ctx, player, marriage, "commands.pet.water.no_pet");
                if (pet == null) {
                    return;
                }

                if (pet.getThirst() > 95) {
                    ctx.sendLocalized("commands.pet.water.no_need", EmoteReference.ERROR);
                    return;
                }

                var item = ItemReference.WATER_BOTTLE;

                if (isFull) {
                    amount = (100 - pet.getThirst()) / baseline;
                    if (pet.getThirst() + (baseline * amount) < 100 || amount == 0) {
                        amount += 1;
                    }
                }

                var increase = baseline * amount;
                if (!playerInventory.containsItem(item)) {
                    ctx.sendLocalized("commands.pet.water.not_inventory", EmoteReference.ERROR);
                    return;
                }

                if (playerInventory.getAmount(item) < amount) {
                    ctx.sendLocalized("commands.pet.water.not_enough_inventory", EmoteReference.ERROR, amount);
                    return;
                }

                if ((pet.getThirst() + increase) > 110) {
                    ctx.sendLocalized("commands.pet.water.too_much", EmoteReference.ERROR);
                    return;
                }

                pet.increaseThirst(increase);
                pet.increaseHealth();
                pet.increaseStamina();

                playerInventory.process(new ItemStack(item, -amount));

                player.save();
                if (player.getData().getActiveChoice(marriage) == PetChoice.MARRIAGE) {
                    marriage.saveUpdating();
                }

                ctx.sendLocalized("commands.pet.water.success", EmoteReference.POPPER, amount, increase, pet.getThirst());
            }
        });

        // I guess...
        pet.createSubCommandAlias("hydrate", "water");

        pet.addSubCommand("info", new SubCommand() {
            @Override
            public String description() {
                return "Shows info about a pet type.";
            }

            @Override
            protected void call(Context ctx, I18nContext languageContext, String content) {
                var lookup = HousePetType.lookupFromString(content);
                if (lookup == null) {
                    ctx.sendLocalized("commands.pet.info.not_found", EmoteReference.ERROR);
                    return;
                }

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
                        .filter(ability -> ability != HousePetType.HousePetAbility.CHEER)
                        .collect(Collectors.toList());

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

                ctx.send(embed.build());
            }
        });
    }

    private HousePet getCurrentPet(Context ctx) {
        final var playerData = ctx.getPlayer().getData();
        final var marriage = ctx.getMarriage(ctx.getDBUser().getData());

        if (playerData.getActiveChoice(marriage) == PetChoice.PERSONAL) {
            return playerData.getPet();
        } else {
            if (marriage == null) {
                return null;
            }

            return marriage.getData().getPet();
        }
    }

    private Pair<PetChoice, HousePet> getPetOpposite(Player player, Marriage marriage) {
        final var playerData = player.getData();
        final var petChoice = playerData.getActiveChoice(marriage);
        if (petChoice == PetChoice.PERSONAL) {
            if (marriage == null) {
                // Technically the opposite...
                return Pair.of(PetChoice.MARRIAGE, null);
            }

            final var marriageData = marriage.getData();
            return Pair.of(PetChoice.MARRIAGE, marriageData.getPet());
        } else {
            return Pair.of(PetChoice.PERSONAL, playerData.getPet());
        }
    }

    private HousePet getCurrentPet(Context ctx, Player player, Marriage marriage, String missing) {
        final var playerData = player.getData();
        final var petChoice = playerData.getActiveChoice(marriage);
        final var languageContext = ctx.getLanguageContext();

        if (petChoice == PetChoice.PERSONAL) {
            final var personalPet = playerData.getPet();
            if (personalPet == null) {
                var opposite = getPetOpposite(player, marriage);
                var oppositePet = opposite.getRight();
                var extra = oppositePet == null ? "" :
                     languageContext.get("commands.pet.status.pet_in_other_category")
                            .formatted(EmoteReference.WARNING, Utils.capitalize(opposite.getLeft().toString()), oppositePet.getName());

                ctx.sendLocalized(missing, EmoteReference.ERROR, petChoice.getReadableName(), extra);
                return null;
            }

            return personalPet;
        } else {
            if (marriage == null) {
                ctx.sendLocalized("commands.pet.no_marriage", EmoteReference.ERROR);
                return null;
            }

            final var marriageData = marriage.getData();
            final var marriagePet = marriageData.getPet();
            if (marriagePet == null) {
                var opposite = getPetOpposite(player, marriage);
                var oppositePet = opposite.getRight();
                var extra = oppositePet == null ? "" :
                        languageContext.get("commands.pet.status.pet_in_other_category")
                             .formatted(EmoteReference.WARNING, Utils.capitalize(opposite.getLeft().toString()), oppositePet.getName());

                ctx.sendLocalized(missing, EmoteReference.ERROR, petChoice.getReadableName(), extra);
                return null;
            }

            return marriagePet;
        }
    }
}
