import {Component, Inject, OnInit} from '@angular/core';
import {MatDialogRef, MAT_DIALOG_DATA} from '@angular/material/dialog';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';

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
  timeline: boolean = true;
  showGridLines: boolean = true;
  showAreaChart: boolean = true;
  gradient: boolean = true;
  logarithmic: boolean = false;

  patientId: string;
  codeId: string;
  term: string;

  colorScheme = {
    domain: ['#5AA454', '#E44D25', '#CFC0BB', '#7aa3e5', '#a8385d', '#aae3f5']
  };

  constructor(
    public dialogRef: MatDialogRef<TrendComponent>,
    private carerecordService: CareRecordService,
    private log: LoggerService,
    @Inject(MAT_DIALOG_DATA) public data: DialogData) {

    this.patientId = data.patientId;
    this.codeId = data.codeId;
    this.term = data.term;

    let count = (this.codeId.match(/,/g) || []).length;
    console.log(count);

    if (count > 1) {
      this.logarithmic = true;
    }

  }

  ngOnInit() {
    this.showLineCharts = true;

    this.refresh();
  }

  refresh() {
    this.carerecordService.getDashboard(this.codeId, this.patientId, this.formatDate(this.dateFrom), this.formatDate(this.dateTo), this.term)
      .subscribe(result => {

        this.chartResults = result.results;

        if (this.logarithmic) {
          // apply log10 to values in series
          this.chartResults = this.chartResults.map(
            e => {
              return {
                name: e.name,
                series: e.series.map(
                  v => {
                    return {
                      name: v.name,
                      value: Math.log10(v.value)
                    }
                  }
                )
              }
            }
          )
        }
      });
  }

  // apply pow10 to yAxis tick values and tootip value
  getMathPower(val: number){
    console.log("log1:"+this.logarithmic);
    if (this.logarithmic)
      return Math.round(Math.pow(10,val));
    else
      return val;
  }

  dateTickFormatting(val: any): String {
    console.log("log2:"+this.logarithmic);
    return new Date(val).toLocaleDateString();
  }

  yFormatting(val: any): String {
    let r = val;

    console.log("log3:"+this.logarithmic);
    if (this.logarithmic)
      r = "";

    return r;
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
