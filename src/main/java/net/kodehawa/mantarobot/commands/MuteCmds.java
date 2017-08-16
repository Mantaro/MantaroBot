package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.kodehawa.mantarobot.commands.moderation.ModLog;
import net.kodehawa.mantarobot.commands.moderation.MuteTask;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.MantaroObj;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.listeners.events.PostLoadEvent;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.options.Option;
import net.kodehawa.mantarobot.options.OptionType;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.StringUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Module
public class MuteCmds {

    private final ScheduledExecutorService muteExecutor = Executors.newSingleThreadScheduledExecutor();
    private static Pattern timePattern = Pattern.compile("-time [(\\d+)((?:h(?:our(?:s)?)?)|(?:m(?:in(?:ute(?:s)?)?)?)|(?:s(?:ec(?:ond(?:s)?)?)?))]+");

    @Subscribe
    public void mute(CommandRegistry registry) {
        registry.register("mute", new SimpleCommand(Category.MODERATION, CommandPermission.ADMIN) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                ManagedDatabase db = MantaroData.db();
                DBGuild dbGuild = db.getGuild(event.getGuild());
                GuildData guildData = dbGuild.getData();
                String reason = "Not specified";
                Map<String, Optional<String>> opts = br.com.brjdevs.java.utils.texts.StringUtils.parse(args);

                if(guildData.getMutedRole() == null) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "The mute role is not set in this server, you can set it by doing `~>opts muterole set <role>`").queue();
                    return;
                }

                Role mutedRole = event.getGuild().getRoleById(guildData.getMutedRole());

                if(args.length > 1) {
                    reason = StringUtils.splitArgs(content, 2)[1];
                }

                if(event.getMessage().getMentionedUsers().isEmpty()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You need to mention at least one user to mute.").queue();
                    return;
                }

