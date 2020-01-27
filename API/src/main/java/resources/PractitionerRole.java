package resources;

import org.endeavourhealth.recordviewer.common.models.PractitionerFull;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import static org.endeavourhealth.recordviewer.common.constants.ResourceConstants.PRACTITIONER_ROLE_SYSTEM;
import static org.endeavourhealth.recordviewer.common.constants.ResourceConstants.PRACTITIONER_ROLE_URL;

public class PractitionerRole {
    private static final Logger LOG = LoggerFactory.getLogger(PractitionerRole.class);

    private PractitionerFull practitionerResult;

    public PractitionerRole(PractitionerFull practitionerResult) {
        this.practitionerResult = practitionerResult;
    }

    public org.hl7.fhir.dstu3.model.PractitionerRole getPractitionerRoleResource() {
        LOG.info("Entering getPractitionerRoleResource() method");
        org.hl7.fhir.dstu3.model.PractitionerRole practitionerRole = new org.hl7.fhir.dstu3.model.PractitionerRole();

        UUID uuid = UUID.randomUUID();
        practitionerRole.setId(uuid.toString());

        practitionerRole.getMeta().addProfile(PRACTITIONER_ROLE_URL);

        CodeableConcept code = new CodeableConcept();
        code.addCoding()
                .setCode(practitionerResult.getRoleCode())
                .setDisplay(practitionerResult.getRoleDesc())
                .setSystem(PRACTITIONER_ROLE_SYSTEM);
        practitionerRole.addCode(code);

        return practitionerRole;
    }
}