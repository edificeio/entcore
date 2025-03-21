package org.entcore.common.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.entcore.common.utils.StringUtils.*;

public class HtmlUtils {

    private static HashMap<String, String> htmlEntities;

    static {
        htmlEntities = new HashMap<String, String>();
        htmlEntities.put("&lt;", "<");
        htmlEntities.put("&gt;", ">");
        htmlEntities.put("&amp;", "&");
        htmlEntities.put("&quot;", "\"");
        htmlEntities.put("&agrave;", "à");
        htmlEntities.put("&Agrave;", "À");
        htmlEntities.put("&acirc;", "â");
        htmlEntities.put("&auml;", "ä");
        htmlEntities.put("&Auml;", "Ä");
        htmlEntities.put("&Acirc;", "Â");
        htmlEntities.put("&aring;", "å");
        htmlEntities.put("&Aring;", "Å");
        htmlEntities.put("&aelig;", "æ");
        htmlEntities.put("&AElig;", "Æ");
        htmlEntities.put("&ccedil;", "ç");
        htmlEntities.put("&Ccedil;", "Ç");
        htmlEntities.put("&eacute;", "é");
        htmlEntities.put("&Eacute;", "É");
        htmlEntities.put("&egrave;", "è");
        htmlEntities.put("&Egrave;", "È");
        htmlEntities.put("&ecirc;", "ê");
        htmlEntities.put("&Ecirc;", "Ê");
        htmlEntities.put("&euml;", "ë");
        htmlEntities.put("&Euml;", "Ë");
        htmlEntities.put("&iuml;", "ï");
        htmlEntities.put("&Iuml;", "Ï");
        htmlEntities.put("&ocirc;", "ô");
        htmlEntities.put("&Ocirc;", "Ô");
        htmlEntities.put("&ouml;", "ö");
        htmlEntities.put("&Ouml;", "Ö");
        htmlEntities.put("&oslash;", "ø");
        htmlEntities.put("&Oslash;", "Ø");
        htmlEntities.put("&szlig;", "ß");
        htmlEntities.put("&ugrave;", "ù");
        htmlEntities.put("&Ugrave;", "Ù");
        htmlEntities.put("&ucirc;", "û");
        htmlEntities.put("&Ucirc;", "Û");
        htmlEntities.put("&uuml;", "ü");
        htmlEntities.put("&Uuml;", "Ü");
        htmlEntities.put("&nbsp;", " ");
        htmlEntities.put("&copy;", "\u00a9");
        htmlEntities.put("&reg;", "\u00ae");
        htmlEntities.put("&euro;", "\u20a0");
    }

    private static Set<String> htmlEntitiesKeys = htmlEntities.keySet();
    private static final Pattern imageSrcPattern = Pattern.compile("<img(\\s+[^>]*)?\\ssrc=\"([^\"]+)\"");
    private static final Pattern audioSrcPattern = Pattern.compile("<audio(\\s+[^>]*)?\\ssrc=\"([^\"]+)\"");
    private static final Pattern videoTagPattern = Pattern.compile("<video(\\s+[^>]*)?>");
    private static final Pattern videoFieldsPattern = Pattern.compile("(data-)?([^= ]+)=\"([^\"]+)\"");
    private static final Pattern iframeSrcPattern = Pattern.compile("<iframe(\\s+[^>]*)?\\ssrc=\"([^\"]+)\"");
    private static final Pattern attachmentsPattern = Pattern.compile("<div(\\s+[^>]*)?\\sclass=\"attachments\"(\\s+[^>]*)?>\\s*((<a(\\s+[^>]*)?>[\\s\\S]*?<\\/a>\\s*)+)<\\/div>");
    private static final Pattern attachmentLinkPattern = Pattern.compile("<a(\\s+[^>]*?)href=\"([^\"]+?)\"(\\s*[^>]*?)><div(\\s+[^>]*?)class=\"download\"><\\/div>([^<]+?)<\\/a>");
    private static final Pattern plainTextPattern = Pattern.compile(">([^</]+)");
    private static final Pattern htmlEntityPattern = Pattern.compile("&.*?;");
    private static final Pattern unsafeAttributePattern = Pattern.compile("(ng-[a-zA-Z0-9-]+\\s*=\\s*\"[^\"]*\")|"
            + "(on[a-zA-Z]+\\s*=\\s*\"[^\"]*\")|"
            + "(href\\s*=\\s*\"javascript:[^\"]*\")");

    public static JsonArray getAllImagesSrc(String htmlContent) {
        JsonArray images = new JsonArray();
        Matcher matcher = imageSrcPattern.matcher(htmlContent);
        while (matcher.find()) {
            images.add(matcher.group(2));
        }
        return images;
    }

    public static JsonArray getAllImagesSrc(String htmlContent, int limit) {
        JsonArray images = new JsonArray();
        Matcher matcher = imageSrcPattern.matcher(htmlContent);
        int nbfind = 0;
        while (nbfind < limit && matcher.find()) {
            images.add(matcher.group(2));
            nbfind++;
        }

        return images;
    }

