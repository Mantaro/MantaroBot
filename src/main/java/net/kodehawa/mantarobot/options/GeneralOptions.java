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
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.commands.game.core.GameLobby;
import net.kodehawa.mantarobot.commands.interaction.polls.Poll;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.helpers.GuildData;
import net.kodehawa.mantarobot.options.annotations.Option;
import net.kodehawa.mantarobot.options.core.OptionHandler;
import net.kodehawa.mantarobot.options.event.OptionRegistryEvent;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Option
public class GeneralOptions extends OptionHandler {
    @Subscribe
    public void onRegistry(OptionRegistryEvent e) {
        registerOption("lobby:reset", "Lobby reset", "Fixes stuck game/poll/operations session.", (event, lang) -> {
            GameLobby.LOBBYS.remove(event.getChannel().getIdLong());
            Poll.getRunningPolls().remove(event.getChannel().getId());

            List<Future<Void>> stuck = InteractiveOperations.get(event.getChannel());
            if (stuck.size() > 0)
                stuck.forEach(f -> f.cancel(true));

            event.getChannel().sendMessageFormat(lang.get("options.lobby_reset.success"), EmoteReference.CORRECT).queue();
        });

        registerOption("modlog:blacklist", "Prevents an user from appearing in modlogs", """
                Prevents an user from appearing in modlogs.
                You need the user mention.
                Example: ~>opts modlog blacklist @user""", (event, lang) -> {
                    List<User> mentioned = event.getMessage().getMentionedUsers();
                    if (mentioned.isEmpty()) {
                        event.getChannel().sendMessageFormat(lang.get("options.modlog_blacklist.no_mentions"), EmoteReference.ERROR).queue();
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();

                    List<String> toBlackList = mentioned.stream().map(ISnowflake::getId).collect(Collectors.toList());
                    String blacklisted = mentioned.stream().map(user -> user.getName() + "#" + user.getDiscriminator()).collect(Collectors.joining(","));

                    guildData.getModlogBlacklistedPeople().addAll(toBlackList);
                    dbGuild.save();

                    event.getChannel().sendMessageFormat(lang.get("options.modlog_blacklist.success"), EmoteReference.CORRECT, blacklisted).queue();
        });

        registerOption("modlog:whitelist", "Allows an user from appearing in modlogs (everyone by default)", """
                Allows an user from appearing in modlogs.
                You need the user mention.
                Example: ~>opts modlog whitelist @user""", (event, lang) -> {
                    List<User> mentioned = event.getMessage().getMentionedUsers();
                    if (mentioned.isEmpty()) {
                        event.getChannel().sendMessageFormat(lang.get("options.modlog_whitelist.no_mentions"), EmoteReference.ERROR).queue();
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();

                    List<String> toUnBlacklist = mentioned.stream().map(ISnowflake::getId).collect(Collectors.toList());
                    String unBlacklisted = mentioned.stream().map(user -> user.getName() + "#" + user.getDiscriminator()).collect(Collectors.joining(","));

                    guildData.getModlogBlacklistedPeople().removeAll(toUnBlacklist);
                    dbGuild.save();

                    event.getChannel().sendMessageFormat(lang.get("options.modlog_whitelist.success"), EmoteReference.CORRECT, unBlacklisted).queue();
        });

        registerOption("imageboard:tags:blacklist:add", "Blacklist imageboard tags", "Blacklists the specified imageboard tag from being looked up.",
                "Blacklist imageboard tags", (event, args, lang) -> {
                    if (args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.imageboard_tags_blacklist_add.no_tag"), EmoteReference.ERROR).queue();
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();

                    for (String tag : args) {
                        guildData.getBlackListedImageTags().add(tag.toLowerCase());
                    }

                    dbGuild.saveUpdating();
                    event.getChannel().sendMessageFormat(lang.get("options.imageboard_tags_blacklist_add.success"),
                            EmoteReference.CORRECT, String.join(" ,", args)
                    ).queue();
        });

        registerOption("imageboard:tags:blacklist:remove", "Un-blacklist imageboard tags", "Un-blacklist the specified imageboard tag from being looked up.",
                "Un-blacklist imageboard tags", (event, args, lang) -> {
                    if (args.length == 0) {
                        event.getChannel().sendMessageFormat(lang.get("options.imageboard_tags_blacklist_remove.no_tag"), EmoteReference.ERROR).queue();
                        return;
                    }

                    DBGuild dbGuild = MantaroData.db().getGuild(event.getGuild());
                    GuildData guildData = dbGuild.getData();

                    for (String tag : args) {
                        guildData.getBlackListedImageTags().remove(tag.toLowerCase());
                    }

                    dbGuild.saveAsync();
                    event.getChannel().sendMessageFormat(lang.get("options.imageboard_tags_blacklist_remove.success"),
                            EmoteReference.CORRECT, String.join(" ,", args)
                    ).queue();
        });
    }

    @Override
    public String description() {
        return "Everything that doesn't fit anywhere else.";
    }
}
