package org.endeavourhealth.recordviewer.common.models;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PractitionerResult {

    private String id;
    private String name;
    private String role_code;
    private String role_desc;

    public String getId() {
        return id;
    }

    public PractitionerResult setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public PractitionerResult setName(String name) {
        this.name = name;
        return this;
    }

    public String getRole_code() {
        return role_code;
    }

    public PractitionerResult setRole_code(String role_code) {
        this.role_code = role_code;
        return this;
    }

    public String getRole_desc() {
        return role_desc;
    }

    public PractitionerResult setRole_desc(String role_desc) {
        this.role_desc = role_desc;
        return this;
    }
}
