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
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.PremiumKey;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.log.LogUtils;
import net.kodehawa.mantarobot.utils.Pair;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.awt.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.System.currentTimeMillis;

@Module
@SuppressWarnings("unused")
public class PremiumCmds {
    private Config config = MantaroData.config().get();
    
    @Subscribe
    public void comprevip(CommandRegistry cr) {
        cr.register("activatekey", new SimpleCommand(Category.UTILS) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                TextChannel channel = event.getChannel();
                
                final ManagedDatabase db = MantaroData.db();
                
                if(config.isPremiumBot()) {
                    channel.sendMessageFormat(languageContext.get("commands.activatekey.mp"), EmoteReference.WARNING).queue();
                    return;
                }
                
                if(!(args.length == 0) && args[0].equalsIgnoreCase("check")) {
                    PremiumKey currentKey = db.getPremiumKey(db.getUser(event.getAuthor()).getData().getPremiumKey());
                    
                    if(currentKey != null && currentKey.isEnabled() && currentTimeMillis() < currentKey.getExpiration()) { //Should always be enabled...
                        channel.sendMessageFormat(languageContext.get("commands.activatekey.check.key_valid_for"), EmoteReference.EYES, currentKey.validFor()).queue();
                    } else {
                        channel.sendMessageFormat(languageContext.get("commands.activatekey.check.no_key_found"), EmoteReference.ERROR).queue();
                    }
                    return;
                }
                
                if(args.length < 1) {
                    channel.sendMessageFormat(languageContext.get("commands.activatekey.no_key_provided"), EmoteReference.ERROR).queue();
                    return;
                }
                
                PremiumKey key = db.getPremiumKey(args[0]);
                
                if(key == null || (key.isEnabled())) {
                    channel.sendMessageFormat(languageContext.get("commands.activatekey.invalid_key"), EmoteReference.ERROR).queue();
                    return;
                }
                
                PremiumKey.Type scopeParsed = key.getParsedType();
                
                if(scopeParsed.equals(PremiumKey.Type.GUILD)) {
                    DBGuild guild = db.getGuild(event.getGuild());
                    
                    PremiumKey currentKey = db.getPremiumKey(guild.getData().getPremiumKey());
                    
                    if(currentKey != null && currentKey.isEnabled() && currentTimeMillis() < currentKey.getExpiration()) { //Should always be enabled...
                        channel.sendMessageFormat(languageContext.get("commands.activatekey.guild_already_premium"), EmoteReference.POPPER).queue();
                        return;
                    }
                    
                    key.activate(180);
                    channel.sendMessageFormat(languageContext.get("commands.activatekey.guild_successful"), EmoteReference.POPPER, key.getDurationDays()).queue();
                    guild.getData().setPremiumKey(key.getId());
                    guild.saveAsync();
                    return;
                }
                
                if(scopeParsed.equals(PremiumKey.Type.USER)) {
                    DBUser user = db.getUser(event.getAuthor());
                    Player player = db.getPlayer(event.getAuthor());
                    
                    PremiumKey currentUserKey = db.getPremiumKey(user.getData().getPremiumKey());
                    
                    if(user.isPremium()) {
                        channel.sendMessageFormat(languageContext.get("commands.activatekey.user_already_premium"), EmoteReference.POPPER).queue();
                        return;
                    }
                    
                    if(event.getAuthor().getId().equals(key.getOwner())) {
                        player.getData().addBadgeIfAbsent(Badge.DONATOR);
                        player.saveAsync();
                    }
                    
                    //Add to keys claimed storage if it's NOT your first key (count starts at 2/2 = 1)
                    if(!event.getAuthor().getId().equals(key.getOwner())) {
                        DBUser ownerUser = db.getUser(key.getOwner());
                        ownerUser.getData().getKeysClaimed().put(event.getAuthor().getId(), key.getId());
                        ownerUser.saveAsync();
                    }
                    
                    key.activate(event.getAuthor().getId().equals(key.getOwner()) ? 365 : 180);
                    channel.sendMessageFormat(languageContext.get("commands.activatekey.user_successful"), EmoteReference.POPPER, key.getDurationDays()).queue();
                    user.getData().setPremiumKey(key.getId());
                    user.saveAsync();
                }
            }
            
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("Activates a premium key.\n" +
                                                       "Example: `~>activatekey a4e98f07-1a32-4dcc-b53f-c540214d54ec`. No, that isn't a valid key.")
                               .setUsage("`~>activatekey <key>`")
                               .addParameter("key", "The key to use. If it's a server key, make sure to run this command in the server where you want to enable premium on.")
                               .build();
            }
        });
    }
    
    //TODO: uncomment when you know most keys are registered.
    //@Subscribe
    public void claimkey(CommandRegistry cr) {
        cr.register("claimkey", new SimpleCommand(Category.UTILS) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                TextChannel channel = event.getChannel();
                
                if(config.isPremiumBot()) {
                    channel.sendMessageFormat(languageContext.get("commands.activatekey.mp"), EmoteReference.WARNING).queue();
                    return;
                }
                
                String type = "";
                PremiumKey.Type scopeParsed = PremiumKey.Type.USER;
                if(args.length > 0) {
                    try {
                        scopeParsed = PremiumKey.Type.valueOf(args[0].toUpperCase());
                    } catch(IllegalArgumentException e) {
                        channel.sendMessageFormat(languageContext.get("commands.claimkey.invalid_scope"), EmoteReference.ERROR).queue();
                    }
                }
                
                final ManagedDatabase db = MantaroData.db();
                final User author = event.getAuthor();
                
                //left: isPatron, right: pledgeAmount, basically.
                Pair<Boolean, String> pledgeInfo = Utils.getPledgeInformation(author.getId());
                if(pledgeInfo == null || !pledgeInfo.getLeft()) {
                    channel.sendMessageFormat(languageContext.get("commands.claimkey.not_patron"), EmoteReference.ERROR).queue();
                    return;
                }
                
                double pledgeAmount = Double.parseDouble(pledgeInfo.getRight());
                DBUser dbUser = db.getUser(author);
                UserData data = dbUser.getData();
                
                //Check for pledge changes on DBUser#isPremium
                if(pledgeAmount == 1 || data.getKeysClaimed().size() >= (pledgeAmount / 2)) {
                    channel.sendMessageFormat(languageContext.get("commands.claimkey.already_top"), EmoteReference.ERROR).queue();
                    return;
                }
                
                PremiumKey newKey = PremiumKey.generatePremiumKey(author.getId(), scopeParsed, true);
                
                //Placeholder so they don't spam key creation. Save as random UUID first, to avoid conflicting.
                data.getKeysClaimed().put(UUID.randomUUID().toString(), newKey.getId());
                
                //Send message in a DM (it's private after all)
                int amountClaimed = data.getKeysClaimed().size();
                event.getAuthor().openPrivateChannel()
                        .queue(privateChannel -> {
                            privateChannel.sendMessageFormat(languageContext.get("commands.claimkey.successful"),
                                    EmoteReference.HEART, newKey.getId(), amountClaimed, (int) ((pledgeAmount / 2) - amountClaimed), newKey.getParsedType()).queue();
                            dbUser.saveAsync();
                            newKey.saveAsync();
                            channel.sendMessageFormat(languageContext.get("commands.claimkey.success"), EmoteReference.CORRECT).queue();
                        }, error -> channel.sendMessageFormat(languageContext.get("commands.claimkey.cant_dm"), EmoteReference.ERROR).queue());
            }
        });
    }
    
    @Subscribe
    public void vipstatus(CommandRegistry cr) {
        cr.register("vipstatus", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                TextChannel channel = event.getChannel();
                
                if(config.isPremiumBot()) {
                    channel.sendMessageFormat(languageContext.get("commands.activatekey.mp"), EmoteReference.WARNING).queue();
                    return;
                }
                
                EmbedBuilder embedBuilder = new EmbedBuilder();
                
                List<User> mentionedUsers = event.getMessage().getMentionedUsers();
                boolean isMention = !mentionedUsers.isEmpty();
                if(args.length == 0 || isMention) {
                    User toCheck = event.getAuthor();
                    if(isMention)
                        toCheck = mentionedUsers.get(0);
                    
                    DBUser dbUser = MantaroData.db().getUser(toCheck);
                    if(!dbUser.isPremium()) {
                        channel.sendMessageFormat(languageContext.get("commands.vipstatus.user.not_premium"), EmoteReference.ERROR).queue();
                        return;
                    }
                    
                    embedBuilder.setAuthor(isMention ?
                                                   String.format(languageContext.get("commands.vipstatus.user.header_other"), toCheck.getName()) :
                                                   languageContext.get("commands.vipstatus.user.header"), null, toCheck.getEffectiveAvatarUrl()
                    );
                    
                    final UserData data = dbUser.getData();
                    PremiumKey currentKey = MantaroData.db().getPremiumKey(data.getPremiumKey());
                    
                    if(currentKey != null && currentKey.validFor() > 0) {
                        User owner = MantaroBot.getInstance().getShardManager().getUserById(currentKey.getOwner());
                        boolean marked = false;
                        if(owner == null) {
                            marked = true;
                            owner = event.getAuthor();
                        }
                        
                        if(!marked && isMention) {
                            Player p = MantaroData.db().getPlayer(owner);
                            if(p.getData().addBadgeIfAbsent(Badge.DONATOR_2))
                                p.saveAsync();
                        }
                        
                        Pair<Boolean, String> patreonInformation = Utils.getPledgeInformation(owner.getId());
                        String linkedTo = currentKey.getData().getLinkedTo();
                        int amountClaimed = data.getKeysClaimed().size();
                        embedBuilder.setColor(Color.CYAN)
                                .setThumbnail(toCheck.getEffectiveAvatarUrl())
                                .setDescription(languageContext.get("commands.vipstatus.user.premium"))
                                .addField(languageContext.get("commands.vipstatus.expire"), currentKey.validFor() + " " + languageContext.get("general.days"), true)
                                .addField(languageContext.get("commands.vipstatus.key_duration"), currentKey.getDurationDays() + " " + languageContext.get("general.days"), true)
                                .addField(languageContext.get("commands.vipstatus.key_owner"), owner.getName() + "#" + owner.getDiscriminator(), true)
                                .addField(languageContext.get("commands.vipstatus.patreon"), patreonInformation == null ? "Error" : String.valueOf(patreonInformation.getLeft()), true)
                                .addField(languageContext.get("commands.vipstatus.keys_claimed"), String.valueOf(amountClaimed), true)
                                .addField(languageContext.get("commands.vipstatus.linked"), String.valueOf(linkedTo != null), true);
                        
                        try {
                            double patreonAmount = Double.parseDouble(patreonInformation.getRight());
                            if((patreonAmount / 2) - amountClaimed < 0 && patreonInformation.getLeft()) {
                                LogUtils.log(String.format("%s has more keys claimed than given keys, dumping keys:\n%s", owner.getId(),
                                        Utils.paste2(data.getKeysClaimed().entrySet().stream().map(entry -> "to:" + entry.getKey() + ", key:" + entry.getValue()).collect(Collectors.joining("\n"))))
                                );
                            }
                        } catch(Exception ignored) {
                        }
                        
                        if(linkedTo != null) {
                            User linkedUser = MantaroBot.getInstance().getShardManager().getUserById(currentKey.getOwner());
                            embedBuilder.addField(languageContext.get("commands.vipstatus.linked_to"), linkedUser.getName() + "#" + linkedUser.getDiscriminator(), true);
                        }
                    } else {
                        embedBuilder.setDescription(languageContext.get("commands.vipstatus.user.old_system"))
                                .addField(languageContext.get("commands.vipstatus.valid_for_old"),
                                        Math.max(0, TimeUnit.MILLISECONDS.toDays(dbUser.getPremiumUntil() - currentTimeMillis())) + " days more", false);
                    }
                    
                    embedBuilder.setFooter(languageContext.get("commands.vipstatus.thank_note"), null);
                    channel.sendMessage(embedBuilder.build()).queue();
                } else {
                    if(args[0].equals("guild")) {
                        DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                        if(!dbGuild.isPremium()) {
                            channel.sendMessageFormat(languageContext.get("commands.vipstatus.guild.not_premium"), EmoteReference.ERROR).queue();
                            return;
                        }
                        
                        embedBuilder.setAuthor(String.format(languageContext.get("commands.vipstatus.guild.header"), event.getGuild().getName()), null, event.getAuthor().getEffectiveAvatarUrl());
                        PremiumKey currentKey = MantaroData.db().getPremiumKey(dbGuild.getData().getPremiumKey());
                        
                        if(currentKey != null && currentKey.validFor() > 0) {
                            User owner = MantaroBot.getInstance().getShardManager().getUserById(currentKey.getOwner());
                            if(owner == null)
                                owner = event.getGuild().getOwner().getUser();
                            
                            Pair<Boolean, String> patreonInformation = Utils.getPledgeInformation(owner.getId());
                            String linkedTo = currentKey.getData().getLinkedTo();
                            embedBuilder.setColor(Color.CYAN)
                                    .setThumbnail(event.getGuild().getIconUrl())
                                    .setDescription(languageContext.get("commands.vipstatus.guild.premium"))
                                    .addField(languageContext.get("commands.vipstatus.expire"), currentKey.validFor() + " days", true)
                                    .addField(languageContext.get("commands.vipstatus.key_duration"), currentKey.getDurationDays() + " days", true)
                                    .addField(languageContext.get("commands.vipstatus.key_owner"), owner.getName() + "#" + owner.getDiscriminator(), true)
                                    .addField(languageContext.get("commands.vipstatus.patreon"), patreonInformation == null ? "Error" : String.valueOf(patreonInformation.getLeft()), true)
                                    .addField(languageContext.get("commands.vipstatus.linked"), String.valueOf(linkedTo != null), false);
                            
                            if(linkedTo != null) {
                                User linkedUser = MantaroBot.getInstance().getShardManager().getUserById(currentKey.getOwner());
                                embedBuilder.addField(languageContext.get("commands.vipstatus.linked_to"), linkedUser.getName() + "#" + linkedUser.getDiscriminator(), false);
                            }
                        } else {
                            //TODO: remove
                            embedBuilder.setDescription(languageContext.get("commands.vipstatus.guild.old_system"))
                                    .addField(languageContext.get("commands.vipstatus.valid_for_old"),
                                            String.format(languageContext.get("commands.vipstatus.old_days_more"),
                                                    Math.max(0, TimeUnit.MILLISECONDS.toDays(dbGuild.getPremiumUntil() - currentTimeMillis()))
                                            ), false
                                    );
                        }
                        
                        embedBuilder.setFooter(languageContext.get("commands.vipstatus.thank_note"), null);
                        channel.sendMessage(embedBuilder.build()).queue();
                        return;
                    }
                    
                    channel.sendMessageFormat(languageContext.get("commands.vipstatus.wrong_args"), EmoteReference.ERROR).queue();
                }
            }
            
            
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("Checks your premium status or the guild status.")
                               .setUsage("`~>vipstatus` - Returns your premium key status\n" +
                                                 "`~>vipstatus guild` - Return this guild's premium status.")
                               .build();
            }
        });
    }
    
    //Won't translate this. Owner command.
    @Subscribe
    public void createkey(CommandRegistry cr) {
        cr.register("createkey", new SimpleCommand(Category.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                TextChannel channel = event.getChannel();
                
                if(args.length < 3) {
                    channel.sendMessage(EmoteReference.ERROR + "You need to provide a scope, an id and whether this key is linked (example: guild 1558674582032875529 true)").queue();
                    return;
                }
                
                String scope = args[0];
                String owner = args[1];
                boolean linked = Boolean.parseBoolean(args[2]);
                
                PremiumKey.Type scopeParsed = null;
                try {
                    scopeParsed = PremiumKey.Type.valueOf(scope.toUpperCase()); //To get the ordinal
                } catch(IllegalArgumentException ignored) {
                }
                
                if(scopeParsed == null) {
                    channel.sendMessage(EmoteReference.ERROR + "Invalid scope (Valid ones are: `user` or `guild`)").queue();
                    return;
                }
                
                //This method generates a premium key AND saves it on the database! Please use this result!
                PremiumKey generated = PremiumKey.generatePremiumKey(owner, scopeParsed, linked);
                channel.sendMessage(EmoteReference.CORRECT + String.format("Generated: `%s` (S: %s) **[NOT ACTIVATED]** (Linked: %s)",
                        generated.getId(), generated.getParsedType(), linked)).queue();
            }
            
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("Makes a premium key, what else? Needs scope (user or guild) and id. Also add true or false for linking status at the end")
                               .build();
            }
        });
    }
}
