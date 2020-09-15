package org.entcore.cas.mapping;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Mapping {
    private final String type;
    private final String casType;
    private final String pattern;
    private final Pattern patternCompiled;

    public static Mapping unknown(String casType, String pattern){
        return new Mapping("unknown", casType, pattern);
    }

    public Mapping(String type, String casType, String pattern) {
        this.type = type;
        this.casType = casType;
        this.pattern = pattern;
        this.patternCompiled = Pattern.compile(pattern == null? "" :pattern, Pattern.CASE_INSENSITIVE);
    }

    public String getType() {
        return type;
    }

    public String getCasType() {
        return casType;
    }

    public String getPattern() {
        return pattern;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mapping that = (Mapping) o;
        return Objects.equals(casType, that.casType) &&
                Objects.equals(pattern, that.pattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(casType, pattern);
    }

    public boolean matches(final String serviceUri){
        final Matcher matcher = patternCompiled.matcher(serviceUri);
        return matcher.matches();
    }

    public String pattern(){
        return this.patternCompiled.pattern();
    }

}
