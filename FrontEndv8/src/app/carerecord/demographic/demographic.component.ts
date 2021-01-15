import {Component, ViewChild} from '@angular/core';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {PrecisComponent} from "../precis/precis.component";

@Component({
  selector: 'app-demographic',
  templateUrl: './demographic.component.html',
  styleUrls: ['./demographic.component.scss']
})
export class DemographicComponent {
  // @ts-ignore
  @ViewChild(PrecisComponent) precisComponentReference;

  id : string;
  title : string;
  name : string;
  gender : string;
  dob : string;
  dod : string;
  nhsNumber : string;
  mobile : string;
  address : string;
  orgname : string;
  startdate : string;
  usualgp: string;
  regtype: string;

  constructor(
    private carerecordService: CareRecordService,
    private log: LoggerService
  ) { }

  loadPatient() {
    this.carerecordService.getPatientSummary(this.precisComponentReference.nhsNumber)
      .subscribe(
        (result) => this.setPatient(result),
        (error) => this.log.error(error)
      );
  }

  setPatient(patient: any) {
      this.id = patient.id;
      this.name = patient.name;
      this.gender = patient.gender;
      this.dob = patient.dob;
      this.dod = patient.dod;
      this.nhsNumber = patient.nhsNumber;
      this.mobile = patient.mobile;
      this.address = patient.address;
      this.orgname = patient.organisation;
      this.startdate = patient.start_date;
      this.usualgp = patient.usual_gp;
      this.regtype = patient.registration;
    }

}
