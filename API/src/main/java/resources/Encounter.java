package resources;

import org.endeavourhealth.recordviewer.common.models.EncounterFull;

import java.util.UUID;

public class Encounter {

    /*
    org.hl7.fhir.dstu3.model.Encounter encounter = new org.hl7.fhir.dstu3.model.Encounter();

     */

    public static org.hl7.fhir.dstu3.model.Encounter getEncounterResource(EncounterFull encounterFull) {
        org.hl7.fhir.dstu3.model.Encounter encounter = new org.hl7.fhir.dstu3.model.Encounter();
        encounter.setId(UUID.randomUUID().toString());

      /*  if (encounterFull.getStatus().equalsIgnoreCase("active"))
            encounter.setStatus(org.hl7.fhir.dstu3.model.Encounter.EncounterStatus.INPROGRESS);
        else
            encounter.setStatus(org.hl7.fhir.dstu3.model.Encounter.EncounterStatus.FINISHED);*/

        encounter.setClass_(new org.hl7.fhir.dstu3.model.Coding("http://terminology.hl7.org/CodeSystem/v3-ActCode", encounterFull.getCode(), encounterFull.getName()));

        return encounter;
    }
}
