package resources;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class MedicationStatement {
	private Dosage addDosage(String dosagetext, String qtyvalue, String qtyunit)
	{
		Dosage dose = null;

		dose = new Dosage();
		dose.setText(dosagetext);


		if ( (qtyvalue != null) & (qtyunit != null) ) {
			dose.setDose(new SimpleQuantity()
					//.setValue(Integer.parseInt(qtyvalue))
					.setValue(Double.parseDouble(qtyvalue))
					.setUnit(qtyunit)
			);
		}

		return dose;
	}

	private String GetMedicationStatementResource(Integer patientid, String dose, String quantityvalue, String quantityunit, String clinicaleffdate, String medicationname, String snomedcode, String PatientRef, String rxref, Integer ddsid, String putloc)
	{
		FhirContext ctx = FhirContext.forDstu3();

		// MedicationStatement rxstatement = null;

		org.hl7.fhir.dstu3.model.MedicationStatement rxstatement = new org.hl7.fhir.dstu3.model.MedicationStatement();

		if (putloc.length()>0) {
			rxstatement.setId(putloc);
		}

		rxstatement.addIdentifier()
				.setSystem("https://discoverydataservice.net")
				.setValue(ddsid.toString());

		rxstatement.getMeta().addProfile("https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-MedicationStatement-1");

		// this needs to be a switch statement using ?
		rxstatement.setStatus(org.hl7.fhir.dstu3.model.MedicationStatement.MedicationStatementStatus.ACTIVE);

		//rxstatement.setSubject(new Reference("/api/Patient/33"));

		Period period = new Period();
		try {
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			period.setStart(format.parse(clinicaleffdate));
		} catch (Exception e) {
		}
		rxstatement.setEffective(period);

		rxstatement.setTaken(org.hl7.fhir.dstu3.model.MedicationStatement.MedicationStatementTaken.UNK);

		rxstatement.setSubject(new Reference("Patient/" + PatientRef));

		rxstatement.setMedication(new Reference("Medication/" + rxref)
				.setDisplay(medicationname));

		ArrayList dosages=new ArrayList();

		Dosage doseage = addDosage(dose, quantityvalue, quantityunit);
		dosages.add(doseage);
		rxstatement.setDosage(dosages);

		String encoded = ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString(rxstatement);

		return encoded;
	}

}
