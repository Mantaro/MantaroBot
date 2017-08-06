package net.kodehawa.mantarobot.utils;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Class made by AdrianTodt with a lot of useful and fast {@link String} and String[] utilities methods.
 */
public class StringUtils {
    public static final Pattern SPLIT_PATTERN = Pattern.compile("\\s+");

    public static String[] advancedSplitArgs(String args, int expectedArgs) {
        List<String> result = new ArrayList<>();
        boolean inAString = false;
        StringBuilder currentBlock = new StringBuilder();
        for(int i = 0; i < args.length(); i++) {
            if(args.charAt(i) == '"' && (i == 0 || args.charAt(i - 1) != '\\' || args.charAt(i - 2) == '\\'))
                inAString = !inAString;

            if(inAString)
                currentBlock.append(args.charAt(i));
            else if(Character.isSpaceChar(args.charAt(i))) {
                if(currentBlock.length() != 0) {
                    if(currentBlock.charAt(0) == '"' && currentBlock.charAt(currentBlock.length() - 1) == '"') {
                        currentBlock.deleteCharAt(0);
                        currentBlock.deleteCharAt(currentBlock.length() - 1);
                    }

                    result.add(advSplArgUnb(currentBlock.toString()));
                    currentBlock = new StringBuilder();
                }
            } else currentBlock.append(args.charAt(i));
        }

        if(currentBlock.length() != 0) {
            if(currentBlock.charAt(0) == '"' && currentBlock.charAt(currentBlock.length() - 1) == '"') {
                currentBlock.deleteCharAt(0);
                currentBlock.deleteCharAt(currentBlock.length() - 1);
            }

            result.add(advSplArgUnb(currentBlock.toString()));
        }

        String[] raw = result.toArray(new String[result.size()]);

        if(expectedArgs < 1) return raw;
        return normalizeArray(raw, expectedArgs);
    }

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
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
            if(i < raw.length && raw[i] != null && !raw[i].isEmpty()) normalized[i] = raw[i];
        return normalized;
    }

    public static String notNullOrDefault(String str, String defaultStr) {
        if(str == null || str.trim().isEmpty()) return defaultStr;
        return str;
    }

    public static Map<String, String> parse(String[] args) {
        Map<String, String> options = new HashMap<>();

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
            StringBuilder builder = new StringBuilder("");

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

    public static String replaceLast(String text, String regex, String replacement) {
        return text.replaceFirst("(?s)(.*)" + regex, "$1" + replacement);
    }

    /**
     * Enchanced {@link String#split(String, int)} with SPLIT_PATTERN as the Pattern used.
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

    //Short for:
    //advancedSplitArgsUnbox
    private static String advSplArgUnb(String s) {
        return s.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").replace("\\\"", "\"").replace("\\\\", "\\");
    }
}