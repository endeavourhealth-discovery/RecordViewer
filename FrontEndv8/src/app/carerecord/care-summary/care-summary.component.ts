import {AfterViewInit, Component, OnInit, ViewChild} from '@angular/core';
import {MatTableDataSource} from '@angular/material';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {PageEvent} from '@angular/material/paginator';
import {PrecisComponent} from "../precis/precis.component";
import {TrendComponent} from "../trend/trend.component";
import {MatDialog} from "@angular/material/dialog";
import {SelectionModel} from '@angular/cdk/collections';

@Component({
  selector: 'app-care-summary',
  templateUrl: './care-summary.component.html',
  styleUrls: ['./care-summary.component.scss']
})
export class CareSummaryComponent implements OnInit {
  // @ts-ignore
  @ViewChild(PrecisComponent) precisComponentReference;
  selection = new SelectionModel<any>(true, []);

  // medication
  events1: any;
  dataSource1: MatTableDataSource<any>;
  page1: number = 0;
  size1: number = 999;
  active1: number = 1;
  displayedColumns1: string[] = ['name','type','last'];

  // conditions
  events2: any;
  dataSource2: MatTableDataSource<any>;
  page2: number = 0;
  size2: number = 999;
  active2: number = 1;
  displayedColumns2: string[] = ['name', 'date'];
  term2: string = '';

  // allergies
  events3: any;
  dataSource3: MatTableDataSource<any>;
  page3: number = 0;
  size3: number = 999;
  displayedColumns3: string[] = ['name','date'];

  // warnings
  events4: any;
  dataSource4: MatTableDataSource<any>;
  page4: number = 0;
  size4: number = 999;
  active4: number = 1;
  displayedColumns4: string[] = ['name','date'];
  term4: string = '';

  // encounters
  events5: any;
  dataSource5: MatTableDataSource<any>;
  page5: number = 0;
  size5: number = 5;
  displayedColumns5: string[] = ['type', 'location', 'practitioner','date'];

  // health status
  events6: any;
  dataSource6: MatTableDataSource<any>;
  page6: number = 0;
  size6: number = 10;
  displayedColumns6: string[] = ['select', 'term', 'result', 'date'];
  patientId: number;

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

      this.loadConditions();
      this.loadAllergies();
      this.loadMedication();
      this.loadWarnings();
      this.loadEncounters();
      this.loadDiagnostics();
    });
  }

  loadMedication() {
    this.events1 = null;
    this.carerecordService.getMedication(this.page1, this.size1, this.patientId, this.active1)
      .subscribe(
        (result) => this.displayMedication(result),
        (error) => this.log.error(error)
      );
  }

  loadAllergies() {
    this.events3 = null;
    this.carerecordService.getAllergy(this.page3, this.size3, this.patientId)
      .subscribe(
        (result) => this.displayAllergies(result),
        (error) => this.log.error(error)
      );
  }

  loadConditions() {
    this.events2 = null;
    this.carerecordService.getObservation(this.page2, this.size2, this.patientId, 1, this.active2, this.term2)
      .subscribe(
        (result) => this.displayConditions(result),
        (error) => this.log.error(error)
      );
  }

  loadWarnings() {
    this.events4 = null;
    this.carerecordService.getObservation(this.page4, this.size4, this.patientId, 8, this.active4, this.term4)
      .subscribe(
        (result) => this.displayWarnings(result),
        (error) => this.log.error(error)
      );
  }

  loadEncounters() {
    this.events5 = null;
    this.carerecordService.getEncounters(this.page5, this.size5, this.patientId)
      .subscribe(
        (result) => this.displayEncounters(result),
        (error) => this.log.error(error)
      );
  }

  loadDiagnostics() {
    this.events6 = null;
    this.carerecordService.getDiagnostics(this.page6, this.size6, this.patientId)
      .subscribe(
        (result) => this.displayDiagnostics(result),
        (error) => this.log.error(error)
      );
  }

  displayMedication(events: any) {
    this.events1 = events;
    this.dataSource1 = new MatTableDataSource(events.results);
  }

  displayConditions(events: any) {
    this.events2 = events;
    this.dataSource2 = new MatTableDataSource(events.results);
  }

  displayAllergies(events: any) {
    this.events3 = events;
    this.dataSource3 = new MatTableDataSource(events.results);
  }

  displayWarnings(events: any) {
    this.events4 = events;
    this.dataSource4 = new MatTableDataSource(events.results);
  }

  displayEncounters(events: any) {
    this.events5 = events;
    this.dataSource5 = new MatTableDataSource(events.results);
  }

  displayDiagnostics(events: any) {
    this.events6 = events;
    this.dataSource6 = new MatTableDataSource(events.results);
  }

  onPage1(event: PageEvent) {
    this.page1 = event.pageIndex;
    this.size1 = event.pageSize;
    this.loadMedication();
  }

  onPage2(event: PageEvent) {
    this.page2 = event.pageIndex;
    this.size2 = event.pageSize;
    this.loadConditions();
  }

  onPage3(event: PageEvent) {
    this.page3 = event.pageIndex;
    this.size3 = event.pageSize;
    this.loadAllergies();
  }

  onPage4(event: PageEvent) {
    this.page4 = event.pageIndex;
    this.size4 = event.pageSize;
    this.loadWarnings();
  }

  onPage5(event: PageEvent) {
    this.page5 = event.pageIndex;
    this.size5 = event.pageSize;
    this.loadEncounters();
  }

  onPage6(event: PageEvent) {
    this.page6 = event.pageIndex;
    this.size6 = event.pageSize;
    this.loadDiagnostics();
  }

  showTrend(code_id: string, term: string) {

    var terms = "";
    var codeIds = "";
    for (let s of this.selection.selected) {
      console.log(s);
      terms = terms + s.term + ",";
      codeIds = codeIds + s.codeId + ",";
    }
    console.log(terms);
    if (terms != "")
    {
      code_id = codeIds;
      term = terms;
    }

    const dialogRef = this.dialog.open(TrendComponent, {
      height: '850px',
      width: '1600px',
      data: {patientId: this.patientId, codeId: code_id, term: term}
    });

    dialogRef.afterClosed().subscribe(result => {

    });
  }

  isAllSelected() {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource6.data.length;
    return numSelected === numRows;
  }

  masterToggle() {
    this.isAllSelected() ?
      this.selection.clear() :
      this.dataSource6.data.forEach(row => this.selection.select(row));
  }

  checkboxLabel(row?: any): string {
    if (!row) {
      return `${this.isAllSelected() ? 'select' : 'deselect'} all`;
    }
    return `${this.selection.isSelected(row) ? 'deselect' : 'select'} row ${row.position + 1}`;
  }

}
