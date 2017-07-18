package net.kodehawa.mantarobot.options.opts;

import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.ISnowflake;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.kodehawa.mantarobot.commands.OptsCmd;
import net.kodehawa.mantarobot.commands.game.core.GameLobby;
import net.kodehawa.mantarobot.commands.interaction.polls.Poll;
import net.kodehawa.mantarobot.options.annotations.Option;
import net.kodehawa.mantarobot.options.event.OptionRegistryEvent;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.dataporter.oldentities.OldGuild;
import net.kodehawa.mantarobot.db.entities.helpers.ExtraGuildData;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.List;
import java.util.stream.Collectors;

@Option
@Slf4j
public class GeneralOptions extends OptionHandler {

    @Subscribe
    public void onRegistry(OptionRegistryEvent e) {
        registerOption("lobby:reset", "Lobby reset","Fixes stuck game/poll/operations session.", event -> {
            GameLobby.LOBBYS.remove(event.getChannel());
            Poll.getRunningPolls().remove(event.getChannel().getId());
            InteractiveOperations.get(event.getChannel()).cancel(true);
            event.getChannel().sendMessage(EmoteReference.CORRECT + "Reset the lobby correctly.").queue();
        });

        registerOption("modlog:blacklist", "Modlog blacklist",
                "Prevents an user from appearing in modlogs.\n" +
                        "You need the user mention.\n" +
                        "Example: ~>opts modlog blacklist @user",
                "Prevents an user from appearing in modlogs", event -> {
                    List<User> mentioned = event.getMessage().getMentionedUsers();
                    if(mentioned.isEmpty()){
                        event.getChannel().sendMessage(EmoteReference.ERROR + "**You need to specify the users to locally blacklist from mod logs.**").queue();
                        return;
                    }

                    OldGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    ExtraGuildData guildData = dbGuild.getData();

                    List<String> toBlackList = mentioned.stream().map(ISnowflake::getId).collect(Collectors.toList());
                    String blacklisted = mentioned.stream().map(user -> user.getName() + "#" + user.getDiscriminator()).collect(Collectors.joining(","));

                    guildData.getModlogBlacklistedPeople().addAll(toBlackList);
                    dbGuild.save();

                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Locally blacklisted users from mod-log: **" + blacklisted + "**").queue();
                });

        registerOption("modlog:whitelist", "Modlog whitelist",
                "Allows an user from appearing in modlogs.\n" +
                        "You need the user mention.\n" +
                        "Example: ~>opts modlog whitelist @user",
                "Allows an user from appearing in modlogs (everyone by default)", event -> {
                    List<User> mentioned = event.getMessage().getMentionedUsers();
                    if(mentioned.isEmpty()){
                        event.getChannel().sendMessage(EmoteReference.ERROR + "**You need to specify the users to locally whitelist from mod logs.**").queue();
                        return;
                    }

                    OldGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    ExtraGuildData guildData = dbGuild.getData();

                    List<String> toUnBlacklist = mentioned.stream().map(ISnowflake::getId).collect(Collectors.toList());
                    String unBlacklisted = mentioned.stream().map(user -> user.getName() + "#" + user.getDiscriminator()).collect(Collectors.joining(","));

                    guildData.getModlogBlacklistedPeople().removeAll(toUnBlacklist);
                    dbGuild.save();

                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Locally un-blacklisted users from mod-log: **" + unBlacklisted + "**").queue();
                });

        registerOption("linkprotection:toggle", "Link-protection toggle", "Toggles anti-link protection.", event -> {
            OldGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            ExtraGuildData guildData = dbGuild.getData();
            boolean toggler = guildData.isLinkProtection();

            guildData.setLinkProtection(!toggler);
            event.getChannel().sendMessage(EmoteReference.CORRECT + "Set link protection to " + "`" + !toggler + "`").queue();
            dbGuild.save();
        });

        registerOption("slowmode:toggle", "Slow mode toggle", "Toggles slow mode (1 message/3s)", event -> {
            OldGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            ExtraGuildData guildData = dbGuild.getData();
            boolean toggler = guildData.isSlowMode();

            guildData.setSlowMode(!toggler);
            event.getChannel().sendMessage(EmoteReference.CORRECT + "Set slowmode chat to " + "`" + !toggler + "`").queue();
            dbGuild.save();
        });

        registerOption("antispam:toggle", "Link-protection toggle", "Toggles anti-spam (3 messages/3s)", event -> {
            OldGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            ExtraGuildData guildData = dbGuild.getData();
            boolean toggler = guildData.isAntiSpam();

            guildData.setAntiSpam(!toggler);
            event.getChannel().sendMessage(EmoteReference.CORRECT + "Set anti-spam chat mode to " + "`" + !toggler + "`").queue();
            dbGuild.save();
        });

        registerOption("linkprotection:channel:allow", "Link-protection channel allow",
                "Allows the posting of invites on a channel.\n" +
                        "You need the channel name.\n" +
                        "Example: ~>opts linkprotection channel allow promote-here",
                "Allows the posting of invites on a channel.", (event, args) -> {
                    if (args.length == 0) {
                        OptsCmd.onHelp(event);
                        return;
                    }

                    OldGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    ExtraGuildData guildData = dbGuild.getData();
                    String channelName = args[0];
                    List<TextChannel> textChannels = event.getGuild().getTextChannels().stream()
                            .filter(textChannel -> textChannel.getName().contains(channelName))
                            .collect(Collectors.toList());

                    if (textChannels.isEmpty()) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "There were no channels matching your search.").queue();
                    }

                    if (textChannels.size() <= 1) {
                        guildData.getLinkProtectionAllowedChannels().add(textChannels.get(0).getId());
                        dbGuild.save();
                        event.getChannel().sendMessage(EmoteReference.CORRECT + textChannels.get(0).getAsMention() + " can now be used to post discord invites.").queue();
                        return;
                    }

                    DiscordUtils.selectList(event, textChannels,
                            textChannel -> String.format("%s (ID: %s)", textChannel.getName(), textChannel.getId()),
                            s -> OptsCmd.getOpts().baseEmbed(event, "Select the Channel:").setDescription(s).build(),
                            textChannel -> {
                                guildData.getLinkProtectionAllowedChannels().add(textChannel.getId());
                                dbGuild.save();
                                event.getChannel().sendMessage(EmoteReference.OK + textChannel.getAsMention() + " can now be used to send discord invites.").queue();
                            }
                    );
                });

        registerOption("linkprotection:channel:disallow", "Link-protection channel disallow",
                "Disallows the posting of invites on a channel.\n" +
                        "You need the channel name.\n" +
                        "Example: ~>opts linkprotection channel disallow general",
                "Disallows the posting of invites on a channel (every channel by default)", (event, args) -> {
                    if (args.length == 0) {
                        OptsCmd.onHelp(event);
                        return;
                    }

                    OldGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    ExtraGuildData guildData = dbGuild.getData();
                    String channelName = args[0];
                    List<TextChannel> textChannels = event.getGuild().getTextChannels().stream()
                            .filter(textChannel -> textChannel.getName().contains(channelName))
                            .collect(Collectors.toList());

                    if (textChannels.isEmpty()) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "There were no channels matching your search.").queue();
                    }

                    if (textChannels.size() <= 1) {
                        guildData.getLinkProtectionAllowedChannels().remove(textChannels.get(0).getId());
                        dbGuild.save();
                        event.getChannel().sendMessage(EmoteReference.CORRECT + textChannels.get(0).getAsMention() + " cannot longer be used to post discord invites.").queue();
                        return;
                    }

                    DiscordUtils.selectList(event, textChannels,
                            textChannel -> String.format("%s (ID: %s)", textChannel.getName(), textChannel.getId()),
                            s -> OptsCmd.getOpts().baseEmbed(event, "Select the Channel:").setDescription(s).build(),
                            textChannel -> {
                                guildData.getLinkProtectionAllowedChannels().remove(textChannel.getId());
                                dbGuild.save();
                                event.getChannel().sendMessage(EmoteReference.OK + textChannel.getAsMention() + " cannot longer be used to send discord invites.").queue();
                            }
                    );
                });
    }

    @Override
    public String description() {
        return "Everything that doesn't fit anywhere else.";
    }
}
