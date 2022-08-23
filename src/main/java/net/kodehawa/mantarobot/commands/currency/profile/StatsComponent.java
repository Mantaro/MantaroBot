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
import net.kodehawa.mantarobot.commands.currency.item.PlayerEquipment;
import net.kodehawa.mantarobot.commands.currency.item.special.Potion;
import net.kodehawa.mantarobot.core.command.slash.IContext;
import net.kodehawa.mantarobot.core.modules.commands.i18n.I18nContext;
import net.kodehawa.mantarobot.db.entities.DBUser;
import net.kodehawa.mantarobot.db.entities.Player;
import net.kodehawa.mantarobot.db.entities.helpers.PlayerData;
import net.kodehawa.mantarobot.db.entities.helpers.UserData;
import net.kodehawa.mantarobot.utils.Utils;
import net.kodehawa.mantarobot.utils.commands.EmoteReference;

import java.util.function.Function;

// This isn't pretty, but the old one was *less* pretty!
public enum StatsComponent {
    MARKET_USED(EmoteReference.MARKET, lang -> lang.get("commands.profile.stats.market"), holder -> "%,d %s".formatted(holder.getPlayerData().getMarketUsed(), holder.getI18nContext().get("commands.profile.stats.times"))),

    POTION_ACTIVE(EmoteReference.BOOSTER, lang -> lang.get("commands.profile.stats.potion"), holder -> {
        var equippedItems = holder.getUserData().getEquippedItems();
        var potion = (Potion) equippedItems.getEffectItem(PlayerEquipment.EquipmentType.POTION);
        var potionEffect = equippedItems.getCurrentEffect(PlayerEquipment.EquipmentType.POTION);
        var isPotionActive = potion != null &&
                (equippedItems.isEffectActive(PlayerEquipment.EquipmentType.POTION, potion.getMaxUses())
                        || potionEffect.getAmountEquipped() > 1);

        var potionEquipped = 0L;
        if (potion != null) {
            potionEquipped = equippedItems.isEffectActive(
                    PlayerEquipment.EquipmentType.POTION, potion.getMaxUses()
            ) ? potionEffect.getAmountEquipped() : potionEffect.getAmountEquipped() - 1;
        }
        var noPotion = potion == null || !isPotionActive;
        if (noPotion) {
            return "None";
        } else {
            return "%s (%dx)\n%s: %,d %s".formatted(potion.getName(), potionEquipped, holder.getI18nContext().get("commands.profile.stats.times_used"),
                    potionEffect.getTimesUsed(), holder.getI18nContext().get("commands.profile.stats.times")
            );
        }
    }),

    BUFF_ACTIVE(EmoteReference.BOOSTER, lang -> lang.get("commands.profile.stats.buff"), holder -> {
        var equippedItems = holder.getUserData().getEquippedItems();
        var buff = (Potion) equippedItems.getEffectItem(PlayerEquipment.EquipmentType.BUFF);
        var buffEffect = equippedItems.getCurrentEffect(PlayerEquipment.EquipmentType.BUFF);
        var isBuffActive = buff != null &&
                (equippedItems.isEffectActive(PlayerEquipment.EquipmentType.BUFF, buff.getMaxUses())
                        || buffEffect.getAmountEquipped() > 1);
        var buffEquipped = 0L;
        if (buff != null) {
            buffEquipped = equippedItems.isEffectActive(
                    PlayerEquipment.EquipmentType.BUFF, buff.getMaxUses()
            ) ? buffEffect.getAmountEquipped() : buffEffect.getAmountEquipped() - 1;
        }
        var noBuff = buff == null || !isBuffActive;

        if (noBuff) {
            return "None";
        } else {
            return "%s (%dx)\n%s: %,d %s".formatted(buff.getName(), buffEquipped, holder.getI18nContext().get("commands.profile.stats.times_used"),
                    buffEffect.getTimesUsed(), holder.getI18nContext().get("commands.profile.stats.times")
            );
        }
    }),

    EQUIPMENT(EmoteReference.PICK, lang -> lang.get("commands.profile.stats.equipment"), holder -> {
        var equippedItems = holder.getUserData().getEquippedItems();
        return ProfileCmd.parsePlayerEquipment(equippedItems, holder.getI18nContext());
    }),

