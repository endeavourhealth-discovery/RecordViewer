import {AfterViewInit, Component, OnInit, ViewChild} from '@angular/core';
import {MatTableDataSource} from '@angular/material';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {PageEvent} from '@angular/material/paginator';
import {PrecisComponent} from "../precis/precis.component";

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
  size: number = 15;
  patientId: number;

  displayedColumns: string[] = ["date", "term", "result"];

  ngAfterViewInit(): void {
    this.patientId = this.precisComponentReference.patientId;
  }

  constructor(
    private carerecordService: CareRecordService,
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

}
