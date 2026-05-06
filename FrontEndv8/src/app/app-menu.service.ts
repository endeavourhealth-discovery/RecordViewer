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
import {EmergencyDashboardComponent} from './carerecord/emergency-dashboard/emergency-dashboard.component';
import {AiReviewViewComponent} from './carerecord/ai-review-view/ai-review-view.component';
import {EmergencyMedicationComponent} from './carerecord/emergency-medication/emergency-medication.component';
import {EmergencyAllergyComponent} from './carerecord/emergency-allergy/emergency-allergy.component';
import {EmergencyConditionsComponent} from './carerecord/emergency-conditions/emergency-conditions.component';
import {EmergencyVitalsComponent} from './carerecord/emergency-vitals/emergency-vitals.component';
import {EmergencyWarningsComponent} from './carerecord/emergency-warnings/emergency-warnings.component';
import {EmergencyEncountersComponent} from './carerecord/emergency-encounters/emergency-encounters.component';

@Injectable()
export class AppMenuService implements AbstractMenuProvider {

  private _clinicianViewMode: string = 'gp';

  setClinicianViewMode(mode: string) {
    this._clinicianViewMode = mode;
  }

  getClinicianViewMode(): string {
    return this._clinicianViewMode;
  }

  static getRoutes(): Routes {
    return [
      {path: '', redirectTo: '/summary', pathMatch: 'full'},
      {path: 'demographic', component: DemographicComponent, data: {role: 'record-viewer', helpContext: 'Patient_demographics'}},
      {path: 'allergy', component: AllergyComponent, data: {role: 'record-viewer', helpContext: 'Allergies'}},
      {path: 'summary', component: CareSummaryComponent, data: {role: 'record-viewer', helpContext: 'Care_record_summary'}},
      {path: 'medication', component: MedicationComponent, data: {role: 'record-viewer', helpContext: 'Medication'}},
      {path: 'appointment', component: AppointmentComponent, data: {role: 'record-viewer', helpContext: 'Appointments'}},
      {path: 'referrals', component: ReferralsComponent, data: {role: 'record-viewer', helpContext: 'Referral_requests'}},
      {path: 'encounters', component: EncountersComponent, data: {role: 'record-viewer', helpContext: 'Encounters'}},
      {path: 'diagnostics', component: DiagnosticsComponent, data: {role: 'record-viewer', helpContext: 'Diagnostic_reports'}},
      {path: 'condition', component: ObservationComponent, data: {role: 'record-viewer', helpContext: 'Conditions', eventType: '1'}},
      {path: 'procedure', component: ObservationComponent, data: {role: 'record-viewer', helpContext: 'Procedures', eventType: '3'}},
      {path: 'observation', component: ObservationComponent, data: {role: 'record-viewer', helpContext: 'Observations', eventType: '2'}},
      {path: 'family', component: ObservationComponent, data: {role: 'record-viewer', helpContext: 'Family_history', eventType: '4'}},
      {path: 'immunisation', component: ObservationComponent, data: {role: 'record-viewer', helpContext: 'Immunisations', eventType: '5'}},
      {path: 'procedure requests', component: ObservationComponent, data: {role: 'record-viewer', helpContext: 'Procedure_requests', eventType: '6'}},
      {path: 'diagnostic orders', component: ObservationComponent, data: {role: 'record-viewer', helpContext: 'Diagnostic_orders', eventType: '7'}},
      {path: 'warnings', component: ObservationComponent, data: {role: 'record-viewer', helpContext: 'Warnings_%26_flags', eventType: '8'}},
      {path: 'registries', component: RegistriesComponent, data: {role: 'record-viewer', helpContext: 'Registries'}},
      // Emergency Clinician Routes
      {path: 'emergency-dashboard', component: EmergencyDashboardComponent, data: {role: 'record-viewer', helpContext: 'Emergency_Dashboard'}},
      {path: 'ai-review', component: AiReviewViewComponent, data: {role: 'record-viewer', helpContext: 'AI_Review'}},
      {path: 'emergency-medication', component: EmergencyMedicationComponent, data: {role: 'record-viewer', helpContext: 'Emergency_Medication'}},
      {path: 'emergency-allergy', component: EmergencyAllergyComponent, data: {role: 'record-viewer', helpContext: 'Emergency_Allergies'}},
      {path: 'emergency-conditions', component: EmergencyConditionsComponent, data: {role: 'record-viewer', helpContext: 'Emergency_Conditions'}},
      {path: 'emergency-vitals', component: EmergencyVitalsComponent, data: {role: 'record-viewer', helpContext: 'Emergency_Vitals'}},
      {path: 'emergency-warnings', component: EmergencyWarningsComponent, data: {role: 'record-viewer', helpContext: 'Emergency_Warnings'}},
      {path: 'emergency-encounters', component: EmergencyEncountersComponent, data: {role: 'record-viewer', helpContext: 'Emergency_Encounters'}},
    ];
  }

