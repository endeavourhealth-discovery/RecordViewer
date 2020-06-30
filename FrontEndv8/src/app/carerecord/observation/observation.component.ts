import {Component, OnInit, ViewChild} from '@angular/core';
import {MatTableDataSource} from '@angular/material';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {PageEvent} from '@angular/material/paginator';
import {PrecisComponent} from "../precis/precis.component";
import {ActivatedRoute} from "@angular/router";
import {AppMenuService} from "../../app-menu.service";

@Component({
  selector: 'app-observation',
  templateUrl: './observation.component.html',
  styleUrls: ['./observation.component.scss']
})
export class ObservationComponent implements OnInit {
  // @ts-ignore
  @ViewChild(PrecisComponent) precisComponentReference;

  events: any;
  dataSource: MatTableDataSource<any>;
  page: number = 0;
  size: number = 12;
  eventType: number;
  eventTypeTitle: string;
  dateTitle: string;
  subTitle: string;
  icon: string;
  active: number = 0;
  term: string = '';
  showSearch: boolean = false;

  displayedColumns: string[] = ['name', 'date'];

  constructor(
    private route: ActivatedRoute,
    private carerecordService: CareRecordService,
    private log: LoggerService,
    private menuProvider: AppMenuService) {}

  ngOnInit() {
    this.route.data.subscribe(
      (data) => this.eventType = data.eventType
    );

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
      this.showSearch = true;
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

    this.carerecordService.getObservation(this.page, this.size, this.precisComponentReference.patientId, this.eventType, this.active, this.term)
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
    this.events = events;
    this.dataSource = new MatTableDataSource(events.results);

    if (this.eventType==8) {
      this.menuProvider.setMenuBadge(5, this.events.length.toString());
    }
  }

  onPage(event: PageEvent) {
    this.page = event.pageIndex;
    this.size = event.pageSize;
    this.loadEvents();
  }
}

