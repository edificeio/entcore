package org.entcore.cas.mapping;

import org.entcore.common.utils.StringUtils;

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
    private final Set<Pattern> extraPatterns = new HashSet<>();

    public static Mapping unknown(String casType, String pattern){
        return new Mapping("unknown", casType, pattern);
    }

    public Mapping(String type, String casType, String pattern) {
        this.type = type;
        this.casType = casType;
        this.pattern = StringUtils.isEmpty(pattern)? "" : pattern;
        this.patternCompiled = Pattern.compile(pattern == null? "" :pattern, Pattern.CASE_INSENSITIVE);
    }

    public Mapping addExtraPatter(String pattern){
        final Pattern p = Pattern.compile(pattern == null? "" :pattern, Pattern.CASE_INSENSITIVE);
        this.extraPatterns.add(p);
        return this;
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
        Mapping mapping = (Mapping) o;
        return allStructures == mapping.allStructures &&
                Objects.equals(type, mapping.type) &&
                Objects.equals(casType, mapping.casType) &&
                Objects.equals(pattern, mapping.pattern) &&
                Objects.equals(patternCompiled, mapping.patternCompiled) &&
                Objects.equals(structureIds, mapping.structureIds) &&
                Objects.equals(extraPatterns, mapping.extraPatterns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, casType, pattern, patternCompiled, allStructures, structureIds, extraPatterns);
    }

    public boolean matches(final Collection<String> structureIds, final String serviceUri){
        if(!allStructures){
            boolean ok = false;
            for(final String s : structureIds){
                if(this.structureIds.contains(s)){
                   ok = true;
                   break;
                }
            }
            if(!ok){
                return false;
            }
        }
        if(patternCompiled.matcher(serviceUri).matches()){
            return true;
        }
        for(final Pattern extra : extraPatterns){
            if(extra.matcher(serviceUri).matches()){
                return true;
            }
        }
        return false;
    }

    public Set<Pattern> getExtraPatterns() {
        return extraPatterns;
    }

    public String pattern(){
        return this.patternCompiled.pattern();
    }

    public Mapping copyWith(Set<String> structureIds, boolean allStructures){
        final Mapping maping = new Mapping(this.type, this.casType, this.pattern).setAllStructures(allStructures).setStructureIds(structureIds);
        maping.extraPatterns.addAll(this.extraPatterns);
        return maping;
    }

    @Override
    public String toString() {
        return "Mapping{" +
                "type='" + type + '\'' +
                ", casType='" + casType + '\'' +
                ", pattern='" + pattern + '\'' +
                ", patternCompiled=" + patternCompiled +
                ", allStructures=" + allStructures +
                ", structureIds=" + structureIds +
                ", extraPatterns=" + extraPatterns +
                '}';
    }
}
