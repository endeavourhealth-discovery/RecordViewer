package service;

import org.endeavourhealth.recordviewer.common.dal.RecordViewerJDBCDAL;
import org.endeavourhealth.recordviewer.common.models.PractitionerResult;
import org.hl7.fhir.dstu3.model.PractitionerRole;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.Resource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FhirService {
    RecordViewerJDBCDAL viewerDAL;
    Map<Integer, List<Resource>> practitionerRoleResourceList;

    public FhirService(RecordViewerJDBCDAL viewerDAL, Map<Integer, List<Resource>> practitionerRoleResourceList) {
        this.viewerDAL = viewerDAL;
        this.practitionerRoleResourceList = practitionerRoleResourceList;
    }

    public PractitionerRole getPractitionerRoleResource(Integer practitionerId, Organization organizationResource) throws Exception{
        PractitionerResult practitionerResult = viewerDAL.getPractitioner(practitionerId);
        resources.Practitioner practitioner = new resources.Practitioner(practitionerResult);
        org.hl7.fhir.dstu3.model.Practitioner practitionerResource = practitioner.getPractitionerResource();

        resources.PractitionerRole practitionerRole = new resources.PractitionerRole(practitionerResult);
        org.hl7.fhir.dstu3.model.PractitionerRole practitionerRoleResource = practitionerRole.getPractitionerRoleResource();
        practitionerRoleResource.setPractitioner(new Reference(practitionerResource));
        practitionerRoleResource.setOrganization(new Reference(organizationResource));
        practitionerRoleResourceList.put(practitionerId, Arrays.asList(practitionerResource, practitionerRoleResource));
        return practitionerRoleResource;
    }
}