  getClientId(): string {
    return 'record-viewer';
  }

  getApplicationTitle(): string {
    return 'Record Viewer';
  }

  menu: MenuOption[] = [
    {icon: 'fas fa-home-alt', caption: 'Patient demographics', state: 'demographic'},
    {icon: 'fas fa-notes-medical', caption: 'Care record summary', state: 'summary'},
    {icon: 'fas fa-diagnoses', caption: 'Conditions', state: 'condition', badge: '0'},
    {icon: 'fas fa-pills', caption: 'Medication', state: 'medication', badge: '0'},
    {icon: 'fas fa-allergies', caption: 'Allergies', state: 'allergy', badge: '0'},
    {icon: 'fas fa-exclamation-triangle', caption: 'Warnings & flags', state: 'warnings', badge: '0'},
    {icon: 'fas fa-calendar-alt', caption: 'Appointments', state: 'appointment'},
    {icon: 'fas fa-user-doctor', caption: 'Encounters', state: 'encounters'},
    {icon: 'fas fa-heart-pulse', caption: 'Observations', state: 'observation'},
    {icon: 'fas fa-microscope', caption: 'Diagnostic orders', state: 'diagnostic orders'},
    {icon: 'fas fa-x-ray', caption: 'Diagnostic reports', state: 'diagnostics'},
    {icon: 'fas fa-house-medical-flag', caption: 'Procedure requests', state: 'procedure requests'},
    {icon: 'fas fa-procedures', caption: 'Procedures', state: 'procedure'},
    {icon: 'fas fa-hospital-user', caption: 'Referral requests', state: 'referrals'},
    {icon: 'fas fa-user-friends', caption: 'Family history', state: 'family'},
    {icon: 'fas fa-syringe', caption: 'Immunisations', state: 'immunisation'},
  ];

  emergencyMenu: MenuOption[] = [
    {icon: 'fas fa-robot', caption: 'AI Record Review', state: 'ai-review'},
    {icon: 'fas fa-truck-medical', caption: 'Emergency Dashboard', state: 'emergency-dashboard'},
    {icon: 'fas fa-pills', caption: 'Current Medications', state: 'emergency-medication'},
    {icon: 'fas fa-triangle-exclamation', caption: 'Allergies & Intolerances', state: 'emergency-allergy'},
    {icon: 'fas fa-diagnoses', caption: 'Active Conditions', state: 'emergency-conditions'},
    {icon: 'fas fa-heart-pulse', caption: 'Vitals & Observations', state: 'emergency-vitals'},
    {icon: 'fas fa-exclamation-triangle', caption: 'Warnings & Flags', state: 'emergency-warnings'},
    {icon: 'fas fa-user-doctor', caption: 'Recent Encounters', state: 'emergency-encounters'},
    {icon: 'fas fa-notes-medical', caption: 'GP Patient Summary', state: 'summary'},
  ];

  getMenuOptions(): MenuOption[] {
    const service = AppMenuService.getInstance();
    if (service && service._clinicianViewMode === 'emergency') {
      return this.emergencyMenu;
    }
    return this.menu;
  }

  setMenuBadge(index: number, value: string) {
    // Update badge on either GP or emergency menu based on current view mode
    const service = AppMenuService.getInstance();
    if (service && service._clinicianViewMode === 'emergency') {
      this.emergencyMenu[index] = this.emergencyMenu[index] || {};
      this.emergencyMenu[index].badge = value;
    } else {
      this.menu[index] = this.menu[index] || {};
      this.menu[index].badge = value;
    }
  }

  private static instance: AppMenuService;

  constructor() {
    AppMenuService.instance = this;
  }

  static getInstance(): AppMenuService | null {
    return AppMenuService.instance;
  }
}
