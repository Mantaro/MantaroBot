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
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.command.meta.*;
import net.kodehawa.mantarobot.core.command.slash.SlashCommand;
import net.kodehawa.mantarobot.core.command.slash.SlashContext;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandCategory;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.PremiumKey;
import net.kodehawa.mantarobot.log.LogUtils;
import net.kodehawa.mantarobot.utils.APIUtils;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.awt.*;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.System.currentTimeMillis;

@Module
public class PremiumCmds {
    @Subscribe
    public void register(CommandRegistry cr) {
        cr.registerSlash(Premium.class);
    }

    @Description("Check or activate premium status for a user or server.")
    @Category(CommandCategory.UTILS)
    @Help(
            description = "Check or activate premium status for a user or server.",
            usage = "`/premium user` or `/premium server`, `/premium activate` to activate a key"
    )
    public static class Premium extends SlashCommand {
        @Override
        protected void process(SlashContext ctx) {}

        @Name("activate")
        @Description("Activates a premium key.")
        @Category(CommandCategory.UTILS)
        @Defer // Just in case lol
        @Ephemeral
        @Options({
                @Options.Option(type = OptionType.STRING, name = "key", description = "The key to use.", required = true)
        })
        @Help(
                description = "Activates a premium key. Example: `/premium activate key:a4e98f07-1a32-4dcc-b53f-c540214d54ec`. No, that isn't a valid key.",
                usage = "`/premium activate key:[key]`",
                parameters = {
                        @Help.Parameter(name = "key", description = "The key to activate. If it's a server key, make sure to run this command in the server where you want to enable premium on.")
                }
        )
        public static class ActivateKey extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                final var db = ctx.db();
                if (ctx.getConfig().isPremiumBot()) {
                    ctx.reply("commands.activatekey.mp", EmoteReference.WARNING);
                    return;
                }

                var key = db.getPremiumKey(ctx.getOptionAsString("key"));
                if (key == null || (key.isEnabled())) {
                    ctx.reply("commands.activatekey.invalid_key", EmoteReference.ERROR);
                    return;
                }

                var scopeParsed = key.getParsedType();
                var author = ctx.getAuthor();
                if (scopeParsed.equals(PremiumKey.Type.GUILD)) {
                    var guild = ctx.getDBGuild();
                    var currentKey = db.getPremiumKey(guild.getData().getPremiumKey());
                    if (currentKey != null && currentKey.isEnabled() && currentTimeMillis() < currentKey.getExpiration()) { //Should always be enabled...
                        ctx.reply("commands.activatekey.guild_already_premium", EmoteReference.POPPER);
                        return;
                    }

                    // Add to keys claimed storage if it's NOT your first key (count starts at 2/2 = 1)
                    if (!author.getId().equals(key.getOwner())) {
                        var ownerUser = db.getUser(key.getOwner());
                        ownerUser.getData().getKeysClaimed().put(author.getId(), key.getId());
                        ownerUser.saveAsync();
                    }

                    key.activate(180);
                    guild.getData().setPremiumKey(key.getId());
                    guild.saveAsync();

                    ctx.reply("commands.activatekey.guild_successful", EmoteReference.POPPER, key.getDurationDays());
                    return;
                }

