import {Component, EventEmitter, OnInit, Output, ViewChild} from '@angular/core';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {PatientComponent} from "../patient/patient.component";
import {MatDialog} from "@angular/material/dialog";
import {Globals} from '../globals'

@Component({
  selector: 'app-precis',
  templateUrl: './precis.component.html',
  styleUrls: ['./precis.component.scss']
})
export class PrecisComponent implements OnInit {
  globals: Globals;

  patientId: number = 0;
  name: string = "";
  dob: string = "";
  nhsNumber: string = "";
  address: string = "";
  gender: string;
  age: string = "";
  usual_gp: string = "";
  organisation: string = "";
  registration: string = "";

  @Output() patientChange: EventEmitter<number> = new EventEmitter();

  constructor(
    private carerecordService: CareRecordService,
    private log: LoggerService,
    private dialog: MatDialog,
    globals: Globals) {
        this.globals = globals;
    }

  ngOnInit() {
    this.patientId = this.globals.patientId;
    console.log("Patient ID = "+this.patientId);

    if (this.patientId==0)
      this.swapPatient();
    else
      this.loadPatient();
  }

  swapPatient() {
      const dialogRef = this.dialog.open(PatientComponent, {
        height: '850px',
        width: '1600px',
        data: {patientId: this.patientId}
      });

      dialogRef.afterClosed().subscribe(result => {
        if (result)
          this.patientId = result;
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
    this.name = patient.name;
    this.dob = patient.dob;
    this.nhsNumber = patient.nhsNumber;
    this.address = patient.address;
    this.gender = patient.gender;
    this.age = patient.age;
    this.usual_gp = patient.usual_gp;
    this.organisation = patient.organisation;
    this.registration = patient.registration;

    this.patientChange.emit(this.patientId);

    this.globals.patientId = this.patientId;

  }

}
