package org.endeavourhealth.recordviewer.common.models;

public class EpisodeOfCareFull {
    private int id;
    private String code;
    private String name;
    private String dateRegistered;
    private String dateRegisteredEnd;
    private int organizationId;
    private int practitionerId;

    public int getId() {
        return id;
    }

    public EpisodeOfCareFull setId(int id) {
        this.id = id;
        return this;
    }

    public String getCode() {
        return code;
    }

    public EpisodeOfCareFull setCode(String code) {
        this.code = code;
        return this;
    }

    public String getName() {
        return name;
    }

    public EpisodeOfCareFull setName(String name) {
        this.name = name;
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
