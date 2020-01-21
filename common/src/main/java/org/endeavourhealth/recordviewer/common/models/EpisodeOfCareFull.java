package org.endeavourhealth.recordviewer.common.models;

public class EpisodeOfCareFull {
    private String code;
    private String description;
    private String dateRegistered;
    private String dateRegisteredEnd;
    private int organizationId;
    private int practitionerId;

    public String getCode() {
        return code;
    }

    public EpisodeOfCareFull setCode(String code) {
        this.code = code;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public EpisodeOfCareFull setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getDateRegistered() {
        return dateRegistered;
    }

    public EpisodeOfCareFull setDateRegistered(String dateRegistered) {
        this.dateRegistered = dateRegistered;
        return this;
    }

    public String getDateRegisteredEnd() {
        return dateRegisteredEnd;
    }

    public EpisodeOfCareFull setDateRegisteredEnd(String dateRegisteredEnd) {
        this.dateRegisteredEnd = dateRegisteredEnd;
        return this;
    }

    public int getOrganizationId() {
        return organizationId;
    }

    public EpisodeOfCareFull setOrganizationId(int organizationId) {
        this.organizationId = organizationId;
        return this;
    }

    public int getPractitionerId() {
        return practitionerId;
    }

    public EpisodeOfCareFull setPractitionerId(int practitionerId) {
        this.practitionerId = practitionerId;
        return this;
    }


}
