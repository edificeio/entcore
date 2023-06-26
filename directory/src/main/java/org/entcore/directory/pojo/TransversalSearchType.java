package org.entcore.directory.pojo;

public enum TransversalSearchType {
    EMAIL("email"),
    FULL_NAME("fullName"),
    DISPLAY_NAME("displayName"),
    NONE("");
    private final String code;

    TransversalSearchType(final String code) {
        this.code = code;
    }

    public static TransversalSearchType fromCode(final String searchType) {
        if(EMAIL.code.equals(searchType)) {
            return EMAIL;
        }
        if(FULL_NAME.code.equals(searchType)) {
            return FULL_NAME;
        }
        if(DISPLAY_NAME.code.equals(searchType)) {
            return DISPLAY_NAME;
        }
        return NONE;
    }
}
