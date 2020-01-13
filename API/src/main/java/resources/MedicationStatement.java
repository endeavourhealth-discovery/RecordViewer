package resources;

import ca.uhn.fhir.context.FhirContext;
import org.endeavourhealth.recordviewer.common.models.MedicationStatementFull;
import org.endeavourhealth.recordviewer.common.models.PatientFull;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.Medication;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class MedicationStatement {

	/**
	 *
	 * @param medicationStatementResult
	 * @return
	 * @throws Exception
	 */
	public org.hl7.fhir.dstu3.model.MedicationStatement getMedicationStatementResource(MedicationStatementFull medicationStatementResult) throws Exception {
		String clinicalEffDate = replaceNull(medicationStatementResult.getDate());
		int status = medicationStatementResult.getStatus();
		String dose = replaceNull(medicationStatementResult.getDose());
		UUID id = UUID.randomUUID();

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		Date clinicalEffDt = formatter.parse(clinicalEffDate);

		org.hl7.fhir.dstu3.model.MedicationStatement medicationStatement = new org.hl7.fhir.dstu3.model.MedicationStatement();

		medicationStatement.setId(String.valueOf(id));
		medicationStatement.getMeta().addProfile("https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-MedicationStatement-1");
		medicationStatement.setTaken(org.hl7.fhir.dstu3.model.MedicationStatement.MedicationStatementTaken.UNK);

		medicationStatement.setDateAsserted(clinicalEffDt);
		//medicationStatement.getEffectivePeriod().setStart(clinicalEffDt);
		medicationStatement.addBasedOn().setReference("new MedicationRequest()"); //To be updated with MedicationRequest Object

		Extension extension1 = new Extension();
		extension1.setUrl("https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-MedicationStatementLastIssueDate-1");

		Extension extension2 = new Extension();
		extension2.setUrl("https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-PrescribingAgency-1");

		Coding coding = new Coding();
		coding.setCode("prescribed-at-gp-practice");
		coding.setSystem("https://fhir.hl7.org.uk/STU3/CodeSystem/CareConnect-PrescribingAgency-1");
		coding.setDisplay("Prescribed at GP practice");

		List<Extension> extensionList = new ArrayList<Extension>();
		extensionList.add(extension1);
		extensionList.add(extension2);
		medicationStatement.setExtension(extensionList);

		if(status == 1)
			medicationStatement.setStatus(org.hl7.fhir.dstu3.model.MedicationStatement.MedicationStatementStatus.ACTIVE);
		else
			medicationStatement.setStatus(org.hl7.fhir.dstu3.model.MedicationStatement.MedicationStatementStatus.COMPLETED);

		//medicationStatement.getMedicationReference().setReference("new Medication()"); //To be updated with Medication Object

		List<Dosage> dosageList = new ArrayList<Dosage>();
		Dosage dosage = new Dosage();
		dosage.setPatientInstruction("INSTRUCTIONS FOR PATIENT");
		dosage.setText(dose);
        dosageList.add(dosage);
		medicationStatement.setDosage(dosageList);

		return medicationStatement;
	}

	/**
	 *
	 * @param medicationStatementResult
	 * @return
	 * @throws Exception
	 */
	public org.hl7.fhir.dstu3.model.MedicationRequest getMedicationRequestResource(MedicationStatementFull medicationStatementResult) throws Exception {
		String clinicalEffDate = replaceNull(medicationStatementResult.getDate());
		String dose = replaceNull(medicationStatementResult.getDose());
		double qValue = medicationStatementResult.getQuantityValue();
		String qUnit = replaceNull(medicationStatementResult.getQuantityUnit());
		UUID id = UUID.randomUUID();

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		Date clinicalEffDt = formatter.parse(clinicalEffDate);

		org.hl7.fhir.dstu3.model.MedicationRequest medicationRequest = new org.hl7.fhir.dstu3.model.MedicationRequest();

		medicationRequest.setId(String.valueOf(id));
		medicationRequest.getMeta().addProfile("https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-MedicationRequest-1");
		medicationRequest.setStatus(org.hl7.fhir.dstu3.model.MedicationRequest.MedicationRequestStatus.COMPLETED);
		medicationRequest.setIntent(org.hl7.fhir.dstu3.model.MedicationRequest.MedicationRequestIntent.PLAN);

		medicationRequest.setAuthoredOn(clinicalEffDt);

		List<Dosage> dosageList = new ArrayList<Dosage>();
		Dosage dosage = new Dosage();
		dosage.setPatientInstruction("INSTRUCTIONS FOR PATIENT");
		dosage.setText(dose);
		dosageList.add(dosage);
		medicationRequest.setDosageInstruction(dosageList);

		Extension extension = new Extension();
		extension.setUrl("https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-PrescriptionType-1");
		medicationRequest.addExtension(extension);

		Extension extension1 = new Extension();

		Quantity quantity = new Quantity();
		quantity.getExtension().add(extension1);

				org.hl7.fhir.dstu3.model.MedicationRequest.MedicationRequestDispenseRequestComponent medicationRequestDispenseRequestComponent = new
						MedicationRequest.MedicationRequestDispenseRequestComponent();
		medicationRequestDispenseRequestComponent.getQuantity().setValue(qValue);
				medicationRequest.setDispenseRequest(medicationRequestDispenseRequestComponent);

		return medicationRequest;
	}

	/**
	 *
	 * @param input
	 * @return
	 */
	public static String replaceNull(String input) {
		return input == null ? "" : input;
	}

}
