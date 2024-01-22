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

package net.kodehawa.mantarobot.commands.currency.profile;

import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.commands.ProfileCmd;
import net.kodehawa.mantarobot.core.command.slash.IContext;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.db.entities.MongoUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.function.Function;

// This isn't pretty, but the old one was *less* pretty!
public enum StatsComponent {
    MARKET_USED(EmoteReference.MARKET, lang -> lang.get("commands.profile.stats.market"), holder -> "%,d %s".formatted(holder.getPlayer().getMarketUsed(), holder.getI18nContext().get("commands.profile.stats.times"))),
    EQUIPMENT(EmoteReference.PICK, lang -> lang.get("commands.profile.stats.equipment"), holder -> {
        var equippedItems = holder.getDbUser().getEquippedItems();
        return ProfileCmd.parsePlayerEquipment(equippedItems, holder.getI18nContext());
    }),

    EXPERIENCE(EmoteReference.ZAP, lang -> lang.get("commands.profile.stats.experience"), holder -> {
        var experienceNext = (long) (holder.getPlayer().getLevel() * Math.log10(holder.getPlayer().getLevel()) * 1000) +
                (50 * holder.getPlayer().getLevel() / 2);

        return "%,d/%,d XP".formatted(holder.getPlayer().getExperience(), experienceNext);
    }),

    AUTO_EQUIP(EmoteReference.SATELLITE, lang -> lang.get("commands.profile.stats.autoequip"), holder -> String.valueOf(holder.getDbUser().isAutoEquip())),

    ACTIVITY_EXPERIENCE(EmoteReference.ZAP, lang -> lang.get("commands.profile.stats.activity_xp"), holder -> {
        var data = holder.getPlayer();
        var mine = data.getMiningExperience();
        var fish = data.getFishingExperience();
        var chop = data.getChopExperience();

        return "**Mine:** %,d XP | **Fish:** %,d XP | **Chop:** %,d XP".formatted(mine, fish, chop);
    }),

    SHARKS_CAUGHT(EmoteReference.SHARK, lang -> lang.get("commands.profile.stats.sharks_caught"),
            holder -> "%,d".formatted(holder.getPlayer().getSharksCaught())
    ),

    CRATES_OPEN(EmoteReference.LOOT_CRATE, lang -> lang.get("commands.profile.stats.crates_open"),
            holder -> "%,d".formatted(holder.getPlayer().getCratesOpened())
    ),

    TIMES_MOPPED(EmoteReference.MOP, lang -> lang.get("commands.profile.stats.times_mop"),
            holder -> "%,d".formatted(holder.getPlayer().getTimesMopped())
    ),

    DAILY_COUNT(EmoteReference.CALENDAR, lang -> lang.get("commands.profile.stats.daily"),
            holder -> "%,d %s".formatted(holder.getPlayer().getDailyStreak(), holder.getI18nContext().get("commands.profile.stats.days"))
    ),

    DAILY_AT(EmoteReference.STOPWATCH, lang -> lang.get("commands.profile.stats.daily_at"), holder -> {
        var playerData = holder.getPlayer();
        if (playerData.getLastDailyAt() == 0) {
            return holder.getI18nContext().get("commands.profile.stats.never");
        } else {
            return "<t:%s>".formatted(playerData.getLastDailyAt() / 1000);
        }
    }),

    WAIFU_CLAIMED(EmoteReference.ROSE, lang -> lang.get("commands.profile.stats.waifu_claimed"),
            holder -> "%,d %s".formatted(holder.getDbUser().getTimesClaimed(), holder.getI18nContext().get("commands.profile.stats.times"))
    ),

    WAIFU_LOCKED(EmoteReference.LOCK, lang -> lang.get("commands.profile.stats.waifu_locked"),
            holder -> String.valueOf(holder.getPlayer().isClaimLocked())
    ),

    DUST_LEVEL(EmoteReference.DUST, lang -> lang.get("commands.profile.stats.dust"),
            holder -> "%d%%".formatted(holder.getDbUser().getDustLevel())
    ),

    REMINDER_COUNT(EmoteReference.CALENDAR2, lang -> lang.get("commands.profile.stats.reminders"),
            holder -> "%,d %s".formatted(holder.getDbUser().getRemindedTimes(), holder.getI18nContext().get("commands.profile.stats.times"))
    ),

    LANGUAGE(EmoteReference.GLOBE, lang -> lang.get("commands.profile.stats.lang"),
            holder -> (holder.getDbUser().getLang() == null ? "en_US" : holder.getDbUser().getLang())
    ),

    CASINO_WINS(EmoteReference.MONEY, lang -> lang.get("commands.profile.stats.wins"), holder -> {
        var SEPARATOR_ONE = "\u2009\u2009";
        var playerStats = holder.getContext().db().getPlayerStats(holder.getUser());
        var playerData = holder.getPlayer();

        return String.format("\n\u3000%1$s" +
                        "%2$s%3$sGamble: %4$,d, Slots: %5$,d, Game: %6$,d (times)",
                SEPARATOR_ONE, EmoteReference.CREDITCARD, SEPARATOR_ONE,
                playerStats.getGambleWins(), playerStats.getSlotsWins(),
                playerData.getGamesWon()
        );
    });

    private final Function<I18nContext, String> name;
    private final Function<Holder, String> content;
    private final EmoteReference emote;

    StatsComponent(EmoteReference emote, Function<I18nContext, String> name, Function<Holder, String> content) {
        this.emote = emote;
        this.name = name;
        this.content = content;
    }

    public String getContent(Holder holder) {
        return content.apply(holder);
    }

    public String getName(IContext ctx) {
        return name.apply(ctx.getLanguageContext());
    }

    public String getEmoji() {
        return emote.toHeaderString();
    }

    public static class Holder {
        private final Player player;
        private final MongoUser dbUser;
        private final I18nContext i18nContext;
        private final IContext context;
        private final User user;

        public Holder(IContext context, I18nContext i18nContext, Player player, MongoUser dbUser, User member) {
            this.player = player;
            this.dbUser = dbUser;
            this.i18nContext = i18nContext;
            this.context = context;
            this.user = member;
        }

        public Player getPlayer() {
            return player;
        }

        public MongoUser getDbUser() {
            return dbUser;
        }

        public I18nContext getI18nContext() {
            return i18nContext;
        }

        public IContext getContext() {
            return context;
        }

        public User getUser() {
            return user;
        }
    }
}
