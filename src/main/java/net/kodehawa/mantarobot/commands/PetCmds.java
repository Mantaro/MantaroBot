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

import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.item.Item;
import net.kodehawa.mantarobot.commands.currency.item.ItemStack;
import net.kodehawa.mantarobot.commands.currency.item.ItemType;
import net.kodehawa.mantarobot.commands.currency.item.Items;
import net.kodehawa.mantarobot.commands.currency.item.special.Food;
import net.kodehawa.mantarobot.commands.currency.pets.Pet;
import net.kodehawa.mantarobot.commands.currency.pets.PetData;
import net.kodehawa.mantarobot.commands.currency.pets.PetStats;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.core.listeners.operations.core.Operation;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import net.kodehawa.mantarobot.db.entities.helpers.PlayerData;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.IncreasingRateLimiter;

import java.awt.*;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static net.kodehawa.mantarobot.commands.CurrencyCmds.applyPotionEffect;
import static net.kodehawa.mantarobot.commands.CustomCmds.NAME_PATTERN;

@Module
public class PetCmds {
    private final static String BLOCK_INACTIVE = "\u25A1";
    private final static String BLOCK_ACTIVE = "\u25A0";
    private static final int TOTAL_BLOCKS = 5;
    
    private static String getProgressBar(long now, long total) {
        int activeBlocks = (int) ((float) now / total * TOTAL_BLOCKS);
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < TOTAL_BLOCKS; i++)
            builder.append(activeBlocks == i ? BLOCK_ACTIVE : BLOCK_INACTIVE);
        
