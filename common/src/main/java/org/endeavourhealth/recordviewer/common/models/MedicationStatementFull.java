package org.endeavourhealth.recordviewer.common.models;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.text.SimpleDateFormat;
import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MedicationStatementFull {
    private String id;
    private String nhsNumber;
    private String date;
    private String dose;
    private String name;
    private double quantityValue;
    private String quantityUnit;
    private String note;
    private String startDate;
    private int status;
    private int coding;

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

    public String getName() {
        return name;
    }

    public MedicationStatementFull setName(String name) {
        this.name = name;
        return this;
    }

    public double getQuantityValue() {
        return quantityValue;
    }

    public MedicationStatementFull setQuantityValue(double quantityValue) {
        this.quantityValue = quantityValue;
        return this;
    }

    public String getQuantityUnit() {
        return quantityUnit;
    }

    public MedicationStatementFull setQuantityUnit(String quantityUnit) {
        this.quantityUnit = quantityUnit;
        return this;
    }

    public String getNote() {
        return note;
    }

    public MedicationStatementFull setNote(String note) {
        this.note = note;
        return this;
    }

    public String getStartDate() {
        return startDate;
    }

    public MedicationStatementFull setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public int getStatus() {
        return status;
    }

    public MedicationStatementFull setStatus(int status) {
        this.status = status;
        return this;
    }

    public int getCoding() {
        return coding;
    }

    public MedicationStatementFull setCoding(int coding) {
        this.coding = coding;
        return this;
    }
}
