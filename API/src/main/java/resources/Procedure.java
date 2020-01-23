package resources;

import org.endeavourhealth.recordviewer.common.models.EpisodeOfCareFull;
import org.endeavourhealth.recordviewer.common.models.PractitionerFull;
import org.endeavourhealth.recordviewer.common.models.ProcedureFull;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Narrative;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.endeavourhealth.recordviewer.common.constants.ResourceConstants.*;

public class Procedure {
    private static final Logger LOG = LoggerFactory.getLogger(PractitionerRole.class);

    private ProcedureFull procedureFull;

    public Procedure(ProcedureFull procedureFull) {
        this.procedureFull = procedureFull;
    }

    public org.hl7.fhir.dstu3.model.Procedure getProcedureResource() {
        LOG.info("Entering getProcedureResource() method");
        org.hl7.fhir.dstu3.model.Procedure procedure = new org.hl7.fhir.dstu3.model.Procedure();

        UUID uuid = UUID.randomUUID();
        procedure.setId(uuid.toString());
        procedure.setStatus(org.hl7.fhir.dstu3.model.Procedure.ProcedureStatus.COMPLETED);
        DateTimeType dateTimeType = new DateTimeType();
        dateTimeType.setValue(procedureFull.getDate());
        procedure.setPerformed(dateTimeType);

        Narrative narrative = new Narrative();
        narrative.setStatus(Narrative.NarrativeStatus.GENERATED);
        procedure.setText(narrative);

        procedure.setCode(getCode());

        return procedure;
    }

    private CodeableConcept getCode() {
        CodeableConcept codeableConcept = new CodeableConcept();
        Coding coding = new Coding();
        coding.setSystem(PROCEDURE_SYSTEM);
        coding.setCode(procedureFull.getCode());
        coding.setDisplay(procedureFull.getName());
        return codeableConcept.addCoding(coding);
    }
}

