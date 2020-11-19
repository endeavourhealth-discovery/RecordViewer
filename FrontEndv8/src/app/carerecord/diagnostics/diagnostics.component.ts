import {Component, ViewChild} from '@angular/core';
import {MatTableDataSource} from '@angular/material';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {MatPaginator, PageEvent} from '@angular/material/paginator';
import {PrecisComponent} from "../precis/precis.component";
import {TrendComponent} from "../trend/trend.component";
import {MatDialog} from "@angular/material/dialog";
import {SelectionModel} from '@angular/cdk/collections';
import {animate, state, style, transition, trigger} from "@angular/animations";
import {MatSort} from "@angular/material/sort";

@Component({
  selector: 'app-diagnostics',
  templateUrl: './diagnostics.component.html',
  styleUrls: ['./diagnostics.component.scss'],
  animations: [
    trigger('detailExpand', [
      state('collapsed', style({height: '0px', minHeight: '0'})),
      state('expanded', style({height: '*'})),
      transition('expanded <=> collapsed', animate('225ms cubic-bezier(0.4, 0.0, 0.2, 1)')),
    ]),
  ]
})

export class DiagnosticsComponent {
  // @ts-ignore
  @ViewChild(PrecisComponent) precisComponentReference;
  expandedElement: DiagnosticsComponent | null;
  selection = new SelectionModel<any>(true, []);
  summaryMode: number = 0;

  events: any;
  dataSource: MatTableDataSource<any>;
  term: string = '';

  @ViewChild(MatPaginator, {static: false}) paginator: MatPaginator;
  @ViewChild(MatSort, {static: false}) sort: MatSort;
  displayedColumns: string[] = ["select", "battery", "term", "result", "date", "expandArrow"];

  constructor(
    private carerecordService: CareRecordService,
    private dialog: MatDialog,
    private log: LoggerService) {}

  loadEvents() {
    this.events = null;
    this.carerecordService.getDiagnostics(this.precisComponentReference.patientId, this.term, this.summaryMode)
      .subscribe(
        (result) => this.displayEvents(result),
        (error) => this.log.error(error)
      );

    this.selection.clear();
  }

  displayEvents(events: any) {
    this.events = events;

    let batteryList = [];
    let prevFolder = '';
    let thisFolder = '';
    events.results.forEach( (item, index) => {
      thisFolder = events.results[index].battery;
      if (thisFolder==prevFolder) {
        events.results[index].battery = '↳';
      }
      batteryList.push(events.results[index]);
      if (events.results[index].battery!='↳')
        prevFolder = events.results[index].battery;
    });

    this.dataSource = new MatTableDataSource(batteryList);
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  showTrend(term: string) {
    var terms = "";

    for (let s of this.selection.selected) {
      terms = terms + s.term + ",";
    }

    if (terms != "")
    {
      term = terms.replace(/,\s*$/, "");
    }

    const dialogRef = this.dialog.open(TrendComponent, {
      height: '820px',
      width: '1600px',
      data: {patientId: this.precisComponentReference.patientId, term: term}
    });

    dialogRef.afterClosed().subscribe(result => {

    });
  }

  checkboxLabel(row?: any): string {
    return `${this.selection.isSelected(row) ? 'deselect' : 'select'} row ${row.position + 1}`;
  }

  applyFilter(event: Event) {
    const filterValue = (event.target as HTMLInputElement).value;
    this.dataSource.filter = filterValue.trim().toLowerCase();

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }
}
