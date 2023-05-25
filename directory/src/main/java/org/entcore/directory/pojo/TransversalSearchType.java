package org.entcore.directory.pojo;

public enum TransversalSearchType {
    EMAIL("email"),
    NAME("displayName"),
    NONE("");
    private final String code;

    TransversalSearchType(final String code) {
        this.code = code;
    }

    public static TransversalSearchType fromCode(final String searchType) {
        if(EMAIL.code.equals(searchType)) {
            return EMAIL;
        }
        if(NAME.code.equals(searchType)) {
            return NAME;
        }
        return NONE;
    }
}
