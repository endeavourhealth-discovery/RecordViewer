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
      {icon: 'fas fa-fire-alt', caption: 'FHIR Care Connect API', state: 'fhir'},
      {icon: 'fas fa-address-card', caption: 'Patient Demographics', state: 'demographic'},
      {icon: 'fas fa-notes-medical', caption: 'Care Record Summary', state: 'summary'},
      {icon: 'fas fa-lungs', caption: 'Conditions', state: 'condition'},
      {icon: 'fas fa-prescription-bottle-alt', caption: 'Medication', state: 'medication'},
      {icon: 'fas fa-radiation-alt', caption: 'Warnings', state: 'observation'},
      {icon: 'fas fa-book-medical', caption: 'Observations', state: 'observation'},
      {icon: 'fas fa-file-medical-alt', caption: 'Investigations', state: 'values'},
      {icon: 'fas fa-allergies', caption: 'Allergies', state: 'allergy'},
      {icon: 'fas fa-users-medical', caption: 'Encounters', state: 'observation'},
      {icon: 'fas fa-procedures', caption: 'Procedures', state: 'procedure'},
      {icon: 'fas fa-hospital', caption: 'Procedure requests', state: 'observation'},
      {icon: 'fas fa-hospital-user', caption: 'Referrals', state: 'observation'},
      {icon: 'fas fa-x-ray', caption: 'Diagnostic Reports', state: 'observation'},
      {icon: 'fas fa-users', caption: 'Family history', state: 'family'},
      {icon: 'fas fa-syringe', caption: 'Immunisations', state: 'immunisation'},
      {icon: 'fas fa-calendar-alt', caption: 'Appointments', state: 'observation'}
    ];
  }
}
