import {Injectable} from '@angular/core';
import {Routes} from '@angular/router';
import {CareSummaryComponent} from './carerecord/care-summary/care-summary.component';
import {MedicationComponent} from "./carerecord/medication/medication.component";
import {DemographicComponent} from "./carerecord/demographic/demographic.component";
import {ObservationComponent} from "./carerecord/observation/observation.component";
import {AllergyComponent} from "./carerecord/allergy/allergy.component";
import {AppointmentComponent} from "./carerecord/appointment/appointment.component";
import {ReferralsComponent} from "./carerecord/referrals/referrals.component";
import {EncountersComponent} from "./carerecord/encounters/encounters.component";
import {DiagnosticsComponent} from "./carerecord/diagnostics/diagnostics.component";
import {RegistriesComponent} from "./carerecord/registries/registries.component";
import {AbstractMenuProvider, MenuOption} from "dds-angular8";

@Injectable()
export class AppMenuService implements AbstractMenuProvider {

  static getRoutes(): Routes {
    return [
      {path: '', redirectTo: '/summary', pathMatch: 'full'},
      {path: 'demographic', component: DemographicComponent, data: {role: 'record-viewer'}},
      {path: 'allergy', component: AllergyComponent, data: {role: 'record-viewer'}},
      {path: 'summary', component: CareSummaryComponent, data: {role: 'record-viewer'}},
      {path: 'medication', component: MedicationComponent, data: {role: 'record-viewer'}},
      {path: 'appointment', component: AppointmentComponent, data: {role: 'record-viewer'}},
      {path: 'referrals', component: ReferralsComponent, data: {role: 'record-viewer'}},
      {path: 'encounters', component: EncountersComponent, data: {role: 'record-viewer'}},
      {path: 'diagnostics', component: DiagnosticsComponent, data: {role: 'record-viewer'}},
      {path: 'condition', component: ObservationComponent, data: {role: 'record-viewer', eventType: '1'}},
      {path: 'procedure', component: ObservationComponent, data: {role: 'record-viewer', eventType: '3'}},
      {path: 'observation', component: ObservationComponent, data: {role: 'record-viewer', eventType: '2'}},
      {path: 'family', component: ObservationComponent, data: {role: 'record-viewer', eventType: '4'}},
      {path: 'immunisation', component: ObservationComponent, data: {role: 'record-viewer', eventType: '5'}},
      {path: 'procedure requests', component: ObservationComponent, data: {role: 'record-viewer', eventType: '6'}},
      {path: 'diagnostic orders', component: ObservationComponent, data: {role: 'record-viewer', eventType: '7'}},
      {path: 'warnings', component: ObservationComponent, data: {role: 'record-viewer', eventType: '8'}},
      {path: 'registries', component: RegistriesComponent, data: {role: 'record-viewer'}},
    ];
  }

  getClientId(): string {
    return 'record-viewer';
  }

  getApplicationTitle(): string {
    return 'Patient Record Viewer';
  }

  menu: MenuOption[] = [
    {icon: 'fas fa-home-alt', caption: 'Patient demographics', state: 'demographic'},
    {icon: 'fas fa-notes-medical', caption: 'Care record summary', state: 'summary'},
    {icon: 'fas fa-diagnoses', caption: 'Conditions', state: 'condition', badge: '0'},
    {icon: 'fas fa-pills', caption: 'Medication', state: 'medication'},
    {icon: 'fas fa-allergies', caption: 'Allergies', state: 'allergy', badge: '0'},
    {icon: 'fas fa-exclamation-triangle', caption: 'Warnings & flags', state: 'warnings', badge: '0'},
    {icon: 'fas fa-calendar-check', caption: 'Registries', state: 'registries'},
    {icon: 'fas fa-calendar-alt', caption: 'Appointments', state: 'appointment'},
    {icon: 'fas fa-users-medical', caption: 'Encounters', state: 'encounters'},
    {icon: 'fas fa-monitor-heart-rate', caption: 'Observations', state: 'observation'},
    {icon: 'fas fa-microscope', caption: 'Diagnostic orders', state: 'diagnostic orders'},
    {icon: 'fas fa-x-ray', caption: 'Diagnostic reports', state: 'diagnostics'},
    {icon: 'fas fa-user-md-chat', caption: 'Procedure requests', state: 'procedure requests'},
    {icon: 'fas fa-procedures', caption: 'Procedures', state: 'procedure'},
    {icon: 'fas fa-hospital-user', caption: 'Referral requests', state: 'referrals'},
    {icon: 'fas fa-user-friends', caption: 'Family history', state: 'family'},
    {icon: 'fas fa-syringe', caption: 'Immunisations', state: 'immunisation'},
    ];

  getMenuOptions(): MenuOption[] {
    return this.menu;
  }

  setMenuBadge(index: number, value: string) {
    this.menu[index].badge = value;
  }

}
