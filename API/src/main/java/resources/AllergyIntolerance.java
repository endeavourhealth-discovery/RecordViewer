package resources;

import ca.uhn.fhir.context.FhirContext;
import org.endeavourhealth.recordviewer.common.models.AllergyFull;
import org.hl7.fhir.dstu3.model.*;

import java.text.SimpleDateFormat;
import java.util.UUID;

public class AllergyIntolerance {

	public static org.hl7.fhir.dstu3.model.AllergyIntolerance getAllergyIntoleranceResource(AllergyFull allergyfull)
	{
		org.hl7.fhir.dstu3.model.AllergyIntolerance allergy = new org.hl7.fhir.dstu3.model.AllergyIntolerance();

		allergy.setClinicalStatus(org.hl7.fhir.dstu3.model.AllergyIntolerance.AllergyIntoleranceClinicalStatus.ACTIVE);
		allergy.setVerificationStatus(org.hl7.fhir.dstu3.model.AllergyIntolerance.AllergyIntoleranceVerificationStatus.CONFIRMED);
		allergy.setType(org.hl7.fhir.dstu3.model.AllergyIntolerance.AllergyIntoleranceType.ALLERGY);
		allergy.getMeta().addProfile("https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-AllergyIntolerance-1");

		// manifestation or codeable concept?
		CodeableConcept code = new CodeableConcept();
		code.addCoding()
				.setCode(allergyfull.getCode())
				.setSystem("http://snomed.info/sct")
				.setDisplay(allergyfull.getName());
		allergy.setId(UUID.randomUUID().toString());
        allergy.setCode(code);

		allergy.setAssertedDate(allergyfull.getDate());

		return allergy;
	}

}
