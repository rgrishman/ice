package edu.nyu.jet.ice.utils;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.Normalizer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for KBP lexical processing
 *
 * @author yhe
 * @version 1.0
 */
public class LexUtils {

    public static final Pattern NORMALIZED_LATIN_PATTERN = Pattern.compile("[A-Za-z0-9\\-]+");

    /**
     * Normalize input string.
     * 1) Compatibility Decomposition, followed by Canonical Composition
     * 2) Diacritics removal
     * 3) Normalize spaces and to lowercase
     *
     * @param s input string
     * @return normalized string
     */
    public static String normalize(String s) {
        return StringSimplifier.simplifiedString(s).trim();
    }

    public static String powerTrim(String s) {
        return s.replaceAll("^\\W+", "")
                .replaceAll("\\W+$", "");
    }

    public static String normalizeMySql(String s) {
        return s.replaceAll("\\\\'", "'");
    }

    public static boolean isNormalizedLatinString(String s) {
        Matcher matcher = NORMALIZED_LATIN_PATTERN.matcher(powerTrim(normalize(s)));
        return matcher.matches();
    }

    public static String stripQuote(String s) {
        if (s.length() < 2) {
            return s;
        }
        else {
            if (s.startsWith("\"") && s.endsWith("\"") ||
                    s.startsWith("'") && s.endsWith("'") ||
                    s.startsWith("(") && s.endsWith(")") ||
                    s.startsWith("[") && s.endsWith("]") ||
                    s.startsWith("{") && s.endsWith("}")) {
                return s.substring(1, s.length()-1);
            }
            else {
                return s;
            }
        }
    }



    public static void main(String[] args) {
        System.out.println(normalize("A ł.ó. d.ź.")); // should be a-lo-dz
        System.out.println(stripQuote(normalize("\"A ł.ó. d.ź.\""))); // should be a-lo-dz
        System.out.println(powerTrim("--a-b--"));
        System.out.println("abc".substring(1));
        System.out.println(isNormalizedLatinString("-ł.ó.-a-b--"));
        System.out.println(isNormalizedLatinString("-ł.ó.-a-岳阳楼-b--"));
        System.out.println(normalizeMySql("Anti-Fascist_Assembly_for_the_People\\'s_Liberation_of_Macedonia"));
    }

    public static String removeTrailingParenthesis(String name) {
        if (!name.endsWith(")")) return name;
        char[] chars = name.toCharArray();
        int i = chars.length - 1;
        while (i >= 0) {
            if (chars[i] == '(') {
                break;
            }
            i--;
        }
        if (i > 0) {
            return name.substring(0, i);
        }
        else {
            return name;
        }
    }

    /**
     * Simplify UTF-8 character sequences
     */
    public static class StringSimplifier {
        public static final char DEFAULT_REPLACE_CHAR = '-';
        public static final String DEFAULT_REPLACE = String.valueOf(DEFAULT_REPLACE_CHAR);
        private static final ImmutableMap<String, String> NONDIACRITICS = ImmutableMap.<String, String>builder()

                //Remove crap strings with no sematics
                .put(".", "")
                .put("\"", "")
                .put("'", "")

                        //Keep relevant characters as seperation
                .put(" ", DEFAULT_REPLACE)
                .put("]", DEFAULT_REPLACE)
                .put("[", DEFAULT_REPLACE)
                .put(")", DEFAULT_REPLACE)
                .put("(", DEFAULT_REPLACE)
                .put("=", DEFAULT_REPLACE)
                .put("!", DEFAULT_REPLACE)
                .put("/", DEFAULT_REPLACE)
                .put("\\", DEFAULT_REPLACE)
                .put("&", DEFAULT_REPLACE)
                .put(",", DEFAULT_REPLACE)
                .put("?", DEFAULT_REPLACE)
                .put("°", DEFAULT_REPLACE) //Remove ?? is diacritic?
                .put("|", DEFAULT_REPLACE)
                .put("<", DEFAULT_REPLACE)
                .put(">", DEFAULT_REPLACE)
                .put(";", DEFAULT_REPLACE)
                .put(":", DEFAULT_REPLACE)
                .put("_", DEFAULT_REPLACE)
                .put("#", DEFAULT_REPLACE)
                .put("~", DEFAULT_REPLACE)
                .put("+", DEFAULT_REPLACE)
                .put("*", DEFAULT_REPLACE)

                        //Replace non-diacritics as their equivalent characters
                .put("\u0141", "l") // BiaLystock
                .put("\u0142", "l") // Bialystock
                .put("ß", "ss")
                .put("æ", "ae")
                .put("ø", "o")
                .put("©", "c")
                .put("\u00D0", "d") // All Ð ð from http://de.wikipedia.org/wiki/%C3%90
                .put("\u00F0", "d")
                .put("\u0110", "d")
                .put("\u0111", "d")
                .put("\u0189", "d")
                .put("\u0256", "d")
                .put("\u00DE", "th") // thorn Þ
                .put("\u00FE", "th") // thorn þ
                .build();


        public static String simplifiedString(String orig) {
            String str = orig;
            if (str == null) {
                return null;
            }
            str = stripDiacritics(str);
            str = stripNonDiacritics(str);
            if (str.length() == 0) {
                // Ugly special case to work around non-existing empty strings
                // in Oracle. Store original crapstring as simplified.
                // It would return an empty string if Oracle could store it.
                return orig;
            }
            return str.toLowerCase();
        }

        private static String stripNonDiacritics(String orig) {
            StringBuffer ret = new StringBuffer();
            String lastchar = null;
            for (int i = 0; i < orig.length(); i++) {
                String source = orig.substring(i, i + 1);
                String replace = NONDIACRITICS.get(source);
                String toReplace = replace == null ? String.valueOf(source) : replace;
                if (DEFAULT_REPLACE.equals(lastchar) && DEFAULT_REPLACE.equals(toReplace)) {
                    toReplace = "";
                } else {
                    lastchar = toReplace;
                }
                ret.append(toReplace);
            }
            if (ret.length() > 0 && DEFAULT_REPLACE_CHAR == ret.charAt(ret.length() - 1)) {
                ret.deleteCharAt(ret.length() - 1);
            }
            return ret.toString();
        }

        /*
        Special regular expression character ranges relevant for simplification -> see http://docstore.mik.ua/orelly/perl/prog3/ch05_04.htm
        InCombiningDiacriticalMarks: special marks that are part of "normal" ä, ö, î etc..
            IsSk: Symbol, Modifier see http://www.fileformat.info/info/unicode/category/Sk/list.htm
            IsLm: Letter, Modifier see http://www.fileformat.info/info/unicode/category/Lm/list.htm
         */
        public static final Pattern DIACRITICS_AND_FRIENDS
                = Pattern.compile("[\\p{InCombiningDiacriticalMarks}\\p{IsLm}\\p{IsSk}]+");


        private static String stripDiacritics(String str) {
            str = Normalizer.normalize(str, Normalizer.Form.NFD);
            str = DIACRITICS_AND_FRIENDS.matcher(str).replaceAll("");
            return str;
        }
    }




}