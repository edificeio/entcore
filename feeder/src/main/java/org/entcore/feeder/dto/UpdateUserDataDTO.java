package org.entcore.feeder.dto;

import java.util.List;

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

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getLoginAlias() { return loginAlias; }
    public void setLoginAlias(String loginAlias) { this.loginAlias = loginAlias; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getHomePhone() { return homePhone; }
    public void setHomePhone(String homePhone) { this.homePhone = homePhone; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getChildrenIds() { return childrenIds; }
    public void setChildrenIds(String childrenIds) { this.childrenIds = childrenIds; }

    public List<String> getPositionIds() { return positionIds; }
    public void setPositionIds(List<String> positionIds) { this.positionIds = positionIds; }
}