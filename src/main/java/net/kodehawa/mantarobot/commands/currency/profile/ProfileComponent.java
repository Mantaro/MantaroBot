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

package net.kodehawa.mantarobot.commands.currency.profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.dv8tion.jda.core.entities.User;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.commands.currency.seasons.SeasonalPlayer;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Marriage;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.Inventory;
import net.kodehawa.mantarobot.db.entities.helpers.PlayerData;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ProfileComponent {
    HEADER(null, i18nContext -> String.format(i18nContext.get("commands.profile.badge_header"), EmoteReference.TROPHY), (holder, i18nContext) -> {
        PlayerData playerData = holder.getPlayer().getData();
        if(holder.getBadges().isEmpty() || !playerData.isShowBadge())
            return "None";

        if(playerData.getMainBadge() != null)
            return String.format("**%s**\n", playerData.getMainBadge());
        else
            return String.format("**%s**\n", holder.getBadges().get(0));
    }, true, false),
    CREDITS(EmoteReference.DOLLAR, i18nContext -> i18nContext.get("commands.profile.credits"), (holder, i18nContext) ->
            "$ " + (holder.isSeasonal() ? holder.getSeasonalPlayer().getMoney() : holder.getPlayer().getMoney())
    ),
    REPUTATION(EmoteReference.REP, i18nContext -> i18nContext.get("commands.profile.rep"), (holder, i18nContext) ->
            holder.isSeasonal() ? String.valueOf(holder.getSeasonalPlayer().getReputation()) : String.valueOf(holder.getPlayer().getReputation())
    ),
    LEVEL(EmoteReference.ZAP, i18nContext -> i18nContext.get("commands.profile.level"), (holder, i18nContext) -> {
        Player player = holder.getPlayer();
        return String.format("%d (%s: %d)", player.getLevel(), i18nContext.get("commands.profile.xp"), player.getData().getExperience());
    }),
    BIRTHDAY(EmoteReference.POPPER, i18nContext -> i18nContext.get("commands.profile.birthday"), (holder, i18nContext) -> {
        UserData data = holder.getDbUser().getData();
        if(data.getBirthday() == null)
            return i18nContext.get("commands.profile.not_specified");
        else
            return data.getBirthday().substring(0, 5);
    }),
    MARRIAGE(EmoteReference.HEART, i18nContext -> i18nContext.get("commands.profile.married"), (holder, i18nContext) -> {
        Player player = holder.getPlayer();
        PlayerData playerData = player.getData();
        //LEGACY SUPPORT
        User marriedTo = (playerData.getMarriedWith() == null || playerData.getMarriedWith().isEmpty()) ? null : MantaroBot.getInstance().getUserById(playerData.getMarriedWith());

        //New marriage support.
        UserData userData = holder.getDbUser().getData();
        Marriage currentMarriage = userData.getMarriage();
        User marriedToNew = null;
        boolean isNewMarriage = false;

        //Expecting save to work in PlayerCmds, not here, just handle this here.
        if(currentMarriage != null) {
            String marriedToId = currentMarriage.getOtherPlayer(holder.getUser().getId());
            if(marriedToId != null) {
                marriedToNew = MantaroBot.getInstance().getUserById(marriedToId);
                playerData.setMarriedWith(null); //delete old marriage
                marriedTo = null;
                isNewMarriage = true;
            }
        }

        if(marriedTo == null && marriedToNew == null) {
            return i18nContext.get("commands.profile.nobody");
        } else if(isNewMarriage) {
            return String.format("%s#%s", marriedToNew.getName(), marriedToNew.getDiscriminator());
        } else {
            return String.format("%s#%s", marriedTo.getName(), marriedTo.getDiscriminator());
        }
    }),
    INVENTORY(EmoteReference.POUCH, i18nContext -> i18nContext.get("commands.profile.inventory"), (holder, i18nContext) -> {
        Inventory inv = holder.isSeasonal() ? holder.getSeasonalPlayer().getInventory() : holder.getPlayer().getInventory();
        return inv.asList().stream().map(i -> i.getItem().getEmoji()).collect(Collectors.joining("  "));
    }),
    BADGES(EmoteReference.HEART, i18nContext -> i18nContext.get("commands.profile.badges"), (holder, i18nContext) -> {
        String displayBadges = holder.getBadges().stream().map(Badge::getUnicode).limit(5).collect(Collectors.joining("  "));

        if(displayBadges.isEmpty())
            return i18nContext.get("commands.profile.no_badges");
        else
            return displayBadges;
    }, true, true),
    FOOTER(null, null, (holder, i18nContext) -> {
        UserData userData = holder.getDbUser().getData();
        String timezone;

        if(userData.getTimezone() == null)
            timezone = i18nContext.get("commands.profile.no_timezone");
        else
            timezone = userData.getTimezone();

        String seasonal = holder.isSeasonal() ? " | Seasonal profile" : "";

        return String.format("%s%s", String.format(i18nContext.get("commands.profile.timezone_user"), timezone), seasonal);
    }, false);

    //See: getTitle()
    private EmoteReference emoji;
    private Function<I18nContext, String> title;

    @Getter
    private BiFunction<Holder, I18nContext, String> content;
    @Getter
    private boolean assignable;
    @Getter
    private boolean inline;

    ProfileComponent(EmoteReference emoji, Function<I18nContext, String> title, BiFunction<Holder, I18nContext, String> content, boolean isAssignable, boolean inline) {
        this.emoji = emoji;
        this.title = title;
        this.content = content;
        this.assignable = isAssignable;
        this.inline = inline;
    }

    ProfileComponent(EmoteReference emoji, Function<I18nContext, String> title, BiFunction<Holder, I18nContext, String> content, boolean isAssignable) {
        this.emoji = emoji;
        this.title = title;
        this.content = content;
        this.assignable = isAssignable;
        this.inline = true;
    }

    ProfileComponent(EmoteReference emoji, Function<I18nContext, String> title, BiFunction<Holder, I18nContext, String> content) {
        this.emoji = emoji;
        this.title = title;
        this.content = content;
        this.assignable = true;
        this.inline = true;
    }

    public String getTitle(I18nContext context) {
        return (emoji == null ? "" : emoji) + title.apply(context);
    }

    /**
     * Looks up the component based on a String value, if nothing is found returns null.
     *
     * @param name The String value to match
     * @return The component, or null if nothing is found.
     */
    public static ProfileComponent lookupFromString(String name) {
        for(ProfileComponent c : ProfileComponent.values()) {
            if (c.name().equalsIgnoreCase(name)) {
                return c;
            }
        }
        return null;
    }

    @AllArgsConstructor
    @Data
    public static class Holder {
        private User user;
        private Player player;
        private SeasonalPlayer seasonalPlayer;
        private DBUser dbUser;
        private List<Badge> badges;

        public boolean isSeasonal() {
            return seasonalPlayer != null;
        }
    }
}
