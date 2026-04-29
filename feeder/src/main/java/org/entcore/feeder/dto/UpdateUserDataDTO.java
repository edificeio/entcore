package org.entcore.feeder.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@DataObject
@JsonGen
public class UpdateUserDataDTO {

    private String firstName;
    private String lastName;
    private String displayName;
    private String birthDate;
    private String address;
    private String zipCode;
    private String city;
    private String loginAlias;
    private String email;
    private String homePhone;
    private String mobile;
    private String childrenIds;
    private List<String> positionIds;

    public UpdateUserDataDTO() {}

    public UpdateUserDataDTO(JsonObject json) {
        UpdateUserDataDTOConverter.fromJson(json, this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        UpdateUserDataDTOConverter.toJson(this, json);
        return json;
    }

    public String getFirstName() { return firstName; }
    public UpdateUserDataDTO setFirstName(String firstName) { this.firstName = firstName; return this; }

    public String getLastName() { return lastName; }
    public UpdateUserDataDTO setLastName(String lastName) { this.lastName = lastName; return this; }

    public String getDisplayName() { return displayName; }
    public UpdateUserDataDTO setDisplayName(String displayName) { this.displayName = displayName; return this; }

    public String getBirthDate() { return birthDate; }
    public UpdateUserDataDTO setBirthDate(String birthDate) { this.birthDate = birthDate; return this; }

    public String getAddress() { return address; }
    public UpdateUserDataDTO setAddress(String address) { this.address = address; return this; }

    public String getZipCode() { return zipCode; }
    public UpdateUserDataDTO setZipCode(String zipCode) { this.zipCode = zipCode; return this; }

    public String getCity() { return city; }
    public UpdateUserDataDTO setCity(String city) { this.city = city; return this; }

    public String getLoginAlias() { return loginAlias; }
    public UpdateUserDataDTO setLoginAlias(String loginAlias) { this.loginAlias = loginAlias; return this; }

    public String getEmail() { return email; }
    public UpdateUserDataDTO setEmail(String email) { this.email = email; return this; }

    public String getHomePhone() { return homePhone; }
    public UpdateUserDataDTO setHomePhone(String homePhone) { this.homePhone = homePhone; return this; }

    public String getMobile() { return mobile; }
    public UpdateUserDataDTO setMobile(String mobile) { this.mobile = mobile; return this; }

    public String getChildrenIds() { return childrenIds; }
    public UpdateUserDataDTO setChildrenIds(String childrenIds) { this.childrenIds = childrenIds; return this; }

    public List<String> getPositionIds() { return positionIds; }
    public UpdateUserDataDTO setPositionIds(List<String> positionIds) { this.positionIds = positionIds; return this; }
}