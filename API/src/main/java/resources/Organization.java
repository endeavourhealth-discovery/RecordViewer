package resources;

import ca.uhn.fhir.context.FhirContext;
import org.endeavourhealth.recordviewer.common.models.OrgnizationSummary;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Organization {

	public org.hl7.fhir.dstu3.model.Organization  getOrgFhirResource(OrgnizationSummary orgnizationSummary,String id)
	{
		org.hl7.fhir.dstu3.model.Organization organization = null;
		organization = new org.hl7.fhir.dstu3.model.Organization();

		if(orgnizationSummary!=null) {
			organization.addIdentifier()
					.setSystem("https://fhir.nhs.uk/Id/ods-organization-code")
					.setValue(orgnizationSummary.getOdscode());
			organization.getMeta().addProfile("https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-Organization-1");
			organization.setName(orgnizationSummary.getName());
			//id is hardcoded now later we need to generate dynamically
            organization.setId(id);
			organization.addAddress()
					.setPostalCode(orgnizationSummary.getPostcode());
		}
		return organization;
	}


}
