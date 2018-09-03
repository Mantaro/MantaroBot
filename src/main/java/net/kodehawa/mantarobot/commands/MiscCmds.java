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

import br.com.brjdevs.java.utils.texts.StringUtils;
import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.kodehawa.mantarobot.commands.interaction.polls.Poll;
import net.kodehawa.mantarobot.commands.interaction.polls.PollBuilder;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SimpleTreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.ITreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static br.com.brjdevs.java.utils.collections.CollectionUtils.random;

@Module
@Slf4j
@SuppressWarnings("unused")
public class MiscCmds {
    private final String[] HEX_LETTERS = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};
    private final DataManager<List<String>> facts = new SimpleFileDataManager("assets/mantaro/texts/facts.txt");
    private final Random rand = new Random();
    private final Pattern pollOptionSeparator = Pattern.compile(",\\s*");

    public static void iamFunction(String autoroleName, GuildMessageReceivedEvent event, I18nContext languageContext) {
        DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
        Map<String, String> autoroles = dbGuild.getData().getAutoroles();

        if(autoroles.containsKey(autoroleName)) {
            Role role = event.getGuild().getRoleById(autoroles.get(autoroleName));
            if(role == null) {
                event.getChannel().sendMessageFormat(languageContext.get("commands.iam.deleted_role"), EmoteReference.ERROR).queue();

                //delete the non-existent autorole.
                dbGuild.getData().getAutoroles().remove(autoroleName);
                dbGuild.saveAsync();
            } else {
                if(event.getMember().getRoles().stream().filter(r1 -> r1.getId().equals(role.getId())).collect(Collectors.toList()).size() > 0) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.iam.already_assigned"), EmoteReference.ERROR).queue();
                    return;
                }
                try {
                    event.getGuild().getController().addSingleRoleToMember(event.getMember(), role)
                            //don't translate the reason!
                            .reason("Auto-assignable roles assigner (~>iam)")
                            .queue(aVoid -> event.getChannel().sendMessageFormat(languageContext.get("commands.iam.success"), EmoteReference.OK, event.getMember().getEffectiveName(), role.getName()).queue());
                } catch(PermissionException pex) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.iam.error"), EmoteReference.ERROR, role.getName()).queue();
                }
            }
        } else {
            event.getChannel().sendMessageFormat(languageContext.get("commands.iam.no_role"), EmoteReference.ERROR, autoroleName).queue();
        }
    }

    public static void iamnotFunction(String autoroleName, GuildMessageReceivedEvent event, I18nContext languageContext) {
        DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
        Map<String, String> autoroles = dbGuild.getData().getAutoroles();

        if(autoroles.containsKey(autoroleName)) {
            Role role = event.getGuild().getRoleById(autoroles.get(autoroleName));
            if(role == null) {
                event.getChannel().sendMessageFormat(languageContext.get("commands.iam.deleted_role"), EmoteReference.ERROR).queue();

                //delete the non-existent autorole.
                dbGuild.getData().getAutoroles().remove(autoroleName);
                dbGuild.saveAsync();
            } else {
                if(!(event.getMember().getRoles().stream().filter(r1 -> r1.getId().equals(role.getId())).collect(Collectors.toList()).size() > 0)) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.iamnot.not_assigned"), EmoteReference.ERROR).queue();
                    return;
                }
                try {
                    event.getGuild().getController().removeRolesFromMember(event.getMember(), role)
                            .queue(aVoid -> event.getChannel().sendMessageFormat(languageContext.get("commands.iamnot.sucess"), EmoteReference.OK, event.getMember().getEffectiveName(), role.getName()).queue());
                } catch(PermissionException pex) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.iam.error"), EmoteReference.ERROR, role.getName()).queue();
                }
            }
        } else {
            event.getChannel().sendMessageFormat(languageContext.get("commands.iam.no_role"), EmoteReference.ERROR, autoroleName).queue();
        }
    }

    @Subscribe
    public void eightBall(CommandRegistry cr) {
        cr.register("8ball", new SimpleCommand(Category.MISC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(content.isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.8ball.no_args"), EmoteReference.ERROR).queue();
                    return;
                }

                String textEncoded;
                String answer;
                try {
                    textEncoded = URLEncoder.encode(content, "UTF-8");
                    String json = Utils.wgetResty(String.format("https://8ball.delegator.com/magic/JSON/%1s", textEncoded), event);
                    answer = new JSONObject(json).getJSONObject("magic").getString("answer");
                } catch(Exception exception) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.8ball.error"), EmoteReference.ERROR).queue();
                    return;
                }

                event.getChannel().sendMessage("\uD83D\uDCAC " + answer + ".").queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "8ball")
                        .setDescription("**Retrieves an answer from the almighty 8ball.**")
                        .addField("Usage",
                                "`~>8ball <question>` - **Retrieves an answer from 8ball based on the question or sentence provided.**",
                                false)
                        .build();
            }
        });

        cr.registerAlias("8ball", "8b");
    }

    @Subscribe
    public void iam(CommandRegistry cr) {
        cr.register("iam", new SimpleCommand(Category.MISC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                Map<String, String> autoroles = dbGuild.getData().getAutoroles();
                if(args.length == 0 || content.length() == 0) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.iam.no_iam"), EmoteReference.ERROR).queue();
                    return;
                }

                StringBuilder stringBuilder = new StringBuilder();
                if(content.equals("list") || content.equals("ls")) {
                    EmbedBuilder embed = null;
                    boolean hasReactionPerms = event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_ADD_REACTION);
                    if(!hasReactionPerms)
                        stringBuilder.append(languageContext.get("general.text_menu")).append("\n");

                    if(autoroles.size() > 0) {
                        autoroles.forEach((name, roleId) -> {
                            Role role = event.getGuild().getRoleById(roleId);
                            if(role != null) {
                                stringBuilder.append(languageContext.get("commands.iam.list.name")).append(name).append(languageContext.get("commands.iam.list.role")).append(role.getName()).append("**");
                            }
                        });

                        List<String> parts = DiscordUtils.divideString(MessageEmbed.TEXT_MAX_LENGTH, stringBuilder);
                        if(hasReactionPerms) {
                            DiscordUtils.list(event, 30, false, (current, max) -> baseEmbed(event, languageContext.get("commands.iam.list.header")), parts);
                        } else {
                            DiscordUtils.listText(event, 30, false, (current, max) -> baseEmbed(event, languageContext.get("commands.iam.list.header")), parts);
                        }
                    } else {
                        embed = baseEmbed(event, languageContext.get("commands.iam.list.header"));
                        embed.setDescription(languageContext.get("commands.iam.list.no_autoroles"));
                    }

                    if(embed != null) {
                        event.getChannel().sendMessage(embed.build()).queue();
                    }

                    return;
                }

                iamFunction(content.trim().replace("\"", ""), event, languageContext);
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Iam (autoroles)")
                        .setDescription("**Get an autorole that your server administrators have set up!**")
                        .addField("Usage", "`~>iam <name>` - **Get the role with the specified name**.\n"
                                + "`~>iam list` - **List all the available autoroles in this server**", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void iamnot(CommandRegistry cr) {
        cr.register("iamnot", new SimpleCommand(Category.MISC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if(content.isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.iamnot.no_args"), EmoteReference.ERROR).queue();
                    return;
                }

                iamnotFunction(content.trim().replace("\"", ""), event, languageContext);
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Iamnot (autoroles)")
                        .setDescription("**Remove an autorole from yourself that your server administrators have set up!**")
                        .addField("Usage", "~>iamnot <name>. Remove the role from yourself with the specified name.\n"
                                + "~>iamnot list. List all the available autoroles in this server", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void randomFact(CommandRegistry cr) {
        cr.register("randomfact", new SimpleCommand(Category.MISC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                event.getChannel().sendMessage(EmoteReference.TALKING + facts.get().get(rand.nextInt(facts.get().size() - 1))).queue();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Random Fact")
                        .setDescription("**Sends a random fact.**")
                        .build();
            }
        });

        cr.registerAlias("randomfact", "rf");
    }

    @Subscribe
    public void createPoll(CommandRegistry registry) {
        registry.register("createpoll", new SimpleCommand(Category.MISC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                Map<String, Optional<String>> opts = StringUtils.parse(args);
                PollBuilder builder = Poll.builder();
                if(!opts.containsKey("time") || !opts.get("time").isPresent()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.poll.missing"), EmoteReference.ERROR, "`-time`", "Example: `~>poll -options \"hi there\",\"wew\",\"owo what's this\" -time 10m20s -name \"test poll\"").queue();
                    return;
                }

                if(!opts.containsKey("options") || !opts.get("options").isPresent()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.poll.missing"), EmoteReference.ERROR, "`-options`", "Example: ~>poll -options \"hi there\",\"wew\",\"owo what's this\" -time 10m20s -name \"test poll\"").queue();
                    return;
                }

                if(!opts.containsKey("name") || !opts.get("name").isPresent()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.poll.missing"), EmoteReference.ERROR, "`-name`", "Example: ~>poll -options \"hi there\",\"wew\",\"owo what's this\" -time 10m20s -name \"test poll\"").queue();
                    return;
                }

                if(opts.containsKey("name") && opts.get("name").isPresent()) {
                    builder.setName(opts.get("name").get().replaceAll(String.valueOf('"'), ""));
                }

                if(opts.containsKey("image") && opts.get("image").isPresent()) {
                    builder.setImage(opts.get("image").get());
                }


                String[] options = pollOptionSeparator.split(opts.get("options").get().replaceAll(String.valueOf('"'), ""));
                long timeout = Utils.parseTime(opts.get("time").get());

                builder.setEvent(event)
                        .setTimeout(timeout)
                        .setOptions(options)
                        .setLanguage(languageContext)
                        .build()
                        .startPoll();
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Poll Command")
                        .setDescription("**Creates a poll**")
                        .addField("Usage", "`~>poll [-options <options>] [-time <time>] [-name <name>] [-image <image>]`", false)
                        .addField("Parameters", "`-options` The options to add. Minimum is 2 and maximum is 9. For instance: `Pizza,Spaghetti,Pasta,\"Spiral Nudels\"` (Enclose options with multiple words in double quotes).\n" +
                                "`-time` The time the operation is gonna take. The format is as follows `1m29s` for 1 minute and 21 seconds. Maximum poll runtime is 45 minutes.\n" +
                                "`-name` The name of the poll for reference.\n" +
                                "`-image` The image to embed to the poll. Optional.", false)
                        .addField("Considerations", "To cancel the running poll type &cancelpoll. Only the person who started it or an Admin can cancel it.", false)
                        .addField("Example", "~>poll -options \"hi there\",\"wew\",\"owo what's this\" -time 10m20s -name \"test poll\"", false)
                        .build();
            }
        });

        registry.registerAlias("createpoll", "poll");
    }

    /**
     * @return a random hex color.
     */
    private String randomColor() {
        return IntStream.range(0, 6).mapToObj(i -> random(HEX_LETTERS)).collect(Collectors.joining());
    }
}
