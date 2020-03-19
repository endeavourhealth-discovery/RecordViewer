package resources;

import ca.uhn.fhir.model.api.Tag;
import org.endeavourhealth.recordviewer.common.constants.ResourceConstants;
import org.endeavourhealth.recordviewer.common.models.EncounterFull;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.Period;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class Encounter {

    /*
    org.hl7.fhir.dstu3.model.Encounter encounter = new org.hl7.fhir.dstu3.model.Encounter();

     */

    public static org.hl7.fhir.dstu3.model.Encounter getEncounterResource(EncounterFull encounterFull) throws ParseException {
        org.hl7.fhir.dstu3.model.Encounter encounter = new org.hl7.fhir.dstu3.model.Encounter();
        encounter.setId(UUID.randomUUID().toString());

        if (encounterFull.getStatus().equalsIgnoreCase("active")) {
            encounter.setStatus(org.hl7.fhir.dstu3.model.Encounter.EncounterStatus.INPROGRESS);
        } else {
            encounter.setStatus(org.hl7.fhir.dstu3.model.Encounter.EncounterStatus.FINISHED);
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date clinicalEffDt = formatter.parse(encounterFull.getDate());

        if(clinicalEffDt != null) {
            encounter.setPeriod(new Period().setStart(clinicalEffDt));
        }

        encounter.addIdentifier()
                .setValue(String.valueOf(encounterFull.getEncounterid()))
                .setSystem(ResourceConstants.SYSTEM_ID);

        encounter.setClass_(new org.hl7.fhir.dstu3.model.Coding("http://terminology.hl7.org/CodeSystem/v3-ActCode", encounterFull.getCode(), encounterFull.getName()));

        return encounter;
    }
}
