package org.entcore.cas.mapping;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Mapping {
    private String type;
    private final String casType;
    private final String pattern;
    private final Pattern patternCompiled;
    private boolean allStructures = false;
    private final Set<String> structureIds = new HashSet<>();

    public static Mapping unknown(String casType, String pattern){
        return new Mapping("unknown", casType, pattern);
    }

    public Mapping(String type, String casType, String pattern) {
        this.type = type;
        this.casType = casType;
        this.pattern = pattern;
        this.patternCompiled = Pattern.compile(pattern == null? "" :pattern, Pattern.CASE_INSENSITIVE);
    }

    public Mapping setType(String type) {
        this.type = type;
        return this;
    }

    public boolean isAllStructures() {
        return allStructures;
    }

    public Mapping setAllStructures(boolean allStructures) {
        this.allStructures = allStructures;
        return this;
    }

    public Set<String> getStructureIds() {
        return structureIds;
    }

    public Pattern getPatternCompiled() {
        return patternCompiled;
    }

    public Mapping setStructureIds(Set<String> structureIds) {
        this.structureIds.addAll(structureIds);
        return this;
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

    public boolean matches(final Collection<String> structureIds, final String serviceUri){
        if(!allStructures){
            boolean ok = false;
            for(final String s : structureIds){
                if(this.structureIds.contains(s)){
                   ok = true;
                }
            }
            if(!ok){
                return false;
            }
        }
        final Matcher matcher = patternCompiled.matcher(serviceUri);
        return matcher.matches();
    }

    public String pattern(){
        return this.patternCompiled.pattern();
    }

    public Mapping copyWith(Set<String> structureIds, boolean allStructures){
        return new Mapping(this.type, this.casType, this.pattern).setAllStructures(allStructures).setStructureIds(structureIds);
    }
}
