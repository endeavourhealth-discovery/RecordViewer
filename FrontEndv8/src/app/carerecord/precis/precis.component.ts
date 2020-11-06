import {Component, EventEmitter, OnInit, Output, ViewChild} from '@angular/core';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {PatientComponent} from "../patient/patient.component";
import {MatDialog} from "@angular/material/dialog";
import {Globals} from '../globals'
import {AppMenuService} from "../../app-menu.service";

@Component({
  selector: 'app-precis',
  templateUrl: './precis.component.html',
  styleUrls: ['./precis.component.scss']
})
export class PrecisComponent implements OnInit {
  globals: Globals;
  summaryMode: number = 0;

  patientId: number = 0;
  name: string = "";
  dob: string = "";
  dod: string = "";
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
    private menuProvider: AppMenuService,
    globals: Globals) {
        this.globals = globals;
    }

  ngOnInit() {
    this.patientId = this.globals.patientId;

    if (this.patientId==0)
      this.swapPatient();
    else
      this.loadPatient();
  }

  swapPatient() {
      const dialogRef = this.dialog.open(PatientComponent, {
        height: '820px',
        width: '1600px',
        data: {patientId: this.patientId}
      });

      dialogRef.afterClosed().subscribe(result => {
        if (result)
          this.patientId = result;
        this.loadPatient();
      });
  }

  clearPatient() {
    this.patientId = -1;
    this.carerecordService.getPatientSummary(this.patientId)
      .subscribe(
        (result) => this.setPatient(result),
        (error) => this.log.error(error)
      );
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
    this.dod = patient.dod;
    this.nhsNumber = patient.nhsNumber;
    this.address = patient.address;
    this.gender = patient.gender;
    this.age = patient.age;
    this.usual_gp = patient.usual_gp;
    this.organisation = patient.organisation;
    this.registration = patient.registration;

    this.patientChange.emit(this.patientId);
    this.globals.patientId = this.patientId;

    // get warnings for menu badge
    this.carerecordService.getObservation(this.patientId, 8, 1, '', this.summaryMode)
      .subscribe(
        (result) => this.menuProvider.setMenuBadge(5, result.length.toString()),
        (error) => this.log.error(error)
      );
    // get allergies for menu badge
    this.carerecordService.getAllergy(this.patientId, this.summaryMode)
      .subscribe(
        (result) => this.menuProvider.setMenuBadge(4, result.length.toString()),
        (error) => this.log.error(error)
      );
    // get conditions for menu badge
    this.carerecordService.getObservation(this.patientId, 1, 1, '', this.summaryMode)
      .subscribe(
        (result) => this.menuProvider.setMenuBadge(2, result.length.toString()),
        (error) => this.log.error(error)
      );

  }

}