                if (scopeParsed.equals(PremiumKey.Type.USER)) {
                    var dbUser = ctx.getDBUser();
                    var player = ctx.getPlayer();

                    if (dbUser.isPremium()) {
                        ctx.reply("commands.activatekey.user_already_premium", EmoteReference.POPPER);
                        return;
                    }

                    if (author.getId().equals(key.getOwner())) {
                        if (player.getData().addBadgeIfAbsent(Badge.DONATOR_2)) {
                            player.saveUpdating();
                        }
                    }

                    // Add to keys claimed storage if it's NOT your first key (count starts at 2/2 = 1)
                    if (!author.getId().equals(key.getOwner())) {
                        var ownerUser = db.getUser(key.getOwner());
                        ownerUser.getData().getKeysClaimed().put(author.getId(), key.getId());
                        ownerUser.saveAsync();
                    }

                    key.activate(author.getId().equals(key.getOwner()) ? 365 : 180);
                    dbUser.getData().setPremiumKey(key.getId());
                    dbUser.saveAsync();

                    ctx.reply("commands.activatekey.user_successful", EmoteReference.POPPER);
                }
            }
        }

        @Name("user")
        @Description("Checks the premium status of a user.")
        @Options({
                @Options.Option(type = OptionType.USER, name = "user", description = "The user to check for premium status.")
        })
        @Help(
                description = "Checks the premium status of a user.",
                usage = "`/premium check user:[user]`",
                parameters = {
                        @Help.Parameter(name = "user", description = "The user to check for. Yourself if empty.", optional = true)
                }
        )
        public static class UserCommand extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var toCheck = ctx.getOptionAsUser("user", ctx.getAuthor());
                var dbUser = ctx.db().getUser(toCheck);
                var data = dbUser.getData();
                var isLookup = toCheck.getIdLong() != ctx.getAuthor().getIdLong();

                if (!dbUser.isPremium()) {
                    ctx.reply("commands.vipstatus.user.not_premium", EmoteReference.ERROR, toCheck.getAsTag());
                    return;
                }

                var lang = ctx.getLanguageContext();
                var embedBuilder = new EmbedBuilder()
                        .setAuthor(isLookup ? String.format(lang.get("commands.vipstatus.user.header_other"), toCheck.getName())
                                : lang.get("commands.vipstatus.user.header"), null, toCheck.getEffectiveAvatarUrl()
                        );

                var currentKey = ctx.db().getPremiumKey(data.getPremiumKey());

                if (currentKey == null || currentKey.validFor() < 1) {
                    ctx.reply("commands.vipstatus.user.not_premium", toCheck.getAsTag(), EmoteReference.ERROR);
                    return;
                }

                var owner = ctx.getShardManager().retrieveUserById(currentKey.getOwner()).complete();
                var marked = false;
                if (owner == null) {
                    marked = true;
                    owner = ctx.getAuthor();
                }

                // Give the badge to the key owner, I'd guess?
                if (!marked && isLookup) {
                    Player player = ctx.db().getPlayer(owner);
                    if (player.getData().addBadgeIfAbsent(Badge.DONATOR_2))
                        player.saveUpdating();
                }

                var patreonInformation = APIUtils.getPledgeInformation(owner.getId());
                var linkedTo = currentKey.getData().getLinkedTo();
                var amountClaimed = data.getKeysClaimed().size();

                embedBuilder.setColor(Color.CYAN)
                        .setThumbnail(toCheck.getEffectiveAvatarUrl())
                        .setDescription(lang.get("commands.vipstatus.user.premium") + "\n" + lang.get("commands.vipstatus.description"))
                        .addField(lang.get("commands.vipstatus.key_owner"), owner.getName() + "#" + owner.getDiscriminator(), true)
                        .addField(lang.get("commands.vipstatus.patreon"),
                                patreonInformation == null ? "Error" : String.valueOf(patreonInformation.left()), true)
                        .addField(lang.get("commands.vipstatus.keys_claimed"), String.valueOf(amountClaimed), false)
                        .addField(lang.get("commands.vipstatus.linked"), String.valueOf(linkedTo != null), false)
                        .setFooter(lang.get("commands.vipstatus.thank_note"), null);

                try {
                    // User has more keys than what the system would allow. Warn.
                    if (patreonInformation != null && patreonInformation.left()) {
                        var patreonAmount = Double.parseDouble(patreonInformation.right());
                        if ((patreonAmount / 2) - amountClaimed < 0) {
                            var amount = amountClaimed - (patreonAmount / 2);
                            var keys = data.getKeysClaimed()
                                    .values()
                                    .stream()
                                    .limit((long) amount)
                                    .map(s -> "key:" + s)
                                    .collect(Collectors.joining("\n"));

                            LogUtils.log(
                                    """
                                    %s has more keys claimed than given keys, dumping extra keys:
                                    %s
                                    Currently pledging: %s, Claimed keys: %s, Should have %s total keys.""".formatted(
                                            owner.getId(), Utils.paste(keys, true),
                                            patreonAmount, amountClaimed, (patreonAmount / 2)
                                    )
                            );
                        }
                    }
                } catch (Exception ignored) { }

                if (linkedTo != null) {
                    var linkedUser = ctx.getShardManager().retrieveUserById((currentKey.getOwner())).complete();
                    if (linkedUser != null)
                        embedBuilder.addField(lang.get("commands.vipstatus.linked_to"),
                                linkedUser.getAsTag(),
                                true
                        );
                } else {
                    embedBuilder.addField(lang.get("commands.vipstatus.expire"),
                            currentKey.validFor() + " " + lang.get("general.days"),
                            true
                    ).addField(lang.get("commands.vipstatus.key_duration"),
                            currentKey.getDurationDays() + " " + lang.get("general.days"),
                            true
                    );
                }

                ctx.reply(embedBuilder.build());
            }
        }

        @Name("server")
        @Description("Checks the premium status of this server.")
        public static class GuildCommand extends SlashCommand {
            @Override
            protected void process(SlashContext ctx) {
                var dbGuild = ctx.getDBGuild();
                if (!dbGuild.isPremium()) {
                    ctx.reply("commands.vipstatus.guild.not_premium", EmoteReference.ERROR);
                    return;
                }

                var lang = ctx.getLanguageContext();
                var embedBuilder = new EmbedBuilder()
                        .setAuthor(String.format(lang.get("commands.vipstatus.guild.header"), ctx.getGuild().getName()),
                                null, ctx.getAuthor().getEffectiveAvatarUrl());

                var currentKey = ctx.db().getPremiumKey(dbGuild.getData().getPremiumKey());
                if (currentKey == null || currentKey.validFor() < 1) {
                    ctx.reply("commands.vipstatus.guild.not_premium", EmoteReference.ERROR);
                    return;
                }

                var owner = ctx.getShardManager().retrieveUserById(currentKey.getOwner()).complete();
                if (owner == null)
                    owner = Objects.requireNonNull(ctx.getGuild().getOwner()).getUser();

                var patreonInformation = APIUtils.getPledgeInformation(owner.getId());
                var linkedTo = currentKey.getData().getLinkedTo();
                embedBuilder.setColor(Color.CYAN)
                        .setThumbnail(ctx.getGuild().getIconUrl())
                        .setDescription(lang.get("commands.vipstatus.guild.premium")  + "\n" + lang.get("commands.vipstatus.description"))
                        .addField(lang.get("commands.vipstatus.key_owner"), owner.getName() + "#" + owner.getDiscriminator(), true)
                        .addField(lang.get("commands.vipstatus.patreon"),
                                patreonInformation == null ? "Error" : String.valueOf(patreonInformation.left()), true)
                        .addField(lang.get("commands.vipstatus.linked"), String.valueOf(linkedTo != null), false)
                        .setFooter(lang.get("commands.vipstatus.thank_note"), null);

                if (linkedTo != null) {
                    User linkedUser = ctx.getShardManager().retrieveUserById(currentKey.getOwner()).complete();
                    if (linkedUser != null)
                        embedBuilder.addField(lang.get("commands.vipstatus.linked_to"), linkedUser.getName()  + "#" +
                                linkedUser.getDiscriminator(), false);
                } else {
                    embedBuilder
                            .addField(lang.get("commands.vipstatus.expire"), currentKey.validFor() + " " + lang.get("general.days"), true)
                            .addField(lang.get("commands.vipstatus.key_duration"), currentKey.getDurationDays() + " " + lang.get("general.days"), true);
                }

                ctx.reply(embedBuilder.build());
            }
        }
    }
}
