import {Component, ViewChild} from '@angular/core';
import {CareRecordService} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {PrecisComponent} from "../precis/precis.component";
import {MatTableDataSource} from "@angular/material/table";

@Component({
  selector: 'app-fhir',
  templateUrl: './fhir.component.html',
  styleUrls: ['./fhir.component.scss']
})
export class FhirComponent {
  // @ts-ignore
  @ViewChild(PrecisComponent) precisComponentReference;

  params : string;
  url : string;
  status : string;
  fhir: any;

  constructor(
    private carerecordService: CareRecordService,
    private log: LoggerService
    ) { }

  loadFhir() {
    this.carerecordService.getFhir(this.precisComponentReference.patientId)
      .subscribe(
        (result) => this.displayFhir(result),
        (error) => this.log.error(error)
      );
  }

  loadFilteredFhir(requestParams:string) {
    this.carerecordService.getFilteredFhir(requestParams)
      .subscribe(
        (result) => this.displayFhir(result),
        (error) => this.log.error(error)
      );
  }

  displayFhir(fhir: any) {
    this.fhir = JSON.stringify(fhir, null, 2);

    this.params = '{"meta": {"profile": ["https://fhir.hl7.org.uk/STU3/OperationDefinition/CareConnect-GetStructuredRecord-Operation-1"]},\r'+
    '"resourceType": "Parameters",\r'+
    '"parameter": [{"name": "patientNHSNumber","valueIdentifier": {"system": "https://fhir.hl7.org.uk/Id/nhs-number","value": "9999999999"}}]}';
    this.url = "POST: https://rqy8l5vl8l.execute-api.eu-west-2.amazonaws.com/discovery-fhir-api-dev/patient/$getstructuredrecord\r\r";

    this.status = "200 (OK)";
  }


}