        return builder.append(BLOCK_INACTIVE).toString();
    }
    
    //@Subscribe
    public void petAction(CommandRegistry cr) {
        IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                                                    .cooldown(20, TimeUnit.SECONDS)
                                                    .limit(1)
                                                    .maxCooldown(5, TimeUnit.MINUTES)
                                                    .premiumAware(false)
                                                    .prefix("petaction")
                                                    .pool(MantaroData.getDefaultJedisPool())
                                                    .build();
        
        IncreasingRateLimiter petRateLimiter = new IncreasingRateLimiter.Builder()
                                                       .cooldown(30, TimeUnit.MINUTES)
                                                       .maxCooldown(1, TimeUnit.HOURS)
                                                       .limit(1)
                                                       .premiumAware(false)
                                                       .prefix("petpet") //owo
                                                       .pool(MantaroData.getDefaultJedisPool())
                                                       .build();
        
        IncreasingRateLimiter trainRatelimiter = new IncreasingRateLimiter.Builder()
                                                         .cooldown(10, TimeUnit.MINUTES)
                                                         .maxCooldown(1, TimeUnit.HOURS)
                                                         .limit(1)
                                                         .premiumAware(false)
                                                         .randomIncrement(false)
                                                         .prefix("pettrain") //owo
                                                         .pool(MantaroData.getDefaultJedisPool())
                                                         .build();
        
        IncreasingRateLimiter incubateRatelimiter = new IncreasingRateLimiter.Builder()
                                                            .cooldown(6, TimeUnit.HOURS)
                                                            .limit(1)
                                                            .premiumAware(false)
                                                            .randomIncrement(false)
                                                            .prefix("pettrain") //owo
                                                            .pool(MantaroData.getDefaultJedisPool())
                                                            .build();
        
        TreeCommand petActionCommand = (TreeCommand) cr.register("pet", new TreeCommand(Category.PETS) {
            @Override
            public Command defaultTrigger(GuildMessageReceivedEvent event, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                        TextChannel channel = event.getChannel();
                        String[] args = StringUtils.advancedSplitArgs(content, 2);
                        ManagedDatabase db = MantaroData.db();
                        List<Member> found = FinderUtil.findMembers(content, event.getGuild());
                        String userId;
                        String petName;
                        
                        if(content.isEmpty()) {
                            channel.sendMessageFormat(languageContext.get("commands.pet.no_content"), EmoteReference.ERROR).queue();
                            return;
                        }
                        
                        //We only want one result, don't we?
                        if(found.size() > 1 && args.length > 1) {
                            channel.sendMessageFormat(languageContext.get("general.too_many_members"), EmoteReference.THINKING, found.stream().limit(7).map(m -> String.format("%s#%s", m.getUser().getName(), m.getUser().getDiscriminator())).collect(Collectors.joining(", "))).queue();
                            return;
                        }
                        
                        if(found.isEmpty() || args.length == 1) {
                            userId = event.getAuthor().getId();
                            petName = content;
                        } else {
                            userId = found.get(0).getUser().getId();
                            petName = args[1];
                        }
                        
                        if(petName.isEmpty()) {
                            channel.sendMessageFormat(languageContext.get("commands.pet.not_specified"), EmoteReference.ERROR).queue();
                            return;
                        }
                        
                        Player player = db.getPlayer(userId);
                        PlayerData playerData = player.getData();
                        
                        Pet pet = playerData.getPets().get(petName);
                        if(pet == null) {
                            channel.sendMessageFormat(languageContext.get("commands.pet.not_found"), EmoteReference.ERROR).queue();
                            return;
                        }
                        
                        final PetStats stats = pet.getStats();
                        DateTimeFormatter formatter =
                                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                                        .withLocale(Locale.UK)
                                        .withZone(ZoneId.systemDefault());
                        
                        //This is a placeholder to test stuff. Mostly how it'll look on release though.
                        channel.sendMessage(
                                new EmbedBuilder()
                                        .setAuthor(languageContext.get("commands.pet.overview"), null, event.getAuthor().getEffectiveAvatarUrl())
                                        //change to pet image when i actually have it
                                        .setThumbnail(event.getAuthor().getEffectiveAvatarUrl())
                                        .setDescription(
                                                Utils.prettyDisplay(languageContext.get("commands.pet.name"), pet.getName()) + "\n" +
                                                        Utils.prettyDisplay(languageContext.get("commands.pet.tier"), String.valueOf(pet.calculateTier())) + "\n" +
                                                        Utils.prettyDisplay(languageContext.get("commands.pet.element"), pet.getElement().getReadable()) + "\n" +
                                                        Utils.prettyDisplay(languageContext.get("commands.pet.owner"), MantaroBot.getInstance().getShardManager().getUserById(pet.getOwner()).getAsTag()) + "\n" +
                                                        Utils.prettyDisplay(languageContext.get("commands.pet.created"), formatter.format(Instant.ofEpochMilli(pet.getEpochCreatedAt())))
                                        )
                                        .addField(languageContext.get("commands.pet.affection"), getProgressBar(stats.getAffection(), 50) + String.format(" (%s/%s)", stats.getAffection(), 50), true)
                                        .addField(languageContext.get("commands.pet.hp"), getProgressBar(stats.getCurrentHP(), stats.getHp()) + String.format(" (%s/%s)", stats.getCurrentHP(), stats.getHp()), true)
                                        .addField(languageContext.get("commands.pet.stamina"), getProgressBar(stats.getCurrentStamina(), stats.getStamina()) + String.format(" (%s/%s)", stats.getCurrentStamina(), stats.getStamina()), true)
                                        .addField(languageContext.get("commands.pet.hunger"), getProgressBar(pet.getData().getHunger(), 100) + String.format(" (%s/%s)", pet.getData().getHunger(), 100), true)
                                        .addField(languageContext.get("commands.pet.hydration"), getProgressBar(pet.getData().getCurrentHydration(), 100) + String.format(" (%s/%s)", pet.getData().getCurrentHydration(), 100), true)
                                        .addField(languageContext.get("commands.pet.fly"), String.valueOf(pet.getStats().isFly()), true)
                                        .addField(languageContext.get("commands.pet.venom"), String.valueOf(pet.getStats().isVenom()), true)
                                        .addField(languageContext.get("commands.pet.inventory"), ItemStack.toString(pet.getPetInventory().asList()), false)
                                        .setFooter(String.format(languageContext.get("commands.pet.id"), pet.getData().getId()), null)
                                        .setColor(Color.PINK)
                                        .build()
                        ).queue();
                    }
                };
            }
        });
        
        petActionCommand.setPredicate(event -> Utils.handleDefaultIncreasingRatelimit(rateLimiter, event.getAuthor(), event, null));
        
        //Incubate new pet.
        petActionCommand.addSubCommand("incubate", new SubCommand() {
            SecureRandom random = new SecureRandom();
            
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                if(!Utils.handleDefaultIncreasingRatelimit(incubateRatelimiter, event.getAuthor(), event, null))
                    return;
                
                TextChannel channel = event.getChannel();
                
                ManagedDatabase managedDatabase = MantaroData.db();
                Player player = managedDatabase.getPlayer(event.getAuthor());
                PlayerData playerData = player.getData();
                
                String name;
                
                if(content.isEmpty()) {
                    channel.sendMessageFormat(languageContext.get("commands.petactions.incubate.no_name"), EmoteReference.ERROR).queue();
                    return;
                }
                
                if(!content.trim().matches("^[A-Za-z]+$")) {
                    channel.sendMessageFormat(languageContext.get("commands.petactions.incubate.only_letters"), EmoteReference.ERROR).queue();
                    return;
                }
                
                name = content.trim();
                
                if(!NAME_PATTERN.matcher(name).matches()) {
                    channel.sendMessageFormat(languageContext.get("commands.pet.character_not_allowed"), EmoteReference.ERROR).queue();
                    return;
                }
                
                if(!player.getInventory().containsItem(Items.INCUBATOR_EGG)) {
                    channel.sendMessageFormat(languageContext.get("commands.petactions.incubate.no_egg"), EmoteReference.ERROR).queue();
                    return;
                }
                
                if(playerData.getPetSlots() < playerData.getPets().size() + 1) {
                    channel.sendMessageFormat(languageContext.get("commands.petactions.incubate.not_enough_slots"), EmoteReference.ERROR, playerData.getPetSlots(), playerData.getPets().size()).queue();
                    return;
                }
                
                if(playerData.getPets().containsKey(name)) {
                    channel.sendMessageFormat(languageContext.get("commands.petactions.incubate.already_exists"), EmoteReference.ERROR).queue();
                    return;
                }
                
                long moneyNeeded = Math.max(70, random.nextInt(1000));
                if(player.getMoney() < moneyNeeded) {
                    channel.sendMessageFormat(languageContext.get("commands.petactions.incubate.no_money"), EmoteReference.ERROR, moneyNeeded).queue();
                    return;
                }
                
                Pet pet = generatePet(event.getAuthor().getId(), name);
                playerData.getPets().put(name, pet);
                
                player.getInventory().process(new ItemStack(Items.INCUBATOR_EGG, -1));
                player.save();
                
                channel.sendMessageFormat(languageContext.get("commands.petactions.incubate.success"), EmoteReference.POPPER, name, pet.getData().getId()).queue();
            }
        });
        
        petActionCommand.addSubCommand("rename", new SubCommand() {
            long renameCost = 500L;
            
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                TextChannel channel = event.getChannel();
                ManagedDatabase managedDatabase = MantaroData.db();
                Player player = managedDatabase.getPlayer(event.getAuthor());
                DBGuild guildData = managedDatabase.getGuild(event.getGuild());
                PlayerData playerData = player.getData();
                
                Map<String, Pet> playerPets = playerData.getPets();
                String[] args = StringUtils.advancedSplitArgs(content, -1);
                if(args.length < 2) {
                    channel.sendMessageFormat(languageContext.get("commands.petactions.rename.not_enough_args"), EmoteReference.ERROR).queue();
                    return;
                }
                
                String originalName = args[0];
                String rename = args[1];
                
                if(!NAME_PATTERN.matcher(rename).matches()) {
                    channel.sendMessageFormat(languageContext.get("commands.pet.character_not_allowed"), EmoteReference.ERROR).queue();
                    return;
                }
                
                if(!playerPets.containsKey(originalName)) {
                    channel.sendMessageFormat(languageContext.get("commands.petactions.pet.no_pet"), EmoteReference.ERROR).queue();
                    return;
                }
                
                if(playerPets.containsKey(rename)) {
                    channel.sendMessageFormat(languageContext.get("commands.petactions.rename.new_name_exists"), EmoteReference.ERROR).queue();
                    return;
                }
                
                if(player.getMoney() < renameCost) {
                    channel.sendMessageFormat(languageContext.get("commands.petactions.rename.not_enough_money"), EmoteReference.ERROR, renameCost).queue();
                    return;
                }
                
                new MessageBuilder()
                        .append(String.format(languageContext.get("commands.petactions.rename.confirmation"), EmoteReference.WARNING, originalName, rename))
                        .stripMentions(event.getJDA())
                        .sendTo(channel)
                        .queue();
                
                InteractiveOperations.create(channel, event.getAuthor().getIdLong(), 30, ie -> {
                    //Ignore all messages from anyone that isn't the user we already proposed to. Waiting for confirmation...
                    if(!ie.getAuthor().getId().equals(event.getAuthor().getId()))
                        return Operation.IGNORED;
                    
                    //Replace prefix because people seem to think you have to add the prefix before saying yes.
                    String message = ie.getMessage().getContentRaw();
                    for(String s : MantaroData.config().get().prefix) {
                        if(message.toLowerCase().startsWith(s)) {
                            message = message.substring(s.length());
                        }
                    }
                    
                    String guildCustomPrefix = guildData.getData().getGuildCustomPrefix();
                    if(guildCustomPrefix != null && !guildCustomPrefix.isEmpty() && message.toLowerCase().startsWith(guildCustomPrefix)) {
                        message = message.substring(guildCustomPrefix.length());
                    }
                    
                    if(message.equalsIgnoreCase("yes")) {
                        Player player2 = managedDatabase.getPlayer(event.getAuthor());
                        PlayerData playerData2 = player.getData();
                        Map<String, Pet> playerPetsConfirmed = playerData2.getPets();
                        if(!playerPetsConfirmed.containsKey(originalName)) {
                            channel.sendMessageFormat(languageContext.get("commands.petactions.rename.no_pet"), EmoteReference.ERROR).queue();
                            return Operation.COMPLETED;
                        }
                        
                        if(playerPetsConfirmed.containsKey(rename)) {
                            channel.sendMessageFormat(languageContext.get("commands.petactions.rename.new_name_exists"), EmoteReference.ERROR).queue();
                            return Operation.COMPLETED;
                        }
                        
                        if(player2.getMoney() < renameCost) {
                            channel.sendMessageFormat(languageContext.get("commands.petactions.rename.not_enough_money"), EmoteReference.ERROR, renameCost).queue();
                            return Operation.COMPLETED;
                        }
                        
                        Pet renamedPet = playerData2.getPets().remove(originalName);
                        playerData2.getPets().put(rename, renamedPet);
                        player2.removeMoney(500);
                        
                        player2.save();
                        new MessageBuilder().setContent(String.format(languageContext.get("commands.petactions.rename.success"), EmoteReference.ERROR, originalName, rename, renamedPet.getData().getId(), renameCost))
                                .stripMentions(event.getJDA())
                                .sendTo(channel)
                                .queue();
                        
                        return Operation.COMPLETED;
                    }
                    
                    return Operation.IGNORED;
                });
            }
        });
        
        //List your pets.
        petActionCommand.addSubCommand("ls", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                TextChannel channel = event.getChannel();
                ManagedDatabase managedDatabase = MantaroData.db();
                Player player = managedDatabase.getPlayer(event.getAuthor());
                PlayerData playerData = player.getData();
                
                Map<String, Pet> playerPets = playerData.getPets();
                EmbedBuilder builder = new EmbedBuilder()
                                               .setAuthor("Pet List", null, event.getAuthor().getEffectiveAvatarUrl())
                                               .setThumbnail(event.getAuthor().getEffectiveAvatarUrl())
                                               .setColor(Color.DARK_GRAY)
                                               .setFooter("Pet slots: " + playerData.getPetSlots() + ", Used: " + playerData.getPets().size(), null);
                
                List<MessageEmbed.Field> fields = new LinkedList<>();
                
                playerPets.forEach((key, pet) -> fields.add(new MessageEmbed.Field(pet.getName(),
                        Utils.prettyDisplay(languageContext.get("commands.pet.tier"), String.valueOf(pet.getTier())) + "\n" +
                                Utils.prettyDisplay(languageContext.get("commands.petactions.ls.experience"), String.format("%s (Level %s)", pet.getData().getXp(), pet.getData().getLevel()) + "\n" +
                                                                                                                      Utils.prettyDisplay(languageContext.get("commands.pet.element"), pet.getElement().getReadable()) + "\n" +
                                                                                                                      Utils.prettyDisplay(languageContext.get("commands.petactions.ls.age"), pet.getAgeDays() + " days") + "\n"
                                ),
                        true)
                ));
                
                List<List<MessageEmbed.Field>> splitFields = DiscordUtils.divideFields(8, fields);
                if(splitFields.isEmpty()) {
                    channel.sendMessageFormat("%1$sYou have no pets.", EmoteReference.BLUE_SMALL_MARKER).queue();
                    return;
                }
                
                boolean hasReactionPerms = event.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_ADD_REACTION);
                
                builder.setDescription(String.format(languageContext.get("commands.petactions.ls.pages"), splitFields.size()) +
                                               EmoteReference.TALKING + languageContext.get("commands.petactions.ls.header"));
                
                if(hasReactionPerms) {
                    DiscordUtils.list(event, 120, false, builder, splitFields);
                } else {
                    DiscordUtils.listText(event, 120, false, builder, splitFields);
                }
            }
        }).createSubCommandAlias("ls", "list");
        
        Random rand = new SecureRandom();
        
        //Yes. Exactly. Can increase affection. Don't overdo it though!
        petActionCommand.addSubCommand("pet", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                TextChannel channel = event.getChannel();
                ManagedDatabase managedDatabase = MantaroData.db();
                Player player = managedDatabase.getPlayer(event.getAuthor());
                PlayerData playerData = player.getData();
                
                String petName = content.trim();
                Map<String, Pet> playerPets = playerData.getPets();
                
                if(!playerPets.containsKey(petName)) {
                    channel.sendMessageFormat(languageContext.get("commands.petactions.pet.no_pet"), EmoteReference.ERROR).queue();
                    return;
                }
                
                Pet pet = playerPets.get(petName);
                PetData data = pet.getData();
                //Too many pats @ _ @
                if(!Utils.handleDefaultIncreasingRatelimit(petRateLimiter, data.getId(), event, languageContext, false)) {
                    return;
                }
                
                data.setTimesPetted(data.getTimesPetted() + 1);
                long affectionIncrease = data.getTimesPetted() / Math.max(10, rand.nextInt(50));
                data.setAffection(data.getAffection() + affectionIncrease);
                
                channel.sendMessageFormat(languageContext.get("commands.petactions.pet.pet"), EmoteReference.HEART, data.getAffection(), data.getTimesPetted()).queue();
                player.save();
            }
        });
        
        petActionCommand.addSubCommand("train", new SubCommand() {
            long trainCost = 1300L;
            
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                if(!Utils.handleDefaultIncreasingRatelimit(trainRatelimiter, event.getAuthor(), event, languageContext))
                    return;
                
                TextChannel channel = event.getChannel();
                ManagedDatabase managedDatabase = MantaroData.db();
                Player player = managedDatabase.getPlayer(event.getAuthor());
                PlayerData playerData = player.getData();
                
                String petName = content.trim();
                Map<String, Pet> playerPets = playerData.getPets();
                
                if(!playerPets.containsKey(petName)) {
                    channel.sendMessageFormat(languageContext.get("commands.petactions.pet.no_pet"), EmoteReference.ERROR).queue();
                    return;
                }
                
                if(player.getMoney() < trainCost) {
                    channel.sendMessageFormat(languageContext.get("commands.petactions.rename.not_enough_money"), EmoteReference.ERROR, trainCost).queue();
                    return;
                }
                
                Pet pet = playerPets.get(petName);
                PetData data = pet.getData();
                int factor = rand.nextInt(100);
                int experienceFactor;
                
                if(factor < 50)
                    experienceFactor = rand.nextInt(120) + factor;
                else
                    experienceFactor = rand.nextInt(40) + factor / 2;
                
                PetData.PetSkill trainedSkill = PetData.PetSkill.getRandom(); //Collect a random skill.
                Map<PetData.PetSkill, AtomicLong> petSkills = pet.getData().getPetSkills();
                petSkills.computeIfAbsent(trainedSkill, i -> new AtomicLong(0L));
                
                petSkills.get(trainedSkill).addAndGet(experienceFactor / 2);
                
                player.save();
                channel.sendMessageFormat(languageContext.get("commands.petactions.train.success"), EmoteReference.CORRECT, trainedSkill, experienceFactor, petSkills.get(trainedSkill).get(), trainCost).queue();
            }
        });
        
        //Apply effect to pet.
        petActionCommand.addSubCommand("effect", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                TextChannel channel = event.getChannel();
                ManagedDatabase managedDatabase = MantaroData.db();
                Player player = managedDatabase.getPlayer(event.getAuthor());
                PlayerData playerData = player.getData();
                Map<String, String> t = StringUtils.parse(content.split("\\s+"));
                
                String[] args = StringUtils.advancedSplitArgs(content, 2);
                
                String potion = args[0];
                String petName = content.replace(args[0], "").trim();
                Map<String, Pet> playerPets = playerData.getPets();
                
                if(!playerPets.containsKey(petName)) {
                    channel.sendMessageFormat(languageContext.get("commands.petactions.pet.no_pet"), EmoteReference.ERROR).queue();
                    return;
                }
                
                Pet pet = playerPets.get(petName);
                Item item = Items.fromAnyNoId(potion).orElse(null);
                
                //Well, shit.
                if(item == null) {
                    channel.sendMessageFormat(languageContext.get("general.item_lookup.not_found"), EmoteReference.ERROR).queue();
                    return;
                }
                
                if(item.getItemType() != ItemType.INTERACTIVE && item.getItemType() != ItemType.CRATE && item.getItemType() != ItemType.POTION && item.getItemType() != ItemType.BUFF) {
                    channel.sendMessageFormat(languageContext.get("commands.useitem.not_interactive"), EmoteReference.ERROR).queue();
                    return;
                }
                
                if(item.getAction() == null && (item.getItemType() != ItemType.POTION && item.getItemType() != ItemType.BUFF)) {
                    channel.sendMessageFormat(languageContext.get("commands.useitem.interactive_no_action"), EmoteReference.ERROR).queue();
                    return;
                }
                
                if(!player.getInventory().containsItem(item)) {
                    channel.sendMessageFormat(languageContext.get("commands.useitem.no_item"), EmoteReference.SAD).queue();
                    return;
                }
                
                applyPotionEffect(event, item, player, t, petName, true, languageContext);
            }
        });
        
        //Upgrade stats: requires materials and luck.
        petActionCommand.addSubCommand("upgrade", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                TextChannel channel = event.getChannel();
                ManagedDatabase managedDatabase = MantaroData.db();
                Player player = managedDatabase.getPlayer(event.getAuthor());
                PlayerData playerData = player.getData();
                
                String petName = content.trim();
                Map<String, Pet> playerPets = playerData.getPets();
                
                if(!playerPets.containsKey(petName)) {
                    channel.sendMessageFormat(languageContext.get("commands.petactions.pet.no_pet"), EmoteReference.ERROR).queue();
                    return;
                }
                
                Pet pet = playerPets.get(petName);
                PetData data = pet.getData();
                
                //todo PetData#getUpgradeLevel
            }
        });
        
        //Not aplicable to fire-type pets.
        petActionCommand.addSubCommand("feed", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                TextChannel channel = event.getChannel();
                ManagedDatabase managedDatabase = MantaroData.db();
                Player player = managedDatabase.getPlayer(event.getAuthor());
                PlayerData playerData = player.getData();
                String[] args = StringUtils.advancedSplitArgs(content, 2);
                String strippedContent = content.replace(args[0], "");
                
                String petName = args[0];
                Map<String, Pet> playerPets = playerData.getPets();
                
                if(!playerPets.containsKey(petName)) {
                    channel.sendMessageFormat(languageContext.get("commands.petactions.pet.no_pet"), EmoteReference.ERROR).queue();
                    return;
                }
                
                Pet pet = playerPets.get(petName);
                
                //Water-type pets can "eat" bc it wouldn't melt the food. Well, fire type would just make charcoal out of food... there are other ways to increase their energy.
                if(pet.getElement() == Pet.Type.FIRE) {
                    channel.sendMessageFormat(languageContext.get("commands.petactions.feed.fire_type"), EmoteReference.ERROR).queue();
                    return;
                }
                
                PetData data = pet.getData();
                Item item = Items.fromAnyNoId(strippedContent).orElse(null);
                
                if(item == null) {
                    channel.sendMessageFormat(languageContext.get("commands.petactions.feed.no_item"), EmoteReference.ERROR).queue();
                    return;
                }
                
                if(item.getItemType() != ItemType.PET_FOOD || !(item instanceof Food)) {
                    channel.sendMessageFormat(languageContext.get("commands.petactions.feed.not_food"), EmoteReference.ERROR).queue();
                    return;
                }
                
                Food foodItem = (Food) item; //Item is of type PET_FOOD, so assuming it's a Food type. Still, an additional check is done up there.
                float saturationIncrease = foodItem.getSaturation();
                int hungerIncrease = foodItem.getHungerLevel();
                
                if(data.getHunger() == 100) {
                    channel.sendMessageFormat(languageContext.get("commands.petactions.feed.full"), EmoteReference.ERROR).queue();
                    return;
                }
                
                //Update current values.
                data.updateSaturation();
                data.checkCurrentHunger();
                
                data.increaseHunger(hungerIncrease);
                data.increaseSaturation(saturationIncrease);
                
                player.getInventory().process(new ItemStack(item, -1));
                channel.sendMessageFormat(languageContext.get("commands.petaction.feed.success"), EmoteReference.POPPER, petName, data.getHunger(), data.getSaturation()).queue();
            }
        });
        
        //hydration nation
        petActionCommand.addSubCommand("hydrate", new SubCommand() {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                TextChannel channel = event.getChannel();
                ManagedDatabase managedDatabase = MantaroData.db();
                Player player = managedDatabase.getPlayer(event.getAuthor());
                Inventory playerInventory = player.getInventory();
                PlayerData playerData = player.getData();
                
                String petName = content.trim();
                Map<String, Pet> playerPets = playerData.getPets();
                
                if(!playerPets.containsKey(petName)) {
                    channel.sendMessageFormat(languageContext.get("commands.petactions.pet.no_pet"), EmoteReference.ERROR).queue();
                    return;
                }
                
                Pet pet = playerPets.get(petName);
                PetData petData = pet.getData();
                
                //You'd put them off...
                if(pet.getElement() == Pet.Type.FIRE) {
                    channel.sendMessageFormat(languageContext.get("commands.petactions.hydrate.fire_type"), EmoteReference.ERROR).queue();
                    return;
                }
                
                if(pet.getElement() != Pet.Type.WATER) {
                    channel.sendMessageFormat(languageContext.get("commands.petactions.hydrate.not_water"), EmoteReference.ERROR).queue();
                    return;
                }
                
                if(!playerInventory.containsItem(Items.WATER_BOTTLE)) {
                    channel.sendMessageFormat(languageContext.get("commands.petaction.hydrate.no_water"), EmoteReference.ERROR).queue();
                    return;
                }
                
                //Update hydration level.
                petData.getCurrentHydration();
                long after = petData.increaseHydration();
                
                petData.setLastHydratedAt(System.currentTimeMillis());
                
                player.save();
                channel.sendMessageFormat(languageContext.get("commands.petaction.hydrate.success"), EmoteReference.POPPER, after).queue();
            }
        });
    }
    
    //@Subscribe
    public void battle(CommandRegistry cr) {
        IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                                                    .cooldown(10, TimeUnit.MINUTES)
                                                    .limit(1)
                                                    .maxCooldown(15, TimeUnit.MINUTES)
                                                    .premiumAware(false)
                                                    .prefix("battle")
                                                    .pool(MantaroData.getDefaultJedisPool())
                                                    .build();
        
        cr.register("battle", new SimpleCommand(Category.PETS) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                //first check if it's in condition to battle, then check if the ratelimiter should run: if this doesn't happen, people will be waiting 10 minutes for a mistake, which isn't intended.
                if(!Utils.handleDefaultIncreasingRatelimit(rateLimiter, event.getAuthor(), event, languageContext, false))
                    return;
                
                TextChannel channel = event.getChannel();
            }
        });
    }
    
    private Pet generatePet(String owner, String name) {
        SecureRandom random = new SecureRandom();
        
        //Get random element.
        Pet pet = Pet.create(owner, name, Pet.Type.values()[random.nextInt(Pet.Type.values().length)]);
        PetStats petStats = pet.getStats();
        
        petStats.setHp(Math.max(20, random.nextInt(150)));
        petStats.setStamina(Math.max(20, random.nextInt(140)));
        petStats.setAffection(Math.max(15, random.nextInt(100)));
        
        //Can't have both a venom-type and fly-type pet: would be broken
        petStats.setVenom(random.nextBoolean());
        petStats.setFly(!petStats.isVenom() && random.nextBoolean());
        
        pet.getData().setId(UUID.randomUUID().toString());
        return pet;
    }
}
