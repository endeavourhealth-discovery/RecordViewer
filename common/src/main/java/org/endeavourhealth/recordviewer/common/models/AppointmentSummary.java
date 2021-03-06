package org.endeavourhealth.recordviewer.common.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppointmentSummary {
    private static final Logger LOG = LoggerFactory.getLogger(AppointmentSummary.class);
    private String type;
    private String location;
    private String date;
    private int duration;
    private int delay;
    private String status;
    private String orgName;
    private String code;

    public String getType() {
        return type;
    }
    public AppointmentSummary setType(String type) {
        this.type = type;
        return this;
    }

    public String getLocation() {
        return location;
    }
    public AppointmentSummary setLocation(String location) {
        this.location = location;
        return this;
    }

    public String getDate() {
        return date;
    }
    public AppointmentSummary setDate(Date date) {
        try {
            String pattern = "yyyy-MM-dd";;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

            this.date = simpleDateFormat.format(date);
        }
        catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return this;
    }

    public int getDuration() {
        return duration;
    }
    public AppointmentSummary setDuration(int duration) {
        this.duration = duration;
        return this;
    }

    public int getDelay() {
        return delay;
    }
    public AppointmentSummary setDelay(int delay) {
        this.delay = delay;
        return this;
    }

    public String getStatus() {
        return status;
    }
    public AppointmentSummary setStatus(String status) {
        this.status = status;
        return this;
    }

    public String getOrgName() {
        return orgName;
    }

    public AppointmentSummary setOrgName(String orgName) {
        this.orgName = orgName;
        return this;
    }

    public String getCode() {
        return code;
    }

    public AppointmentSummary setCode(String code) {
        this.code = code;
        return this;
    }

}
