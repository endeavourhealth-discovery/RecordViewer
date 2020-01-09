package org.endeavourhealth.recordviewer.common.models;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgnizationSummary {

    private String odscode;
    private String name;
    private String postcode;

    public String getOdscode() {
        return odscode;
    }

    public OrgnizationSummary setOdscode(String odscode) {
        this.odscode = odscode;
        return this;
    }

    public String getName() {
        return name;
    }

    public OrgnizationSummary setName(String name) {
        this.name = name;
        return this;
    }

    public String getPostcode() {
        return postcode;
    }

    public OrgnizationSummary setPostcode(String postcode) {
        this.postcode = postcode;
        return this;
    }



}
