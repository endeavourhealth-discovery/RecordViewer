package resources;

import org.endeavourhealth.recordviewer.common.constants.ResourceConstants;
import org.endeavourhealth.recordviewer.common.models.ObservationFull;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static org.endeavourhealth.recordviewer.common.constants.ResourceConstants.*;

public class Observation {
    private static final Logger LOG = LoggerFactory.getLogger(Observation.class);
    private ObservationFull observationFull;

    public Observation(ObservationFull observationFull) {
        this.observationFull = observationFull;
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

       // List<Extension> extensionList = new ArrayList<>();
        Extension extension = new Extension();
        extension.setUrl(OBSERVATION_DESCRIPTION);
        StringType stringType = (StringType) extension.addChild(VALUE_STRING);
        stringType.setValue(observationFull.getName());

        Quantity typeQuantity = (Quantity) observation.addChild(VALUE_QUANTITY);
        typeQuantity.setValue(observationFull.getResultValue());

        typeQuantity.setSystem(OBSERVATION_QUANTITY_VALUE);
        typeQuantity.setUnit(observationFull.getResultValueUnits());

        CodeableConcept codeConcept = addCodeableConcept(observationFull.getCode(), observationFull.getDescription(), ResourceConstants.OBSERVATION_SYSTEM,
                "",extension);

        observation.setCode(codeConcept);

        return observation;
    }


    private Period getEffectiveDateTime(String date) {
        Period period = new Period();
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            period.setStart(format.parse(date));
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return period;
    }


    private CodeableConcept addCodeableConcept(String codeValue, String displayValue, String systemValue, String idValue, Extension extension) {
        CodeableConcept code = new CodeableConcept();
        code.addCoding()
                .setCode(codeValue)
                .setDisplay(displayValue)
                .setSystem(systemValue)
                .setId(idValue).addExtension(extension);

        return code;
    }
}
