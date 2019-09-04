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

package net.kodehawa.mantarobot.utils;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Class made by AdrianTodt (and modified by Kodehawa) with a lot of useful and fast {@link String} and String[] utilities methods.
 */
public class StringUtils {
    public static final Pattern SPLIT_PATTERN = Pattern.compile("\\s+");

    public static String[] advancedSplitArgs(String args, int expectedArgs) {
        //Final result to work with.
        List<String> result = new ArrayList<>();

        //Whether a string is in a "quotation block".
        boolean inBlock = false;
        //Current "quotation block"
        StringBuilder currentBlock = new StringBuilder();

        for(int i = 0; i < args.length(); i++) {
            char currentChar = args.charAt(i);
            //Flip inBlock if current character is a " or a start/end smart quote character aka “ or ” (but only if it's not escaped)
            if((currentChar == '"' || currentChar == '“' || currentChar == '”') && (i == 0 || args.charAt(i - 1) != '\\' || args.charAt(i - 2) == '\\'))
                inBlock = !inBlock;

            //If character is currently in a block (aka "this is one block"), append to the current block.
            if(inBlock)
                currentBlock.append(currentChar);
            //If current character is a space.
            else if(Character.isSpaceChar(currentChar)) {
                //Check if next or last character is a " or a start/end smart quote character aka “ or ” and remove them.
                if(currentBlock.length() != 0) {
                    if(((currentBlock.charAt(0) == '"' || currentBlock.charAt(0) == '“') &&
                            (currentBlock.charAt(currentBlock.length() - 1) == '"' || currentBlock.charAt(currentBlock.length() - 1) == '”'))
                    ) {
                        //Remove start quote.
                        currentBlock.deleteCharAt(0);
                        //Remove end quote.
                        currentBlock.deleteCharAt(currentBlock.length() - 1);
                    }

                    //Add the unboxed result to the current block.
                    result.add(advancedSplitArgsUnbox(currentBlock.toString()));
                    //Reset the current block: end of block, parse another argument (assume each block is one argument)
                    currentBlock = new StringBuilder();
                }
            } else {
                //Append to current block.
                currentBlock.append(currentChar);
            }
        }

        if(currentBlock.length() != 0) {
            //Check if next or last character is a " or a start/end smart quote character aka “ or ” and remove them.
            if((currentBlock.charAt(0) == '"' || currentBlock.charAt(0) == '“') &&
                    (currentBlock.charAt(currentBlock.length() - 1) == '"' || currentBlock.charAt(currentBlock.length() - 1) == '”')
            ) {
                //Remove start quote.
                currentBlock.deleteCharAt(0);
                //Remove end quote.
                currentBlock.deleteCharAt(currentBlock.length() - 1);
            }

            //Remove escape characters.
            result.add(advancedSplitArgsUnbox(currentBlock.toString()));
        }

        //Convert result to an string array.
        String[] raw = result.toArray(new String[result.size()]);

        //If the amount of arguments this detected is less than one, just return the string as a whole.
        if(expectedArgs < 1)
            return raw;

        //Whatever this is. Really, I have no idea, but it returns the result.
        return normalizeArray(raw, expectedArgs);
    }

    public static String limit(String value, int length) {
        StringBuilder buf = new StringBuilder(value);
        if(buf.length() > length) {
            buf.setLength(length - 3);
            buf.append("...");
        }

        return buf.toString();
    }

    /**
     * Normalize an {@link String} Array.
     *
     * @param raw          the String array to be normalized
     * @param expectedSize the final size of the Array.
     * @return {@link String}[] with the size of expectedArgs
     */
    public static String[] normalizeArray(String[] raw, int expectedSize) {
        String[] normalized = new String[expectedSize];

        Arrays.fill(normalized, "");
        for(int i = 0; i < normalized.length; i++)
            if(i < raw.length && raw[i] != null && !raw[i].isEmpty())
                normalized[i] = raw[i];

        return normalized;
    }

    public static Map<String, String> parse(String[] args) {
        Map<String, String> options = new HashMap<>();

        try {
            for(int i = 0; i < args.length; i++) {
                if(args[i].charAt(0) == '-' || args[i].charAt(0) == '/') //This start with - or /
                {
                    args[i] = args[i].substring(1);
                    if(i + 1 >= args.length || args[i + 1].charAt(0) == '-' || args[i + 1].charAt(0) == '/') //Next start with - (or last arg)
                    {
                        options.put(args[i], "null");
                    } else {
                        options.put(args[i], args[i + 1]);
                        i++;
                    }
                } else {
                    options.put(null, args[i]);
                }
            }

            return options;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public static String parseTime(long duration) {
        final long
                years = duration / 31104000000L,
                months = duration / 2592000000L % 12,
                days = duration / 86400000L % 30,
                hours = duration / 3600000L % 24,
                minutes = duration / 60000L % 60,
                seconds = duration / 1000L % 60;
        String uptime = (years == 0 ? "" : years + " Years, ") + (months == 0 ? "" : months + " Months, ")
                + (days == 0 ? "" : days + " Days, ") + (hours == 0 ? "" : hours + " Hours, ")
                + (minutes == 0 ? "" : minutes + " Minutes, ") + (seconds == 0 ? "" : seconds + " Seconds, ");

        uptime = replaceLast(uptime, ", ", "");
        return replaceLast(uptime, ",", " and");
    }

    public static String removeLines(String str, int startline, int numlines) {
        try(BufferedReader br = new BufferedReader(new StringReader(str))) {
            //String buffer to store contents of the file
            StringBuilder builder = new StringBuilder();

            //Keep track of the line number
            int linenumber = 0;
            numlines--;
            String line;

            while((line = br.readLine()) != null) {
                //Store each valid line in the string buffer
                if(linenumber < startline || linenumber >= startline + numlines)
                    builder.append(line).append("\n");
                linenumber++;
            }

            return builder.toString();
        } catch(RuntimeException e) {
            throw e;
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String replaceLast(String text, String regex, String replacement) {
        return text.replaceFirst("(?s)(.*)" + regex, "$1" + replacement);
    }

    /**
     * Enhanced {@link String#split(String, int)} with SPLIT_PATTERN as the Pattern used.
     *
     * @param args         the {@link String} to be split.
     * @param expectedArgs the size of the returned array of Non-null {@link String}s
     * @return a {@link String}[] with the size of expectedArgs
     */
    public static String[] splitArgs(String args, int expectedArgs) {
        String[] raw = SPLIT_PATTERN.split(args, expectedArgs);
        if(expectedArgs < 1) return raw;
        return normalizeArray(raw, expectedArgs);
    }

    //Basically removes escape characters.
    private static String advancedSplitArgsUnbox(String s) {
        return s.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
