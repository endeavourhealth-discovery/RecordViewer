package org.endeavourhealth.recordviewer.common.models;

import java.util.Date;

public class ProcedureFull {
    private Date date;
    private String status;
    private String name;
    private String code;

    public Date getDate() {
        return date;
    }

    public ProcedureFull setDate(Date date) {
        this.date = date;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public ProcedureFull setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getName() {
        return name;
    }

    public ProcedureFull setName(String name) {
        this.name = name;
        return this;
    }

    public String getCode() {
        return code;
    }

    public ProcedureFull setCode(String code) {
        this.code = code;
        return this;
    }
}
