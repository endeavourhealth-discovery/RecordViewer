import {AfterViewInit, Component, OnInit, ViewChild} from '@angular/core';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {PrecisComponent} from "../precis/precis.component";

@Component({
  selector: 'app-demographic',
  templateUrl: './demographic.component.html',
  styleUrls: ['./demographic.component.scss']
})
export class DemographicComponent implements OnInit, AfterViewInit {
  // @ts-ignore
  @ViewChild(PrecisComponent) precisComponentReference;

  patientId: number = 0;

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

  ngAfterViewInit(): void {
    this.patientId = this.precisComponentReference.patientId;
  }

  constructor(
    private carerecordService: CareRecordService,
    private log: LoggerService
  ) { }

  ngOnInit() {
    this.precisComponentReference.patientChange.subscribe(patientId => {
      console.log("patient changed to "+patientId);
      this.patientId = patientId;
      this.loadPatient();
    });
  }

  loadPatient() {
    this.carerecordService.getPatientSummary(this.patientId)
      .subscribe(
        (result) => this.setPatient(result),
        (error) => this.log.error(error)
      );
  }

  setPatient(patient: any) {
    console.log(patient);
    this.id = patient.id;
    this.name = patient.name;
    this.gender = patient.gender;
    this.dob = patient.dob;
    this.dod = patient.date_of_death;
    this.nhsNumber = patient.nhsNumber;
    this.mobile = patient.mobile;
    this.address = patient.address;
    this.orgname = patient.organisation;
    this.startdate = patient.start_date;
    this.usualgp = patient.usual_gp;
    this.regtype = patient.registration;


  }

}
