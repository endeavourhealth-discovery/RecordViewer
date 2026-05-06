package org.endeavourhealth.recordviewer.common.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AiReviewResponse {

    private String summary;

    @JsonProperty("emergency_flags")
    private EmergencyFlags emergencyFlags;

    private java.util.List<String> recommendations;

    @JsonProperty("chat_response")
    private String chatResponse;

    @JsonProperty("thinking_content")
    private String thinkingContent;

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public EmergencyFlags getEmergencyFlags() {
        return emergencyFlags;
    }

    public void setEmergencyFlags(EmergencyFlags emergencyFlags) {
        this.emergencyFlags = emergencyFlags;
    }

    public java.util.List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(java.util.List<String> recommendations) {
        this.recommendations = recommendations;
    }

    public String getChatResponse() {
        return chatResponse;
    }

    public void setChatResponse(String chatResponse) {
        this.chatResponse = chatResponse;
    }

    public String getThinkingContent() {
        return thinkingContent;
    }

    public void setThinkingContent(String thinkingContent) {
        this.thinkingContent = thinkingContent;
    }
}
