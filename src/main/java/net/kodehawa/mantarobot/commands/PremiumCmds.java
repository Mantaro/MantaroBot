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

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.PremiumKey;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;

@Module
public class PremiumCmds {
    @Subscribe
    public void comprevip(CommandRegistry cr){
        cr.register("activatekey", new SimpleCommand(Category.UTILS) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {

                if(!(args.length == 0) && args[0].equalsIgnoreCase("check")){
                    PremiumKey currentKey = MantaroData.db().getPremiumKey(MantaroData.db().getUser(event.getAuthor()).getData().getPremiumKey());

                    if(currentKey != null && currentKey.isEnabled() && currentTimeMillis() < currentKey.getExpiration()){ //Should always be enabled...
                        event.getChannel().sendMessage(EmoteReference.EYES + "Your key is valid for " + currentKey.validFor() + " more days :heart:").queue();
                    } else {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You don't have a key enabled... If you're a premium from the old system you should " +
                                "still have your rewards, though!").queue();
                    }
                    return;
                }

                if(args.length < 2){
                    event.getChannel().sendMessage(EmoteReference.ERROR + "I cannot enable a premium key if you don't give me one or if you don't give me the scope!").queue();
                    return;
                }

                PremiumKey key = MantaroData.db().getPremiumKey(args[0]);

                if(key == null || (key.isEnabled() && !(key.getParsedType().equals(PremiumKey.Type.MASTER)))){
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You provided an invalid or already enabled key!").queue();
                    return;
                }

                String scope = args[1];
                PremiumKey.Type scopeParsed = null;
                try{
                    scopeParsed = PremiumKey.Type.valueOf(scope.toUpperCase()); //To get the ordinal
                } catch (IllegalArgumentException ignored){}

                if(scopeParsed == null){
                    event.getChannel().sendMessage(EmoteReference.ERROR + "Invalid scope (Valid ones are: `user` or `guild`)").queue();
                    return;
                }

                if(!key.getParsedType().equals(scopeParsed)){
                    event.getChannel().sendMessage(EmoteReference.ERROR + "This key is for another scope (Scope: " + key.getParsedType() + ")").queue();
                    return;
                }

                if (scopeParsed.equals(PremiumKey.Type.GUILD)) {
                    DBGuild guild = MantaroData.db().getGuild(event.getGuild());

                    PremiumKey currentKey = MantaroData.db().getPremiumKey(guild.getData().getPremiumKey());

                    if(currentKey != null && currentKey.isEnabled() && currentTimeMillis() < key.getExpiration()){ //Should always be enabled...
                        event.getChannel().sendMessage(EmoteReference.ERROR + "This server already has a premium subscription!").queue();
                        return;
                    }

                    key.activate(180);
                    event.getChannel().sendMessage(EmoteReference.POPPER + "This server is now **Premium** :heart: (For: " + key.getDurationDays() + " days)").queue();
                    guild.getData().setPremiumKey(key.getId());
                    guild.saveAsync();
                    return;
                }

                if(scopeParsed.equals(PremiumKey.Type.USER)){
                    DBUser user = MantaroData.db().getUser(event.getAuthor());
                    Player player = MantaroData.db().getPlayer(event.getAuthor());

                    PremiumKey currentUserKey = MantaroData.db().getPremiumKey(user.getData().getPremiumKey());
                    if(currentUserKey != null && currentUserKey.isEnabled() && currentTimeMillis() < key.getExpiration()){ //Should always be enabled...
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You're already premium :heart:!").queue();
                        return;
                    }

                    if(event.getAuthor().getId().equals(key.getOwner())) {
                        player.getData().addBadge(Badge.DONATOR);
                        player.saveAsync();
                    }

                    key.activate(event.getAuthor().getId().equals(key.getOwner()) ? 365 : 180);
                    event.getChannel().sendMessage(EmoteReference.POPPER + "You're now **Premium** :heart: (For: " + key.getDurationDays() + " days)").queue();
                    user.getData().setPremiumKey(key.getId());
                    user.saveAsync();
                    return;
                }

                event.getChannel().sendMessage(EmoteReference.ERROR + "This shouldn't happen...").queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Premium Key Actvation")
                        .setDescription("Activates a premium key!\n" +
                                "Example: `~>activatekey a4e98f07-1a32-4dcc-b53f-c540214d54ec user` or `~>activatekey 550e8400-e29b-41d4-a716-446655440000 guild`\n" +
                                "No, those aren't valid keys.")
                        .build();
            }
        });
    }

    @Subscribe
    public void checkpremium(CommandRegistry cr) {
        cr.register("vipstatus", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                EmbedBuilder embedBuilder = new EmbedBuilder();

                if(args.length == 0) {
                    DBUser dbUser = MantaroData.db().getUser(event.getAuthor());
                    if(!dbUser.isPremium()) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You aren't premium :c").queue();
                        return;
                    }

                    embedBuilder.setAuthor("Your Premium Status", null, event.getAuthor().getEffectiveAvatarUrl());

                    if(dbUser.getData().getPremiumKey() != null) {
                        PremiumKey currentKey = MantaroData.db().getPremiumKey(MantaroData.db().getUser(event.getAuthor()).getData().getPremiumKey());
                        User owner = MantaroBot.getInstance().getUserById(currentKey.getOwner());
                        if(owner == null)
                            owner = event.getAuthor();

                        embedBuilder.setDescription("**Premium user! <3**")
                                .addField("Valid for", currentKey.validFor() + " days", false)
                                .addField("Key total duration", currentKey.getDurationDays() + " days", false)
                                .addField("Key owner", owner.getName() + "#" + owner.getDiscriminator(), false);
                    } else {
                        embedBuilder.setDescription("**Premium user under the old system! Don't worry tho, you're premium and your benefits" +
                                " will work properly!**")
                                .addField("Valid for", TimeUnit.MILLISECONDS.toDays(dbUser.getPremiumUntil() - currentTimeMillis()) + " days more", false);
                    }

                    embedBuilder.setFooter("Thanks you for your support <3", null);
                    event.getChannel().sendMessage(embedBuilder.build()).queue();
                } else {
                    if(args[0].equals("guild")) {
                        DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());

                        embedBuilder.setAuthor(event.getGuild().getName() + "'s Premium Status", null, event.getAuthor().getEffectiveAvatarUrl());

                        if(dbGuild.getData().getPremiumKey() != null) {
                            PremiumKey currentKey = MantaroData.db().getPremiumKey(MantaroData.db().getUser(event.getAuthor()).getData().getPremiumKey());
                            User owner = MantaroBot.getInstance().getUserById(currentKey.getOwner());
                            if(owner == null)
                                owner = event.getAuthor();

                            embedBuilder.setDescription("**Premium guild! <3**")
                                    .addField("Valid for", currentKey.validFor() + " days", false)
                                    .addField("Key total duration", currentKey.getDurationDays() + " days", false)
                                    .addField("Key owner", owner.getName() + "#" + owner.getDiscriminator(), false);
                        } else {
                            embedBuilder.setDescription("**Premium guild under the old system! Don't worry tho, you're premium and your benefits" +
                                    " will work properly!**")
                                    .addField("Valid for", TimeUnit.MILLISECONDS.toDays(dbGuild.getPremiumUntil() - currentTimeMillis()) + " days more", false);
                        }

                        embedBuilder.setFooter("Thanks you for your support <3", null);
                        event.getChannel().sendMessage(embedBuilder.build()).queue();
                        return;
                    }

                    event.getChannel().sendMessage(EmoteReference.ERROR + "Wrong arguments...").queue();
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Check premium status")
                        .setDescription("**Checks your premium status or the guild status**")
                        .addField("Arguments", "`~>vipstatus` - Returns your premium key status\n" +
                                "`~>vipstatus guild` - Return this guild's premium status.", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void createkey(CommandRegistry cr){
        cr.register("createkey", new SimpleCommand(Category.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {

                if(args.length < 2){
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You need to provide a scope and an id (example: master 1558674582032875529)").queue();
                    return;
                }

                String scope = args[0];
                String owner = args[1];
                PremiumKey.Type scopeParsed = null;
                try{
                    scopeParsed = PremiumKey.Type.valueOf(scope.toUpperCase()); //To get the ordinal
                } catch (IllegalArgumentException ignored){}

                if(scopeParsed == null){
                    event.getChannel().sendMessage(EmoteReference.ERROR + "Invalid scope (Valid ones are: `user`, `guild` or `master`)").queue();
                    return;
                }


                //This method generates a premium key AND saves it on the database! Please use this result!
                PremiumKey generated = PremiumKey.generatePremiumKey(owner, scopeParsed);
                event.getChannel().sendMessage(EmoteReference.CORRECT + String.format("Generated: `%s` (S: %s) **[NOT ACTIVATED]**",
                        generated.getId(), generated.getParsedType())).queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Makes a premium key, what else? Needs scope (user or guild) and id.").build();
            }
        });
    }
}
