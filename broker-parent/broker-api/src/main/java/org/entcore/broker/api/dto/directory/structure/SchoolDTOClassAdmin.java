package org.entcore.broker.api.dto.directory.structure;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.entcore.broker.api.dto.directory.clazz.ClassDTOClassAdmin;
import java.util.List;

/*
    * This DTO is used to represent school information for class-admin
    * it used in GetClassAdminResponseDTO to represent 
    * the response of the directory.class.admin.by.id nats subject
    * It is intended to be a duplicate of the others SchoolDTO as some informations
    * aren't return in the directory.class.admin.by.id subject 
    * and to avoid confusion between the different subjects responses
*/
public class SchoolDTOClassAdmin {
    private final List<ClassDTOClassAdmin> classes;
    private final String name;
    private final String id;
    private final String source;
    private final String address;
    private final String city;
    private final String zipCode;
    private final String academy;
    private final String uai;

    @JsonCreator
    public SchoolDTOClassAdmin(
            @JsonProperty("classes") List<ClassDTOClassAdmin> classes,
            @JsonProperty("name") String name,
            @JsonProperty("id") String id,
            @JsonProperty("source") String source,
            @JsonProperty("address") String address,
            @JsonProperty("city") String city,
            @JsonProperty("zipCode") String zipCode,
            @JsonProperty("academy") String academy,
            @JsonProperty("uai") String uai) {
        this.classes = classes;
        this.name = name;
        this.id = id;
        this.source = source;
        this.address = address;
        this.city = city;
        this.zipCode = zipCode;
        this.academy = academy;
        this.uai = uai;
    }

    public List<ClassDTOClassAdmin> getClasses() {
        return classes;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getAcademy() {
        return academy;
    }

    public String getUai() {
        return uai;
    }
}