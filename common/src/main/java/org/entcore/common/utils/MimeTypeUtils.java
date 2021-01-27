package org.entcore.common.utils;

import java.util.*;

public class MimeTypeUtils {

    private static final Set<String> wordExtensions = new HashSet<>();
    private static final Set<String> excelExtensions = new HashSet<>();
    private static final Set<String> pptExtensions = new HashSet<>();
    private static final Map<String, String> fileExtensionMap = new HashMap<>();
    public static final String PDF = "application/pdf";
    public static final String CSV = "text/csv";
    public static final String OCTET_STREAM = "application/octet-stream";

    static {
        //word extensions
        wordExtensions.add("doc");
        wordExtensions.add("dot");
        wordExtensions.add("docx");
        wordExtensions.add("dotx");
        wordExtensions.add("docm");
        wordExtensions.add("dotm");
        wordExtensions.add("odt");
        wordExtensions.add("ott");
        wordExtensions.add("oth");
        wordExtensions.add("odm");
        //excel extensions
        excelExtensions.add("xls");
        excelExtensions.add("xlt");
        excelExtensions.add("xla");
        excelExtensions.add("xlsx");
        excelExtensions.add("xltx");
        excelExtensions.add("xlsm");
        excelExtensions.add("xltm");
        excelExtensions.add("xlam");
        excelExtensions.add("xlsb");
        excelExtensions.add("ods");
        excelExtensions.add("ots");
        //ppt extensions
        pptExtensions.add("ppt");
        pptExtensions.add("pot");
        pptExtensions.add("pps");
        pptExtensions.add("ppa");
        pptExtensions.add("pptx");
        pptExtensions.add("potx");
        pptExtensions.add("ppsx");
        pptExtensions.add("ppam");
        pptExtensions.add("pptm");
        pptExtensions.add("potm");
        pptExtensions.add("ppsm");
        pptExtensions.add("odp");
        pptExtensions.add("otp");
        // MS Office
        fileExtensionMap.put("pdf", "application/pdf");
        fileExtensionMap.put("doc", "application/msword");
        fileExtensionMap.put("dot", "application/msword");
        fileExtensionMap.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        fileExtensionMap.put("dotx", "application/vnd.openxmlformats-officedocument.wordprocessingml.template");
        fileExtensionMap.put("docm", "application/vnd.ms-word.document.macroEnabled.12");
        fileExtensionMap.put("dotm", "application/vnd.ms-word.template.macroEnabled.12");
        fileExtensionMap.put("xls", "application/vnd.ms-excel");
        fileExtensionMap.put("xlt", "application/vnd.ms-excel");
        fileExtensionMap.put("xla", "application/vnd.ms-excel");
        fileExtensionMap.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        fileExtensionMap.put("xltx", "application/vnd.openxmlformats-officedocument.spreadsheetml.template");
        fileExtensionMap.put("xlsm", "application/vnd.ms-excel.sheet.macroEnabled.12");
        fileExtensionMap.put("xltm", "application/vnd.ms-excel.template.macroEnabled.12");
        fileExtensionMap.put("xlam", "application/vnd.ms-excel.addin.macroEnabled.12");
        fileExtensionMap.put("xlsb", "application/vnd.ms-excel.sheet.binary.macroEnabled.12");
        fileExtensionMap.put("ppt", "application/vnd.ms-powerpoint");
        fileExtensionMap.put("pot", "application/vnd.ms-powerpoint");
        fileExtensionMap.put("pps", "application/vnd.ms-powerpoint");
        fileExtensionMap.put("ppa", "application/vnd.ms-powerpoint");
        fileExtensionMap.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        fileExtensionMap.put("potx", "application/vnd.openxmlformats-officedocument.presentationml.template");
        fileExtensionMap.put("ppsx", "application/vnd.openxmlformats-officedocument.presentationml.slideshow");
        fileExtensionMap.put("ppam", "application/vnd.ms-powerpoint.addin.macroEnabled.12");
        fileExtensionMap.put("pptm", "application/vnd.ms-powerpoint.presentation.macroEnabled.12");
        fileExtensionMap.put("potm", "application/vnd.ms-powerpoint.presentation.macroEnabled.12");
        fileExtensionMap.put("ppsm", "application/vnd.ms-powerpoint.slideshow.macroEnabled.12");
        // Open Office
        fileExtensionMap.put("odt", "application/vnd.oasis.opendocument.text");
        fileExtensionMap.put("ott", "application/vnd.oasis.opendocument.text-template");
        fileExtensionMap.put("oth", "application/vnd.oasis.opendocument.text-web");
        fileExtensionMap.put("odm", "application/vnd.oasis.opendocument.text-master");
        fileExtensionMap.put("odg", "application/vnd.oasis.opendocument.graphics");
        fileExtensionMap.put("otg", "application/vnd.oasis.opendocument.graphics-template");
        fileExtensionMap.put("odp", "application/vnd.oasis.opendocument.presentation");
        fileExtensionMap.put("otp", "application/vnd.oasis.opendocument.presentation-template");
        fileExtensionMap.put("ods", "application/vnd.oasis.opendocument.spreadsheet");
        fileExtensionMap.put("ots", "application/vnd.oasis.opendocument.spreadsheet-template");
        fileExtensionMap.put("odc", "application/vnd.oasis.opendocument.chart");
        fileExtensionMap.put("odf", "application/vnd.oasis.opendocument.formula");
        fileExtensionMap.put("odb", "application/vnd.oasis.opendocument.database");
        fileExtensionMap.put("odi", "application/vnd.oasis.opendocument.image");
        fileExtensionMap.put("oxt", "application/vnd.openofficeorg.extension");
        //image
        fileExtensionMap.put("jpg", "image/jpeg");
        fileExtensionMap.put("gif", "image/gif");
        fileExtensionMap.put("png", "image/png");
        fileExtensionMap.put("swf", "application/x-shockwave-flash");
        fileExtensionMap.put("psd", "image/psd");
        fileExtensionMap.put("bmp", "image/bmp");
        fileExtensionMap.put("tiff", "image/tiff");
        fileExtensionMap.put("jpc", "application/octet-stream");
        fileExtensionMap.put("jp2", "image/jp2");
        fileExtensionMap.put("iff", "image/iff");
        fileExtensionMap.put("webp", "image/webp");
        fileExtensionMap.put("xbm", "image/xbm");
        fileExtensionMap.put("ico", "image/vnd.microsoft.icon");
        fileExtensionMap.put("wbmp", "image/vnd.wap.wbmp");
        fileExtensionMap.put("jpeg", "image/jpeg");
        fileExtensionMap.put("svg", "image/svg+xml");
        fileExtensionMap.put("tif", "image/tiff");
        //audio
        fileExtensionMap.put("aac", "audio/aac");
        fileExtensionMap.put("mid", "audio/midi");
        fileExtensionMap.put("oga", "audio/ogg");
        fileExtensionMap.put("wav", "audio/x-wav");
        fileExtensionMap.put("weba", "audio/webm");
        fileExtensionMap.put("mp3", "video/mp3");
        //video
        fileExtensionMap.put("avi", "video/x-msvideo");
        fileExtensionMap.put("mp4", "video/mp4");
        fileExtensionMap.put("mpeg", "video/mpeg");
        fileExtensionMap.put("ogv", "video/ogg");
        fileExtensionMap.put("webm", "video/webm");
        fileExtensionMap.put("3gp", "video/3gpp");
        fileExtensionMap.put("3g2", "video/3gpp2");
        //text
        fileExtensionMap.put("css", "text/css");
        fileExtensionMap.put("csv", "text/csv");
        fileExtensionMap.put("htm", "text/html");
        fileExtensionMap.put("ics", "text/calendar");
        //font
        fileExtensionMap.put("woff", "font/woff");
        fileExtensionMap.put("woff2", "font/woff2");
        fileExtensionMap.put("otf", "font/otf");
        fileExtensionMap.put("ttf", "font/ttf");
        //all others
        fileExtensionMap.put("abw", "application/x-abiword");
        fileExtensionMap.put("arc", "application/octet-stream");
        fileExtensionMap.put("azw", "application/vnd.amazon.ebook");
        fileExtensionMap.put("bin", "application/octet-stream");
        fileExtensionMap.put("bz", "application/x-bzip");
        fileExtensionMap.put("bz2", "application/x-bzip2");
        fileExtensionMap.put("csh", "application/x-csh");
        fileExtensionMap.put("eot", "application/vnd.ms-fontobject");
        fileExtensionMap.put("epub", "application/epub+zip");
        fileExtensionMap.put("jar", "application/java-archive");
        fileExtensionMap.put("js", "application/javascript");
        fileExtensionMap.put("json", "application/json");
        fileExtensionMap.put("mpkg", "application/vnd.apple.installer+xml");
        fileExtensionMap.put("ogx", "application/ogg");
        fileExtensionMap.put("rar", "application/x-rar-compressed");
        fileExtensionMap.put("rtf", "application/rtf");
        fileExtensionMap.put("sh", "application/x-sh");
        fileExtensionMap.put("tar", "application/x-tar");
        fileExtensionMap.put("ts", "application/typescript");
        fileExtensionMap.put("vsd", "application/vnd.visio");
        fileExtensionMap.put("xhtml", "application/xhtml+xml");
        fileExtensionMap.put("xml", "application/xml");
        fileExtensionMap.put("xul", "application/vnd.mozilla.xul+xml");
        fileExtensionMap.put("zip", "application/zip");
        fileExtensionMap.put("7z", "application/x-7z-compressed");
    }

