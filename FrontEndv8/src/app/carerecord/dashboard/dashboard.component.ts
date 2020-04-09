import {AfterViewInit, Component, OnInit, ViewChild} from '@angular/core';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {PrecisComponent} from "../precis/precis.component";
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { single } from './data';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent {
  // @ts-ignore
  single: any[]; multi: any[];
  view: any[] = [1200, 600];

  // options
  showXAxis = true;
  showYAxis = true;
  gradient = false;
  showLegend = true;
  showXAxisLabel = true;
  xAxisLabel = 'CCG';
  showYAxisLabel = true;
  yAxisLabel = 'Incidence of COVID-19';

  colorScheme = {
    domain: ['#89ddb5', '#829cc7', '#dd99e5', '#3ad2f2','#e2ee80', '#72c9b8', '#dde7f9']
  };

  constructor(
    private carerecordService: CareRecordService,
    private log: LoggerService
  ) {
    Object.assign(this, { single })
  }

  onSelect(event) {
    console.log(event);
  }

}
