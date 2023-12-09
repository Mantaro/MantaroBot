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
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.kodehawa.mantarobot.commands.utils.polls.Poll;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.Category;
import net.kodehawa.mantarobot.core.command.meta.Defer;
import net.kodehawa.mantarobot.core.command.meta.Description;
import net.kodehawa.mantarobot.core.command.meta.Help;
import net.kodehawa.mantarobot.core.command.meta.Name;
import net.kodehawa.mantarobot.core.command.meta.Options;
import net.kodehawa.mantarobot.core.command.slash.IContext;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.command.meta.Module;
import net.kodehawa.mantarobot.core.command.helpers.CommandCategory;
import net.kodehawa.mantarobot.core.command.helpers.CommandPermission;
import net.kodehawa.mantarobot.core.command.i18n.I18nContext;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Module
public class MiscCmds {
    private static final Pattern pollOptionSeparator = Pattern.compile(",\\s*");

    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(IAm.class);
        cr.registerSlash(PollCommand.class);
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
                var languageContext = ctx.getLanguageContext();
                var autoroles = dbGuild.getAutoroles();
                var autorolesCategories = dbGuild.getAutoroleCategories();

                var embed = new EmbedBuilder()
                        .setAuthor(languageContext.get("commands.iam.list.header"), null, ctx.getMember().getEffectiveAvatarUrl())
                        .setColor(ctx.getMemberColor())
                        .setDescription(languageContext.get("commands.iam.list.description"))
                        .setThumbnail(ctx.getGuild().getIconUrl());

                var emptyEmbed = new EmbedBuilder()
                        .setAuthor(languageContext.get("commands.iam.list.header"), null, ctx.getMember().getEffectiveAvatarUrl())
                        .setColor(ctx.getMemberColor())
                        .setDescription(languageContext.get("commands.iam.list.no_autoroles"))
                        .setThumbnail(ctx.getGuild().getIconUrl());

                if (!autoroles.isEmpty()) {
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

                        if (!roleString.isEmpty()) {
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
        private final Random rand = new Random();
        private final List<String> answers = Arrays.asList(
                "It is certain.",
                "It is decidedly so.",
                "Without a doubt",
                "Yes - definitely.",
                "You may rely on it.",
                "As I see it, yes.",
                "Most likely.",
                "Outlook good.",
                "Yes.",
                "Signs point to yes.",
                "Reply hazy try again.",
                "Ask again later.",
                "Better not tell you now.",
                "Cannot predict now.",
                "Concentrate and ask again.",
                "Don't count on it.",
                "My reply is no.",
                "My sources say no.",
                "Outlook not so good.",
                "Very doubtful."
        );

        @Override
        protected void process(SlashContext ctx) {
            ctx.reply("\uD83D\uDCAC " + answers.get(rand.nextInt(answers.size())));
        }
    }

    @Name("poll")
    @Description("The hub for poll commands.")
    @Category(CommandCategory.UTILS)
    public static class PollCommand extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) { }

        @Override
        public Predicate<SlashContext> getPredicate() {
            return ctx -> {
                // We should also add some kind of configurable role, probably.
                if (!CommandPermission.ADMIN.test(ctx.getMember())) {
                    ctx.replyEphemeral("commands.poll.no_permissions", EmoteReference.ERROR);
                    return false;
                }

                return true;
            };
        }

