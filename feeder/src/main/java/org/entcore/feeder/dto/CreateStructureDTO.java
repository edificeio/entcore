package org.entcore.feeder.dto;

import java.util.List;

public class CreateStructureDTO {

    private String name;
    private String externalId;
    private String feederName;
    private String siret;
    private String siren;
    private List<String> joinKey;
    private String uai;
    private String type;
    private String address;
    private String postbox;
    private String zipCode;
    private String city;
    private String phone;
    private String accountable;
    private String email;
    private String website;
    private String contact;
    private String ministry;
    private String contract;
    private List<String> administrativeAttachment;
    private List<String> functionalAttachment;
    private String area;
    private String town;
    private String district;
    private String sector;
    private String rpi;
    private String academy;
    private Boolean hasApp;
    private List<String> groups;
    private Boolean ignoreMFA;
    private Integer transactionId;
    private Boolean commit;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getFeederName() { return feederName; }
    public void setFeederName(String feederName) { this.feederName = feederName; }

    public String getSiret() { return siret; }
    public void setSiret(String siret) { this.siret = siret; }

    public String getSiren() { return siren; }
    public void setSiren(String siren) { this.siren = siren; }

    public List<String> getJoinKey() { return joinKey; }
    public void setJoinKey(List<String> joinKey) { this.joinKey = joinKey; }

    public String getUai() { return uai; }
    public void setUai(String uai) { this.uai = uai; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPostbox() { return postbox; }
    public void setPostbox(String postbox) { this.postbox = postbox; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAccountable() { return accountable; }
    public void setAccountable(String accountable) { this.accountable = accountable; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getMinistry() { return ministry; }
    public void setMinistry(String ministry) { this.ministry = ministry; }

    public String getContract() { return contract; }
    public void setContract(String contract) { this.contract = contract; }

    public List<String> getAdministrativeAttachment() { return administrativeAttachment; }
    public void setAdministrativeAttachment(List<String> administrativeAttachment) { this.administrativeAttachment = administrativeAttachment; }

    public List<String> getFunctionalAttachment() { return functionalAttachment; }
    public void setFunctionalAttachment(List<String> functionalAttachment) { this.functionalAttachment = functionalAttachment; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getTown() { return town; }
    public void setTown(String town) { this.town = town; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public String getRpi() { return rpi; }
    public void setRpi(String rpi) { this.rpi = rpi; }

    public String getAcademy() { return academy; }
    public void setAcademy(String academy) { this.academy = academy; }

    public Boolean getHasApp() { return hasApp; }
    public void setHasApp(Boolean hasApp) { this.hasApp = hasApp; }

    public List<String> getGroups() { return groups; }
    public void setGroups(List<String> groups) { this.groups = groups; }

    public Boolean getIgnoreMFA() { return ignoreMFA; }
    public void setIgnoreMFA(Boolean ignoreMFA) { this.ignoreMFA = ignoreMFA; }

    public Integer getTransactionId() { return transactionId; }
    public void setTransactionId(Integer transactionId) { this.transactionId = transactionId; }

    public Boolean getCommit() { return commit; }
    public void setCommit(Boolean commit) { this.commit = commit; }
}