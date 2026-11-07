package org.endeavourhealth.recordviewer.common.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Array;
import java.text.SimpleDateFormat;
import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PatientSummary {
    private static final Logger LOG = LoggerFactory.getLogger(PatientSummary.class);
    private String id;
    private String name;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MMM-yyyy")
    private Date dob;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MMM-yyyy")
    private Date dod;
    private String nhsNumber;
    private String address;
    private String gender;
    private String age;
    private String usual_gp;
    private String organisation;
    private String registration;
    private String start_date;
    private String mobile;
    private String personId;
    private String patientAdditionalTextValue;
    private String patientAdditionalConcept;

    public String getId() {
        return id;
    }

    public PatientSummary setId(String id) {
        this.id = id;
        return this;
    }

    public String getPersonId(){
        return this.personId;
    }
    public PatientSummary setPersonId(String pid){
        this.personId = pid;
        return this;
    }

    public String getPatientAdditionalTextValue(){
        return this.patientAdditionalTextValue;
    }
    public PatientSummary setPatientAdditionalTextValue(String value){
        this.patientAdditionalTextValue = value;
        return this;
    }

    public String getPatientAdditionalConcept(){
        return this.patientAdditionalConcept;
    }
    public PatientSummary setPatientAdditionalConcept(String concept){
        this.patientAdditionalConcept = concept;
        return this;
    }

    public String getName() {
        return name;
    }

    public PatientSummary setName(String name) {
        this.name = name;
        return this;
    }

    public Date getDob() {
        return dob;
    }

    public PatientSummary setDob(Date dob) {
        this.dob = dob;
        return this;
    }

    public Date getDod() {
        return dod;
    }

    public PatientSummary setDod(Date dod) {
        this.dod = dod;
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

    public String getUsual_gp() {
        return usual_gp;
    }

    public PatientSummary setUsual_gp(String usual_gp) {
        this.usual_gp = usual_gp;
        return this;
    }

    public String getOrganisation() {
        return organisation;
    }

    public PatientSummary setOrganisation(String organisation) {
        this.organisation = organisation;
        return this;
    }

    public String getRegistration() { return registration; }

    public PatientSummary setRegistration(String registration) {
        this.registration = registration;
        return this;
    }

    public String getStart_date() { return start_date; }

    public PatientSummary setStart_date(String start_date) {
        this.start_date = start_date;
        return this;
    }

    public String getMobile() { return mobile; }

    public PatientSummary setMobile(String mobile) {
        this.mobile = mobile;
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
