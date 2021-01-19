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
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.commands.game.core.GameLobby;
import net.kodehawa.mantarobot.commands.interaction.polls.Poll;
import net.kodehawa.mantarobot.core.listeners.operations.InteractiveOperations;
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
        registerOption("lobby:reset", "Lobby reset", "Fixes stuck game/poll/operations session.", (ctx) -> {
            GameLobby.LOBBYS.remove(ctx.getChannel().getIdLong());
            Poll.getRunningPolls().remove(ctx.getChannel().getId());

            List<Future<Void>> stuck = InteractiveOperations.get(ctx.getChannel());
            if (stuck.size() > 0)
                stuck.forEach(f -> f.cancel(true));

            ctx.sendLocalized("options.lobby_reset.success", EmoteReference.CORRECT);
        });

        registerOption("modlog:blacklist", "Prevents an user from appearing in modlogs", """
                Prevents an user from appearing in modlogs.
                You need the user mention.
                Example: ~>opts modlog blacklist @user""", (ctx) -> {
            List<Member> mentioned = ctx.getMentionedMembers();
            if (mentioned.isEmpty()) {
                ctx.sendLocalized("options.modlog_blacklist.no_mentions", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            List<String> toBlackList = mentioned.stream().map(ISnowflake::getId).collect(Collectors.toList());
            String blacklisted = mentioned.stream()
                    .map(Member::getUser)
                    .map(User::getAsTag)
                    .collect(Collectors.joining(","));

            guildData.getModlogBlacklistedPeople().addAll(toBlackList);
            dbGuild.save();

            ctx.sendLocalized("options.modlog_blacklist.success", EmoteReference.CORRECT, blacklisted);
        });

        registerOption("modlog:whitelist", "Allows an user from appearing in modlogs (everyone by default)", """
                Allows an user from appearing in modlogs.
                You need the user mention.
                Example: ~>opts modlog whitelist @user""", (ctx) -> {
            List<Member> mentioned = ctx.getMentionedMembers();
            if (mentioned.isEmpty()) {
                ctx.sendLocalized("options.modlog_whitelist.no_mentions", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            List<String> toUnBlacklist = mentioned.stream().map(ISnowflake::getId).collect(Collectors.toList());
            String unBlacklisted = mentioned.stream()
                    .map(Member::getUser)
                    .map(User::getAsTag)
                    .collect(Collectors.joining(","));

            guildData.getModlogBlacklistedPeople().removeAll(toUnBlacklist);
            dbGuild.save();

            ctx.sendLocalized("options.modlog_whitelist.success", EmoteReference.CORRECT, unBlacklisted);
        });

        registerOption("modlog:blacklistwords:add", "Modlog Word Blacklist add", """
                Adds a word to the modlog word blacklist (won't add any messages with that word). Can contain spaces.
                **Example:** `~>opts modlog blacklistwords add mood`
                """, "Sets the join message.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.modlog_blacklistwords_add.no_word", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            if (guildData.getModLogBlacklistWords().size() > 20) {
                ctx.sendLocalized("options.modlog_blacklistwords_add.too_many", EmoteReference.ERROR);
                return;
            }

            String word = String.join(" ", args);
            guildData.getModLogBlacklistWords().add(word);
            dbGuild.save();
            ctx.sendLocalized("options.modlog_blacklistwords_add.success", EmoteReference.CORRECT, word);
        });

        registerOption("modlog:blacklistwords:remove", "Modlog word blacklist remove", """
                Removes a word from the modlog word blacklist. Can contain spaces
                **Example:** `~>opts modlog blacklistwords remove mood`
                """, "Sets the join message.", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.modlog_blacklistwords_add.no_word", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            String word = String.join(" ", args);

            if (!guildData.getModLogBlacklistWords().contains(word)) {
                ctx.sendLocalized("options.modlog_blacklistwords_remove.not_in", EmoteReference.ERROR, word);
                return;
            }

            guildData.getModLogBlacklistWords().remove(word);
            dbGuild.save();
            ctx.sendLocalized("options.modlog_blacklistwords_remove.success", EmoteReference.CORRECT, word);
        });

        registerOption("imageboard:tags:blacklist:add", "Blacklist imageboard tags", 
                "Blacklists the specified imageboard tag from being looked up.",
                "Blacklist imageboard tags", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.imageboard_tags_blacklist_add.no_tag", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            for (String tag : args) {
                guildData.getBlackListedImageTags().add(tag.toLowerCase());
            }

            dbGuild.saveUpdating();
            ctx.sendLocalized("options.imageboard_tags_blacklist_add.success",
                    EmoteReference.CORRECT, String.join(" ,", args)
            );
        });

        registerOption("imageboard:tags:blacklist:remove", "Un-blacklist imageboard tags", 
                "Un-blacklist the specified imageboard tag from being looked up.",
                "Un-blacklist imageboard tags", (ctx, args) -> {
            if (args.length == 0) {
                ctx.sendLocalized("options.imageboard_tags_blacklist_remove.no_tag", EmoteReference.ERROR);
                return;
            }

            DBGuild dbGuild = ctx.getDBGuild();
            GuildData guildData = dbGuild.getData();

            for (String tag : args) {
                guildData.getBlackListedImageTags().remove(tag.toLowerCase());
            }

            dbGuild.saveAsync();
            ctx.sendLocalized("options.imageboard_tags_blacklist_remove.success", EmoteReference.CORRECT, String.join(" ,", args));
        });
    }

    @Override
    public String description() {
        return "Everything that doesn't fit anywhere else.";
    }
}