                if(!event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MANAGE_ROLES)) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "I don't have permissions to administrate roles on this server!").queue();
                    return;
                }

                //Regex from: Fabricio20
                final String finalReason = timePattern.matcher(reason).replaceAll("");

                MantaroObj data = db.getMantaroData();

                event.getMessage().getMentionedUsers().forEach(user -> {
                    Member m = event.getGuild().getMember(user);
                    long time = guildData.getSetModTimeout() > 0 ? System.currentTimeMillis() + guildData.getSetModTimeout() : 0L;

                    if(opts.containsKey("time")) {
                        if(opts.get("time").get().isEmpty()) {
                            event.getChannel().sendMessage(EmoteReference.WARNING + "You wanted time but didn't specify for how long!").queue();
                            return;
                        }

                        time = System.currentTimeMillis() + Utils.parseTime(opts.get("time").get());
                        data.getMutes().put(user.getIdLong(), Pair.of(event.getGuild().getId(), time));
                        data.save();
                        dbGuild.save();
                    } else {
                        if(time > 0) {
                            data.getMutes().put(user.getIdLong(), Pair.of(event.getGuild().getId(), time));
                            data.save();
                            dbGuild.save();
                        } else {
                            event.getChannel().sendMessage(EmoteReference.ERROR + "You didn't specify any time!").queue();
                        }
                    }


                    if(m.getRoles().contains(mutedRole)) {
                        event.getChannel().sendMessage(EmoteReference.WARNING + "This user already has a mute role assigned. Please do `~>unmute` to unmute them.").queue();
                        return;
                    }

                    if(!event.getGuild().getSelfMember().canInteract(m)) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "I cannot assign the mute role to this user because they're in a higher hierarchy than me, or the role is in a higher hierarchy!").queue();
                        return;
                    }

                    if(!event.getMember().canInteract(m)) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "I cannot assign the mute role to this user because they're in a higher hierarchy than me, or the role is in a higher hierarchy than you!").queue();
                        return;
                    }

                    final DBGuild dbg = db.getGuild(event.getGuild());
                    event.getGuild().getController().addSingleRoleToMember(m, mutedRole).queue();
                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Added mute role to **" +
                            m.getEffectiveName() + (time > 0 ? "** for around " + Utils.getVerboseTime(time - System.currentTimeMillis()) : "**")).queue();
                    dbg.getData().setCases(dbg.getData().getCases() + 1);
                    dbg.saveAsync();
                    ModLog.log(event.getMember(), user, finalReason, ModLog.ModAction.MUTE, dbg.getData().getCases());
                });
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Mute")
                        .setDescription("**Mutes the specified users**")
                        .addField("Usage", "`~>mute <user> <reason> [-time <time>]` - Mutes the specified users.", false)
                        .addField("Parameters", "`users` - The users to mute. Needs to be mentions.\n" +
                                "`[-time <time>]` - The time to mute an user for. For example `~>mute @Natan#1289 wew, nice -time 1m20s` will mute Natan for 1 minute and 20 seconds.", false)
                        .addField("Considerations", "To unmute an user, do `~>unmute`.", false)
                        .addField("Extended usage", "`time` - can be used with the following parameters: " +
                                "d (days), s (second), m (minutes), h (hour). **For example -time 1d1h will mute for one day and one hour.**", false)
                        .build();
            }
        }).addOption("defaultmutetimeout:set", new Option("Default mute timeout",
                "Sets the default mute timeout for ~>mute.\n" +
                        "This command will set the timeout of ~>mute to a fixed value **unless you specify another time in the command**\n" +
                        "**Example:** `~>opts defaultmutetimeout set 1m20s`\n" +
                        "**Considerations:** Time is in 1m20s or 1h10m3s format, for example.", OptionType.GUILD)
                .setAction(((event, args) -> {
                    if(args.length == 0) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You have to specify a timeout in the format of 1m20s, for example.").queue();
                        return;
                    }

                    if(!(args[0]).matches("(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?")) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Wrong time format. You have to specify a timeout in the format of 1m20s, for example.").queue();
                        return;
                    }

                    long timeoutToSet = Utils.parseTime(args[0]);
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    guildData.setSetModTimeout(timeoutToSet);
                    dbGuild.save();

                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Successfully set mod action timeout to `" + args[0] + "` (" + timeoutToSet + "ms)").queue();
                })).setShortDescription("Sets the default timeout for the ~>mute command"))
                .addOption("defaultmutetimeout:reset", new Option("Default mute timeout reset",
                        "Resets the default mute timeout which was set previously with `defaultmusictimeout set`", OptionType.GUILD)
                        .setAction((event -> {
                            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                            GuildData guildData = dbGuild.getData();

                            guildData.setSetModTimeout(0L);
                            dbGuild.save();

                            event.getChannel().sendMessage(EmoteReference.CORRECT + "Successfully reset timeout.").queue();
                        })).setShortDescription("Resets the default mute timeout."))
                .addOption("muterole:set", new Option("Mute role set",
                        "Sets this guilds mute role to apply on the ~>mute command.\n" +
                                "To use this command you need to specify a role name. *In case the name contains spaces, the name should" +
                                " be wrapped in quotation marks", OptionType.COMMAND)
                        .setAction((event, args) -> {
                            if (args.length < 1) {
                                OptsCmd.onHelp(event);
                                return;
                            }

                            String roleName = String.join(" ", args);
                            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                            GuildData guildData = dbGuild.getData();

                            List<Role> roleList = event.getGuild().getRolesByName(roleName, true);
                            if (roleList.size() == 0) {
                                event.getChannel().sendMessage(EmoteReference.ERROR + "I didn't find a role with that name!").queue();
                            } else if (roleList.size() == 1) {
                                Role role = roleList.get(0);
                                guildData.setMutedRole(role.getId());
                                dbGuild.saveAsync();
                                event.getChannel().sendMessage(EmoteReference.OK + "Set mute role to **" + roleName + "**").queue();
                            } else {
                                DiscordUtils.selectList(event, roleList, role -> String.format("%s (ID: %s)  | Position: %s", role.getName(),
                                        role.getId(), role.getPosition()), s -> OptsCmd.getOpts().baseEmbed(event, "Select the Mute Role:")
                                                .setDescription(s).build(),
                                        role -> {
                                            guildData.setMutedRole(role.getId());
                                            dbGuild.saveAsync();
                                            event.getChannel().sendMessage(EmoteReference.OK + "Set mute role to **" + roleName + "**").queue();
                                        });
                            }
                        }).setShortDescription("Sets this guilds mute role to apply on the ~>mute command"))
                .addOption("muterole:unbind", new Option("Mute Role unbind", "Resets the current value set for the mute role", OptionType.GENERAL)
                        .setAction(event -> {
                            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                            GuildData guildData = dbGuild.getData();
                            guildData.setMutedRole(null);
                            dbGuild.saveAsync();
                            event.getChannel().sendMessage(EmoteReference.OK + "Correctly resetted mute role.").queue();
                        }).setShortDescription("Resets the current value set for the mute role."));
    }

    @Subscribe
    public void unmute(CommandRegistry commandRegistry) {
        commandRegistry.register("unmute", new SimpleCommand(Category.MODERATION, CommandPermission.ADMIN) {
            @Override
            protected void call(GuildMessageReceivedEvent event, String content, String[] args) {
                ManagedDatabase db = MantaroData.db();
                DBGuild dbGuild = db.getGuild(event.getGuild());
                GuildData guildData = dbGuild.getData();
                String reason = "Not specified";

                if(guildData.getMutedRole() == null) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "The mute role is not set in this server, you can set it by doing `~>opts muterole set <role>`").queue();
                    return;
                }

                Role mutedRole = event.getGuild().getRoleById(guildData.getMutedRole());

                if(args.length > 1) {
                    reason = StringUtils.splitArgs(content, 2)[1];
                }

                if(event.getMessage().getMentionedUsers().isEmpty()) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "You need to mention at least one user to un-mute.").queue();
                    return;
                }

                if(!event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MANAGE_ROLES)) {
                    event.getChannel().sendMessage(EmoteReference.ERROR + "I don't have permissions to administer roles on this server!").queue();
                    return;
                }

                final String finalReason = reason;
                final DBGuild dbg = db.getGuild(event.getGuild());

                event.getMessage().getMentionedUsers().forEach(user -> {
                    Member m = event.getGuild().getMember(user);

                    guildData.getMutedTimelyUsers().remove(user.getIdLong());
                    if(!event.getGuild().getSelfMember().canInteract(m)) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "I cannot remove a mute role to this user because they're in a higher hierarchy than me, or the role is in a higher hierarchy!").queue();
                        return;
                    }

                    if(!event.getMember().canInteract(m)) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "I cannot remove a mute role to this user because they're in a higher hierarchy than me, or the role is in a higher hierarchy than you!").queue();
                        return;
                    }

                    if(m.getRoles().contains(mutedRole)) {
                        event.getGuild().getController().removeRolesFromMember(m, mutedRole).queue();
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Removed mute role from **" + m.getEffectiveName() + "**").queue();
                        dbg.getData().setCases(dbg.getData().getCases() + 1 );
                        dbg.saveAsync();
                        ModLog.log(event.getMember(), user, finalReason, ModLog.ModAction.UNMUTE, db.getGuild(event.getGuild()).getData().getCases());
                    } else {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "This user doesn't have the mute role assigned to them.").queue();
                    }
                });
            }

            @Override
            public MessageEmbed help(GuildMessageReceivedEvent event) {
                return helpEmbed(event, "Un-mute")
                        .setDescription("**Un-mutes the specified users**")
                        .addField("Usage", "`~>unmute <user> <reason>` - Un-mutes the specified users.", false)
                        .addField("Parameters", "`users` - The users to un-mute. Needs to be mentions.", false)
                        .addField("Considerations", "To mute an user, do `~>mute`.", false)
                        .build();
            }
        });
    }

    @Subscribe
    public void onPostLoad(PostLoadEvent e){
        muteExecutor.scheduleAtFixedRate(new MuteTask(), 0, 25, TimeUnit.SECONDS);
    }
}
