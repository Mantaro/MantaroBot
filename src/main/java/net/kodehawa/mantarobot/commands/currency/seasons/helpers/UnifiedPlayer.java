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

package net.kodehawa.mantarobot.commands.currency.seasons.helpers;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.kodehawa.mantarobot.commands.currency.seasons.Season;
import net.kodehawa.mantarobot.commands.currency.seasons.SeasonPlayer;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.db.ManagedDatabase;
import net.kodehawa.mantarobot.db.entities.Player;

public class UnifiedPlayer {
    private static final ManagedDatabase managedDatabase = MantaroData.db();

    public Player player;
    public SeasonPlayer seasonalPlayer;

    private UnifiedPlayer() { }

    private UnifiedPlayer(Player player, SeasonPlayer seasonalPlayer) {
        this.player = player;
        this.seasonalPlayer = seasonalPlayer;
    }

    public static UnifiedPlayer of(String userId, Season season) {
        return new UnifiedPlayer(managedDatabase.getPlayer(userId), managedDatabase.getPlayerForSeason(userId, season));
    }

    public static UnifiedPlayer of(User user, Season season) {
        return UnifiedPlayer.of(user.getId(), season);
    }

    public static UnifiedPlayer of(Member member, Season season) {
        return UnifiedPlayer.of(member.getUser().getId(), season);
    }

    /**
     * Adds x amount of money from the player.
     *
     * @param money How much?
     * @return pls dont overflow.
     */
    public boolean addMoney(long money) {
        if (money < 0) {
            return false;
        }

        try {
            player.setCurrentMoney(Math.addExact(player.getCurrentMoney(), money));
            seasonalPlayer.setMoney(Math.addExact(seasonalPlayer.getMoney(), money));
            return true;
        } catch (ArithmeticException ignored) {
            player.setCurrentMoney(0L);
            seasonalPlayer.setMoney(0L);
            return false;
        }
    }

    /**
     * Adds x amount of reputation to a player. Normally 1.
     *
     * @param rep how much?
     */
    public void addReputation(long rep) {
        player.setReputation(player.getReputation() + rep);
        seasonalPlayer.setReputation(seasonalPlayer.getReputation() + rep);
    }

    /**
     * Removes x amount of money from the player. Only goes though if money removed sums more than zero (avoids negative values).
     *
     * @param money How much?
     */
    public boolean removeMoney(long money) {
        if (player.getCurrentMoney() - money < 0 && seasonalPlayer.getMoney() - money < 0) {
            return false;
        }

        if (seasonalPlayer.getMoney() - money > 0) {
            seasonalPlayer.setMoney(Math.subtractExact(seasonalPlayer.getMoney(), money));
        }

        if (player.getCurrentMoney() - money > 0) {
            player.setCurrentMoney(Math.subtractExact(player.getCurrentMoney(), money));
        }

        return true;
    }

    public void save() {
        player.save();
        seasonalPlayer.save();
    }

    public void saveUpdating() {
        player.saveUpdating();
        seasonalPlayer.saveUpdating();
    }

    public void saveAsync() {
        player.saveAsync();
        seasonalPlayer.saveAsync();
    }

    public Player getPlayer() {
        return this.player;
    }

    public SeasonPlayer getSeasonalPlayer() {
        return this.seasonalPlayer;
    }
}
