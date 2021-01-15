import {Component, ViewChild} from '@angular/core';
import {MatTableDataSource} from '@angular/material';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {MatPaginator, PageEvent} from '@angular/material/paginator';
import {PrecisComponent} from "../precis/precis.component";
import {animate, state, style, transition, trigger} from "@angular/animations";
import {MatSort} from "@angular/material/sort";

@Component({
  selector: 'app-medication',
  templateUrl: './medication.component.html',
  styleUrls: ['./medication.component.scss'],
  animations: [
    trigger('detailExpand', [
      state('collapsed', style({height: '0px', minHeight: '0'})),
      state('expanded', style({height: '*'})),
      transition('expanded <=> collapsed', animate('225ms cubic-bezier(0.4, 0.0, 0.2, 1)')),
    ]),
  ]

})
export class MedicationComponent {
  // @ts-ignore
  @ViewChild(PrecisComponent) precisComponentReference;
  expandedElement: MedicationComponent | null;

  events: any;
  dataSource: MatTableDataSource<any>;
  active: number = 0;
  summaryMode: number = 0;
  @ViewChild(MatPaginator, {static: false}) paginator: MatPaginator;
  @ViewChild(MatSort, {static: false}) sort: MatSort;
  displayedColumns: string[] = ['name', 'dose', 'status', 'date', 'expandArrow'];

  constructor(
    private carerecordService: CareRecordService,
    private log: LoggerService
    ) {}

  loadEvents() {
    this.events = null;

    this.carerecordService.getMedication(this.precisComponentReference.nhsNumber, this.active, this.summaryMode)
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
