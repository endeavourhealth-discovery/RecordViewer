package org.endeavourhealth.recordviewer.common.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class MedicationSummary {
    private static final Logger LOG = LoggerFactory.getLogger(MedicationSummary.class);
    private String date;
    private String dose;
    private String name;
    private String quantity;

    public String getDate() {
        return date;
    }

    public MedicationSummary setDate(Date date) {
        try {
            String pattern = "dd-MMM-yyyy";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

            this.date = simpleDateFormat.format(date);
        }
        catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return this;
    }

    public String getDose() {
        return dose;
    }

    public MedicationSummary setDose(String dose) {
        this.dose = dose;
        return this;
    }

    public String getName() {
        return name;
    }

    public MedicationSummary setName(String name) {
        this.name = name.replaceAll("Product containing precisely ","");
        this.name = this.name.replaceAll("Product containing ","");
        this.name = toTitleCase(this.name);
        return this;
    }

    public String getQuantity() {
        return quantity;
    }

    public MedicationSummary setQuantity(String quantity) {
        this.quantity = quantity;
        return this;
    }

    public String toTitleCase(String input) {
        StringBuilder titleCase = new StringBuilder(input.length());
        boolean nextTitleCase = true;

        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            }

            titleCase.append(c);
        }

        return titleCase.toString();
    }
}
