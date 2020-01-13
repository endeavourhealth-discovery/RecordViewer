package org.endeavourhealth.recordviewer.common.models;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.text.SimpleDateFormat;
import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MedicationStatementFull {
    private String id;
    private String nhsNumber;
    private String name;
    private String code;
    private String valueDateTime;
    private int status;
    private String date;
    private String dose;

    public String getId() {
        return id;
    }

    public MedicationStatementFull setId(String id) {
        this.id = id;
        return this;
    }

    public String getNhsNumber() {
        return nhsNumber;
    }

    public MedicationStatementFull setNhsNumber(String nhsNumber) {
        this.nhsNumber = nhsNumber;
        return this;
    }

    public String getName() {
        return name;
    }

    public MedicationStatementFull setName(String name) {
        this.name = name;
        return this;
    }

    public String getCode() {
        return code;
    }

    public MedicationStatementFull setCode(String code) {
        this.code = code;
        return this;
    }

    public String getValueDateTime() {
        return valueDateTime;
    }

    public MedicationStatementFull setValueDateTime(String valueDateTime) {
        this.valueDateTime = valueDateTime;
        return this;
    }

    public int getStatus() {
        return status;
    }

    public MedicationStatementFull setStatus(int status) {
        this.status = status;
        return this;
    }

    public String getDate() {
        return date;
    }

    public MedicationStatementFull setDate(String date) {
        this.date = date;
        return this;
    }

    public String getDose() {
        return dose;
    }

    public MedicationStatementFull setDose(String dose) {
        this.dose = dose;
        return this;
    }
}
