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

package net.kodehawa.mantarobot;

import com.google.common.eventbus.Subscribe;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.commands.currency.pets.Pet;
import net.kodehawa.mantarobot.commands.currency.pets.PetStats;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.PlayerData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Module
public class PetCmds {
    private final static String BLOCK_INACTIVE = "\u25AC";
    private final static String BLOCK_ACTIVE = "\uD83D\uDD18";
    private static final int TOTAL_BLOCKS = 10;

    @Subscribe
    public void pet(CommandRegistry cr) {
        SimpleCommand petCommand = (SimpleCommand) cr.register("pet", new SimpleCommand(Category.PETS) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                ManagedDatabase db = MantaroData.db();
                List<Member> found = FinderUtil.findMembers(content, event.getGuild());
                String userId;
                String petName;

                if(content.isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.pet.no_content"), EmoteReference.ERROR).queue();
                    return;
                }

                //We only want one result, don't we?
                if(found.size() > 1) {
                    event.getChannel().sendMessageFormat(languageContext.get("general.too_many_members"), EmoteReference.THINKING, found.stream().limit(7).map(m -> String.format("%s#%s", m.getUser().getName(), m.getUser().getDiscriminator())).collect(Collectors.joining(", "))).queue();
                    return;
                }

                if(found.isEmpty()) {
                    userId = event.getAuthor().getId();
                    petName = content;
                } else {
                    userId = found.get(0).getUser().getId();
                    petName = args[1];
                }


                if(petName.isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.pet.not_specified"), EmoteReference.ERROR).queue();
                    return;
                }

                Player player = db.getPlayer(userId);
                PlayerData playerData = player.getData();

                Pet pet = playerData.getProfilePets().get(petName.toLowerCase());
                if(pet == null) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.pet.not_found"), EmoteReference.ERROR).queue();
                    return;
                }

                final PetStats stats = pet.getStats();

                //This is a placeholder to test stuff. Mostly how it'll look on release though.
                event.getChannel().sendMessage(
                        new EmbedBuilder()
                                .setTitle(String.format("%s Pet Overview and Statistics", pet.getName()))
                                .setThumbnail(pet.getImage().getImage())
                                .setDescription(
                                        Utils.prettyDisplay("Tier", String.valueOf(pet.calculateTier())) + "\n" +
                                        // ------ Change to translatable when I have the translation tables ready for this
                                        Utils.prettyDisplay("Element", pet.getElement().getReadable()) + "\n" +
                                        Utils.prettyDisplay("Owner", MantaroBot.getInstance().getUserById(pet.getOwner()).getAsTag())  + "\n" +
                                        Utils.prettyDisplay("Created At", String.valueOf(pet.getEpochCreatedAt()))
                                )
                                .addField("Current HP", getProgressBar(stats.getCurrentHP(), stats.getHp()) + String.format(" (%s/%s)", stats.getCurrentHP(), stats.getHp()), false)
                                .addField("Current Stamina", getProgressBar(stats.getCurrentStamina(), stats.getStamina()) + String.format(" (%s/%s)", stats.getCurrentStamina(), stats.getStamina()), false)
                                .addField("Fly", String.valueOf(pet.getStats().isFly()), true)
                                .addField("Venom", String.valueOf(pet.getStats().isVenom()), true)
                                .addField("Inventory", pet.getData().getInventory().toString(), false)
                                .setColor(pet.getData().getColor())
                                .build()
                ).queue();
            }
        });

        cr.registerAlias("pet", "petstats");
    }

    @Subscribe
    public void petActions(CommandRegistry cr) {
        TreeCommand petActionCommand = (TreeCommand) cr.register("petactions", new TreeCommand(Category.PETS) {
            @Override
            public Command defaultTrigger(GuildMessageReceivedEvent event, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                        //List all what you can do with a pet here...
                    }
                };
            }
        });

        //Incubate new pet.
        petActionCommand.addSubCommand("incubate", new SubCommand() {
            SecureRandom random = new SecureRandom();

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                ManagedDatabase managedDatabase = MantaroData.db();
                Player player = managedDatabase.getPlayer(event.getAuthor());
                PlayerData playerData = player.getData();

                String name;

                if(content.isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.petactions.incubate.no_name"), EmoteReference.ERROR).queue();
                    return;
                }

                if(!content.matches("^[a-z]+$")) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.petactions.incubate.only_letters"), EmoteReference.ERROR).queue();
                    return;
                }

                name = content;

                if(!player.getInventory().containsItem(Items.INCUBATOR_EGG)) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.petactions.incubate.no_egg"), EmoteReference.ERROR).queue();
                    return;
                }

                if(playerData.getPetSlots() < playerData.getProfilePets().size() + 1) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.petactions.incubate.not_enough_slots"), EmoteReference.ERROR, playerData.getPetSlots(), playerData.getProfilePets().size()).queue();
                    return;
                }

                long moneyNeeded = Math.max(70, random.nextInt(1000));
                if(player.getMoney() < moneyNeeded) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.petactions.incubate.no_money"), EmoteReference.ERROR, moneyNeeded, player.getMoney()).queue();
                    return;
                }

                Pet pet = generatePet(event.getAuthor().getId(), name);
                playerData.getProfilePets().put(name, pet);

                event.getChannel().sendMessageFormat(languageContext.get("commands.petactions.incubate.success"), EmoteReference.POPPER).queue();
            }
        });

        //List your pets.
        petActionCommand.addSubCommand("ls", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {

            }
        });

        //Yes. Exactly. Can increase affection. Don't overdo it though!
        petActionCommand.addSubCommand("pet", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {

            }
        });

        //Apply effect to pet.
        petActionCommand.addSubCommand("effect", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {

            }
        });

        //Upgrade stats: requires materials and luck.
        petActionCommand.addSubCommand("upgrade", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {

            }
        });

        //Not aplicable to fire-type pets.
        petActionCommand.addSubCommand("feed", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {

            }
        });

        //Only for water-type pets.
        petActionCommand.addSubCommand("hydrate", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {

            }
        });
    }

    @Subscribe
    public void battle(CommandRegistry cr) {
        cr.register("battle", new SimpleCommand(Category.PETS) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {

            }
        });
    }

    private static String getProgressBar(long now, long total) {
        int activeBlocks = (int) ((float) now / total * TOTAL_BLOCKS);
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < TOTAL_BLOCKS; i++)
            builder.append(activeBlocks == i ? BLOCK_ACTIVE : BLOCK_INACTIVE);

        return builder.append(BLOCK_INACTIVE).toString();
    }

    private Pet generatePet(String owner, String name) {
        SecureRandom random = new SecureRandom();

        //Get random element.
        Pet pet = Pet.create(owner, name, PetStats.Type.values()[random.nextInt(PetStats.Type.values().length)]);
        PetStats petStats = pet.getStats();

        petStats.setHp(Math.max(20, random.nextInt(150)));
        petStats.setStamina(Math.max(20, random.nextInt(140)));
        petStats.setAffection(Math.max(15, random.nextInt(100)));

        //Can't have both a venom-type and fly-type pet: would be broken
        petStats.setVenom(random.nextBoolean());
        petStats.setFly(!petStats.isVenom() && random.nextBoolean());

        return pet;
    }
}
