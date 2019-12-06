import {AfterViewInit, Component, OnInit, ViewChild} from '@angular/core';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {PrecisComponent} from "../precis/precis.component";

@Component({
  selector: 'app-values',
  templateUrl: './values.component.html',
  styleUrls: ['./values.component.scss']
})
export class ValuesComponent implements OnInit, AfterViewInit {
  // @ts-ignore
  @ViewChild(PrecisComponent) precisComponentReference;

  patientId: number = 0;

  id : string;
  title : string;
  name : string;
  gender : string;
  dob : string;
  dod : string;
  nhsNumber : string;
  telecom : string;
  adduse : string;
  address : string;
  postcode : string;
  city : string;
  otheraddresses : string;
  orglocation : string;
  startdate : string;

  imagePath = "assets/values.png";

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

    });
  }


}
