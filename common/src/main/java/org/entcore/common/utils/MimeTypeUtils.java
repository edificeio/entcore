package org.entcore.common.utils;

import java.util.*;

public class MimeTypeUtils {

    private static final Set<String> wordExtensions = new HashSet<>();
    private static final Set<String> excelExtensions = new HashSet<>();
    private static final Set<String> pptExtensions = new HashSet<>();
    private static final Map<String, String> fileExtensionMap = new HashMap<>();
    public static final String PDF = "application/pdf";
    public static final String CSV = "text/csv";

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

    public static boolean isWordLike(String contentType) {
        Optional<String> extension = getExtensionForContentType(contentType);
        if (extension.isPresent()) {
            final String ext = extension.get();
            return wordExtensions.contains(ext);
        }
        return false;
    }

    public static boolean isExcelLike(String contentType) {
        Optional<String> extension = getExtensionForContentType(contentType);
        if (extension.isPresent()) {
            final String ext = extension.get();
            return excelExtensions.contains(ext);
        }
        return false;
    }

    public static boolean isPowerpointLike(String contentType) {
        Optional<String> extension = getExtensionForContentType(contentType);
        if (extension.isPresent()) {
            final String ext = extension.get();
            return pptExtensions.contains(ext);
        }
        return false;
    }
}