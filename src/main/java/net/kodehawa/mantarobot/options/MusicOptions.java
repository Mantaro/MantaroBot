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

package net.kodehawa.mantarobot.options;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.options.annotations.Option;
import net.kodehawa.mantarobot.options.core.OptionHandler;
import net.kodehawa.mantarobot.options.core.OptionType;
import net.kodehawa.mantarobot.options.event.OptionRegistryEvent;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import org.slf4j.Logger;

import java.util.function.Consumer;

@Option
public class MusicOptions extends OptionHandler {
    public MusicOptions() {
        setType(OptionType.MUSIC);
    }

    @Subscribe
    public void onRegistry(OptionRegistryEvent e) {
        registerOption("fairqueue:max", "Fair queue maximum",
                "Sets the maximum fairqueue value (max amount of the same song any user can add).\n" +
                        "Example: `~>opts fairqueue max 5`",
                "Sets the maximum fairqueue value.", (event, args, lang) -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();

                    if (args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.fairqueue_max.invalid"), EmoteReference.ERROR).queue();
                        return;
                    }

                    String much = args[0];
                    final int fq;
                    try {
                        fq = Integer.parseInt(much);
                    } catch (Exception ex) {
                        event.getChannel().sendMessageFormat(lang.get("general.invalid_number"), EmoteReference.ERROR).queue();
                        return;
                    }

                    guildData.setMaxFairQueue(fq);
                    dbGuild.save();
                    event.getChannel().sendMessageFormat(lang.get("options.fairqueue_max.success"), EmoteReference.CORRECT, fq).queue();
                });

        registerOption("musicannounce:toggle", "Music announce toggle", "Toggles whether the bot will announce the new song playing or no.",
                (event, lang) -> {
                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    boolean t1 = guildData.isMusicAnnounce();

                    guildData.setMusicAnnounce(!t1);
                    event.getChannel().sendMessageFormat(lang.get("options.musicannounce_toggle.success"), EmoteReference.CORRECT, !t1).queue();
                    dbGuild.save();
                });

        registerOption("music:channel", "Music VC lock",
                "Locks the bot to a VC. You need the VC name.\n" +
                        "Example: `~>opts music channel Music`",
                "Locks the music feature to the specified VC.", (event, args, lang) -> {
                    if (args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.music_channel.no_channel"), EmoteReference.ERROR).queue();
                        return;
                    }

                    String channelName = String.join(" ", args);

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    Consumer<VoiceChannel> consumer = voiceChannel -> {
                        guildData.setMusicChannel(voiceChannel.getId());
                        dbGuild.save();
                        event.getChannel().sendMessageFormat(lang.get("options.music_channel.success"), EmoteReference.OK, voiceChannel.getName()).queue();
                    };

                    VoiceChannel channel = Utils.findVoiceChannelSelect(event, channelName, consumer);

                    if (channel != null) {
                        consumer.accept(channel);
                    }
                });

        registerOption("music:queuelimit", "Music queue limit",
                "Sets a custom queue limit.\n" +
                        "Example: `~>opts music queuelimit 90`",
                "Sets a custom queue limit.", (event, args, lang) -> {
                    if (args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.music_queuelimit.no_args"), EmoteReference.ERROR).queue();
                        return;
                    }

                    boolean isNumber = args[0].matches("^[0-9]*$");
                    if (!isNumber) {
                        event.getChannel().sendMessageFormat(lang.get("general.invalid_number"), EmoteReference.ERROR).queue();
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();
                    try {
                        int finalSize = Integer.parseInt(args[0]);
                        int applySize = Math.min(finalSize, 300);
                        guildData.setMusicQueueSizeLimit((long) applySize);
                        dbGuild.save();
                        event.getChannel().sendMessageFormat(lang.get("options.music_queuelimit.success"), EmoteReference.MEGA, applySize).queue();
                    } catch (NumberFormatException ex) {
                        event.getChannel().sendMessageFormat(lang.get("options.music_queuelimit.invalid"), EmoteReference.ERROR).queue();
                    }
                });

        registerOption("music:clearchannel", "Music channel clear", "Clears the specific music channel.", (event, lang) -> {
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            guildData.setMusicChannel(null);
            dbGuild.save();
            event.getChannel().sendMessageFormat(lang.get("options.music_clearchannel.success"), EmoteReference.CORRECT).queue();
        });

        registerOption("music:vote:toggle", "Vote toggle", "Toggles voting.", (event, lang) -> {
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            guildData.setMusicVote(!guildData.isMusicVote());
            dbGuild.save();
            event.getChannel().sendMessageFormat(lang.get("options.music_vote_toggle.success"), EmoteReference.CORRECT, guildData.isMusicVote()).queue();
        });
    }

    @Override
    public String description() {
        return "Music related options. Everything from fair queue to locking the bot to a specific channel";
    }
}
