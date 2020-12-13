/*
 * Copyright (C) 2016-2020 David Rubio Escares / Kodehawa
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
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.kodehawa.mantarobot.commands.interaction.polls.Poll;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.SimpleFileDataManager;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

@Module
public class MiscCmds {
    private final DataManager<List<String>> facts = new SimpleFileDataManager("assets/mantaro/texts/facts.txt");
    private final Random rand = new Random();
    private final Pattern pollOptionSeparator = Pattern.compile(",\\s*");

    public static void iamFunction(String autoroleName, Context ctx) {
        iamFunction(autoroleName, ctx, null);
    }

    public static void iamFunction(String autoroleName, Context ctx, String message) {
        var dbGuild = ctx.getDBGuild();
        var autoroles = dbGuild.getData().getAutoroles();

        if (autoroles.containsKey(autoroleName)) {
            var role = ctx.getGuild().getRoleById(autoroles.get(autoroleName));
            if (role == null) {
                ctx.sendLocalized("commands.iam.deleted_role", EmoteReference.ERROR);

                //delete the non-existent autorole.
                dbGuild.getData().getAutoroles().remove(autoroleName);
                dbGuild.saveAsync();
            } else {
                if (ctx.getMember().getRoles().stream().anyMatch(r1 -> r1.getId().equals(role.getId()))) {
                    ctx.sendLocalized("commands.iam.already_assigned", EmoteReference.ERROR);
                    return;
                }
                try {
                    ctx.getGuild().addRoleToMember(ctx.getMember(), role)
                            //don't translate the reason!
                            .reason("Auto-assignable roles assigner (~>iam)")
                            .queue(aVoid -> {
                                if (message == null || message.isEmpty())
                                    ctx.sendLocalized("commands.iam.success", EmoteReference.OK, ctx.getAuthor().getName(), role.getName());
                                else
                                    //Simple stuff for custom commands. (iamcustom:)
                                    ctx.sendStripped(message);
                            });
                } catch (PermissionException pex) {
                    ctx.sendLocalized("commands.iam.error", EmoteReference.ERROR, role.getName());
                }
            }
        } else {
            ctx.sendStrippedLocalized("commands.iam.no_role", EmoteReference.ERROR, autoroleName);
        }
    }

    public static void iamnotFunction(String autoroleName, Context ctx) {
        iamnotFunction(autoroleName, ctx, null);
    }

    public static void iamnotFunction(String autoroleName, Context ctx, String message) {
        var dbGuild = ctx.getDBGuild();
        var autoroles = dbGuild.getData().getAutoroles();

        if (autoroles.containsKey(autoroleName)) {
            Role role = ctx.getGuild().getRoleById(autoroles.get(autoroleName));
            if (role == null) {
                ctx.sendLocalized("commands.iam.deleted_role", EmoteReference.ERROR);

                //delete the non-existent autorole.
                dbGuild.getData().getAutoroles().remove(autoroleName);
                dbGuild.saveAsync();
            } else {
                if (ctx.getMember().getRoles().stream().noneMatch(r1 -> r1.getId().equals(role.getId()))) {
                    ctx.sendLocalized("commands.iamnot.not_assigned", EmoteReference.ERROR);
                    return;
                }

                try {
                    ctx.getGuild().removeRoleFromMember(ctx.getMember(), role)
                            .queue(aVoid -> {
                                if (message == null || message.isEmpty())
                                    ctx.sendLocalized("commands.iamnot.success", EmoteReference.OK, ctx.getAuthor().getName(), role.getName());
                                else
                                    ctx.sendStrippedLocalized(message);
                            });
                } catch (PermissionException pex) {
                    ctx.sendLocalized("commands.iam.error", EmoteReference.ERROR, role.getName());
                }
            }
        } else {
            ctx.sendStrippedLocalized("commands.iam.no_role", EmoteReference.ERROR, autoroleName);
        }
    }

    @Subscribe
    public void eightBall(CommandRegistry cr) {
        cr.register("8ball", new SimpleCommand(CommandCategory.FUN) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.8ball.no_args", EmoteReference.ERROR);
                    return;
                }

                var textEncoded = URLEncoder.encode(content.replace("/", "|"), StandardCharsets.UTF_8);
                var json = Utils.httpRequest("https://8ball.delegator.com/magic/JSON/%1s".formatted(textEncoded));

                if (json == null) {
                    ctx.sendLocalized("commands.8ball.error", EmoteReference.ERROR);
                    return;
                }

                String answer = new JSONObject(json).getJSONObject("magic").getString("answer");
                ctx.send("\uD83D\uDCAC " + answer + ".");
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
        TreeCommand iamCommand = cr.register("iam", new TreeCommand(CommandCategory.UTILS) {
            @Override
            public Command defaultTrigger(Context ctx, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, I18nContext languageContext, String content) {
                        if (content.trim().isEmpty()) {
                            ctx.sendLocalized("commands.iam.no_iam", EmoteReference.ERROR);
                            return;
                        }

                        iamFunction(content.trim().replace("\"", ""), ctx);
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
            protected void call(Context ctx, I18nContext languageContext, String content) {
                EmbedBuilder embed;
                List<MessageEmbed.Field> fields = new LinkedList<>();

                var dbGuild = ctx.getDBGuild();
                var guildData = dbGuild.getData();

                var autoroles = guildData.getAutoroles();
                var autorolesCategories = guildData.getAutoroleCategories();
                embed = baseEmbed(ctx, languageContext.get("commands.iam.list.header"))
                        .setDescription(languageContext.get("commands.iam.list.description") + "")
                        .setThumbnail(ctx.getGuild().getIconUrl());

                if (autoroles.size() > 0) {
                    var categorizedRoles = new ArrayList<>();
                    autorolesCategories.forEach((cat, roles) -> {
                        var roleString = new StringBuilder();

                        for (var iam : roles) {
                            var roleId = autoroles.get(iam);
                            if (roleId != null) {
                                Role role = ctx.getGuild().getRoleById(roleId);
                                if (role == null)
                                    continue;

                                roleString.append("`").append(iam).append("`. Gives role: ").append(role.getName()).append(", ");
                                categorizedRoles.add(role.getId());
                            }
                        }

                        if (roleString.length() > 0) {
                            fields.add(new MessageEmbed.Field(
                                    cat,
                                    languageContext.get("commands.iam.list.role") + " `" + roleString + "`",
                                    false)
                            );
                        }
                    });

                    autoroles.forEach((name, roleId) -> {
                        if (!categorizedRoles.contains(roleId)) {
                            var role = ctx.getGuild().getRoleById(roleId);
                            if (role != null) {
                                fields.add(new MessageEmbed.Field(name,
                                        languageContext.get("commands.iam.list.role") + " `" + role.getName() + "`",
                                        false)
                                );
                            }
                        }
                    });

                    DiscordUtils.sendPaginatedEmbed(ctx, embed, DiscordUtils.divideFields(6, fields));
                } else {
                    embed = baseEmbed(ctx, languageContext.get("commands.iam.list.header"))
                            .setThumbnail(ctx.getGuild().getIconUrl())
                            .setDescription(languageContext.get("commands.iam.list.no_autoroles"));

                    ctx.send(embed.build());
                }
            }
        });

        cr.registerAlias("iam", "autoroles");
        iamCommand.createSubCommandAlias("ls", "list");
        iamCommand.createSubCommandAlias("ls", "Is");
    }

    @Subscribe
    public void iamnot(CommandRegistry cr) {
        cr.register("iamnot", new SimpleCommand(CommandCategory.UTILS) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (content.isEmpty()) {
                    ctx.sendLocalized("commands.iamnot.no_args", EmoteReference.ERROR);
                    return;
                }

                iamnotFunction(content.trim().replace("\"", ""), ctx);
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
        cr.register("randomfact", new SimpleCommand(CommandCategory.UTILS) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                ctx.send(EmoteReference.TALKING + facts.get().get(rand.nextInt(facts.get().size() - 1)));
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
        registry.register("createpoll", new SimpleCommand(CommandCategory.UTILS) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                var opts = ctx.getOptionalArguments();
                var builder = Poll.builder();
                var failure = (!opts.containsKey("time") || opts.get("time") == null) ||
                        (!opts.containsKey("options") || opts.get("options") == null) ||
                        (!opts.containsKey("name") || opts.get("name") == null);

                if (failure) {
                    ctx.sendLocalized("commands.poll.missing", EmoteReference.ERROR,
                            "`-time`",
                            "Example: `~>poll -options \"hi there\",\"wew\",\"owo what's this\" -time 10m20s -name \"test poll\""
                    );
                    return;
                }

                if (opts.containsKey("name") && opts.get("name") != null) {
                    builder.setName(opts.get("name").replaceAll(String.valueOf('"'), ""));
                }

                if (opts.containsKey("image") && opts.get("image") != null) {
                    builder.setImage(opts.get("image"));
                }


                var options = pollOptionSeparator.split(opts.get("options").replaceAll(String.valueOf('"'), ""));
                long timeout;

                try {
                    timeout = Utils.parseTime(opts.get("time"));
                } catch (Exception e) {
                    ctx.sendLocalized("commands.poll.incorrect_time_format", EmoteReference.ERROR);
                    return;
                }

                builder.setEvent(ctx.getEvent())
                        .setTimeout(timeout)
                        .setOptions(options)
                        .setLanguage(ctx.getLanguageContext())
                        .build()
                        .startPoll(ctx);
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Creates a poll.")
                        .setUsage("""
                                `~>poll [-options <options>] [-time <time>] [-name <name>] [-image <image>]`
                                To cancel the running poll type &cancelpoll. Only the person who started it or an Admin can cancel it.
                                Example: `~>poll -options "hi there","wew","owo what's this" -time 10m20s -name "test poll"`""")
                        .addParameter("-options", "The options to add. Minimum is 2 and maximum is 9. " +
                                "For instance: `Pizza,Spaghetti,Pasta,\"Spiral Nudels\"` " +
                                "(Enclose options with multiple words in double quotes, there has to be no spaces between the commas)")
                        .addParameter("time", "The time the operation is gonna take. " +
                                "The format is as follows `1m29s` for 1 minute and 21 seconds. Maximum poll runtime is 45 minutes.")
                        .addParameter("-name", "The name of the poll.")
                        .addParameter("-image", "The image to embed to the poll.")
                        .build();
            }
        });

        registry.registerAlias("createpoll", "poll");
    }
}
