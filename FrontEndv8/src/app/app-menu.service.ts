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
import {DashboardComponent} from "./carerecord/dashboard/dashboard.component";
import {AppointmentComponent} from "./carerecord/appointment/appointment.component";
import {ReferralsComponent} from "./carerecord/referrals/referrals.component";
import {EncountersComponent} from "./carerecord/encounters/encounters.component";

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
      {path: 'appointment', component: AppointmentComponent, data: {role: 'record-viewer'}},
      {path: 'referrals', component: ReferralsComponent, data: {role: 'record-viewer'}},
      {path: 'encounters', component: EncountersComponent, data: {role: 'record-viewer'}},
      {path: 'condition', component: ObservationComponent, data: {role: 'record-viewer', eventType: '1'}},
      {path: 'procedure', component: ObservationComponent, data: {role: 'record-viewer', eventType: '3'}},
      {path: 'observation', component: ObservationComponent, data: {role: 'record-viewer', eventType: '2'}},
      {path: 'family', component: ObservationComponent, data: {role: 'record-viewer', eventType: '4'}},
      {path: 'immunisation', component: ObservationComponent, data: {role: 'record-viewer', eventType: '5'}},
      {path: 'procedure requests', component: ObservationComponent, data: {role: 'record-viewer', eventType: '6'}},
      {path: 'diagnostic orders', component: ObservationComponent, data: {role: 'record-viewer', eventType: '7'}},
      {path: 'dashboard', component: DashboardComponent, data: {role: 'record-viewer'}}
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
      {icon: 'fas fa-home-alt', caption: 'Patient demographics', state: 'demographic'},
      {icon: 'fas fa-notes-medical', caption: 'Care record summary', state: 'summary'},
      {icon: 'fas fa-diagnoses', caption: 'Conditions', state: 'condition'},
      {icon: 'fas fa-pills', caption: 'Medication', state: 'medication'},
      {icon: 'fas fa-exclamation-triangle', caption: 'Warnings', state: 'observation'},
      {icon: 'fas fa-monitor-heart-rate', caption: 'Observations', state: 'observation'},
      {icon: 'fas fa-microscope', caption: 'Investigations', state: 'values'},
      {icon: 'fas fa-allergies', caption: 'Allergies', state: 'allergy'},
      {icon: 'fas fa-users-medical', caption: 'Encounters', state: 'encounters'},
      {icon: 'fas fa-procedures', caption: 'Procedures', state: 'procedure'},
      {icon: 'fas fa-user-md-chat', caption: 'Procedure requests', state: 'procedure requests'},
      {icon: 'fas fa-hospital-user', caption: 'Referrals', state: 'referrals'},
      {icon: 'fas fa-x-ray', caption: 'Diagnostic orders', state: 'diagnostic orders'},
      {icon: 'fas fa-user-friends', caption: 'Family history', state: 'family'},
      {icon: 'fas fa-syringe', caption: 'Immunisations', state: 'immunisation'},
      {icon: 'fas fa-calendar-alt', caption: 'Appointments', state: 'appointment'},
      {icon: 'fas fa-analytics', caption: 'Dashboard', state: 'dashboard'}
    ];
  }
}
