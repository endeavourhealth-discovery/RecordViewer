package resources;

import org.apache.commons.lang3.ArrayUtils;
import org.endeavourhealth.recordviewer.common.models.PractitionerFull;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import static org.endeavourhealth.recordviewer.common.constants.ResourceConstants.IDENTIFIER_URL;
import static org.endeavourhealth.recordviewer.common.constants.ResourceConstants.PRACTITIONER_URL;

public class Practitioner {
    private static final Logger LOG = LoggerFactory.getLogger(Practitioner.class);

    private PractitionerFull practitionerResult;

    public Practitioner(PractitionerFull practitionerResult) {
        this.practitionerResult = practitionerResult;
    }

    public org.hl7.fhir.dstu3.model.Practitioner getPractitionerResource() {
        LOG.info("Entering getPractitionerResource() method");
        org.hl7.fhir.dstu3.model.Practitioner practitioner = new org.hl7.fhir.dstu3.model.Practitioner();

        UUID uuid = UUID.randomUUID();
        practitioner.setId(uuid.toString());

        practitioner.getMeta().addProfile(PRACTITIONER_URL);
        practitioner.getMeta().setLastUpdated(getSystemDate());

        practitioner.addIdentifier()
                .setSystem(IDENTIFIER_URL);

        HumanName name = practitioner.addName();
        String[] nameList = splitName(practitionerResult.getName());
        name.setUse(HumanName.NameUse.USUAL);

        if (ArrayUtils.isNotEmpty(nameList) && nameList.length == 3) {
            name.setFamily(nameList[0]);
            name.setGiven(Arrays.asList(new StringType(nameList[1])));
            name.setPrefix(Arrays.asList(new StringType(nameList[2])));
        } else {
            LOG.error("Something wrong in receiving Practitioner name details" + practitionerResult.getName());
        }

        return practitioner;
    }

    private Date getSystemDate() {
        return java.sql.Timestamp.valueOf(LocalDateTime.now());
    }

    private static String[] splitName(String fullName) {
        String modifiedName = fullName.replaceAll("\\W", " ");
        return Arrays.stream(modifiedName.split("  "))
                .map(String::trim)
                .toArray(String[]::new);

    }
}
