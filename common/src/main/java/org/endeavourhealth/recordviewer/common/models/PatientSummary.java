package org.endeavourhealth.recordviewer.common.models;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.text.SimpleDateFormat;
import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PatientSummary {
    private String id;
    private String name;
    private String dob;
    private String nhsNumber;
    private String address;
    private String gender;
    private String age;

    public String getId() {
        return id;
    }

    public PatientSummary setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public PatientSummary setName(String name) {
        this.name = name;
        return this;
    }

    public String getDob() {
        return dob;
    }

    public PatientSummary setDob(Date dob) {
        try {
            String pattern = "dd-MMM-yyyy";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

            this.dob = simpleDateFormat.format(dob);
        }
        catch (Exception e) {

        }
        return this;
    }

    public String getNhsNumber() {
        return nhsNumber;
    }

    public PatientSummary setNhsNumber(String nhsNumber) {
        this.nhsNumber = nhsNumber;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public PatientSummary setAddress(String address) {
        this.address = address;
        return this;
    }

    public String getGender() {
        return gender;
    }

    public PatientSummary setGender(String gender) {
        this.gender = gender;
        return this;
    }

    public String getAge() {
        return age;
    }

    public PatientSummary setAge(String age) {
        this.age = age;
        return this;
    }

    public String toTitleCase(String input) {
        StringBuilder titleCase = new StringBuilder(input.length());
        boolean nextTitleCase = true;

        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            }

            titleCase.append(c);
        }

        return titleCase.toString();
    }
}
