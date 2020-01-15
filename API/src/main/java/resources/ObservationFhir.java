package resources;

import org.apache.commons.lang3.StringUtils;
import org.endeavourhealth.recordviewer.common.constants.ResourceConstants;
import org.endeavourhealth.recordviewer.common.models.ObservationFull;
import org.hl7.fhir.dstu3.model.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.endeavourhealth.recordviewer.common.constants.ResourceConstants.*;

public class ObservationFhir {
    private ObservationFull observationFull;

    public ObservationFhir(ObservationFull observationResult) {
        this.observationFull = observationResult;
    }

    public org.hl7.fhir.dstu3.model.Observation getObservationResource() {
        org.hl7.fhir.dstu3.model.Observation observation = new org.hl7.fhir.dstu3.model.Observation();

        observation.setStatus(org.hl7.fhir.dstu3.model.Observation.ObservationStatus.FINAL);
        observation.setIssued(new Date()); //observation.clinical_effective_date
        observation.setEffective(getEffectiveDateTime(observationFull.getDate())); //observation.clinical_effective_date
        observation.getMeta().addProfile(ResourceConstants.OBSERVATION_PROFILE);

        UUID uuid = UUID.randomUUID();
        observation.addIdentifier()
                .setSystem(OBSERVATION_IDENTIFIER).setValue(uuid.toString());

        List<Extension> extensionList = new ArrayList<>();
        Extension extension = new Extension();
        extension.setUrl(OBSERVATION_DESCRIPTION);
        extension.setValue(new StringType(observationFull.getName()));
        extension.addChild(VALUE_STRING);
        extensionList.add(extension);

        Extension extension1 = new Extension();
        Quantity typeQuantity = (Quantity) extension.addChild(VALUE_QUANTITY);
        if(StringUtils.isNotEmpty(observationFull.getResultValue())) {
            typeQuantity.setValue(Long.parseLong(observationFull.getResultValue()));
        }
        typeQuantity.setSystem(OBSERVATION_QUANTITY_VALUE);
        typeQuantity.setUnit(observationFull.getResultValueUnits());
        extensionList.add(extension1);

        CodeableConcept codeConcept = addCodeableConcept(observationFull.getCode(), observationFull.getDescription(), ResourceConstants.OBSERVATION_SYSTEM,
                "");
        codeConcept.setExtension(extensionList);
        observation.setCode(codeConcept).addExtension(extension);


        return observation;
    }

    private Period getEffectiveDateTime(String date) {
        Period period = new Period();
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            period.setStart(format.parse(date));
        } catch (Exception e) {
        }
        return period;
    }


    private CodeableConcept addCodeableConcept(String codeValue, String displayValue, String systemValue, String idValue) {
        CodeableConcept code = new CodeableConcept();
        code.addCoding()
                .setCode(codeValue)
                .setDisplay(displayValue)
                .setSystem(systemValue)
                .setId(idValue);

        return code;
    }
}
