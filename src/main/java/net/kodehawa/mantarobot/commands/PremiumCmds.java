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
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.PremiumKey;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;

@Module
@SuppressWarnings("unused")
public class PremiumCmds {
    @Subscribe
    public void comprevip(CommandRegistry cr) {
        cr.register("activatekey", new SimpleCommand(Category.UTILS) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(!(args.length == 0) && args[0].equalsIgnoreCase("check")) {
                    PremiumKey currentKey = MantaroData.db().getPremiumKey(MantaroData.db().getUser(event.getAuthor()).getData().getPremiumKey());

                    if(currentKey != null && currentKey.isEnabled() && currentTimeMillis() < currentKey.getExpiration()) { //Should always be enabled...
                        event.getChannel().sendMessageFormat(languageContext.get("commands.activatekey.check.key_valid_for"), EmoteReference.EYES, currentKey.validFor()).queue();
                    } else {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.activatekey.check.no_key_found"), EmoteReference.ERROR).queue();
                    }
                    return;
                }

                if(args.length < 1) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.activatekey.no_key_provided"), EmoteReference.ERROR).queue();
                    return;
                }

                PremiumKey key = MantaroData.db().getPremiumKey(args[0]);

                if(key == null || (key.isEnabled() && !(key.getParsedType().equals(PremiumKey.Type.MASTER)))) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.activatekey.invalid_key"), EmoteReference.ERROR).queue();
                    return;
                }

                PremiumKey.Type scopeParsed = key.getParsedType();

                if(scopeParsed.equals(PremiumKey.Type.GUILD)) {
                    DBGuild guild = MantaroData.db().getGuild(event.getGuild());

                    PremiumKey currentKey = MantaroData.db().getPremiumKey(guild.getData().getPremiumKey());

                    if(currentKey != null && currentKey.isEnabled() && currentTimeMillis() < currentKey.getExpiration()) { //Should always be enabled...
                        event.getChannel().sendMessageFormat(languageContext.get("commands.activatekey.guild_already_premium"), EmoteReference.POPPER).queue();
                        return;
                    }

                    key.activate(180);
                    event.getChannel().sendMessageFormat(languageContext.get("commands.activatekey.guild_successful"), EmoteReference.POPPER, key.getDurationDays()).queue();
                    guild.getData().setPremiumKey(key.getId());
                    guild.saveAsync();
                    return;
                }

