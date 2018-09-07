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

import br.com.brjdevs.java.utils.functions.interfaces.TriConsumer;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.options.core.Option;
import net.kodehawa.mantarobot.options.core.OptionType;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Map.Entry;
import static net.kodehawa.mantarobot.utils.Utils.mapObjects;

@Module
@SuppressWarnings("unused")
public class OptsCmd {
    public static Command optsCmd;

    public static void onHelp(GuildMessageReceivedEvent event) {
        event.getChannel().sendMessage(String.format("%sHey, if you're lost or want help on using opts, check <https://github.com/Mantaro/MantaroBot/wiki/Configuration> for a guide on how to use opts.\nNote: Only administrators, people with Manage Server or people with the Bot Commander role can use this command!", EmoteReference.HEART)).queue();
    }

    public static SimpleCommand getOpts() {
        return (SimpleCommand) optsCmd;
    }

    @Subscribe
    public void register(CommandRegistry registry) {
        registry.register("opts", optsCmd = new SimpleCommand(Category.MODERATION, CommandPermission.ADMIN) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(args.length == 0) {
                    onHelp(event);
                    return;
                }

                if(args.length == 1 && args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("ls")) {
                    StringBuilder builder = new StringBuilder();

                    for(String s : Option.getAvaliableOptions()) {
                        builder.append(s).append("\n");
                    }

                    List<String> m = DiscordUtils.divideString(builder);
                    List<String> messages = new LinkedList<>();
                    boolean hasReactionPerms = event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_ADD_REACTION);
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
                    event.getChannel().sendMessage(help(event)).queue();
                    return;
                }

                StringBuilder name = new StringBuilder();

                if(args[0].equalsIgnoreCase("help")) {
                    for(int i = 1; i < args.length; i++) {
                        String s = args[i];
                        if(name.length() > 0) name.append(":");
                        name.append(s);
                        Option option = Option.getOptionMap().get(name.toString());

                        if(option != null) {
                            try {
                                EmbedBuilder builder = new EmbedBuilder()
                                        .setAuthor(option.getOptionName(), null, event.getAuthor().getEffectiveAvatarUrl())
                                        .setDescription(option.getDescription())
                                        .setThumbnail("https://cdn.pixabay.com/photo/2012/04/14/16/26/question-34499_960_720.png")
                                        .addField("Type", option.getType().toString(), false);
                                event.getChannel().sendMessage(builder.build()).queue();
                            } catch(IndexOutOfBoundsException ignored) {}
                            return;
                        }
                    }
                    event.getChannel().sendMessageFormat(languageContext.get("commands.opts.option_not_found"), EmoteReference.ERROR).queue(
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
                            if(++i < args.length) a = Arrays.copyOfRange(args, i, args.length);
                            else a = new String[0];
                            callable.accept(event, a, new I18nContext(MantaroData.db().getGuild(event.getGuild()).getData(), MantaroData.db().getUser(event.getAuthor().getId()).getData()));
                            Player p = MantaroData.db().getPlayer(event.getAuthor());
                            if(p.getData().addBadgeIfAbsent(Badge.DID_THIS_WORK)) {
                                p.saveAsync();
                            }
                        } catch(IndexOutOfBoundsException ignored) { }
                        return;
                    }
                }

                event.getChannel().sendMessageFormat(languageContext.get("commands.opts.invalid_args"), EmoteReference.ERROR).queue(
                        message -> message.delete().queueAfter(10, TimeUnit.SECONDS)
                );
                event.getChannel().sendMessage(help(event)).queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Options and Configurations Command")
                        .setDescription("**This command allows you to change Mantaro settings for this server.**\n" +
                                "All values set are local rather than global, meaning that they will only effect this server.\n" +
                                "- Hey, if you're lost or want help on using opts, check https://github.com/Mantaro/MantaroBot/wiki/Configuration for a guide on how to use opts.\nNote: Only administrators, people with Manage Server or people with the Bot Commander role can use this command!")
                        .build();
            }
        }).addOption("check:data", new Option("Data check.",
                "Checks the data values you have set on this server. **THIS IS NOT USER-FRIENDLY**", OptionType.GENERAL)
                .setActionLang((event, lang) -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    //Map as follows: name, value
                    Map<String, Object> fieldMap = mapObjects(guildData);

                    if(fieldMap == null) {
                        event.getChannel().sendMessage(String.format(lang.get("options.check_data.retrieve_failure"), EmoteReference.ERROR)).queue();
                        return;
                    }

                    StringBuilder show = new StringBuilder();
                    show.append(String.format(lang.get("options.check_data.header"), event.getGuild().getName()))
                            .append("\n\n");

                    AtomicInteger ai = new AtomicInteger();

                    for(Entry e : fieldMap.entrySet()) {
                        if(e.getKey().equals("localPlayerExperience")) {
                            continue;
                        }

                        show.append(ai.incrementAndGet())
                                .append(".- `")
                                .append(e.getKey())
                                .append("`");

                        if(e.getValue() == null) {
                            show.append(" **")
                                    .append(lang.get("options.check_data.null_set"))
                                    .append("**\n");
                        } else {
                            show.append(" **")
                                    .append(lang.get("options.check_data.set_to"))
                                    .append(" ")
                                    .append(e.getValue())
                                    .append("**\n");
                        }
                    }

                    List<String> toSend = DiscordUtils.divideString(1600, show);
                    toSend.forEach(message -> event.getChannel().sendMessage(message).queue());
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