    public static String extractPlainText(String htmlContent) {
        StringBuilder sb = new StringBuilder();
        Matcher matcher = plainTextPattern.matcher(formatSpaces(htmlContent));
        while (matcher.find()) {
            sb.append(matcher.group(1));
        }
        return sb.toString().replaceAll(attachmentsPattern.pattern(), "");
    }

    public static String extractFormatText(String htmlContent) {
        String textWithoutattachments = htmlContent.replaceAll(attachmentsPattern.pattern(), "");
        String onlyText = textWithoutattachments.replaceAll("<(/div|/p|/li|br|)>", "\n").replaceAll("<.*?>", "");
        String unescaped = unescapeHtmlEntities(onlyText);
        return formatSpaces(unescaped);
    }

    public static String extractFormatText(String htmlContent, int limitLines) {
        String text = extractFormatText(htmlContent);
        int pos = ordinalIndexOf(text, "\n", limitLines);
        if (pos != -1)
            return text.substring(0, pos);
        return text;
    }

    public static String extractFormatText(String htmlContent, int limitLines, int limitChar) {
        String text = extractFormatText(htmlContent, limitLines);
        if (limitChar > text.length())
            return text;
        return text.substring(0, limitChar);
    }

    public static String unescapeHtmlEntities(String htmlContent) {
        if (isEmpty(htmlContent))
            return htmlContent;
        for (String key : htmlEntitiesKeys) {
            htmlContent = htmlContent.replaceAll(key, htmlEntities.get(key));
        }
        return htmlContent;
    }

    public static String formatSpaces(String content) {
        return trimToBlank(content).replaceAll("\\u200b", "").replaceAll("[ ,\\t]{2,}", " ").replaceAll("[\\s]{2,}", "\n");
    }

    public static JsonArray extractMedias(String htmlContent) {
        return extractMedias(htmlContent, 0);
    }

    public static JsonArray extractMedias(String htmlContent, int limitEach) {
        TreeMap<Integer, JsonObject> medias = new TreeMap();
        Matcher matcher, subMatcher;
        // 1. Images
        int nbFound = 0;
        matcher = imageSrcPattern.matcher(htmlContent);
        while ((limitEach == 0 || nbFound < limitEach) && matcher.find()) {
            medias.put(matcher.start(2), new JsonObject()
                    .put("type", "image")
                    .put("src", matcher.group(2))
            );
            nbFound++;
        }
        // 2. Sounds
        nbFound = 0;
        matcher = audioSrcPattern.matcher(htmlContent);
        while ((limitEach == 0 || nbFound < limitEach) && matcher.find()) {
            medias.put(matcher.start(2), new JsonObject()
                    .put("type", "audio")
                    .put("src", matcher.group(2))
            );
            nbFound++;
        }
        // 3. Videos
        nbFound = 0;
        matcher = videoTagPattern.matcher(htmlContent);
        while ((limitEach == 0 || nbFound < limitEach) && matcher.find()) {
            String videoAttrs = matcher.group(1);
            subMatcher = videoFieldsPattern.matcher(videoAttrs);
            JsonObject videoJson = new JsonObject().put("type", "video");
            while (subMatcher.find()) {
                videoJson.put(subMatcher.group(2), subMatcher.group(3));
            }
            medias.put(matcher.start(1), videoJson);
            nbFound++;
        }
        // 4. Iframes
        nbFound = 0;
        matcher = iframeSrcPattern.matcher(htmlContent);
        while ((limitEach == 0 || nbFound < limitEach) && matcher.find()) {
            medias.put(matcher.start(2), new JsonObject()
                    .put("type", "iframe")
                    .put("src", matcher.group(2))
            );
            nbFound++;
        }
        // 5. Attachments
        nbFound = 0;
        matcher = attachmentsPattern.matcher(htmlContent);
        while ((limitEach == 0 || nbFound < limitEach) && matcher.find()) {
            int start = matcher.start(3);
            String attachmentBlockContent = matcher.group(3);
            subMatcher = attachmentLinkPattern.matcher(attachmentBlockContent);
            while ((limitEach == 0 || nbFound < limitEach) && subMatcher.find()) {
                medias.put(subMatcher.start(2) + start, new JsonObject()
                        .put("type", "attachment")
                        .put("src", subMatcher.group(2))
                        .put("name", subMatcher.group(5))
                );
                nbFound++;
            }
        }
        // 6. Compute Json Array in order
        return new JsonArray(new ArrayList(medias.values()));
    }

    public static String removeUnsafeAttributes(String inputHtml) {
        final Matcher matcher = unsafeAttributePattern.matcher(inputHtml);
        final String cleanHtml = matcher.replaceAll("");
        return cleanHtml;
    }

}
