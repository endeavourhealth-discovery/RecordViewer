import {AfterViewInit, Component, OnInit, ViewChild} from '@angular/core';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {PrecisComponent} from "../precis/precis.component";
import {MatTableDataSource} from "@angular/material/table";

@Component({
  selector: 'app-fhir',
  templateUrl: './fhir.component.html',
  styleUrls: ['./fhir.component.scss']
})
export class FhirComponent implements OnInit {
  // @ts-ignore
  @ViewChild(PrecisComponent) precisComponentReference;

  patientId: number;
  params : string;
  url : string;
  status : string;
  fhir: any;

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

      this.loadFhir();
    });
  }

  loadFhir() {
    this.carerecordService.getFhir(this.patientId)
      .subscribe(
        (result) => this.displayFhir(result),
        (error) => this.log.error(error)
      );
  }

  displayFhir(fhir: any) {
    this.fhir = JSON.stringify(fhir, null, 2);

    this.params = '{"id":'+this.patientId+', "resources" : [{"type" : "patient"}, {"type" : "medication"}]}';
    this.url = "POST: https://rqy8l5vl8l.execute-api.eu-west-2.amazonaws.com/discovery-fhir-api-dev/getrecord";

    this.status = "200 (OK)";
  }


}
