package resources;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.*;

import java.sql.SQLException;

public class Medication {

	// should really be in a different class
	private static CodeableConcept addCodeableConcept(String snomed, String term)
	{
		CodeableConcept code = new CodeableConcept();
		code.addCoding()
				.setCode(snomed)
				.setDisplay(term)
				.setSystem("http://snomed.info/sct");

		return code;
	}

	private String getMedicationResource(String snomedcode, String term)
	{
		FhirContext ctx = FhirContext.forDstu3();

		//Medication medication = null;

		org.hl7.fhir.dstu3.model.Medication medication = new org.hl7.fhir.dstu3.model.Medication();

		medication.getMeta().addProfile("https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-Medication-1");

		CodeableConcept code = addCodeableConcept(snomedcode, term);
		medication.setCode(code);

		String encoded = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(medication);

		return encoded;
	}

}
