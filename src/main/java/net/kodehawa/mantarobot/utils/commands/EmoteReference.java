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

package net.kodehawa.mantarobot.utils.commands;

import java.util.Optional;

public enum EmoteReference {
    ERROR(":x:", "\u274C"),
    ERROR2(":x:", "\u274C"),
    DICE(":game_die:", "\uD83C\uDFB2"),
    SAD(":frowning:", "\uD83D\uDE26"),
    CORRECT(":white_check_mark:", "\u2705"),
    OK(":ok_hand:", "\uD83D\uDC4C"),
    STOP(":octagonal_sign:", "\uD83D\uDED1"),
    TALKING(":speech_balloon:", "\uD83D\uDCAC"),
    CRYING(":sob:", "\uD83D\uDE2D"),
    WARNING(":warning:", "\u26a0"),
    POPPER(":tada:", "\uD83C\uDF89"),
    ZAP(":zap:", "\u26a1"),
    MEGA(":mega:", "\uD83D\uDCE3"),
    CLOCK(":clock1:", "\uD83D\uDD50"),
    CONFUSED(":confused:", "\uD83D\uDE15"),
    WORRIED(":worried:", "\uD83D\uDE1F"),
    THINKING(":thinking:", "\uD83E\uDD14"),
    STOPWATCH(":stopwatch:", "\u23f1"),
    BUY(":inbox_tray:", "\uD83D\uDCE5"),
    SELL(":outbox_tray:", "\uD83D\uDCE4"),
    MARKET(":shopping_car:", "\uD83D\uDED2"),
    MONEY(":money_bag:", "\uD83D\uDCB0"),
    PENCIL(":pencil:", "\uD83D\uDCDD"),
    SMILE(":smile:", "\uD83D\uDE04"),
    PICK(":pick:", "\u26cf"),
    HEART(":heart:", "\u2764"),
    BLUE_HEART(":blue_heart:", "\uD83D\uDC99"),
    RUNNER(":runner:", "\uD83C\uDFC3"),
    POTION1(":milk:", "\uD83E\uDD5B"),
    POTION2(":champagne:", "\uD83C\uDF7E"),
    CREDITCARD(":credit_card:", "\uD83D\uDCB3"),
    POUCH(":pouch:", "\uD83D\uDC5D"),
    REP(":military_medal:", "\uD83C\uDF96"),
    MAGAZINE(":newspaper:", "\uD83D\uDCF0"),
    AXE(":hammer_pick:", "\u2692"),
    DOLLAR(":dollar:", "\uD83D\uDCB5"),
    WOOD(":bamboo:", "\uD83C\uDF8D"),
    EYES(":eyes:", "\uD83D\uDC40"),
    PENNY(":cd:", "\uD83D\uDCBF"),
    RING(":ring:", "\uD83D\uDC8D"),
    WIND(":wind_blowing_face:", "\uD83C\uDF2C"),
    BOOSTER(":runner:", "\uD83C\uDFC3"),
    JOY(":joy:", "\uD83D\uDE02"),
    CROSSED_SWORD(":crossed_sword:", "\u2694"),
    MAG(":mag_right:", "\uD83D\uDD0E"),
    KEY(":key:", "\uD83D\uDD11"),
    DOG(":dog:", "\uD83D\uDC36"),
    CAT(":cat:", "\uD83D\uDC31"),
    HAMSTER(":hamster:", 	"\ud83d\udc39"),
    DOOR(":door:", "\uD83D\uDEAA"),
    LOVE_LETTER(":love_letter:", "\uD83D\uDC8C"),
    NECKLACE(":prayer_beads:", "\uD83D\uDCFF"),
    DIAMOND(":gem:", "\uD83D\uDC8E"),
    TUXEDO(":man_in_tuxedo:", "\uD83E\uDD35"),
    DRESS(":dress:", "\uD83D\uDC57"),
    COOKIE(":cookie:", "\uD83C\uDF6A"),
    CHOCOLATE(":chocolate_bar:", "\uD83C\uDF6B"),
    CLOTHES(":shirt:", "\uD83D\uDC55"),
    SHOES(":athletic_shoe:", "\uD83D\uDC5F"),
    ROSE(":rose:", "\uD83C\uDF39"),
    PARTY(":tada:", "\uD83C\uDF89"),
    STAR(":star:", "\u2b50"),
    HOUSE(":house:", "\uD83C\uDFE0"),
    MARKER(":large_orange_diamond:", "\uD83D\uDD36"),
    BLUE_SMALL_MARKER(":small_blue_diamond:", "\uD83D\uDD39"),
    DUST(":dash:", "\uD83D\uDCA8"),
    TROPHY(":trophy:", "\uD83C\uDFC6"),
    WRENCH(":wrench:", "\ud83d\udd27"),
    WHITE_CIRCLE(":white_circle:", "\u25CB"),
    MUSICAL_NOTE(":musical_note", "\uD83C\uDFB5"),
    DROPLET(":droplet:", "\uD83D\uDCA7"),
    DEV(":desktop:", "\uD83D\uDDA5"),
    ROCK(":rock:", "\uD83E\uDEA8"),
    //Custom emotes.
    LOOT_CRATE("<:lootbox:556992254749966346>", null),
    MINE_CRATE("<:mine_lootbox:556992254623875073>", null),
    FISH_CRATE("<:fish_lootbox:556992254770937876>", null),
    DIAMOND_PICK("<:diamond_pick:492882142557372458>", null),
    STAR_PICK("<:star_pick:492882142993580038>", null),
    COMET_PICK("<:comet_pick:492882142788059146>", null),
    SPARKLE_PICK("<:sparkle_pick:492882143404359690>", null),
    STAR_ROD("<:star_rod:492882143354028064>", null),
    COMET_ROD("<:comet_rod:492882142779670528>", null),
    SPARKLE_ROD("<:sparkle_rod:492882143505154048>", null),
    PREMIUM_MINE_CRATE("<:premium_mine_lootbox:556992254472880129>", null),
    PREMIUM_FISH_CRATE("<:premium_fish_lootbox:556992254724538417>", null),
    SPARKLE_WRENCH("<:sparkle_wrench:551979816262434819>", null),
    COMET_WRENCH("<:comet_wrench:551979816174354443>", null),
    BROKEN_SPARKLE_PICK("<:broken_sparkle_pickaxe:553769632926924813>", null),
    BROKEN_COMET_PICK("<:broken_comet_pickaxe:553769633266532389>", null),
    BROKEN_STAR_PICK("<:broken_star_pickaxe:557349870726414347>", null),
    BROKEN_SPARKLE_ROD("<:broken_sparkle_rod:560885907562037248>", null),
    BROKEN_COMET_ROD("<:broken_comet_rod:560885907004325889>", null),
    BROKEN_STAR_ROD("<:broken_star_rod:560885906857263116>", null),
    MOON_PICK("<:moon_pick:762027644697968683>", null),
    HELLFIRE_PICK("<:hellfire_pick:762027645004808202>", null),
    MOON_ROD("<:moon_rod:762027644845686795>", null),
    HELLFIRE_ROD("<:hellfire_rod:762027645054615602>", null),
    COMET_AXE("<:comet_axe:762027646439129169>", null),
    MOON_AXE("<:moon_axe:762027644941369345>", null),
    DIAMOND_AXE("<:diamond_axe:762027644937306142>", null),
    STAR_AXE("<:star_axe:762027644996157451>", null),
    SPARKLE_AXE("<:sparkle_axe:762027645155541002>", null),
    HELLFIRE_AXE("<:hellfire_axe:762027644971253760>", null),
    BROKEN_SPARKLE_AXE("<:sparkle_axe_broken:762027645105864714>", null),
    BROKEN_COMET_AXE("<:comet_axe_broken:762027644791291935>", null),
    BROKEN_MOON_AXE("<:moon_axe_broken:762027645025386546>", null),
    BROKEN_STAR_AXE("<:star_axe_broken:762027644874915845>", null),
    BROKEN_MOON_ROD("<:moon_rod_broken:762027645054484510>", null),
    BROKEN_MOON_PICK("<:moon_pick_broken:762027644802957365>", null),
    PET_HOUSE("<:dog_house:762425894857605150>", null),
    BROKEN_HELLFIRE_PICK("<:broken_hellfire_pick_plh:787812272708780043>", null),
    BROKEN_HELLFIRE_AXE("<:broken_hellfire_axe_plh:787812333690683393>", null),
    BROKEN_HELLFIRE_ROD("<:broken_hellfire_rod_plh:787812374593536000>", null);

    final String discordNotation;
    final String unicode;

    EmoteReference(String discordNotation, String unicode) {
        this.discordNotation = discordNotation;
        this.unicode = unicode;
    }

    @Override
    public String toString() {
        return Optional.ofNullable(unicode).orElse(discordNotation) + " ";
    }

    public String getDiscordNotation() {
        return discordNotation;
    }

    public String getUnicode() {
        return unicode;
    }
}
