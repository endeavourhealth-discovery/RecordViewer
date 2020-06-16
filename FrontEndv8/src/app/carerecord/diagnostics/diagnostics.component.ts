import {AfterViewInit, Component, OnInit, ViewChild} from '@angular/core';
import {MatTableDataSource} from '@angular/material';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {PageEvent} from '@angular/material/paginator';
import {PrecisComponent} from "../precis/precis.component";
import {TrendComponent} from "../trend/trend.component";
import {MatDialog} from "@angular/material/dialog";

@Component({
  selector: 'app-diagnostics',
  templateUrl: './diagnostics.component.html',
  styleUrls: ['./diagnostics.component.scss']
})
export class DiagnosticsComponent implements OnInit, AfterViewInit {
  // @ts-ignore
  @ViewChild(PrecisComponent) precisComponentReference;

  events: any;
  dataSource: MatTableDataSource<any>;
  page: number = 0;
  size: number = 12;
  patientId: number;

  displayedColumns: string[] = ["term", "result", "date"];

  ngAfterViewInit(): void {
    this.patientId = this.precisComponentReference.patientId;
  }

  constructor(
    private carerecordService: CareRecordService,
    private dialog: MatDialog,
    private log: LoggerService
    ) { }

  ngOnInit() {
    this.precisComponentReference.patientChange.subscribe(patientId => {
      console.log("patient changed to "+patientId);
      this.patientId = patientId;
      this.loadEvents();
    });
  }

  loadEvents() {
    this.events = null;
    console.log("page: "+this.page+", size: "+this.size);
    this.carerecordService.getDiagnostics(this.page, this.size, this.patientId)
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

  showTrend(code_id: string, term: string) {
    const dialogRef = this.dialog.open(TrendComponent, {
      height: '850px',
      width: '1600px',
      data: {patientId: this.patientId, codeId: code_id, term: term}
    });

    dialogRef.afterClosed().subscribe(result => {

    });
  }

}
