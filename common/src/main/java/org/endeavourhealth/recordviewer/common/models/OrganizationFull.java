package org.endeavourhealth.recordviewer.common.models;

public class OrganizationFull {

    private String odsCode;
    private String name;
    private String postCode;

    public String getOdsCode() {
        return odsCode;
    }

    public OrganizationFull setOdsCode(String odsCode) {
        this.odsCode = odsCode;
        return this;
    }

    public String getName() {
        return name;
    }

    public OrganizationFull setName(String name) {
        this.name = name;
        return this;
    }

    public String getPostCode() {
        return postCode;
    }

    public OrganizationFull setPostCode(String postCode) {
        this.postCode = postCode;
        return this;
    }



}
