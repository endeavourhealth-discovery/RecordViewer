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

    @JsonProperty("prompt_tokens")
    private Integer promptTokens;

    @JsonProperty("completion_tokens")
    private Integer completionTokens;

    @JsonProperty("total_tokens")
    private Integer totalTokens;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    @JsonProperty("max_context")
    private Integer maxContext;

    @JsonProperty("identified_patient")
    private PatientSummary identifiedPatient;

    @JsonProperty("action_items")
    private java.util.List<String> actionItems;

    private String urgency;

    @JsonProperty("debug_images")
    private java.util.List<String> debugImages;

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

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Integer getMaxContext() {
        return maxContext;
    }

    public void setMaxContext(Integer maxContext) {
        this.maxContext = maxContext;
    }

    public PatientSummary getIdentifiedPatient() {
        return identifiedPatient;
    }

    public void setIdentifiedPatient(PatientSummary identifiedPatient) {
        this.identifiedPatient = identifiedPatient;
    }

    public java.util.List<String> getActionItems() {
        return actionItems;
    }

    public void setActionItems(java.util.List<String> actionItems) {
        this.actionItems = actionItems;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }

    public java.util.List<String> getDebugImages() {
        return debugImages;
    }

    public void setDebugImages(java.util.List<String> debugImages) {
        this.debugImages = debugImages;
    }
}
