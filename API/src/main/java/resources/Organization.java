package resources;

import org.endeavourhealth.recordviewer.common.models.OrganizationFull;
import org.endeavourhealth.recordviewer.common.models.OrganizationSummary;

import java.util.UUID;

public class Organization {

	public static org.hl7.fhir.dstu3.model.Organization  getOrgFhirResource(OrganizationFull organizationFull)
	{
		org.hl7.fhir.dstu3.model.Organization organization = null;
		organization = new org.hl7.fhir.dstu3.model.Organization();

		if(organizationFull !=null) {
			organization.addIdentifier()
					.setSystem("https://fhir.nhs.uk/Id/ods-organization-code")
					.setValue(organizationFull.getOdscode());
			organization.getMeta().addProfile("https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-Organization-1");
			organization.setName(organizationFull.getName());
			//id is hardcoded now later we need to generate dynamically
            organization.setId(UUID.randomUUID().toString());
			organization.addAddress()
					.setPostalCode(organizationFull.getPostcode());
		}
		return organization;
	}


}
