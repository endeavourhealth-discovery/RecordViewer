package resources;

import org.endeavourhealth.recordviewer.common.models.AppointmentFull;

import org.hl7.fhir.dstu3.model.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.endeavourhealth.recordviewer.common.constants.ResourceConstants.VALUE_STRING;

public class Appointment {

	/**
	 *
	 * @param appointmentResult
	 * @return
	 * @throws Exception
	 */
	public org.hl7.fhir.dstu3.model.Appointment getAppointmentResource(AppointmentFull appointmentResult) throws Exception {
		int actualDuration = appointmentResult.getActualDuration();
		String startdate = replaceNull(appointmentResult.getStartDate());
		int plannedDuration = appointmentResult.getPlannedDuration();
		String type = replaceNull(appointmentResult.getType());
		UUID id = UUID.randomUUID();

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		Date startDt = formatter.parse(startdate);

		org.hl7.fhir.dstu3.model.Appointment appointment = new org.hl7.fhir.dstu3.model.Appointment();

		appointment.setId(String.valueOf(id));
		appointment.setMinutesDuration(actualDuration);
		appointment.setCreated(startDt);

		Coding coding1 = new Coding();
		/*coding1.setSystem("");*/
		coding1.setCode("gp");
		coding1.setDisplay("General Practice");
		appointment.getServiceCategory().addCoding(coding1);

		Coding coding2 = new Coding();
		/*coding2.setSystem("");*/
		coding2.setCode("gp");
		coding2.setDisplay("General Practice");
		appointment.addSpecialty().addCoding(coding2);

		Coding coding = new Coding();
		coding.setDisplay(type);
		appointment.getAppointmentType().addCoding(coding);
		
		return appointment;
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
