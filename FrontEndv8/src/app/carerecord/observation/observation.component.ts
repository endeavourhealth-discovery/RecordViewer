import {Component, OnInit, ViewChild} from '@angular/core';
import {MatTableDataSource} from '@angular/material';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {MatPaginator, PageEvent} from '@angular/material/paginator';
import {PrecisComponent} from "../precis/precis.component";
import {ActivatedRoute} from "@angular/router";
import {animate, state, style, transition, trigger} from "@angular/animations";
import {MatSort} from "@angular/material/sort";

@Component({
  selector: 'app-observation',
  templateUrl: './observation.component.html',
  styleUrls: ['./observation.component.scss'],
  animations: [
    trigger('detailExpand', [
      state('collapsed', style({height: '0px', minHeight: '0'})),
      state('expanded', style({height: '*'})),
      transition('expanded <=> collapsed', animate('225ms cubic-bezier(0.4, 0.0, 0.2, 1)')),
    ]),
  ]
})
export class ObservationComponent implements OnInit {
  // @ts-ignore
  @ViewChild(PrecisComponent) precisComponentReference;
  expandedElement: ObservationComponent | null;

  events: any;
  dataSource: MatTableDataSource<any>;
  eventType: number;
  eventTypeTitle: string;
  dateTitle: string;
  subTitle: string;
  icon: string;
  active: number = 0;
  term: string = '';
  showSearch: boolean = false;
  summaryMode: number = 0;

  conditions: boolean = false;
  observations: boolean = false;
  procedures: boolean = false;
  familyHistory: boolean = false;
  immunisations: boolean = false;
  procedureRequests: boolean = false;
  diagnosticOrders: boolean = false;
  warningsFlags: boolean = false;

  @ViewChild(MatPaginator, {static: false}) paginator: MatPaginator;
  @ViewChild(MatSort, {static: false}) sort: MatSort;
  displayedColumns: string[] = ['name', 'date', 'expandArrow'];

  constructor(
    private route: ActivatedRoute,
    private carerecordService: CareRecordService,
    private log: LoggerService) {}

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
      this.displayedColumns = ['name', 'status', 'date', 'expandArrow'];
      this.conditions = true;
    }
    else if (this.eventType==2) {
      this.eventTypeTitle = "Observations";
      this.subTitle = "Measurements and simple assertions made about the selected patient";
      this.icon = 'fa-monitor-heart-rate';
      this.dateTitle = "Effective date";
      this.showSearch = true;
      this.displayedColumns = ['name', 'category', 'date', 'expandArrow'];
      this.observations = true;
    }
    else if (this.eventType==3) {
      this.eventTypeTitle = "Procedures";
      this.subTitle = "Details of procedures performed on the selected patient";
      this.icon = 'fa-procedures';
      this.dateTitle = "Effective date";
      this.procedures = true;
    }
    else if (this.eventType==4) {
      this.eventTypeTitle = "Family history";
      this.subTitle = "Significant health conditions for a person related to the patient relevant in the context of care for the selected patient";
      this.icon = 'fa-user-friends';
      this.dateTitle = "Effective date";
      this.familyHistory = true;
    }
    else if (this.eventType==5) {
      this.eventTypeTitle = "Immunisations";
      this.subTitle = "Current and historical administration of vaccines";
      this.icon = 'fa-syringe';
      this.dateTitle = "Effective date";
      this.immunisations = true;
    }
    else if (this.eventType==6) {
      this.eventTypeTitle = "Procedure requests";
      this.subTitle = "A record of a request for a procedure to be performed";
      this.icon = 'fa-user-md-chat';
      this.dateTitle = "Effective date";
      this.displayedColumns = ['name','status', 'date', 'expandArrow'];
      this.procedureRequests = true;
    }
    else if (this.eventType==7) {
      this.eventTypeTitle = "Diagnostic orders";
      this.subTitle = "A record of a request for a diagnostic investigation service to be performed";
      this.icon = 'fa-microscope';
      this.dateTitle = "Effective date";
      this.diagnosticOrders = true;
    }
    else if (this.eventType==8) {
      this.eventTypeTitle = "Warnings & flags";
      this.subTitle = "Prospective warnings of potential issues when providing care to the selected patient";
      this.icon = 'fa-exclamation-triangle';
      this.dateTitle = "Effective date";
      this.displayedColumns = ['name','status', 'date', 'expandArrow'];
      this.warningsFlags = true;
    }

    this.events = null;

    this.carerecordService.getObservation(this.precisComponentReference.nhsNumber, this.eventType, this.active, this.term, this.summaryMode)
      .subscribe(
        (result) => this.displayEvents(result),
        (error) => this.log.error(error)
      );
  }

  displayEvents(events: any) {
    this.events = events;
    this.dataSource = new MatTableDataSource(events.results);
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  applyFilter(event: Event) {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

}
