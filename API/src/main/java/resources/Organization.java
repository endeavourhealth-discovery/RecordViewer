package resources;

import ca.uhn.fhir.context.FhirContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Organization {

	private static String GetOrgResource(String odscode, String name, String postcode)
	{
		FhirContext ctx = FhirContext.forDstu3();

		org.hl7.fhir.dstu3.model.Organization organization = null;
		organization = new org.hl7.fhir.dstu3.model.Organization();

		organization.addIdentifier()
				.setSystem("https://fhir.nhs.uk/Id/ods-organization-code")
				.setValue(odscode);

		organization.setName(name);

		organization.addAddress()
				.setPostalCode(postcode);

		String encoded = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(organization);

		return encoded;
	}

}
