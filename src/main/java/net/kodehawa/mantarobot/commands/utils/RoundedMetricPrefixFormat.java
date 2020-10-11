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

package net.kodehawa.mantarobot.commands.utils;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.regex.Pattern;

/**
 * Converts a number to a string in <a href="http://en.wikipedia.org/wiki/Metric_prefix">metric prefix</a> format.
 * For example, 7800000 will be formatted as '7.8M'. Numbers under 1000 will be unchanged. Refer to the tests for further examples.
 * <p>
 * <p>
 * Literally taken out of https://stackoverflow.com/questions/4753251/how-to-go-about-formatting-1200-to-1-2k-in-java
 * Answer isn't the first one so gotta scroll up.
 * Decided to take this answer as I felt like it was the most complete one and had support for reversal.
 */
public class RoundedMetricPrefixFormat extends Format {
    private static final long serialVersionUID = 1;

    private static final String[] METRIC_PREFIXES = new String[]{"", "k", "M", "G", "T"};

    /**
     * The maximum number of characters in the output, excluding the negative sign
     */
    private static final Integer MAX_LENGTH = 4;

    private static final Pattern TRAILING_DECIMAL_POINT = Pattern.compile("[0-9]+\\.[kMGT]");

    private static final Pattern METRIC_PREFIXED_NUMBER = Pattern.compile("-?[0-9]+(\\.[0-9])?[kMGT]");

    @Override
    public StringBuffer format(Object obj, @NotNull StringBuffer output, @NotNull FieldPosition pos) {
        double number = Double.parseDouble(obj.toString());
        // if the number is negative, convert it to a positive number and add the minus sign to the output at the end
        boolean isNegative = number < 0;
        number = Math.abs(number);

        String result = new DecimalFormat("##0E0").format(number);

        int index = Character.getNumericValue(result.charAt(result.length() - 1)) / 3;
        result = result.replaceAll("E[0-9]", METRIC_PREFIXES[index]);

        while (result.length() > MAX_LENGTH || TRAILING_DECIMAL_POINT.matcher(result).matches()) {
            int length = result.length();
            result = result.substring(0, length - 2) + result.substring(length - 1);
        }

        return output.append(isNegative ? "-" + result : result);
    }

    /**
     * Convert a String produced by <tt>format()</tt> back to a number. This will generally not restore
     * the original number because <tt>format()</tt> is a lossy operation, e.g.
     *
     * <pre>
     * {@code
     * def formatter = new RoundedMetricPrefixFormat()
     * Long number = 5821L
     * String formattedNumber = formatter.format(number)
     * assert formattedNumber == '5.8k'
     *
     * Long parsedNumber = formatter.parseObject(formattedNumber)
     * assert parsedNumber == 5800
     * assert parsedNumber != number
     * }
     * </pre>
     *
     * @param source a number that may have a metric prefix
     * @param pos    if parsing succeeds, this should be updated to the index after the last parsed character
     * @return Long
     */
    @Override
    public Long parseObject(String source, @NotNull ParsePosition pos) {
        if (StringUtils.isNumeric(source)) {
            //I don't need decimals. Original returned Object and not Long because it kept decimals.
            pos.setIndex(source.length());
            return Long.parseLong(source);
        } else if (METRIC_PREFIXED_NUMBER.matcher(source).matches()) {
            boolean isNegative = source.charAt(0) == '-';
            int length = source.length();

            String number = isNegative ? source.substring(1, length - 1) : source.substring(0, length - 1);
            String metricPrefix = Character.toString(source.charAt(length - 1));

            long absoluteNumber = 0;
            try {
                absoluteNumber = Long.parseLong(number);
            } catch (NumberFormatException ignored) {
            }

            int index = 0;

            for (; index < METRIC_PREFIXES.length; index++) {
                if (METRIC_PREFIXES[index].equals(metricPrefix)) {
                    break;
                }
            }

            int exponent = 3 * index;
            double factor = Math.pow(10, exponent);
            factor *= isNegative ? -1 : 1;

            pos.setIndex(source.length());
            float result = (float) absoluteNumber * (long) factor;
            return (long) result;
        }

        return null;
    }
}
