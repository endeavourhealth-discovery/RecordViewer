package org.endeavourhealth.recordviewer.common.models;

import java.util.Date;

public class FamilyMemberHistoryFull {
    private String date;
    private String status;
    private String name;
    private String code;

    public String getDate() {
        return date;
    }

    public FamilyMemberHistoryFull setDate(String date) {
        this.date = date;
        return this;
    }

    public String getStatus() { return status; }

    public FamilyMemberHistoryFull setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getName() {
        return name;
    }

    public FamilyMemberHistoryFull setName(String name) {
        this.name = name;
        return this;
    }

    public String getCode() {
        return code;
    }

    public FamilyMemberHistoryFull setCode(String code) {
        this.code = code;
        return this;
    }

}