        @Name("create")
        @Description("Creates a poll.")
        @Options({
                @Options.Option(type = OptionType.STRING, name = "name", description = "The poll name.", required = true),
                @Options.Option(type = OptionType.STRING, name = "time", description = "The time the poll will run for. (Format example: 1m25s for 1 minute 25 seconds)", required = true),
                @Options.Option(type = OptionType.STRING, name = "options", description = "The poll options, separated by commas.", required = true),
                @Options.Option(type = OptionType.STRING, name = "image", description = "An image URL for the poll."),
        })
        @Help(description = "Creates a poll.", usage = """
            `/poll create name:<name> time:<time> options:<poll options> image:[image url]`
            To cancel the running poll type &cancelpoll. Only the person who started it or an Admin can cancel it.
            The bot needs to be able to send and read messages on the channel this is ran for this to work.
            Example: `/poll create name:test poll time:10m30s options:"hi there","wew","owo what's this"`
            """, parameters = {
                @Help.Parameter(name = "name", description = "The name of the option."),
                @Help.Parameter(name = "time", description = "The time the poll is gonna run for. The format is as follows `1m30s` for 1 minute and 30 seconds. Maximum poll runtime is 45 minutes."),
                @Help.Parameter(name = "options", description = """
                        The options to add. Minimum is 2 and maximum is 9.
                        For instance: `Pizza,Spaghetti,Pasta,"Spiral Nudels"` (Enclose options with multiple words in double quotes, there has to be no spaces between the commas)
                        """),
                @Help.Parameter(name = "image", description = "The image to embed to the poll.", optional = true)

        })
        public static class CreatePoll extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                if (!ctx.getGuild().getSelfMember().hasPermission(ctx.getChannel(), Permission.MESSAGE_ADD_REACTION)) {
                    ctx.replyEphemeral("commands.poll.no_reaction_perms", EmoteReference.ERROR);
                    return;
                }

                var options = pollOptionSeparator.split(
                        ctx.getOptionAsString("options").replaceAll(String.valueOf('"'), "")
                );

                long timeout;
                try {
                    timeout = Utils.parseTime(ctx.getOptionAsString("time"));
                } catch (Exception e) {
                    ctx.replyEphemeral("commands.poll.incorrect_time_format", EmoteReference.ERROR);
                    return;
                }

                if (timeout == 0) {
                    ctx.replyEphemeral("commands.poll.incorrect_time_format", EmoteReference.ERROR);
                    return;
                }

                var name = ctx.getOptionAsString("name");
                if (name.length() > 500) {
                    ctx.replyEphemeral("commands.poll.name_too_long", EmoteReference.ERROR);
                    return;
                }

                if (timeout < TimeUnit.MINUTES.toMillis(2)) {
                    ctx.replyEphemeral("commands.poll.too_little_time", EmoteReference.ERROR);
                    return;
                }

                if (timeout > TimeUnit.DAYS.toMillis(2)) {
                    ctx.replyEphemeral("commands.poll.too_much_time", EmoteReference.ERROR);
                    return;
                }

                var guildData = ctx.getDBGuild();
                if (guildData.getRunningPolls().size() >= 5) {
                    ctx.replyEphemeral("commands.poll.too_many", EmoteReference.ERROR);
                    return;
                }

                if (options.length < 2) {
                    ctx.replyEphemeral("commands.poll.too_few_options", EmoteReference.ERROR);
                    return;
                }

                if (options.length > 9) {
                    ctx.replyEphemeral("commands.poll.too_many_options", EmoteReference.ERROR);
                    return;
                }

