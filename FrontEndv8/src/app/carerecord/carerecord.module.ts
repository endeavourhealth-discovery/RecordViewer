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
        MatTooltipModule
    ],
  entryComponents: [
    PatientComponent,
    TrendComponent
  ],
  providers: [ Globals ]
})
export class CareRecordModule { }
