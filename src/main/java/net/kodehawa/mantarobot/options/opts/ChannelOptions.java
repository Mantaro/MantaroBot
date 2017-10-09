/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
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
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.options.OptionType;
import net.kodehawa.mantarobot.options.annotations.Option;
import net.kodehawa.mantarobot.options.event.OptionRegistryEvent;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

@Option
public class ChannelOptions extends OptionHandler {

    public ChannelOptions() {
        setType(OptionType.CHANNEL);
    }

    @Subscribe
    public void onRegister(OptionRegistryEvent e) {
        registerOption("nsfw:toggle", "NSFW toggle", "Toggles NSFW mode in the channel the command was ran at.", (event) -> {
            DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
            GuildData guildData = dbGuild.getData();
            if(guildData.getGuildUnsafeChannels().contains(event.getChannel().getId())) {
                guildData.getGuildUnsafeChannels().remove(event.getChannel().getId());
                event.getChannel().sendMessage(EmoteReference.CORRECT + "NSFW in this channel has been disabled").queue();
                dbGuild.saveAsync();
                return;
            }

            event.getChannel().sendMessage(EmoteReference.CORRECT +
                    "Please use the guild's NSFW channel configuration instead of this (This can still be used to disable the current ones set with this command).").queue();
        });
    }

    @Override
    public String description() {
        return null;
    }
}