                try {
                    var builder = Poll.builder()
                            .guildId(ctx.getGuild().getId())
                            .channelId(ctx.getChannel().getId())
                            .options(List.of(options))
                            .name(name)
                            .time(timeout + System.currentTimeMillis());

                    var image = ctx.getOptionAsString("image");
                    if (image != null && !image.isBlank()) {
                        builder.image(image);
                    }

                    builder.build().start(ctx);
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    // This shouldn't happen, but I guess it could happen, so we're better off sending the error alongside.
                    ctx.replyEphemeral("commands.poll.invalid", EmoteReference.WARNING);
                }
            }
        }

        @Name("list")
        @Description("List running polls.")
        public static class ListCommand extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var guildData = ctx.getDBGuild();
                var polls = guildData.getRunningPolls();
                if (polls.isEmpty()) {
                    ctx.replyEphemeral("commands.poll.list.empty", EmoteReference.ERROR);
                    return;
                }

                var lang = ctx.getLanguageContext();

                var builder = new EmbedBuilder()
                        .setAuthor(lang.get("commands.poll.list.header").formatted(ctx.getGuild().getName()), null, ctx.getAuthor().getEffectiveAvatarUrl())
                        .setDescription(lang.get("commands.poll.list.description"))
                        .setFooter(lang.get("commands.poll.list.footer").formatted(polls.size()));

                var atomic = new AtomicInteger();
                var pollList = polls.values()
                        .stream()
                        .sorted(Comparator.comparingLong(Poll.PollDatabaseObject::time))
                        .toList();

                for (var poll : pollList) {
                    builder.addField(
                            lang.get("commands.poll.list.format_header").formatted(atomic.incrementAndGet()),
                            lang.get("commands.poll.list.format").formatted(
                                    StringUtils.limit(poll.name(), 50), poll.time() / 1000,
                                    "https://discord.com/channels/%s/%s/%s".formatted(ctx.getGuild().getId(), poll.channelId(), poll.messageId())),
                            false
                    );
                }

                ctx.send(builder.build());
            }
        }

        @Name("cancel")
        @Description("Cancels a poll.")
        public static class CancelCommand extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var dbGuild = ctx.getDBGuild();
                try {
                    var polls = dbGuild.getRunningPolls();
                    if (polls.isEmpty()) {
                        ctx.replyEphemeral("commands.poll.cancel.no_polls", EmoteReference.ERROR);
                        return;
                    }

                    if (polls.size() == 1) {
                        Poll.cancel(polls.entrySet().iterator().next().getKey(), dbGuild);
                        ctx.replyEphemeral("commands.poll.cancel.success", EmoteReference.CORRECT);
                    } else {
                        I18nContext lang = ctx.getLanguageContext();
                        var poll = polls.values()
                                .stream()
                                .sorted(Comparator.comparingLong(Poll.PollDatabaseObject::time))
                                .toList();

                        DiscordUtils.selectListButtonSlash(ctx, poll,
                                r -> "%s [link](%s), %s: %s".formatted(r.name(),
                                        "https://discord.com/channels/%s/%s/%s".formatted(ctx.getGuild().getId(), r.channelId(), r.messageId()),
                                        lang.get("commands.poll.due_at"), Utils.formatDuration(lang, r.time() - System.currentTimeMillis())),
                                r1 -> new EmbedBuilder().setColor(Color.CYAN).setTitle(lang.get("commands.poll.cancel.select"), null)
                                        .setDescription(r1)
                                        .setFooter(lang.get("general.timeout").formatted(10), null).build(),
                                (sr, hook) -> {
                                    Poll.cancel(sr.channelId() + ":" + sr.messageId(), dbGuild);
                                    hook.editOriginal(lang.get("commands.poll.cancel.success").formatted(EmoteReference.CORRECT)).setEmbeds().setComponents().queue();
                                });
                    }
                } catch (Exception e) {
                    ctx.replyEphemeral("commands.poll.cancel.no_polls", EmoteReference.ERROR);
                }
            }
        }
    }

    public static void iamFunction(String autoroleName, IContext ctx, String message) {
        var dbGuild = ctx.db().getGuild(ctx.getGuild());
        var autoroles = dbGuild.getAutoroles();

        if (autoroles.containsKey(autoroleName)) {
            var role = ctx.getGuild().getRoleById(autoroles.get(autoroleName));
            if (role == null) {
                ctx.sendLocalized("commands.iam.deleted_role", EmoteReference.ERROR);

                // delete the non-existent autorole.
                dbGuild.removeAutorole(autoroleName);
                dbGuild.updateAllChanged();
            } else {
                if (ctx.getMember().getRoles().stream().anyMatch(r1 -> r1.getId().equals(role.getId()))) {
                    ctx.sendLocalized("commands.iam.already_assigned", EmoteReference.ERROR);
                    return;
                }

                if (Utils.isRoleAdministrative(role)) {
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
        var autoroles = dbGuild.getAutoroles();

        if (autoroles.containsKey(autoroleName)) {
            Role role = ctx.getGuild().getRoleById(autoroles.get(autoroleName));
            if (role == null) {
                ctx.sendLocalized("commands.iam.deleted_role", EmoteReference.ERROR);
                dbGuild.removeAutorole(autoroleName);
                dbGuild.updateAllChanged();
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
