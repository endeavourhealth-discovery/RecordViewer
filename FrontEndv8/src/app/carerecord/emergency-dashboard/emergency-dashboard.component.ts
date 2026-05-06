import {Component, OnInit, ViewChild} from '@angular/core';
import {MatTableDataSource} from '@angular/material/table';
import {CareRecordService, AiReviewResponse, EmergencyFlags, ChatMessage} from '../carerecord.service';
import {LoggerService} from 'dds-angular8';
import {PrecisComponent} from "../precis/precis.component";
import {Globals} from "../globals";

@Component({
  selector: 'app-emergency-dashboard',
  templateUrl: './emergency-dashboard.component.html',
  styleUrls: ['./emergency-dashboard.component.scss']
})
export class EmergencyDashboardComponent implements OnInit {
  @ViewChild(PrecisComponent) precisComponentReference: PrecisComponent;

  globals: Globals;
  searchingForData = false;
  viewMode = 'gp';

  currentMedications: any[] = [];
  allergies: any[] = [];
  activeConditions: any[] = [];
  recentVitals: any[] = [];
  activeWarnings: any[] = [];
  recentEncounters: any[] = [];

  aiSummary = '';
  aiError = '';
  emergencyFlags: EmergencyFlags | null = null;
  generatingSummary = false;

  criticalAlerts: {type: string, icon: string, text: string}[] = [];

  constructor(
    private carerecordService: CareRecordService,
    private log: LoggerService,
    globals: Globals) {
    this.globals = globals;
  }

  ngOnInit() {
    if (this.precisComponentReference && this.precisComponentReference.nhsNumber !== '0') {
      this.loadAllData();
    }
  }

  loadAllData() {
    const nhsNumber = this.precisComponentReference?.nhsNumber;
    if (!nhsNumber || nhsNumber === '0') return;

    this.searchingForData = true;
    this.aiSummary = '';
    this.aiError = '';
    this.emergencyFlags = null;

    const combined = this.carerecordService.getMedication(nhsNumber, 1, 0).pipe(
      // We need to combine multiple observables - using forkJoin pattern manually
    );

    Promise.all([
      this.carerecordService.getMedication(nhsNumber, 1, 0).toPromise(),
      this.carerecordService.getAllergy(nhsNumber, 0).toPromise(),
      this.carerecordService.getObservation(nhsNumber, 1, 1, '', 0).toPromise(),
      this.carerecordService.getObservation(nhsNumber, 2, 0, '', 1).toPromise(),
      this.carerecordService.getObservation(nhsNumber, 8, 1, '', 0).toPromise(),
      this.carerecordService.getEncounters(nhsNumber, 1).toPromise()
    ]).then(([meds, allergiesData, conditions, vitals, warnings, encounters]) => {
      this.currentMedications = meds?.results || [];
      this.allergies = allergiesData?.results || [];
      this.activeConditions = conditions?.results || [];
      this.recentVitals = vitals?.results || [];
      this.activeWarnings = warnings?.results || [];
      this.recentEncounters = encounters?.results || [];

      this.buildCriticalAlerts();
    }).catch(error => {
      this.log.error('Error loading emergency dashboard data:', error);
    }).finally(() => {
      this.searchingForData = false;
    });
  }

  buildCriticalAlerts() {
    this.criticalAlerts = [];

    if (this.allergies.length > 0) {
      const allergyNames = this.allergies.slice(0, 3).map((a: any) => a.name).join(', ');
      this.criticalAlerts.push({
        type: 'allergy',
        icon: 'fas fa-triangle-exclamation',
        text: `ALLERGIES (${this.allergies.length}): ${allergyNames}${this.allergies.length > 3 ? '...' : ''}`
      });
    }

    if (this.activeWarnings.length > 0) {
      const warningNames = this.activeWarnings.slice(0, 2).map((w: any) => w.name).join(', ');
      this.criticalAlerts.push({
        type: 'warning',
        icon: 'fas fa-exclamation-circle',
        text: `WARNINGS (${this.activeWarnings.length}): ${warningNames}${this.activeWarnings.length > 2 ? '...' : ''}`
      });
    }

    if (this.currentMedications.length >= 5) {
      this.criticalAlerts.push({
        type: 'polypharmacy',
        icon: 'fas fa-pills',
        text: `POLYPHARMACY: ${this.currentMedications.length} current medications`
      });
    }

    if (this.activeConditions.length >= 3 && this.currentMedications.length >= 3) {
      this.criticalAlerts.push({
        type: 'multimorbidity',
        icon: 'fas fa-user-injured',
        text: `MULTI-MORBIDITY: ${this.activeConditions.length} conditions, ${this.currentMedications.length} medications`
      });
    }
  }

  generateAiSummary() {
    const nhsNumber = this.precisComponentReference?.nhsNumber;
    if (!nhsNumber || nhsNumber === '0') return;

    this.generatingSummary = true;
    this.aiError = '';
    this.emergencyFlags = null;

    this.carerecordService.generateAiReview(nhsNumber).subscribe({
      next: (response: AiReviewResponse) => {
        this.aiSummary = response.summary || 'No summary generated.';
        this.emergencyFlags = response.emergency_flags || null;
        this.generatingSummary = false;
      },
      error: (error: any) => {
        this.log.error('Error generating AI summary:', error);
        this.aiError = error.message || 'Failed to generate AI review. Please try again.';
        this.generatingSummary = false;
      }
    });
  }

  clearCache() {
    const nhsNumber = this.precisComponentReference?.nhsNumber;
    if (!nhsNumber || nhsNumber === '0') return;

    this.carerecordService.clearAiCache(nhsNumber).subscribe({
      next: () => {
        this.aiSummary = '';
        this.emergencyFlags = null;
      },
      error: (error: any) => {
        this.log.error('Error clearing cache:', error);
      }
    });
  }

  renderMarkdown(text: string): string {
    if (!text) return '';
    let html = text
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g, '<em>$1</em>')
      .replace(/#{1,6}\s*(.*?)(\n|$)/g, '<h4>$1</h4>')
      .replace(/\n/g, '<br/>');
    return html;
  }

  setViewMode(mode: string) {
    this.viewMode = mode;
  }
}
