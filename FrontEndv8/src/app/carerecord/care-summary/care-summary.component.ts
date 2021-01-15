import {Component, OnInit, ViewChild} from '@angular/core';
import {MatTableDataSource} from '@angular/material';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {PageEvent} from '@angular/material/paginator';
import {PrecisComponent} from "../precis/precis.component";
import {TrendComponent} from "../trend/trend.component";
import {MatDialog} from "@angular/material/dialog";
import {SelectionModel} from '@angular/cdk/collections';
import {ActivatedRoute} from "@angular/router";
import {Globals} from "../globals";
import {animate, state, style, transition, trigger} from "@angular/animations";

@Component({
  selector: 'app-care-summary',
  templateUrl: './care-summary.component.html',
  styleUrls: ['./care-summary.component.scss'],
  animations: [
    trigger('detailExpand', [
      state('collapsed', style({height: '0px', minHeight: '0'})),
      state('expanded', style({height: '*'})),
      transition('expanded <=> collapsed', animate('225ms cubic-bezier(0.4, 0.0, 0.2, 1)')),
    ]),
  ]
})

export class CareSummaryComponent implements OnInit {
  // @ts-ignore
  @ViewChild(PrecisComponent) precisComponentReference;
  selection = new SelectionModel<any>(true, []);
  expandedElement: CareSummaryComponent | null;

  globals: Globals;
  summaryMode: number = 1;

  // medication
  events1: any;
  dataSource1: MatTableDataSource<any>;
  active1: number = 1;
  displayedColumns1: string[] = ['name', 'expandArrow'];

  // conditions
  events2: any;
  dataSource2: MatTableDataSource<any>;
  active2: number = 1;
  displayedColumns2: string[] = ['name', 'expandArrow'];
  term2: string = '';

  // allergies
  events3: any;
  dataSource3: MatTableDataSource<any>;
  displayedColumns3: string[] = ['name','expandArrow'];

  // warnings
  events4: any;
  dataSource4: MatTableDataSource<any>;
  active4: number = 1;
  displayedColumns4: string[] = ['name','expandArrow'];
  term4: string = '';

  // encounters
  events5: any;
  dataSource5: MatTableDataSource<any>;
  displayedColumns5: string[] = ['type', 'location', 'date', 'expandArrow'];

  // health status
  events6: any;
  dataSource6: MatTableDataSource<any>;
  displayedColumns6: string[] = ['select', 'term', 'result', 'date', 'expandArrow'];
  term6: string = '';
  diagnostics: boolean = true;

  constructor(
    private carerecordService: CareRecordService,
    private route: ActivatedRoute,
    private dialog: MatDialog,
    private log: LoggerService,
    globals: Globals) {
      this.globals = globals;
  }

  ngOnInit() {
    this.route.queryParams
      .subscribe(params => {
          this.globals.nhsNumber = params['nhsNumber'] || this.globals.nhsNumber;
      });
  }

  loadEvents() {
      this.selection.clear();

      this.loadConditions();
      this.loadAllergies();
      this.loadMedication();
      this.loadWarnings();
      this.loadEncounters();
      this.loadDiagnostics();
  }

  loadMedication() {
    this.events1 = null;
    this.carerecordService.getMedication(this.precisComponentReference.nhsNumber, this.active1, this.summaryMode)
      .subscribe(
        (result) => this.displayMedication(result),
        (error) => this.log.error(error)
      );
  }

  loadAllergies() {
    this.events3 = null;
    this.carerecordService.getAllergy(this.precisComponentReference.nhsNumber, this.summaryMode)
      .subscribe(
        (result) => this.displayAllergies(result),
        (error) => this.log.error(error)
      );
  }

  loadConditions() {
    this.events2 = null;
    this.carerecordService.getObservation(this.precisComponentReference.nhsNumber, 1, this.active2, this.term2, this.summaryMode)
      .subscribe(
        (result) => this.displayConditions(result),
        (error) => this.log.error(error)
      );
  }

  loadWarnings() {
    this.events4 = null;
    this.carerecordService.getObservation(this.precisComponentReference.nhsNumber, 8, this.active4, this.term4, this.summaryMode)
      .subscribe(
        (result) => this.displayWarnings(result),
        (error) => this.log.error(error)
      );
  }

  loadEncounters() {
    this.events5 = null;
    this.carerecordService.getEncounters(this.precisComponentReference.nhsNumber, this.summaryMode)
      .subscribe(
        (result) => this.displayEncounters(result),
        (error) => this.log.error(error)
      );
  }

  loadDiagnostics() {
    this.events6 = null;
    this.carerecordService.getDiagnostics(this.precisComponentReference.nhsNumber, this.term6, this.summaryMode)
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
    if (events.results == '') {
      this.diagnostics = false;
    }
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
      data: {nhsNumber: this.precisComponentReference.nhsNumber, term: term}
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
