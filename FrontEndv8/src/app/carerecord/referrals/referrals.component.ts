import {Component, ViewChild} from '@angular/core';
import {MatTableDataSource} from '@angular/material';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {PageEvent} from '@angular/material/paginator';
import {PrecisComponent} from "../precis/precis.component";
import {animate, state, style, transition, trigger} from "@angular/animations";

@Component({
  selector: 'app-referrals',
  templateUrl: './referrals.component.html',
  styleUrls: ['./referrals.component.scss'],
  animations: [
    trigger('detailExpand', [
      state('collapsed', style({height: '0px', minHeight: '0'})),
      state('expanded', style({height: '*'})),
      transition('expanded <=> collapsed', animate('225ms cubic-bezier(0.4, 0.0, 0.2, 1)')),
    ]),
  ]
})
export class ReferralsComponent {
  // @ts-ignore
  @ViewChild(PrecisComponent) precisComponentReference;
  expandedElement: ReferralsComponent | null;

  events: any;
  dataSource: MatTableDataSource<any>;
  page: number = 0;
  size: number = 12;

  displayedColumns: string[] = ["recipient",  "priority",  "type", "mode", "speciality", "date", "expandArrow"];

  constructor(
    private carerecordService: CareRecordService,
    private log: LoggerService
    ) { }

  loadEvents() {
    this.events = null;
    console.log("page: "+this.page+", size: "+this.size);
    this.carerecordService.getReferrals(this.page, this.size, this.precisComponentReference.patientId)
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
