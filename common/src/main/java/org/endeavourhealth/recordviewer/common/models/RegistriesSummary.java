package org.endeavourhealth.recordviewer.common.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegistriesSummary {
    private static final Logger LOG = LoggerFactory.getLogger(RegistriesSummary.class);

    private String registry;
    private String indicator;
    private String entryDate;
    private String entryValue;
    private String achieved;
    private String notes;

    public String getEntryDate() { return entryDate; }
    public RegistriesSummary setEntryDate(Date entryDate) {
        try {
            String pattern = "yyyy-MM-dd";;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

            this.entryDate = simpleDateFormat.format(entryDate);
        }
        catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return this;
    }

    public String getRegistry() {
        return registry;
    }
    public RegistriesSummary setRegistry(String registry) {
        this.registry = registry;
        return this;
    }

    public String getIndicator() {
        return indicator;
    }
    public RegistriesSummary setIndicator(String indicator) {
        this.indicator = indicator;
        return this;
    }

    public String getEntryValue() {
        return entryValue;
    }
    public RegistriesSummary setEntryValue(String entryValue) {
        this.entryValue = entryValue;
        return this;
    }

    public String getAchieved() {
        return achieved;
    }
    public RegistriesSummary setAchieved(String achieved) {
        this.achieved = achieved;
        return this;
    }

    public String getNotes() {
        return notes;
    }
    public RegistriesSummary setNotes(String notes) {
        this.notes = notes;
        return this;
    }


}
