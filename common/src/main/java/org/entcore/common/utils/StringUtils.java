/*
 * Copyright © WebServices pour l'Éducation, 2014
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package org.entcore.common.utils;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by dbreyton on 30/03/2016.
 */
public final class StringUtils {
    /** Empty string constant. */
    public static final String EMPTY_STRING = "";

    /**
     * The Constructor.
     */
    private StringUtils() {
    }

    /**
     * Removes space characters (space, tab, etc ...)
     * beginning and end of string. If the result is an empty string, or if the
     * input string is null, null
     * is returned.
     * 
     * @param str the str
     * 
     * @return the string
     */
    public static String trimToNull(String str) {
        if (str == null) {
            return null;
        }
        final String newStr = str.trim();

        return (newStr.length() == 0) ? null : newStr;
    }

    /**
     * Test whether a string is null, empty or contains only spaces.
     * 
     * @param str the str
     * 
     * @return true, if is empty
     */
    public static boolean isEmpty(String str) {
        return trimToNull(str) == null;
    }

    /**
     * Removes space characters (space, tab, etc ...)
     * beginning and end of string. If the result is an empty string, or if the
     * input string is null, an empty string is returned.
     * 
     * @param str the str
     * 
     * @return the string
     */
    public static String trimToBlank(String str) {
        final String newStr = trimToNull(str);
        return (newStr != null) ? newStr : EMPTY_STRING;
    }

    /**
     * Concatenate two strings. A null string is replaced by an empty string.
     * 
     * @param a the a
     * @param b the b
     * 
     * @return the string
     */
    public static String concat(String a, String b) {
        return trimToBlank(a) + trimToBlank(b);
    }

    /**
     * Cutting a character string following a delimiter.
     * 
     * @param regexDelim the regex delim
     * @param str the str
     * 
     * @return the list<string>
     */
    public static List<String> split(String str, String regexDelim) {
        return Arrays.asList(str.split(regexDelim));
    }
    
    /**
     * Cutting a character string following a delimiter, with a limited use of delimiter "limitUseRegex" times.
     * 
     * @param limitUseRegex limit use regex
     * @param regexDelim the regex delim
     * @param str the str
     * 
     * @return the list<string>
     */
    public static List<String> split(String str, String regexDelim, int limitUseRegex) {
        return Arrays.asList(str.split(regexDelim, limitUseRegex));
    }
    
    /**
     * Returns the concatenation of strings from a list using the
     * delimiter indicated.
     * Example: <code>join ([ "A", "B", "CD"], "/") -> "A / B / CD"</code>
     * 
     * @param list the list of strings to concatenate.
     * @param delim the delimiter to be inserted between each value.
     * 
     * @return the string containing the concatenated values.
     */
    public static String join(List<String> list, String delim) {
        boolean isDelimiter = false;
        final StringBuilder builder = new StringBuilder();
        if (list != null) {
            for (String val : list) {
                if (isDelimiter) {
                    builder.append(delim);
                }
                builder.append(val);
                isDelimiter = true;
            }
        }
        return builder.toString();
    }

    /**
     * If a search words separated by a delimiter string contains a
     * given element. the delimiter is given to using an expression
     * regular.
     * Example : <code>contains ("banana, apple, orange", "[] *, [;] [] *", "apple") = true</code>
     *
     * @param str the str
     * @param regexDelim the regex delim
     * @param searchString the search string
     * 
     * @return true, if contains
     */
    public static boolean contains(String str, String regexDelim, String searchString) {
        final List<String> tokens = split(str, regexDelim);
        if ((tokens != null) && (tokens.size() > 0)) {
            return tokens.contains(searchString);
        }
        return false;
    }

    /**
     * Remove any spaces in the string provided.
     * 
     * @param str the string
     * 
     * @return the string
     */
    public static String stripSpaces(String str) {
        return (str != null) ? str.replaceAll("\\s", "") : null;
    }
    
    /**
     * Remove all extra spaces in the string provided, representing a sentence.
     * 
     * @param str the string
     * 
     * @return a sentence or an empty string (if the string is empty or null).
     */
    public static String stripSpacesSentence(String str) {
        return (str != null) ? trimToBlank(str.replaceAll("\\s+", " ")) : "";
    }

    /**
     * Remove all accents in the string provided.
     *
     * @param str the string
     *
     * @return a string without accents.
     */
    public static String stripAccentsToLowerCase(String str) {
        return stripAccents(str).toLowerCase();
    }

    /**
     * Remove all accents in the string provided.
     *
     * @param str the string
     *
     * @return a string without accents.
     */
    public static String stripAccents(String str) {
        String strUnaccent = Normalizer.normalize(str, Normalizer.Form.NFD);
        strUnaccent = strUnaccent.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return strUnaccent;
    }

    /**
     * Remove all html tag in the string provided.
     *
     * @param str the string
     *
     * @return a string without html tag.
     */
    public static String stripHtmlTag(String str) {
        return trimToBlank(str).replaceAll("<[^>]*>", "");
    }

    /**
     * Indicates if the string 1 starts with string2.
     * 
     * @param string1 String to test
     * @param string2 String to be contained
     * 
     * @return true, If the string1 starts with string2
     */
    public static Boolean startWith(String string1, String string2) {
        return (string1 != null) ? string1.startsWith(string2) : false;
    }
    
    /**
     * Indicates whether the string1 ends with string2.
     * 
     * @param string1 String to test
     * @param string2 String to be contained
     * 
     * @return true, If the string1 ends with string2
     */
    public static Boolean endWith(String string1, String string2) {
        return (string1 != null) ? string1.endsWith(string2) : false;
    }

    /**
     * returns true if the searched string is found.
     * 
     * @param find : the string to find.
     * @param seeks : the string where seeks.
     * @param separator : the separator.
     * 
     * @return : true, if found
     */
    public static boolean isIn(final String find, String seeks, String separator){
        if(trimToNull(find)!= null && trimToNull(seeks)!= null){
            final StringTokenizer st = new StringTokenizer(seeks, separator);
            while (st.hasMoreTokens()) {
                if(st.nextToken().equals(find)){
                    return true;
                }
            }
        }

        return false;
    }
}
