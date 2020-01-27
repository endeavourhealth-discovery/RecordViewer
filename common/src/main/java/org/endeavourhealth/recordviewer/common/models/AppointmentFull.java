package org.endeavourhealth.recordviewer.common.models;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppointmentFull {
    private int id;
    private int scheduleId;
    private int orgId;
    private int practitionerId;
    private int actualDuration;
    private String startDate;
    private int plannedDuration;
    private String type;

    public int getId() { return id; }

    public AppointmentFull setId(int id) {
        this.id = id;
        return this;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public AppointmentFull setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
        return this;
    }

    public int getOrgId() {
        return orgId;
    }

    public AppointmentFull setOrgId(int orgId) {
        this.orgId = orgId;
        return this;
    }

    public int getPractitionerId() {
        return practitionerId;
    }

    public AppointmentFull setPractitionerId(int practitionerId) {
        this.practitionerId = practitionerId;
        return this;
    }

    public int getActualDuration() {
        return actualDuration;
    }

    public AppointmentFull setActualDuration(int actualDuration) {
        this.actualDuration = actualDuration;
        return this;
    }

    public String getStartDate() {
        return startDate;
    }

    public AppointmentFull setStartDate(String startDate) {
        this.startDate = startDate;
        return this;
    }

    public int getPlannedDuration() {
        return plannedDuration;
    }

    public AppointmentFull setPlannedDuration(int plannedDuration) {
        this.plannedDuration = plannedDuration;
        return this;
    }

    public String getType() {
        return type;
    }

    public AppointmentFull setType(String type) {
        this.type = type;
        return this;
    }

}
