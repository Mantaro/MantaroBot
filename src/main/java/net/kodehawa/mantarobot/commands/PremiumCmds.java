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

package net.kodehawa.mantarobot.commands;

import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.profile.Badge;
import net.kodehawa.mantarobot.core.CommandRegistry;
import net.kodehawa.mantarobot.core.modules.Module;
import net.kodehawa.mantarobot.core.modules.commands.SimpleCommand;
import net.kodehawa.mantarobot.core.modules.commands.SubCommand;
import net.kodehawa.mantarobot.core.modules.commands.TreeCommand;
import net.kodehawa.mantarobot.core.modules.commands.base.Category;
import net.kodehawa.mantarobot.core.modules.commands.base.Command;
import net.kodehawa.mantarobot.core.modules.commands.base.CommandPermission;
import net.kodehawa.mantarobot.core.modules.commands.base.Context;
import net.kodehawa.mantarobot.core.modules.commands.help.HelpContent;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.data.Config;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.DBGuild;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.PremiumKey;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.log.LogUtils;
import net.kodehawa.mantarobot.utils.APIUtils;
import net.kodehawa.mantarobot.utils.Pair;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;
import net.kodehawa.mantarobot.utils.commands.IncreasingRateLimiter;

import java.awt.*;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.System.currentTimeMillis;
import static net.kodehawa.mantarobot.utils.Utils.handleIncreasingRatelimit;

@Module
@SuppressWarnings("unused")
public class PremiumCmds {
    private final Config config = MantaroData.config().get();

