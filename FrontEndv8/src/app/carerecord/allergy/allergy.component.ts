import {Component, ViewChild} from '@angular/core';
import {MatTableDataSource} from '@angular/material';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {PageEvent} from '@angular/material/paginator';
import {PrecisComponent} from "../precis/precis.component";
import {ActivatedRoute} from "@angular/router";
import {animate, state, style, transition, trigger} from "@angular/animations";

@Component({
  selector: 'app-allergy',
  templateUrl: './allergy.component.html',
  styleUrls: ['./allergy.component.scss'],
  animations: [
    trigger('detailExpand', [
      state('collapsed', style({height: '0px', minHeight: '0'})),
      state('expanded', style({height: '*'})),
      transition('expanded <=> collapsed', animate('225ms cubic-bezier(0.4, 0.0, 0.2, 1)')),
    ]),
  ]
})
export class AllergyComponent {
  // @ts-ignore
  @ViewChild(PrecisComponent) precisComponentReference;
  expandedElement: AllergyComponent | null;

  events: any;
  dataSource: MatTableDataSource<any>;
  page: number = 0;
  size: number = 12;
  eventTypeTitle: string;
  subTitle: string;
  icon: string;

  displayedColumns: string[] = ['name', 'date' , 'expandArrow'];

  constructor(
    private route: ActivatedRoute,
    private carerecordService: CareRecordService,
    private log: LoggerService
    ) { }

  loadEvents() {
    this.eventTypeTitle = "Allergies & intolerances";
    this.subTitle = "The clinical assessment of the selected patient's allergies or intolerances.";
    this.icon = 'warning';

    this.events = null;
    console.log("page: "+this.page+", size: "+this.size);
    this.carerecordService.getAllergy(this.page, this.size, this.precisComponentReference.patientId)
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
