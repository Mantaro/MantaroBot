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

package net.kodehawa.mantarobot.commands.utils;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.regex.Pattern;

/**
 * Converts a number to a string in <a href="http://en.wikipedia.org/wiki/Metric_prefix">metric prefix</a> format.
 * For example, 7800000 will be formatted as '7.8M'.
 * Numbers under 1000 will be unchanged. Refer to the tests for further examples.
 * <p>
 * <p>
 * Base taken out of
 * <a href="https://stackoverflow.com/questions/4753251/how-to-go-about-formatting-1200-to-1-2k-in-java">...</a>
 * Answer isn't the first one so gotta scroll up.
 * Decided to take this answer as I felt like it was the most complete one and had support for reversal.
 * 2022: Reply seems gone now. This isn't exactly the original reply's code anymore, but it was based on it.
 */
public class RoundedMetricPrefixFormat {
    private static final String[] METRIC_PREFIXES = new String[]{"", "k", "M", "G", "T"};

    /**
     * The maximum number of characters in the output, excluding the negative sign
     */
    private static final Integer MAX_LENGTH = 5;
    private static final Pattern TRAILING_DECIMAL_POINT = Pattern.compile("[0-9]+\\.[kMGT]");
    private static final Pattern METRIC_PREFIXED_NUMBER = Pattern.compile("[0-9]+(\\.[0-9])?[kMGT]");

    public StringBuffer format(Object obj, @NotNull StringBuffer output) {
        var number = Double.parseDouble(obj.toString());
        // If the number is negative,
        // convert it to a positive number and add the minus sign to the output at the end
        var isNegative = number < 0;
        number = Math.abs(number);

        var result = new DecimalFormat("##0E0").format(number);

        var index = Character.getNumericValue(result.charAt(result.length() - 1)) / 3;
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
     * This will only output absolute numbers.
     * @param source a number that may have a metric prefix
     * @return Long
     */
    public Double parseObject(String source) {
        if (StringUtils.isNumeric(source)) {
            return Math.abs(Double.parseDouble(source));
        } else if (METRIC_PREFIXED_NUMBER.matcher(source).matches()) {
            var length = source.length();
            var number = source.substring(0, length - 1);
            var metricPrefix = Character.toString(source.charAt(length - 1));

            var absoluteNumber = 0D;
            try {
                absoluteNumber = Double.parseDouble(number);
            } catch (NumberFormatException ignored) { }

            int index = 0;
            for (; index < METRIC_PREFIXES.length; index++) {
                if (METRIC_PREFIXES[index].equals(metricPrefix)) {
                    break;
                }
            }

            var exponent = 3 * index;
            var factor = Math.pow(10, exponent);
            // This should already be absolute, as we do not parse negative numbers with the regex used,
            // nor do we take it into account in the calculations done here.
            return absoluteNumber * (long) factor;
        }

        return null;
    }
}
