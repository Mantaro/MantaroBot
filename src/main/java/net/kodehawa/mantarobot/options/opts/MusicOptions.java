package net.kodehawa.mantarobot.options.opts;

import com.google.common.eventbus.Subscribe;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.kodehawa.mantarobot.commands.OptsCmd;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.options.OptionType;
import net.kodehawa.mantarobot.options.annotations.Option;
import net.kodehawa.mantarobot.options.event.OptionRegistryEvent;
import net.kodehawa.mantarobot.utils.DiscordUtils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.List;
import java.util.stream.Collectors;

@Option
@Slf4j
public class MusicOptions extends OptionHandler {

    public MusicOptions() {
        setType(OptionType.MUSIC);
    }

    @Subscribe
    public void onRegistry(OptionRegistryEvent e) {
        registerOption("fairqueue:max", "Fair queue maximum",
                "Sets the maximum fairqueue value (max amount of the same song any user can add).\n" +
                        "Example: `~>opts fairqueue max 5`",
                "Sets the maximum fairqueue value.", (event, args) -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();

                    if(args.length == 0) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You need to specify a positive integer.").queue();
                        return;
                    }

                    String much = args[0];
                    final int fq;
                    try {
                        fq = Integer.parseInt(much);
                    } catch(Exception ex) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "Not a valid number").queue();
                        return;
                    }

                    guildData.setMaxFairQueue(fq);
                    dbGuild.save();
                    event.getChannel().sendMessage(EmoteReference.CORRECT + "Set max fair queue size to " + fq).queue();
                });

        registerOption("musicannounce:toggle", "Music announce toggle", "Toggles whether the bot will announce the new song playing or no.", event -> {
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            boolean t1 = guildData.isMusicAnnounce();

            guildData.setMusicAnnounce(!t1);
            event.getChannel().sendMessage(EmoteReference.CORRECT + "Set music announce to " + "**" + !t1 + "**").queue();
            dbGuild.save();
        });

        registerOption("music:channel", "Music VC lock",
                "Locks the bot to a VC. You need the VC name.\n" +
                        "Example: `~>opts music channel Music`",
                "Locks the music feature to the specified VC.", (event, args) -> {
                    if(args.length == 0) {
                        OptsCmd.onHelp(event);
                        return;
                    }

                    String channelName = String.join(" ", args);

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();

                    VoiceChannel channel = null;

                    try {
                        channel = event.getGuild().getVoiceChannelById(channelName);
                    } catch(Exception ignored) {
                    }

                    if(channel == null) {
                        try {
                            List<VoiceChannel> voiceChannels = event.getGuild().getVoiceChannels().stream()
                                    .filter(voiceChannel -> voiceChannel.getName().contains(channelName))
                                    .collect(Collectors.toList());

                            if(voiceChannels.size() == 0) {
                                event.getChannel().sendMessage(EmoteReference.ERROR + "I couldn't find a voice channel matching that" +
                                        " name or id").queue();
                            } else if(voiceChannels.size() == 1) {
                                channel = voiceChannels.get(0);
                                guildData.setMusicChannel(channel.getId());
                                dbGuild.save();
                                event.getChannel().sendMessage(EmoteReference.OK + "Music Channel set to: " + channel.getName())
                                        .queue();
                            } else {
                                DiscordUtils.selectList(event, voiceChannels,
                                        voiceChannel -> String.format("%s (ID: %s)", voiceChannel.getName(), voiceChannel.getId()),
                                        s -> OptsCmd.getOpts().baseEmbed(event, "Select the Channel:").setDescription(s).build(),
                                        voiceChannel -> {
                                            guildData.setMusicChannel(voiceChannel.getId());
                                            dbGuild.save();
                                            event.getChannel().sendMessage(EmoteReference.OK + "Music Channel set to: " +
                                                    voiceChannel.getName()).queue();
                                        }
                                );
                            }
                        } catch(Exception ex) {
                            log.warn("Error while setting voice channel", ex);
                            event.getChannel().sendMessage("I couldn't set the voice channel " + EmoteReference.SAD + " - try again " +
                                    "in a few minutes " +
                                    "-> " + ex.getClass().getSimpleName()).queue();
                        }
                    }
                });

        registerOption("music:queuelimit", "Music queue limit",
                "Sets a custom queue limit.\n" +
                        "Example: `~>opts music queuelimit 90`",
                "Sets a custom queue limit.", (event, args) -> {
                    if(args.length == 0) {
                        OptsCmd.onHelp(event);
                        return;
                    }

                    boolean isNumber = args[0].matches("^[0-9]*$");
                    if(!isNumber) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "That's not a valid number!").queue();
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    try {
                        int finalSize = Integer.parseInt(args[0]);
                        int applySize = finalSize >= 300 ? 300 : finalSize;
                        guildData.setMusicQueueSizeLimit((long) applySize);
                        dbGuild.save();
                        event.getChannel().sendMessage(String.format(EmoteReference.MEGA + "The queue limit on this server is now " +
                                "**%d** songs.", applySize)).queue();
                    } catch(NumberFormatException ex) {
                        event.getChannel().sendMessage(EmoteReference.ERROR + "You're trying to set too high of a number (which won't" +
                                " be applied anyway), silly").queue();
                    }
                });

        registerOption("music:clear", "Music clear settings", "Clears the specific music channel.", (event) -> {
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            guildData.setMusicChannel(null);
            dbGuild.save();
            event.getChannel().sendMessage(EmoteReference.CORRECT + "I can play music on all channels now").queue();
        });
    }

    @Override
    public String description() {
        return "Music related options. Everything from fair queue to locking the bot to a specific channel";
    }
}
