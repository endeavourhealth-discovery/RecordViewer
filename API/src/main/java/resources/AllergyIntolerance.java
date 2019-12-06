package resources;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.*;

import java.text.SimpleDateFormat;

public class AllergyIntolerance {

	private String getAllergyResource(Integer patientid, String clineffdate, String allergyname, String snomedcode, String PatientRef, Integer ddsid, String putloc)
	{
		//AllergyIntolerance allergy = null;

		FhirContext ctx = FhirContext.forDstu3();

		org.hl7.fhir.dstu3.model.AllergyIntolerance allergy = new org.hl7.fhir.dstu3.model.AllergyIntolerance();

		if (putloc.length()>0) {
			allergy.setId(putloc);
		}

		allergy.addIdentifier()
				.setSystem("https://discoverydataservice.net")
				.setValue(ddsid.toString());

		allergy.getMeta().addProfile("https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-AllergyIntolerance-1");
		allergy.setClinicalStatus(org.hl7.fhir.dstu3.model.AllergyIntolerance.AllergyIntoleranceClinicalStatus.ACTIVE);
		allergy.setVerificationStatus(org.hl7.fhir.dstu3.model.AllergyIntolerance.AllergyIntoleranceVerificationStatus.CONFIRMED);

		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			allergy.setAssertedDate(format.parse(clineffdate));
		} catch (Exception e) {
		}

		// manifestation or codeable concept?
		CodeableConcept code = new CodeableConcept();
		code.addCoding()
				.setCode(snomedcode)
				.setSystem("http://snomed.info/sct")
				.setDisplay(allergyname);

		allergy.setCode(code);

		allergy.setPatient(new Reference("Patient/" + PatientRef));

		String encoded = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(allergy);

		return encoded;
	}

}
