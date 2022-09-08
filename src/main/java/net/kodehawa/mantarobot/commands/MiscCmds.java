/*
 * Copyright (C) 2016 Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.kodehawa.mantarobot.commands.interaction.polls.Poll;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.*;
import net.kodehawa.mantarobot.core.command.slash.IContext;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

@Module
public class MiscCmds {
    private final static Pattern pollOptionSeparator = Pattern.compile(",\\s*");

    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(IAm.class);
        cr.registerSlash(CreatePoll.class);
        cr.registerSlash(EightBall.class);
    }

    @Name("iam")
    @Description("Get or remove custom autoroles from you.")
    @Category(CommandCategory.UTILS)
    @Help(
            description = "Get or remove an autorole that your server administrators have set up.",
            usage = """
                    /iam add name:[iam role name] - Get the autorole with the specified name.
                    /iam not name:[iam role name] - Remove the autorole with the specified name.
                    /iam list - Check the list of autoroles.
                    """
    )
    public static class IAm extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {}

        @Name("add")
        @Description("Get an autorole assigned to you.")
        @Options({
                @Options.Option(type = OptionType.STRING, name = "role", description = "The role to assign.", required = true)
        })
        public static class Add extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                iamFunction(ctx.getOptionAsString("role"), ctx);
            }
        }

        @Name("not")
        @Description("Remove an autorole from you.")
        @Options({
                @Options.Option(type = OptionType.STRING, name = "role", description = "The role to remove.", required = true)
        })
        public static class Not extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                iamnotFunction(ctx.getOptionAsString("role"), ctx);
            }
        }

        @Name("list")
        @Description("List all autoroles.")
        public static class ListAll extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                List<MessageEmbed.Field> fields = new LinkedList<>();
                var dbGuild = ctx.getDBGuild();
                var guildData = dbGuild.getData();
                var languageContext = ctx.getLanguageContext();
                var autoroles = guildData.getAutoroles();
                var autorolesCategories = guildData.getAutoroleCategories();

                var embed = new EmbedBuilder()
                        .setAuthor(languageContext.get("commands.iam.list.header"), null, ctx.getMember().getEffectiveAvatarUrl())
                        .setColor(ctx.getMemberColor())
                        .setDescription(languageContext.get("commands.iam.list.description") + "")
                        .setThumbnail(ctx.getGuild().getIconUrl());

                var emptyEmbed = new EmbedBuilder()
                        .setAuthor(languageContext.get("commands.iam.list.header"), null, ctx.getMember().getEffectiveAvatarUrl())
                        .setColor(ctx.getMemberColor())
                        .setDescription(languageContext.get("commands.iam.list.no_autoroles"))
                        .setThumbnail(ctx.getGuild().getIconUrl());

                if (autoroles.size() > 0) {
                    var categorizedRoles = new ArrayList<>();
                    autorolesCategories.forEach((cat, roles) -> {
                        var roleString = new StringBuilder();

                        for (var iam : roles) {
                            var roleId = autoroles.get(iam);
                            if (roleId != null) {
                                Role role = ctx.getGuild().getRoleById(roleId);
                                if (role == null) {
                                    continue;
                                }

                                roleString.append(languageContext.get("commands.iam.list.role")).append(" `")
                                        .append(iam)
                                        .append("`, ")
                                        .append(languageContext.get("commands.iam.list.role_give"))
                                        .append(" `")
                                        .append(role.getName())
                                        .append("`\n");

                                categorizedRoles.add(role.getId());
                            }
                        }

                        if (roleString.length() > 0) {
                            fields.add(new MessageEmbed.Field(
                                    languageContext.get("commands.iam.list.category") + " " + cat,  roleString.toString(), false)
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

                    if (fields.isEmpty()) {
                        ctx.reply(emptyEmbed.build());
                        return;
                    }

                    DiscordUtils.sendPaginatedEmbed(ctx.getUtilsContext(), embed, DiscordUtils.divideFields(6, fields));
                } else {
                    ctx.reply(emptyEmbed.build());
                }
            }
        }
    }

    @Defer
    @Name("8ball")
    @Category(CommandCategory.FUN)
    @Description("Retrieves an answer from the almighty 8ball.")
    @Options(@Options.Option(type = OptionType.STRING, name = "question", description = "The question to ask.", required = true))
    public static class EightBall extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var question = ctx.getOptionAsString("question");
            var textEncoded = URLEncoder.encode(question.replace("/", "|"), StandardCharsets.UTF_8);
            var json = Utils.httpRequest("https://8ball.delegator.com/magic/JSON/%1s".formatted(textEncoded));

            if (json == null) {
                ctx.reply("commands.8ball.error", EmoteReference.ERROR);
                return;
            }

            String answer = new JSONObject(json).getJSONObject("magic").getString("answer");
            ctx.reply("\uD83D\uDCAC " + answer + ".");
        }
    }

    @Name("poll")
    @Description("Creates a poll.")
    @Category(CommandCategory.UTILS)
    @Options({
            @Options.Option(type = OptionType.STRING, name = "name", description = "The poll name.", required = true),
            @Options.Option(type = OptionType.STRING, name = "time", description = "The time the poll will run for. (Format example: 1m25s for 1 minute 25 seconds)", required = true),
            @Options.Option(type = OptionType.STRING, name = "options", description = "The poll options, separated by commas.", required = true),
            @Options.Option(type = OptionType.STRING, name = "image", description = "An image URL for the poll."),
    })
    @Help(description = "Creates a poll.", usage = """
            `/poll name:<name> time:<time> options:<poll options> image:[image url]`
            To cancel the running poll type &cancelpoll. Only the person who started it or an Admin can cancel it.
            The bot needs to be able to send and read messages on the channel this is ran for this to work.
            Example: `/poll name:test poll time:10m30s options:"hi there","wew","owo what's this"`
            """, parameters = {
                @Help.Parameter(name = "name", description = "The name of the option."),
                @Help.Parameter(name = "time", description = "The time the poll is gonna run for. The format is as follows `1m30s` for 1 minute and 30 seconds. Maximum poll runtime is 45 minutes."),
                @Help.Parameter(name = "options", description = """
                        The options to add. Minimum is 2 and maximum is 9.
                        For instance: `Pizza,Spaghetti,Pasta,"Spiral Nudels"` (Enclose options with multiple words in double quotes, there has to be no spaces between the commas)
                        """),
                @Help.Parameter(name = "image", description = "The image to embed to the poll.", optional = true)

            }
    )
    public static class CreatePoll extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {
            var builder = Poll.builder();
            var options = pollOptionSeparator.split(ctx.getOptionAsString("options").replaceAll(String.valueOf('"'), ""));
            long timeout;

            try {
                timeout = Utils.parseTime(ctx.getOptionAsString("time"));
            } catch (Exception e) {
                ctx.reply("commands.poll.incorrect_time_format", EmoteReference.ERROR);
                return;
            }

            if (timeout == 0) {
                ctx.reply("commands.poll.incorrect_time_format", EmoteReference.ERROR);
                return;
            }

            var image = ctx.getOptionAsString("image");
            if (image != null && !image.isBlank()) {
                builder.setImage(image);
            }

            builder.setEvent(ctx.getEvent())
                    .setName(ctx.getOptionAsString("name"))
                    .setTimeout(timeout)
                    .setOptions(options)
                    .setLanguage(ctx.getLanguageContext())
                    .build()
                    .startPoll(ctx);
        }
    }

    public static void iamFunction(String autoroleName, IContext ctx, String message) {
        var dbGuild = ctx.db().getGuild(ctx.getGuild());
        var autoroles = dbGuild.getData().getAutoroles();

        if (autoroles.containsKey(autoroleName)) {
            var role = ctx.getGuild().getRoleById(autoroles.get(autoroleName));
            if (role == null) {
                ctx.sendLocalized("commands.iam.deleted_role", EmoteReference.ERROR);

                // delete the non-existent autorole.
                dbGuild.getData().getAutoroles().remove(autoroleName);
                dbGuild.saveAsync();
            } else {
                if (ctx.getMember().getRoles().stream().anyMatch(r1 -> r1.getId().equals(role.getId()))) {
                    ctx.sendLocalized("commands.iam.already_assigned", EmoteReference.ERROR);
                    return;
                }

                if(Utils.isRoleAdministrative(role)) {
                    ctx.sendLocalized("commands.iam.privileged_role", EmoteReference.ERROR);
                    return;
                }

                try {
                    ctx.getGuild().addRoleToMember(ctx.getMember(), role)
                            .reason("Auto-assignable roles assigner (/iam)")
                            .queue(aVoid -> {
                                if (message == null || message.isEmpty())
                                    ctx.sendLocalized("commands.iam.success", EmoteReference.OK, ctx.getAuthor().getName(), role.getName());
                                else
                                    //Simple stuff for custom commands. (iamcustom:)
                                    ctx.send(message);
                            });
                } catch (PermissionException pex) {
                    ctx.sendLocalized("commands.iam.error", EmoteReference.ERROR, role.getName());
                }
            }
        } else {
            ctx.sendLocalized("commands.iam.no_role", EmoteReference.ERROR);
        }
    }

    public static void iamnotFunction(String autoroleName, IContext ctx, String message) {
        var dbGuild = ctx.db().getGuild(ctx.getGuild());
        var autoroles = dbGuild.getData().getAutoroles();

        if (autoroles.containsKey(autoroleName)) {
            Role role = ctx.getGuild().getRoleById(autoroles.get(autoroleName));
            if (role == null) {
                ctx.sendLocalized("commands.iam.deleted_role", EmoteReference.ERROR);
                dbGuild.getData().getAutoroles().remove(autoroleName);
                dbGuild.saveAsync();
            } else {
                if (ctx.getMember().getRoles().stream().noneMatch(r1 -> r1.getId().equals(role.getId()))) {
                    ctx.sendLocalized("commands.iamnot.not_assigned", EmoteReference.ERROR);
                    return;
                }

                try {
                    ctx.getGuild().removeRoleFromMember(ctx.getMember(), role).queue(__ -> {
                        if (message == null || message.isEmpty())
                            ctx.sendLocalized("commands.iamnot.success", EmoteReference.OK, ctx.getAuthor().getName(), role.getName());
                        else
                            ctx.sendLocalized(message);
                    });
                } catch (PermissionException pex) {
                    ctx.sendLocalized("commands.iam.error", EmoteReference.ERROR, role.getName());
                }
            }
        } else {
            ctx.sendLocalized("commands.iam.no_role", EmoteReference.ERROR);
        }
    }

    public static void iamFunction(String autoroleName, IContext ctx) {
        iamFunction(autoroleName, ctx, null);
    }

    public static void iamnotFunction(String autoroleName, IContext ctx) {
        iamnotFunction(autoroleName, ctx, null);
    }
}
