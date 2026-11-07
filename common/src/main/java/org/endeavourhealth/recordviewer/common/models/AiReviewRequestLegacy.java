package org.endeavourhealth.recordviewer.common.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class AiReviewRequestLegacy {

    private String nhsNumber;

    @JsonProperty("project_id")
    private String projectID;

    private List<ChatMessage> conversationHistory;

    private String symptoms;

    public String getNhsNumber() {
        return nhsNumber;
    }

    public void setNhsNumber(String nhsNumber) {
        this.nhsNumber = nhsNumber;
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
}