                if(scopeParsed.equals(PremiumKey.Type.USER)) {
                    DBUser user = MantaroData.db().getUser(event.getAuthor());
                    Player player = MantaroData.db().getPlayer(event.getAuthor());

                    PremiumKey currentUserKey = MantaroData.db().getPremiumKey(user.getData().getPremiumKey());

                    if(currentUserKey != null && currentUserKey.isEnabled() && currentTimeMillis() < currentUserKey.getExpiration()) { //Should always be enabled...
                        event.getChannel().sendMessageFormat(languageContext.get("commands.activatekey.user_already_premium"), EmoteReference.POPPER).queue();
                        return;
                    }

                    if(event.getAuthor().getId().equals(key.getOwner())) {
                        player.getData().addBadgeIfAbsent(Badge.DONATOR);
                        player.saveAsync();
                    }

                    key.activate(event.getAuthor().getId().equals(key.getOwner()) ? 365 : 180);
                    event.getChannel().sendMessageFormat(languageContext.get("commands.activatekey.user_successful"), EmoteReference.POPPER, key.getDurationDays()).queue();
                    user.getData().setPremiumKey(key.getId());
                    user.saveAsync();
                }
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Premium Key Actvation")
                        .setDescription("Activates a premium key!\n" +
                                "Example: `~>activatekey a4e98f07-1a32-4dcc-b53f-c540214d54ec`\n" +
                                "No, that isn't a valid key.")
                        .build();
            }
        });
    }

    @Subscribe
    public void checkpremium(CommandRegistry cr) {
        cr.register("vipstatus", new SimpleCommand(Category.INFO) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                EmbedBuilder embedBuilder = new EmbedBuilder();

                List<User> mentionedUsers = event.getMessage().getMentionedUsers();
                boolean isMention = !mentionedUsers.isEmpty();
                if(args.length == 0 || isMention) {
                    User toCheck = event.getAuthor();
                    if(isMention)
                        toCheck = mentionedUsers.get(0);

                    DBUser dbUser = MantaroData.db().getUser(toCheck);
                    if(!dbUser.isPremium()) {
                        event.getChannel().sendMessageFormat(languageContext.get("commands.vipstatus.user.not_premium"), EmoteReference.ERROR).queue();
                        return;
                    }

                    embedBuilder.setAuthor(isMention ?
                            String.format(languageContext.get("commands.vipstatus.user.header_other"), toCheck.getName()) :
                            languageContext.get("commands.vipstatus.user.header"), null, event.getAuthor().getEffectiveAvatarUrl()
                    );

                    final UserData data = dbUser.getData();
                    PremiumKey currentKey = MantaroData.db().getPremiumKey(data.getPremiumKey());

                    if(currentKey != null) {
                        User owner = MantaroBot.getInstance().getUserById(currentKey.getOwner());
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

                        embedBuilder.setDescription(languageContext.get("commands.vipstatus.user.premium"))
                                .addField(languageContext.get("commands.vipstatus.expire"), currentKey.validFor() + " " + languageContext.get("general.days"), false)
                                .addField(languageContext.get("commands.vipstatus.key_duration"), currentKey.getDurationDays() + " " + languageContext.get("general.days"), false)
                                .addField(languageContext.get("commands.vipstatus.key_owner"), owner.getName() + "#" + owner.getDiscriminator(), false);
                    } else {
                        embedBuilder.setDescription(languageContext.get("commands.vipstatus.user.old_system"))
                                .addField(languageContext.get("commands.vipstatus.valid_for_old"),
                                        Math.max(0, TimeUnit.MILLISECONDS.toDays(dbUser.getPremiumUntil() - currentTimeMillis())) + " days more", false);
                    }

                    embedBuilder.setFooter(languageContext.get("commands.vipstatus.thank_note"), null);
                    event.getChannel().sendMessage(embedBuilder.build()).queue();
                } else {
                    if(args[0].equals("guild")) {
                        DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                        if(!dbGuild.isPremium()) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.vipstatus.guild.not_premium"), EmoteReference.ERROR).queue();
                            return;
                        }

                        embedBuilder.setAuthor(String.format(languageContext.get("commands.vipstatus.guild.header"), event.getGuild().getName()), null, event.getAuthor().getEffectiveAvatarUrl());
                        PremiumKey currentKey = MantaroData.db().getPremiumKey(dbGuild.getData().getPremiumKey());

                        if(currentKey != null) {
                            User owner = MantaroBot.getInstance().getUserById(currentKey.getOwner());
                            if(owner == null)
                                owner = event.getGuild().getOwner().getUser();

                            embedBuilder.setDescription(languageContext.get("commands.vipstatus.guild.premium"))
                                    .addField(languageContext.get("commands.vipstatus.expire"), currentKey.validFor() + " days", false)
                                    .addField(languageContext.get("commands.vipstatus.key_duration"), currentKey.getDurationDays() + " days", false)
                                    .addField(languageContext.get("commands.vipstatus.key_owner"), owner.getName() + "#" + owner.getDiscriminator(), false);
                        } else {
                            embedBuilder.setDescription(languageContext.get("commands.vipstatus.guild.old_system"))
                                    .addField(languageContext.get("commands.vipstatus.valid_for_old"),
                                            String.format(languageContext.get("commands.vipstatus.old_days_more"),
                                                    Math.max(0, TimeUnit.MILLISECONDS.toDays(dbGuild.getPremiumUntil() - currentTimeMillis()))
                                            ), false
                                    );
                        }

                        embedBuilder.setFooter(languageContext.get("commands.vipstatus.thank_note"), null);
                        event.getChannel().sendMessage(embedBuilder.build()).queue();
                        return;
                    }

                    event.getChannel().sendMessageFormat(languageContext.get("commands.vipstatus.wrong_args"), EmoteReference.ERROR).queue();
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

    //Won't translate this. Owner command.
    @Subscribe
    public void createkey(CommandRegistry cr) {
        cr.register("createkey", new SimpleCommand(Category.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(args.length < 2) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You need to provide a scope and an id (example: guild 1558674582032875529)").queue();
                    return;
                }

                String scope = args[0];
                String owner = args[1];

                PremiumKey.Type scopeParsed = null;
                try {
                    scopeParsed = PremiumKey.Type.valueOf(scope.toUpperCase()); //To get the ordinal
                } catch(IllegalArgumentException ignored) {}

                if(scopeParsed == null) {
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
