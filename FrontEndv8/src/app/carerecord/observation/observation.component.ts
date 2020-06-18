import {AfterViewInit, Component, OnInit, ViewChild} from '@angular/core';
import {MatTableDataSource} from '@angular/material';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {PageEvent} from '@angular/material/paginator';
import {PrecisComponent} from "../precis/precis.component";
import {ActivatedRoute} from "@angular/router";

@Component({
  selector: 'app-observation',
  templateUrl: './observation.component.html',
  styleUrls: ['./observation.component.scss']
})
export class ObservationComponent implements OnInit, AfterViewInit {
  // @ts-ignore
  @ViewChild(PrecisComponent) precisComponentReference;

  events: any;
  dataSource: MatTableDataSource<any>;
  page: number = 0;
  size: number = 12;
  patientId: number;
  eventType: number;
  eventTypeTitle: string;
  dateTitle: string;
  subTitle: string;
  icon: string;
  active: number = 0;
  term: string = '';

  displayedColumns: string[] = ['name', 'date'];

  ngAfterViewInit(): void {
    this.patientId = this.precisComponentReference.patientId;
  }

  constructor(
    private route: ActivatedRoute,
    private carerecordService: CareRecordService,
    private log: LoggerService
    ) { }

  ngOnInit() {
    this.route.data.subscribe(
      (data) => this.eventType = data.eventType
    );

    this.precisComponentReference.patientChange.subscribe(patientId => {
      console.log("patient changed to "+patientId);
      this.patientId = patientId;
      this.loadEvents();
    });
  }

  loadEvents() {
    if (this.eventType==1) {
      this.eventTypeTitle = "Conditions";
      this.subTitle = "A clinical condition, problem, diagnosis, or other event, situation, issue, or clinical concept that has risen to a level of concern";
      this.icon = 'fa-diagnoses';
      this.dateTitle = "Onset date";
      this.displayedColumns = ['name','status', 'date'];
    }
    else if (this.eventType==2) {
      this.eventTypeTitle = "Observations";
      this.subTitle = "Measurements and simple assertions made about a patient";
      this.icon = 'fa-monitor-heart-rate';
      this.dateTitle = "Effective date";
    }
    else if (this.eventType==3) {
      this.eventTypeTitle = "Procedures";
      this.subTitle = "Details of procedures performed on a patient";
      this.icon = 'fa-procedures';
      this.dateTitle = "Effective date";
    }
    else if (this.eventType==4) {
      this.eventTypeTitle = "Family history";
      this.subTitle = "Significant health conditions for a person related to the patient relevant in the context of care for the patient";
      this.icon = 'fa-user-friends';
      this.dateTitle = "Effective date";
    }
    else if (this.eventType==5) {
      this.eventTypeTitle = "Immunisations";
      this.subTitle = "Current and historical administration of vaccines";
      this.icon = 'fa-syringe';
      this.dateTitle = "Effective date";
    }
    else if (this.eventType==6) {
      this.eventTypeTitle = "Procedure requests";
      this.subTitle = "A record of a request for a procedure to be performed";
      this.icon = 'fa-user-md-chat';
      this.dateTitle = "Effective date";
      this.displayedColumns = ['name','status', 'date'];
    }
    else if (this.eventType==7) {
      this.eventTypeTitle = "Diagnostic orders";
      this.subTitle = "A record of a request for a diagnostic investigation service to be performed";
      this.icon = 'fa-x-ray';
      this.dateTitle = "Effective date";
    }
    else if (this.eventType==8) {
      this.eventTypeTitle = "Warnings & Flags";
      this.subTitle = "Prospective warnings of potential issues when providing care to the patient";
      this.icon = 'fa-exclamation-triangle';
      this.dateTitle = "Effective date";
      this.displayedColumns = ['name','status', 'date'];
    }

    this.events = null;
    console.log("page: "+this.page+", size: "+this.size);
    this.carerecordService.getObservation(this.page, this.size, this.patientId, this.eventType, this.active, this.term)
      .subscribe(
        (result) => this.displayEvents(result),
        (error) => this.log.error(error)
      );
  }

  termEntered(event) {
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

}
