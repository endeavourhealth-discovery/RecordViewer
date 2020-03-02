package resources;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import org.endeavourhealth.recordviewer.common.constants.ResourceConstants;
import org.endeavourhealth.recordviewer.common.models.ConditionFull;
import org.hl7.fhir.dstu3.model.CodeableConcept;

import java.util.TimeZone;
import java.util.UUID;

public class Condition {

    public static org.hl7.fhir.dstu3.model.Condition getConditionResource(ConditionFull conditionfull)
    {
        org.hl7.fhir.dstu3.model.Condition condition = new org.hl7.fhir.dstu3.model.Condition();
        if(conditionfull.getClinicalStatus().equalsIgnoreCase("active"))
        { condition.setClinicalStatus(org.hl7.fhir.dstu3.model.Condition.ConditionClinicalStatus.ACTIVE); }
        else {
            condition.setClinicalStatus(org.hl7.fhir.dstu3.model.Condition.ConditionClinicalStatus.RESOLVED);
            condition.setOnset(new org.hl7.fhir.dstu3.model.DateTimeType(conditionfull.getDate(), TemporalPrecisionEnum.SECOND, TimeZone.getDefault()));
            condition.getMeta().addProfile("https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-ProblemHeader-Condition-1");
        }
        condition.addIdentifier()
                .setValue(String.valueOf(conditionfull.getId()))
                .setSystem(ResourceConstants.SYSTEM_ID);

        // manifestation or codeable concept?
        CodeableConcept code = new CodeableConcept();
        code.setText(conditionfull.getName());
        code.addCoding()
                .setCode(conditionfull.getCode())
                .setSystem("http://snomed.info/sct")
                .setDisplay(conditionfull.getName());
        condition.setId(UUID.randomUUID().toString());
        condition.setCode(code);

        return condition;
    }
}
