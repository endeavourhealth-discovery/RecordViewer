package resources;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.*;

import java.text.SimpleDateFormat;

public class AllergyIntolerance {

	public static org.hl7.fhir.dstu3.model.AllergyIntolerance getAllergyIntlResource(Integer patientid, String clineffdate, String allergyname, String snomedcode, String PatientRef, Integer ddsid, String putloc)
	{
		org.hl7.fhir.dstu3.model.AllergyIntolerance allergy = new org.hl7.fhir.dstu3.model.AllergyIntolerance();

		allergy.setClinicalStatus(org.hl7.fhir.dstu3.model.AllergyIntolerance.AllergyIntoleranceClinicalStatus.ACTIVE);
		allergy.setVerificationStatus(org.hl7.fhir.dstu3.model.AllergyIntolerance.AllergyIntoleranceVerificationStatus.CONFIRMED);
		allergy.setType(org.hl7.fhir.dstu3.model.AllergyIntolerance.AllergyIntoleranceType.ALLERGY);
		allergy.getMeta().addProfile("https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-AllergyIntolerance-1");

		// manifestation or codeable concept?
		CodeableConcept code = new CodeableConcept();
		code.addCoding()
				.setCode(snomedcode)
				.setSystem("http://snomed.info/sct")
				.setDisplay(allergyname);

		allergy.setCode(code);

		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			allergy.setAssertedDate(format.parse(clineffdate));
		} catch (Exception e) {
		}

		return allergy;
	}

}
