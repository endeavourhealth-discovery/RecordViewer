import { NgModule } from '@angular/core';
import {CareSummaryComponent} from './care-summary/care-summary.component';
import {BrowserModule} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {
  MatCardModule,
  MatFormFieldModule,
  MatIconModule,
  MatInputModule,
  MatPaginatorModule, MatProgressSpinnerModule,
  MatSortModule,
  MatTableModule
} from '@angular/material';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {RouterModule} from '@angular/router';
import {FlexModule} from '@angular/flex-layout';
import {MatSelectModule} from '@angular/material/select';
import {MatSnackBarModule} from '@angular/material/snack-bar';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatMenuModule} from '@angular/material/menu';
import {CoreModule} from 'dds-angular8';
import {MatDialogModule} from '@angular/material/dialog';
import {MatButtonModule} from '@angular/material/button';
import {MatTreeModule} from '@angular/material/tree';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MedicationComponent} from "./medication/medication.component";
import {DemographicComponent} from "./demographic/demographic.component";
import {PatientComponent} from "./patient/patient.component";
import {TrendComponent} from "./trend/trend.component";
import {MatGridListModule} from "@angular/material/grid-list";
import {MatDatepickerModule} from "@angular/material/datepicker";
import {MatNativeDateModule} from "@angular/material/core";
import {PrecisComponent} from "./precis/precis.component";
import {Globals} from "./globals";
import {ObservationComponent} from "./observation/observation.component";
import {AllergyComponent} from "./allergy/allergy.component";
import {BarChartModule, LineChartModule, AreaChartModule} from "@swimlane/ngx-charts";
import {AppointmentComponent} from "./appointment/appointment.component";
import {ReferralsComponent} from "./referrals/referrals.component";
import {EncountersComponent} from "./encounters/encounters.component";
import {DiagnosticsComponent} from "./diagnostics/diagnostics.component";
import {MatTooltipModule} from "@angular/material/tooltip";
import {RegistriesComponent} from "./registries/registries.component";
import {MatTabsModule} from "@angular/material/tabs";
import {EmergencyDashboardComponent} from './emergency-dashboard/emergency-dashboard.component';
import {AiReviewViewComponent} from './ai-review-view/ai-review-view.component';
import {EmergencyMedicationComponent} from './emergency-medication/emergency-medication.component';
import {EmergencyAllergyComponent} from './emergency-allergy/emergency-allergy.component';
import {EmergencyConditionsComponent} from './emergency-conditions/emergency-conditions.component';
import {EmergencyVitalsComponent} from './emergency-vitals/emergency-vitals.component';
import {EmergencyWarningsComponent} from './emergency-warnings/emergency-warnings.component';
import {EmergencyEncountersComponent} from './emergency-encounters/emergency-encounters.component';
import {MatExpansionModule} from '@angular/material/expansion';
import {MatGridListModule as MatGridListModule2} from '@angular/material/grid-list';
import {MatProgressSpinnerModule as MatProgressSpinnerModule2} from '@angular/material/progress-spinner';
import {FormsModule} from '@angular/forms';

@NgModule({
  declarations: [
    PrecisComponent,
    DemographicComponent,
    PatientComponent,
    TrendComponent,
    CareSummaryComponent,
    MedicationComponent,
    ObservationComponent,
    AppointmentComponent,
    ReferralsComponent,
    EncountersComponent,
    DiagnosticsComponent,
    AllergyComponent,
    RegistriesComponent,
    EmergencyDashboardComponent,
    AiReviewViewComponent,
    EmergencyMedicationComponent,
    EmergencyAllergyComponent,
    EmergencyConditionsComponent,
    EmergencyVitalsComponent,
    EmergencyWarningsComponent,
    EmergencyEncountersComponent
    ],
    imports: [
        BrowserModule,
        BrowserAnimationsModule,
        MatCardModule,
        MatTableModule,
        MatSortModule,
        MatFormFieldModule,
        MatInputModule,
        MatIconModule,
        FormsModule,
        MatPaginatorModule,
        MatProgressSpinnerModule,
        RouterModule,
        FlexModule,
        MatSelectModule,
        MatSnackBarModule,
        MatCheckboxModule,
        MatMenuModule,
        MatDialogModule,
        CoreModule,
        MatButtonModule,
        MatTreeModule,
        MatProgressBarModule,
        MatGridListModule,
        MatNativeDateModule,
        MatDatepickerModule,
        BarChartModule,
        LineChartModule,
        AreaChartModule,
        ReactiveFormsModule,
    MatTooltipModule,
    MatTabsModule,
    MatExpansionModule,
    FormsModule
    ],
  entryComponents: [
    PatientComponent,
    TrendComponent
  ],
  providers: [ Globals ]
})
export class CareRecordModule { }
