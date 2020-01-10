package org.endeavourhealth.recordviewer.common.models;

public class OrganizationFull {

    private String odscode;
    private String name;
    private String postcode;

    public String getOdscode() {
        return odscode;
    }

    public OrganizationFull setOdscode(String odscode) {
        this.odscode = odscode;
        return this;
    }

    public String getName() {
        return name;
    }

    public OrganizationFull setName(String name) {
        this.name = name;
        return this;
    }

    public String getPostcode() {
        return postcode;
    }

    public OrganizationFull setPostcode(String postcode) {
        this.postcode = postcode;
        return this;
    }



}