    EXPERIENCE(EmoteReference.ZAP, lang -> lang.get("commands.profile.stats.experience"), holder -> {
        var experienceNext = (long) (holder.getPlayer().getLevel() * Math.log10(holder.getPlayer().getLevel()) * 1000) +
                (50 * holder.getPlayer().getLevel() / 2);

        return "%,d/%,d XP".formatted(holder.getPlayerData().getExperience(), experienceNext);
    }),

    AUTO_EQUIP(EmoteReference.SATELLITE, lang -> lang.get("commands.profile.stats.autoequip"), holder -> String.valueOf(holder.getUserData().isAutoEquip())),

    ACTIVITY_EXPERIENCE(EmoteReference.ZAP, lang -> lang.get("commands.profile.stats.activity_xp"), holder -> {
        var data = holder.getPlayerData();
        var mine = data.getMiningExperience();
        var fish = data.getFishingExperience();
        var chop = data.getChopExperience();

        return "**Mine:** %,d XP | **Fish:** %,d XP | **Chop:** %,d XP".formatted(mine, fish, chop);
    }),

    SHARKS_CAUGHT(EmoteReference.SHARK, lang -> lang.get("commands.profile.stats.sharks_caught"),
            holder -> "%,d".formatted(holder.getPlayerData().getSharksCaught())
    ),

    CRATES_OPEN(EmoteReference.LOOT_CRATE, lang -> lang.get("commands.profile.stats.crates_open"),
            holder -> "%,d".formatted(holder.getPlayerData().getCratesOpened())
    ),

    TIMES_MOPPED(EmoteReference.MOP, lang -> lang.get("commands.profile.stats.times_mop"),
            holder -> "%,d".formatted(holder.getPlayerData().getTimesMopped())
    ),

    DAILY_COUNT(EmoteReference.CALENDAR, lang -> lang.get("commands.profile.stats.daily"),
            holder -> "%,d %s".formatted(holder.getPlayerData().getDailyStreak(), holder.getI18nContext().get("commands.profile.stats.days"))
    ),

    DAILY_AT(EmoteReference.STOPWATCH, lang -> lang.get("commands.profile.stats.daily_at"), holder -> {
        var playerData = holder.getPlayerData();
        if (playerData.getLastDailyAt() == 0) {
            return holder.getI18nContext().get("commands.profile.stats.never");
        } else {
            return Utils.formatDate(playerData.getLastDailyAt(), holder.getUserData().getLang());
        }
    }),

    WAIFU_CLAIMED(EmoteReference.ROSE, lang -> lang.get("commands.profile.stats.waifu_claimed"),
            holder -> "%,d %s".formatted(holder.getUserData().getTimesClaimed(), holder.getI18nContext().get("commands.profile.stats.times"))
    ),

    WAIFU_LOCKED(EmoteReference.LOCK, lang -> lang.get("commands.profile.stats.waifu_locked"),
            holder -> String.valueOf(holder.getPlayerData().isClaimLocked())
    ),

    DUST_LEVEL(EmoteReference.DUST, lang -> lang.get("commands.profile.stats.dust"),
            holder -> "%d%%".formatted(holder.getUserData().getDustLevel())
    ),

    REMINDER_COUNT(EmoteReference.CALENDAR2, lang -> lang.get("commands.profile.stats.reminders"),
            holder -> "%,d %s".formatted(holder.getUserData().getRemindedTimes(), holder.getI18nContext().get("commands.profile.stats.times"))
    ),

    LANGUAGE(EmoteReference.GLOBE, lang -> lang.get("commands.profile.stats.lang"),
            holder -> (holder.getUserData().getLang() == null ? "en_US" : holder.getUserData().getLang())
    ),

    CASINO_WINS(EmoteReference.MONEY, lang -> lang.get("commands.profile.stats.wins"), holder -> {
        var SEPARATOR_ONE = "\u2009\u2009";
        var playerStats = holder.getContext().db().getPlayerStats(holder.getUser());
        var playerData = holder.getPlayerData();

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
        private final DBUser dbUser;
        private final I18nContext i18nContext;
        private final IContext context;
        private final User user;

        public Holder(IContext context, I18nContext i18nContext, Player player, DBUser dbUser, User member) {
            this.player = player;
            this.dbUser = dbUser;
            this.i18nContext = i18nContext;
            this.context = context;
            this.user = member;
        }

        public Player getPlayer() {
            return player;
        }

        public PlayerData getPlayerData() {
            return player.getData();
        }

        public DBUser getDbUser() {
            return dbUser;
        }

        public UserData getUserData() {
            return getDbUser().getData();
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
