import {Component, ViewChild} from '@angular/core';
import {MatTableDataSource} from '@angular/material';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {PageEvent} from '@angular/material/paginator';
import {PrecisComponent} from "../precis/precis.component";
import {TrendComponent} from "../trend/trend.component";
import {MatDialog} from "@angular/material/dialog";
import {SelectionModel} from '@angular/cdk/collections';

@Component({
  selector: 'app-diagnostics',
  templateUrl: './diagnostics.component.html',
  styleUrls: ['./diagnostics.component.scss']
})
export class DiagnosticsComponent {
  // @ts-ignore
  @ViewChild(PrecisComponent) precisComponentReference;

  selection = new SelectionModel<any>(true, []);

  events: any;
  dataSource: MatTableDataSource<any>;
  page: number = 0;
  size: number = 12;
  term: string = '';

  displayedColumns: string[] = ["select", "term", "result", "date"];

  constructor(
    private carerecordService: CareRecordService,
    private dialog: MatDialog,
    private log: LoggerService
    ) { }

  loadEvents() {
    this.selection.clear();

    this.events = null;

    this.carerecordService.getDiagnostics(this.page, this.size, this.precisComponentReference.patientId, this.term)
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
      height: '850px',
      width: '1600px',
      data: {patientId: this.precisComponentReference.patientId, term: term}
    });

    dialogRef.afterClosed().subscribe(result => {

    });
  }

  isAllSelected() {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.data.length;
    return numSelected === numRows;
  }

  masterToggle() {
    this.isAllSelected() ?
      this.selection.clear() :
      this.dataSource.data.forEach(row => this.selection.select(row));
  }

  checkboxLabel(row?: any): string {
    if (!row) {
      return `${this.isAllSelected() ? 'select' : 'deselect'} all`;
    }
    return `${this.selection.isSelected(row) ? 'deselect' : 'select'} row ${row.position + 1}`;
  }

}
