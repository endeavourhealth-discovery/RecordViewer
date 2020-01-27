package org.endeavourhealth.recordviewer.common.models;

public class LocationFull {

    private String code;
    private String name;
    private String desc;
    private String postCode;

    public String getCode() {
        return code;
    }

    public LocationFull setCode(String code) {
        this.code = code;
        return this;
    }

    public String getName() {
        return name;
    }

    public LocationFull setName(String name) {
        this.name = name;
        return this;
    }

    public String getDesc() {
        return desc;
    }

    public LocationFull setDesc(String desc) {
        this.desc = desc;
        return this;
    }

    public String getPostCode() {
        return postCode;
    }

    public LocationFull setPostCode(String postCode) {
        this.postCode = postCode;
        return this;
    }
}