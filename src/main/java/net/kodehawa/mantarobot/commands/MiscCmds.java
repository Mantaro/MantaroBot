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
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.kodehawa.mantarobot.commands.interaction.polls.Poll;
import net.kodehawa.mantarobot.commands.interaction.polls.PollBuilder;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Module
@SuppressWarnings("unused")
public class MiscCmds {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(MiscCmds.class);
    private final String[] HEX_LETTERS = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F"};
    private final DataManager<List<String>> facts = new SimpleFileDataManager("assets/mantaro/texts/facts.txt");
    private final Random rand = new Random();
    private final Pattern pollOptionSeparator = Pattern.compile(",\\s*");

    public static void iamFunction(String autoroleName, GuildMessageReceivedEvent event, I18nContext languageContext) {
        iamFunction(autoroleName, event, languageContext, null);
    }

    public static void iamFunction(String autoroleName, GuildMessageReceivedEvent event, I18nContext languageContext, String message) {
        DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
        Map<String, String> autoroles = dbGuild.getData().getAutoroles();
        TextChannel channel = event.getChannel();

        if (autoroles.containsKey(autoroleName)) {
            Role role = event.getGuild().getRoleById(autoroles.get(autoroleName));
            if (role == null) {
                channel.sendMessageFormat(languageContext.get("commands.iam.deleted_role"), EmoteReference.ERROR).queue();

                //delete the non-existent autorole.
                dbGuild.getData().getAutoroles().remove(autoroleName);
                dbGuild.saveAsync();
            } else {
                if (event.getMember().getRoles().stream().anyMatch(r1 -> r1.getId().equals(role.getId()))) {
                    channel.sendMessageFormat(languageContext.get("commands.iam.already_assigned"), EmoteReference.ERROR).queue();
                    return;
                }
                try {
                    event.getGuild().addRoleToMember(event.getMember(), role)
                            //don't translate the reason!
                            .reason("Auto-assignable roles assigner (~>iam)")
                            .queue(aVoid -> {
                                if (message == null || message.isEmpty())
                                    channel.sendMessageFormat(languageContext.get("commands.iam.success"), EmoteReference.OK, event.getAuthor().getName(), role.getName()).queue();
                                else
                                    //Simple stuff for custom commands. (iamcustom:)
                                    new MessageBuilder()
                                            .append(message)
                                            .stripMentions(event.getGuild(), Message.MentionType.EVERYONE, Message.MentionType.HERE, Message.MentionType.ROLE)
                                            .sendTo(channel)
                                            .queue();
                            });
                } catch (PermissionException pex) {
                    channel.sendMessageFormat(languageContext.get("commands.iam.error"), EmoteReference.ERROR, role.getName()).queue();
                }
            }
        } else {
            new MessageBuilder().
                    append(String.format(languageContext.get("commands.iam.no_role"), EmoteReference.ERROR, autoroleName))
                    .stripMentions(event.getJDA())
                    .sendTo(channel)
                    .queue();
        }
    }

    public static void iamnotFunction(String autoroleName, GuildMessageReceivedEvent event, I18nContext languageContext) {
        iamnotFunction(autoroleName, event, languageContext, null);
    }

    public static void iamnotFunction(String autoroleName, GuildMessageReceivedEvent event, I18nContext languageContext, String message) {
        DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
        Map<String, String> autoroles = dbGuild.getData().getAutoroles();
        TextChannel channel = event.getChannel();

        if (autoroles.containsKey(autoroleName)) {
            Role role = event.getGuild().getRoleById(autoroles.get(autoroleName));
            if (role == null) {
                channel.sendMessageFormat(languageContext.get("commands.iam.deleted_role"), EmoteReference.ERROR).queue();

                //delete the non-existent autorole.
                dbGuild.getData().getAutoroles().remove(autoroleName);
                dbGuild.saveAsync();
            } else {
                if (event.getMember().getRoles().stream().noneMatch(r1 -> r1.getId().equals(role.getId()))) {
                    channel.sendMessageFormat(languageContext.get("commands.iamnot.not_assigned"), EmoteReference.ERROR).queue();
                    return;
                }
                try {
                    event.getGuild().removeRoleFromMember(event.getMember(), role)
                            .queue(aVoid -> {
                                if (message == null || message.isEmpty())
                                    channel.sendMessageFormat(languageContext.get("commands.iamnot.success"), EmoteReference.OK, event.getAuthor().getName(), role.getName()).queue();
                                else
                                    new MessageBuilder()
                                            .append(message)
                                            .stripMentions(event.getGuild(), Message.MentionType.EVERYONE, Message.MentionType.HERE, Message.MentionType.ROLE)
                                            .sendTo(channel)
                                            .queue();
                            });
                } catch (PermissionException pex) {
                    channel.sendMessageFormat(languageContext.get("commands.iam.error"), EmoteReference.ERROR, role.getName()).queue();
                }
            }
        } else {
            new MessageBuilder().
                    append(String.format(languageContext.get("commands.iam.no_role"), EmoteReference.ERROR, autoroleName))
                    .stripMentions(event.getJDA())
                    .sendTo(channel)
                    .queue();
        }
    }

