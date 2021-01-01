/*
 * Copyright (C) 2016-2021 David Rubio Escares / Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.options;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.options.annotations.Option;
import net.kodehawa.mantarobot.options.core.OptionHandler;
import net.kodehawa.mantarobot.options.core.OptionType;
import net.kodehawa.mantarobot.options.event.OptionRegistryEvent;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.FinderUtils;

import java.util.function.Consumer;

@Option
public class MusicOptions extends OptionHandler {
    public MusicOptions() {
        setType(OptionType.MUSIC);
    }

    @Subscribe
    public void onRegistry(OptionRegistryEvent e) {
        registerOption("fairqueue:max", "Fair queue maximum",
                "Sets the maximum fairqueue value (max amount of the same song any user can add).\n" + "Example: `~>opts fairqueue max 5`",
                "Sets the maximum fairqueue value.", (ctx, args) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            if (args.length == 0) {
                ctx.sendLocalized("options.fairqueue_max.invalid", EmoteReference.ERROR);
                return;
            }

            String much = args[0];
            final int fq;
            try {
                fq = Integer.parseInt(much);
            } catch (Exception ex) {
                ctx.sendLocalized("general.invalid_number", EmoteReference.ERROR);
                return;
            }

            guildData.setMaxFairQueue(fq);
            dbGuild.save();
            ctx.sendLocalized("options.fairqueue_max.success", EmoteReference.CORRECT, fq);
        });

        registerOption("musicannounce:toggle", "Music announce toggle",
                "Toggles whether the bot will announce the new song playing or no.", (ctx) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            boolean t1 = guildData.isMusicAnnounce();

            guildData.setMusicAnnounce(!t1);
            ctx.sendLocalized("options.musicannounce_toggle.success", EmoteReference.CORRECT, !t1);
            dbGuild.save();
        });

        registerOption("music:channel", "Music VC lock", """
                Locks the bot to a VC. You need the VC name.
                Example: `~>opts music channel Music`
                """, "Locks the music feature to the specified VC.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.music_channel.no_channel", EmoteReference.ERROR);
                return;
            }

            String channelName = String.join(" ", args);

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            Consumer<VoiceChannel> consumer = voiceChannel -> {
                guildData.setMusicChannel(voiceChannel.getId());
                dbGuild.save();
                ctx.sendLocalized("options.music_channel.success", EmoteReference.OK, voiceChannel.getName());
            };

            VoiceChannel channel = FinderUtils.findVoiceChannelSelect(ctx.getEvent(), channelName, consumer);

            if (channel != null) {
                consumer.accept(channel);
            }
        });

        registerOption("music:queuelimit", "Music queue limit",
                "Sets a custom queue limit.\nExample: `~>opts music queuelimit 90`",
                "Sets a custom queue limit.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.music_queuelimit.no_args", EmoteReference.ERROR);
                return;
            }

            boolean isNumber = args[0].matches("^[0-9]*$");
            if (!isNumber) {
                ctx.sendLocalized("general.invalid_number", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            try {
                int finalSize = Integer.parseInt(args[0]);
                int applySize = Math.min(finalSize, 300);
                guildData.setMusicQueueSizeLimit((long) applySize);
                dbGuild.save();
                ctx.sendLocalized("options.music_queuelimit.success", EmoteReference.MEGA, applySize);
            } catch (NumberFormatException ex) {
                ctx.sendLocalized("options.music_queuelimit.invalid", EmoteReference.ERROR);
            }
        });

        registerOption("music:clearchannel", "Music channel clear", "Clears the specific music channel.", (ctx) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            guildData.setMusicChannel(null);
            dbGuild.save();
            ctx.sendLocalized("options.music_clearchannel.success", EmoteReference.CORRECT);
        });

        registerOption("music:vote:toggle", "Vote toggle", "Toggles voting.", (ctx) -> {
            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();
            guildData.setMusicVote(!guildData.isMusicVote());
            dbGuild.save();
            ctx.sendLocalized("options.music_vote_toggle.success", EmoteReference.CORRECT, guildData.isMusicVote());
        });
    }

    @Override
    public String description() {
        return "Music related options. Everything from fair queue to locking the bot to a specific channel";
    }
}
