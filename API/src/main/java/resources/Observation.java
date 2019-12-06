package resources;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.*;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class Observation {

	private CodeableConcept addCodeableConcept(String snomed, String term, String parent)
	{
		CodeableConcept code = new CodeableConcept();
		code.addCoding()
				.setCode(snomed)
				.setDisplay(term)
				.setSystem("http://snomed.info/sct")
                .setId(parent);

		return code;
	}

	private org.hl7.fhir.dstu3.model.Observation.ObservationComponentComponent ObsCompComp(String coreconceptid, String term, String resultvalue, String resultvalueunits, String zid)
	{
		org.hl7.fhir.dstu3.model.Observation.ObservationComponentComponent occ= new org.hl7.fhir.dstu3.model.Observation.ObservationComponentComponent();
		CodeableConcept codecc = new CodeableConcept();
		codecc.addCoding()
				.setCode(coreconceptid)
				.setSystem("http://snomed.info/sct")
				.setDisplay(term)
				.setId(zid);
		occ.setCode(codecc);

		Quantity q = new Quantity();
		q.setValue(Double.parseDouble(resultvalue));
		q.setSystem("http://unitsofmeasure.org");
		if (resultvalueunits !=null) {q.setCode(resultvalueunits);}
		occ.setValue(q);

		return occ;
	}

	private String getObervationResource(Integer patientid, String snomedcode, String orginalterm, String resultvalue, String clineffdate, String resultvalunits, String PatientRef, String ids, Integer parent, Integer ddsid, String putloc)
	{
		String id = "";

		//Observation observation = null;

		FhirContext ctx = FhirContext.forDstu3();

		org.hl7.fhir.dstu3.model.Observation observation = new org.hl7.fhir.dstu3.model.Observation();

		if (putloc.length()>0) {
			observation.setId(putloc);
		}
		observation.setStatus(org.hl7.fhir.dstu3.model.Observation.ObservationStatus.FINAL);

        observation.addIdentifier()
                .setSystem("https://discoverydataservice.net/ddsid")
                .setValue(ddsid.toString());

        // for reporting
        if (parent!=0) {
			observation.addIdentifier()
					.setSystem("https://discoverydataservice.net/ddsparentid")
					.setValue(parent.toString());
		}

		String ObsRec = ""; String noncoreconceptid = "";

		// use parent code if necessary
		if (parent !=0) {
			try {

				//ObsRec= repository.getObservationRecordNew(Integer.toString(parent));

				String[] ss = ObsRec.split("\\~");

				noncoreconceptid = ss[0]; orginalterm = ss[1];
				if (noncoreconceptid.length()==0) noncoreconceptid = ss[5];

				CodeableConcept code = addCodeableConcept(noncoreconceptid, orginalterm, parent.toString());
				observation.setCode(code);

				//System.out.println(ObsRec);
			} catch (Exception e) {
			}
		}

		if (parent == 0) {
			CodeableConcept code = addCodeableConcept(snomedcode, orginalterm, "");
			observation.setCode(code);
		}

		// http://hl7.org/fhir/stu3/valueset-observation-category.html
        // social-history, vital-signs, imaging, laboratory, procedure, survey, exam, therapy

        /*
		CodeableConcept vital = new CodeableConcept();
		vital.addCoding()
				.setCode("vital-signs");

		// might be a lab result, or something else?
		observation.addCategory(vital);
        */

		observation.setSubject(new Reference("/Patient/" + PatientRef));

		Period period = new Period();
		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			period.setStart(format.parse(clineffdate));
		} catch (Exception e) {
		}
		observation.setEffective(period);

		// nests codeable concepts
		String encoded = "";
		ArrayList occs=new ArrayList();

		if (ids.length() > 0) {
			String[] ss = ids.split("\\~");
			for (int i = 0; i < ss.length; i++) {
				id = ss[i];
				try {

					//ObsRec = repository.getObservationRecordNew(id);

					if (ObsRec.length() == 0) {continue;}
					String obs[] = ObsRec.split("\\~");
					snomedcode = obs[0]; orginalterm = obs[1]; resultvalue = obs[2]; clineffdate = obs[3]; resultvalunits = obs[4];
					if (snomedcode.length() == 0) snomedcode = obs[5];
					if (resultvalue.length() > 0 || resultvalunits.length() > 0) {
                        org.hl7.fhir.dstu3.model.Observation.ObservationComponentComponent ocs = ObsCompComp(snomedcode, orginalterm, resultvalue, resultvalunits, id);
                        occs.add(ocs);
                        observation.setComponent(occs);
                    }
				} catch (Exception e) {
				}
			}

			encoded = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(observation);
			return encoded;
		}

        System.out.println(resultvalue.length());

		if (resultvalue.length()>0) {
            org.hl7.fhir.dstu3.model.Observation.ObservationComponentComponent ocs = ObsCompComp(snomedcode, orginalterm, resultvalue, resultvalunits, ddsid.toString());
            occs.add(ocs);
            observation.setComponent(occs);
        }

		encoded = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(observation);

		return encoded;
	}

	public void DT(String prefix) {
        long timeNow = Calendar.getInstance().getTimeInMillis();
        java.sql.Timestamp ts = new java.sql.Timestamp(timeNow);
        String str = ts.toString();
        System.out.println(prefix+" "+str);
	}


}
