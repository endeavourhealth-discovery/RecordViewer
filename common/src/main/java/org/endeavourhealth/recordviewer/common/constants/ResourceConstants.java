package org.endeavourhealth.recordviewer.common.constants;

public class ResourceConstants {

    //Practitioner Role
    public static final String PRACTITIONER_ROLE_URL = "https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-PractitionerRole-1";

    //Practitioner
    public static final String IDENTIFIER_URL = "https://fhir.nhs.uk/Id/sds-user-id";
    public static final String PRACTITIONER_URL = "https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-Practitioner-1";
    public static final String PRACTITIONER_ROLE_SYSTEM = "https://fhir.hl7.org.uk/STU3/CodeSystem/CareConnect-SDSJobRoleName-1";

    //Observation
    public static final String TITLE = "Miscellaneous record";
    public static final String  OBSERVATION_LIST_PROFILE = "https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-List-1";
    public static final String  OBSERVATION_LIST_CODE = "event-date";
    public static final String  OBSERVATION_LIST_SYSTEM = "http://hl7.org/fhir/list-order";
    public static final String OBSERVATION_PROFILE = "https://fhir.nhs.uk/STU3/StructureDefinition/CareConnect-GPC-Observation-1";
    public static final String OBSERVATION_IDENTIFIER = "https://fhir.nhs.uk/Id/cross-care-setting-identifier";
    public static final String OBSERVATION_SYSTEM = "http://snomed.info/sct";
    public static final String OBSERVATION_DESCRIPTION = "descriptionDisplay";
    public static final String OBSERVATION_QUANTITY_VALUE = "http://unitsofmeasure.org";
    public static final String VALUE_STRING = "valueString";
    public static final String VALUE_QUANTITY = "valueQuantity";

}
