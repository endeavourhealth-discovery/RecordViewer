import {AfterViewInit, Component, OnInit, ViewChild} from '@angular/core';
import {MatTableDataSource} from '@angular/material';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {PageEvent} from '@angular/material/paginator';
import {PrecisComponent} from "../precis/precis.component";

@Component({
  selector: 'app-care-summary',
  templateUrl: './care-summary.component.html',
  styleUrls: ['./care-summary.component.scss']
})
export class CareSummaryComponent implements OnInit {
  // @ts-ignore
  @ViewChild(PrecisComponent) precisComponentReference;

  events1: any;
  dataSource1: MatTableDataSource<any>;
  page1: number = 0;
  size1: number = 5;
  events2: any;
  dataSource2: MatTableDataSource<any>;
  page2: number = 0;
  size2: number = 5;
  patientId: number;

  displayedColumns1: string[] = ['name','dose','quantity','date'];
  displayedColumns2: string[] = ['name','status', 'date'];

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

      this.loadConditions();
      this.loadMedication();
    });
  }

  loadMedication() {
    this.events1 = null;
    this.carerecordService.getMedication(this.page1, this.size1, this.patientId)
      .subscribe(
        (result) => this.displayMedication(result),
        (error) => this.log.error(error)
      );
  }

  loadConditions() {
    this.events2 = null;
    this.carerecordService.getObservation(this.page2, this.size2, this.patientId, 1)
      .subscribe(
        (result) => this.displayConditions(result),
        (error) => this.log.error(error)
      );
  }

  displayMedication(events: any) {
    this.events1 = events;
    this.dataSource1 = new MatTableDataSource(events.results);
  }

  displayConditions(events: any) {
    this.events2 = events;
    this.dataSource2 = new MatTableDataSource(events.results);
  }

  onPage1(event: PageEvent) {
    this.page1 = event.pageIndex;
    this.size1 = event.pageSize;
    this.loadMedication();
  }

  onPage2(event: PageEvent) {
    this.page2 = event.pageIndex;
    this.size2 = event.pageSize;
    this.loadConditions();
  }

}
