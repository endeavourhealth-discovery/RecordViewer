package resources;

import org.endeavourhealth.recordviewer.common.constants.ResourceConstants;
import org.endeavourhealth.recordviewer.common.models.PatientFull;
import org.hl7.fhir.dstu3.model.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Patient {

	public static org.hl7.fhir.dstu3.model.Patient getPatientResource(PatientFull patientResult) throws Exception {
		String id = replaceNull(patientResult.getId());
		String nhsNumber = replaceNull(patientResult.getNhsNumber());
		String gender = replaceNull(patientResult.getGender());
		String lastname = replaceNull(patientResult.getLastname());
		String title = replaceNull(patientResult.getTitle());
		String firstname = replaceNull(patientResult.getFirstname());
		String dob = replaceNull(patientResult.getDob());
		String dod = replaceNull(patientResult.getDod());
		String telecom = replaceNull(patientResult.getTelecom());
		String adduse = replaceNull(patientResult.getAdduse());
		String add1 = replaceNull(patientResult.getAdd1());
		String add2 = replaceNull(patientResult.getAdd2());
		String add3 = replaceNull(patientResult.getAdd3());
		String add4 = replaceNull(patientResult.getAdd4());
		String postcode = replaceNull(patientResult.getPostcode());
		String city = replaceNull(patientResult.getCity());
		String otheraddresses = replaceNull(patientResult.getOtheraddresses());
		String orglocation = replaceNull(patientResult.getOrglocation());
		String startdate = replaceNull(patientResult.getStartdate());

		org.hl7.fhir.dstu3.model.Patient patient = new org.hl7.fhir.dstu3.model.Patient();

		patient.setId(UUID.randomUUID().toString());

		patient.getMeta().addProfile("https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-Patient-1");

		patient.addIdentifier()
				.setValue(String.valueOf(patientResult.getId()))
				.setSystem(ResourceConstants.SYSTEM_ID);


		Identifier nhs = patient.addIdentifier()
				.setSystem("https://fhir.hl7.org.uk/Id/nhs-number")
				.setValue(nhsNumber);
		CodeableConcept code = new CodeableConcept();
		code.addCoding()
				.setCode("01")
				.setDisplay("Number present and verified")
				.setSystem("https://fhir.hl7.org.uk/CareConnect-NHSNumberVerificationStatus-1");

		nhs.addExtension()
				.setUrl("https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-NHSNumberVerificationStatus-1")
				.setValue(code);

		switch(gender) {
			case "Other":
				patient.setGender(Enumerations.AdministrativeGender.OTHER);
				break;
			case "Male":
				patient.setGender(Enumerations.AdministrativeGender.MALE);
				break;
			case "Female":
				patient.setGender(Enumerations.AdministrativeGender.FEMALE);
				break;
			case "Unknown":
				patient.setGender(Enumerations.AdministrativeGender.UNKNOWN);
				break;
			default:
				// code block
		}

		patient.addName()
				.setFamily(lastname)
				.addPrefix(title)
				.addGiven(firstname)
				.setUse(HumanName.NameUse.OFFICIAL);

		// contact_type`contact_use`contact_value|
		if (telecom.length()>0) {
			String[] ss = telecom.split("\\|");
			String z = "";
			for (int i = 0; i < ss.length; i++) {
				z = ss[i];
				String[] contact = z.split("\\`");
				ContactPoint t = new ContactPoint();

				t.setValue(contact[0]);

				if (contact[2].equals("Mobile")) t.setUse(ContactPoint.ContactPointUse.MOBILE);
				if (contact[2].equals("Home")) t.setUse(ContactPoint.ContactPointUse.HOME);

				if (contact[1].equals("Email")) t.setSystem(ContactPoint.ContactPointSystem.EMAIL);
				if (contact[1].equals("Phone")) t.setSystem(ContactPoint.ContactPointSystem.PHONE);

				patient.addTelecom(t);
			}
		}

		if (!dob.isEmpty()) {
			SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy");
			patient.setBirthDate(format.parse(dob));
		}

		if (!dod.isEmpty()) {
			BooleanType bool = new BooleanType();
			bool.setValue(true);
			patient.setDeceased(bool);
		}

		Address address = new Address();

		if (adduse.equals("HOME")) {address.setUse(Address.AddressUse.HOME);}
		if (adduse.equals("TEMP")) {address.setUse(Address.AddressUse.TEMP);}
		if (adduse.equals("OLD")) {address.setUse(Address.AddressUse.OLD);}

		address.addLine(add1);
		address.addLine(add2);
		address.addLine(add3);
		address.addLine(add4);
		address.setPostalCode(postcode);
		address.setCity(city);

		patient.addAddress(address);

		// add1`add2`add3`add4`city`postcode`useconceptid| <= alternative addresses
		if (otheraddresses.length()>0) {
			String[] ss = otheraddresses.split("\\|");
			String z = "";
			for (int i = 0; i < ss.length; i++) {
				z = ss[i];
				String[] zaddress = z.split("\\`");
				Address t = new Address();
				if (zaddress[6].equals("1335358")) {t.setUse(Address.AddressUse.HOME);}
				if (zaddress[6].equals("1335360")) {t.setUse(Address.AddressUse.TEMP);}
				if (zaddress[6].equals("1335361")) {t.setUse(Address.AddressUse.OLD);}
				t.addLine(zaddress[0]);
				t.addLine(zaddress[1]);
				t.addLine(zaddress[2]);
				t.addLine(zaddress[3]);
				t.setPostalCode(zaddress[5]);
				t.setCity(zaddress[4]);
				patient.addAddress(t);
			}
		}

		if (!startdate.isEmpty()) {
			SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy");
			Period period = new Period();
			period.setStart(format.parse(startdate));
			Extension registration = patient.addExtension();
			registration.setUrl("https://fhir.hl7.org.uk/STU3/StructureDefinition/Extension-CareConnect-RegistrationDetails-1");
			Extension ext1 = new Extension();
			ext1.setUrl("registrationPeriod");
			ext1.setValue(period);
			List<Extension> ext = new ArrayList<>();
			ext.add(ext1);
			registration.setExtension(ext);
		}




		return patient;
	}

	public static String replaceNull(String input) {
		return input == null ? "" : input;
	}

}
