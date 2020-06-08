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
  size: number = 15;
  patientId: number;
  eventType: number;
  eventTypeTitle: string;
  subTitle: string;
  icon: string;

  displayedColumns: string[] = ['name','status', 'date'];

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
      this.subTitle = "conditions (problems)";
      this.icon = 'fa-diagnoses';
    }
    else if (this.eventType==2) {
      this.eventTypeTitle = "Observations";
      this.subTitle = "general clinical observations";
      this.icon = 'fa-monitor-heart-rate';
    }
    else if (this.eventType==3) {
      this.eventTypeTitle = "Procedures";
      this.subTitle = "procedures";
      this.icon = 'fa-procedures';
    }
    else if (this.eventType==4) {
      this.eventTypeTitle = "Family history";
      this.subTitle = "family history";
      this.icon = 'fa-user-friends';
    }
    else if (this.eventType==5) {
      this.eventTypeTitle = "Immunisations";
      this.subTitle = "immunisations";
      this.icon = 'fa-syringe';
    }
    else if (this.eventType==5) {
      this.eventTypeTitle = "Immunisations";
      this.subTitle = "immunisations";
      this.icon = 'fa-syringe';
    }
    else if (this.eventType==6) {
      this.eventTypeTitle = "Procedure Requests";
      this.subTitle = "procedure requests";
      this.icon = 'fa-user-md-chat';
    }

    this.events = null;
    console.log("page: "+this.page+", size: "+this.size);
    this.carerecordService.getObservation(this.page, this.size, this.patientId, this.eventType)
      .subscribe(
        (result) => this.displayEvents(result),
        (error) => this.log.error(error)
      );
  }

  displayEvents(events: any) {
    this.events = events;
    this.dataSource = new MatTableDataSource(events.results);
  }

  onPage(event: PageEvent) {
    this.page = event.pageIndex;
    this.size = event.pageSize;
    this.loadEvents();
  }

}
