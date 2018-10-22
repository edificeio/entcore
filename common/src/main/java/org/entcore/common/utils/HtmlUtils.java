package org.entcore.common.utils;

import fr.wseduc.webutils.collections.JsonArray;


import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.entcore.common.utils.StringUtils.*;

public class HtmlUtils {

    private static HashMap<String,String> htmlEntities;
    static {
        htmlEntities = new HashMap<String,String>();
        htmlEntities.put("&lt;","<")    ; htmlEntities.put("&gt;",">");
        htmlEntities.put("&amp;","&")   ; htmlEntities.put("&quot;","\"");
        htmlEntities.put("&agrave;","à"); htmlEntities.put("&Agrave;","À");
        htmlEntities.put("&acirc;","â") ; htmlEntities.put("&auml;","ä");
        htmlEntities.put("&Auml;","Ä")  ; htmlEntities.put("&Acirc;","Â");
        htmlEntities.put("&aring;","å") ; htmlEntities.put("&Aring;","Å");
        htmlEntities.put("&aelig;","æ") ; htmlEntities.put("&AElig;","Æ" );
        htmlEntities.put("&ccedil;","ç"); htmlEntities.put("&Ccedil;","Ç");
        htmlEntities.put("&eacute;","é"); htmlEntities.put("&Eacute;","É" );
        htmlEntities.put("&egrave;","è"); htmlEntities.put("&Egrave;","È");
        htmlEntities.put("&ecirc;","ê") ; htmlEntities.put("&Ecirc;","Ê");
        htmlEntities.put("&euml;","ë")  ; htmlEntities.put("&Euml;","Ë");
        htmlEntities.put("&iuml;","ï")  ; htmlEntities.put("&Iuml;","Ï");
        htmlEntities.put("&ocirc;","ô") ; htmlEntities.put("&Ocirc;","Ô");
        htmlEntities.put("&ouml;","ö")  ; htmlEntities.put("&Ouml;","Ö");
        htmlEntities.put("&oslash;","ø") ; htmlEntities.put("&Oslash;","Ø");
        htmlEntities.put("&szlig;","ß") ; htmlEntities.put("&ugrave;","ù");
        htmlEntities.put("&Ugrave;","Ù"); htmlEntities.put("&ucirc;","û");
        htmlEntities.put("&Ucirc;","Û") ; htmlEntities.put("&uuml;","ü");
        htmlEntities.put("&Uuml;","Ü")  ; htmlEntities.put("&nbsp;"," ");
        htmlEntities.put("&copy;","\u00a9");
        htmlEntities.put("&reg;","\u00ae");
        htmlEntities.put("&euro;","\u20a0");
    }
    private static Set<String> htmlEntitiesKeys = htmlEntities.keySet();
    private static final Pattern imageSrcPattern = Pattern.compile("<img src=\"([^\"]+)");
    private static final Pattern plainTextPattern = Pattern.compile(">([^</]+)");
    private static final Pattern htmlEntityPattern = Pattern.compile("&.*?;");

    public static JsonArray getAllImagesSrc(String htmlContent){
        JsonArray images = new JsonArray();
        Matcher matcher = imageSrcPattern.matcher(htmlContent);
        while (matcher.find()) {
            images.add(matcher.group(1));
        }
        return images;
    }

    public static JsonArray getAllImagesSrc(String htmlContent, int limit){
        JsonArray images = new JsonArray();
        Matcher matcher = imageSrcPattern.matcher(htmlContent);
        int nbfind = 0;
        while (nbfind < limit && matcher.find()) {
            images.add(matcher.group(1));
            nbfind++;
        }

        return images;
    }

    public static String extractPlainText(String htmlContent){
        StringBuilder sb = new StringBuilder();
        Matcher matcher = plainTextPattern.matcher(formatSpaces(htmlContent));
        while (matcher.find()){
            sb.append(matcher.group(1));
        }
        return sb.toString();
    }

    public static String extractFormatText(String htmlContent){
        return formatSpaces(unescapeHtmlEntities(htmlContent.replaceAll("<(/div|/p|/li|br|)>","\n").replaceAll("<.*?>","")));
    }

    public static String extractFormatText(String htmlContent, int limitLines){
        String text = extractFormatText(htmlContent);
        int pos = ordinalIndexOf(text, "\n", limitLines);
        if(pos != -1)
            return text.substring(0, pos);
        return text;
    }

    public static String extractFormatText(String htmlContent, int limitLines, int limitChar){
        String text = extractFormatText(htmlContent, limitLines);
        if(limitChar > text.length())
            return text;
        return text.substring(0, limitChar);
    }

    public static String unescapeHtmlEntities(String htmlContent){
        if(isEmpty(htmlContent))
            return htmlContent;
        for(String key : htmlEntitiesKeys){
            htmlContent = htmlContent.replaceAll(key, htmlEntities.get(key));
        }
        return htmlContent;
    }

    public static String formatSpaces(String content){
        return trimToBlank(content).replaceAll("\\u200b","").replaceAll("[ ,\\t]{2,}"," ").replaceAll("[\\s]{2,}","\n");
    }

}
