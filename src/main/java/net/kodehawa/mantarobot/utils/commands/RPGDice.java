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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Thanks: https://stackoverflow.com/a/35024121
public class RPGDice {
    private static final Pattern DICE_PATTERN = Pattern.compile("(?<A>\\d*)d(?<B>\\d+)(?>(?<MULT>[x/])(?<C>\\d+))?(?>(?<ADD>[+-])(?<D>\\d+))?");
    private final int rolls;
    private final int faces;
    private final int multiplier;
    private final int additive;

    public RPGDice(int rolls, int faces, int multiplier, int additive) {
        this.rolls = rolls;
        this.faces = faces;
        this.multiplier = multiplier;
        this.additive = additive;
    }

    public int getRolls() {
        return rolls;
    }

    public int getFaces() {
        return faces;
    }

    public int getMultiplier() {
        return multiplier;
    }

    public int getAdditive() {
        return additive;
    }

    private static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private static int getInt(Matcher matcher, String group, int defaultValue) {
        String groupValue = matcher.group(group);
        return isEmpty(groupValue) ? defaultValue : Integer.parseInt(groupValue);
    }

    private static int getSign(Matcher matcher, String group, String positiveValue) {
        String groupValue = matcher.group(group);
        return isEmpty(groupValue) || groupValue.equals(positiveValue) ? 1 : -1;
    }

    public static RPGDice parse(String str) {
        Matcher matcher = DICE_PATTERN.matcher(str);

        if (matcher.matches()) {
            int rolls = getInt(matcher, "A", 1);
            int faces = getInt(matcher, "B", -1);
            int multiplier = getInt(matcher, "C", 1);
            int additive = getInt(matcher, "D", 0);
            int multiplierSign = getSign(matcher, "MULT", "x");
            int additiveSign = getSign(matcher, "ADD", "+");
            return new RPGDice(rolls, faces, multiplier * multiplierSign, additive * additiveSign);
        }

        return null;
    }
}
