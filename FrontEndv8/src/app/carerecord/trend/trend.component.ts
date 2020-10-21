import {Component, Inject, OnInit} from '@angular/core';
import {MatDialogRef, MAT_DIALOG_DATA} from '@angular/material/dialog';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {FormControl} from '@angular/forms';

export interface DialogData {
  patientId: string;
  term: string;
}

@Component({
  selector: 'app-trend',
  templateUrl: './trend.component.html',
  styleUrls: ['./trend.component.scss']
})

export class TrendComponent {
  view: any[] = [1300, 550];
  chartResults: any[];
  dateFrom: string = '1900-01-01';
  dateTo: string = this.formatDate(new Date());
  showLineCharts: boolean = true;
  results = new FormControl();
  resultList: string[] = [''];
  selected: string = '';
  months: string[] = [''];

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
  showAreaChart: boolean = false;
  gradient: boolean = true;
  logarithmic: boolean = true;

  patientId: string;
  term: string;

  colorScheme = {
    domain: ["#3366cc","#dc3912","#ff9900","#109618","#990099","#0099c6","#dd4477","#66aa00","#b82e2e","#316395","#3366cc",
      "#994499","#22aa99","#aaaa11","#6633cc","#e67300","#8b0707","#651067","#329262",
      "#5574a6","#3b3eac","#b77322","#16d620","#b91383","#f4359e","#9c5935","#a9c413","#2a778d","#668d1c","#bea413","#0c5922","#743411"]
  };

  constructor(
    public dialogRef: MatDialogRef<TrendComponent>,
    private carerecordService: CareRecordService,
    private log: LoggerService,
    @Inject(MAT_DIALOG_DATA) public data: DialogData) {

    this.patientId = data.patientId;
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
      values = this.resultList.toString();
    }
    else {
      values = this.selected;
    }

    this.carerecordService.getDashboard(this.patientId, this.formatDate(this.dateFrom), this.formatDate(this.dateTo), values)
      .subscribe(result => {

        this.chartResults = result.results;

        console.log(this.chartResults);

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

  formatTooltipYAxis(val: number) {
    if (this.logarithmic == true) {
      val = Math.round((Math.pow(10, val) + Number.EPSILON) * 100) / 100;
      return val.toLocaleString()
    }
    else {
      return Number(val).toLocaleString()
    }
  }

  formatYAxis(val: number) {
    if (val<5) {
      val = Math.round(Math.pow(10, val));
      return val.toLocaleString()
    }
    else {
      return Number(val).toLocaleString()
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

  formatXAxis(val: any): String {
    this.months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    var month = (val.toLocaleString()).substring(3,5);
    var monthName = this.months[(Number(month)-1)];
    var day = (val.toLocaleString()).substring(0,2);
    var year = (val.toLocaleString()).substring(6,10);
    val = (day + " " + monthName + " " + year);

    return val.toLocaleString();
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
