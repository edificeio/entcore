package org.entcore.broker.api.dto.directory.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.entcore.broker.api.dto.directory.clazz.ClassDTOStructure;
import org.entcore.broker.api.dto.directory.structure.SchoolDTOStructure;
import java.util.List;

/*
    * This DTO is used to represent user information
    * it used in GetStructureUserResponse to represent 
    * the response of the directory.structure.users.by.id nats subject
    * It is intended to be a duplicate of the others UserProfileDTO as some informations
    * aren't return in the directory.structure.users.by.id subject 
    * and to avoid confusion between the different subjects responses
*/
public class UserProfileDTOStructure {
    private final String id;
    private final String type;
    private final String code;
    private final String login;
    private final String firstName;
    private final String lastName;
    private final String displayName;
    private final String source;
    private final String deleteDate;
    private final String disappearanceDate;
    private final Boolean blocked;
    private final String creationDate;
    private final List<String> removedFromStructures;
    private final Object aafFunctions;
    private final List<ClassDTOStructure> classes;
    private final List<String> functionalGroups;
    private final List<String> manualGroups;
    private final List<FunctionDTOStructure> functions;
    private final List<String> duplicates;
    private final List<SchoolDTOStructure> structures;
    private final List<Object> userPositions;

    @JsonCreator
    public UserProfileDTOStructure(
            @JsonProperty("id") String id,
            @JsonProperty("type") String type,
            @JsonProperty("code") String code,
            @JsonProperty("login") String login,
            @JsonProperty("firstName") String firstName,
            @JsonProperty("lastName") String lastName,
            @JsonProperty("displayName") String displayName,
            @JsonProperty("source") String source,
            @JsonProperty("deleteDate") String deleteDate,
            @JsonProperty("disappearanceDate") String disappearanceDate,
            @JsonProperty("blocked") Boolean blocked,
            @JsonProperty("creationDate") String creationDate,
            @JsonProperty("removedFromStructures") List<String> removedFromStructures,
            @JsonProperty("aafFunctions") Object aafFunctions,
            @JsonProperty("classes") List<ClassDTOStructure> classes,
            @JsonProperty("functionalGroups") List<String> functionalGroups,
            @JsonProperty("manualGroups") List<String> manualGroups,
            @JsonProperty("functions") List<FunctionDTOStructure> functions,
            @JsonProperty("duplicates") List<String> duplicates,
            @JsonProperty("structures") List<SchoolDTOStructure> structures,
            @JsonProperty("userPositions") List<Object> userPositions) {
        this.id = id;
        this.type = type;
        this.code = code;
        this.login = login;
        this.firstName = firstName;
        this.lastName = lastName;
        this.displayName = displayName;
        this.source = source;
        this.deleteDate = deleteDate;
        this.disappearanceDate = disappearanceDate;
        this.blocked = blocked;
        this.creationDate = creationDate;
        this.removedFromStructures = removedFromStructures;
        this.aafFunctions = aafFunctions;
        this.classes = classes;
        this.functionalGroups = functionalGroups;
        this.manualGroups = manualGroups;
        this.functions = functions;
        this.duplicates = duplicates;
        this.structures = structures;
        this.userPositions = userPositions;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getCode() {
        return code;
    }

    public String getLogin() {
        return login;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSource() {
        return source;
    }

    public String getDeleteDate() {
        return deleteDate;
    }

    public String getDisappearanceDate() {
        return disappearanceDate;
    }

    public Boolean getBlocked() {
        return blocked;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public List<String> getRemovedFromStructures() {
        return removedFromStructures;
    }

    public Object getAafFunctions() {
        return aafFunctions;
    }

    public List<ClassDTOStructure> getClasses() {
        return classes;
    }

    public List<String> getFunctionalGroups() {
        return functionalGroups;
    }

    public List<String> getManualGroups() {
        return manualGroups;
    }

    public List<FunctionDTOStructure> getFunctions() {
        return functions;
    }

    public List<String> getDuplicates() {
        return duplicates;
    }

    public List<SchoolDTOStructure> getStructures() {
        return structures;
    }

    public List<Object> getUserPositions() {
        return userPositions;
    }
}