import {Component, Inject, OnInit} from '@angular/core';
import {MatDialogRef, MAT_DIALOG_DATA} from '@angular/material/dialog';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {MatTableDataSource} from "@angular/material/table";
import {PageEvent} from "@angular/material/paginator";

export interface DialogData {
  nhsNumber: string;
}

@Component({
  selector: 'app-patient',
  templateUrl: './patient.component.html',
  styleUrls: ['./patient.component.scss']
})

export class PatientComponent {
  events: any[] = [];
  dataSource: MatTableDataSource<any>;
  page: number = 0;
  size: number = 10;
  name: string = '';
  nhsNumber: string = '0';
  dob: string = '';

  displayedColumns: string[] = ['name/address', 'dob/nhsNumber', 'age/gender', 'usual_gp/organisation', 'registration'];

  constructor(
    public dialogRef: MatDialogRef<PatientComponent>,
    private carerecordService: CareRecordService,
    private log: LoggerService,
    @Inject(MAT_DIALOG_DATA) public data: DialogData) {

    this.events = [
      {id: "0", name: "no results"},{id: "0", name: "no results"},
      {id: "0", name: "no results"},{id: "0", name: "no results"},
      {id: "0", name: "no results"},{id: "0", name: "no results"},
      {id: "0", name: "no results"},{id: "0", name: "no results"},
      {id: "0", name: "no results"},{id: "0", name: "no results"}
    ];
    this.dataSource = new MatTableDataSource(this.events);
  }

  loadEvents() {
    this.events = null;
    this.carerecordService.getPatients(this.page, this.size, this.name, this.nhsNumber, this.formatDate(this.dob))
      .subscribe(
        (result) => this.displayEvents(result),
        (error) => this.log.error(error)
      );
  }

  patientEntered(event) {
    if (this.name!="")
      this.nhsNumber = "";
    if (event.key === "Enter") {
      this.loadEvents();
    }
  }

  displayEvents(events: any) {
    console.log(events);
    this.events = events;
    this.dataSource = new MatTableDataSource(events.results);
  }

  onPage(event: PageEvent) {
    this.page = event.pageIndex;
    this.size = event.pageSize;
    this.loadEvents();
  }

  onCancelClick(): void {
    this.dialogRef.close();
  }

  formatDate(date) {
    if (date=="") return "";

    var d = new Date(date),
      month = '' + (d.getMonth() + 1),
      day = '' + d.getDate(),
      year = d.getFullYear();

    if (month.length < 2)
      month = '0' + month;
    if (day.length < 2)
      day = '0' + day;

    return [year, month, day].join('-');
  }

}
