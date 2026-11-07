package org.endeavourhealth.recordviewer.common.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class AiReviewRequest {

    private String personID;

    @JsonProperty("project_id")
    private String projectID;

    private List<ChatMessage> conversationHistory;

    private String symptoms;

    private String purpose;

    public String getPersonID() {
        return personID;
    }

    public void setPersonID(String personID) {
        this.personID = personID;
    }

    public String getProjectID() {
        return projectID;
    }

    public void setProjectID(String projectID) {
        this.projectID = projectID;
    }

    public List<ChatMessage> getConversationHistory() {
        return conversationHistory;
    }

    public void setConversationHistory(List<ChatMessage> conversationHistory) {
        this.conversationHistory = conversationHistory;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}