    @Subscribe
    public void comprevip(CommandRegistry cr) {
        cr.register("activatekey", new SimpleCommand(Category.UTILS) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                final ManagedDatabase db = ctx.db();

                if (config.isPremiumBot()) {
                    ctx.sendLocalized("commands.activatekey.mp", EmoteReference.WARNING);
                    return;
                }

                if (!(args.length == 0) && args[0].equalsIgnoreCase("check")) {
                    PremiumKey currentKey = db.getPremiumKey(ctx.getDBUser().getData().getPremiumKey());

                    if (currentKey != null && currentKey.isEnabled() && currentTimeMillis() < currentKey.getExpiration()) { //Should always be enabled...
                        ctx.sendLocalized("commands.activatekey.check.key_valid_for", EmoteReference.EYES, currentKey.validFor());
                    } else {
                        ctx.sendLocalized("commands.activatekey.check.no_key_found", EmoteReference.ERROR);
                    }

                    return;
                }

                if (args.length < 1) {
                    ctx.sendLocalized("commands.activatekey.no_key_provided", EmoteReference.ERROR);
                    return;
                }

                PremiumKey key = db.getPremiumKey(args[0]);

                if (key == null || (key.isEnabled())) {
                    ctx.sendLocalized("commands.activatekey.invalid_key", EmoteReference.ERROR);
                    return;
                }

                PremiumKey.Type scopeParsed = key.getParsedType();

                if (scopeParsed.equals(PremiumKey.Type.GUILD)) {
                    DBGuild guild = ctx.getDBGuild();

                    PremiumKey currentKey = db.getPremiumKey(guild.getData().getPremiumKey());

                    if (currentKey != null && currentKey.isEnabled() && currentTimeMillis() < currentKey.getExpiration()) { //Should always be enabled...
                        ctx.sendLocalized("commands.activatekey.guild_already_premium", EmoteReference.POPPER);
                        return;
                    }

                    //Add to keys claimed storage if it's NOT your first key (count starts at 2/2 = 1)
                    if (!ctx.getAuthor().getId().equals(key.getOwner())) {
                        DBUser ownerUser = db.getUser(key.getOwner());
                        ownerUser.getData().getKeysClaimed().put(ctx.getAuthor().getId(), key.getId());
                        ownerUser.saveAsync();
                    }

                    key.activate(180);
                    ctx.sendLocalized("commands.activatekey.guild_successful", EmoteReference.POPPER, key.getDurationDays());
                    guild.getData().setPremiumKey(key.getId());
                    guild.saveAsync();

                    return;
                }

                if (scopeParsed.equals(PremiumKey.Type.USER)) {
                    DBUser dbUser = ctx.getDBUser();
                    Player player = ctx.getPlayer();

                    PremiumKey currentUserKey = db.getPremiumKey(dbUser.getData().getPremiumKey());

                    if (dbUser.isPremium()) {
                        ctx.sendLocalized("commands.activatekey.user_already_premium", EmoteReference.POPPER);
                        return;
                    }

                    if (ctx.getAuthor().getId().equals(key.getOwner())) {
                        player.getData().addBadgeIfAbsent(Badge.DONATOR);
                        player.saveAsync();
                    }

                    //Add to keys claimed storage if it's NOT your first key (count starts at 2/2 = 1)
                    if (!ctx.getAuthor().getId().equals(key.getOwner())) {
                        DBUser ownerUser = db.getUser(key.getOwner());
                        ownerUser.getData().getKeysClaimed().put(ctx.getAuthor().getId(), key.getId());
                        ownerUser.saveAsync();
                    }

                    key.activate(ctx.getAuthor().getId().equals(key.getOwner()) ? 365 : 180);
                    ctx.sendLocalized("commands.activatekey.user_successful", EmoteReference.POPPER, key.getDurationDays());
                    dbUser.getData().setPremiumKey(key.getId());
                    dbUser.saveAsync();
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Activates a premium key.\n" +
                                "Example: `~>activatekey a4e98f07-1a32-4dcc-b53f-c540214d54ec`. No, that isn't a valid key.")
                        .setUsage("`~>activatekey <key>`")
                        .addParameter("key", "The key to use. If it's a server key, make sure to run this command in the server where you want to enable premium on.")
                        .build();
            }
        });
    }

    //@Subscribe
    public void claimkey(CommandRegistry cr) {
        final IncreasingRateLimiter rateLimiter = new IncreasingRateLimiter.Builder()
                .spamTolerance(3)
                .limit(2)
                .cooldown(1, TimeUnit.MINUTES)
                .maxCooldown(5, TimeUnit.MINUTES)
                .pool(MantaroData.getDefaultJedisPool())
                .prefix("claimkey")
                .build();

        cr.register("claimkey", new SimpleCommand(Category.UTILS) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if (config.isPremiumBot()) {
                    ctx.sendLocalized("commands.activatekey.mp", EmoteReference.WARNING);
                    return;
                }

                String type = "";
                PremiumKey.Type scopeParsed = PremiumKey.Type.USER;
                if (args.length > 0) {
                    try {
                        scopeParsed = PremiumKey.Type.valueOf(args[0].toUpperCase());
                    } catch (IllegalArgumentException e) {
                        ctx.sendLocalized("commands.claimkey.invalid_scope", EmoteReference.ERROR);
                    }
                }

                final ManagedDatabase db = MantaroData.db();
                final User author = ctx.getAuthor();
                DBUser dbUser = ctx.getDBUser();

                //left: isPatron, right: pledgeAmount, basically.
                Pair<Boolean, String> pledgeInfo = APIUtils.getPledgeInformation(author.getId());
                if (pledgeInfo == null || !pledgeInfo.getLeft() || !dbUser.isPremium()) {
                    ctx.sendLocalized("commands.claimkey.not_patron", EmoteReference.ERROR);
                    return;
                }

                double pledgeAmount = Double.parseDouble(pledgeInfo.getRight());
                UserData data = dbUser.getData();

                //Check for pledge changes on DBUser#isPremium
                if (pledgeAmount == 1 || data.getKeysClaimed().size() >= (pledgeAmount / 2)) {
                    ctx.sendLocalized("commands.claimkey.already_top", EmoteReference.ERROR);
                    return;
                }

                if(!Utils.handleIncreasingRatelimit(rateLimiter, ctx.getAuthor(), ctx.getEvent(), null)) {
                    return;
                }

                final var scope = scopeParsed;

                //Send message in a DM (it's private after all)
                ctx.getAuthor().openPrivateChannel()
                        .flatMap(privateChannel -> {
                            PremiumKey newKey = PremiumKey.generatePremiumKey(author.getId(), scope, true);
                            I18nContext languageContext = ctx.getLanguageContext();

                            //Placeholder so they don't spam key creation. Save as random UUID first, to avoid conflicting.
                            data.getKeysClaimed().put(UUID.randomUUID().toString(), newKey.getId());
                            int amountClaimed = data.getKeysClaimed().size();

                            privateChannel.sendMessageFormat(languageContext.get("commands.claimkey.successful"),
                                    EmoteReference.HEART, newKey.getId(), amountClaimed, (int) ((pledgeAmount / 2) - amountClaimed), newKey.getParsedType()
                            ).queue();

                            dbUser.saveAsync();
                            newKey.saveAsync();

                            //Assume it all went well.
                            //This one is actually needed, lol.
                            return ctx.getChannel().sendMessageFormat(languageContext.get("commands.claimkey.success"), EmoteReference.CORRECT);
                        }).queue(null, error -> ctx.sendLocalized("commands.claimkey.cant_dm", EmoteReference.ERROR));
            }
        });
    }

    @Subscribe
    public void vipstatus(CommandRegistry cr) {
        final ManagedDatabase db = MantaroData.db();

        TreeCommand vipstatusCmd = (TreeCommand) cr.register("vipstatus", new TreeCommand(Category.INFO) {
            @Override
            public Command defaultTrigger(Context ctx, String mainCommand, String commandName) {
                return new SubCommand() {
                    @Override
                    protected void call(Context ctx, String content) {
                        if(config.isPremiumBot()) {
                            ctx.sendLocalized("commands.activatekey.mp", EmoteReference.WARNING);
                            return;
                        }

                        Member member = null;
                        User toCheck = ctx.getAuthor();
                        I18nContext languageContext = ctx.getLanguageContext();

                        if(!content.isEmpty()) {
                            member = Utils.findMember(ctx.getEvent(), languageContext, content);
                            //Search failed, return.
                            if (member == null) {
                                return;
                            }
                        }

                        boolean isLookup = member != null;
                        if (isLookup)
                            toCheck = member.getUser();

                        DBUser dbUser = db.getUser(toCheck);
                        UserData data = dbUser.getData();

                        if(!dbUser.isPremium()) {
                            ctx.sendLocalized("commands.vipstatus.user.not_premium", EmoteReference.ERROR);
                            return;
                        }

                        EmbedBuilder embedBuilder = new EmbedBuilder()
                                .setAuthor(isLookup ? String.format(languageContext.get("commands.vipstatus.user.header_other"), toCheck.getName())
                                        : languageContext.get("commands.vipstatus.user.header"), null, toCheck.getEffectiveAvatarUrl()
                                );

                        PremiumKey currentKey = db.getPremiumKey(data.getPremiumKey());

                        if(currentKey == null || currentKey.validFor() < 1) {
                            ctx.sendLocalized("commands.vipstatus.user.not_premium", EmoteReference.ERROR);
                            return;
                        }

                        User owner = MantaroBot.getInstance().getShardManager().getUserById(currentKey.getOwner());
                        boolean marked = false;
                        if (owner == null) {
                            marked = true;
                            owner = ctx.getAuthor();
                        }

                        //Give the badge to the key owner, I'd guess?
                        if(!marked && isLookup) {
                            Player p = db.getPlayer(owner);
                            if (p.getData().addBadgeIfAbsent(Badge.DONATOR_2))
                                p.saveAsync();
                        }

                        Pair<Boolean, String> patreonInformation = APIUtils.getPledgeInformation(owner.getId());
                        String linkedTo = currentKey.getData().getLinkedTo();
                        int amountClaimed = data.getKeysClaimed().size();

                        embedBuilder.setColor(Color.CYAN)
                                .setThumbnail(toCheck.getEffectiveAvatarUrl())
                                .setDescription(languageContext.get("commands.vipstatus.user.premium") + "\n" + languageContext.get("commands.vipstatus.description"))
                                .addField(languageContext.get("commands.vipstatus.key_owner"), owner.getName() + "#" + owner.getDiscriminator(), true)
                                .addField(languageContext.get("commands.vipstatus.patreon"),
                                        patreonInformation == null ? "Error" : String.valueOf(patreonInformation.getLeft()), true)
                                .addField(languageContext.get("commands.vipstatus.keys_claimed"), String.valueOf(amountClaimed), false)
                                .addField(languageContext.get("commands.vipstatus.linked"), String.valueOf(linkedTo != null), false)
                                .setFooter(languageContext.get("commands.vipstatus.thank_note"), null);

                        try {
                            //User has more keys than what the system would allow. Warn.
                            if (patreonInformation != null && patreonInformation.getLeft()) {
                                double patreonAmount = Double.parseDouble(patreonInformation.getRight());

                                if((patreonAmount / 2) - amountClaimed < 0) {
                                    LogUtils.log(
                                            String.format(
                                                    "%s has more keys claimed than given keys, dumping keys:\n%s\nCurrently pledging: %s, Claimed keys: %s, Should have %s total keys.", owner.getId(),
                                                    Utils.paste(
                                                            data.getKeysClaimed().entrySet().stream().map(entry ->
                                                                    "to:" + entry.getKey() + ", key:" + entry.getValue()).collect(Collectors.joining("\n")
                                                            )
                                                    ), patreonAmount, amountClaimed, (patreonAmount / 2)
                                            )
                                    );
                                }
                            }
                        } catch (Exception ignored) { }

                        if (linkedTo != null) {
                            User linkedUser = MantaroBot.getInstance().getShardManager().getUserById(currentKey.getOwner());
                            if(linkedUser != null)
                                embedBuilder.addField(languageContext.get("commands.vipstatus.linked_to"), linkedUser.getName() +
                                        "#" + linkedUser.getDiscriminator(), true);
                        } else {
                            embedBuilder.addField(languageContext.get("commands.vipstatus.expire"), currentKey.validFor() + " " + languageContext.get("general.days"), true)
                                    .addField(languageContext.get("commands.vipstatus.key_duration"), currentKey.getDurationDays() + " " + languageContext.get("general.days"), true);
                        }

                        ctx.send(embedBuilder.build());
                    }
                };
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Checks your premium status or the guild status.")
                        .setUsage("`~>vipstatus` - Returns your premium key status\n" +
                                "`~>vipstatus guild` - Return this guild's premium status.")
                        .build();
            }
        });

        vipstatusCmd.addSubCommand("guild", new SubCommand() {
            @Override
            protected void call(Context ctx, String content) {
                DBGuild dbGuild = ctx.getDBGuild();
                I18nContext languageContext = ctx.getLanguageContext();

                if (!dbGuild.isPremium()) {
                    ctx.sendLocalized("commands.vipstatus.guild.not_premium", EmoteReference.ERROR);
                    return;
                }

                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setAuthor(String.format(languageContext.get("commands.vipstatus.guild.header"), ctx.getGuild().getName()),
                                null, ctx.getAuthor().getEffectiveAvatarUrl());

                PremiumKey currentKey = db.getPremiumKey(dbGuild.getData().getPremiumKey());

                if(currentKey == null || currentKey.validFor() < 1) {
                    ctx.sendLocalized("commands.vipstatus.guild.not_premium", EmoteReference.ERROR);
                    return;
                }

                User owner = MantaroBot.getInstance().getShardManager().getUserById(currentKey.getOwner());
                if (owner == null)
                    owner = Objects.requireNonNull(ctx.getGuild().getOwner()).getUser();

                Pair<Boolean, String> patreonInformation = APIUtils.getPledgeInformation(owner.getId());
                String linkedTo = currentKey.getData().getLinkedTo();
                embedBuilder.setColor(Color.CYAN)
                        .setThumbnail(ctx.getGuild().getIconUrl())
                        .setDescription(languageContext.get("commands.vipstatus.guild.premium")  + "\n" + languageContext.get("commands.vipstatus.description"))
                        .addField(languageContext.get("commands.vipstatus.key_owner"), owner.getName() + "#" + owner.getDiscriminator(), true)
                        .addField(languageContext.get("commands.vipstatus.patreon"),
                                patreonInformation == null ? "Error" : String.valueOf(patreonInformation.getLeft()), true)
                        .addField(languageContext.get("commands.vipstatus.linked"), String.valueOf(linkedTo != null), false)
                        .setFooter(languageContext.get("commands.vipstatus.thank_note"), null);

                if (linkedTo != null) {
                    User linkedUser = MantaroBot.getInstance().getShardManager().getUserById(currentKey.getOwner());
                    if(linkedUser != null)
                        embedBuilder.addField(languageContext.get("commands.vipstatus.linked_to"), linkedUser.getName()  + "#" +
                                linkedUser.getDiscriminator(), false);
                } else {
                    embedBuilder
                            .addField(languageContext.get("commands.vipstatus.expire"), currentKey.validFor() + " " + languageContext.get("general.days"), true)
                            .addField(languageContext.get("commands.vipstatus.key_duration"), currentKey.getDurationDays() + " " + languageContext.get("general.days"), true);
                }

                ctx.send(embedBuilder.build());
            }
        });
    }

    //Won't translate this. Owner command.
    @Subscribe
    public void invalidatekey(CommandRegistry cr) {
        cr.register("invalidatekey", new SimpleCommand(Category.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                if(args.length == 0) {
                    ctx.send(EmoteReference.ERROR + "Give me a key to invalidate!");
                    return;
                }

                PremiumKey key = MantaroData.db().getPremiumKey(args[0]);
                if(key == null) {
                    ctx.send("Invalid key.");
                    return;
                }

                DBUser dbUser = MantaroData.db().getUser(key.getOwner());
                Map<String, String> keysClaimed = dbUser.getData().getKeysClaimed();

                keysClaimed.remove(Utils.getKeyByValue(keysClaimed, key.getId()));
                dbUser.save();
                key.delete();

                ctx.send("Invalidated key " + args[0]);
            }
        });
    }

    //Won't translate this. Owner command.
    @Subscribe
    public void createkey(CommandRegistry cr) {
        cr.register("createkey", new SimpleCommand(Category.OWNER, CommandPermission.OWNER) {
            @Override
            protected void call(Context ctx, String content, String[] args) {
                Map<String, String> t = ctx.getOptionalArguments();

                if (args.length < 3) {
                    ctx.send(EmoteReference.ERROR + "You need to provide a scope, an id and whether this key is linked (example: guild 1558674582032875529 true)");
                    return;
                }

                String scope = args[0];
                String owner = args[1];
                boolean linked = Boolean.parseBoolean(args[2]);

                PremiumKey.Type scopeParsed = null;
                try {
                    scopeParsed = PremiumKey.Type.valueOf(scope.toUpperCase()); //To get the ordinal
                } catch (IllegalArgumentException ignored) { }

                if (scopeParsed == null) {
                    ctx.send(EmoteReference.ERROR + "Invalid scope (Valid ones are: `user` or `guild`)");
                    return;
                }

                //This method generates a premium key AND saves it on the database! Please use this result!
                PremiumKey generated = PremiumKey.generatePremiumKey(owner, scopeParsed, linked);
                if(t.containsKey("mobile")) {
                    ctx.send(generated.getId());
                } else {
                    ctx.send(EmoteReference.CORRECT + String.format("Generated: `%s` (S: %s) **[NOT ACTIVATED]** (Linked: %s)",
                            generated.getId(), generated.getParsedType(), linked));
                }
            }

            @Override
            public HelpContent help() {
                return new HelpContent.Builder()
                        .setDescription("Makes a premium key, what else? Needs scope (user or guild) and id. Also add true or false for linking status at the end")
                        .build();
            }
        });
    }
}
