package org.endeavourhealth.recordviewer.common.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class EncountersSummary {
    private static final Logger LOG = LoggerFactory.getLogger(EncountersSummary.class);

    private String id, episode_id;
    private String date, end_date;
    private String type_code, type_desc;
    private String type;
    private String location;
    private String practitioner;
    private String orgName;
    private String code;

    public String getDate() { return date; }
    public EncountersSummary setDate(Date date) {
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
    public String getEndDate() { return end_date; }
    public EncountersSummary setEndDate(Date date) {
        try {
            String pattern = "yyyy-MM-dd";;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

            this.end_date = simpleDateFormat.format(date);
        }
        catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return this;
    }

    public String getId() {
        return id;
    }
    public EncountersSummary setId(String id) {
        this.id = id;
        return this;
    }

    public String getEpisodeId() {
        return episode_id;
    }
    public EncountersSummary setEpisodeId(String episode_id) {
        this.episode_id = episode_id;
        return this;
    }
    public String getType() {
        return type;
    }
    public EncountersSummary setType(String type) {
        this.type = type;
        return this;
    }

    public String getLocation() {
        return location;
    }
    public EncountersSummary setLocation(String location) {
        this.location = location;
        return this;
    }

    public String getPractitioner() {
        return practitioner;
    }
    public EncountersSummary setPractitioner(String practitioner) {
        this.practitioner = practitioner;
        return this;
    }

    public String getOrgName() {
        return orgName;
    }

    public EncountersSummary setOrgName(String orgName) {
        this.orgName = orgName;
        return this;
    }

    public String getCode() {
        return code;
    }

    public EncountersSummary setCode(String code) {
        this.code = code;
        return this;
    }

    public EncountersSummary setTypeDesc(String type_desc) {
        this.type_desc = type_desc;
        return this;
    }
    public String getTypeDesc() {
        return type_desc;
    }

    public EncountersSummary setTypeCode(String type_code) {
        this.type_code = type_code;
        return this;
    }
    public String getTypeCode() {
        return type_code;
    }

}
