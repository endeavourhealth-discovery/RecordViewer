import {Injectable} from '@angular/core';
import {Routes} from '@angular/router';
import {CareSummaryComponent} from './carerecord/care-summary/care-summary.component';
import {AbstractMenuProvider, MenuOption} from 'dds-angular8';
import {MedicationComponent} from "./carerecord/medication/medication.component";
import {FhirComponent} from "./carerecord/fhir/fhir.component";
import {DemographicComponent} from "./carerecord/demographic/demographic.component";
import {ObservationComponent} from "./carerecord/observation/observation.component";
import {ValuesComponent} from "./carerecord/values/values.component";
import {AllergyComponent} from "./carerecord/allergy/allergy.component";

@Injectable()
export class AppMenuService implements  AbstractMenuProvider {
  static getRoutes(): Routes {
    return [
      {path: '', redirectTo: '/summary', pathMatch: 'full'},
      {path: 'fhir', component: FhirComponent, data: {role: 'record-viewer:fhir'}},
      {path: 'demographic', component: DemographicComponent, data: {role: 'record-viewer'}},
      {path: 'allergy', component: AllergyComponent, data: {role: 'record-viewer'}},
      {path: 'summary', component: CareSummaryComponent, data: {role: 'record-viewer'}},
      {path: 'medication', component: MedicationComponent, data: {role: 'record-viewer'}},
      {path: 'values', component: ValuesComponent, data: {role: 'record-viewer'}},
      {path: 'condition', component: ObservationComponent, data: {role: 'record-viewer', eventType: '1'}},
      {path: 'procedure', component: ObservationComponent, data: {role: 'record-viewer', eventType: '3'}},
      {path: 'observation', component: ObservationComponent, data: {role: 'record-viewer', eventType: '2'}},
      {path: 'family', component: ObservationComponent, data: {role: 'record-viewer', eventType: '4'}},
      {path: 'immunisation', component: ObservationComponent, data: {role: 'record-viewer', eventType: '5'}}
    ];
  }

  getClientId(): string {
    return 'record-viewer';
  }

  getApplicationTitle(): string {
    return 'Patient Record Viewer';
  }

  getMenuOptions(): MenuOption[] {
    return [
      {icon: 'http', caption: 'FHIR Care Connect API', state: 'fhir'},
      {icon: 'account_circle', caption: 'Patient Demographics', state: 'demographic'},
      {icon: 'list_alt', caption: 'Care Record Summary', state: 'summary'},
      {icon: 'error_outline', caption: 'Conditions', state: 'condition'},
      {icon: 'enhanced_encryption', caption: 'Medication', state: 'medication'},
      {icon: 'notifications_active', caption: 'Warnings', state: 'observation'},
      {icon: 'event_note', caption: 'Observations', state: 'observation'},
      {icon: 'bar_chart', caption: 'Investigations', state: 'values'},
      {icon: 'warning', caption: 'Allergies', state: 'allergy'},
      {icon: 'supervised_user_circle', caption: 'Encounters', state: 'observation'},
      {icon: 'airline_seat_flat', caption: 'Procedures', state: 'procedure'},
      {icon: 'local_hospital', caption: 'Procedure requests', state: 'observation'},
      {icon: 'apartment', caption: 'Referrals', state: 'observation'},
      {icon: 'poll', caption: 'Diagnostic Reports', state: 'observation'},
      {icon: 'people_alt', caption: 'Family history', state: 'family'},
      {icon: 'colorize', caption: 'Immunisations', state: 'immunisation'},
      {icon: 'today', caption: 'Appointments', state: 'observation'}
    ];
  }
}
