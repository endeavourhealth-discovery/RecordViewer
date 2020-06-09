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
  telecom : string;
  adduse : string;
  address : string;
  postcode : string;
  city : string;
  otheraddresses : string;
  orgname : string;
  startdate : string;

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
    this.carerecordService.getDemographic(this.patientId)
      .subscribe(
        (result) => this.setPatient(result),
        (error) => this.log.error(error)
      );
  }

  setPatient(patient: any) {
    this.id = patient.id;
    this.title = patient.title;
    this.name = patient.lastname+", "+patient.firstname;
    this.gender = patient.gender;
    this.dob = patient.dob;
    this.dod = patient.dod;
    this.nhsNumber = patient.nhsNumber;
    this.telecom = patient.telecom;
    this.adduse = patient.adduse;
    this.address = patient.add1+"\n"+patient.add2+"\n"+patient.add3+"\n"+patient.add4;
    this.postcode = patient.postcode;
    this.city = patient.city;
    this.otheraddresses = patient.otheraddresses;
    this.orgname = patient.orgname;
    this.startdate = patient.startdate;


  }

}