    @Subscribe
    public void eightBall(CommandRegistry cr) {
        cr.register("8ball", new SimpleCommand(Category.MISC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                TextChannel channel = event.getChannel();

                if (content.isEmpty()) {
                    channel.sendMessageFormat(languageContext.get("commands.8ball.no_args"), EmoteReference.ERROR).queue();
                    return;
                }

                String textEncoded;
                String answer;
                try {
                    textEncoded = URLEncoder.encode(content.replace("/", "|"), StandardCharsets.UTF_8);
                    String json = Utils.wgetOkHttp(String.format("https://8ball.delegator.com/magic/JSON/%1s", textEncoded));
                    answer = new JSONObject(json).getJSONObject("magic").getString("answer");
                } catch (Exception exception) {
                    channel.sendMessageFormat(languageContext.get("commands.8ball.error"), EmoteReference.ERROR).queue();
                    return;
                }

                channel.sendMessage("\uD83D\uDCAC " + answer + ".").queue(); //owo
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Retrieves an answer from the almighty 8ball.")
                        .setUsage("`~>8ball <question>` - Retrieves an answer from 8ball based on the question or sentence provided.")
                        .addParameter("question", "The question to ask.")
                        .build();
            }
        });

        cr.registerAlias("8ball", "8b");
    }

    @Subscribe
    public void iam(CommandRegistry cr) {
        TreeCommand iamCommand = (TreeCommand) cr.register("iam", new TreeCommand(Category.MISC) {
            @Override
            public Command defaultTrigger(GuildMessageReceivedEvent event, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                        DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                        Map<String, String> autoroles = dbGuild.getData().getAutoroles();
                        if (content.trim().isEmpty()) {
                            event.getChannel().sendMessageFormat(languageContext.get("commands.iam.no_iam"), EmoteReference.ERROR).queue();
                            return;
                        }

                        iamFunction(content.trim().replace("\"", ""), event, languageContext);
                    }
                };
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Get an autorole that your server administrators have set up.")
                        .setUsage("`~>iam <name>` - Get the role with the specified name.\n"
                                + "`~>iam list` - List all the available autoroles in this server. Use this to check which autoroles you can get!")
                        .addParameter("name", "The name of the autorole to get.")
                        .build();
            }
        });

        iamCommand.addSubCommand("ls", new SubCommand() {
            @Override
            public String description() {
                return "Lists all the available autoroles for this server.";
            }

            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content) {
                TextChannel channel = event.getChannel();

                EmbedBuilder embed;
                DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                GuildData guildData = dbGuild.getData();

                Map<String, String> autoroles = guildData.getAutoroles();
                Map<String, List<String>> autorolesCategories = guildData.getAutoroleCategories();
                List<MessageEmbed.Field> fields = new LinkedList<>();
                StringBuilder stringBuilder = new StringBuilder();

                boolean hasReactionPerms = event.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_ADD_REACTION);

                if (!hasReactionPerms)
                    stringBuilder.append(languageContext.get("general.text_menu")).append("\n");

                embed = baseEmbed(event, languageContext.get("commands.iam.list.header"))
                        .setDescription(languageContext.get("commands.iam.list.description") + stringBuilder.toString())
                        .setThumbnail(event.getGuild().getIconUrl());

                if (autoroles.size() > 0) {
                    List<String> categorizedRoles = new ArrayList<>();
                    autorolesCategories.forEach((cat, roles) -> {
                        StringBuilder roleString = new StringBuilder();

                        for (String iam : roles) {
                            String roleId = autoroles.get(iam);
                            if (roleId != null) {
                                Role role = event.getGuild().getRoleById(roleId);
                                roleString.append("`").append(iam).append("`. Gives role: ").append(role.getName()).append(", ");
                                categorizedRoles.add(role.getId());
                            }
                        }

                        if (roleString.length() > 0)
                            fields.add(new MessageEmbed.Field(cat, languageContext.get("commands.iam.list.role") + " `" + roleString + "`", false));
                    });

                    autoroles.forEach((name, roleId) -> {
                        if (!categorizedRoles.contains(roleId)) {
                            Role role = event.getGuild().getRoleById(roleId);
                            if (role != null) {
                                fields.add(new MessageEmbed.Field(name, languageContext.get("commands.iam.list.role") + " `" + role.getName() + "`", false));
                            }
                        }
                    });

                    List<List<MessageEmbed.Field>> parts = DiscordUtils.divideFields(6, fields);
                    if (hasReactionPerms) {
                        DiscordUtils.list(event, 100, false, embed, parts);
                    } else {
                        DiscordUtils.listText(event, 100, false, embed, parts);
                    }
                } else {
                    embed = baseEmbed(event, languageContext.get("commands.iam.list.header"))
                            .setThumbnail(event.getGuild().getIconUrl())
                            .setDescription(languageContext.get("commands.iam.list.no_autoroles"));

                    channel.sendMessage(embed.build()).queue();
                }
            }
        });

        cr.registerAlias("iam", "autoroles");
        iamCommand.createSubCommandAlias("ls", "list");
        iamCommand.createSubCommandAlias("ls", "Is");
    }

    @Subscribe
    public void iamnot(CommandRegistry cr) {
        cr.register("iamnot", new SimpleCommand(Category.MISC) {
            @Override
            protected void call(GuildMessageReceivedEvent event, I18nContext languageContext, String content, String[] args) {
                if (content.isEmpty()) {
                    event.getChannel().sendMessageFormat(languageContext.get("commands.iamnot.no_args"), EmoteReference.ERROR).queue();
                    return;
                }

                iamnotFunction(content.trim().replace("\"", ""), event, languageContext);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Remove an autorole from yourself that your server administrators have set up.")
                        .setUsage("`~>iamnot <name>` - Remove the role from yourself with the specified name.")
                        .addParameter("name", "The name of the autorole to remove.")
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
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Sends a random fact.")
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
                TextChannel channel = event.getChannel();

                Map<String, String> opts = StringUtils.parse(args);
                PollBuilder builder = Poll.builder();
                if (!opts.containsKey("time") || opts.get("time") == null) {
                    channel.sendMessageFormat(languageContext.get("commands.poll.missing"), EmoteReference.ERROR, "`-time`", "Example: `~>poll -options \"hi there\",\"wew\",\"owo what's this\" -time 10m20s -name \"test poll\"").queue();
                    return;
                }

                if (!opts.containsKey("options") || opts.get("options") == null) {
                    channel.sendMessageFormat(languageContext.get("commands.poll.missing"), EmoteReference.ERROR, "`-options`", "Example: ~>poll -options \"hi there\",\"wew\",\"owo what's this\" -time 10m20s -name \"test poll\"").queue();
                    return;
                }

                if (!opts.containsKey("name") || opts.get("name") == null) {
                    channel.sendMessageFormat(languageContext.get("commands.poll.missing"), EmoteReference.ERROR, "`-name`", "Example: ~>poll -options \"hi there\",\"wew\",\"owo what's this\" -time 10m20s -name \"test poll\"").queue();
                    return;
                }

                if (opts.containsKey("name") && opts.get("name") != null) {
                    builder.setName(opts.get("name").replaceAll(String.valueOf('"'), ""));
                }

                if (opts.containsKey("image") && opts.get("image") != null) {
                    builder.setImage(opts.get("image"));
                }


                String[] options = pollOptionSeparator.split(opts.get("options").replaceAll(String.valueOf('"'), ""));
                long timeout;

                try {
                    timeout = Utils.parseTime(opts.get("time"));
                } catch (Exception e) {
                    channel.sendMessageFormat(languageContext.get("commands.poll.incorrect_time_format"), EmoteReference.ERROR).queue();
                    return;
                }

                builder.setEvent(event)
                        .setTimeout(timeout)
                        .setOptions(options)
                        .setLanguage(languageContext)
                        .build()
                        .startPoll();
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Creates a poll.")
                        .setUsage("`~>poll [-options <options>] [-time <time>] [-name <name>] [-image <image>]`\n" +
                                "To cancel the running poll type &cancelpoll. Only the person who started it or an Admin can cancel it.\n" +
                                "Example: `~>poll -options \"hi there\",\"wew\",\"owo what's this\" -time 10m20s -name \"test poll\"`")
                        .addParameter("-options", "The options to add. Minimum is 2 and maximum is 9. For instance: `Pizza,Spaghetti,Pasta,\"Spiral Nudels\"` (Enclose options with multiple words in double quotes, there has to be no spaces between the commas)")
                        .addParameter("time", "The time the operation is gonna take. The format is as follows `1m29s` for 1 minute and 21 seconds. Maximum poll runtime is 45 minutes.")
                        .addParameter("-name", "The name of the poll.")
                        .addParameter("-image", "The image to embed to the poll.")
                        .build();
            }
        });

        registry.registerAlias("createpoll", "poll");
    }

    /**
     * @return a random hex color.
     */
    private String randomColor() {
        return IntStream.range(0, 6).mapToObj(i -> HEX_LETTERS[rand.nextInt(HEX_LETTERS.length)]).collect(Collectors.joining());
    }
}
