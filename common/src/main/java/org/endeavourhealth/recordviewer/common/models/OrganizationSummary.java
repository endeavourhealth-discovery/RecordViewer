package org.endeavourhealth.recordviewer.common.models;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrganizationSummary {

    private String odscode;
    private String name;
    private String postcode;

    public String getOdscode() {
        return odscode;
    }

    public OrganizationSummary setOdscode(String odscode) {
        this.odscode = odscode;
        return this;
    }

    public String getName() {
        return name;
    }

    public OrganizationSummary setName(String name) {
        this.name = name;
        return this;
    }

    public String getPostcode() {
        return postcode;
    }

    public OrganizationSummary setPostcode(String postcode) {
        this.postcode = postcode;
        return this;
    }



}