    public static Optional<String> getContentTypeForExtension(String extension) {
        if (fileExtensionMap.containsKey(extension)) {
            return Optional.ofNullable(fileExtensionMap.get(extension));
        }
        return Optional.empty();
    }

    public static Optional<String> getExtensionForContentType(String contentType) {
        for (Map.Entry<String, String> entry : fileExtensionMap.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(contentType)) {
                return Optional.ofNullable(entry.getKey());
            }
        }
        return Optional.empty();
    }

    private static boolean isUnknownType(String contentType){
        return OCTET_STREAM.equalsIgnoreCase(trim(contentType));
    }

    private static String trim(String extension){
        if(extension!=null){
            return extension.trim();
        }
        return "";
    }

    public static boolean isWordLike(String contentType) {
        Optional<String> extension = getExtensionForContentType(contentType);
        if (extension.isPresent()) {
            final String ext = extension.get();
            return wordExtensions.contains(ext);
        }
        return false;
    }

    public static boolean isWordLike(String contentType, String extension) {
        final boolean isWorkLike = isWordLike(contentType);
        if(isWorkLike) return true;
        return isUnknownType(contentType) && wordExtensions.contains(trim(extension));
    }

    public static boolean isExcelLike(String contentType) {
        Optional<String> extension = getExtensionForContentType(contentType);
        if (extension.isPresent()) {
            final String ext = extension.get();
            return excelExtensions.contains(ext);
        }
        return false;
    }

    public static boolean isExcelLike(String contentType, String extension) {
        final boolean isExcelLike = isExcelLike(contentType);
        if(isExcelLike) return true;
        return isUnknownType(contentType) && excelExtensions.contains(trim(extension));
    }

    public static boolean isPowerpointLike(String contentType) {
        Optional<String> extension = getExtensionForContentType(contentType);
        if (extension.isPresent()) {
            final String ext = extension.get();
            return pptExtensions.contains(ext);
        }
        return false;
    }

    public static boolean isPowerpointLike(String contentType, String extension) {
        final boolean isPowerpointLike = isPowerpointLike(contentType);
        if(isPowerpointLike) return true;
        return isUnknownType(contentType) && pptExtensions.contains(trim(extension));
    }
}