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
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.options.core.Option;
import net.kodehawa.mantarobot.options.core.OptionType;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.Pair;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.TriConsumer;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.Map.Entry;
import static net.kodehawa.mantarobot.utils.Utils.mapConfigObjects;

@Module
@SuppressWarnings("unused")
public class OptsCmd {
    public static Command optsCmd;
    
    public static SimpleCommand getOpts() {
        return (SimpleCommand) optsCmd;
    }
    
    @Subscribe
    public void register(CommandRegistry registry) {
        registry.register("opts", optsCmd = new SimpleCommand(Category.MODERATION, CommandPermission.ADMIN) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                TextChannel channel = event.getChannel();
                
                if(args.length == 0) {
                    channel.sendMessage(String.format(languageContext.get("options.error_general"), EmoteReference.WARNING)).queue();
                    return;
                }
                
                if(args.length == 1 && args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("ls")) {
                    StringBuilder builder = new StringBuilder();
                    
                    for(String s : Option.getAvaliableOptions())
                        builder.append(s).append("\n");
                    
                    List<String> m = DiscordUtils.divideString(builder);
                    List<String> messages = new LinkedList<>();
                    boolean hasReactionPerms = event.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_ADD_REACTION);
                    for(String s1 : m) {
                        messages.add(String.format(languageContext.get("commands.opts.list.header"),
                                hasReactionPerms ? languageContext.get("general.text_menu") + " " : languageContext.get("general.arrow_react"), String.format("```prolog\n%s```", s1)));
                    }
                    
                    if(hasReactionPerms) {
                        DiscordUtils.list(event, 45, false, messages);
                    } else {
                        DiscordUtils.listText(event, 45, false, messages);
                    }
                    
                    return;
                }
                
                if(args.length < 2) {
                    channel.sendMessage(String.format(languageContext.get("options.error_general"), EmoteReference.WARNING)).queue();
                    return;
                }
                
                StringBuilder name = new StringBuilder();
                
                if(args[0].equalsIgnoreCase("help")) {
                    for(int i = 1; i < args.length; i++) {
                        String s = args[i];
                        if(name.length() > 0)
                            name.append(":");
                        
                        name.append(s);
                        Option option = Option.getOptionMap().get(name.toString());
                        
                        if(option != null) {
                            try {
                                EmbedBuilder builder = new EmbedBuilder()
                                                               .setAuthor(option.getOptionName(), null, event.getAuthor().getEffectiveAvatarUrl())
                                                               .setDescription(option.getDescription())
                                                               .setThumbnail("https://i.imgur.com/lFTJSE4.png")
                                                               .addField("Type", option.getType().toString(), false);
                                channel.sendMessage(builder.build()).queue();
                            } catch(IndexOutOfBoundsException ignored) {
                            }
                            return;
                        }
                    }
                    channel.sendMessageFormat(languageContext.get("commands.opts.option_not_found"), EmoteReference.ERROR).queue(
                            message -> message.delete().queueAfter(10, TimeUnit.SECONDS)
                    );
                    
                    return;
                }
                
                for(int i = 0; i < args.length; i++) {
                    String s = args[i];
                    if(name.length() > 0) name.append(":");
                    name.append(s);
                    Option option = Option.getOptionMap().get(name.toString());
                    
                    if(option != null) {
                        TriConsumer<GuildMessageReceivedEvent, String[], I18nContext> callable = Option.getOptionMap().get(name.toString()).getEventConsumer();
                        try {
                            String[] a;
                            if(++i < args.length)
                                a = Arrays.copyOfRange(args, i, args.length);
                            else
                                a = new String[0];
                            
                            callable.accept(event, a, new I18nContext(MantaroData.db().getGuild(event.getGuild()).getData(), MantaroData.db().getUser(event.getAuthor().getId()).getData()));
                            Player p = MantaroData.db().getPlayer(event.getAuthor());
                            
                            if(p.getData().addBadgeIfAbsent(Badge.DID_THIS_WORK)) {
                                p.saveAsync();
                            }
                        } catch(IndexOutOfBoundsException ignored) {
                        }
                        return;
                    }
                }
                
                channel.sendMessage(String.format(languageContext.get("options.error_general"), EmoteReference.WARNING)).queue();
            }
            
            
            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                               .setDescription("This command allows you to change Mantaro settings for this server.\n" +
                                                       "All values set are local and NOT global, meaning that they will only effect this server. " +
                                                       "No, you can't give away currency or give yourself coins or anything like that.")
                               .setUsage("Check https://github.com/Mantaro/MantaroBot/wiki/Configuration for a guide on how to use opts. Welcome to the jungle.")
                               .build();
            }
        }).addOption("check:data", new Option("Data check.",
                "Checks the data values you have set on this server. **THIS IS NOT USER-FRIENDLY**", OptionType.GENERAL)
                                           .setActionLang((event, args, lang) -> {
                                               DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                                               GuildData guildData = dbGuild.getData();
                    
                                               //Map as follows: name, value
                                               //This filters out unused configs.
                                               Map<String, Pair<String, Object>> fieldMap = mapConfigObjects(guildData);
                    
                                               if(fieldMap == null) {
                                                   event.getChannel().sendMessage(String.format(lang.get("options.check_data.retrieve_failure"), EmoteReference.ERROR)).queue();
                                                   return;
                                               }
                    
                                               Map<String, String> opts = StringUtils.parse(args);
                                               if(opts.containsKey("print")) {
                                                   StringBuilder builder = new StringBuilder();
                                                   for(Entry<String, Pair<String, Object>> e : fieldMap.entrySet()) {
                                                       builder.append("* ").append(e.getKey()).append(": ").append(e.getValue().getRight()).append("\n");
                                                   }
                        
                                                   event.getChannel().sendMessage("Send this: " + Utils.paste3(builder.toString())).queue();
                                                   return;
                                               }
                    
                                               EmbedBuilder embedBuilder = new EmbedBuilder();
                                               embedBuilder.setAuthor("Option Debug", null, event.getAuthor().getEffectiveAvatarUrl())
                                                       .setDescription(String.format(lang.get("options.check_data.header") + lang.get("options.check_data.terminology"), event.getGuild().getName()))
                                                       .setThumbnail(event.getGuild().getIconUrl())
                                                       .setFooter(lang.get("options.check_data.footer"), null);
                                               List<MessageEmbed.Field> fields = new LinkedList<>();
                    
                                               for(Entry<String, Pair<String, Object>> e : fieldMap.entrySet()) {
                                                   fields.add(new MessageEmbed.Field(EmoteReference.BLUE_SMALL_MARKER + e.getKey() + ":\n" + e.getValue().getLeft() + "",
                                                           e.getValue() == null ? lang.get("options.check_data.null_set") : String.valueOf(e.getValue().getRight()),
                                                           false)
                                                   );
                                               }
                    
                                               List<List<MessageEmbed.Field>> splitFields = DiscordUtils.divideFields(6, fields);
                                               boolean hasReactionPerms = event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_ADD_REACTION);
                    
                                               if(hasReactionPerms)
                                                   DiscordUtils.list(event, 100, false, embedBuilder, splitFields);
                                               else
                                                   DiscordUtils.listText(event, 100, false, embedBuilder, splitFields);
                                           }).setShortDescription("Checks the data values you have set on this server.")
        ).addOption("reset:all", new Option("Options reset.",
                "Resets all options set on this server.", OptionType.GENERAL)
                                         .setActionLang((event, lang) -> {
                                                     //Temporary stuff.
                                                     DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                                                     GuildData temp = MantaroData.db().getGuild(event.getGuild()).getData();
                            
                                                     //The persistent data we wish to maintain.
                                                     String premiumKey = temp.getPremiumKey();
                                                     long quoteLastId = temp.getQuoteLastId();
                                                     long ranPolls = temp.getQuoteLastId();
                                                     String gameTimeoutExpectedAt = temp.getGameTimeoutExpectedAt();
                                                     long cases = temp.getCases();
                            
                                                     //Assign everything all over again
                                                     DBGuild newDbGuild = DBGuild.of(dbGuild.getId(), dbGuild.getPremiumUntil());
                                                     GuildData newTmp = newDbGuild.getData();
                                                     newTmp.setGameTimeoutExpectedAt(gameTimeoutExpectedAt);
                                                     newTmp.setRanPolls(ranPolls);
                                                     newTmp.setCases(cases);
                                                     newTmp.setPremiumKey(premiumKey);
                                                     newTmp.setQuoteLastId(quoteLastId);
                            
                                                     //weee
                                                     newDbGuild.saveAsync();
                            
                                                     event.getChannel().sendMessage(String.format(lang.get("options.reset_all.success"), EmoteReference.CORRECT)).queue();
                                                 }
                                         ));
    }
}
