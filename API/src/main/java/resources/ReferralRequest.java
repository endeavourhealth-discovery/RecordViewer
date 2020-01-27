package resources;

import org.endeavourhealth.recordviewer.common.models.ImmunizationFull;
import org.endeavourhealth.recordviewer.common.models.ReferralRequestFull;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.endeavourhealth.recordviewer.common.constants.ResourceConstants.PROCEDURE_SYSTEM;

public class ReferralRequest {
    private static final Logger LOG = LoggerFactory.getLogger(Observation.class);
    public static org.hl7.fhir.dstu3.model.ReferralRequest getReferralRequestResource(ReferralRequestFull referralRequestFull) {

        org.hl7.fhir.dstu3.model.ReferralRequest referralRequest = new org.hl7.fhir.dstu3.model.ReferralRequest();
        referralRequest.setId(UUID.randomUUID().toString());
        referralRequest.setStatus(org.hl7.fhir.dstu3.model.ReferralRequest.ReferralRequestStatus.ACTIVE);

        if(null!=referralRequestFull.getPriority())
        {
            try {
                referralRequest.setPriority(org.hl7.fhir.dstu3.model.ReferralRequest.ReferralPriority.fromCode(referralRequestFull.getPriority()));
            }
            catch(Exception e)
            {
                LOG.error("In valid Priority found:"+referralRequestFull.getPriority());

            }
             }

        if(null!=referralRequestFull.getIntent())
        {
            try {
                referralRequest.setIntent(org.hl7.fhir.dstu3.model.ReferralRequest.ReferralCategory.fromCode(referralRequestFull.getIntent()));
            }
            catch(Exception e)
            {
                LOG.error("In valid Intent found:"+referralRequestFull.getIntent());

            }
              }

        if(null!=referralRequestFull.getClinicalEffectiveDate())
        {
            referralRequest.setAuthoredOn(referralRequestFull.getClinicalEffectiveDate());
        }

        if(null!=referralRequestFull.getTypeCode())
        {

            CodeableConcept codeableConcept = new CodeableConcept();
            Coding coding = new Coding();
            coding.setSystem("http://snomed.info/sct");
            coding.setCode(referralRequestFull.getTypeCode());
            coding.setDisplay(referralRequestFull.getTypeDisplay());
            referralRequest.setType( codeableConcept.addCoding(coding));
        }

        if(null!=referralRequestFull.getSpecialityCode())
        {

            CodeableConcept codeableConcept = new CodeableConcept();
            Coding coding = new Coding();
            coding.setSystem("http://orionhealth.com/fhir/apps/specialties");
            coding.setCode(referralRequestFull.getSpecialityCode());
            coding.setDisplay(referralRequestFull.getSpecialityName());
            referralRequest.setSpecialty( codeableConcept.addCoding(coding));
        }
       return referralRequest;
    }



}