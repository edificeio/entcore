/*
 * Copyright © "Open Digital Education", 2014
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

 */

package org.entcore.common.utils;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.lang.Integer;
import java.lang.Long;
import java.lang.IllegalArgumentException;

/**
 * Created by dbreyton on 30/03/2016.
 */
public final class StringUtils {
    /** Empty string constant. */
    public static final String EMPTY_STRING = "";
    public static final String UUID_REGEX = "[0-9a-f]{8}-[0-9a-f]{4}-[4][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";

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
     * Generates a string containing all permutations of the given string list (maximum 16 strings)
     * Example: <code>createAllPermutations ([ "A", "B", "C"]) -> "ABCACBACAB"</code>
     *
     * @param list the list of strings to permute.
     *
     * @return the string containing all permutations.
     */
    public static String createAllPermutations(List<String> list)
    {
        int size = list.size();
        if(size > 16) // The limit is 16 because 16*4 == 64 == number of bits in a long. 20! > MAX_LONG so that would be the hard limit anyway
            throw new IllegalArgumentException("The number of string to permute exceeds 16");
        else if(size == 0)
            return "";

        int nbPermuts = size; // the number of permutations is size!
        for(int i = size; i-- > 1;)
            nbPermuts *= i;

        StringBuilder str = new StringBuilder();
        List<Long> permuts = new ArrayList<Long>(nbPermuts);
        Map<Long, List<Integer>> prefixIndex = new HashMap<Long, List<Integer>>();
        Set<Integer> usedPermuts = new HashSet<Integer>();

        // Recursively generate all permutations
        StringUtils.generateAllPermutations(permuts, prefixIndex, size, 0, 0);

        Long current = permuts.get(0);
        usedPermuts.add(new Integer(0));

        int elementsToRead = size;
        for(int i = nbPermuts; i-- > 0;)
        {
            long ptr = 0xF << ((elementsToRead - 1) * 4);
            long val = current.longValue();

            // Only append the necessary strings
            for(int e = elementsToRead; e-- > 0;)
            {
                int strToAppend = (int)((val & ptr) >>> (e * 4));
                ptr >>>= 4;
                str.append(list.get(strToAppend));
            }
            elementsToRead = 0;

            Long next = current;
            do
            {
                ++elementsToRead;
                // Find the best next permutation (aka current suffix matches the biggest next prefix possible) (e.g. aBCD should match BCDa)
                long prefixToClear = ((long)(size - elementsToRead));
                long prefixLengthToLookFor = prefixToClear << (15 * 4);
                val &= (~((long)0) ^ (0xF << (prefixToClear * 4)));

                List<Integer> nextCandidates = prefixIndex.get(new Long(prefixLengthToLookFor | val));

                if(nextCandidates == null)
                {
                    System.err.println("\n\nImpossible error in createAllPermutations: No candidates\n\n");
                    continue;
                }
                else
                {
                    // Find a permutation that hasn't already been used
                    for(int candidateIx = nextCandidates.size(); candidateIx-- > 0;)
                    {
                        Integer candidate = nextCandidates.get(candidateIx);
                        if(usedPermuts.contains(candidate) == false)
                        {
                            usedPermuts.add(candidate);
                            next = permuts.get(candidate.intValue());
                            break;
                        }
                    }
                }
            }
            // If we can't find a n-sized prefix, try a (n-1)-sized prefix on the next iteration
            while(next == current && val != 0 && usedPermuts.size() != nbPermuts);

            current = next;
        }

        if(usedPermuts.size() != nbPermuts)
          System.err.println("\n\nImpossible error in createAllPermutations: Used " + usedPermuts.size() + " out of " + nbPermuts + "\n\n");

        return str.toString();
    }

    // nbElements must be at most 16. Call with permut = 0, taken = 0
    private static void generateAllPermutations(List<Long> allPermuts, Map<Long, List<Integer>> prefixIndex, int nbElements,
        long permut, long taken)
    {
        boolean isFinished = true;
        for(int i = nbElements; i-- > 0;)
        {
            if((taken & (1 << i)) != 0)
                continue;
            else
            {
                isFinished = false;
                generateAllPermutations(allPermuts, prefixIndex, nbElements, (permut << 4) | i, taken | (1 << i));
            }
        }

        if(isFinished == true)
        {
            Long finalPermut = new Long(permut);
            Integer permutIx = new Integer(allPermuts.size());
            allPermuts.add(finalPermut);

            for(long i = nbElements; i-- > 1;)
            {
                permut >>>= 4;

                // The prefixed length makes it possible to distinguish 0x01 and 0x1
                long prefixLength = (i << (15 * 4));
                Long prefix = new Long(prefixLength | permut);

                List<Integer> prefixes = prefixIndex.get(prefix);
                if(prefixes == null)
                {
                    prefixes = new ArrayList<Integer>(nbElements); // Real capacity should be (nbElements - 1)! but this is good enough
                    prefixIndex.put(prefix, prefixes);
                }
                prefixes.add(permutIx);
            }
        }
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

	public static String substringBeforeLast(final String str, final String separator) {
		if (isEmpty(str) || isEmpty(separator)) {
			return str;
		}
		final int pos = str.lastIndexOf(separator);
		if (pos == -1) {
			return str;
		}
		return str.substring(0, pos);
	}

	public static String substringAfterLast(final String str, final String separator) {
		if (isEmpty(str)) {
			return str;
		}
		if (isEmpty(separator)) {
			return EMPTY_STRING;
		}
		final int pos = str.lastIndexOf(separator);
		if (pos == -1 || pos == str.length() - separator.length()) {
			return EMPTY_STRING;
		}
		return str.substring(pos + separator.length());
	}

	public static String substringAfter(final String str, final String separator) {
		if (isEmpty(str)) {
			return str;
		}
		if (separator == null) {
			return EMPTY_STRING;
		}
		final int pos = str.indexOf(separator);
		if (pos == -1) {
			return EMPTY_STRING;
		}
		return str.substring(pos + separator.length());
	}

    public static String padRight(String s, int n, char c) {
        return String.format("%1$-" + n + "s", s).replace(' ', c);
    }

    public static String padLeft(String s, int n, char c) {
        return String.format("%1$" + n + "s", s).replace(' ', c);
    }

    public static int ordinalIndexOf(String str, String substr, int n) {
        if (str == null || substr == null || n < 1) {
            return -1;
        }
        int pos = str.indexOf(substr);
        while (--n > 0 && pos != -1)
            pos = str.indexOf(substr, pos + 1);
        return pos;
    }

    public static String replaceForbiddenCharacters(String s) {
        return s.replaceAll("(\\\\|\\/|\\*|\\\"|\\<|\\>|:|\\?|\\|)","_");
    }

    public final static Comparator<String> versionComparator = (v1, v2) -> {
        try {
            return new Double(Double.parseDouble("."+v1.split("-", 2)[0].replaceAll("\\.", "")))
                    .compareTo(Double.parseDouble("."+v2.split("-", 2)[0].replaceAll("\\.", "")));
        } catch (NumberFormatException nbe) {
            return 0;
        }
    };
}
