package org.endeavourhealth.recordviewer.common.models;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

public class EncounterTreeSummary {
    private static final Logger LOG = LoggerFactory.getLogger(EncountersSummary.class);

    private String encounterId, date, episodeStartDate, episodeEndDate;
    private String orgName, practitioner, conceptName, conceptId;

    public String getEncounterId() {
        return encounterId;
    }
    public EncounterTreeSummary setEncounterId(String encounterId) {
        this.encounterId = encounterId;
        return this;
    }

    public String getConceptName() {
        return conceptName;
    }
    public EncounterTreeSummary setConceptName(String conceptName) {
        this.conceptName = conceptName;
        return this;
    }
    public String getConceptId() {
        return conceptId;
    }
    public EncounterTreeSummary setConceptId(String conceptId) {
        this.conceptId = conceptId;
        return this;
    }
    public String getPractitioner() {
        return practitioner;
    }
    public EncounterTreeSummary setPractitioner(String practitioner) {
        this.practitioner = practitioner;
        return this;
    }

    public String getEncounterDate() { return date; }
    public EncounterTreeSummary setEncounterDate(Date date) {
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

    public String getEpisodeStartDate() { return episodeStartDate; }
    public EncounterTreeSummary setEpisodeStartDate(Date date) {
        try {
            String pattern = "yyyy-MM-dd";;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

            this.episodeStartDate = simpleDateFormat.format(date);
        }
        catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return this;
    }

    public String getEpisodeEndDate() { return episodeEndDate; }
    public EncounterTreeSummary setEpisodeEndDate(Date date) {
        try {
            String pattern = "yyyy-MM-dd";;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

            this.episodeEndDate = simpleDateFormat.format(date);
        }
        catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return this;
    }

    public String getOrgName() {
        return orgName;
    }

    public EncounterTreeSummary setOrgName(String orgName) {
        this.orgName = orgName;
        return this;
    }
}
