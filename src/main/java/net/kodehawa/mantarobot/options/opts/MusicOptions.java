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
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.function.Consumer;

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
                    Consumer<VoiceChannel> consumer = voiceChannel -> {
                        guildData.setMusicChannel(voiceChannel.getId());
                        dbGuild.save();
                        event.getChannel().sendMessage(EmoteReference.OK + "Music Channel set to: " + voiceChannel.getName()).queue();
                    };

                    VoiceChannel channel = Utils.findVoiceChannelSelect(event, channelName, consumer);

                    if (channel != null) {
                        consumer.accept(channel);
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

        registerOption("music:channnel:clear", "Music channel clear", "Clears the specific music channel.", (event) -> {
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
