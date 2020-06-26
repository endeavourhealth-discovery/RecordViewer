import {Component, Inject, OnInit} from '@angular/core';
import {MatDialogRef, MAT_DIALOG_DATA} from '@angular/material/dialog';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {FormControl} from '@angular/forms';

export interface DialogData {
  patientId: string;
  codeId: string;
  term: string;
}

@Component({
  selector: 'app-trend',
  templateUrl: './trend.component.html',
  styleUrls: ['./trend.component.scss']
})

export class TrendComponent {

  view: any[] = [1300, 500];
  chartResults: any[];
  dateFrom: string = '1900-01-01';
  dateTo: string = this.formatDate(new Date());
  showLineCharts: boolean = true;
  results = new FormControl();
  resultList: string[] = [''];
  selected: string = '';

  // options
  legend: boolean = true;
  legendPosition: string = 'right';
  animations: boolean = true;
  xAxis: boolean = true;
  yAxis: boolean = true;
  showYAxisLabel: boolean = true;
  showXAxisLabel: boolean = true;
  xAxisLabel: string = 'Date';
  yAxisLabel: string = 'Value';
  timeline: boolean = false;
  showGridLines: boolean = true;
  showAreaChart: boolean = true;
  gradient: boolean = true;
  logarithmic: boolean = true;

  patientId: string;
  codeId: string;
  term: string;

  colorScheme = {
    domain: ['#5aa454', '#e44d25', '#cfc0bb', '#7aa3e5', '#a8385d', '#aae3f5']
  };

  constructor(
    public dialogRef: MatDialogRef<TrendComponent>,
    private carerecordService: CareRecordService,
    private log: LoggerService,
    @Inject(MAT_DIALOG_DATA) public data: DialogData) {

    this.patientId = data.patientId;
    this.codeId = data.codeId;
    this.term = data.term;
  }

  ngOnInit() {
    this.showLineCharts = true;



    this.refresh();
  }

  refresh() {

    let values = "";

    this.resultList = this.term.split(',');

    if (this.selected == "") {
      values = this.term;
    }
    else {
      values = this.selected;
    }

    this.carerecordService.getDashboard(this.codeId, this.patientId, this.formatDate(this.dateFrom), this.formatDate(this.dateTo), values)
      .subscribe(result => {

        this.chartResults = result.results;

        // apply log10 to values in series
        this.chartResults = this.chartResults.map(
          e => {
            return {
              name: e.name,
              series: e.series.map(
                v => {
                  return {
                    name: new Date(v.name),
                    value: this.applyLogarithm(v.value)
                  }
                }
              )
            }
          }
        )
      });
  }

  // apply pow10 to yAxis tick values and tooltip value
  getMathPower(val: number) {
    if (this.logarithmic == true) {
      return Math.round((Math.pow(10, val) + Number.EPSILON) * 100) / 100;
    }
    else {
      return val;
    }
  }

  getYMathPower(val: number) {
    if (this.logarithmic == true) {
      return Math.round(Math.pow(10, val));
    }
    else {
      return val;
    }
  }

  applyLogarithm(value: number) {
    if (this.logarithmic == true) {
      return Math.log10(value)
    }
    else  {
      return value
    }
  }

  dateTickFormatting(val: any): String {
    return new Date(val).toLocaleDateString();
  }

  onSelect(data): void {
    console.log('Item clicked', JSON.parse(JSON.stringify(data)));
  }

  formatDate(date) {
    var d = new Date(date),
      month = '' + (d.getMonth() + 1),
      day = '' + d.getDate(),
      year = d.getFullYear();

    if (month.length < 2)
      month = '0' + month;
    if (day.length < 2)
      day = '0' + day;

    return [year, month, day].join('-');
  }

  onCancelClick(): void {
    this.dialogRef.close();
  }
}
