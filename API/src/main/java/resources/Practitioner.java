package resources;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.endeavourhealth.recordviewer.common.models.PractitionerResult;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.StringType;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Practitioner {
    private static final Logger LOG = LoggerFactory.getLogger(Practitioner.class);

    private PractitionerResult practitionerResult;
    private static final String IDENTIFIER_URL = "https://fhir.nhs.uk/Id/sds-user-id";
    private static final String PRACTITIONER_URL = "https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-Practitioner-1";
    private static final String USE = "Usual";

    public Practitioner(PractitionerResult practitionerResult){
        this.practitionerResult = practitionerResult;
    }

    public org.hl7.fhir.dstu3.model.Practitioner getPractitionerResource()
    {
        LOG.info("Entering getPractitionerResource() method");
        String id = StringUtils.defaultString(practitionerResult.getId());
        org.hl7.fhir.dstu3.model.Practitioner practitioner = new org.hl7.fhir.dstu3.model.Practitioner();

        byte[] bytes = new byte[0];
        try {
            bytes = id.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOG.error("UUID generation error" + e.getMessage());
        }
        UUID uuid = UUID.nameUUIDFromBytes(bytes);

        practitioner.setId(uuid.toString());

        practitioner.getMeta().addProfile(PRACTITIONER_URL);
        practitioner.getMeta().setLastUpdated(getSystemDate());

        practitioner.addIdentifier()
                .setSystem(IDENTIFIER_URL);

        HumanName name = practitioner.addName();
        String[] nameList = splitName(practitionerResult.getName());
        name.setUse(HumanName.NameUse.USUAL);

        if(ArrayUtils.isNotEmpty(nameList) && nameList.length == 3) {
            name.setFamily(nameList[0]);
            name.setGiven(Arrays.asList(new StringType(nameList[1])));
            name.setPrefix(Arrays.asList(new StringType(nameList[2])));
        } else {
            LOG.error("Something wrong in receiving Practitioner name details" + practitionerResult.getName());
        }
        return practitioner;
    }

    private Date getSystemDate(){
            return java.sql.Timestamp.valueOf(LocalDateTime.now());
    }

    private static String[] splitName(String fullName){
        String modifiedName = fullName.replaceAll("\\W", " ");
        return Arrays.stream(modifiedName.split("  "))
                .map(String::trim)
                .toArray(String[]::new);

    }
}
